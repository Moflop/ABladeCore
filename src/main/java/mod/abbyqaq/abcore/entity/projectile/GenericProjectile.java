package mod.abbyqaq.abcore.entity.projectile;

import mod.abbyqaq.abcore.init.ModEntities;
import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
import mod.abbyqaq.abcore.utils.EntityFindUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class GenericProjectile extends Entity {
	// 发射前的锚定模式
	public enum AnchorMode {OWNER, TARGET}

	// 发射前的瞄准模式
	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}

	// ===========实体数据（自动同步）===========
	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LOCKED_TARGET_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);

	// 1. 锚定模式 (用 String 存储，兼容性更好)
	private static final EntityDataAccessor<String> ANCHOR_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 3. 锚定偏移量 (客户端计算位置必需) 1.21.1 推荐使用 Vector3f
	private static final EntityDataAccessor<Vector3f> ANCHOR_OFFSET =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.VECTOR3);

	// 瞄准模式
	private static final EntityDataAccessor<String> AIM_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 旋转
	private static final EntityDataAccessor<Quaternionf> ROTATION =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.QUATERNION);

	// 横滚值
	private static final EntityDataAccessor<Float> ROLL =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TICK_COUNT = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<String> RENDER_TYPE = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// ============没有双端同步============
	@Nullable
	private UUID ownerUUID = null;
	@Nullable
	private UUID lockedTargetUUID = null;

	@Nullable
	private Entity cachedOwner = null;
	@Nullable
	private Entity cachedLockedTarget = null;

	// 上一次的旋转四元数，用于插帧
	public Quaternionf prevRotation = new Quaternionf();

	// 独立于 EntityData，用于保护客户端渲染不被服务端强行覆盖
	public Quaternionf clientRotation = new Quaternionf();

	public GenericProjectile(EntityType<? extends Entity> type, Level level) {
		super(type, level);
	}

	public GenericProjectile(Level level, LivingEntity shooter) {
		super(ModEntities.GENERIC_PROJECTILE.get(), level);
		this.setOwner(shooter);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// 初始值定义
		builder.define(OWNER_ID, -1);
		builder.define(LOCKED_TARGET_ID, -1);

		builder.define(ANCHOR_MODE, "");
		builder.define(ANCHOR_OFFSET, new Vector3f(0, 0, 0));
		builder.define(AIM_MODE, AimMode.FIXED.name());
		builder.define(ROTATION, new Quaternionf());
		builder.define(ROLL, 0.0F);

		builder.define(MAX_AGE, 1200);
		builder.define(TICK_COUNT, 0);
		builder.define(DELAY_SHOOT_TICKS, 0);
		builder.define(RENDER_TYPE, ModProjectileRenderTypes.DRIVE_TEST);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (ROTATION.equals(key)) {
			// 实体刚创建时将旋转同步到
			if (this.tickCount <= 1 && this.level().isClientSide) {
				this.clientRotation.set(this.entityData.get(ROTATION));
				this.prevRotation.set(this.getRotation());
			}
		}
	}

	// ============ EntityData 的 Set/Get 方法 ============
	public void setAnchorMode(@Nullable AnchorMode mode) {
		this.entityData.set(ANCHOR_MODE, mode == null ? "" : mode.name());
	}
	@Nullable
	public AnchorMode getAnchorMode() {
		String modeStr = this.entityData.get(ANCHOR_MODE);
		return modeStr.isEmpty() ? null : AnchorMode.valueOf(modeStr);
	}

	public void setAnchorOffset(@Nullable Vec3 offset) {
		if (offset != null) {
			this.entityData.set(ANCHOR_OFFSET, new Vector3f((float) offset.x, (float) offset.y, (float) offset.z));
		}
	}
	public Vec3 getAnchorOffset() {
		Vector3f vec = this.entityData.get(ANCHOR_OFFSET);
		return new Vec3(vec.x(), vec.y(), vec.z());
	}

	public void setAimMode(AimMode mode) {
		this.entityData.set(AIM_MODE, mode.name());
	}
	public AimMode getAimMode() {
		String modeStr = this.entityData.get(AIM_MODE);
		return modeStr.isEmpty() ? AimMode.FIXED : AimMode.valueOf(modeStr);
	}

	public void setRotation(Quaternionf quat) {
		if (this.level().isClientSide) {
			this.clientRotation.set(quat);
			return;
		}
		this.entityData.set(ROTATION, new Quaternionf(quat));
	}
	public Quaternionf getRotation() {
		// 客户端直接读取本地丝滑数据，不读 EntityData（避免拿到延迟数据）
		if (this.level().isClientSide) {
			return this.clientRotation;
		}
		return this.entityData.get(ROTATION);
	}

	public void setRoll(float roll) {
		this.entityData.set(ROLL, roll);
	}
	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public void setMaxAge(int maxAge) { this.entityData.set(MAX_AGE, maxAge); }
	public int getMaxAge() { return this.entityData.get(MAX_AGE); }

	public void setTickCount(int tickCount) { this.entityData.set(TICK_COUNT, tickCount); }
	public int getTickCount() { return this.entityData.get(TICK_COUNT); }

	public void setDelayShootTicks(int delayShootTicks) { this.entityData.set(DELAY_SHOOT_TICKS, delayShootTicks); }
	public int getDelayShootTicks() { return this.entityData.get(DELAY_SHOOT_TICKS); }

	public void setRenderType(String location) { this.entityData.set(RENDER_TYPE, location); }
	public String getRenderType() { return this.entityData.get(RENDER_TYPE); }

	// ============ 非 NBT 的 Set/Get 方法 ============

	public void setOwner(@Nullable Entity owner) {
		if (owner != null) {
			this.ownerUUID = owner.getUUID();
			this.cachedOwner = owner;
			this.entityData.set(OWNER_ID, owner.getId());
		}
	}
	public Entity getOwner() {
		if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
			return this.cachedOwner;
		}

		if (this.level().isClientSide) {
			int id = this.entityData.get(OWNER_ID);
			this.cachedOwner = id == -1 ? null : this.level().getEntity(id);
		}else {
			this.cachedOwner = this.ownerUUID == null ? null : ((ServerLevel) this.level()).getEntity(this.ownerUUID);
			this.entityData.set(OWNER_ID, cachedOwner == null ? -1 : cachedOwner.getId());
		}
		return this.cachedOwner;
	}

	public void setLockedTarget(@Nullable Entity target) {
		if (target != null) {
			this.lockedTargetUUID = target.getUUID();
			this.cachedLockedTarget = target;
			this.entityData.set(LOCKED_TARGET_ID, target.getId()); // 同步 ID
		}
	}
	@Nullable
	public Entity getLockedTarget() {
		if (this.cachedLockedTarget != null && !this.cachedLockedTarget.isRemoved()) {
			return this.cachedLockedTarget;
		}

		if (this.level().isClientSide) {
			int id = this.entityData.get(LOCKED_TARGET_ID);
			this.cachedLockedTarget = id == -1 ? null : this.level().getEntity(id);
		}else {
			this.cachedLockedTarget = this.lockedTargetUUID == null ? null : ((ServerLevel) this.level()).getEntity(this.lockedTargetUUID);
			this.entityData.set(LOCKED_TARGET_ID, cachedLockedTarget == null ? -1 : cachedLockedTarget.getId());
		}
		return this.cachedLockedTarget;
	}

	/**
	 * 双端均可用的获取锚定的实体
	 */
	@Nullable
	private Entity getAnchorEntity() {
		AnchorMode mode = this.getAnchorMode();
		if (mode == null) return null;

		return (mode == AnchorMode.TARGET) ? this.getLockedTarget() : this.getOwner();
	}

	// ============ 生成实体用的方法 ============

	// 设置初始化锚定模式和偏移量
	public void setStartAnchorModeAndOffset(AnchorMode anchorMode, @Nullable Vec3 anchorOffset) {
		this.setAnchorMode(anchorMode);
		this.setAnchorOffset(anchorOffset);

		Entity target = (anchorMode == AnchorMode.TARGET) ? this.getLockedTarget() : this.getOwner();
		if (target != null) {
			// 加入实体之前就将实体坐标设置到锚定位置，防止一帧抽搐
			Vec3 offset = anchorOffset == null ? Vec3.ZERO : anchorOffset;
			if (anchorMode == AnchorMode.OWNER) {
				float yawRadians = (float) Math.toRadians(-target.getYRot());
				offset = offset.yRot(yawRadians);
			}
			Vec3 startPos = target.position().add(offset);
			this.xo = startPos.x;
			this.yo = startPos.y;
			this.zo = startPos.z;
			this.setPos(startPos.x, startPos.y, startPos.z);

			this.startRiding(target, true);
		}
	}


	public void setStartShootDirection(float x, float y, float z) {

	}

	// ============ Tick 逻辑 ============

	@Override
	public void rideTick() {
		Entity vehicle = this.getVehicle();
		if (vehicle != null) {
			// 同步旧坐标，防止抽搐
			this.xo = this.getX();
			this.yo = this.getY();
			this.zo = this.getZ();

			Vec3 anchorOffset = this.getAnchorOffset();
			// 如果锚定的是玩家，跟随视角旋转
			if (this.getAnchorMode() == AnchorMode.OWNER) {
				float yawRadians = (float) Math.toRadians(-vehicle.getYRot());
				anchorOffset = anchorOffset.yRot(yawRadians);
			}
			Vec3 targetPos = vehicle.position().add(anchorOffset);

			// 设置到偏移位置
			this.setPos(targetPos.x, targetPos.y, targetPos.z);
			this.tick();
		}
	}

	@Override
	public void tick() {
		super.tick();
		this.prevRotation.set(this.getRotation());

		// 【重点修改】双端共同自增 lifetime，保持时间轴完全一致
		int currentLifetime = this.getTickCount() + 1;
		if (!this.level().isClientSide) {
			this.setTickCount(currentLifetime);
			if (currentLifetime > this.getMaxAge()) {
				this.discard();
			}

			if (currentLifetime <= this.getDelayShootTicks()) {
				if (this.getVehicle() == null) {
					Entity anchor = this.getAnchorEntity();
					if (anchor != null && !anchor.isRemoved()) {
						this.startRiding(anchor, true);
					}
				}

				this.setNoGravity(true);
				this.setDeltaMovement(Vec3.ZERO);
			}
		}
	}

	// =========== NBT存储 (已适配 EntityData) ===========
	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		if (this.ownerUUID != null) {
			tag.putUUID("Owner", this.ownerUUID);
		}
		if (this.lockedTargetUUID != null) {
			tag.putUUID("LockedTarget", this.lockedTargetUUID);
		}

		AnchorMode anchorMode = this.getAnchorMode();
		if (anchorMode != null) {
			tag.putString("AnchorMode", anchorMode.name());
		}

		// 保存锚点偏移量
		Vec3 offset = this.getAnchorOffset();
		CompoundTag anchorTag = new CompoundTag();
		anchorTag.putDouble("x", offset.x);
		anchorTag.putDouble("y", offset.y);
		anchorTag.putDouble("z", offset.z);
		tag.put("AnchorOffset", anchorTag);

		AimMode aimMode = this.getAimMode();
		if (aimMode != null) {
			tag.putString("AimMode", aimMode.name());
		}

		Quaternionf quat = this.entityData.get(ROTATION);
		CompoundTag rotTag = new CompoundTag();
		rotTag.putFloat("x", quat.x());
		rotTag.putFloat("y", quat.y());
		rotTag.putFloat("z", quat.z());
		rotTag.putFloat("w", quat.w());
		tag.put("ProjectileRotation", rotTag);
		tag.putFloat("Roll", this.entityData.get(ROLL));

		tag.putInt("MaxAge", this.entityData.get(MAX_AGE));
		tag.putInt("TickCount", this.entityData.get(TICK_COUNT));
		tag.putInt("DelayShootTicks", this.entityData.get(DELAY_SHOOT_TICKS));
		tag.putString("RenderType", this.entityData.get(RENDER_TYPE));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner")) {
			this.ownerUUID = tag.getUUID("Owner");
		}
		if (tag.hasUUID("LockedTarget")) {
			this.lockedTargetUUID = tag.getUUID("LockedTarget");
		}

		if (tag.contains("AnchorMode")) {
			try {
				this.setAnchorMode(AnchorMode.valueOf(tag.getString("AnchorMode")));
			} catch (IllegalArgumentException e) {
				this.setAnchorMode(null);
			}
		}

		if (tag.contains("AnchorOffset", Tag.TAG_COMPOUND)) {
			CompoundTag anchorTag = tag.getCompound("AnchorOffset");
			this.setAnchorOffset(new Vec3(anchorTag.getDouble("x"), anchorTag.getDouble("y"), anchorTag.getDouble("z")));
		}

		if (tag.contains("AimMode")) {
			try {
				this.setAimMode(AimMode.valueOf(tag.getString("AimMode")));
			} catch (IllegalArgumentException e) {
				this.setAimMode(AimMode.FIXED);
			}
		}

		if (tag.contains("ProjectileRotation", Tag.TAG_COMPOUND)) {
			CompoundTag rotTag = tag.getCompound("ProjectileRotation");

			Quaternionf savedQuat = new Quaternionf(
					rotTag.getFloat("x"),
					rotTag.getFloat("y"),
					rotTag.getFloat("z"),
					rotTag.getFloat("w")
			);

			this.entityData.set(ROTATION, savedQuat);
			if (this.prevRotation != null) {
				this.prevRotation.set(savedQuat);
			}
		}

		if (tag.contains("Roll")) {
			this.entityData.set(ROLL, tag.getFloat("Roll"));
		}

		if (tag.contains("MaxAge")) {
			this.setMaxAge(tag.getInt("MaxAge"));
		}
		if (tag.contains("TickCount")) {
			this.setTickCount(tag.getInt("TickCount"));
		}
		if (tag.contains("DelayShootTicks")) {
			this.setDelayShootTicks(tag.getInt("DelayShootTicks"));
		}
		if (tag.contains("RenderType")) {
			this.setRenderType(tag.getString("RenderType"));
		}
	}
}
