package mod.abbyqaq.abcore.client.renderer.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import mods.flammpfeil.slashblade.SlashBlade;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

public interface ProjectileRenderBehavior {
	void render(GenericProjectile entity, float entityYaw, float partialTicks,
			PoseStack poseStack, MultiBufferSource buffer, int packedLight);

	// 可选：提供该投掷物的纹理
	default ResourceLocation getTexture(GenericProjectile entity) {
		return SlashBlade.prefix("model/util/ss.png");
	}
}
