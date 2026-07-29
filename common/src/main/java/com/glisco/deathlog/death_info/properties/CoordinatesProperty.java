package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

public class CoordinatesProperty implements DeathInfoProperty {

    private static final Codec<CoordinatesProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("coordinates").forGetter(property -> property.coordinates)
    ).apply(instance, CoordinatesProperty::new));

    private final BlockPos coordinates;

    public CoordinatesProperty(BlockPos coordinates) {
        this.coordinates = coordinates;
    }

    public BlockPos getCoordinates() {
        return this.coordinates;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.translatable(
                "deathlogger.deathinfoproperty.coordinates.value",
                this.coordinates.getX(), this.coordinates.getY(), this.coordinates.getZ()
        );
    }

    @Override
    public String toSearchableString() {
        return this.coordinates.getX() + " " + this.coordinates.getY() + " " + this.coordinates.getZ();
    }

    public static class Type extends DeathInfoPropertyType<CoordinatesProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.coordinates", DeathLog.id("coordinates"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public Codec<CoordinatesProperty> codec() {
            return CoordinatesProperty.CODEC;
        }
    }
}
