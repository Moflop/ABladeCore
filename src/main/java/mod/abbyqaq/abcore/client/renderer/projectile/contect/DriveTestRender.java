package mod.abbyqaq.abcore.client.renderer.projectile.contect;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.abbyqaq.abcore.annotation.AutoRegisterProjectileRenderer;
import mod.abbyqaq.abcore.client.renderer.projectile.ProjectileRenderBehavior;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import mod.abbyqaq.abcore.init.ModProjectileRenderTypes;
import mods.flammpfeil.slashblade.SlashBlade;
import mods.flammpfeil.slashblade.client.renderer.model.BladeModelManager;
import mods.flammpfeil.slashblade.client.renderer.model.obj.WavefrontObject;
import mods.flammpfeil.slashblade.client.renderer.util.BladeRenderState;
import mods.flammpfeil.slashblade.client.renderer.util.MSAutoCloser;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-31
 */
@AutoRegisterProjectileRenderer(type = ModProjectileRenderTypes.DRIVE_TEST)
public class DriveTestRender implements ProjectileRenderBehavior {
	private static final ResourceLocation MODEL = SlashBlade.prefix("model/util/drive.obj");
	@Override
	public void render(GenericProjectile entity, float entityYaw, float partialTicks,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight) {

		try (MSAutoCloser ignored = MSAutoCloser.pushMatrix(poseStack)) {
			//			float lifetime = entity.getLifetime();
			//			double deathTime = lifetime;
			//			double baseAlpha = (Math.clamp(lifetime - (entity.tickCount), 0, deathTime) / deathTime);
			//			baseAlpha = Math.max(0, -Math.pow(baseAlpha - 1, 4.0) + 0.75);
			//
			//			Quaternionf renderRot = new Quaternionf();
			//			entity.prevRotation.slerp(entity.getSyncRotation(), partialTicks, renderRot);
			//
			//			poseStack.mulPose(renderRot);
			//
			//			float scale = 0.015f;
			//			poseStack.scale(scale, scale, scale);
			//			poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
			//			int color = entity.getRenderColor() & 0xFFFFFF;
			//			int alpha = ((0xFF & (int) (0xFF * baseAlpha)) << 24);
			//			WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
			//
			//			BladeRenderState.setCol(color | alpha);
			//			BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "base", this.getTexture(entity), poseStack, buffer,
			//					packedLight);

//			Quaternionf renderRot = new Quaternionf();
//			entity.prevRotation.slerp(entity.getSyncRotation(), partialTicks, renderRot);
//
//			poseStack.mulPose(renderRot);

			Quaternionf renderRot = new Quaternionf();
			entity.clientPrevRotation.slerp(entity.clientRotation, partialTicks, renderRot);
			poseStack.mulPose(renderRot);

			float scale = 0.015f;
			poseStack.scale(scale, scale, scale);
			WavefrontObject model = BladeModelManager.getInstance().getModel(MODEL);
			BladeRenderState.renderOverridedLuminous(ItemStack.EMPTY, model, "base", this.getTexture(entity), poseStack, buffer,
					packedLight);
		}

	}
}
