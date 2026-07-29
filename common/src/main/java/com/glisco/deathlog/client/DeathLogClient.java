package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.client.Minecraft;

/**
 * Client-side entry point shared by every way of opening the screen: the key
 * mapping, the vanilla statistics screen and the BetterStats menu.
 */
public final class DeathLogClient {

    private DeathLogClient() {}

    public static DirectDeathLogStorage getClientStorage() {
        return DeathLogCommon.getStorage() instanceof DirectDeathLogStorage direct ? direct : null;
    }

    public static void openScreen() {
        var storage = getClientStorage();
        if (storage == null) return;

        var client = Minecraft.getInstance();
        DeathLogScreen.open(client.screen, storage, client.getCurrentServer() == null);
    }
}
