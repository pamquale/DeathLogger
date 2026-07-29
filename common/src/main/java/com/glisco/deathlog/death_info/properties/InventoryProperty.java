package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class InventoryProperty implements RestorableDeathInfoProperty {

    /** 36 main slots plus the offhand, which the vanilla inventory keeps at index 40. */
    private static final int ITEM_COUNT = 37;
    private static final int ARMOR_COUNT = 4;
    private static final int ARMOR_START = 36;
    private static final int OFFHAND_SLOT = 40;

    private static final Codec<InventoryProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            stackList(ITEM_COUNT).fieldOf("items").forGetter(property -> property.playerItems),
            stackList(ARMOR_COUNT).fieldOf("armor").forGetter(property -> property.playerArmor)
    ).apply(instance, InventoryProperty::new));

    private final NonNullList<ItemStack> playerItems;
    private final NonNullList<ItemStack> playerArmor;

    public InventoryProperty(NonNullList<ItemStack> playerItems, NonNullList<ItemStack> playerArmor) {
        this.playerItems = playerItems;
        this.playerArmor = playerArmor;
    }

    public InventoryProperty(Inventory inventory) {
        this.playerItems = NonNullList.withSize(ITEM_COUNT, ItemStack.EMPTY);
        this.playerArmor = NonNullList.withSize(ARMOR_COUNT, ItemStack.EMPTY);

        for (int i = 0; i < ARMOR_COUNT; i++) {
            this.playerArmor.set(i, inventory.getItem(ARMOR_START + i).copy());
        }
        for (int i = 0; i < ARMOR_START; i++) {
            this.playerItems.set(i, inventory.getItem(i).copy());
        }
        this.playerItems.set(ITEM_COUNT - 1, inventory.getItem(OFFHAND_SLOT).copy());
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.empty();
    }

    @Override
    public String toSearchableString() {
        var builder = new StringBuilder();

        this.playerItems.forEach(stack -> builder.append(stack.getHoverName().getString()));
        this.playerArmor.forEach(stack -> builder.append(stack.getHoverName().getString()));

        return builder.toString();
    }

    @Override
    public void restore(ServerPlayer player) {
        final var inventory = player.getInventory();
        inventory.clearContent();

        for (int i = 0; i < ARMOR_COUNT; i++) {
            inventory.setItem(ARMOR_START + i, this.playerArmor.get(i).copy());
        }
        for (int i = 0; i < ARMOR_START; i++) {
            inventory.setItem(i, this.playerItems.get(i).copy());
        }
        inventory.setItem(OFFHAND_SLOT, this.playerItems.get(ITEM_COUNT - 1).copy());
    }

    public NonNullList<ItemStack> getPlayerArmor() {
        return this.playerArmor;
    }

    public NonNullList<ItemStack> getPlayerItems() {
        return this.playerItems;
    }

    /**
     * Decodes into a fixed-size {@link NonNullList} so short or corrupt lists cannot
     * make the GUI index out of bounds. OPTIONAL_CODEC is required because empty
     * slots are stored too.
     */
    private static Codec<NonNullList<ItemStack>> stackList(int size) {
        return ItemStack.OPTIONAL_CODEC.listOf().xmap(
                stacks -> {
                    var list = NonNullList.withSize(size, ItemStack.EMPTY);
                    for (int i = 0; i < Math.min(size, stacks.size()); i++) {
                        list.set(i, stacks.get(i));
                    }
                    return list;
                },
                List::copyOf
        );
    }

    public static class Type extends DeathInfoPropertyType<InventoryProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.inventory", DeathLog.id("inventory"));
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public Codec<InventoryProperty> codec() {
            return InventoryProperty.CODEC;
        }
    }
}
