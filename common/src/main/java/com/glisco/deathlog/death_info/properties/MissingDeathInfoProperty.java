package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Stand-in for a property whose type is not registered, e.g. one written while
 * an addon mod was installed. It holds the payload verbatim and writes it back
 * unchanged, so loading and re-saving a log never destroys another mod's data.
 */
public class MissingDeathInfoProperty implements DeathInfoProperty {

    private final Type type;
    private final CompoundTag data;

    public MissingDeathInfoProperty(Type type, CompoundTag data) {
        this.type = type;
        this.data = data;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return this.type;
    }

    @Override
    public Component formatted() {
        return Component.empty();
    }

    @Override
    public String toSearchableString() {
        return "";
    }

    public static class Type extends DeathInfoPropertyType<MissingDeathInfoProperty> {

        private final Codec<MissingDeathInfoProperty> codec =
                CompoundTag.CODEC.xmap(tag -> new MissingDeathInfoProperty(this, tag), property -> property.data);

        public Type(Identifier id) {
            super("deathlogger.deathinfoproperty.missing", id);
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public Codec<MissingDeathInfoProperty> codec() {
            return this.codec;
        }
    }
}
