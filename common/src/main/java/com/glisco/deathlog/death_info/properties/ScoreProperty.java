package com.glisco.deathlog.death_info.properties;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.death_info.DeathInfoPropertyType;
import com.glisco.deathlog.death_info.RestorableDeathInfoProperty;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ScoreProperty implements RestorableDeathInfoProperty {

    private static final Codec<ScoreProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("score").forGetter(property -> property.score),
            Codec.INT.fieldOf("levels").forGetter(property -> property.levels),
            Codec.FLOAT.fieldOf("progress").forGetter(property -> property.progress),
            Codec.INT.fieldOf("xp").forGetter(property -> property.xp)
    ).apply(instance, ScoreProperty::new));

    private final int score;
    private final int levels;
    private final float progress;
    private final int xp;

    public ScoreProperty(int score, int levels, float progress, int xp) {
        this.score = score;
        this.levels = levels;
        this.progress = progress;
        this.xp = xp;
    }

    @Override
    public DeathInfoPropertyType<?> getType() {
        return Type.INSTANCE;
    }

    @Override
    public Component formatted() {
        return Component.translatable("deathlogger.deathinfoproperty.score.value", this.score, this.levels, this.xp);
    }

    @Override
    public String toSearchableString() {
        return this.xp + " " + this.levels;
    }

    @Override
    public void restore(ServerPlayer player) {
        player.experienceProgress = this.progress;
        player.setExperienceLevels(this.levels);
    }

    public static class Type extends DeathInfoPropertyType<ScoreProperty> {

        public static final Type INSTANCE = new Type();

        private Type() {
            super("deathlogger.deathinfoproperty.score", DeathLog.id("score"));
        }

        @Override
        public boolean displayedInInfoView() {
            return true;
        }

        @Override
        public Codec<ScoreProperty> codec() {
            return ScoreProperty.CODEC;
        }
    }
}
