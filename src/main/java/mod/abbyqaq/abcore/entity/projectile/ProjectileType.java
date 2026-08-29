package mod.abbyqaq.abcore.entity.projectile;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-29
 */
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ProjectileType {
	public final Physics physics;
	public final Collision collision;
	public final Payload payload;
	public final Visuals visuals;
	public final Lifecycle lifecycle;

	private ProjectileType(Physics physics, Collision collision, Payload payload, Visuals visuals, Lifecycle lifecycle) {
		this.physics = physics;
		this.collision = collision;
		this.payload = payload;
		this.visuals = visuals;
		this.lifecycle = lifecycle;
	}

	// ==========================================
	// 模块 1：物理与运动
	// ==========================================
	public static class Physics {
		public final float gravity;             // 每 Tick 的重力下坠值
		public final float frictionAir;         // 空气阻力系数
		public final float frictionWater;       // 水中阻力系数

		private Physics(float gravity, float frictionAir, float frictionWater) {
			this.gravity = gravity; this.frictionAir = frictionAir; this.frictionWater = frictionWater;
		}
		public static class Builder {
			private float gravity = 0.03f, frictionAir = 0.99f, frictionWater = 0.60f;
			public Builder gravity(float g) { this.gravity = g; return this; }
			public Builder friction(float air, float water) { this.frictionAir = air; this.frictionWater = water; return this; }
			Physics build() { return new Physics(gravity, frictionAir, frictionWater); }
		}
	}

	// ==========================================
	// 模块 2：碰撞与判定
	// ==========================================
	public static class Collision {
		public final float width, height;       // 碰撞箱尺寸
		public final int pierceLevel;           // 穿透力
		public final boolean bounces;           // 是否会在方块上反弹
		public final float bounceFactor;        // 每次反弹保留的动能比例

		private Collision(float width, float height, int pierceLevel, boolean bounces, float bounceFactor) {
			this.width = width; this.height = height; this.pierceLevel = pierceLevel;
			this.bounces = bounces; this.bounceFactor = bounceFactor;
		}
		public static class Builder {
			private float width = 0.25f, height = 0.25f;
			private int pierceLevel = 0;
			private boolean bounces = false;
			private float bounceFactor = 0.5f;

			public Builder size(float w, float h) { this.width = w; this.height = h; return this; }
			public Builder pierce(int p) { this.pierceLevel = p; return this; }
			public Builder bounce(float factor) { this.bounces = true; this.bounceFactor = factor; return this; }
			Collision build() { return new Collision(width, height, pierceLevel, bounces, bounceFactor); }
		}
	}

	// ==========================================
	// 模块 3：有效载荷与反馈
	// ==========================================
	public static class Payload {
		public final float baseDamage;          // 基础伤害
		public final float knockback;           // 击退力
		public final int fireSeconds;           // 燃烧的秒数
		public final float explosionRadius;     // 爆炸半径

		private Payload(float baseDamage, float knockback, int fireSeconds, float explosionRadius) {
			this.baseDamage = baseDamage; this.knockback = knockback;
			this.fireSeconds = fireSeconds; this.explosionRadius = explosionRadius;
		}
		public static class Builder {
			private float baseDamage = 5.0f, knockback = 0.0f, explosionRadius = 0.0f;
			private int fireSeconds = 0;

			public Builder damage(float d) { this.baseDamage = d; return this; }
			public Builder knockback(float k) { this.knockback = k; return this; }
			public Builder fire(int seconds) { this.fireSeconds = seconds; return this; }
			public Builder explode(float radius) { this.explosionRadius = radius; return this; }
			Payload build() { return new Payload(baseDamage, knockback, fireSeconds, explosionRadius); }
		}
	}

	// ==========================================
	// 模块 4：视听与服务端表现 (渲染代码仍在客户端)
	// ==========================================
	public static class Visuals {
		public final float scale;               // 渲染缩放比例
		public final boolean glowing;           // 是否自带发光轮廓
		public final ParticleOptions tickParticle;// 粒子

		private Visuals(float scale, boolean glowing, ParticleOptions tickParticle) {
			this.scale = scale; this.glowing = glowing; this.tickParticle = tickParticle;
		}
		public static class Builder {
			private float scale = 1.0f;
			private boolean glowing = false;
			private ParticleOptions tickParticle = null;

			public Builder scale(float s) { this.scale = s; return this; }
			public Builder glowing(boolean g) { this.glowing = g; return this; }
			public Builder particle(ParticleOptions p) { this.tickParticle = p; return this; }
			Visuals build() { return new Visuals(scale, glowing, tickParticle); }
		}
	}

	// ==========================================
	// 模块 5：生命周期与回调
	// ==========================================
	public static class Lifecycle {
		public final int maxAge;                //最大生命周期
		public final Consumer<GenericProjectile> onTick;// tick回调函数
		public final BiConsumer<GenericProjectile, EntityHitResult> onHitEntity;// 命中实体的回调函数
		public final BiConsumer<GenericProjectile, BlockHitResult> onHitBlock;  // 命中方块的回调函数

		private Lifecycle(int maxAge, Consumer<GenericProjectile> onTick,
				BiConsumer<GenericProjectile, EntityHitResult> onHitEntity,
				BiConsumer<GenericProjectile, BlockHitResult> onHitBlock) {
			this.maxAge = maxAge; this.onTick = onTick;
			this.onHitEntity = onHitEntity; this.onHitBlock = onHitBlock;
		}
		public static class Builder {
			private int maxAge = 200; // 默认 10 秒
			private Consumer<GenericProjectile> onTick = p -> {};
			private BiConsumer<GenericProjectile, EntityHitResult> onHitEntity = (p, hit) -> {};
			private BiConsumer<GenericProjectile, BlockHitResult> onHitBlock = (p, hit) -> {};

			public Builder maxAge(int ticks) { this.maxAge = ticks; return this; }
			public Builder onTick(Consumer<GenericProjectile> action) { this.onTick = action; return this; }
			public Builder onHitEntity(BiConsumer<GenericProjectile, EntityHitResult> action) { this.onHitEntity = action; return this; }
			public Builder onHitBlock(BiConsumer<GenericProjectile, BlockHitResult> action) { this.onHitBlock = action; return this; }
			Lifecycle build() { return new Lifecycle(maxAge, onTick, onHitEntity, onHitBlock); }
		}
	}

	// ==========================================
	// 主 Builder (使用 Consumer 语法糖组装模块)
	// ==========================================
	public static class Builder {
		private final Physics.Builder physics = new Physics.Builder();
		private final Collision.Builder collision = new Collision.Builder();
		private final Payload.Builder payload = new Payload.Builder();
		private final Visuals.Builder visuals = new Visuals.Builder();
		private final Lifecycle.Builder lifecycle = new Lifecycle.Builder();

		public Builder physics(Consumer<Physics.Builder> consumer) { consumer.accept(this.physics); return this; }
		public Builder collision(Consumer<Collision.Builder> consumer) { consumer.accept(this.collision); return this; }
		public Builder payload(Consumer<Payload.Builder> consumer) { consumer.accept(this.payload); return this; }
		public Builder visuals(Consumer<Visuals.Builder> consumer) { consumer.accept(this.visuals); return this; }
		public Builder lifecycle(Consumer<Lifecycle.Builder> consumer) { consumer.accept(this.lifecycle); return this; }

		public ProjectileType build() {
			return new ProjectileType(
					physics.build(), collision.build(), payload.build(), visuals.build(), lifecycle.build()
			);
		}
	}
}
