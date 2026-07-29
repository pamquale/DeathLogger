package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

public class StringProperty implements DeathInfoProperty {

    private static final Codec<StringProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("translation_key").forGetter(property -> property.translationKey),
            Codec.STRING.fieldOf("data").forGetter(property -> property.data)
    ).apply(instance, StringProperty::new));

    private final String translationKey;
    private final String data;

    public StringProperty(String translationKey, String data) {
        this.translationKey = translationKey;
        this.data = data;
    }

    public String getValue() {
        return this.data;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.literal(this.data);
    }

    @Override
    public String toSearchableString() {
        return this.data;
    }

    @Override
    public Component getName() {
        return DeathInfoPropertyType.decorateName(Component.translatable(this.translationKey));
    }

    public static class Type extends DeathInfoPropertyType<StringProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.string", DeathLog.id("string"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public Codec<StringProperty> codec() {
            return StringProperty.CODEC;
        }
    }
}
