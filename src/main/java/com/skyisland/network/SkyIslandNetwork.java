package com.skyisland.network;

import com.skyisland.SkyIslandMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Vanilla Forge channel used to open the block selection screen and to report button clicks. */
public final class SkyIslandNetwork {

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SkyIslandMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private SkyIslandNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, SelectionMessage.class,
                SelectionMessage::encode,
                SelectionMessage::decode,
                SelectionMessage::handle);
    }
}
