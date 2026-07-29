package com.glisco.deathlog.fabric;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.ClientDeathLogStorage;
import com.glisco.deathlog.client.DeathLogClientNetworking;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.fabric.network.DeathLogPayloads;
import com.glisco.deathlog.mixin.MinecraftServerAccessor;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.apache.commons.codec.digest.DigestUtils;
import org.lwjgl.glfw.GLFW;

public class DeathLoggerFabricClient implements ClientModInitializer {

    private static ClientDeathLogStorage storage;

    public static final KeyMapping OPEN_DEATH_SCREEN = new KeyMapping(
            "key.deathlogger.death_screen", GLFW.GLFW_KEY_END, KeyMapping.Category.MISC
    );

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(OPEN_DEATH_SCREEN);

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            storage = createStorage(client);
            DeathLogCommon.setStorage(storage);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            storage = null;
            DeathLogCommon.setStorage(null);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_DEATH_SCREEN.consumeClick()) openScreen();
        });

        ClientPlayNetworking.registerGlobalReceiver(DeathLogPayloads.OpenScreen.TYPE, (payload, context) ->
                context.client().execute(() -> DeathLogClientNetworking.handleClientbound(payload.packet())));
        ClientPlayNetworking.registerGlobalReceiver(DeathLogPayloads.DeathInfoData.TYPE, (payload, context) ->
                context.client().execute(() -> DeathLogClientNetworking.handleClientbound(payload.packet())));
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
        var file = FabricLoader.getInstance().getGameDir()
                .resolve(DeathLog.MOD_ID)
                .resolve("deaths_" + suffix + ".dat");

        return new ClientDeathLogStorage(client.level.registryAccess(), file, locationName, multiplayer);
    }

    public static DirectDeathLogStorage getClientStorage() {
        return storage;
    }

    public static void openScreen() {
        if (storage == null) return;

        var client = Minecraft.getInstance();
        DeathLogScreen.open(client.screen, storage, client.getCurrentServer() == null);
    }
}
