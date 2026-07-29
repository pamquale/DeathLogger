package com.glisco.deathlog.mixin;

import com.glisco.deathlog.client.DeathLogClient;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the DeathLogger button to the vanilla statistics screen. Doing this with a
 * mixin rather than a loader event keeps it in common and gives all three loaders
 * the same entry point.
 */
@Mixin(StatsScreen.class)
public abstract class StatsScreenMixin extends Screen {

    protected StatsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void deathlogger$addButton(CallbackInfo ci) {
        addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.title"), button -> DeathLogClient.openScreen()
        ).bounds(6, this.height - 26, 80, 20).build());
    }
}
