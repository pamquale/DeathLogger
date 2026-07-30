package com.glisco.deathlog.client.gui;

import com.glisco.deathlog.client.DeathLogConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Settings screen, reachable from the death log itself and from Mod Menu.
 * Replaces the screen owo's config annotation processor used to generate.
 */
public class DeathLogConfigScreen extends Screen {

    private static final int ROW_WIDTH = 220;
    private static final int ROW_HEIGHT = 20;

    private final Screen parent;

    public DeathLogConfigScreen(Screen parent) {
        super(Component.translatable("text.deathlogger.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        var config = DeathLogConfig.get();
        int x = (this.width - ROW_WIDTH) / 2;
        int y = this.height / 4;

        addRenderableWidget(CycleButton.onOffBuilder(config.screenshotsEnabled())
                .withTooltip(value -> net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("text.deathlogger.config.screenshots.tooltip")))
                .create(x, y, ROW_WIDTH, ROW_HEIGHT,
                        Component.translatable("text.deathlogger.config.screenshots"),
                        (button, value) -> config.setScreenshotsEnabled(value)));

        y += ROW_HEIGHT + 6;
        addRenderableWidget(CycleButton.onOffBuilder(config.useLegacyDeathDetection())
                .withTooltip(value -> net.minecraft.client.gui.components.Tooltip.create(
                        Component.translatable("text.deathlogger.config.legacy_detection.tooltip")))
                .create(x, y, ROW_WIDTH, ROW_HEIGHT,
                        Component.translatable("text.deathlogger.config.legacy_detection"),
                        (button, value) -> config.setUseLegacyDeathDetection(value)));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, this.height - 32, ROW_WIDTH, ROW_HEIGHT).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 4 - 24, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.gui.setScreen(this.parent);
    }
}
