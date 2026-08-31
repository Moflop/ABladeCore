package mod.abbyqaq.abcore;

import com.mojang.logging.LogUtils;
import mod.abbyqaq.abcore.init.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(ABladeCoreMod.MODID)
public class ABladeCoreMod {
	public static final String MODID = "abladecore";
	private static final Logger LOGGER = LogUtils.getLogger();

	public ABladeCoreMod(IEventBus modEventBus, ModContainer modContainer) {
		ModEntities.register(modEventBus);
		LOGGER.info("AbbyQAQ's SlashBlade Core initialized!");
	}

	public static ResourceLocation prefix(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
