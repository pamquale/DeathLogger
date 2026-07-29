package com.glisco.deathlog.client.gui;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.network.RemoteDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The death browser, rebuilt on vanilla widgets.
 * <p>
 * The owo version declared its layout in XML and leaned on owo-ui for the list,
 * the context menu and the animations. None of that exists in vanilla, so the
 * layout is computed here and the list is drawn directly. Restore and delete are
 * buttons rather than a right-click menu, which also makes them discoverable.
 */
public class DeathLogScreen extends Screen {

    private static final int PANEL_PADDING = 8;
    private static final int ENTRY_HEIGHT = 24;
    private static final int SLOT_SIZE = 18;
    private static final int PANEL_COLOR = 0x77000000;
    private static final int SELECTED_OUTLINE = 0xFFAFAFAF;

    private final Screen parent;
    private final DirectDeathLogStorage storage;

    private final List<DeathInfo> visibleInfos = new ArrayList<>();
    private String searchTerm = "";
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private boolean canRestore = true;

    private EditBox searchBox;
    private Button restoreButton;
    private Button deleteButton;

    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int detailX;
    private int detailWidth;

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
        this.listWidth = Math.max(120, (int) (this.width * 0.36));
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
            rebuildVisible();
        });
        addRenderableWidget(this.searchBox);

        int buttonWidth = Math.min(90, (this.detailWidth - PANEL_PADDING) / 2);
        this.restoreButton = addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.action.restore"), button -> restoreSelected()
        ).bounds(this.detailX, this.height - PANEL_PADDING - 20, buttonWidth, 20).build());

        this.deleteButton = addRenderableWidget(Button.builder(
                Component.translatable("text.deathlogger.action.delete"), button -> deleteSelected()
        ).bounds(this.detailX + buttonWidth + 4, this.height - PANEL_PADDING - 20, buttonWidth, 20).build());

        this.restoreButton.active = false;
        this.deleteButton.active = false;

        rebuildVisible();
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
        rebuildVisible();
    }

    /** Called when the server sends the full entry for a partial one. */
    public void updateInfo(int index, DeathInfo info) {
        if (this.storage instanceof RemoteDeathLogStorage remote) remote.updateInfo(index, info);
        rebuildVisible();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        graphics.fill(this.listX, PANEL_PADDING, this.listX + this.listWidth, this.height - PANEL_PADDING, PANEL_COLOR);
        graphics.fill(this.detailX, PANEL_PADDING, this.detailX + this.detailWidth, this.height - PANEL_PADDING, PANEL_COLOR);

        graphics.text(this.font, Component.translatable("text.deathlogger.death_list_title", this.visibleInfos.size()),
                this.listX + 4, PANEL_PADDING + 3, 0xFFFFFFFF);

        renderList(graphics, mouseX, mouseY);

        var info = selected();
        if (info == null) {
            graphics.centeredText(this.font, Component.translatable("text.deathlogger.no_info_selected_hint"),
                    this.detailX + this.detailWidth / 2, this.height / 2, 0xFFAAAAAA);
            return;
        }

        renderDetail(graphics, info, mouseX, mouseY);
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

    private void renderDetail(GuiGraphicsExtractor graphics, DeathInfo info, int mouseX, int mouseY) {
        int y = PANEL_PADDING + 4;
        graphics.text(this.font, info.getTitle(), this.detailX + 4, y, 0xFFFFFFFF);
        y += 14;

        if (info.isPartial()) {
            graphics.text(this.font, Component.translatable("text.deathlogger.death_info_loading"),
                    this.detailX + 4, y, 0xFFAAAAAA);
            return;
        }

        var left = info.getLeftColumnText();
        var right = info.getRightColumnText();
        int labelWidth = 0;
        for (var label : left) labelWidth = Math.max(labelWidth, this.font.width(label));

        for (int i = 0; i < left.size() && i < right.size(); i++) {
            graphics.text(this.font, left.get(i), this.detailX + 4, y, 0xFFFFFFFF);
            graphics.text(this.font, right.get(i), this.detailX + 8 + labelWidth, y, 0xFFFFFFFF);
            y += 11;
        }

        y += 6;
        renderInventory(graphics, info, this.detailX + 4, y, mouseX, mouseY);
    }

    private void renderInventory(GuiGraphicsExtractor graphics, DeathInfo info, int x, int y, int mouseX, int mouseY) {
        var items = info.getPlayerItems();
        var armor = info.getPlayerArmor();

        ItemStack hovered = null;

        for (int i = 0; i < armor.size(); i++) {
            int slotX = x + i * SLOT_SIZE;
            hovered = drawSlot(graphics, armor.get(i), slotX, y, mouseX, mouseY, hovered);
        }

        int gridY = y + SLOT_SIZE + 4;
        for (int i = 0; i < items.size() && i < 36; i++) {
            int slotX = x + (i % 9) * SLOT_SIZE;
            int slotY = gridY + (i / 9) * SLOT_SIZE;
            hovered = drawSlot(graphics, items.get(i), slotX, slotY, mouseX, mouseY, hovered);
        }

        if (items.size() > 36) {
            int offhandY = gridY + 4 * SLOT_SIZE + 4;
            hovered = drawSlot(graphics, items.get(36), x, offhandY, mouseX, mouseY, hovered);
        }

        if (hovered != null && !hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(
                    Screen.getTooltipFromItem(this.minecraft, hovered).stream().map(Component::getVisualOrderText).toList(),
                    mouseX, mouseY
            );
        }
    }

    private ItemStack drawSlot(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y, int mouseX, int mouseY, ItemStack hovered) {
        graphics.fill(x, y, x + SLOT_SIZE - 2, y + SLOT_SIZE - 2, 0x55000000);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 1, y + 1);
            graphics.itemDecorations(this.font, stack, x + 1, y + 1);
        }

        boolean isHovered = mouseX >= x && mouseX < x + SLOT_SIZE - 2 && mouseY >= y && mouseY < y + SLOT_SIZE - 2;
        return isHovered && !stack.isEmpty() ? stack : hovered;
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
        var info = selected();
        if (info == null || info.isPartial() || this.minecraft == null || this.minecraft.player == null) return false;

        var stack = stackAt(info, mouseX, mouseY);
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

    private ItemStack stackAt(DeathInfo info, int mouseX, int mouseY) {
        // Mirrors the layout in renderInventory; kept in one place would need a slot
        // model, which is more machinery than two call sites justify.
        var items = info.getPlayerItems();
        var armor = info.getPlayerArmor();

        int x = this.detailX + 4;
        int y = detailInventoryTop(info);

        for (int i = 0; i < armor.size(); i++) {
            if (inSlot(mouseX, mouseY, x + i * SLOT_SIZE, y)) return armor.get(i);
        }

        int gridY = y + SLOT_SIZE + 4;
        for (int i = 0; i < items.size() && i < 36; i++) {
            if (inSlot(mouseX, mouseY, x + (i % 9) * SLOT_SIZE, gridY + (i / 9) * SLOT_SIZE)) return items.get(i);
        }

        if (items.size() > 36 && inSlot(mouseX, mouseY, x, gridY + 4 * SLOT_SIZE + 4)) return items.get(36);
        return null;
    }

    private int detailInventoryTop(DeathInfo info) {
        int rows = Math.min(info.getLeftColumnText().size(), info.getRightColumnText().size());
        return PANEL_PADDING + 4 + 14 + rows * 11 + 6;
    }

    private boolean inSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE - 2 && mouseY >= slotY && mouseY < slotY + SLOT_SIZE - 2;
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
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }

    public static void open(Screen parent, DirectDeathLogStorage storage, boolean canRestore) {
        var screen = new DeathLogScreen(parent, storage);
        Minecraft.getInstance().setScreen(screen);
        if (!canRestore) screen.disableRestoring();
    }
}
