package com.glisco.deathlog.client;

import blue.endless.jankson.Jankson;
import io.wispforest.owo.config.ConfigWrapper;
import io.wispforest.owo.config.ConfigWrapper.BuilderConsumer;
import io.wispforest.owo.config.Option;
import io.wispforest.owo.util.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DeathLogConfig extends ConfigWrapper<com.glisco.deathlog.client.DeathLogConfigModel> {

    public final Keys keys = new Keys();

    private final Option<java.lang.Boolean> screenshotsEnabled = this.optionForKey(this.keys.screenshotsEnabled);
    private final Option<java.lang.Boolean> useLegacyDeathDetection = this.optionForKey(this.keys.useLegacyDeathDetection);

    private DeathLogConfig() {
        super(com.glisco.deathlog.client.DeathLogConfigModel.class);
    }

    private DeathLogConfig(BuilderConsumer consumer) {
        super(com.glisco.deathlog.client.DeathLogConfigModel.class, consumer);
    }

    public static DeathLogConfig createAndLoad() {
        var wrapper = new DeathLogConfig();
        wrapper.load();
        return wrapper;
    }

    public static DeathLogConfig createAndLoad(BuilderConsumer consumer) {
        var wrapper = new DeathLogConfig(consumer);
        wrapper.load();
        return wrapper;
    }

    public boolean screenshotsEnabled() {
        return screenshotsEnabled.value();
    }

    public void screenshotsEnabled(boolean value) {
        screenshotsEnabled.set(value);
    }

    public boolean useLegacyDeathDetection() {
        return useLegacyDeathDetection.value();
    }

    public void useLegacyDeathDetection(boolean value) {
        useLegacyDeathDetection.set(value);
    }


    public static class Keys {
        public final Option.Key screenshotsEnabled = new Option.Key("screenshotsEnabled");
        public final Option.Key useLegacyDeathDetection = new Option.Key("useLegacyDeathDetection");
    }
}

