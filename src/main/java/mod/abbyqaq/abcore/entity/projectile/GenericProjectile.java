package mod.abbyqaq.abcore.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class GenericProjectile extends ThrowableItemProjectile {

	public GenericProjectile(EntityType<? extends ThrowableItemProjectile> p_37442_,
			Level p_37443_) {
		super(p_37442_, p_37443_);
	}

	@Override
	protected Item getDefaultItem() {
		return null;
	}
}
