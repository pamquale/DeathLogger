package com.glisco.deathlog.compat;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.platform.Services;

/**
 * Loads the Better Statistics Screen menu entry, and only then.
 * <p>
 * The guard has to live in a class that mentions no BetterStats type of its own,
 * or the JVM would try to resolve them while verifying this class and fail when
 * the mod is absent.
 */
public final class BetterStatsIntegration {

    private static final String MOD_ID = "betterstats";

    private BetterStatsIntegration() {}

    public static void init() {
        if (!Services.PLATFORM.isModLoaded(MOD_ID)) return;

        try {
            DeathLogMenubarItem.register();
        } catch (Throwable e) {
            // A BetterStats rewrite costs the menu entry, never the game.
            DeathLog.LOGGER.warn("Failed to add the DeathLogger entry to the Better Statistics Screen menu bar", e);
        }
    }
}
