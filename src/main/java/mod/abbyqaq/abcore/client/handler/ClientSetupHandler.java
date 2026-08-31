package mod.abbyqaq.abcore.client.handler;

import mod.abbyqaq.abcore.annotation.AutoRegisterProjectileRenderer;
import mod.abbyqaq.abcore.client.renderer.projectile.ProjectileRenderBehavior;
import mod.abbyqaq.abcore.client.renderer.projectile.ProjectileRenderRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
@EventBusSubscriber()
public class ClientSetupHandler {
	@SubscribeEvent
	public static void onClientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			Type annotationType = Type.getType(AutoRegisterProjectileRenderer.class);
			for (ModFileScanData scanData : ModList.get().getAllScanData()) {
				for (ModFileScanData.AnnotationData data : scanData.getAnnotations()) {

					// 如果找到了我们的注解
					if (data.annotationType().equals(annotationType)) {
						try {
							String className = data.clazz().getClassName();
							Class<?> clazz = Class.forName(className);

							// 检查是否实现了ProjectileRenderBehavior接口
							if (ProjectileRenderBehavior.class.isAssignableFrom(clazz)) {

								// 获取注解里的 id 参数
								String id = (String) data.annotationData().get("id");

								// 实例化对象并注册
								ProjectileRenderBehavior instance = (ProjectileRenderBehavior) clazz.getDeclaredConstructor().newInstance();
								ProjectileRenderRegistry.register(id, instance);
								if (!FMLEnvironment.production) {
									System.out.println("Auto-registered renderer: " + id + " -> " + className);
								}
							}else {
								throw new IllegalStateException(
										String.format("错误: 类 [%s] 使用了 @AutoRegisterProjectileRenderer 注解，但它并没有实现 ProjectileRenderBehavior 接口！", className)
								);
							}
						} catch (Exception e) {
							throw new RuntimeException("Failed to auto-register projectile renderer: " + data.clazz().getClassName(), e);
						}
					}
				}
			}
		});
	}
}
