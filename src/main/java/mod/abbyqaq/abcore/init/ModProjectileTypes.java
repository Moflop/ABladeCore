package mod.abbyqaq.abcore.init;

import mod.abbyqaq.abcore.ABladeCoreMod;
import mod.abbyqaq.abcore.entity.projectile.ProjectileType;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class ModProjectileTypes {
	// 注册表的唯一标识符 (RegistryKey)
	public static final ResourceKey<Registry<ProjectileType>> REGISTRY_KEY =
			ResourceKey.createRegistryKey(ABladeCoreMod.prefix("projectile_types"));

	public static final DeferredRegister<ProjectileType> PROJECTILE_TYPES =
			DeferredRegister.create(new RegistryBuilder<>(REGISTRY_KEY).sync(false).create(), "mymod");

	public static final DeferredHolder<ProjectileType, ProjectileType> DEFAULT_TYPE =
			PROJECTILE_TYPES.register("default", () -> new ProjectileType.Builder().build());


	public static void register(IEventBus modEventBus) {
		PROJECTILE_TYPES.register(modEventBus);
	}
}
