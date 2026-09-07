package mod.abbyqaq.abcore.test;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import mod.abbyqaq.abcore.entity.projectile.GenericProjectile;
import mod.abbyqaq.abcore.utils.EntityFindHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * TODO：描述
 *
 * @author Arcomit
 * @since 2026-09-01
 */
@EventBusSubscriber
public class TestCommand {

	@SubscribeEvent
	public static void onCommandsRegister(RegisterCommandsEvent event) {
		TestCommand.register(event.getDispatcher());
	}

	// 注册指令的方法
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("test")
				.requires(source -> source.hasPermission(2))
				.executes(TestCommand::execute));
		dispatcher.register(Commands.literal("test2")
				.requires(source -> source.hasPermission(2))
				.executes(TestCommand::execute2));
	}

	// 指令的执行逻辑
	private static int execute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();

		if (source.getEntity() instanceof LivingEntity shooter) {
			GenericProjectile projectile = new GenericProjectile(level, shooter);
			projectile.setStartAnchorModeAndOffset(GenericProjectile.AnchorMode.OWNER, new Vec3(0.0F, 0, 0.0));
			projectile.setDelayShootTicks(60);
			projectile.setMaxAge(60000);
			projectile.setRoll(90);
			projectile.setAimMode(GenericProjectile.AimMode.OWNER_LOOK);
			projectile.setHasGravity(true);
			projectile.setHomingMode(GenericProjectile.HomingMode.LOCKED_TARGET);
			projectile.setLockedTarget(EntityFindHelper.getCrosshairOrNearestLivingEntity(shooter));
			projectile.setHomingTurnRate(30);

			level.addFreshEntity(projectile);

			source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("生成了 GenericProjectile!"), false);
		}

		return 1; // 返回 1 表示指令执行成功
	}

	private static int execute2(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		ServerLevel level = source.getLevel();

		if (source.getEntity() instanceof LivingEntity shooter) {
			GenericProjectile projectile = new GenericProjectile(level, shooter);
			projectile.setLockedTarget(EntityFindHelper.getCrosshairOrNearestLivingEntity(shooter));
			projectile.setStartAnchorModeAndOffset(GenericProjectile.AnchorMode.OWNER, new Vec3(0.0F, 5.0, 0.0));
			projectile.setDelayShootTicks(30);
			projectile.setMaxAge(6000);
			projectile.setAimMode(GenericProjectile.AimMode.TARGET_CENTER);
			projectile.setHomingMode(GenericProjectile.HomingMode.LOCKED_TARGET);

			level.addFreshEntity(projectile);

			source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("生成了 GenericProjectile!"), false);
		}

		return 1; // 返回 1 表示指令执行成功
	}
}
