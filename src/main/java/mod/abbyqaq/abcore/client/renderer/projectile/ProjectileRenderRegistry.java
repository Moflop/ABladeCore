package mod.abbyqaq.abcore.client.renderer.projectile;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class ProjectileRenderRegistry {

	// 渲染行为接口
	public interface RenderBehavior {
		void render(GenericProjectile entity, float entityYaw, float partialTicks,
				PoseStack poseStack, MultiBufferSource buffer, int packedLight);

		// 可选：提供该投掷物的纹理
		default ResourceLocation getTexture(GenericProjectile entity) {
			return ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
		}
	}

	private static final Map<ResourceLocation, RenderBehavior> RENDERERS = new HashMap<>();

	public static void register(ResourceLocation typeId, RenderBehavior behavior) {
		RENDERERS.put(typeId, behavior);
	}

	public static RenderBehavior get(ResourceLocation typeId) {
		return RENDERERS.get(typeId);
	}
}
