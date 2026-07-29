package com.glisco.deathlog.death_info;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * Describes one kind of {@link DeathInfoProperty}. Types are looked up by
 * {@link Identifier} in {@link DeathInfoPropertyTypes} and drive the dispatch
 * codec used to (de)serialize properties.
 */
public abstract class DeathInfoPropertyType<P extends DeathInfoProperty> {

    private final String translationKey;
    private final Identifier id;

    protected DeathInfoPropertyType(String translationKey, Identifier id) {
        this.translationKey = translationKey;
        this.id = id;
    }

    public MutableComponent getName() {
        return Component.translatable(this.translationKey);
    }

    public static Component decorateName(MutableComponent name) {
        return name.withStyle(ChatFormatting.BLUE);
    }

    public Identifier getId() {
        return this.id;
    }

    /** Whether this property shows up in the detail panel's two-column list. */
    public abstract boolean displayedInInfoView();

    public abstract Codec<P> codec();
}
