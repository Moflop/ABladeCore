//package mod.abbyqaq.abcore.entity.projectile;
//
//import lombok.Getter;
//import lombok.Setter;
//import mod.abbyqaq.abcore.init.ModEntities;
//import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.nbt.Tag;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.server.level.ServerLevel;
//import net.minecraft.world.entity.Entity;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.LivingEntity;
//import net.minecraft.world.entity.projectile.Projectile;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.phys.BlockHitResult;
//import net.minecraft.world.phys.EntityHitResult;
//import net.minecraft.world.phys.Vec3;
//import org.joml.Quaternionf;
//
//import javax.annotation.Nullable;
//import java.util.UUID;
//
///**
// * TODO：描述
// *
// * @author Arcomit
// * @since 2026-08-29
// */
//public class GenericProjectileV2 extends Projectile {
//	// 发射前的锚定模式
//	public enum AnchorMode {NONE, OWNER, TARGET}
//
//
//	// 发射前瞄准模式
//	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}
//
//
//	// 追踪模式
//	public enum HomingMode {NONE, LOCKED_TARGET, AUTO_NEAREST, CURSOR_GUIDED}
//
//
//	// ===========无需服务端同步客户端的数据===========
//	// ============只存NBT，用于服务端计算============
//	@Getter
//	//发射前的锚定模式
//	private AnchorMode anchorMode = AnchorMode.NONE;
//	@Getter
//	//锚定偏移量 (用于 OWNER 和 TARGET 模式)，todo：NBT存储
//	private Vec3 anchorOffset = Vec3.ZERO;
//
//	@Nullable
//	private UUID lockedTargetUUID;
//	@Nullable
//	private Entity cachedLockedTarget;
//
//	@Getter
//	@Setter
//	// 发射前的索敌模式
//	private AimMode aimMode = AimMode.FIXED;
//	@Getter
//	@Setter
//	// 发射后的追踪模式
//	private HomingMode homingMode = HomingMode.NONE;
//	@Getter
//	@Setter
//	// 转向力度 (0.01~1.0) -> 决定它拐弯有多快。值越低弧线越优美，但也越容易被躲开。
//	public float turnRate = 0.1f;
//	@Getter
//	@Setter
//	// 扫描半径 -> AUTO_NEAREST 专用，多远能“看见”敌人。
//	public float scanRadius = 16.0F;
//	@Getter
//	@Setter
//	// 追踪延迟 -> 发射后直飞几 Tick 才开始追踪 (用于制作“散开然后再追踪”的导弹群效果)。
//	public int startHomingDelay = 0;
//	@Getter
//	@Setter
//	// 发射时的速度
//	public float launch_velocity = 1.0F;
//	// 重力
//	@Setter
//	public float gravity = 0.03F;
//	@Getter
//	@Setter
//	// 空气阻力
//	public float frictionAir = 0.99F;
//	@Getter
//	@Setter
//	// 水中阻力
//	public float frictionWater = 0.60F;
//	@Getter
//	@Setter
//	// 伤害
//	public float damage = 5.0F;
//	@Getter
//	@Setter
//	// 攻击碰撞箱宽度
//	public float hitboxWideth = 1.0F;
//	@Getter
//	@Setter
//	// 攻击碰撞箱高度
//	public float hitboxHeight = 1.0F;
//	@Getter
//	@Setter
//	// 击退力
//	public float knockback = 0.0F;
//	@Getter
//	@Setter
//	// 穿透力
//	public int pierceLevel = 0;
//	@Getter
//	@Setter
//	// 是否反弹
//	public boolean bounces = false;
//	@Getter
//	@Setter
//	// 反弹系数 (0.0~1.0) -> 决定它反弹后保留多少动能。值越低反弹越快消失。
//	public float bounceFactor = 0.5F;
//
//	public Quaternionf prevRotation = new Quaternionf();
//
//	// 存活时间（单位：Tick）
//	private static final EntityDataAccessor<Float> LIFETIME =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.FLOAT);
//	// 最大寿命（单位：Tick）
//	private static final EntityDataAccessor<Integer> MAX_AGE =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.INT);
//	// 延迟发射时间
//	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.INT);
//
//	private static final EntityDataAccessor<Quaternionf> ROTATION =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.QUATERNION);
//
//	// 用于同步客户端渲染的外观和缩放
//	// 渲染类型，用于确定实体的实际渲染
//	private static final EntityDataAccessor<String> RENDER_TYPE =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.STRING);
//	// 渲染缩放，控制渲染的大小
//	private static final EntityDataAccessor<Float> RENDER_SCALE =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.FLOAT);
//	// 颜色
//	private static final EntityDataAccessor<Integer> RENDER_COLOR =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.INT);
//	// 拖尾
//	private static final EntityDataAccessor<Boolean> TRAIL =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.BOOLEAN);
//	// 是否发光
//	private static final EntityDataAccessor<Boolean> GLOWING =
//			SynchedEntityData.defineId(GenericProjectileV2.class,
//					EntityDataSerializers.BOOLEAN);
//
//	public GenericProjectileV2(EntityType<? extends Projectile> type, Level level) {
//		super(type, level);
//	}
//
//	public GenericProjectileV2(Level level, LivingEntity shooter) {
//		super(ModEntities.GENERIC_PROJECTILE.get(), level);
//		this.setOwner(shooter);
//	}
//
//	public void setCachedLockedTarget(@Nullable Entity cachedLockedTarget) {
//		if (cachedLockedTarget != null) {
//			this.lockedTargetUUID = cachedLockedTarget.getUUID();
//			this.cachedLockedTarget = cachedLockedTarget;
//		}
//	}
//
//	@Nullable
//	public Entity getCachedLockedTarget() {
//		if (this.cachedLockedTarget != null && !this.cachedLockedTarget.isRemoved()) {
//			return this.cachedLockedTarget;
//		} else {
//			if (this.lockedTargetUUID != null) {
//				Level level = this.level();
//				if (level instanceof ServerLevel) {
//					ServerLevel serverlevel = (ServerLevel)level;
//					this.cachedLockedTarget = serverlevel.getEntity(this.lockedTargetUUID);
//					return this.cachedLockedTarget;
//				}
//			}
//
//			return null;
//		}
//	}
//
//	// 设置初始化锚定模式和偏移量
//	public void setStartAnchorModeAndOffset(AnchorMode anchorMode, @Nullable Vec3 anchorOffset) {
//		if (anchorOffset != null) {
//			this.anchorOffset = anchorOffset;
//		}
//
//		Vec3 basePosition;
//		float yaw;
//
//		if (anchorMode == AnchorMode.TARGET && this.getCachedLockedTarget() != null) {
//			this.anchorMode = anchorMode;
//			basePosition = this.getCachedLockedTarget().position();
//			yaw = this.getCachedLockedTarget().getYRot();
//		} else {
//			if (anchorMode == AnchorMode.OWNER) {
//				this.anchorMode = anchorMode;
//			}
//			basePosition = this.getOwner().position();
//			yaw = this.getOwner().getYRot();
//		}
//
//		float yawRadians = (float) Math.toRadians(-yaw);
//
//		Vec3 rotatedOffset = this.anchorOffset.yRot(yawRadians);
//
//		this.setPos(basePosition.add(rotatedOffset));
//	}
//
//
//	private void handleAnchorMode() {
//		Entity anchor = (this.anchorMode == AnchorMode.TARGET) ? this.getCachedLockedTarget() :
//				(this.anchorMode == AnchorMode.OWNER) ? this.getOwner() : null;
//
//		if (anchor != null) {
//			float yawRadians = (float) Math.toRadians(-anchor.getYRot());
//			Vec3 rotatedOffset = this.anchorOffset.yRot(yawRadians);
//			Vec3 targetPos = anchor.position().add(rotatedOffset);
//
//			// 1. 设置当前位置
//			this.setPos(targetPos);
//
//			// 2. 同步旧位置，防止客户端进行无意义的平滑插值导致视觉抽搐
//			this.xo = targetPos.x;
//			this.yo = targetPos.y;
//			this.zo = targetPos.z;
//			this.xOld = targetPos.x; // 兼容不同映射版本的名称
//			this.yOld = targetPos.y;
//			this.zOld = targetPos.z;
//		}
//	}
//
//	@Override
//	public void tick() {
//		super.tick();
//
//		int delay = getDelayShootTicks();
//		if (delay > 0) {
//			// 服务端负责扣减倒计时
//			if (!this.level().isClientSide) {
//				setDelayShootTicks(delay - 1);
//				if (delay - 1 == 0) {
//					// 延迟结束，恢复重力设定 (仅在服务端执行，之后会自动同步到客户端)
//					this.setNoGravity(this.gravity <= 0.0F);
//					// launch();
//				}
//			}
//
//			// 【关键】客户端和服务端双端都需要执行以下逻辑
//			this.setNoGravity(true);
//			this.setDeltaMovement(Vec3.ZERO);
//
//			// 双端执行锚定，让客户端自己算位置，不再单纯等服务端发包
//			handleAnchorMode();
//
//			// 延迟期间仅同步位置，不执行后续的飞行和碰撞逻辑
//			return;
//		}
//	}
//
//	@Override
//	protected void onHitEntity(EntityHitResult result) {
//		super.onHitEntity(result);
//		if (!this.level().isClientSide) {
//			result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()),
//					getDamage());
//			this.discard();
//		}
//	}
//
//	@Override
//	protected void onHitBlock(BlockHitResult result) {
//		super.onHitBlock(result);
//		if (!this.level().isClientSide) {
//			this.discard();
//		}
//	}
//
//	@Override
//	protected double getDefaultGravity() {
//		return this.gravity;
//	}
//
//	@Override
//	protected void defineSynchedData(SynchedEntityData.Builder builder) {
//		builder.define(LIFETIME, 0.0F);
//		builder.define(MAX_AGE, 1200);
//		builder.define(DELAY_SHOOT_TICKS, 0);
//		builder.define(ROTATION, new Quaternionf());
//		builder.define(RENDER_TYPE, ModProjectileRenderTypes.DRIVE_TEST);
//		builder.define(RENDER_SCALE, 1.0F);
//		builder.define(RENDER_COLOR, 0xFFFFFF);
//		builder.define(TRAIL, false);
//		builder.define(GLOWING, false);
//	}
//
//	public float getLifetime() {
//		return this.entityData.get(LIFETIME);
//	}
//
//	public void setLifetime(float lifetime) {
//		this.entityData.set(LIFETIME, lifetime);
//	}
//
//	public int getDelayShootTicks() {
//		return this.entityData.get(DELAY_SHOOT_TICKS);
//	}
//
//	public void setDelayShootTicks(int delayShootTicks) {
//		this.entityData.set(DELAY_SHOOT_TICKS, delayShootTicks);
//	}
//
//	public void setSyncRotation(Quaternionf quat) {
//		this.entityData.set(ROTATION, quat);
//	}
//
//	public Quaternionf getSyncRotation() {
//		return this.entityData.get(ROTATION);
//	}
//
//	public GenericProjectileV2 setRenderType(String location) {
//		this.entityData.set(RENDER_TYPE, location);
//		return this;
//	}
//
//	public String getRenderType() {
//		return this.entityData.get(RENDER_TYPE);
//	}
//
//	public GenericProjectileV2 setRenderScale(float renderScale) {
//		this.entityData.set(RENDER_SCALE, renderScale);
//		return this;
//	}
//
//	public float getRenderScale() {
//		return this.entityData.get(RENDER_SCALE);
//	}
//
//	public int getRenderColor() {
//		return this.entityData.get(RENDER_COLOR);
//	}
//
//	public GenericProjectileV2 setRenderColor(int renderColor) {
//		this.entityData.set(RENDER_COLOR, renderColor);
//		return this;
//	}
//
//	public boolean isTrail() {
//		return this.entityData.get(TRAIL);
//	}
//
//	public void setTrail(boolean trail) {
//		this.entityData.set(TRAIL, trail);
//	}
//
//	public boolean isGlowing() {
//		return this.entityData.get(GLOWING);
//	}
//
//	public void setGlowing(boolean glowing) {
//		this.entityData.set(GLOWING, glowing);
//	}
//
//	public int getMaxAge() {
//		return this.entityData.get(MAX_AGE);
//	}
//
//	public void setMaxAge(int maxAge) {
//		this.entityData.set(MAX_AGE, maxAge);
//	}
//
//	private static <E extends Enum<E>> void writeEnum(CompoundTag tag, String key, E value) {
//		if (value != null) {
//			tag.putString(key, value.name());
//		}
//	}
//
//	private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, E fallback) {
//		if (!tag.contains(key)) {
//			return fallback;
//		}
//		try {
//			return Enum.valueOf(fallback.getDeclaringClass(), tag.getString(key));
//		} catch (IllegalArgumentException e) {
//			return fallback;
//		}
//	}
//
//	@Override
//	public void addAdditionalSaveData(CompoundTag tag) {
//		super.addAdditionalSaveData(tag);
//
//		writeEnum(tag, "AnchorMode", this.anchorMode);
//		if (this.lockedTargetUUID != null) {
//			tag.putUUID("LockedTarget", this.lockedTargetUUID);
//		}
//		writeEnum(tag, "AimMode", this.aimMode);
//		writeEnum(tag, "HomingMode", this.homingMode);
//		tag.putFloat("TurnRate", this.turnRate);
//		tag.putFloat("ScanRadius", this.scanRadius);
//		tag.putInt("StartHomingDelay", this.startHomingDelay);
//		tag.putInt("StartDelay", this.startHomingDelay);
//		tag.putFloat("LaunchVelocity", this.launch_velocity);
//		tag.putFloat("Gravity", this.gravity);
//		tag.putFloat("FrictionAir", this.frictionAir);
//		tag.putFloat("FrictionWater", this.frictionWater);
//		tag.putFloat("Damage", this.damage);
//		tag.putFloat("HitboxWidth", this.hitboxWideth);
//		tag.putFloat("HitboxHeight", this.hitboxHeight);
//		tag.putFloat("Knockback", this.knockback);
//		tag.putInt("PierceLevel", this.pierceLevel);
//		tag.putBoolean("Bounces", this.bounces);
//		tag.putFloat("BounceFactor", this.bounceFactor);
//		tag.putFloat("Lifetime", this.entityData.get(LIFETIME));
//		tag.putInt("MaxAge", this.entityData.get(MAX_AGE));
//		tag.putInt("DelayShootTicks", this.entityData.get(DELAY_SHOOT_TICKS));
//
//		Quaternionf quat = this.entityData.get(ROTATION);
//		CompoundTag rotTag = new CompoundTag();
//		rotTag.putFloat("x", quat.x());
//		rotTag.putFloat("y", quat.y());
//		rotTag.putFloat("z", quat.z());
//		rotTag.putFloat("w", quat.w());
//		tag.put("ProjectileRotation", rotTag);
//
//		// 保存锚点偏移量（如果存在）
//		CompoundTag anchorTag = new CompoundTag();
//		anchorTag.putDouble("x", this.anchorOffset.x);
//		anchorTag.putDouble("y", this.anchorOffset.y);
//		anchorTag.putDouble("z", this.anchorOffset.z);
//		tag.put("AnchorOffset", anchorTag);
//
//		tag.putString("RenderType", this.entityData.get(RENDER_TYPE));
//		tag.putFloat("RenderScale", this.entityData.get(RENDER_SCALE));
//		tag.putInt("RenderColor", this.entityData.get(RENDER_COLOR));
//		tag.putBoolean("Trail", this.entityData.get(TRAIL));
//		tag.putBoolean("Glowing", this.entityData.get(GLOWING));
//	}
//
//	@Override
//	public void readAdditionalSaveData(CompoundTag tag) {
//		super.readAdditionalSaveData(tag);
//
//		this.anchorMode = readEnum(tag, "AnchorMode", AnchorMode.NONE);
//		if (tag.hasUUID("LockedTarget")) {
//			this.lockedTargetUUID = tag.getUUID("LockedTarget");
//			this.cachedLockedTarget = null;
//		}
//		this.aimMode = readEnum(tag, "AimMode", AimMode.FIXED);
//		this.homingMode = readEnum(tag, "HomingMode", HomingMode.NONE);
//		if (tag.contains("TurnRate"))
//			this.turnRate = tag.getFloat("TurnRate");
//		if (tag.contains("ScanRadius"))
//			this.scanRadius = tag.getFloat("ScanRadius");
//		if (tag.contains("StartHomingDelay")) {
//			this.startHomingDelay = tag.getInt("StartHomingDelay");
//		} else if (tag.contains("StartDelay")) {
//			this.startHomingDelay = tag.getInt("StartDelay");
//		}
//		if (tag.contains("LaunchVelocity"))
//			this.launch_velocity = tag.getFloat("LaunchVelocity");
//		if (tag.contains("Gravity"))
//			this.gravity = tag.getFloat("Gravity");
//		if (tag.contains("FrictionAir"))
//			this.frictionAir = tag.getFloat("FrictionAir");
//		if (tag.contains("FrictionWater"))
//			this.frictionWater = tag.getFloat("FrictionWater");
//		if (tag.contains("Damage"))
//			this.damage = tag.getFloat("Damage");
//		if (tag.contains("HitboxWidth"))
//			this.hitboxWideth = tag.getFloat("HitboxWidth");
//		if (tag.contains("HitboxHeight"))
//			this.hitboxHeight = tag.getFloat("HitboxHeight");
//		if (tag.contains("Knockback"))
//			this.knockback = tag.getFloat("Knockback");
//		if (tag.contains("PierceLevel"))
//			this.pierceLevel = tag.getInt("PierceLevel");
//		if (tag.contains("Bounces"))
//			this.bounces = tag.getBoolean("Bounces");
//		if (tag.contains("BounceFactor"))
//			this.bounceFactor = tag.getFloat("BounceFactor");
//		if (tag.contains("Lifetime"))
//			this.entityData.set(LIFETIME, tag.getFloat("Lifetime"));
//		if (tag.contains("MaxAge"))
//			this.entityData.set(MAX_AGE, tag.getInt("MaxAge"));
//		if (tag.contains("DelayShootTicks"))
//			this.entityData.set(DELAY_SHOOT_TICKS, tag.getInt("DelayShootTicks"));
//
//		if (tag.contains("ProjectileRotation", Tag.TAG_COMPOUND)) {
//			CompoundTag rotTag = tag.getCompound("ProjectileRotation");
//
//			Quaternionf savedQuat = new Quaternionf(
//					rotTag.getFloat("x"),
//					rotTag.getFloat("y"),
//					rotTag.getFloat("z"),
//					rotTag.getFloat("w")
//			);
//
//			this.entityData.set(ROTATION, savedQuat);
//
//			if (this.prevRotation != null) {
//				this.prevRotation.set(savedQuat);
//			}
//		}
//
//		// 读取保存的锚点偏移量
//		if (tag.contains("AnchorOffset", Tag.TAG_COMPOUND)) {
//			CompoundTag anchorTag = tag.getCompound("AnchorOffset");
//			this.anchorOffset = new Vec3(anchorTag.getDouble("x"), anchorTag.getDouble("y"), anchorTag.getDouble("z"));
//		}
//
//		if (tag.contains("RenderType"))
//			this.entityData.set(RENDER_TYPE, tag.getString("RenderType"));
//		if (tag.contains("RenderScale"))
//			this.entityData.set(RENDER_SCALE, tag.getFloat("RenderScale"));
//		if (tag.contains("RenderColor"))
//			this.entityData.set(RENDER_COLOR, tag.getInt("RenderColor"));
//		if (tag.contains("Trail"))
//			this.entityData.set(TRAIL, tag.getBoolean("Trail"));
//		if (tag.contains("Glowing"))
//			this.entityData.set(GLOWING, tag.getBoolean("Glowing"));
//	}
//}
