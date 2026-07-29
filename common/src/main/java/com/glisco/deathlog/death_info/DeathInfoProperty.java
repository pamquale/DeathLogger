package com.glisco.deathlog.death_info;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public interface DeathInfoProperty {

    /**
     * Properties are stored as {@code {"type": <id>, "data": {...}}}. Keeping the
     * payload in its own field is what lets an unknown type round-trip through
     * {@link com.glisco.deathlog.death_info.properties.MissingDeathInfoProperty}
     * without losing the data an addon mod wrote.
     */
    Codec<DeathInfoProperty> CODEC = Identifier.CODEC.dispatch(
            "type",
            property -> property.getType().getId(),
            id -> DeathInfoPropertyTypes.byId(id).codec().fieldOf("data")
    );

    DeathInfoPropertyType<?> getType();

    default Component getName() {
        return DeathInfoPropertyType.decorateName(getType().getName());
    }

    /** The value as shown in the right-hand column of the detail panel. */
    Component formatted();

    /** Contributes to the string the search box filters on. */
    String toSearchableString();
}
