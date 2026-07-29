package com.glisco.deathlog.mixin.compat;

import com.glisco.deathlog.client.integration.BetterStatsIntegration;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Appends a "View Death Log" entry to the "View" menu of the BetterStats
 * screen. The Builder signatures are identical in BetterStats 5.0.x and
 * 5.1.x, so this applies to both; {@code require = 0} keeps a future
 * BetterStats rewrite from crashing the game — the entry just won't appear.
 */
@Mixin(targets = "com.thecsdev.betterstats.mcbs.view.menubar.MenubarItemView", remap = false)
public abstract class MenubarItemViewMixin {

    @Redirect(
            method = "createContextMenu",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/thecsdev/commonmc/api/client/gui/ctxmenu/TContextMenu$Builder;build()Lcom/thecsdev/commonmc/api/client/gui/ctxmenu/TContextMenu;"
            ),
            require = 0
    )
    private TContextMenu deathlogger$addDeathLogEntry(TContextMenu.Builder builder) {
        builder.addSeparator();
        builder.addButton(
                Text.translatable("deathlogger.betterstats.open_deaths"),
                button -> BetterStatsIntegration.openDeathLogScreen()
        );
        return builder.build();
    }
}
