package mod.abbyqaq.abcore.entity.projectile;

import mod.abbyqaq.abcore.init.ModEntities;
import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
import mod.abbyqaq.abcore.utils.EntityFindUtils;
import mods.flammpfeil.slashblade.entity.EntityBlisteringSwords;
import mods.flammpfeil.slashblade.entity.EntityHeavyRainSwords;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
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
public class GenericProjectile extends Entity implements IEntityWithComplexSpawn {
	// 发射前的锚定模式
	public enum AnchorMode {OWNER, TARGET}

	// 发射前的瞄准模式
	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}

	// 发射后的追踪模式
	public enum HomingMode {NONE, LOCKED_TARGET}

	// ===========实体数据（自动同步）===========
	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LOCKED_TARGET_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);

	// 锚定模式 (用 String 存储，兼容性更好)
	private static final EntityDataAccessor<String> ANCHOR_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 锚定偏移量 (客户端计算位置必需) 1.21.1 推荐使用 Vector3f
	private static final EntityDataAccessor<Vector3f> ANCHOR_OFFSET =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.VECTOR3);

	// 瞄准模式
	private static final EntityDataAccessor<String> AIM_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 运动方向 (仅方向，不含速度)
	private static final EntityDataAccessor<Vector3f> SHOOT_DIRECTION =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.VECTOR3);

	// 横滚值(仅用于初始，动画应在渲染处实现)
	private static final EntityDataAccessor<Float> ROLL =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	private static final EntityDataAccessor<Integer> MAX_AGE = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> VELOCITY = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> HAS_GRAVITY = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> GRAVITY = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> FRICTION_AIR = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> FRICTION_WATER = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

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

	public Quaternionf clientPrevRotation = new Quaternionf();
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
		builder.define(SHOOT_DIRECTION, new Vector3f(0, 0, 1));
		builder.define(ROLL, 0.0F);

		builder.define(MAX_AGE, 1200);
		builder.define(DELAY_SHOOT_TICKS, 0);
		builder.define(VELOCITY, 1.0F);
		builder.define(HAS_GRAVITY, false);
		builder.define(GRAVITY, 0.03F);
		builder.define(FRICTION_AIR, 1.0F);
		builder.define(FRICTION_WATER, 1.0F);
		builder.define(RENDER_TYPE, ModProjectileRenderTypes.DRIVE_TEST);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		// 确保只在实体刚生成的初始阶段执行
		if (this.tickCount <= 1 && this.level().isClientSide) {
			if (SHOOT_DIRECTION.equals(key) || ROLL.equals(key)) {
				Vector3f initDir = new Vector3f(this.getShootDirection());
				float initRoll = this.getRoll();
				Quaternionf initQuat = this.calculateRotationFromDirection(initDir, initRoll);
				this.clientRotation.set(initQuat);
				this.clientPrevRotation.set(initQuat);
			}
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(this.tickCount);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		this.tickCount = buffer.readInt();
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
		this.handlerDelayShootMovementDirection();
	}
	public AimMode getAimMode() {
		String modeStr = this.entityData.get(AIM_MODE);
		return modeStr.isEmpty() ? AimMode.FIXED : AimMode.valueOf(modeStr);
	}

	public void setShootDirection(Vector3f direction) {
		this.entityData.set(SHOOT_DIRECTION, direction);
	}

	public Vector3f getShootDirection() {
		return this.entityData.get(SHOOT_DIRECTION);
	}

	public void setRoll(float roll) {
		this.entityData.set(ROLL, roll);
	}
	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public void setMaxAge(int maxAge) { this.entityData.set(MAX_AGE, maxAge); }
	public int getMaxAge() { return this.entityData.get(MAX_AGE); }

	public void setDelayShootTicks(int delayShootTicks) { this.entityData.set(DELAY_SHOOT_TICKS, delayShootTicks); }
	public int getDelayShootTicks() { return this.entityData.get(DELAY_SHOOT_TICKS); }

	public void setVelocity(float velocity) { this.entityData.set(VELOCITY, velocity); }
	public float getVelocity() { return this.entityData.get(VELOCITY); }

	public void setHasGravity(boolean hasGravity) { this.entityData.set(HAS_GRAVITY, hasGravity); }
	public boolean hasGravity() { return this.entityData.get(HAS_GRAVITY); }

	public void setGravity(float gravity) { this.entityData.set(GRAVITY, gravity); }
	@Override
	protected double getDefaultGravity() {
		return this.entityData.get(GRAVITY);
	}

	public void setFrictionAir(float frictionAir) { this.entityData.set(FRICTION_AIR, frictionAir); }
	public float getFrictionAir() { return this.entityData.get(FRICTION_AIR); }

	public void setFrictionWater(float frictionWater) { this.entityData.set(FRICTION_WATER, frictionWater); }
	public float getFrictionWater() { return this.entityData.get(FRICTION_WATER); }

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
		Vector3f direction = new Vector3f(x, y, z);
		// 防止传入全0向量导致归一化时发生除以0的错误
		if (direction.lengthSquared() > 0.0001F) {
			direction.normalize(); // 归一化，使其仅表示方向
		} else {
			direction.set(0, 0, 1);
		}
		this.setShootDirection(direction);
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
			// 处理延迟发射期间瞄准的方向
			this.handlerDelayShootMovementDirection();
			this.tick();
		}
	}

	@Override
	public void tick() {
		super.tick();

		int currentLifetime = this.tickCount;
		if (currentLifetime <= this.getDelayShootTicks()) {
			if (this.getVehicle() == null) {
				Entity anchor = this.getAnchorEntity();
				if (anchor != null && !anchor.isRemoved()) {
					this.startRiding(anchor, true);
				}
			}

			this.setNoGravity(true);
			this.setDeltaMovement(Vec3.ZERO);
		}else if (currentLifetime == this.getDelayShootTicks() + 1) {
			// 脱离骑乘
			if (this.isPassenger()) {
				this.stopRiding();
			}

			Vec3 dir = new Vec3(this.getShootDirection()).scale(this.getVelocity());
			this.setDeltaMovement(dir);

			this.setNoGravity(!this.hasGravity());
			// 通知服务端立即向客户端发送速度更新包，防止起步瞬间位置卡顿
			// this.hasImpulse = true;
		}else {
			// 服务端处理追踪
			if (!this.level().isClientSide) {
				// 如果有追踪模式，可以在这里实时修正方向
				// this.handlerHomingMovement();
			}
		}

		if (currentLifetime > this.getDelayShootTicks() + 1) {
			this.xo = this.getX();
			this.yo = this.getY();
			this.zo = this.getZ();

			Vec3 delta = this.getDeltaMovement();
			double nextX = this.getX() + delta.x;
			double nextY = this.getY() + delta.y;
			double nextZ = this.getZ() + delta.z;
			this.setPos(nextX, nextY, nextZ);

			// 摩擦力系数
			float friction = this.isInWater() ? this.getFrictionWater() : this.getFrictionAir();
			delta = delta.scale(friction);
			// 重力
			delta = delta.subtract(0, this.getGravity(), 0);
			// 实际应用
			this.setDeltaMovement(delta);
		}

		if (!this.level().isClientSide) {
			if (currentLifetime > this.getMaxAge()) {
				this.discard();
			}
		}else {
			this.clientPrevRotation.set(this.clientRotation);
			Vector3f currentDir = new Vector3f(this.getShootDirection());
			Quaternionf targetQuat = this.calculateRotationFromDirection(currentDir, this.getRoll());

			this.clientRotation.set(targetQuat);

			if (this.tickCount <= 1) {
				this.clientPrevRotation.set(this.clientRotation);
			}
		}
	}

	private void handlerDelayShootMovementDirection() {
		AimMode aimMode = this.getAimMode();
		if (aimMode == AimMode.FIXED) {
			return;
		}

		Vec3 targetPos = switch (aimMode) {
			case OWNER_LOOK -> {
				Entity owner = this.getOwner();
				if (owner == null || !owner.isAlive()) yield null;

				Entity crosshairTarget = EntityFindUtils.getNearestAnyEntityToCrosshair(
						owner, 30.0F, 0.0F, e -> e instanceof LivingEntity
				);

				if (crosshairTarget != null) {
					yield crosshairTarget.getBoundingBox().getCenter();
				}
				Vec3 lookVec = owner.getLookAngle();
				yield owner.getEyePosition().add(lookVec.scale(30.0D));
			}
			case TARGET_CENTER -> {
				Entity target = this.getLockedTarget();
				yield (target != null && target.isAlive()) ? target.getBoundingBox().getCenter() : null;
			}
			default -> null;
		};

		if (targetPos == null) {
			return;
		}

		Vec3 dirVec = targetPos.subtract(this.position());
		Vector3f newDir = new Vector3f((float) dirVec.x, (float) dirVec.y, (float) dirVec.z);

		if (newDir.lengthSquared() > 0.0001F) {
			this.setShootDirection(newDir.normalize());
		}
	}

	public float lastValidYaw = 0.0F;
	/**
	 * 根据方向向量与翻滚角生成对应的四元数
	 */
	public Quaternionf calculateRotationFromDirection(Vector3f direction, float roll) {
		float horizontalDistanceSq = direction.x * direction.x + direction.z * direction.z;
		float horizontalDistance = (float) Math.sqrt(horizontalDistanceSq);

		float yaw;

		// 增加状态阻断：仅在延迟发射阶段（仍挂载/跟随期间）才允许视角跟随
		boolean isAimingPhase = this.tickCount <= this.getDelayShootTicks();

		if (this.getAimMode() == AimMode.OWNER_LOOK && horizontalDistanceSq < 0.0001F && isAimingPhase) {
			Entity owner = this.getOwner();
			if (owner != null) {
				yaw = (float) Math.toRadians(-owner.getYRot());
			} else {
				yaw = this.lastValidYaw;
			}
		}
		// 正常计算（包含 TARGET_CENTER 锁定模式及所有已发射的投射物）
		else if (horizontalDistanceSq > 0.0001F) {
			yaw = (float) Math.atan2(direction.x, direction.z);
			this.lastValidYaw = yaw;
		}
		// 绝对极点保护（目标正上/正下，且不在瞄准阶段时锁定为最后的有效 yaw）
		else {
			yaw = this.lastValidYaw;
		}

		float pitch = (float) Math.atan2(direction.y, horizontalDistance);
		Quaternionf quat = new Quaternionf();
		quat.rotationY(yaw);
		quat.rotateX(-pitch);

		if (Math.abs(roll) > 1e-4F) {
			quat.rotateZ((float) Math.toRadians(roll));
		}

		return quat;
	}

	// =========== NBT存储===========
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

		Vector3f dir = this.getShootDirection();
		CompoundTag dirTag = new CompoundTag();
		dirTag.putFloat("x", dir.x());
		dirTag.putFloat("y", dir.y());
		dirTag.putFloat("z", dir.z());
		tag.put("MovementDirection", dirTag);

		tag.putFloat("Roll", this.entityData.get(ROLL));

		tag.putInt("MaxAge", this.entityData.get(MAX_AGE));
		tag.putInt("TickCount", this.tickCount);
		tag.putInt("DelayShootTicks", this.entityData.get(DELAY_SHOOT_TICKS));
		tag.putFloat("Velocity", this.entityData.get(VELOCITY));
		tag.putBoolean("HasGravity", this.entityData.get(HAS_GRAVITY));
		tag.putFloat("Gravity", this.entityData.get(GRAVITY));
		tag.putFloat("frictionAir", this.entityData.get(FRICTION_AIR));
		tag.putFloat("frictionWater", this.entityData.get(FRICTION_WATER));
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

		if (tag.contains("MovementDirection", Tag.TAG_COMPOUND)) {
			CompoundTag dirTag = tag.getCompound("MovementDirection");
			this.setShootDirection(new Vector3f(
					dirTag.getFloat("x"),
					dirTag.getFloat("y"),
					dirTag.getFloat("z")
			));
		}

		if (tag.contains("Roll")) {
			this.entityData.set(ROLL, tag.getFloat("Roll"));
		}

		if (tag.contains("MaxAge")) {
			this.setMaxAge(tag.getInt("MaxAge"));
		}
		if (tag.contains("TickCount")) {
			this.tickCount = tag.getInt("TickCount");
		}
		if (tag.contains("DelayShootTicks")) {
			this.setDelayShootTicks(tag.getInt("DelayShootTicks"));
		}
		if (tag.contains("Velocity")) {
			this.setVelocity(tag.getFloat("Velocity"));
		}
		if (tag.contains("HasGravity")) {
			this.setHasGravity(tag.getBoolean("HasGravity"));
		}
		if (tag.contains("Gravity")) {
			this.setGravity(tag.getFloat("Gravity"));
		}
		if (tag.contains("frictionAir")) {
			this.setFrictionAir(tag.getFloat("frictionAir"));
		}
		if (tag.contains("frictionWater")) {
			this.setFrictionWater(tag.getFloat("frictionWater"));
		}
		if (tag.contains("RenderType")) {
			this.setRenderType(tag.getString("RenderType"));
		}
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double d0 = this.getBoundingBox().getSize();
		if (Double.isNaN(d0)) {
			d0 = 1.0F;
		}

		d0 *= (double)256.0F * Entity.getViewScale();
		return distance < d0 * d0;
	}
}
