package com.skyisland.client;

import com.skyisland.network.SelectionMessage;
import net.minecraft.client.Minecraft;

/** Opens, updates and closes the block selection screen on the client. */
public final class SelectionScreenBridge {

    private SelectionScreenBridge() {
    }

    public static void handleServerState(SelectionMessage message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BlockSelectionScreen screen) {
            screen.applyServerState(message);
        } else if (message.getMode() == SelectionMessage.Mode.OPEN) {
            BlockSelectionScreen.open(message);
        }
    }

    public static void handleServerClose() {
        BlockSelectionScreen.close();
    }
}
