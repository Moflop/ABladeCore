package mod.abbyqaq.abcore.utils;

import mods.flammpfeil.slashblade.capability.slashblade.BladeStateAccess;
import mods.flammpfeil.slashblade.item.ItemSlashBlade;
import mods.flammpfeil.slashblade.util.TargetSelector;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
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
	public static final TargetingConditions ATTACKABLE_TARGETING = new TargetSelector.SlashBladeTargetingConditions()
			.ignoreInvisibilityTesting().selector(new TargetSelector.AttackablePredicate());

	public static LivingEntity getLockedOrNearestLivingEntity(ItemStack blade, LivingEntity user) {
		if (!(blade.getItem() instanceof ItemSlashBlade)) {
			return null;
		}

		return BladeStateAccess.of(blade)
				.map(s -> s.getTargetEntity(user.level()))
				.filter(LivingEntity.class::isInstance)
				.map(entity -> (LivingEntity) entity)
				.orElseGet(() -> getCrosshairOrNearestLivingEntity(user));
	}

	public static LivingEntity getCrosshairOrNearestLivingEntity(LivingEntity source) {
		Predicate<Entity> validLivingFilter = entity ->
				entity instanceof LivingEntity livingEntity
						&& canSee(source, source.getEyePosition(), livingEntity)
						&& ATTACKABLE_TARGETING.test(source, livingEntity);

		Entity target = EntityFindUtils.getNearestAnyEntityToCrosshair(source, 32.0f, 10.0F, validLivingFilter);
		if (target != null) {
			return (LivingEntity) target;
		}

		Entity fallback = EntityFindUtils.getNearestAnyEntity(source, 32.0f, validLivingFilter);
		return fallback != null ? (LivingEntity) fallback : null;
	}

	public static LivingEntity getNearestLivingEntity(LivingEntity source) {
		return getNearestLivingEntity(source, 32.0f);
	}

	public static LivingEntity getNearestLivingEntity(LivingEntity source, float radius) {
		Entity result = EntityFindUtils.getNearestAnyEntity(source, radius, entity ->
				entity instanceof LivingEntity livingEntity
						&& canSee(source, source.getEyePosition(), livingEntity)
						&& ATTACKABLE_TARGETING.test(source, livingEntity));

		return result != null ? (LivingEntity) result : null;
	}

	public static boolean canSee(LivingEntity source, Vec3 sourceEyePos, Entity target) {
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
