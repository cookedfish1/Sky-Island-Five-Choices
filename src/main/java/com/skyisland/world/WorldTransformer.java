package com.skyisland.world;

import com.skyisland.SkyIslandMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

/**
 * Sky Island worlds are ordinary superflat worlds without a single layer, which means they are
 * completely empty: nothing but void from the bottom of the world to the build limit.
 *
 * <p>This class detects such a world and builds the starter island - a 9x9 area of grass blocks at
 * y = 0 - the first time the world is loaded, together with the world spawn point on top of it.</p>
 */
public final class WorldTransformer {

    /** Radius of the starter island in blocks; 4 means a 9x9 platform. */
    public static final int ISLAND_RADIUS = 4;

    /** Side length of the starter island in blocks. */
    public static final int ISLAND_SIZE = ISLAND_RADIUS * 2 + 1;

    /** The y level the grass platform is generated at. */
    public static final int ISLAND_Y = 0;

    /** Where players must appear: the block right above the centre of the platform. */
    public static final BlockPos SPAWN_POS = new BlockPos(0, ISLAND_Y + 1, 0);

    private WorldTransformer() {
    }

    /** @return {@code true} when the level is the flat void world of the Sky Island preset. */
    public static boolean isIslandWorldType(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        return generator instanceof FlatLevelSource flat && isVoidFlat(flat.settings());
    }

    private static boolean isVoidFlat(FlatLevelGeneratorSettings settings) {
        return settings.getLayers().isEmpty();
    }

    /**
     * Prepares a Sky Island world: builds the platform, clears stray structures, pins the world
     * spawn to the platform centre and disables the random spawn radius.
     *
     * <p>This runs as early as possible (while the level is being loaded, before any player is
     * placed) so that the very first join lands on the platform.</p>
     */
    public static void placeStarterIsland(ServerLevel level) {
        if (level.getMinBuildHeight() > ISLAND_Y || ISLAND_Y >= level.getMaxBuildHeight()) {
            return;
        }

        for (int x = -ISLAND_RADIUS; x <= ISLAND_RADIUS; x++) {
            for (int z = -ISLAND_RADIUS; z <= ISLAND_RADIUS; z++) {
                level.setBlock(new BlockPos(x, ISLAND_Y, z), Blocks.GRASS_BLOCK.defaultBlockState(),
                        Block.UPDATE_ALL);
            }
        }

        removeStructures(level);

        // The middle of the platform is where players must appear, never in the void below it.
        level.setDefaultSpawnPos(SPAWN_POS, 0.0F);
        forceSpawnRadiusZero(level);
        SkyIslandMod.LOGGER.info("Sky Island: platform ready ({}x{}), spawn pinned to {}", ISLAND_SIZE, ISLAND_SIZE, SPAWN_POS);
    }

    /**
     * The world spawn must be exact, so the vanilla random spawn radius is switched off. The
     * in-memory rule is enough; it is written to the world data with the next save.
     */
    private static void forceSpawnRadiusZero(ServerLevel level) {
        try {
            GameRules rules = level.getGameRules();
            GameRules.IntegerValue radius = rules.getRule(GameRules.RULE_SPAWN_RADIUS);
            if (radius.get() != 0) {
                radius.set(0, level.getServer());
            }
        } catch (Throwable throwable) {
            SkyIslandMod.LOGGER.warn("Sky Island: could not set the spawn radius to 0", throwable);
        }
    }

    /**
     * Sky Island worlds must not contain any structure. The world preset already uses an empty
     * structure override list; this is a second, explicit safeguard for the spawn chunk.
     */
    private static void removeStructures(ServerLevel level) {
        try {
            var chunk = level.getChunk(0, 0);
            var starts = chunk.getAllStarts();
            if (starts.isEmpty()) {
                return;
            }
            for (var structure : new java.util.ArrayList<>(starts.keySet())) {
                // A null start removes the structure from the chunk.
                chunk.setStartForStructure(structure, null);
            }
            SkyIslandMod.LOGGER.info("Sky Island: removed {} structure(s) from the spawn chunk", starts.size());
        } catch (Throwable throwable) {
            SkyIslandMod.LOGGER.warn("Sky Island: could not clear structures from the spawn chunk", throwable);
        }
    }
}
