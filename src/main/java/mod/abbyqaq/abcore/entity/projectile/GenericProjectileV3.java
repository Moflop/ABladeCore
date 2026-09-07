package mod.abbyqaq.abcore.entity.projectile;

import mod.abbyqaq.abcore.init.ModEntities;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-09-07
 */
public class GenericProjectileV3 extends Entity implements IEntityWithComplexSpawn {

	private static final EntityDataAccessor<Integer> OWNER_ID =
			SynchedEntityData.defineId(GenericProjectileV3.class, EntityDataSerializers.INT);

	public GenericProjectileV3(EntityType<? extends Entity> type, Level level) {
		super(type, level);
	}

	public GenericProjectileV3(Level level, LivingEntity shooter) {
		super(ModEntities.GENERIC_PROJECTILE.get(), level);
		this.setOwner(shooter);
	}
}
