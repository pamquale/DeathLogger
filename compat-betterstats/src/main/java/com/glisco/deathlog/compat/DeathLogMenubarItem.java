package com.glisco.deathlog.compat;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.client.DeathLogClient;
import com.thecsdev.betterstats.api.client.registry.BClientRegistries;
import com.thecsdev.betterstats.api.mcbs.controller.McbsEditor;
import com.thecsdev.betterstats.api.mcbs.view.menubar.MenubarItem;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;

/**
 * A top-level entry in the Better Statistics Screen menu bar, next to File, View
 * and About.
 * <p>
 * BetterStats exposes {@code BClientRegistries.MENUBAR_ITEM} for exactly this, so
 * registering into it beats the mixin the Fabric build used to redirect the View
 * menu's builder: no injection point to break, and the entry sits where it belongs.
 * <p>
 * This class touches BetterStats types directly and must therefore only be loaded
 * when that mod is present - see {@link BetterStatsIntegration}.
 */
public final class DeathLogMenubarItem extends MenubarItem {

    private DeathLogMenubarItem() {}

    static void register() {
        Registry.register(BClientRegistries.MENUBAR_ITEM, DeathLog.id("death_log"), new DeathLogMenubarItem());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("text.deathlogger.title");
    }

    @Override
    public TContextMenu createContextMenu(Minecraft client, McbsEditor editor) {
        var builder = new TContextMenu.Builder(client);
        builder.addButton(
                Component.translatable("deathlogger.betterstats.open_deaths"),
                button -> DeathLogClient.openScreen()
        );
        return builder.build();
    }
}
