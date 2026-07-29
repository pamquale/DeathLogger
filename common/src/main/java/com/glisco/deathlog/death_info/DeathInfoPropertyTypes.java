package com.glisco.deathlog.death_info;

import com.glisco.deathlog.death_info.properties.CoordinatesProperty;
import com.glisco.deathlog.death_info.properties.InventoryProperty;
import com.glisco.deathlog.death_info.properties.LocationProperty;
import com.glisco.deathlog.death_info.properties.MissingDeathInfoProperty;
import com.glisco.deathlog.death_info.properties.ScoreProperty;
import com.glisco.deathlog.death_info.properties.StringProperty;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of property types.
 * <p>
 * The Fabric version of this mod put these in a custom {@code Registry} built by
 * owo's {@code AutoRegistryContainer}. A plain map does the same job here and
 * needs no per-loader registration, which matters because the three loaders have
 * three different registry APIs.
 */
public final class DeathInfoPropertyTypes {

    private static final Map<Identifier, DeathInfoPropertyType<?>> TYPES = new LinkedHashMap<>();

    public static final DeathInfoPropertyType<InventoryProperty> INVENTORY = register(InventoryProperty.Type.INSTANCE);
    public static final DeathInfoPropertyType<CoordinatesProperty> COORDINATES = register(CoordinatesProperty.Type.INSTANCE);
    public static final DeathInfoPropertyType<LocationProperty> LOCATION = register(LocationProperty.Type.INSTANCE);
    public static final DeathInfoPropertyType<ScoreProperty> SCORE = register(ScoreProperty.Type.INSTANCE);
    public static final DeathInfoPropertyType<StringProperty> STRING = register(StringProperty.Type.INSTANCE);

    private DeathInfoPropertyTypes() {}

    public static <P extends DeathInfoProperty, T extends DeathInfoPropertyType<P>> T register(T type) {
        var previous = TYPES.putIfAbsent(type.getId(), type);
        if (previous != null) {
            throw new IllegalStateException("Duplicate death info property type: " + type.getId());
        }
        return type;
    }

    /**
     * Never returns null - an unregistered id yields a {@link MissingDeathInfoProperty.Type},
     * which keeps the raw payload so reading and re-saving a log written with an
     * addon mod installed does not destroy that mod's data.
     */
    public static DeathInfoPropertyType<?> byId(Identifier id) {
        var type = TYPES.get(id);
        return type != null ? type : new MissingDeathInfoProperty.Type(id);
    }

    public static Map<Identifier, DeathInfoPropertyType<?>> all() {
        return Collections.unmodifiableMap(TYPES);
    }

    /** Forces the static initializer to run before the first log is read. */
    public static void init() {}
}
