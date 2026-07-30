package com.glisco.deathlog.neoforge;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.ClientDeathLogStorage;
import com.glisco.deathlog.client.DeathLogClient;
import com.glisco.deathlog.client.DeathLogClientNetworking;
import com.glisco.deathlog.compat.BetterStatsIntegration;
import com.glisco.deathlog.mixin.MinecraftServerAccessor;
import com.glisco.deathlog.network.DeathLogPacket;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.codec.digest.DigestUtils;

@Mod(value = DeathLog.MOD_ID, dist = Dist.CLIENT)
public class DeathLoggerNeoForgeClient {

    /** Unbound by default; the screen is reachable from the statistics screens. */
    public static final KeyMapping OPEN_DEATH_SCREEN = new KeyMapping(
            "key.deathlogger.death_screen", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC
    );

    private static ClientDeathLogStorage storage;

    public DeathLoggerNeoForgeClient(IEventBus modBus) {
        modBus.addListener(DeathLoggerNeoForgeClient::onRegisterKeyMappings);

        NeoForge.EVENT_BUS.addListener(DeathLoggerNeoForgeClient::onLoggingIn);
        NeoForge.EVENT_BUS.addListener(DeathLoggerNeoForgeClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(DeathLoggerNeoForgeClient::onClientTick);

        BetterStatsIntegration.init();
    }

    static void handle(DeathLogPacket packet) {
        DeathLogClientNetworking.handleClientbound(packet);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_DEATH_SCREEN);
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        storage = createStorage(Minecraft.getInstance());
        DeathLogCommon.setStorage(storage);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        storage = null;
        DeathLogCommon.setStorage(null);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_DEATH_SCREEN.consumeClick()) DeathLogClient.openScreen();
    }

    /**
     * One log per world or server, named after a hash of the world directory or the
     * server entry, so logs from different places never mix.
     */
    private static ClientDeathLogStorage createStorage(Minecraft client) {
        if (client.level == null) return null;

        final String locationName;
        final boolean multiplayer;

        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            locationName = ((MinecraftServerAccessor) client.getSingleplayerServer())
                    .deathlogger$getStorageSource().getLevelId();
            multiplayer = false;
        } else if (client.getCurrentServer() != null) {
            locationName = client.getCurrentServer().name;
            multiplayer = true;
        } else {
            locationName = "unknown";
            multiplayer = false;
        }

        var suffix = DigestUtils.sha1Hex(locationName).substring(0, 10);
        var file = FMLPaths.GAMEDIR.get()
                .resolve(DeathLog.MOD_ID)
                .resolve("deaths_" + suffix + ".dat");

        return new ClientDeathLogStorage(client.level.registryAccess(), file, locationName, multiplayer);
    }
}
