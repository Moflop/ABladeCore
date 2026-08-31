package mod.abbyqaq.abcore.entity.projectile;

import lombok.Getter;
import lombok.Setter;
import mod.abbyqaq.abcore.ABladeCoreMod;
import mod.abbyqaq.abcore.init.ModEntities;
import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class GenericProjectile extends ThrowableProjectile {
	// 发射前的锚定模式
	public enum AnchorMode {NONE, OWNER, TARGET}


	// 发射前瞄准模式
	public enum AimMode {FIXED, OWNER_LOOK, TARGET_CENTER}


	// 追踪模式
	public enum HomingMode {NONE, LOCKED_TARGET, AUTO_NEAREST, CURSOR_GUIDED}


	// ===========无需服务端同步客户端的数据===========
	// ============只存NBT，用于服务端计算============
	@Getter
	@Setter
	//发射前的锚定模式
	private AnchorMode anchorMode = AnchorMode.NONE;
	@Getter
	@Setter
	// 发射前的索敌模式
	private AimMode aimMode = AimMode.FIXED;
	@Getter
	@Setter
	// 发射后的追踪模式
	private HomingMode homingMode = HomingMode.NONE;
	@Getter
	@Setter
	// 转向力度 (0.01~1.0) -> 决定它拐弯有多快。值越低弧线越优美，但也越容易被躲开。
	public float turnRate = 0.1f;
	@Getter
	@Setter
	// 扫描半径 -> AUTO_NEAREST 专用，多远能“看见”敌人。
	public float scanRadius = 16.0F;
	@Getter
	@Setter
	// 追踪延迟 -> 发射后直飞几 Tick 才开始追踪 (用于制作“散开然后再追踪”的导弹群效果)。
	public int startHomingDelay = 0;
	@Getter
	@Setter
	// 发射时的速度
	public float launch_velocity = 1.0F;
	// 重力
	@Setter
	public float gravity = 0.03F;
	@Getter
	@Setter
	// 空气阻力
	public float frictionAir = 0.99F;
	@Getter
	@Setter
	// 水中阻力
	public float frictionWater = 0.60F;
	@Getter
	@Setter
	// 伤害
	public float damage = 5.0F;
	@Getter
	@Setter
	// 攻击碰撞箱宽度
	public float hitboxWideth = 1.0F;
	@Getter
	@Setter
	// 攻击碰撞箱高度
	public float hitboxHeight = 1.0F;
	@Getter
	@Setter
	// 击退力
	public float knockback = 0.0F;
	@Getter
	@Setter
	// 穿透力
	public int pierceLevel = 0;
	@Getter
	@Setter
	// 是否反弹
	public boolean bounces = false;
	@Getter
	@Setter
	// 反弹系数 (0.0~1.0) -> 决定它反弹后保留多少动能。值越低反弹越快消失。
	public float bounceFactor = 0.5F;

	// 最大寿命（单位：Tick）
	private static final EntityDataAccessor<Integer> MAX_AGE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);
	// 延迟发射时间
	private static final EntityDataAccessor<Integer> DELAY_SHOOT_TICKS =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.INT);

	// 用于同步客户端渲染的外观和缩放
	// 渲染类型，用于确定实体的实际渲染
	private static final EntityDataAccessor<String> RENDER_TYPE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.STRING);
	// 渲染缩放，控制渲染的大小
	private static final EntityDataAccessor<Float> RENDER_SCALE =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.FLOAT);
	// 拖尾
	private static final EntityDataAccessor<Boolean> TRAIL =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.BOOLEAN);
	// 是否发光
	private static final EntityDataAccessor<Boolean> GLOWING =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.BOOLEAN);

	public GenericProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
		super(type, level);
	}

	public GenericProjectile(Level level, LivingEntity shooter) {
		super(ModEntities.GENERIC_PROJECTILE.get(), shooter, level);
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {

		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!this.level().isClientSide) {
			result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()),
					getDamage());
			this.discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (!this.level().isClientSide) {
			this.discard();
		}
	}

	@Override
	protected double getDefaultGravity() {
		return this.gravity;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DELAY_SHOOT_TICKS, 0);
		builder.define(RENDER_TYPE, ModProjectileRenderTypes.DEFAULT);
		builder.define(RENDER_SCALE, 1.0F);
		builder.define(TRAIL, false);
		builder.define(GLOWING, false);
		builder.define(MAX_AGE, 1200);
	}

	public int getDelayShootTicks() {
		return this.entityData.get(DELAY_SHOOT_TICKS);
	}

	public void setDelayShootTicks(int delayShootTicks) {
		this.entityData.set(DELAY_SHOOT_TICKS, delayShootTicks);
	}

	public GenericProjectile setRenderType(String location) {
		this.entityData.set(RENDER_TYPE, location);
		return this;
	}

	public String getRenderType() {
		return this.entityData.get(RENDER_TYPE);
	}

	public GenericProjectile setRenderScale(float renderScale) {
		this.entityData.set(RENDER_SCALE, renderScale);
		return this;
	}

	public float getRenderScale() {
		return this.entityData.get(RENDER_SCALE);
	}

	public boolean isTrail() {
		return this.entityData.get(TRAIL);
	}

	public void setTrail(boolean trail) {
		this.entityData.set(TRAIL, trail);
	}

	public boolean isGlowing() {
		return this.entityData.get(GLOWING);
	}

	public void setGlowing(boolean glowing) {
		this.entityData.set(GLOWING, glowing);
	}

	public int getMaxAge() {
		return this.entityData.get(MAX_AGE);
	}

	public void setMaxAge(int maxAge) {
		this.entityData.set(MAX_AGE, maxAge);
	}

	private static <E extends Enum<E>> void writeEnum(CompoundTag tag, String key, E value) {
		if (value != null) {
			tag.putString(key, value.name());
		}
	}

	private static <E extends Enum<E>> E readEnum(CompoundTag tag, String key, E fallback) {
		if (!tag.contains(key)) {
			return fallback;
		}
		try {
			return Enum.valueOf(fallback.getDeclaringClass(), tag.getString(key));
		} catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);

		writeEnum(tag, "AnchorMode", this.anchorMode);
		writeEnum(tag, "AimMode", this.aimMode);
		writeEnum(tag, "HomingMode", this.homingMode);
		tag.putFloat("TurnRate", this.turnRate);
		tag.putFloat("ScanRadius", this.scanRadius);
		tag.putInt("StartHomingDelay", this.startHomingDelay);
		tag.putInt("StartDelay", this.startHomingDelay);
		tag.putFloat("LaunchVelocity", this.launch_velocity);
		tag.putFloat("Gravity", this.gravity);
		tag.putFloat("FrictionAir", this.frictionAir);
		tag.putFloat("FrictionWater", this.frictionWater);
		tag.putFloat("Damage", this.damage);
		tag.putFloat("HitboxWidth", this.hitboxWideth);
		tag.putFloat("HitboxHeight", this.hitboxHeight);
		tag.putFloat("Knockback", this.knockback);
		tag.putInt("PierceLevel", this.pierceLevel);
		tag.putBoolean("Bounces", this.bounces);
		tag.putFloat("BounceFactor", this.bounceFactor);
		tag.putInt("MaxAge", this.entityData.get(MAX_AGE));
		tag.putInt("DelayShootTicks", this.entityData.get(DELAY_SHOOT_TICKS));
		tag.putString("RenderType", this.entityData.get(RENDER_TYPE));
		tag.putFloat("RenderScale", this.entityData.get(RENDER_SCALE));
		tag.putBoolean("Trail", this.entityData.get(TRAIL));
		tag.putBoolean("Glowing", this.entityData.get(GLOWING));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);

		this.anchorMode = readEnum(tag, "AnchorMode", AnchorMode.NONE);
		this.aimMode = readEnum(tag, "AimMode", AimMode.FIXED);
		this.homingMode = readEnum(tag, "HomingMode", HomingMode.NONE);
		if (tag.contains("TurnRate"))
			this.turnRate = tag.getFloat("TurnRate");
		if (tag.contains("ScanRadius"))
			this.scanRadius = tag.getFloat("ScanRadius");
		if (tag.contains("StartHomingDelay")) {
			this.startHomingDelay = tag.getInt("StartHomingDelay");
		} else if (tag.contains("StartDelay")) {
			this.startHomingDelay = tag.getInt("StartDelay");
		}
		if (tag.contains("LaunchVelocity"))
			this.launch_velocity = tag.getFloat("LaunchVelocity");
		if (tag.contains("Gravity"))
			this.gravity = tag.getFloat("Gravity");
		if (tag.contains("FrictionAir"))
			this.frictionAir = tag.getFloat("FrictionAir");
		if (tag.contains("FrictionWater"))
			this.frictionWater = tag.getFloat("FrictionWater");
		if (tag.contains("Damage"))
			this.damage = tag.getFloat("Damage");
		if (tag.contains("HitboxWidth"))
			this.hitboxWideth = tag.getFloat("HitboxWidth");
		if (tag.contains("HitboxHeight"))
			this.hitboxHeight = tag.getFloat("HitboxHeight");
		if (tag.contains("Knockback"))
			this.knockback = tag.getFloat("Knockback");
		if (tag.contains("PierceLevel"))
			this.pierceLevel = tag.getInt("PierceLevel");
		if (tag.contains("Bounces"))
			this.bounces = tag.getBoolean("Bounces");
		if (tag.contains("BounceFactor"))
			this.bounceFactor = tag.getFloat("BounceFactor");
		if (tag.contains("MaxAge"))
			this.entityData.set(MAX_AGE, tag.getInt("MaxAge"));
		if (tag.contains("DelayShootTicks"))
			this.entityData.set(DELAY_SHOOT_TICKS, tag.getInt("DelayShootTicks"));
		if (tag.contains("RenderType"))
			this.entityData.set(RENDER_TYPE, tag.getString("RenderType"));
		if (tag.contains("RenderScale"))
			this.entityData.set(RENDER_SCALE, tag.getFloat("RenderScale"));
		if (tag.contains("Trail"))
			this.entityData.set(TRAIL, tag.getBoolean("Trail"));
		if (tag.contains("Glowing"))
			this.entityData.set(GLOWING, tag.getBoolean("Glowing"));
	}
}
