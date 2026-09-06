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
import net.minecraft.util.Mth;
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
	// =========== 模式枚举类型 ============
	// 发射前的锚定模式
	public enum AnchorMode {OWNER, TARGET}

	// 发射前的瞄准模式
	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}

	// 发射后的追踪模式
	public enum HomingMode {NONE, LOCKED_TARGET}

	// =========== 实体数据（服务端会自动同步客户端） ===========
	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LOCKED_TARGET_ID =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);

	// 锚定模式
	private static final EntityDataAccessor<String> ANCHOR_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 锚定偏移量
	private static final EntityDataAccessor<Vector3f> ANCHOR_OFFSET =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.VECTOR3);

	// 瞄准模式
	private static final EntityDataAccessor<String> AIM_MODE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// 横滚值(仅用于初始，动画应在渲染处实现)
	private static final EntityDataAccessor<Float> ROLL =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	// 最大寿命，单位：tick
	private static final EntityDataAccessor<Integer> MAX_AGE =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);

	// 延迟发射时间，单位：tick
	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.INT);

	// 是否已发射
	private static final EntityDataAccessor<Boolean> FIRED =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.BOOLEAN);
	// 发射时的初始速度
	private static final EntityDataAccessor<Float> VELOCITY =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	// 发射时是否有重力
	private static final EntityDataAccessor<Boolean> HAS_GRAVITY =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.BOOLEAN);

	// 发射时的重力加速度
	private static final EntityDataAccessor<Float> GRAVITY =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	// 发射时的空气阻力
	private static final EntityDataAccessor<Float> FRICTION_AIR =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	// 发射时的水中阻力
	private static final EntityDataAccessor<Float> FRICTION_WATER =
			SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.FLOAT);

	// 渲染类型
	private static final EntityDataAccessor<String> RENDER_TYPE = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);

	// ============ 没有自动同步 ============
	// ============ 仅在服务端 ============
	@Nullable
	private UUID ownerUUID = null;
	@Nullable
	private UUID lockedTargetUUID = null;

	// =========== 双端存在 ===========
	@Nullable
	private Entity cachedOwner = null;
	@Nullable
	private Entity cachedLockedTarget = null;

	public GenericProjectile(EntityType<? extends Entity> type, Level level) {
		super(type, level);
	}

	public GenericProjectile(Level level, LivingEntity shooter) {
		super(ModEntities.GENERIC_PROJECTILE.get(), level);
		this.setOwner(shooter);
	}

	@Override
	public void rideTick() {
		if (!this.isFired()) {
			this.setOldPosAndRot();
			Entity vehicle = this.getVehicle();
			if (vehicle != null) {
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
			}
		}else {
			// 发射前脱离锚定
			if (this.isPassenger()) {
				this.stopRiding();
			}
		}

		this.tick();
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

		this.setShootMovementDir(dirVec);
	}

	@Override
	public void tick() {
		// 防止抽搐
		if (!this.isPassenger()) {
			this.setOldPosAndRot();
		}

		Vec3 deltaMovement = this.getDeltaMovement();
		this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(deltaMovement.y, deltaMovement.horizontalDistance()) * (double) Mth.RAD_TO_DEG)));
		if (deltaMovement.horizontalDistanceSqr() > 1.0E-7D) {
			this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(deltaMovement.x, deltaMovement.z) * (double) Mth.RAD_TO_DEG)));
		}

		if (!this.isFired()) {
			// 未发射时确保继续锚定
			if (this.getVehicle() == null) {
				Entity anchor = this.getAnchorEntity();
				if (anchor != null && !anchor.isRemoved()) {
					this.startRiding(anchor, true);
				}
			}
			// 延迟发射期间禁用重力
			this.setNoGravity(true);
		}else {
			// 发射后移动实体
			double nextX = this.getX() + deltaMovement.x;
			double nextY = this.getY() + deltaMovement.y;
			double nextZ = this.getZ() + deltaMovement.z;
			this.setPos(nextX, nextY, nextZ);

			// 并应用摩擦力、重力
			float friction = this.isInWater() ? this.getFrictionWater() : this.getFrictionAir();
			deltaMovement = deltaMovement.scale(friction);
			deltaMovement = deltaMovement.subtract(0, this.getGravity(), 0);
			this.setDeltaMovement(deltaMovement);
		}


		if (!this.level().isClientSide) {
			// 必须服务端触发发射，确保客户端比服务端慢，防止回拉
			if (!this.isFired() && this.tickCount >= this.getDelayShootTicks()) {
				// 在移动判断代码块后触发，确保发射的tick不会移动
				this.setFired(true);

				// 发射前应用速度和恢复重力设置
				this.setDeltaMovement(deltaMovement.scale(this.getVelocity()));
				this.setNoGravity(!this.hasGravity());
			}

			if (this.tickCount > this.getMaxAge()) {
				// 寿命已尽
				this.burst();
			}
		}
	}

	protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
		while(pTargetRotation - pCurrentRotation < -180.0F) {
			pCurrentRotation -= 360.0F;
		}

		while(pTargetRotation - pCurrentRotation >= 180.0F) {
			pCurrentRotation += 360.0F;
		}

		return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
	}

	public void burst(){
		this.discard();
	}

	// ============ 非EntityData（没有自动同步） 的 Set/Get 方法 ============
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

	public void setFired(boolean fired) { this.entityData.set(FIRED, fired); }
	public boolean isFired() { return this.entityData.get(FIRED); }

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

	public void setShootMovementDir(double x, double y, double z) {
		Vec3 direction = new Vec3(x, y, z);
		direction = direction.lengthSqr() > 1.0E-4D ? direction.normalize() : new Vec3(0.0D, 0.0D, 1.0D);

		this.setDeltaMovement(direction);
		this.hasImpulse = true;

		if (direction.horizontalDistanceSqr() > 1.0E-7D) {
			float yRot = (float) (Mth.atan2(direction.x, direction.z) * (double) Mth.RAD_TO_DEG);
			this.setYRot(yRot);
		}

		float xRot = (float) (Mth.atan2(direction.y, direction.horizontalDistance()) * (double) Mth.RAD_TO_DEG);
		this.setXRot(xRot);
	}

	public void setShootMovementDir(Vec3 direction) {
		this.setShootMovementDir(direction.x, direction.y, direction.z);
	}

	// ============ 其它 ============
	// 自动清理机制
	@Override
	public void checkDespawn() {
		super.checkDespawn();
		if (!this.level().isClientSide) {
			// 256格内没有玩家立即消失
			double instantDespawnDistance = 256.0D;
			if (!this.level().hasNearbyAlivePlayer(
					this.getX(),
					this.getY(),
					this.getZ(),
					instantDespawnDistance
			)) {
				this.discard();
			}
		}
	}

	// 渲染距离
	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double d0 = this.getBoundingBox().getSize();
		if (Double.isNaN(d0)) {
			d0 = 1.0F;
		}

		d0 *= (double)256.0F * Entity.getViewScale();
		return distance < d0 * d0;
	}

	// 插值运动
	@Override
	public void lerpMotion(double x, double y, double z) {
		this.setDeltaMovement(x, y, z);
		// 防止第一帧同步跳帧
		if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
			double d0 = Math.sqrt(x * x + z * z);
			this.setXRot((float)(Mth.atan2(y, d0) * (double)(180F / (float)Math.PI)));
			this.setYRot((float)(Mth.atan2(x, z) * (double)(180F / (float)Math.PI)));
			this.xRotO = this.getXRot();
			this.yRotO = this.getYRot();
			this.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
		}

	}

	// ============ 数据存储/同步 ============
	// entityData默认值
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// 初始值定义
		builder.define(OWNER_ID, -1);
		builder.define(LOCKED_TARGET_ID, -1);

		builder.define(ANCHOR_MODE, "");
		builder.define(ANCHOR_OFFSET, new Vector3f(0, 0, 0));
		builder.define(AIM_MODE, AimMode.FIXED.name());
		builder.define(ROLL, 0.0F);

		builder.define(MAX_AGE, 1200);
		builder.define(DELAY_SHOOT_TICKS, 0);

		builder.define(FIRED, false);
		builder.define(VELOCITY, 1.0F);
		builder.define(HAS_GRAVITY, false);
		builder.define(GRAVITY, 0.03F);
		builder.define(FRICTION_AIR, 1.0F);
		builder.define(FRICTION_WATER, 1.0F);

		builder.define(RENDER_TYPE, ModProjectileRenderTypes.DRIVE_TEST);
	}

	// 生成时写入传给客户端
	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
		buffer.writeInt(this.tickCount);
	}

	// 生成时从服务端读取
	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer) {
		this.tickCount = buffer.readInt();
	}

	// 保存nbt
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

		tag.putFloat("Roll", this.entityData.get(ROLL));

		tag.putInt("TickCount", this.tickCount);
		tag.putInt("MaxAge", this.entityData.get(MAX_AGE));
		tag.putInt("DelayShootTicks", this.entityData.get(DELAY_SHOOT_TICKS));

		tag.putBoolean("Fired", this.entityData.get(FIRED));
		tag.putFloat("Velocity", this.entityData.get(VELOCITY));
		tag.putBoolean("HasGravity", this.entityData.get(HAS_GRAVITY));
		tag.putFloat("Gravity", this.entityData.get(GRAVITY));
		tag.putFloat("frictionAir", this.entityData.get(FRICTION_AIR));
		tag.putFloat("frictionWater", this.entityData.get(FRICTION_WATER));

		tag.putString("RenderType", this.entityData.get(RENDER_TYPE));
	}

	// 读取nbt
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

		if (tag.contains("Roll")) {
			this.entityData.set(ROLL, tag.getFloat("Roll"));
		}

		if (tag.contains("TickCount")) {
			this.tickCount = tag.getInt("TickCount");
		}
		if (tag.contains("MaxAge")) {
			this.setMaxAge(tag.getInt("MaxAge"));
		}
		if (tag.contains("DelayShootTicks")) {
			this.setDelayShootTicks(tag.getInt("DelayShootTicks"));
		}

		if (tag.contains("Fired")) {
			this.setFired(tag.getBoolean("Fired"));
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
}
