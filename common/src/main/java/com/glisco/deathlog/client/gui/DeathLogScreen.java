package com.glisco.deathlog.client.gui;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.client.DeathScreenshots;
import com.glisco.deathlog.death_info.properties.ScreenshotProperty;
import com.glisco.deathlog.network.RemoteDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The death browser, rebuilt on vanilla widgets.
 * <p>
 * The owo version declared its layout in XML and leaned on owo-ui for the list,
 * the context menu and the animations. None of that exists in vanilla, so the
 * layout is computed here and the list is drawn directly. Restore and delete are
 * buttons rather than a right-click menu, which also makes them discoverable.
 * <p>
 * Slot positions are recorded into {@link #slots} while drawing and every later
 * hit test reads that list. Computing the layout a second time for tooltips and
 * clicks is what made the two disagree.
 */
public class DeathLogScreen extends Screen {

    private static final int PANEL_PADDING = 8;
    private static final int ENTRY_HEIGHT = 24;
    /** Vanilla slot pitch: a 16px icon in an 18px cell. */
    private static final int SLOT_PITCH = 18;
    private static final int ICON_SIZE = 16;
    private static final int PANEL_COLOR = 0x77000000;
    private static final int SLOT_COLOR = 0x55000000;
    private static final int SELECTED_OUTLINE = 0xFFAFAFAF;
    private static final int MAX_SCREENSHOT_WIDTH = 180;

    private final Screen parent;
    private final DirectDeathLogStorage storage;

    private final List<DeathInfo> visibleInfos = new ArrayList<>();
    private final List<Slot> slots = new ArrayList<>();
    private String searchTerm = "";
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean canRestore = true;

    private EditBox searchBox;
    private Button restoreButton;
    private Button deleteButton;

    private Identifier screenshotTextureId;
    private DynamicTexture screenshotTexture;
    private int screenshotWidth;
    private int screenshotHeight;
    private int screenshotSourceWidth;
    private int screenshotSourceHeight;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int detailX;
    private int detailWidth;
    private int detailBottom;

    private record Slot(int x, int y, ItemStack stack) {
        boolean contains(int mouseX, int mouseY) {
            return mouseX >= this.x && mouseX < this.x + ICON_SIZE
                    && mouseY >= this.y && mouseY < this.y + ICON_SIZE;
        }
    }

    public DeathLogScreen(Screen parent, DirectDeathLogStorage storage) {
        super(Component.translatable("text.deathlogger.title"));
        this.parent = parent;
        this.storage = storage;
    }

    public void disableRestoring() {
        this.canRestore = false;
        if (this.restoreButton != null) this.restoreButton.active = false;
    }

    @Override
    protected void init() {
        this.listX = PANEL_PADDING;
        this.listY = PANEL_PADDING + 14;
        this.listWidth = Math.max(120, (int) (this.width * 0.32));
        this.listHeight = this.height - this.listY - PANEL_PADDING - 24;

        this.detailX = this.listX + this.listWidth + PANEL_PADDING;
        this.detailWidth = this.width - this.detailX - PANEL_PADDING;

        this.searchBox = new EditBox(
                this.font, this.listX + 2, this.listY + this.listHeight + 3, this.listWidth - 4, 18,
                Component.translatable("text.deathlogger.search")
        );
        this.searchBox.setResponder(value -> {
            this.searchTerm = value.toLowerCase(Locale.ROOT);
            this.selectedIndex = -1;
            this.scrollOffset = 0;
            releaseScreenshot();
            rebuildVisible();
        });
        addRenderableWidget(this.searchBox);

        int buttonWidth = Math.min(90, (this.detailWidth - PANEL_PADDING) / 2);
        int buttonY = this.height - PANEL_PADDING - 20;
        this.detailBottom = buttonY - 4;

        this.restoreButton = addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.action.restore"), button -> restoreSelected()
        ).bounds(this.detailX, buttonY, buttonWidth, 20).build());

        this.deleteButton = addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.action.delete"), button -> deleteSelected()
        ).bounds(this.detailX + buttonWidth + 4, buttonY, buttonWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.config.title"),
                button -> this.minecraft.setScreen(new DeathLogConfigScreen(this))
        ).bounds(this.width - PANEL_PADDING - 60, PANEL_PADDING - 6, 60, 16).build());

        this.restoreButton.active = false;
        this.deleteButton.active = false;

        rebuildVisible();
    }

    @Override
    public void resize(int width, int height) {
        var reselect = selected();
        super.resize(width, height);
        if (reselect != null) {
            this.selectedIndex = this.visibleInfos.indexOf(reselect);
            updateButtons();
        }
    }

    private void rebuildVisible() {
        this.visibleInfos.clear();
        for (var info : this.storage.getDeathInfoList()) {
            if (this.searchTerm.isBlank() || info.createSearchString().contains(this.searchTerm)) {
                this.visibleInfos.add(info);
            }
        }
        updateButtons();
    }

    private void updateButtons() {
        boolean hasSelection = selected() != null;
        if (this.restoreButton != null) this.restoreButton.active = hasSelection && this.canRestore;
        if (this.deleteButton != null) this.deleteButton.active = hasSelection;
    }

    private DeathInfo selected() {
        if (this.selectedIndex < 0 || this.selectedIndex >= this.visibleInfos.size()) return null;
        return this.visibleInfos.get(this.selectedIndex);
    }

    private void restoreSelected() {
        var info = selected();
        if (info == null) return;

        int index = this.storage.getDeathInfoList().indexOf(info);
        if (index >= 0) this.storage.restore(index);
    }

    private void deleteSelected() {
        var info = selected();
        if (info == null) return;

        this.storage.delete(info);
        this.selectedIndex = -1;
        releaseScreenshot();
        rebuildVisible();
    }

    /** Called when the server sends the full entry for a partial one. */
    public void updateInfo(int index, DeathInfo info) {
        if (this.storage instanceof RemoteDeathLogStorage remote) remote.updateInfo(index, info);
        rebuildVisible();

        var current = selected();
        if (current != null) loadScreenshot(current);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // Stop short of the search box and the buttons: super has already drawn
        // them, and painting the translucent panel over a widget dims it.
        graphics.fill(this.listX, PANEL_PADDING, this.listX + this.listWidth, this.searchBox.getY() - 2, PANEL_COLOR);
        graphics.fill(this.detailX, PANEL_PADDING, this.detailX + this.detailWidth, this.detailBottom, PANEL_COLOR);

        graphics.text(this.font, Component.translatable("text.deathlogger.death_list_title", this.visibleInfos.size()),
                this.listX + 4, PANEL_PADDING + 3, 0xFFFFFFFF);

        renderList(graphics, mouseX, mouseY);

        this.slots.clear();

        var info = selected();
        if (info == null) {
            graphics.centeredText(this.font, Component.translatable("text.deathlogger.no_info_selected_hint"),
                    this.detailX + this.detailWidth / 2, this.height / 2, 0xFFAAAAAA);
            return;
        }

        renderDetail(graphics, info);
        renderSlotTooltip(graphics, mouseX, mouseY);
    }

    private void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight);

        for (int i = 0; i < this.visibleInfos.size(); i++) {
            int entryY = this.listY + i * ENTRY_HEIGHT - this.scrollOffset;
            if (entryY + ENTRY_HEIGHT < this.listY || entryY > this.listY + this.listHeight) continue;

            var info = this.visibleInfos.get(i);
            boolean hovered = mouseX >= this.listX && mouseX <= this.listX + this.listWidth
                    && mouseY >= entryY && mouseY <= entryY + ENTRY_HEIGHT;

            if (i == this.selectedIndex) {
                graphics.outline(this.listX + 1, entryY, this.listWidth - 2, ENTRY_HEIGHT - 1, SELECTED_OUTLINE);
            } else if (hovered) {
                graphics.fill(this.listX + 1, entryY, this.listX + this.listWidth - 1, entryY + ENTRY_HEIGHT - 1, 0x33FFFFFF);
            }

            graphics.text(this.font, trim(info.getListName(), this.listWidth - 10),
                    this.listX + 4, entryY + 3, 0xFFFFFFFF);
            graphics.text(this.font, trim(info.getTitle(), this.listWidth - 10),
                    this.listX + 4, entryY + 13, 0xFFAAAAAA);
        }

        graphics.disableScissor();
    }

    private void renderDetail(GuiGraphicsExtractor graphics, DeathInfo info) {
        final int x = this.detailX + 4;
        int y = PANEL_PADDING + 4;

        graphics.text(this.font, info.getTitle(), x, y, 0xFFFFFFFF);
        y += 14;

        if (info.isPartial()) {
            graphics.text(this.font, Component.translatable("text.deathlogger.death_info_loading"), x, y, 0xFFAAAAAA);
            return;
        }

        var left = info.getLeftColumnText();
        var right = info.getRightColumnText();
        int labelWidth = 0;
        for (var label : left) labelWidth = Math.max(labelWidth, this.font.width(label));

        for (int i = 0; i < left.size() && i < right.size(); i++) {
            graphics.text(this.font, left.get(i), x, y, 0xFFFFFFFF);
            graphics.text(this.font, right.get(i), x + 8 + labelWidth, y, 0xFFFFFFFF);
            y += 11;
        }

        y += 6;
        y = renderInventory(graphics, info, x, y);
        y = renderContainers(graphics, info, x, y);
        renderScreenshot(graphics, x, y);
    }

    /**
     * Laid out like the vanilla inventory: three rows of storage, then the hotbar,
     * with armour and the offhand to the right.
     */
    private int renderInventory(GuiGraphicsExtractor graphics, DeathInfo info, int x, int y) {
        var items = info.getPlayerItems();
        var armor = info.getPlayerArmor();

        int gridWidth = 9 * SLOT_PITCH;

        for (int i = 0; i < 27 && 9 + i < items.size(); i++) {
            drawSlot(graphics, items.get(9 + i), x + (i % 9) * SLOT_PITCH, y + (i / 9) * SLOT_PITCH);
        }

        int hotbarY = y + 3 * SLOT_PITCH + 4;
        for (int i = 0; i < 9 && i < items.size(); i++) {
            drawSlot(graphics, items.get(i), x + i * SLOT_PITCH, hotbarY);
        }

        int sideX = x + gridWidth + 6;
        for (int i = 0; i < armor.size(); i++) {
            drawSlot(graphics, armor.get(i), sideX, y + i * SLOT_PITCH);
        }
        if (items.size() > 36) {
            drawSlot(graphics, items.get(36), sideX + SLOT_PITCH + 4, y);
        }

        return hotbarY + SLOT_PITCH + 6;
    }

    /** Draws the contents of any shulker box or bundle the player was carrying. */
    private int renderContainers(GuiGraphicsExtractor graphics, DeathInfo info, int x, int y) {
        var carried = new ArrayList<ItemStack>(info.getPlayerItems());
        carried.addAll(info.getPlayerArmor());

        boolean headerDrawn = false;
        for (var stack : carried) {
            var contents = contentsOf(stack);
            if (contents.isEmpty()) continue;

            if (!headerDrawn) {
                graphics.text(this.font, Component.translatable("text.deathlogger.extra_containers"), x, y, 0xFFFFFFFF);
                y += 12;
                headerDrawn = true;
            }

            drawSlot(graphics, stack, x, y);
            graphics.text(this.font, stack.getHoverName(), x + SLOT_PITCH + 2, y + 4, 0xFFAAAAAA);
            y += SLOT_PITCH + 2;

            for (int i = 0; i < contents.size(); i++) {
                drawSlot(graphics, contents.get(i), x + 8 + (i % 9) * SLOT_PITCH, y + (i / 9) * SLOT_PITCH);
            }
            y += ((contents.size() + 8) / 9) * SLOT_PITCH + 4;
        }

        return y + (headerDrawn ? 4 : 0);
    }

    private static List<ItemStack> contentsOf(ItemStack stack) {
        if (stack.isEmpty()) return List.of();

        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            var container = stack.get(DataComponents.CONTAINER);
            if (container != null) return container.allItemsCopyStream().filter(item -> !item.isEmpty()).toList();
        }

        if (stack.getItem() instanceof BundleItem) {
            var bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (bundle != null) return bundle.itemCopyStream().filter(item -> !item.isEmpty()).toList();
        }

        return List.of();
    }

    private void drawSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, SLOT_COLOR);
        if (stack.isEmpty()) return;

        graphics.item(stack, x, y);
        graphics.itemDecorations(this.font, stack, x, y);
        this.slots.add(new Slot(x, y, stack));
    }

    /**
     * One tooltip pass over every slot drawn this frame, so an item inside a
     * shulker shows its enchantments the same way one in the main grid does.
     */
    private void renderSlotTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        var hovered = slotAt(mouseX, mouseY);
        if (hovered == null) return;

        var lines = new ArrayList<>(Screen.getTooltipFromItem(this.minecraft, hovered));
        lines.add(Component.translatable("text.deathlogger.action.give_item.hint").withStyle(ChatFormatting.DARK_GRAY));

        graphics.setTooltipForNextFrame(lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
    }

    private ItemStack slotAt(int mouseX, int mouseY) {
        for (var slot : this.slots) {
            if (slot.contains(mouseX, mouseY)) return slot.stack();
        }
        return null;
    }

    private void renderScreenshot(GuiGraphicsExtractor graphics, int x, int y) {
        if (this.screenshotTextureId == null) return;
        if (y + 12 + this.screenshotHeight > this.detailBottom) return;

        graphics.text(this.font, Component.translatable("text.deathlogger.death_screenshot"), x, y, 0xFFFFFFFF);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED, this.screenshotTextureId,
                x, y + 12,
                0f, 0f,
                this.screenshotWidth, this.screenshotHeight,
                this.screenshotSourceWidth, this.screenshotSourceHeight,
                this.screenshotSourceWidth, this.screenshotSourceHeight
        );
    }

    /**
     * Textures are registered per selection and must be handed back, so this runs
     * on every reselect and on close.
     */
    private void releaseScreenshot() {
        if (this.screenshotTextureId != null && this.minecraft != null) {
            this.minecraft.getTextureManager().release(this.screenshotTextureId);
        }
        if (this.screenshotTexture != null) {
            this.screenshotTexture.close();
        }
        this.screenshotTextureId = null;
        this.screenshotTexture = null;
    }

    private void loadScreenshot(DeathInfo info) {
        releaseScreenshot();
        if (this.minecraft == null) return;

        var property = info.getProperty(DeathInfo.SCREENSHOT_KEY).orElse(null);
        if (!(property instanceof ScreenshotProperty screenshot)) return;

        var path = DeathScreenshots.resolve(screenshot.getFileName());
        if (!Files.exists(path)) return;

        try (var stream = Files.newInputStream(path)) {
            var image = NativeImage.read(stream);

            this.screenshotSourceWidth = image.getWidth();
            this.screenshotSourceHeight = image.getHeight();

            int maxWidth = Math.min(MAX_SCREENSHOT_WIDTH, this.detailWidth - 8);
            float scale = Math.min(1f, (float) maxWidth / this.screenshotSourceWidth);
            this.screenshotWidth = Math.max(1, Math.round(this.screenshotSourceWidth * scale));
            this.screenshotHeight = Math.max(1, Math.round(this.screenshotSourceHeight * scale));

            this.screenshotTexture = new DynamicTexture(() -> "deathlogger_screenshot", image);
            this.screenshotTextureId = DeathLog.id("screenshot/" + UUID.randomUUID());
            this.minecraft.getTextureManager().register(this.screenshotTextureId, this.screenshotTexture);
        } catch (IOException e) {
            DeathLog.LOGGER.error("Failed to load the death screenshot {}", screenshot.getFileName(), e);
            releaseScreenshot();
        }
    }

    private Component trim(Component text, int maxWidth) {
        var string = text.getString();
        if (this.font.width(string) <= maxWidth) return text;

        return Component.literal(this.font.plainSubstrByWidth(string, maxWidth - this.font.width("...")) + "...")
                .withStyle(text.getStyle());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mouseX >= this.listX && mouseX <= this.listX + this.listWidth
                && mouseY >= this.listY && mouseY <= this.listY + this.listHeight) {

            int index = (mouseY - this.listY + this.scrollOffset) / ENTRY_HEIGHT;
            if (index >= 0 && index < this.visibleInfos.size()) {
                this.selectedIndex = index;
                updateButtons();
                requestFullInfo();
                loadScreenshot(this.visibleInfos.get(index));
                return true;
            }
        }

        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && copyGiveCommand(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    private void requestFullInfo() {
        var info = selected();
        if (info != null && this.storage instanceof RemoteDeathLogStorage remote) remote.fetchCompleteInfo(info);
    }

    /**
     * Middle-click an item to put a {@code /give} for it on the clipboard. The owo
     * version also spawned the stack directly in creative; that used an interaction
     * manager call that no longer exists in this form, so this is clipboard-only.
     */
    private boolean copyGiveCommand(int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null) return false;

        var stack = slotAt(mouseX, mouseY);
        if (stack == null || stack.isEmpty()) return false;

        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        var command = "/give " + this.minecraft.player.getName().getString() + " " + id
                + (stack.getCount() > 1 ? " " + stack.getCount() : "");

        this.minecraft.keyboardHandler.setClipboard(command);
        this.minecraft.player.sendSystemMessage(
                Component.translatable("text.deathlogger.action.give_item.copied").withStyle(ChatFormatting.GRAY)
        );
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= this.listX && mouseX <= this.listX + this.listWidth) {
            int contentHeight = this.visibleInfos.size() * ENTRY_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - this.listHeight);
            this.scrollOffset = Math.clamp(this.scrollOffset - (int) (deltaY * ENTRY_HEIGHT), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void onClose() {
        releaseScreenshot();
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    public static void open(Screen parent, DirectDeathLogStorage storage, boolean canRestore) {
        var screen = new DeathLogScreen(parent, storage);
        Minecraft.getInstance().setScreen(screen);
        if (!canRestore) screen.disableRestoring();
    }
}
