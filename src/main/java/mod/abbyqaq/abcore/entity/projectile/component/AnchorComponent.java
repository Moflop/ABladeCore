package mod.abbyqaq.abcore.entity.projectile.component;

import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * 锚定组件
 *
 * @author Arcomit
 * @since 2026-09-07
 */
public class AnchorComponent implements IProjectileComponent{
	public enum AnchorMode {OWNER, TARGET}

	// 锚定偏移量
	private static final EntityDataAccessor<Vector3f> ANCHOR_OFFSET =
			SynchedEntityData.defineId(GenericProjectile.class,
					EntityDataSerializers.VECTOR3);

	public void setAnchorOffset(GenericProjectile projectile, @Nullable Vec3 offset) {
		if (offset == null) return;
		projectile.getEntityData().set(ANCHOR_OFFSET,
				new Vector3f((float) offset.x, (float) offset.y,
						(float) offset.z));
		Entity owner = projectile.getOwner();
		if (owner != null) {
			float yawRadians = (float) Math.toRadians(-owner.getYRot());
			Vec3 offsetPos = owner.position().add(offset.yRot(yawRadians));
			projectile.xo = offsetPos.x;
			projectile.yo = offsetPos.y;
			projectile.zo = offsetPos.z;
			projectile.setPos(offsetPos);

			projectile.startRiding(owner, true);
		}
	}
	public Vec3 getAnchorOffset(GenericProjectile projectile) {
		Vector3f vec = projectile.getEntityData().get(ANCHOR_OFFSET);
		return new Vec3(vec.x(), vec.y(), vec.z());
	}

	@Override
	public void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(ANCHOR_OFFSET, new Vector3f(0, 0, 0));
	}

	@Override
	public void writeNbt(GenericProjectile projectile, CompoundTag tag) {
		Vec3 offset = this.getAnchorOffset(projectile);
		CompoundTag anchorTag = new CompoundTag();
		anchorTag.putDouble("x", offset.x);
		anchorTag.putDouble("y", offset.y);
		anchorTag.putDouble("z", offset.z);
		tag.put("AnchorOffset", anchorTag);
	}

	@Override
	public void readNbt(GenericProjectile projectile, CompoundTag tag) {
		if (tag.contains("AnchorOffset", Tag.TAG_COMPOUND)) {
			CompoundTag anchorTag = tag.getCompound("AnchorOffset");
			this.setAnchorOffset(projectile,
					new Vec3(
							anchorTag.getDouble("x"),
							anchorTag.getDouble("y"),
							anchorTag.getDouble("z")
					)
			);
		}
	}
}
