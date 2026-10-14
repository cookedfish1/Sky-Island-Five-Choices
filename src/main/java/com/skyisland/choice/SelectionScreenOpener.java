package com.skyisland.choice;

import com.skyisland.client.SelectionScreenBridge;
import com.skyisland.network.SelectionMessage;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Client side bridge the network layer calls through {@link DistExecutor}, so that a dedicated
 * server never has to load a GUI class.
 */
public final class SelectionScreenOpener {

    private SelectionScreenOpener() {
    }

    public static void handleServerState(SelectionMessage message) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SelectionScreenBridge.handleServerState(message));
    }

    public static void handleServerClose() {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> SelectionScreenBridge::handleServerClose);
    }
}
