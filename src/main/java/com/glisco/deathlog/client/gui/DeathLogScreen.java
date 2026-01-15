package com.glisco.deathlog.client.gui;

import com.glisco.deathlog.client.DeathInfo;
import com.glisco.deathlog.death_info.properties.StringProperty;
import com.glisco.deathlog.network.RemoteDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.owo.config.ui.ConfigScreen;
import io.wispforest.owo.ui.base.BaseUIModelScreen;
import io.wispforest.owo.ui.component.*;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.GridLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.parsing.UIParsing;
import io.wispforest.owo.util.Observable;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.component.Component;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BundleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DeathLogScreen extends BaseUIModelScreen<FlowLayout> {

    private final Screen parent;
    private final DirectDeathLogStorage storage;

    private FlowLayout detailPanel;

    private final Observable<String> currentSearchTerm = Observable.of("");
    private boolean canRestore = true;

    // Screenshot texture management
    private NativeImageBackedTexture screenshotTexture;
    private Identifier screenshotTextureId;

    public DeathLogScreen(Screen parent, DirectDeathLogStorage storage) {
        super(FlowLayout.class, DataSource.asset(Identifier.of("deathlogger", "deathlogger")));
        this.parent = parent;
        this.storage = storage;

        this.currentSearchTerm.observe(s -> {
            this.buildDeathList();
        });
    }

    @Override
    protected void init() {
        super.init();

        var configButton = this.uiAdapter.rootComponent.childById(ButtonComponent.class, "config-button");
        if (configButton != null) {
            if (this.height >= 275) {
                configButton.positioning(Positioning.relative(100, 100)).margins(Insets.none());
            } else {
                configButton.positioning(Positioning.relative(100, 0)).margins(Insets.top(-5));
            }
        }
    }

    @Override
    @SuppressWarnings("DataFlowIssue")
    protected void build(FlowLayout rootComponent) {
        this.detailPanel = rootComponent.childById(FlowLayout.class, "detail-panel");

        rootComponent.childById(TextBoxComponent.class, "search-box").<TextBoxComponent>configure(searchBox -> {
            searchBox.onChanged().subscribe(value -> {
                this.currentSearchTerm.set(value.toLowerCase(Locale.ROOT));
            });
        });

        rootComponent.childById(ButtonComponent.class, "config-button").onPress(button -> {
            this.client.setScreen(ConfigScreen.getProvider("deathlogger").apply(this));
        });

        this.uiAdapter.rootComponent.childById(LabelComponent.class, "death-count-label").text(
                Text.translatable("text.deathlogger.death_list_title", this.storage.getDeathInfoList().size())
        );

        this.buildDeathList();
    }

    public void updateInfo(DeathInfo info, int index) {
        this.storage.getDeathInfoList().set(index, info);
        this.selectInfo(this.storage.getDeathInfoList().get(index));
    }

    public void disableRestoring() {
        this.canRestore = false;
    }

    private void buildDeathList() {
        this.uiAdapter.rootComponent.childById(FlowLayout.class, "death-list").<FlowLayout>configure(deathList -> {
            deathList.clearChildren();

            for (int i = 0; i < this.storage.getDeathInfoList().size(); i++) {
                final int infoIndex = i;
                var deathInfo = this.storage.getDeathInfoList().get(infoIndex);

                if (!this.currentSearchTerm.get().isBlank() && !deathInfo.createSearchString().contains(this.currentSearchTerm.get())) {
                    continue;
                }

                deathList.child(this.model.expandTemplate(
                        DeathListEntryContainer.class,
                        "death-list-entry",
                        Map.of(
                                "death-time", deathInfo.getListName().getString(),
                                "death-message", deathInfo.getTitle().getString()
                        )
                ).<DeathListEntryContainer>configure(container -> {
                    container.onSelected().subscribe(c -> {
                        this.selectInfo(this.storage.getDeathInfoList().get(infoIndex));
                    });

                    container.mouseDown().subscribe((mouseX, mouseY, button) -> {
                        if (button != GLFW.GLFW_MOUSE_BUTTON_RIGHT) return false;

                        var root = this.uiAdapter.rootComponent;
                        DropdownComponent.openContextMenu(
                                this,
                                root, FlowLayout::child,
                                container.x() - root.padding().get().left() + mouseX,
                                container.y() - root.padding().get().top() + mouseY,
                                dropdown -> {
                                    dropdown.surface(Surface.blur(3, 5).and(Surface.flat(0xC7000000)).and(Surface.outline(0xFF121212)));
                                    dropdown.zIndex(100);

                                    if (this.canRestore) {
                                        dropdown.button(Text.translatable("text.deathlogger.action.restore"), dropdown_ -> {
                                            this.storage.restore(infoIndex);
                                            dropdown.remove();
                                        });
                                    }

                                    dropdown.button(Text.translatable("text.deathlogger.action.delete"), dropdown_ -> {
                                        this.storage.delete(deathInfo);
                                        this.buildDeathList();
                                        dropdown.remove();
                                    });
                                }
                        );

                        return true;
                    });
                }));
            }
        });
    }

    private void selectInfo(DeathInfo info) {
        if (this.storage instanceof RemoteDeathLogStorage remoteStorage) remoteStorage.fetchCompleteInfo(info);

        this.detailPanel.<FlowLayout>configure(panel -> {
            panel.clearChildren();

            if (info.isPartial()) {
                panel.child(Components.label(Text.translatable("text.deathlogger.death_info_loading")).margins(Insets.top(15)));
                return;
            }

            panel.child(Components.label(info.getTitle()).shadow(true).margins(Insets.of(15, 10, 0, 0)));

            FlowLayout leftColumn;
            FlowLayout rightColumn;
            panel.child(Containers.horizontalFlow(Sizing.content(), Sizing.content())
                    .child(leftColumn = Containers.verticalFlow(Sizing.content(), Sizing.content()))
                    .child(rightColumn = Containers.verticalFlow(Sizing.content(), Sizing.content())));

            leftColumn.gap(2);
            for (var text : info.getLeftColumnText()) {
                leftColumn.child(Components.label(text).shadow(true));
            }

            rightColumn.gap(2).margins(Insets.left(5));
            for (var text : info.getRightColumnText()) {
                rightColumn.child(Components.label(text));
            }

            FlowLayout itemContainer;
            panel.child(itemContainer = Containers.verticalFlow(Sizing.content(), Sizing.content()));
            itemContainer.margins(Insets.top(5));

            itemContainer.child(Components.texture(Identifier.of("deathlogger", "textures/gui/inventory_overlay.png"), 0, 0, 210, 107));

            FlowLayout armorFlow;
            itemContainer.child(armorFlow = Containers.verticalFlow(Sizing.content(), Sizing.content()));

            armorFlow.positioning(Positioning.absolute(185, 28));
            for (int i = 0; i < info.getPlayerArmor().size(); i++) {
                armorFlow.child(0, this.makeItem(info.getPlayerArmor().get(i), Insets.of(1)));
            }

            GridLayout itemGrid;
            itemContainer.child(itemGrid = Containers.grid(Sizing.content(), Sizing.content(), 4, 9));

            var inventory = info.getPlayerItems();

            itemGrid.positioning(Positioning.absolute(7, 24));
            for (int i = 0; i < 9; i++) {
                itemGrid.child(this.makeItem(inventory.get(i), Insets.of(5, 1, 1, 1)), 3, i);
            }

            for (int i = 0; i < 27; i++) {
                itemGrid.child(this.makeItem(inventory.get(9 + i), Insets.of(1)), i / 9, i % 9);
            }

            if (!inventory.get(36).isEmpty()) {
                itemContainer.child(this.makeItem(inventory.get(36), Insets.none()).positioning(Positioning.absolute(186, 8)));
            }
            
            // Scroll hint label
            panel.child(Components.label(Text.translatable("text.deathlogger.scroll_hint"))
                    .color(Color.ofFormatting(Formatting.DARK_GRAY))
                    .horizontalTextAlignment(io.wispforest.owo.ui.core.HorizontalAlignment.CENTER)
                    .margins(Insets.of(8, 5, 0, 0)));
            
            // Add screenshot display under inventory
            addScreenshotDisplay(panel, info);
            
            // Add extra containers (Shulkers and Bundles) at the bottom
            addExtraContainers(panel, inventory, info.getPlayerArmor());
        });
    }
    
    /**
     * Renders full contents of all Shulker Boxes and Bundles from the inventory
     */
    private void addExtraContainers(FlowLayout panel, List<ItemStack> inventory, List<ItemStack> armor) {
        List<ItemStack> allItems = new ArrayList<>();
        allItems.addAll(inventory);
        allItems.addAll(armor);
        
        boolean hasContainers = false;
        
        for (ItemStack stack : allItems) {
            if (stack.isEmpty()) continue;
            
            // Check for Shulker Box (CONTAINER component)
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                ContainerComponent containerComponent = stack.get(DataComponentTypes.CONTAINER);
                if (containerComponent != null) {
                    List<ItemStack> contents = containerComponent.stream().toList();
                    if (!contents.isEmpty() && contents.stream().anyMatch(s -> !s.isEmpty())) {
                        if (!hasContainers) {
                            panel.child(Components.label(Text.translatable("text.deathlogger.extra_containers"))
                                    .shadow(true)
                                    .margins(Insets.of(15, 5, 0, 0)));
                            hasContainers = true;
                        }
                        addContainerPreview(panel, stack, contents, 27); // Shulker has 27 slots
                    }
                }
            }
            
            // Check for Bundle (BUNDLE_CONTENTS component)
            if (stack.getItem() instanceof BundleItem) {
                BundleContentsComponent bundleComponent = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (bundleComponent != null) {
                    List<ItemStack> contents = bundleComponent.stream().toList();
                    if (!contents.isEmpty()) {
                        if (!hasContainers) {
                            panel.child(Components.label(Text.translatable("text.deathlogger.extra_containers"))
                                    .shadow(true)
                                    .margins(Insets.of(15, 5, 0, 0)));
                            hasContainers = true;
                        }
                        addContainerPreview(panel, stack, contents, -1); // Bundle has variable size
                    }
                }
            }
        }
    }
    
    /**
     * Creates a visual preview of a container's contents
     */
    private void addContainerPreview(FlowLayout panel, ItemStack containerStack, List<ItemStack> contents, int expectedSlots) {
        // Container header with item name (colored by rarity)
        Text containerName = containerStack.getName().copy().formatted(containerStack.getRarity().getFormatting());
        
        FlowLayout containerSection = Containers.verticalFlow(Sizing.content(), Sizing.content());
        containerSection.margins(Insets.of(8, 5, 0, 5));
        containerSection.surface(Surface.flat(0x40000000));
        containerSection.padding(Insets.of(5));
        
        // Header row with icon and name
        FlowLayout headerRow = Containers.horizontalFlow(Sizing.content(), Sizing.content());
        headerRow.child(Components.item(containerStack).sizing(Sizing.fixed(16), Sizing.fixed(16)));
        headerRow.child(Components.label(containerName).shadow(true).margins(Insets.left(4)));
        headerRow.verticalAlignment(io.wispforest.owo.ui.core.VerticalAlignment.CENTER);
        containerSection.child(headerRow);
        
        // Filter out empty stacks and create grid
        List<ItemStack> nonEmptyContents = contents.stream().filter(s -> !s.isEmpty()).toList();
        
        if (nonEmptyContents.isEmpty()) {
            containerSection.child(Components.label(Text.translatable("text.deathlogger.container_empty"))
                    .color(Color.ofFormatting(Formatting.GRAY))
                    .margins(Insets.of(3, 0, 0, 0)));
        } else {
            // Calculate grid dimensions (9 columns for shulker, dynamic for bundle)
            int columns = expectedSlots > 0 ? 9 : Math.min(9, nonEmptyContents.size());
            int rows = (int) Math.ceil((double) nonEmptyContents.size() / columns);
            
            GridLayout itemGrid = Containers.grid(Sizing.content(), Sizing.content(), rows, columns);
            itemGrid.margins(Insets.top(3));
            
            for (int i = 0; i < nonEmptyContents.size(); i++) {
                ItemStack contentStack = nonEmptyContents.get(i);
                int row = i / columns;
                int col = i % columns;
                itemGrid.child(makeContainerItem(contentStack), row, col);
            }
            
            containerSection.child(itemGrid);
        }
        
        panel.child(containerSection);
    }
    
    /**
     * Creates an item component for container contents (simpler than main inventory items)
     */
    private ItemComponent makeContainerItem(ItemStack stack) {
        var item = Components.item(stack).showOverlay(true);
        item.margins(Insets.of(1));
        
        if (!stack.isEmpty()) {
            var tooltip = new ArrayList<>(stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, 
                    client.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC));
            item.tooltip(tooltip);
        }
        
        return item;
    }

    private ItemComponent makeItem(ItemStack stack, Insets margins) {
        var item = Components.item(stack).showOverlay(true);
        item.margins(margins);

        if (!stack.isEmpty()) {
            var tooltip = new ArrayList<>(stack.getTooltip(Item.TooltipContext.DEFAULT, client.player, client.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC));
            
            tooltip.add(Text.translatable(this.client.player.isCreative() ? "text.deathlogger.action.give_item.spawn" : "text.deathlogger.action.give_item.copy_give"));
            item.tooltip(tooltip);

            item.mouseDown().subscribe((mouseX, mouseY, button) -> {
                if (button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return false;

                if (this.client.player.isCreative()) {
                    this.client.interactionManager.dropCreativeStack(stack);
                } else {
                    var command = "/give " + client.player.getName().getString() +
                            " " +
                            Registries.ITEM.getId(stack.getItem());

                    var ops = storage.registries().getOps(NbtOps.INSTANCE);
                    var components = stack.getComponentChanges().entrySet().stream().flatMap(entry -> {
                        var componentType = entry.getKey();
                        var typeId = Registries.DATA_COMPONENT_TYPE.getId(componentType);
                        if (typeId == null) return Stream.empty();

                        var componentOptional = entry.getValue();
                        if (componentOptional.isPresent()) {
                            Component<?> component = Component.of(componentType, componentOptional.get());
                            return component.encode(ops).result().stream().map(value -> typeId + "=" + value);
                        } else {
                            return Stream.of("!" + typeId);
                        }
                    }).collect(Collectors.joining(String.valueOf(',')));

                    if (!components.isEmpty()) {
                        command += "[" + components + "]";
                    }

                    if (stack.getCount() > 1) {
                        command += " " + stack.getCount();
                    }

                    this.client.keyboard.setClipboard(command);
                }

                return true;
            });
        }

        return item;
    }

    /**
     * Attempts to find and display a screenshot from around the time of death
     */
    private void addScreenshotDisplay(FlowLayout panel, DeathInfo info) {
        // Clean up previous screenshot texture
        cleanupScreenshotTexture();
        
        var timeProperty = info.getProperty(DeathInfo.TIME_OF_DEATH_KEY);
        if (timeProperty.isEmpty()) return;
        
        String timeString = ((StringProperty) timeProperty.get()).getValue();
        
        // Parse the death time
        Date deathTime;
        try {
            // The time format used is Date.toString() format
            SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
            deathTime = format.parse(timeString);
        } catch (ParseException e) {
            return; // Can't parse time, skip screenshot
        }
        
        // Find screenshot file
        File screenshotsDir = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
        if (!screenshotsDir.exists() || !screenshotsDir.isDirectory()) return;
        
        File[] screenshots = screenshotsDir.listFiles((dir, name) -> name.endsWith(".png"));
        if (screenshots == null || screenshots.length == 0) return;
        
        // Find the closest screenshot within 30 seconds of death
        File closestScreenshot = null;
        long closestDiff = Long.MAX_VALUE;
        long deathTimeMs = deathTime.getTime();
        
        for (File screenshot : screenshots) {
            long lastModified = screenshot.lastModified();
            long diff = Math.abs(lastModified - deathTimeMs);
            
            // Within 30 seconds of death
            if (diff < 30000 && diff < closestDiff) {
                closestDiff = diff;
                closestScreenshot = screenshot;
            }
        }
        
        if (closestScreenshot == null) return;
        
        try (InputStream stream = new FileInputStream(closestScreenshot)) {
            NativeImage image = NativeImage.read(stream);
            
            // Create texture
            screenshotTexture = new NativeImageBackedTexture(image);
            screenshotTextureId = Identifier.of("deathlogger", "death_screenshot_" + System.currentTimeMillis());
            MinecraftClient.getInstance().getTextureManager().registerTexture(screenshotTextureId, screenshotTexture);
            
            // Calculate display size (max width 200, maintain aspect ratio)
            int maxWidth = 200;
            int maxHeight = 120;
            float aspectRatio = (float) image.getWidth() / image.getHeight();
            
            int displayWidth = maxWidth;
            int displayHeight = (int) (maxWidth / aspectRatio);
            
            if (displayHeight > maxHeight) {
                displayHeight = maxHeight;
                displayWidth = (int) (maxHeight * aspectRatio);
            }
            
            // Add screenshot section
            panel.child(Components.label(Text.translatable("text.deathlogger.death_screenshot"))
                    .shadow(true)
                    .margins(Insets.of(10, 5, 0, 0)));
            
            panel.child(Components.texture(screenshotTextureId, 0, 0, image.getWidth(), image.getHeight(), image.getWidth(), image.getHeight())
                    .sizing(Sizing.fixed(displayWidth), Sizing.fixed(displayHeight))
                    .margins(Insets.of(5, 0, 0, 5)));
            
        } catch (Exception e) {
            // Failed to load screenshot, just skip it
        }
    }
    
    private void cleanupScreenshotTexture() {
        if (screenshotTexture != null) {
            screenshotTexture.close();
            screenshotTexture = null;
        }
        if (screenshotTextureId != null) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(screenshotTextureId);
            screenshotTextureId = null;
        }
    }

    @Override
    public void close() {
        cleanupScreenshotTexture();
        this.client.setScreen(this.parent);
    }

    static {
        UIParsing.registerFactory(Identifier.of("deathlogger", "death-list-entry-container"), element -> new DeathListEntryContainer());
    }
}
