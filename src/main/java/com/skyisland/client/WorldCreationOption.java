package com.skyisland.client;

import com.mojang.logging.LogUtils;
import com.skyisland.SkyIslandMod;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Adds the "Sky Island World" switch to the world creation screen.
 *
 * <p>The switch selects the {@code skyisland:sky_island} world preset, which describes a completely
 * empty flat world. The mod turns that into a real void island world the first time it is loaded.</p>
 */
@Mod.EventBusSubscriber(modid = SkyIslandMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WorldCreationOption {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceKey<Registry<WorldPreset>> PRESET_REGISTRY = Registries.WORLD_PRESET;

    /** The world preset that belongs to this mod. */
    public static final ResourceKey<WorldPreset> ISLAND_PRESET =
            ResourceKey.create(PRESET_REGISTRY, ResourceLocation.fromNamespaceAndPath(SkyIslandMod.MODID, "sky_island"));

    private static final int WIDTH = 170;
    private static final int HEIGHT = 20;
    private static final int MARGIN = 6;
    private static final int BOTTOM_OFFSET = 34;

    private WorldCreationOption() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof CreateWorldScreen screen)) {
            return;
        }

        WorldCreationUiState uiState = screen.getUiState();
        Registry<WorldPreset> presets = presets(uiState);
        if (presets == null) {
            return;
        }
        Holder<WorldPreset> islandPreset = presets.getHolder(ISLAND_PRESET).orElse(null);
        if (islandPreset == null) {
            LOGGER.warn("Sky Island: world preset {} is missing", ISLAND_PRESET.location());
            return;
        }

        WorldCreationUiState.WorldTypeEntry islandEntry = new WorldCreationUiState.WorldTypeEntry(islandPreset);
        WorldCreationUiState.WorldTypeEntry normalEntry = presets.getHolder(WorldPresets.NORMAL)
                .map(WorldCreationUiState.WorldTypeEntry::new)
                .orElse(islandEntry);

        CycleButton<Boolean> button = CycleButton.onOffBuilder(isIslandSelected(uiState, islandPreset))
                .create(MARGIN, screen.height - BOTTOM_OFFSET - HEIGHT, WIDTH, HEIGHT,
                        Component.translatable("skyisland.option.island"),
                        (cycleButton, enabled) -> uiState.setWorldType(enabled ? islandEntry : normalEntry));
        button.setTooltip(Tooltip.create(Component.translatable("skyisland.option.island.tooltip")));

        event.addListener(button);
    }

    private static boolean isIslandSelected(WorldCreationUiState uiState, Holder<WorldPreset> islandPreset) {
        WorldCreationUiState.WorldTypeEntry current = uiState.getWorldType();
        return current != null && current.preset().unwrapKey().map(islandPreset::is).orElse(false);
    }

    private static Registry<WorldPreset> presets(WorldCreationUiState uiState) {
        try {
            return uiState.getSettings().worldgenLoadContext().registryOrThrow(PRESET_REGISTRY);
        } catch (Throwable throwable) {
            LOGGER.warn("Sky Island: the world preset registry is unavailable", throwable);
            return null;
        }
    }
}
