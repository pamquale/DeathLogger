package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.platform.Services;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Replaces the owo {@code @Config} model and its generated wrapper with a plain
 * JSON file at {@code config/deathlogger.json}.
 */
public final class DeathLogConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DeathLogConfig instance;

    private boolean screenshotsEnabled = true;
    private boolean useLegacyDeathDetection = false;

    public static DeathLogConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    public boolean screenshotsEnabled() {
        return this.screenshotsEnabled;
    }

    public boolean useLegacyDeathDetection() {
        return this.useLegacyDeathDetection;
    }

    public void setScreenshotsEnabled(boolean value) {
        this.screenshotsEnabled = value;
        save();
    }

    public void setUseLegacyDeathDetection(boolean value) {
        this.useLegacyDeathDetection = value;
        save();
    }

    private static Path file() {
        return Services.PLATFORM.getGameDirectory().resolve("config").resolve(DeathLog.MOD_ID + ".json");
    }

    private static DeathLogConfig load() {
        var path = file();
        if (!Files.exists(path)) return new DeathLogConfig();

        try (var reader = Files.newBufferedReader(path)) {
            var loaded = GSON.fromJson(reader, DeathLogConfig.class);
            return loaded != null ? loaded : new DeathLogConfig();
        } catch (IOException | RuntimeException e) {
            DeathLog.LOGGER.error("Failed to read the DeathLogger config, falling back to defaults", e);
            return new DeathLogConfig();
        }
    }

    public void save() {
        var path = file();
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            DeathLog.LOGGER.error("Failed to write the DeathLogger config", e);
        }
    }
}
