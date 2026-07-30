package com.glisco.deathlog.forge.network;

import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.network.services.INetworkHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ForgeNetworkHelper implements INetworkHelper {

    @Override
    public void sendToServer(DeathLogPacket packet) {
        ForgeChannel.CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DeathLogPacket packet) {
        ForgeChannel.CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }
}
