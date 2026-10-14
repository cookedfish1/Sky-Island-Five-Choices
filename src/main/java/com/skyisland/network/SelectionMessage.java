package com.skyisland.network;

import com.skyisland.choice.SelectionHandler;
import com.skyisland.choice.SelectionScreenOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Every message of the block selection screen.
 *
 * <ul>
 *   <li>{@link Mode#OPEN} - server to client: open the screen with the five offered blocks.</li>
 *   <li>{@link Mode#REVEAL} - client to server: the refresh button was pressed.</li>
 *   <li>{@link Mode#COMMIT} - client to server: the confirm button was pressed.</li>
 *   <li>{@link Mode#STATE} - server to client: the shown block moved on / everything is revealed.</li>
 *   <li>{@link Mode#CLOSE} - server to client: close the screen.</li>
 * </ul>
 */
public class SelectionMessage {

    public enum Mode {
        OPEN,
        REVEAL,
        COMMIT,
        STATE,
        CLOSE
    }

    private final Mode mode;
    private final List<Integer> blockIds;
    /** How far the screen is revealed: every block up to this index is uncovered. */
    private final int shownIndex;
    /** Index of the confirmed block, or -1 while nothing is confirmed. */
    private final int chosenIndex;
    private final boolean committed;
    private final int targetY;

    private SelectionMessage(Mode mode, List<Integer> blockIds, int shownIndex, int chosenIndex,
                             boolean committed, int targetY) {
        this.mode = mode;
        this.blockIds = blockIds;
        this.shownIndex = shownIndex;
        this.chosenIndex = chosenIndex;
        this.committed = committed;
        this.targetY = targetY;
    }

    public static SelectionMessage open(List<Block> options, int targetY) {
        List<Integer> ids = new ArrayList<>(options.size());
        for (Block block : options) {
            ids.add(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getId(block));
        }
        return new SelectionMessage(Mode.OPEN, ids, 0, -1, false, targetY);
    }

    /**
     * @param shownIndex  how far the screen is revealed
     * @param chosenIndex the confirmed block, or -1 while nothing is confirmed
     * @param committed   whether the choice has been locked in
     * @param blockCount  how many blocks exist, for the refresh button
     */
    public static SelectionMessage state(int shownIndex, int chosenIndex, boolean committed, int blockCount) {
        return new SelectionMessage(Mode.STATE, List.of(), shownIndex, chosenIndex, committed, blockCount);
    }

    public static SelectionMessage close() {
        return new SelectionMessage(Mode.CLOSE, List.of(), 0, -1, false, 0);
    }

    public static SelectionMessage request(Mode mode) {
        return new SelectionMessage(mode, List.of(), 0, -1, false, 0);
    }

    public Mode getMode() {
        return this.mode;
    }

    public List<Integer> getBlockIds() {
        return this.blockIds;
    }

    public int getShownIndex() {
        return this.shownIndex;
    }

    /** @return the confirmed block, or -1 while nothing is confirmed. */
    public int getChosenIndex() {
        return this.chosenIndex;
    }

    public boolean isCommitted() {
        return this.committed;
    }

    public int getTargetY() {
        return this.targetY;
    }

    public static void encode(SelectionMessage message, FriendlyByteBuf buffer) {
        buffer.writeEnum(message.mode);
        buffer.writeVarInt(message.blockIds.size());
        for (int id : message.blockIds) {
            buffer.writeVarInt(id);
        }
        buffer.writeVarInt(message.shownIndex);
        buffer.writeVarInt(message.chosenIndex);
        buffer.writeBoolean(message.committed);
        buffer.writeVarInt(message.targetY);
    }

    public static SelectionMessage decode(FriendlyByteBuf buffer) {
        Mode mode = buffer.readEnum(Mode.class);
        int size = buffer.readVarInt();
        List<Integer> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readVarInt());
        }
        int shownIndex = buffer.readVarInt();
        int chosenIndex = buffer.readVarInt();
        boolean committed = buffer.readBoolean();
        int targetY = buffer.readVarInt();
        return new SelectionMessage(mode, ids, shownIndex, chosenIndex, committed, targetY);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        switch (this.mode) {
            case OPEN, STATE -> context.enqueueWork(() -> SelectionScreenOpener.handleServerState(this));
            case CLOSE -> context.enqueueWork(SelectionScreenOpener::handleServerClose);
            case REVEAL, COMMIT -> {
                ServerPlayer player = context.getSender();
                if (player != null) {
                    Mode mode = this.mode;
                    context.enqueueWork(() -> SelectionHandler.onClientRequest(player, mode));
                }
            }
        }
        context.setPacketHandled(true);
    }
}
