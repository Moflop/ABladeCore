package mod.abbyqaq.abcore.client.renderer.projectile;

import java.util.HashMap;
import java.util.Map;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
public class ProjectileRenderRegistry {

	private static final Map<String, ProjectileRenderBehavior> RENDERERS = new HashMap<>();

	public static void register(String type, ProjectileRenderBehavior behavior) {
		RENDERERS.put(type, behavior);
	}

	public static ProjectileRenderBehavior get(String type) {
		return RENDERERS.get(type);
	}
}
