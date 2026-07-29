package com.glisco.deathlog.network.services;

import com.glisco.deathlog.network.DeathLogPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transport for {@link DeathLogPacket}. Fabric, NeoForge and Forge each register
 * and dispatch payloads differently; everything above this interface does not care.
 */
public interface INetworkHelper {

    void sendToServer(DeathLogPacket packet);

    void sendToPlayer(ServerPlayer player, DeathLogPacket packet);
}
