package com.glisco.deathlog.client.integration;

import com.glisco.deathlog.client.DeathLogClient;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;

/**
 * Integration with BetterStats: {@link com.glisco.deathlog.mixin.compat.MenubarItemViewMixin}
 * appends a "View Death Log" entry to the "View" menu of the BetterStats screen.
 * This class deliberately references no BetterStats classes, so it is always
 * safe to load regardless of which BetterStats version (if any) is installed.
 */
@Environment(EnvType.CLIENT)
public class BetterStatsIntegration {

    public static void openDeathLogScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        var storage = DeathLogClient.getClientStorage();
        if (storage != null) {
            boolean canRestore = client.getCurrentServerEntry() == null;
            var screen = new DeathLogScreen(client.currentScreen, storage);
            client.setScreen(screen);
            if (!canRestore) {
                screen.disableRestoring();
            }
        }
    }
}
