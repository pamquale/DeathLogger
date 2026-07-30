package com.glisco.deathlog.neoforge;

import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.network.DeathLogPayloads;
import com.glisco.deathlog.network.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(DeathLogPacket packet) {
        var payload = DeathLogPayloads.wrap(packet);
        if (payload != null) ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DeathLogPacket packet) {
        var payload = DeathLogPayloads.wrap(packet);
        if (payload != null) PacketDistributor.sendToPlayer(player, payload);
    }
}
