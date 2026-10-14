package com.skyisland.choice;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds the pool of blocks that may appear in the five-block selection.
 *
 * <p>Blocks that cannot be placed on their own are filtered out: anything that needs support
 * (torches, ladders, signs, doors, plants, rails, ...), anything that falls (sand, gravel, ...),
 * anything that is not really a placeable block (air, fire, fluids, portals) and anything with a
 * block entity.</p>
 */
public final class BlockSelectionPool {

    /** How many different blocks are offered to the player. */
    public static final int OPTION_COUNT = 5;

    /** Used when the world has fewer than five usable blocks (heavily modded edge case). */
    private static final List<Block> FALLBACK = List.of(
            Blocks.STONE, Blocks.DIRT, Blocks.OAK_PLANKS, Blocks.COBBLESTONE, Blocks.GLASS,
            Blocks.SANDSTONE, Blocks.BRICKS, Blocks.OAK_LOG, Blocks.WHITE_WOOL, Blocks.IRON_BLOCK);

    private static List<Block> cache;

    private BlockSelectionPool() {
    }

    /** @return five distinct randomly chosen blocks. */
    public static List<Block> pick(RandomSource random) {
        List<Block> pool = candidates();
        List<Block> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, new Random(random.nextLong()));
        if (shuffled.size() < OPTION_COUNT) {
            for (Block block : FALLBACK) {
                if (!shuffled.contains(block)) {
                    shuffled.add(block);
                }
                if (shuffled.size() >= OPTION_COUNT) {
                    break;
                }
            }
        }
        return List.copyOf(shuffled.subList(0, Math.min(OPTION_COUNT, shuffled.size())));
    }

    /** @return every block that is allowed to be offered. */
    public static synchronized List<Block> candidates() {
        if (cache == null) {
            List<Block> pool = new ArrayList<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                if (isPlaceableAlone(block)) {
                    pool.add(block);
                }
            }
            cache = List.copyOf(pool);
        }
        return cache;
    }

    /** @return true when the block can be placed into the world without any support. */
    public static boolean isPlaceableAlone(Block block) {
        try {
            if (block == Blocks.AIR || block == Blocks.FIRE || block == Blocks.SOUL_FIRE) {
                return false;
            }
            BlockState state = block.defaultBlockState();
            if (state.isAir() || state.hasBlockEntity()) {
                return false;
            }
            // Gravity blocks (sand, gravel, concrete powder, ...) would fall straight off the island.
            if (block instanceof net.minecraft.world.level.block.FallingBlock) {
                return false;
            }
            // Carpets of every colour are excluded, as requested.
            if (block instanceof net.minecraft.world.level.block.CarpetBlock
                    || block instanceof net.minecraft.world.level.block.WoolCarpetBlock) {
                return false;
            }

            // Water and lava are explicitly allowed even though fluids are otherwise filtered out:
            // a source block sits fine on top of a solid layer by itself.
            if (block == Blocks.WATER || block == Blocks.LAVA) {
                return true;
            }

            for (BlockState possible : block.getStateDefinition().getPossibleStates()) {
                // Fluids and anything that has to be attached to a neighbour or the ground.
                if (possible.getPistonPushReaction() != PushReaction.NORMAL) {
                    return false;
                }
                if (possible.hasProperty(BlockStateProperties.WATERLOGGED)
                        || possible.hasProperty(BlockStateProperties.ATTACH_FACE)
                        || possible.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                        || possible.hasProperty(BlockStateProperties.HANGING)
                        || possible.hasProperty(BlockStateProperties.OPEN)
                        || possible.hasProperty(BlockStateProperties.ROTATION_16)
                        || possible.hasProperty(BlockStateProperties.AXIS)
                        || possible.hasProperty(BlockStateProperties.NORTH)
                        || possible.hasProperty(BlockStateProperties.UP)) {
                    return false;
                }
            }
            return true;
        } catch (Throwable throwable) {
            return false;
        }
    }
}
