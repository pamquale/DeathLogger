package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.platform.Services;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.storage.LevelResource;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Client-side entry point shared by every way of opening the screen: the key
 * mapping, the vanilla statistics screen and the BetterStats menu.
 */
public final class DeathLogClient {

    private DeathLogClient() {}

    /**
     * One log per world or server, named after a hash of the world directory or the
     * server entry, so logs from different places never mix.
     * <p>
     * The singleplayer world name comes from the public {@code getWorldPath} rather
     * than an accessor mixin on {@code MinecraftServer}. The mixin worked on Fabric
     * and NeoForge but silently did not apply on Forge, which declares mixin configs
     * in the jar manifest instead of its mods.toml - so every join there died on a
     * ClassCastException.
     */
    public static ClientDeathLogStorage createStorage() {
        var client = Minecraft.getInstance();
        if (client.level == null) return null;

        final String locationName;
        final boolean multiplayer;

        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            locationName = client.getSingleplayerServer()
                    .getWorldPath(LevelResource.ROOT).getFileName().toString();
            multiplayer = false;
        } else if (client.getCurrentServer() != null) {
            locationName = client.getCurrentServer().name;
            multiplayer = true;
        } else {
            locationName = "unknown";
            multiplayer = false;
        }

        var suffix = DigestUtils.sha1Hex(locationName).substring(0, 10);
        var file = Services.PLATFORM.getGameDirectory()
                .resolve(DeathLog.MOD_ID)
                .resolve("deaths_" + suffix + ".dat");

        return new ClientDeathLogStorage(client.level.registryAccess(), file, locationName, multiplayer);
    }

    public static DirectDeathLogStorage getClientStorage() {
        return DeathLogCommon.getStorage() instanceof DirectDeathLogStorage direct ? direct : null;
    }

    public static void openScreen() {
        var storage = getClientStorage();
        if (storage == null) return;

        var client = Minecraft.getInstance();
        DeathLogScreen.open(client.screen, storage, client.getCurrentServer() == null);
    }
}
