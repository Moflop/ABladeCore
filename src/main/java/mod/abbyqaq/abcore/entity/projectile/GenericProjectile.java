package mod.abbyqaq.abcore.entity.projectile;

import mod.abbyqaq.abcore.init.ModEntities;
import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
import mod.abbyqaq.abcore.utils.EntityFindHelper;
import mod.abbyqaq.abcore.utils.EntityFindUtils;
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


	// 发射前的瞄准模式
	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}


	// 发射后的追踪模式
	public enum HomingMode {NONE, LOCKED_TARGET}


	// =========== 实体数据（服务端会自动同步客户端） ===========
	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> LOCKED_TARGET_ID =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);

	// 瞄准模式
	private static final EntityDataAccessor<String> AIM_MODE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.STRING);

	// 横滚值(仅用于初始，动画应在渲染处实现)
	private static final EntityDataAccessor<Float> ROLL =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 最大寿命，单位：tick
	private static final EntityDataAccessor<Integer> MAX_AGE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);

	// 延迟发射时间，单位：tick
	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);

	// 是否已发射
	private static final EntityDataAccessor<Boolean> FIRED =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.BOOLEAN);
	// 发射时的初始速度
	private static final EntityDataAccessor<Float> VELOCITY =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 发射时是否有重力
	private static final EntityDataAccessor<Boolean> HAS_GRAVITY =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.BOOLEAN);

	// 发射时的重力加速度
	private static final EntityDataAccessor<Float> GRAVITY =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 发射时的空气阻力
	private static final EntityDataAccessor<Float> FRICTION_AIR =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 发射时的水中阻力
	private static final EntityDataAccessor<Float> FRICTION_WATER =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 追踪模式
	private static final EntityDataAccessor<String> HOMING_MODE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.STRING);

	// 每 tick 最大转向角度（度）
	private static final EntityDataAccessor<Float> HOMING_TURN_RATE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 自动搜索锁定目标的半径
	private static final EntityDataAccessor<Float> HOMING_SEARCH_RADIUS =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);

	// 渲染类型
	private static final EntityDataAccessor<String> RENDER_TYPE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.STRING);

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
					float yawRadians =
							(float) Math.toRadians(-vehicle.getYRot());
					anchorOffset = anchorOffset.yRot(yawRadians);
				}
				Vec3 targetPos = vehicle.position().add(anchorOffset);

				// 设置到偏移位置
				this.setPos(targetPos.x, targetPos.y, targetPos.z);
				// 处理延迟发射期间瞄准的方向
				this.handlerDelayShootMovementDirection();
			}
		} else {
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
				if (owner == null || !owner.isAlive())
					yield null;

				Entity crosshairTarget =
						EntityFindUtils.getNearestAnyEntityToCrosshair(
								owner, 30.0F, 0.0F,
								e -> e instanceof LivingEntity);

				if (crosshairTarget != null) {
					yield crosshairTarget.getBoundingBox().getCenter();
				}
				Vec3 lookVec = owner.getLookAngle();
				yield owner.getEyePosition().add(lookVec.scale(30.0D));
			}
			case TARGET_CENTER -> {
				Entity target = this.getLockedTarget();
				yield (target != null && target.isAlive()) ?
						target.getBoundingBox().getCenter() :
						null;
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
		this.setXRot(lerpRotation(this.xRotO, (float) (Mth.atan2(deltaMovement.y,
				deltaMovement.horizontalDistance()) * (double) Mth.RAD_TO_DEG)));
		if (deltaMovement.horizontalDistanceSqr() > 1.0E-7D) {
			this.setYRot(lerpRotation(this.yRotO, (float) (Mth.atan2(deltaMovement.x,
					deltaMovement.z) * (double) Mth.RAD_TO_DEG)));
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
		} else {
			// 发射后先处理追踪转向
			this.handleHoming();

			deltaMovement = this.getDeltaMovement();
			// 发射后移动实体
			double nextX = this.getX() + deltaMovement.x;
			double nextY = this.getY() + deltaMovement.y;
			double nextZ = this.getZ() + deltaMovement.z;
			this.setPos(nextX, nextY, nextZ);

			// 并应用摩擦力、重力
			float friction = this.isInWater() ?
					this.getFrictionWater() :
					this.getFrictionAir();
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

	private void handleHoming() {
		if (this.getHomingMode() != HomingMode.LOCKED_TARGET)
			return;

		Entity target = this.getLockedTarget();
		if (target == null || !target.isAlive()) {
			Entity newTarget = EntityFindUtils.getNearestAnyEntity(this,
					this.getHomingSearchRadius(),
					entity -> entity instanceof LivingEntity livingEntity && (this.getOwner() == null || !(this.getOwner() instanceof LivingEntity owner) || EntityFindHelper.ATTACKABLE_TARGETING.test(
							owner, livingEntity)));

			if (newTarget != null && newTarget != this.getOwner()) {
				this.setLockedTarget(newTarget);
			} else {
				this.setLockedTarget(null);
			}
		}

		// 双端执行转向（客户端依赖已同步的目标ID）
		Entity lockedTarget = this.getLockedTarget();
		if (lockedTarget != null) {
			this.applyHomingSteering(lockedTarget.getBoundingBox().getCenter());
		}
	}

	private void applyHomingSteering(Vec3 targetPos) {
		Vec3 currentVel = this.getDeltaMovement();
		double speed = currentVel.length();
		if (speed <= 1.0E-4)
			return; // 速度几乎为零，不处理

		Vec3 currentDir = currentVel.normalize();
		Vec3 targetDir = targetPos.subtract(this.position()).normalize();

		double dot = Mth.clamp(currentDir.dot(targetDir), -1.0, 1.0);
		double angle = Math.acos(dot);
		double maxTurnRad = Math.toRadians(this.getHomingTurnRate());

		if (angle > maxTurnRad) {
			// 限制最大转向角
			Vec3 axis = currentDir.cross(targetDir);
			if (axis.lengthSqr() < 1.0E-8) {
				// 方向完全相反，任选垂直轴
				axis = currentDir.cross(new Vec3(0, 1, 0));
				if (axis.lengthSqr() < 1.0E-8) {
					axis = new Vec3(1, 0, 0);
				}
			}
			axis = axis.normalize();
			Quaternionf rotation = new Quaternionf().fromAxisAngleRad((float) axis.x,
					(float) axis.y, (float) axis.z, (float) maxTurnRad);
			Vector3f rotated = rotation.transform(
					new Vector3f((float) currentDir.x, (float) currentDir.y,
							(float) currentDir.z));
			currentDir = new Vec3(rotated.x, rotated.y, rotated.z);
		} else {
			currentDir = targetDir; // 直接对准目标
		}
		this.setDeltaMovement(currentDir.scale(speed));
	}

	protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
		while (pTargetRotation - pCurrentRotation < -180.0F) {
			pCurrentRotation -= 360.0F;
		}

		while (pTargetRotation - pCurrentRotation >= 180.0F) {
			pCurrentRotation += 360.0F;
		}

		return Mth.lerp(1.0F, pCurrentRotation, pTargetRotation);
	}

	public void burst() {
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
		} else {
			this.cachedOwner = this.ownerUUID == null ?
					null :
					((ServerLevel) this.level()).getEntity(this.ownerUUID);
			this.entityData.set(OWNER_ID,
					cachedOwner == null ? -1 : cachedOwner.getId());
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
		} else {
			this.cachedLockedTarget = this.lockedTargetUUID == null ?
					null :
					((ServerLevel) this.level()).getEntity(
							this.lockedTargetUUID);
			this.entityData.set(LOCKED_TARGET_ID, cachedLockedTarget == null ?
					-1 :
					cachedLockedTarget.getId());
		}
		return this.cachedLockedTarget;
	}

	/**
	 * 双端均可用的获取锚定的实体
	 */
	@Nullable
	private Entity getAnchorEntity() {
		AnchorMode mode = this.getAnchorMode();
		if (mode == null)
			return null;

		return (mode == AnchorMode.TARGET) ? this.getLockedTarget() : this.getOwner();
	}

	// ============ EntityData 的 Set/Get 方法 ============
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

	public void setMaxAge(int maxAge) {
		this.entityData.set(MAX_AGE, maxAge);
	}

	public int getMaxAge() {
		return this.entityData.get(MAX_AGE);
	}

	public void setDelayShootTicks(int delayShootTicks) {
		this.entityData.set(DELAY_SHOOT_TICKS, delayShootTicks);
	}

	public int getDelayShootTicks() {
		return this.entityData.get(DELAY_SHOOT_TICKS);
	}

	public void setFired(boolean fired) {
		this.entityData.set(FIRED, fired);
	}

	public boolean isFired() {
		return this.entityData.get(FIRED);
	}

	public void setVelocity(float velocity) {
		this.entityData.set(VELOCITY, velocity);
	}

	public float getVelocity() {
		return this.entityData.get(VELOCITY);
	}

	public void setHasGravity(boolean hasGravity) {
		this.entityData.set(HAS_GRAVITY, hasGravity);
	}

	public boolean hasGravity() {
		return this.entityData.get(HAS_GRAVITY);
	}

	public void setGravity(float gravity) {
		this.entityData.set(GRAVITY, gravity);
	}

	@Override
	protected double getDefaultGravity() {
		return this.entityData.get(GRAVITY);
	}

	public void setFrictionAir(float frictionAir) {
		this.entityData.set(FRICTION_AIR, frictionAir);
	}

	public float getFrictionAir() {
		return this.entityData.get(FRICTION_AIR);
	}

	public void setFrictionWater(float frictionWater) {
		this.entityData.set(FRICTION_WATER, frictionWater);
	}

	public float getFrictionWater() {
		return this.entityData.get(FRICTION_WATER);
	}

	public void setHomingMode(HomingMode mode) {
		this.entityData.set(HOMING_MODE, mode.name());
	}

	public HomingMode getHomingMode() {
		String modeStr = this.entityData.get(HOMING_MODE);
		return modeStr.isEmpty() ? HomingMode.NONE : HomingMode.valueOf(modeStr);
	}

	public void setHomingTurnRate(float turnRate) {
		this.entityData.set(HOMING_TURN_RATE, turnRate);
	}

	public float getHomingTurnRate() {
		return this.entityData.get(HOMING_TURN_RATE);
	}

	public void setHomingSearchRadius(float radius) {
		this.entityData.set(HOMING_SEARCH_RADIUS, radius);
	}

	public float getHomingSearchRadius() {
		return this.entityData.get(HOMING_SEARCH_RADIUS);
	}

	public void setRenderType(String location) {
		this.entityData.set(RENDER_TYPE, location);
	}

	public String getRenderType() {
		return this.entityData.get(RENDER_TYPE);
	}

	// ============ 生成实体用的方法 ============


	public void setShootMovementDir(double x, double y, double z) {
		Vec3 direction = new Vec3(x, y, z);
		direction = direction.lengthSqr() > 1.0E-4D ?
				direction.normalize() :
				new Vec3(0.0D, 0.0D, 1.0D);

		this.setDeltaMovement(direction);
		this.hasImpulse = true;

		if (direction.horizontalDistanceSqr() > 1.0E-7D) {
			float yRot = (float) (Mth.atan2(direction.x,
					direction.z) * (double) Mth.RAD_TO_DEG);
			this.setYRot(yRot);
		}

		float xRot = (float) (Mth.atan2(direction.y,
				direction.horizontalDistance()) * (double) Mth.RAD_TO_DEG);
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
			if (!this.level()
					.hasNearbyAlivePlayer(this.getX(), this.getY(), this.getZ(),
							instantDespawnDistance)) {
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

		d0 *= (double) 256.0F * Entity.getViewScale();
		return distance < d0 * d0;
	}

	// 插值运动
	@Override
	public void lerpMotion(double x, double y, double z) {
		this.setDeltaMovement(x, y, z);
		// 防止第一帧同步跳帧
		if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
			double d0 = Math.sqrt(x * x + z * z);
			this.setXRot((float) (Mth.atan2(y,
					d0) * (double) (180F / (float) Math.PI)));
			this.setYRot((float) (Mth.atan2(x, z) * (double) (180F / (float) Math.PI)));
			this.xRotO = this.getXRot();
			this.yRotO = this.getYRot();
			this.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(),
					this.getXRot());
		}

	}

	// ============ 数据存储/同步 ============
	// entityData默认值
	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		// 初始值定义
		builder.define(OWNER_ID, -1);
		builder.define(LOCKED_TARGET_ID, -1);

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

		builder.define(HOMING_MODE, HomingMode.NONE.name());
		builder.define(HOMING_TURN_RATE, 5.0F);
		builder.define(HOMING_SEARCH_RADIUS, 32.0F);

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

		tag.putString("HomingMode", this.getHomingMode().name());
		tag.putFloat("HomingTurnRate", this.getHomingTurnRate());
		tag.putFloat("HomingSearchRadius", this.getHomingSearchRadius());

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

		if (tag.contains("HomingMode")) {
			try {
				this.setHomingMode(HomingMode.valueOf(tag.getString("HomingMode")));
			} catch (IllegalArgumentException e) {
				this.setHomingMode(HomingMode.NONE);
			}
		}
		if (tag.contains("HomingTurnRate")) {
			this.setHomingTurnRate(tag.getFloat("HomingTurnRate"));
		}
		if (tag.contains("HomingSearchRadius")) {
			this.setHomingSearchRadius(tag.getFloat("HomingSearchRadius"));
		}

		if (tag.contains("RenderType")) {
			this.setRenderType(tag.getString("RenderType"));
		}
	}
}
