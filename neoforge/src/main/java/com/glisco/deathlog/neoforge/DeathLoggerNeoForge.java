package com.glisco.deathlog.neoforge;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.command.DeathLogCommands;
import com.glisco.deathlog.network.DeathLogNetworking;
import com.glisco.deathlog.network.DeathLogPayloads;
import com.glisco.deathlog.storage.ServerDeathLogStorage;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@Mod(DeathLog.MOD_ID)
public class DeathLoggerNeoForge {

    public DeathLoggerNeoForge(IEventBus modBus) {
        modBus.addListener(DeathLoggerNeoForge::registerPayloads);

        NeoForge.EVENT_BUS.addListener(DeathLoggerNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(DeathLoggerNeoForge::onRegisterCommands);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");

        registrar.playToClient(DeathLogPayloads.OpenScreen.TYPE, DeathLogPayloads.OpenScreen.CODEC,
                (payload, context) -> context.enqueueWork(() -> DeathLoggerNeoForgeClient.handle(payload.packet())));
        registrar.playToClient(DeathLogPayloads.DeathInfoData.TYPE, DeathLogPayloads.DeathInfoData.CODEC,
                (payload, context) -> context.enqueueWork(() -> DeathLoggerNeoForgeClient.handle(payload.packet())));

        registrar.playToServer(DeathLogPayloads.InfoRequest.TYPE, DeathLogPayloads.InfoRequest.CODEC,
                (payload, context) -> handleServerbound(context, payload.packet()));
        registrar.playToServer(DeathLogPayloads.RestoreRequest.TYPE, DeathLogPayloads.RestoreRequest.CODEC,
                (payload, context) -> handleServerbound(context, payload.packet()));
        registrar.playToServer(DeathLogPayloads.DeletionRequest.TYPE, DeathLogPayloads.DeletionRequest.CODEC,
                (payload, context) -> handleServerbound(context, payload.packet()));
    }

    private static void handleServerbound(IPayloadContext context, com.glisco.deathlog.network.DeathLogPacket packet) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sender) {
                DeathLogNetworking.handleServerbound(sender, packet);
            }
        });
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (!event.getServer().isDedicatedServer()) return;

        DeathLogCommon.setStorage(new ServerDeathLogStorage(
                event.getServer().registryAccess(),
                FMLPaths.GAMEDIR.get().resolve("deaths")
        ));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        DeathLogCommands.register(event.getDispatcher());
    }
}
