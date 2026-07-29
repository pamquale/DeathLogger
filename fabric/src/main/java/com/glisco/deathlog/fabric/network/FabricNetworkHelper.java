package com.glisco.deathlog.fabric.network;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.network.services.INetworkHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public class FabricNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(DeathLogPacket packet) {
        var payload = wrap(packet);
        if (payload != null) ClientPlayNetworking.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DeathLogPacket packet) {
        var payload = wrap(packet);
        if (payload != null) ServerPlayNetworking.send(player, payload);
    }

    private static CustomPacketPayload wrap(DeathLogPacket packet) {
        return switch (packet) {
            case DeathLogPacket.OpenScreen p -> new DeathLogPayloads.OpenScreen(p);
            case DeathLogPacket.DeathInfoData p -> new DeathLogPayloads.DeathInfoData(p);
            case DeathLogPacket.InfoRequest p -> new DeathLogPayloads.InfoRequest(p);
            case DeathLogPacket.RestoreRequest p -> new DeathLogPayloads.RestoreRequest(p);
            case DeathLogPacket.DeletionRequest p -> new DeathLogPayloads.DeletionRequest(p);
            default -> {
                DeathLog.LOGGER.warn("No Fabric payload for {}", packet.getClass().getSimpleName());
                yield null;
            }
        };
    }
}
