package com.skyisland;

import com.mojang.logging.LogUtils;
import com.skyisland.choice.IslandEvents;
import com.skyisland.network.SkyIslandNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Entry point of the Sky Island mod.
 *
 * <p>Adds a "Sky Island" world generation option - a completely empty world in which the mod builds
 * a single 9x9 grass platform at y = 0 - plus the once-per-day "pick one of five blocks" mini game
 * that stacks the chosen layer underneath that platform.</p>
 */
@Mod(SkyIslandMod.MODID)
public class SkyIslandMod {

    public static final String MODID = "skyisland";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SkyIslandMod(FMLJavaModLoadingContext context) {
        SkyIslandNetwork.register();
        MinecraftForge.EVENT_BUS.register(IslandEvents.class);

        LOGGER.info("Sky Island mod initialised");
    }
}
