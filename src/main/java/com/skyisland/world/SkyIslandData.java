package com.skyisland.world;

import com.skyisland.SkyIslandMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Per-world state of the Sky Island mini game: how many game days have already been resolved and
 * which day the day clock has been processed up to.
 */
public class SkyIslandData extends SavedData {

    private static final String DATA_NAME = SkyIslandMod.MODID + "_island";

    /** Number of game days that already started / already produced a selection. */
    private long dayCounter;

    /** Number of layers that have actually been filled under the island. */
    private long selectionDays;

    /** The last {@code dayTime / 24000} value that has been handled. */
    private long lastProcessedDay = -1L;

    /** True once the End portal frames have been generated at the bottom of the world. */
    private boolean portalBuilt;

    /** True when this world was created from the Sky Island world preset. */
    private boolean islandWorld;

    /** True once the generation type of the world has been inspected. */
    private boolean islandWorldChecked;

    /** True once the 9x9 grass platform has been built. */
    private boolean islandPlaced;

    public SkyIslandData() {
    }

    public static SkyIslandData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SkyIslandData::load, SkyIslandData::new, DATA_NAME);
    }

    public static SkyIslandData load(CompoundTag tag) {
        SkyIslandData data = new SkyIslandData();
        data.dayCounter = tag.getLong("DayCounter");
        data.selectionDays = tag.getLong("SelectionDays");
        data.lastProcessedDay = tag.contains("LastProcessedDay") ? tag.getLong("LastProcessedDay") : -1L;
        data.portalBuilt = tag.getBoolean("PortalBuilt");
        data.islandWorld = tag.getBoolean("IslandWorld");
        data.islandWorldChecked = tag.getBoolean("IslandWorldChecked");
        data.islandPlaced = tag.getBoolean("IslandPlaced");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("DayCounter", this.dayCounter);
        tag.putLong("SelectionDays", this.selectionDays);
        tag.putLong("LastProcessedDay", this.lastProcessedDay);
        tag.putBoolean("PortalBuilt", this.portalBuilt);
        tag.putBoolean("IslandWorld", this.islandWorld);
        tag.putBoolean("IslandWorldChecked", this.islandWorldChecked);
        tag.putBoolean("IslandPlaced", this.islandPlaced);
        return tag;
    }

    public long getDayCounter() {
        return this.dayCounter;
    }

    public void setDayCounter(long value) {
        this.dayCounter = Math.max(0L, value);
        setDirty();
    }

    public long getSelectionDays() {
        return this.selectionDays;
    }

    public void setSelectionDays(long value) {
        this.selectionDays = Math.max(0L, value);
        setDirty();
    }

    public long getLastProcessedDay() {
        return this.lastProcessedDay;
    }

    public void setLastProcessedDay(long value) {
        this.lastProcessedDay = value;
        setDirty();
    }

    public boolean isPortalBuilt() {
        return this.portalBuilt;
    }

    public void setPortalBuilt(boolean built) {
        this.portalBuilt = built;
        setDirty();
    }

    public boolean isIslandWorld() {
        return this.islandWorld;
    }

    public void setIslandWorld(boolean value) {
        this.islandWorld = value;
        setDirty();
    }

    public boolean isIslandWorldChecked() {
        return this.islandWorldChecked;
    }

    public void setIslandWorldChecked(boolean value) {
        this.islandWorldChecked = value;
        setDirty();
    }

    public boolean isIslandPlaced() {
        return this.islandPlaced;
    }

    public void setIslandPlaced(boolean value) {
        this.islandPlaced = value;
        setDirty();
    }
}
