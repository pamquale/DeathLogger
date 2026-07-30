package com.glisco.deathlog.fabric.network;

import com.glisco.deathlog.network.DeathLogPayloads;

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
        var payload = DeathLogPayloads.wrap(packet);
        if (payload != null) ClientPlayNetworking.send(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DeathLogPacket packet) {
        var payload = DeathLogPayloads.wrap(packet);
        if (payload != null) ServerPlayNetworking.send(player, payload);
    }

}
