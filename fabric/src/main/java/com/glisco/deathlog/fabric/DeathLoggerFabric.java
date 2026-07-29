package com.glisco.deathlog.fabric;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.command.DeathLogCommands;
import com.glisco.deathlog.fabric.network.DeathLogPayloads;
import com.glisco.deathlog.network.DeathLogNetworking;
import com.glisco.deathlog.storage.ServerDeathLogStorage;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class DeathLoggerFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        registerPayloads();
        registerReceivers();

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            if (!server.isDedicatedServer()) return;
            DeathLogCommon.setStorage(new ServerDeathLogStorage(
                    server.registryAccess(),
                    FabricLoader.getInstance().getGameDir().resolve("deaths")
            ));
        });

        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> DeathLogCommands.register(dispatcher)
        );
    }

    private static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(DeathLogPayloads.OpenScreen.TYPE, DeathLogPayloads.OpenScreen.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DeathLogPayloads.DeathInfoData.TYPE, DeathLogPayloads.DeathInfoData.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeathLogPayloads.InfoRequest.TYPE, DeathLogPayloads.InfoRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeathLogPayloads.RestoreRequest.TYPE, DeathLogPayloads.RestoreRequest.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(DeathLogPayloads.DeletionRequest.TYPE, DeathLogPayloads.DeletionRequest.CODEC);
    }

    private static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(DeathLogPayloads.InfoRequest.TYPE, (payload, context) ->
                context.server().execute(() -> DeathLogNetworking.handleServerbound(context.player(), payload.packet())));
        ServerPlayNetworking.registerGlobalReceiver(DeathLogPayloads.RestoreRequest.TYPE, (payload, context) ->
                context.server().execute(() -> DeathLogNetworking.handleServerbound(context.player(), payload.packet())));
        ServerPlayNetworking.registerGlobalReceiver(DeathLogPayloads.DeletionRequest.TYPE, (payload, context) ->
                context.server().execute(() -> DeathLogNetworking.handleServerbound(context.player(), payload.packet())));
    }
}
