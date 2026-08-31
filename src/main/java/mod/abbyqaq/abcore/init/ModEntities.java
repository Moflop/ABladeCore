package mod.abbyqaq.abcore.init;

import mod.abbyqaq.abcore.ABladeCoreMod;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class ModEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES =
			DeferredRegister.create(Registries.ENTITY_TYPE, ABladeCoreMod.MODID);

	public static final DeferredHolder<EntityType<?>, EntityType<GenericProjectile>> GENERIC_PROJECTILE = ENTITIES.register("generic_projectile", () ->
			EntityType.Builder.<GenericProjectile>of(GenericProjectile::new, MobCategory.MISC)
			// 设定基础默认尺寸 (稍后在实体内动态覆盖)
			.sized(0.25F, 0.25F)
			// 客户端追踪范围
			.clientTrackingRange(4)
			// 网络同步间隔 (10 tick 同步一次，原版投掷物标准)
			.updateInterval(10)
			.build("generic_projectile"));

	public static void register(IEventBus modEventBus) {
		ENTITIES.register(modEventBus);
	}
}
