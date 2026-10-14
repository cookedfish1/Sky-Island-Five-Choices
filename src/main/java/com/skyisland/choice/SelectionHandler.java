package com.skyisland.choice;

import com.skyisland.SkyIslandMod;
import com.skyisland.network.SelectionMessage;
import com.skyisland.network.SkyIslandNetwork;
import com.skyisland.world.SkyIslandData;
import com.skyisland.world.WorldTransformer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Server side driver of the once-per-day five-block selection. */
public final class SelectionHandler {

    private static SelectionSession pending;

    private SelectionHandler() {
    }

    public static SelectionSession getPending() {
        return pending;
    }

    /** True while an island world exists, i.e. the selection may run at all. */
    public static boolean hasIslandWorld(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        return overworld != null && SkyIslandData.get(overworld).isIslandWorld();
    }

    /**
     * Starts the selection for a new in-game day. The caller has already made sure the island has not
     * reached the bottom of the world yet.
     *
     * @param day the number of the day that has just started, used for the chat messages
     */
    public static void startDay(MinecraftServer server, long day) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null || !hasIslandWorld(server) || pending != null) {
            return;
        }

        SkyIslandData data = SkyIslandData.get(overworld);
        int targetY = firstTargetY(data.getSelectionDays());

        // The day has started: remember its number so the chat messages can report it.
        data.setDayCounter(day);

        // The last layer is the one whose target level is already below the world. That day still
        // asks for a block, but the chosen block cannot be placed; the portal chest is handed out
        // instead once the choice is confirmed.
        boolean lastLayer = targetY < overworld.getMinBuildHeight();

        ServerPlayer chooser = pickChooser(server);
        if (chooser == null) {
            SkyIslandMod.LOGGER.info("Sky Island: no player is online, the selection for day {} is skipped", day);
            return;
        }

        List<Block> options = BlockSelectionPool.pick(overworld.getRandom());
        pending = new SelectionSession(options, targetY, chooser.getUUID());
        pending.setLastLayer(lastLayer);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == chooser) {
                player.sendSystemMessage(Component.translatable("skyisland.msg.your_turn", day)
                        .withStyle(ChatFormatting.GOLD));
            } else {
                player.sendSystemMessage(Component.translatable("skyisland.msg.waiting", day,
                        chooser.getDisplayName()).withStyle(ChatFormatting.GRAY));
            }
        }

        SkyIslandNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> chooser),
                SelectionMessage.open(options, targetY));
    }

    /**
     * @param filledLayers how many layers have already been filled
     * @return the y level the next layer is filled into: -1, -2, -3 and so on, directly underneath
     *         the layer above it (and the grass platform for the first one)
     */
    public static int firstTargetY(long filledLayers) {
        return (int) (-1L - filledLayers);
    }

    /** Called when the chooser pressed a button on the client screen. */
    public static void onClientRequest(ServerPlayer player, SelectionMessage.Mode mode) {
        if (pending == null || !pending.getChooser().equals(player.getUUID())) {
            return;
        }
        switch (mode) {
            case REVEAL -> {
                pending.revealNext();
                syncState(player);
            }
            case COMMIT -> {
                if (pending.commit()) {
                    // First confirm: remember the shown block and reveal the remaining ones.
                    syncState(player);
                } else {
                    finish(player.getServer(), player);
                }
            }
            default -> {
            }
        }
    }

    private static void syncState(ServerPlayer player) {
        SkyIslandNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                SelectionMessage.state(pending.getShownIndex(), pending.getChosenIndex(),
                        pending.isCommitted(), pending.getOptions().size()));
    }

    /** Second confirm: fills the recorded layer and reports the choice in chat. */
    private static void finish(MinecraftServer server, ServerPlayer player) {
        SelectionSession session = pending;
        Block target = session == null ? null : session.getTarget();
        if (server == null || session == null || target == null) {
            return;
        }

        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) {
            return;
        }

        SkyIslandData data = SkyIslandData.get(overworld);
        boolean lastLayer = session.isLastLayer();
        if (!lastLayer) {
            fillLayer(overworld, session.getTargetY(), target);
        }
        data.setSelectionDays(data.getSelectionDays() + 1L);

        long day = data.getDayCounter();
        List<Component> skipped = new ArrayList<>();
        for (Block block : session.getSkipped()) {
            skipped.add(Component.translatable(block.getDescriptionId()).withStyle(ChatFormatting.GRAY));
        }
        Component skippedText = skipped.isEmpty()
                ? Component.literal("-")
                : Component.empty().append(skipped.get(0)).append(joinRest(skipped));

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(Component.translatable("skyisland.msg.summary",
                            day, Component.translatable(target.getDescriptionId()), skippedText)
                    .withStyle(ChatFormatting.YELLOW));
        }

        pending = null;
        SkyIslandNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                SelectionMessage.close());

        // The last layer is already below the world, so the portal material goes into a chest on top
        // of the platform instead.
        if (lastLayer) {
            givePortalChest(server, overworld);
        }
    }

    private static Component joinRest(List<Component> skipped) {
        MutableComponent result = Component.empty();
        for (int i = 1; i < skipped.size(); i++) {
            result.append(Component.literal(", ")).append(skipped.get(i));
        }
        return result;
    }

    /** Fills a whole layer of the island at the given y level, as wide as the platform. */
    public static void fillLayer(ServerLevel level, int y, Block block) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return;
        }
        BlockState state = block.defaultBlockState();
        int radius = WorldTransformer.ISLAND_RADIUS;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                level.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_ALL);
            }
        }
    }

    /**
     * Places a single chest on top of the platform centre containing the material for an End portal:
     * twelve End portal frames and twelve obsidian.
     */
    public static void givePortalChest(MinecraftServer server, ServerLevel overworld) {
        SkyIslandData data = SkyIslandData.get(overworld);
        if (data.isPortalBuilt()) {
            return;
        }

        // The top of the platform: the highest block of the centre column, chest one above it.
        int y = WorldTransformer.ISLAND_Y + 1;
        BlockPos centre = new BlockPos(0, y, 0);
        if (y < overworld.getMinBuildHeight() || y >= overworld.getMaxBuildHeight()) {
            SkyIslandMod.LOGGER.warn("Sky Island: cannot place the portal chest at y={}", y);
            return;
        }

        overworld.setBlock(centre, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        List<ItemStack> loot = new ArrayList<>(24);
        for (int i = 0; i < 12; i++) {
            loot.add(new ItemStack(Items.END_PORTAL_FRAME));
        }
        for (int i = 0; i < 12; i++) {
            loot.add(new ItemStack(Items.OBSIDIAN));
        }

        if (overworld.getBlockEntity(centre) instanceof ChestBlockEntity chest) {
            for (int slot = 0; slot < Math.min(loot.size(), chest.getContainerSize()); slot++) {
                chest.setItem(slot, loot.get(slot));
            }
            chest.setChanged();
        } else {
            SkyIslandMod.LOGGER.warn("Sky Island: the portal chest at {} has no inventory", centre);
        }

        data.setPortalBuilt(true);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            online.sendSystemMessage(Component.translatable("skyisland.msg.finale", data.getDayCounter())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        SkyIslandMod.LOGGER.info("Sky Island: last layer reached, portal chest placed at {}", centre);
    }

    /** Chooses one random online player, spectators excluded. */
    private static ServerPlayer pickChooser(MinecraftServer server) {
        List<ServerPlayer> candidates = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator()) {
                candidates.add(player);
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    /** Drops a pending selection, e.g. when the chooser disconnects. */
    public static void dropPending() {
        pending = null;
    }

    /** @return true when the given player currently has to choose. */
    public static boolean isChooser(UUID uuid) {
        return pending != null && pending.getChooser().equals(uuid);
    }
}

