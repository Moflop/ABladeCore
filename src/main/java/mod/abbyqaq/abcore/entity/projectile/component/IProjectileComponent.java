package mod.abbyqaq.abcore.entity.projectile.component;

import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;

public interface IProjectileComponent {
	/**
	 * 注册实体同步数据 (在实体 defineSynchedData 中调用)
	 */
	default void defineSynchedData(SynchedEntityData.Builder builder) {}

	/**
	 * 乘骑逻辑 Tick 更新（锚定用）
	 */
	default void rideTick(GenericProjectile projectile) {}

	/**
	 * 逻辑 Tick 更新
	 */
	default void tick(GenericProjectile projectile) {}

	/**
	 * 保存数据到 NBT
	 */
	default void writeNbt(GenericProjectile projectile, CompoundTag tag) {}

	/**
	 * 从 NBT 读取数据
	 */
	default void readNbt(GenericProjectile projectile, CompoundTag tag) {}
}
