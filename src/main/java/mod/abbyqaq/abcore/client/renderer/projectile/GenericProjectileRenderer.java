package mod.abbyqaq.abcore.client.renderer.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class GenericProjectileRenderer extends EntityRenderer<GenericProjectile> {

	public GenericProjectileRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(GenericProjectile entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
		ResourceLocation typeLoc = entity.getProjectileLocation();

		ProjectileRenderRegistry.RenderBehavior behavior = ProjectileRenderRegistry.get(typeLoc);

		if (behavior != null) {
			behavior.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
		}

		super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(GenericProjectile entity) {
		ResourceLocation typeLoc = entity.getProjectileLocation();

		ProjectileRenderRegistry.RenderBehavior behavior = ProjectileRenderRegistry.get(typeLoc);

		return behavior != null ? behavior.getTexture(entity) : ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/missingno.png");
	}
}
