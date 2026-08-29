package mod.abbyqaq.abcore.utils;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-08-28
 */
public class EntityFindUtils {

	/**
	 * 获取指定半径内离目标实体最近的任意实体 (高性能实现)
	 *
	 * @param source  中心实体 (会自动从结果中排除)
	 * @param radius  搜索半径
	 * @param filter  额外的过滤条件 (可为空)
	 * @return 距离最近的实体，如果没有找到则返回 null
	 */
	@Nullable
	public static Entity getNearestAnyEntity(Entity source, double radius, @Nullable Predicate<Entity> filter) {
		Level level = source.level();
		AABB searchBox = source.getBoundingBox().inflate(radius);

		// 创建用于去重的缓存集合 (使用 IdentityHashMap 基于内存地址比较，性能极高)
		Set<Entity> testedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Entity> passedRoots = Collections.newSetFromMap(new IdentityHashMap<>());

		List<Entity> nearbyEntities = level.getEntities(
				source,
				searchBox,
				e -> {
					if (e.isRemoved()) return false;
					Entity root = getRootEntity(e);

					// 如果这个父实体已经被测试过了，直接返回历史结果
					if (testedRoots.contains(root)) {
						return passedRoots.contains(root);
					}

					// 如果没测试过，进行测试并缓存结果
					boolean isPassed = filter == null || filter.test(root);
					testedRoots.add(root);
					if (isPassed) {
						passedRoots.add(root);
					}
					return isPassed;
				}
		);

		Entity nearest = null;
		double minDistanceSqr = radius * radius;

		for (Entity entity : nearbyEntities) {
			double distSqr = source.distanceToSqr(entity);
			if (distSqr < minDistanceSqr) {
				minDistanceSqr = distSqr;
				nearest = entity;
			}
		}

		return getRootEntity(nearest);
	}

	/**
	 * 获取指定半径内离目标实体最近的指定实体 (高性能实现)
	 *
	 * @param source      中心实体
	 * @param targetClass 要查找的目标实体类 (例如 Player.class, Monster.class)
	 * @param radius      搜索半径
	 * @param filter      额外的过滤条件 (可为空)
	 * @return 距离最近的实体，如果没有找到则返回 null
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends Entity> T getNearestEntity(Entity source, Class<T> targetClass, double radius, @Nullable Predicate<T> filter) {
		Level level = source.level();
		AABB searchBox = source.getBoundingBox().inflate(radius);

		Set<Entity> testedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Entity> passedRoots = Collections.newSetFromMap(new IdentityHashMap<>());

		List<T> nearbyEntities = level.getEntitiesOfClass(
				targetClass,
				searchBox,
				e -> {
					if (e == source || e.isRemoved()) return false;
					Entity root = getRootEntity(e);
					if (!targetClass.isInstance(root)) return false;

					if (testedRoots.contains(root)) {
						return passedRoots.contains(root);
					}

					boolean isPassed = filter == null || filter.test((T) root);
					testedRoots.add(root);
					if (isPassed) passedRoots.add(root);

					return isPassed;
				}
		);

		T nearest = null;
		double minDistanceSqr = radius * radius;

		for (T entity : nearbyEntities) {
			double distSqr = source.distanceToSqr(entity);
			if (distSqr < minDistanceSqr) {
				minDistanceSqr = distSqr;
				nearest = entity;
			}
		}

		if (nearest != null) {
			Entity root = getRootEntity(nearest);
			if (targetClass.isInstance(root)) {
				return (T) root;
			}
		}

		return nearest;
	}

	/**
	 * 获取准星方向（视线锥体）内，离准星角度最近的任意实体 (高性能 + 支持多部分实体)
	 *
	 * @param source            中心实体 (会自动从结果中排除)
	 * @param maxDistance       最大搜索距离
	 * @param maxDeviationAngle 最大允许的偏离角度 (单位: 度)。比如 30 表示准星左右各 30 度的圆锥视野。
	 * @param filter            针对最终实体(父实体)的过滤条件 (可为空)
	 * @return 离准星最近的实体 (如果是部件则返回父实体)，如果没有找到则返回 null
	 */
	@Nullable
	public static Entity getNearestAnyEntityToCrosshair(Entity source, double maxDistance, double maxDeviationAngle, @Nullable Predicate<Entity> filter) {
		Level level = source.level();

		// 眼睛坐标与视线向量
		Vec3 eyePos = source.getEyePosition(1.0F);
		Vec3 lookVec = source.getViewVector(1.0F);

		// 预计算常量
		double maxDistSqr = maxDistance * maxDistance;
		double minDotProduct = Math.cos(Math.toRadians(maxDeviationAngle));

		AABB searchBox = source.getBoundingBox().inflate(maxDistance);

		Set<Entity> testedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Entity> passedRoots = Collections.newSetFromMap(new IdentityHashMap<>());

		List<Entity> nearbyEntities = level.getEntities(
				source,
				searchBox,
				e -> {
					if (e.isRemoved()) return false;

					Entity root = getRootEntity(e);

					if (testedRoots.contains(root)) {
						return passedRoots.contains(root);
					}

					boolean isPassed = filter == null || filter.test(root);
					testedRoots.add(root);
					if (isPassed) {
						passedRoots.add(root);
					}

					return isPassed;
				}
		);

		Entity nearest = null;
		double bestScore = -1.0; // 最高得分为 1.0 (与准星完全重合)
		double nearestDistSqr = Double.MAX_VALUE;

		for (Entity entity : nearbyEntities) {
			// 获取目标实体的碰撞箱中心点 (比获取脚部坐标更能代表实体的准星瞄准点)
			Vec3 targetCenter = entity.getBoundingBox().getCenter();
			// 目标相对于眼睛的向量
			Vec3 toTarget = targetCenter.subtract(eyePos);
			double distSqr = toTarget.lengthSqr();

			// 过滤掉超过球形最大距离的实体 (AABB 取的是正方体，球形过滤更精确)
			if (distSqr > maxDistSqr) continue;

			// 处理自身坐标重叠防爆零
			if (distSqr < 0.0001) {
				if (nearest == null || nearestDistSqr > distSqr) {
					nearest = entity;
					bestScore = 1.0;
					nearestDistSqr = distSqr;
				}
				continue;
			}

			// 向量归一化 (此处是本循环唯一不可避免的开方运算计算量)
			Vec3 toTargetNorm = toTarget.normalize();
			// 点乘获取 Cosine (无需计算昂贵的 Math.acos)
			double dot = lookVec.dot(toTargetNorm);

			// 如果点乘结果小于允许的最小值，说明偏角过大 (不在视野圆锥内)
			if (dot < minDotProduct) continue;

			// 【判定逻辑】
			// 1. dot (点乘结果) 越大，说明越贴近准星。
			if (dot > bestScore) {
				bestScore = dot;
				nearest = entity;
				nearestDistSqr = distSqr;
			}
			// 2. 领带打破机制 (Tie-breaker): 如果两个实体的准星夹角相差无几(差别小于0.01)，则优先选取物理距离更近的
			else if (Math.abs(dot - bestScore) < 0.01 && distSqr < nearestDistSqr) {
				nearest = entity;
				nearestDistSqr = distSqr;
			}
		}

		// 返回时追溯父实体
		return getRootEntity(nearest);
	}

	/**
	 * 获取准星方向（视线锥体）内，离准星角度最近的指定类型实体
	 *
	 * @param source         中心实体 (通常是玩家或发射者)
	 * @param targetClass    要寻找的实体类
	 * @param maxDistance    最大搜索距离
	 * @param maxDeviationAngle 最大允许的偏离角度 (单位: 度)。比如 30 表示准星左右各 30 度的圆锥视野。
	 * @param filter         针对父实体的过滤条件
	 * @return 离准星最近的实体 (如果角度极其接近，优先返回物理距离更近的)
	 */
	@Nullable
	@SuppressWarnings("unchecked")
	public static <T extends Entity> T getNearestEntityToCrosshair(Entity source, Class<T> targetClass, double maxDistance, double maxDeviationAngle, @Nullable Predicate<T> filter) {
		Level level = source.level();

		Vec3 eyePos = source.getEyePosition(1.0F);
		Vec3 lookVec = source.getViewVector(1.0F);

		double maxDistSqr = maxDistance * maxDistance;
		double minDotProduct = Math.cos(Math.toRadians(maxDeviationAngle));

		AABB searchBox = source.getBoundingBox().inflate(maxDistance);

		Set<Entity> testedRoots = Collections.newSetFromMap(new IdentityHashMap<>());
		Set<Entity> passedRoots = Collections.newSetFromMap(new IdentityHashMap<>());

		List<T> nearbyEntities = level.getEntitiesOfClass(
				targetClass,
				searchBox,
				e -> {
					if (e == source || e.isRemoved()) return false;
					Entity root = getRootEntity(e);
					if (!targetClass.isInstance(root)) return false;

					if (testedRoots.contains(root)) return passedRoots.contains(root);

					boolean isPassed = filter == null || filter.test((T) root);
					testedRoots.add(root);
					if (isPassed) passedRoots.add(root);

					return isPassed;
				}
		);

		T nearest = null;
		double bestScore = -1.0;
		double nearestDistSqr = Double.MAX_VALUE;

		for (T entity : nearbyEntities) {
			Vec3 targetCenter = entity.getBoundingBox().getCenter();
			Vec3 toTarget = targetCenter.subtract(eyePos);
			double distSqr = toTarget.lengthSqr();

			if (distSqr > maxDistSqr) continue;

			if (distSqr < 0.0001) {
				if (nearest == null || nearestDistSqr > distSqr) {
					nearest = entity;
					bestScore = 1.0;
					nearestDistSqr = distSqr;
				}
				continue;
			}

			Vec3 toTargetNorm = toTarget.normalize();
			double dot = lookVec.dot(toTargetNorm);

			if (dot < minDotProduct) continue;

			if (dot > bestScore) {
				bestScore = dot;
				nearest = entity;
				nearestDistSqr = distSqr;
			}
			else if (Math.abs(dot - bestScore) < 0.01 && distSqr < nearestDistSqr) {
				nearest = entity;
				nearestDistSqr = distSqr;
			}
		}

		if (nearest != null) {
			Entity root = getRootEntity(nearest);
			if (targetClass.isInstance(root)) {
				return (T) root;
			}
		}

		return nearest;
	}

	/**
	 * 辅助方法：获取实体的最顶层父实体
	 */
	@Nullable
	public static Entity getRootEntity(@Nullable Entity entity) {
		if (entity == null) return null;

		if (entity instanceof PartEntity<?> partEntity) {
			return partEntity.getParent();
		}

		// 兼容性防弹处理：以防某些 Mod 未正确实现 PartEntity 接口，但使用了原版末影龙部件逻辑
		if (entity instanceof EnderDragonPart dragonPart) {
			return dragonPart.parentMob;
		}

		// 如果不是部件，返回它本身
		return entity;
	}
}
