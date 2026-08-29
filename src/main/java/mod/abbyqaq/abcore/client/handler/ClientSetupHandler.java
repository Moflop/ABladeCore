package mod.abbyqaq.abcore.client.handler;

import mod.abbyqaq.abcore.client.renderer.projectile.GenericProjectileRenderer;
import mod.abbyqaq.abcore.init.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
@EventBusSubscriber()
public class ClientSetupHandler {

	@SubscribeEvent
	public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntities.GENERIC_PROJECTILE.get(), GenericProjectileRenderer::new);


	}
}
