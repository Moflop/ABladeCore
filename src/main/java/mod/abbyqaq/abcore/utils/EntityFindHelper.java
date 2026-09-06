package mod.abbyqaq.abcore.utils;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.registry.ComboStateRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-09-06
 */
public class EntityFindHelper {

	public static Entity getSlashBladeLockedEntity(ItemStack blade, LivingEntity user) {
		if (!(blade.getItem() instanceof ItemSlashBlade)) {
			return null;
		}

		return BladeStateAccess.of(blade)
				.map(s -> s.getTargetEntity(user.level()))
				.filter(LivingEntity.class::isInstance)
				.orElseGet(() -> getNearestLivingEntity(user));
	}

	public static Entity getNearestLivingEntity(LivingEntity source) {
		Predicate<Entity> validLivingFilter = entity ->
				entity instanceof LivingEntity && hasLineOfSight(source, source.getEyePosition(), entity);

		Entity target = EntityFindUtils.getNearestAnyEntityToCrosshair(source, 32.0f, 10.0F, validLivingFilter);
		return target != null ? target : EntityFindUtils.getNearestAnyEntity(source, 32.0f, validLivingFilter);
	}

	private static boolean hasLineOfSight(LivingEntity source, Vec3 sourceEyePos, Entity target) {
		Vec3 entityPos = target.getBoundingBox().getCenter();
		ClipContext context = new ClipContext(
				sourceEyePos, entityPos,
				ClipContext.Block.VISUAL,
				ClipContext.Fluid.NONE,
				source
		);

		return source.level().clip(context).getType() != HitResult.Type.BLOCK;
	}
}
