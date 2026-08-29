package mod.abbyqaq;

import com.mojang.logging.LogUtils;
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
		LOGGER.info("Mod initialized!");
	}

	public static ResourceLocation prefix(String path) {
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}
}
