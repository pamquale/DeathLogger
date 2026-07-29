package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

public class LocationProperty implements DeathInfoProperty {

    private static final Codec<LocationProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("location").forGetter(property -> property.location),
            Codec.BOOL.fieldOf("multiplayer").forGetter(property -> property.multiplayer)
    ).apply(instance, LocationProperty::new));

    private final String location;
    private final boolean multiplayer;

    public LocationProperty(String location, boolean multiplayer) {
        this.location = location;
        this.multiplayer = multiplayer;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.translatable(
                "deathlogger.deathinfoproperty.location.value", this.location,
                this.multiplayer
                        ? Component.translatable("deathlogger.deathinfoproperty.location.multiplayer")
                        : Component.translatable("deathlogger.deathinfoproperty.location.singleplayer")
        );
    }

    @Override
    public String toSearchableString() {
        return this.location;
    }

    public static class Type extends DeathInfoPropertyType<LocationProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.location", DeathLog.id("location"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public Codec<LocationProperty> codec() {
            return LocationProperty.CODEC;
        }
    }
}
