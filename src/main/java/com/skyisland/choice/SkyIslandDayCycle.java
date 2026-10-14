package com.skyisland.choice;

import com.skyisland.SkyIslandMod;
import com.skyisland.world.SkyIslandData;
import com.skyisland.world.WorldTransformer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Runs the once-per-game-day selection and prepares a newly created Sky Island world.
 *
 * <p>The clock is derived from the overworld day time instead of an accumulating tick counter, so
 * sleeping through the night advances the island exactly like waiting for it.</p>
 */
public final class SkyIslandDayCycle {

    private SkyIslandDayCycle() {
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        SkyIslandData data = SkyIslandData.get(overworld);

        // Normally the level load event has already done this; it is repeated here as a safety net
        // so an island world is never left without its platform.
        if (!data.isIslandPlaced() && WorldTransformer.isIslandWorldType(overworld)) {
            data.setIslandWorld(true);
            data.setIslandWorldChecked(true);
            WorldTransformer.placeStarterIsland(overworld);
            data.setIslandPlaced(true);
        }

        long currentDay = Math.floorDiv(overworld.getDayTime(), 24000L);
        if (data.getLastProcessedDay() < 0) {
            // Adopt the running day without firing a selection for it.
            data.setLastProcessedDay(currentDay);
            return;
        }

        long last = data.getLastProcessedDay();
        while (last < currentDay) {
            last++;
            data.setLastProcessedDay(last);
            try {
                beginSelectionOrFinale(server, overworld, data);
            } catch (Throwable throwable) {
                SkyIslandMod.LOGGER.error("Sky Island: could not run the selection for game day {}", last, throwable);
            }
        }
    }

    private static void beginSelectionOrFinale(MinecraftServer server, ServerLevel overworld, SkyIslandData data) {
        if (!data.isIslandWorld() || data.isPortalBuilt()) {
            return;
        }
        if (SelectionHandler.getPending() != null) {
            SkyIslandMod.LOGGER.warn("Sky Island: a selection is still pending, skipping day {}",
                    data.getLastProcessedDay());
            return;
        }

        // The selection runs on every day, including the last one. The last layer is the one whose
        // target level would already be below the world; that day also drops the portal chest.
        SelectionHandler.startDay(server, data.getLastProcessedDay());
    }
}
