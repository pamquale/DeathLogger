package com.glisco.deathlog.client.integration;

import com.glisco.deathlog.client.DeathLogClient;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import io.github.thecsdev.betterstats.api.events.client.gui.BetterStatsGUIEvent;
import io.github.thecsdev.tcdcommons.api.client.gui.panel.menu.TMenuBarPanel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Integration with BetterStats mod using its official API.
 * This class is only loaded when BetterStats is present.
 */
@Environment(EnvType.CLIENT)
public class BetterStatsIntegration {

    /**
     * Registers DeathLogger integration with BetterStats.
     * Called only when BetterStats is detected.
     */
    public static void register() {
        // Register listener for when the BetterStats menu bar is initialized
        BetterStatsGUIEvent.MENU_BAR_INITIALIZED.register(BetterStatsIntegration::onMenuBarInitialized);
        
        System.out.println("[DeathLogger] BetterStats integration registered successfully");
    }

    /**
     * Called when BetterStats menu bar is initialized.
     * Adds the DeathLogger button to the menu bar.
     */
    private static void onMenuBarInitialized(TMenuBarPanel menuBar) {
        // Add DeathLogger button to the menu bar
        menuBar.addButton(
            Text.translatable("deathlogger.betterstats.menubar"),
            btn -> openDeathLogScreen()
        );
    }

    private static void openDeathLogScreen() {
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
