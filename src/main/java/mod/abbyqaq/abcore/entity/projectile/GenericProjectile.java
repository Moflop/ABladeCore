package mod.abbyqaq.abcore.entity.projectile;

import mod.abbyqaq.abcore.init.ModEntities;
import mod.abbyqaq.abcore.init.ModProjectileTypes;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.item.Item;
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
	// 用于同步客户端渲染的外观和缩放
	private static final EntityDataAccessor<String> TYPE_ID = SynchedEntityData.defineId(GenericProjectile.class, EntityDataSerializers.STRING);
	private ResourceLocation cachedTypeLoc;
	private ProjectileType cachedType;

	public GenericProjectile(EntityType<? extends ThrowableProjectile> type, Level level) {
		super(type, level);
	}


	public GenericProjectile(Level level, LivingEntity shooter) {
		super(ModEntities.GENERIC_PROJECTILE.get(), shooter, level);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(TYPE_ID, ModProjectileTypes.DEFAULT_TYPE.getId().toString());
	}

	public void setProjectileType(ResourceLocation location) {
		this.entityData.set(TYPE_ID, location.toString());
		this.cachedTypeLoc = location;
		this.cachedType = null;// 清除缓存，强制下次获取时重新去原版注册表查找
		this.refreshDimensions();
	}

	public ProjectileType getProjectileType() {
		if (this.cachedType == null) {
			ResourceLocation typeLoc = ResourceLocation.parse(this.entityData.get(TYPE_ID));

			Registry<ProjectileType> registry = this.level().registryAccess()
					.registry(ModProjectileTypes.REGISTRY_KEY)
					.orElse(null);

			if (registry != null) {
				this.cachedType = registry.get(typeLoc);
			}

			if (this.cachedType == null) {
				this.cachedType = ModProjectileTypes.DEFAULT_TYPE.get();
			}
		}
		return this.cachedType;
	}

	public ResourceLocation getProjectileLocation() {
		if (this.cachedTypeLoc == null) {
			this.cachedTypeLoc = ResourceLocation.parse(this.entityData.get(TYPE_ID));
		}
		return this.cachedTypeLoc;
	}


	// 关键：重写重力，应用类型配置
	@Override
	protected double getDefaultGravity() {
		return getProjectileType().gravity;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide) {
			getProjectileType().onTick.accept(this);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult result) {
		super.onHitEntity(result);
		if (!this.level().isClientSide) {
			result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), getProjectileType().damage);
			getProjectileType().onHit.accept(this, result);
			this.discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult result) {
		super.onHitBlock(result);
		if (!this.level().isClientSide) {
			getProjectileType().onHit.accept(this, result);
			this.discard();
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putString("ProjectileType", this.entityData.get(TYPE_ID));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.entityData.set(TYPE_ID, tag.getString("ProjectileType"));
		this.cachedType = null;
	}
}
