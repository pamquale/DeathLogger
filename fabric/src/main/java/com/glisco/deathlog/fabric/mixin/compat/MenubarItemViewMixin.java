package com.glisco.deathlog.fabric.mixin.compat;

import com.glisco.deathlog.client.DeathLogClient;
import com.thecsdev.commonmc.api.client.gui.ctxmenu.TContextMenu;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Appends "View Death Log" to the View menu of the Better Statistics Screen.
 * <p>
 * Targeted by string name with {@code remap = false} because it is another mod's
 * class, and {@code require = 0} so a future rewrite of that mod costs the entry
 * rather than the game.
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
                Component.translatable("deathlogger.betterstats.open_deaths"),
                button -> DeathLogClient.openScreen()
        );
        return builder.build();
    }
}
