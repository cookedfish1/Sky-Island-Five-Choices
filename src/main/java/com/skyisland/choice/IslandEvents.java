package com.skyisland.choice;

import com.skyisland.SkyIslandMod;
import com.skyisland.world.SkyIslandData;
import com.skyisland.world.WorldTransformer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Forge event hooks of the Sky Island mod. */
public final class IslandEvents {

    /** Marked on a player the first time they join, so they can be put on the world spawn point. */
    private static final String FIRST_VISIT_TAG = "skyisland:visited";

    private IslandEvents() {
    }

    /**
     * Prepares a Sky Island world the moment its level is loaded.
     *
     * <p>This runs before any player is placed, and in particular before the vanilla initial spawn
     * point is worked out, so the world spawn can be pinned to the platform centre in time.</p>
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!WorldTransformer.isIslandWorldType(level)) {
            return;
        }

        SkyIslandData data = SkyIslandData.get(level);
        data.setIslandWorld(true);
        data.setIslandWorldChecked(true);
        if (!data.isIslandPlaced()) {
            WorldTransformer.placeStarterIsland(level);
            data.setIslandPlaced(true);
        }
    }

    /** Advances the island clock once per game day and finishes preparing an island world. */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        SkyIslandDayCycle.tick(server);
    }

    /**
     * A player joining for the very first time is always put on the world spawn point, and anyone
     * who somehow ends up below the world is rescued.
     */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player entity = event.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) entity;
        ServerLevel level = player.serverLevel();
        if (level.dimension() != Level.OVERWORLD || !SkyIslandData.get(level).isIslandWorld()) {
            return;
        }

        boolean firstVisit = !player.getPersistentData().getBoolean(FIRST_VISIT_TAG);
        player.getPersistentData().putBoolean(FIRST_VISIT_TAG, true);

        if (firstVisit || player.getY() < level.getMinBuildHeight() + 1.0D) {
            BlockPos spawn = level.getSharedSpawnPos();
            player.teleportTo(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D);
            SkyIslandMod.LOGGER.info("Sky Island: put {} on the world spawn point {} (firstVisit={})",
                    player.getName().getString(), spawn, firstVisit);
        }
    }

    /** The chooser left the game: drop the pending selection so the island does not get stuck. */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player entity = event.getEntity();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer) entity;
        if (SelectionHandler.isChooser(player.getUUID())) {
            SelectionHandler.dropPending();
            SkyIslandMod.LOGGER.info("Sky Island: the chooser left, the pending selection was dropped");
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SelectionHandler.dropPending();
    }
}
