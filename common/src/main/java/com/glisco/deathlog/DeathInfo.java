package com.glisco.deathlog;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import com.glisco.deathlog.death_info.properties.InventoryProperty;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.function.Consumer;

/**
 * One recorded death: an ordered map of properties keyed by the {@code *_KEY}
 * constants below. There are no typed fields, so an addon mod can contribute
 * properties without this class knowing about them.
 */
public class DeathInfo {

    public static final String COORDINATES_KEY = "coordinates";
    public static final String DIMENSION_KEY = "dimension";
    public static final String LOCATION_KEY = "location";
    public static final String SCORE_KEY = "score";
    public static final String DEATH_MESSAGE_KEY = "death_message";
    public static final String TIME_OF_DEATH_KEY = "time_of_death";
    public static final String INVENTORY_KEY = "inventory";
    public static final String SCREENSHOT_KEY = "screenshot";

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("key").forGetter(Entry::key),
            DeathInfoProperty.CODEC.fieldOf("property").forGetter(Entry::property)
    ).apply(instance, Entry::new));

    /**
     * A list rather than a map because property order drives the detail panel and
     * {@code Codec.unboundedMap} gives no ordering guarantee.
     */
    public static final Codec<DeathInfo> CODEC =
            ENTRY_CODEC.listOf().xmap(DeathInfo::fromEntries, DeathInfo::toEntries);

    /**
     * Drops heavy properties on encode <em>and</em> decode, so a death list can be
     * sent to a client without shipping full inventories with it. The client then
     * asks for the complete entry when the player selects one.
     */
    public static final Codec<DeathInfo> PARTIAL_CODEC = ENTRY_CODEC.listOf().xmap(
            entries -> fromEntries(stripHeavyProperties(entries)),
            info -> stripHeavyProperties(info.toEntries())
    );

    private final SequencedMap<String, DeathInfoProperty> properties;

    public DeathInfo() {
        this.properties = new LinkedHashMap<>();
    }

    private DeathInfo(SequencedMap<String, DeathInfoProperty> properties) {
        this.properties = properties;
    }

    public void restore(ServerPlayer player) {
        this.properties.values().stream()
                .filter(property -> property instanceof RestorableDeathInfoProperty)
                .forEach(property -> ((RestorableDeathInfoProperty) property).restore(player));
    }

    public void setProperty(String key, DeathInfoProperty value) {
        this.properties.put(key, value);
    }

    public Optional<DeathInfoProperty> getProperty(String key) {
        return Optional.ofNullable(this.properties.get(key));
    }

    /** True when the inventory was stripped, i.e. this came from {@link #PARTIAL_CODEC}. */
    public boolean isPartial() {
        return getProperty(INVENTORY_KEY).isEmpty();
    }

    public Component getListName() {
        return getProperty(TIME_OF_DEATH_KEY)
                .map(DeathInfoProperty::formatted)
                .orElseGet(() -> Component.translatable("text.deathlogger.info.time_missing"));
    }

    public Component getTitle() {
        return getProperty(DEATH_MESSAGE_KEY)
                .map(DeathInfoProperty::formatted)
                .orElseGet(() -> Component.translatable("text.deathlogger.info.death_message_missing"));
    }

    public List<Component> getLeftColumnText() {
        final var texts = new ArrayList<Component>();
        iterateDisplayProperties(property -> texts.add(property.getName()));
        return texts;
    }

    public List<Component> getRightColumnText() {
        final var texts = new ArrayList<Component>();
        iterateDisplayProperties(property -> texts.add(property.formatted()));
        return texts;
    }

    public String createSearchString() {
        final var builder = new StringBuilder();
        this.properties.forEach((key, property) -> builder.append(property.toSearchableString()));
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private void iterateDisplayProperties(Consumer<DeathInfoProperty> callback) {
        this.properties.forEach((key, property) -> {
            if (!property.getType().displayedInInfoView()) return;
            callback.accept(property);
        });
    }

    public NonNullList<ItemStack> getPlayerArmor() {
        return getProperty(INVENTORY_KEY)
                .map(property -> ((InventoryProperty) property).getPlayerArmor())
                .orElseGet(NonNullList::create);
    }

    public NonNullList<ItemStack> getPlayerItems() {
        return getProperty(INVENTORY_KEY)
                .map(property -> ((InventoryProperty) property).getPlayerItems())
                .orElseGet(NonNullList::create);
    }

    private List<Entry> toEntries() {
        final var entries = new ArrayList<Entry>(this.properties.size());
        this.properties.forEach((key, property) -> entries.add(new Entry(key, property)));
        return entries;
    }

    private static DeathInfo fromEntries(List<Entry> entries) {
        final SequencedMap<String, DeathInfoProperty> properties = new LinkedHashMap<>();
        entries.forEach(entry -> properties.put(entry.key(), entry.property()));
        return new DeathInfo(properties);
    }

    private static List<Entry> stripHeavyProperties(List<Entry> entries) {
        return entries.stream()
                .filter(entry -> !(entry.property() instanceof InventoryProperty))
                .toList();
    }

    private record Entry(String key, DeathInfoProperty property) {}
}
