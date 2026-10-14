package com.skyisland.choice;

import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.UUID;

/**
 * One pending five-block selection: the five offered blocks, the player who has to choose and how
 * far the choice has progressed.
 */
public final class SelectionSession {

    /** Index of the first offered block. */
    private static final int FIRST_INDEX = 0;

    /** Value of {@link #chosenIndex} while no block has been committed yet. */
    private static final int NOT_CHOSEN = -1;

    private final List<Block> options;

    /** The layer the chosen block will be filled into. */
    private final int targetY;

    private final UUID chooser;

    /** Index of the block that is currently shown; earlier blocks were shown and skipped. */
    private int shownIndex = FIRST_INDEX;

    /** Index of the committed block, or {@link #NOT_CHOSEN}. */
    private int chosenIndex = NOT_CHOSEN;

    /** True for the final layer, which is below the world and hands out the portal chest instead. */
    private boolean lastLayer;

    public SelectionSession(List<Block> options, int targetY, UUID chooser) {
        this.options = List.copyOf(options);
        this.targetY = targetY;
        this.chooser = chooser;
    }

    /** @return true when this is the last layer, i.e. when the portal chest has to be handed out. */
    public boolean isLastLayer() {
        return this.lastLayer;
    }

    public void setLastLayer(boolean lastLayer) {
        this.lastLayer = lastLayer;
    }

    public List<Block> getOptions() {
        return this.options;
    }

    public int getTargetY() {
        return this.targetY;
    }

    public UUID getChooser() {
        return this.chooser;
    }

    public int getShownIndex() {
        return this.shownIndex;
    }

    public boolean isCommitted() {
        return this.chosenIndex != NOT_CHOSEN;
    }

    public int getChosenIndex() {
        return this.chosenIndex;
    }

    /** @return true when the last of the five blocks is on screen, i.e. refreshing does nothing. */
    public boolean isFullyRevealed() {
        return this.shownIndex >= this.options.size() - 1;
    }

    /** Reveals the next block. Has no effect once every block has been shown. */
    public void revealNext() {
        if (!isFullyRevealed()) {
            this.shownIndex++;
        }
    }

    /**
     * Locks in the block that is currently shown and reveals the remaining blocks.
     *
     * @return true when this call locked in the choice, false when it already was locked in
     */
    public boolean commit() {
        if (isCommitted()) {
            return false;
        }
        this.chosenIndex = this.shownIndex;
        this.shownIndex = this.options.size() - 1;
        return true;
    }

    /** @return the block that will be placed, or {@code null} while nothing was committed. */
    public Block getTarget() {
        return isCommitted() ? this.options.get(this.chosenIndex) : null;
    }

    /** @return the blocks that were shown before the chosen one and were therefore skipped. */
    public List<Block> getSkipped() {
        if (!isCommitted()) {
            return List.of();
        }
        return List.copyOf(this.options.subList(0, this.chosenIndex));
    }
}
