package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoProperty;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * The file name of the screenshot taken at the moment of death, relative to
 * {@code <gamedir>/deathlogger/screenshots}.
 * <p>
 * The Fabric build stored no link at all and instead scanned the screenshots
 * folder for a file whose mtime fell within 30 seconds of the recorded time.
 * Storing the name removes both the guesswork and the folder scan.
 */
public class ScreenshotProperty implements DeathInfoProperty {

    private static final Codec<ScreenshotProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("file").forGetter(property -> property.fileName)
    ).apply(instance, ScreenshotProperty::new));

    private final String fileName;

    public ScreenshotProperty(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return this.fileName;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.literal(this.fileName);
    }

    @Override
    public String toSearchableString() {
        return "";
    }

    public static class Type extends DeathInfoPropertyType<ScreenshotProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.screenshot", DeathLog.id("screenshot"));
        }

        @Override
        public boolean displayedInInfoView() {
            return false;
        }

        @Override
        public Codec<ScreenshotProperty> codec() {
            return ScreenshotProperty.CODEC;
        }
    }
}
