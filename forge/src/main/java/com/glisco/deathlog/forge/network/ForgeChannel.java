package com.glisco.deathlog.forge.network;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.client.DeathLogClientNetworking;
import com.glisco.deathlog.network.DeathLogNetworking;
import com.glisco.deathlog.network.DeathLogPacket;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;

/**
 * Forge 62 still uses {@link SimpleChannel} rather than the payload registrar
 * NeoForge moved to, so the wire types are registered by class here instead of by
 * {@code CustomPacketPayload.Type}. The packets themselves are the shared ones and
 * carry their own StreamCodecs.
 */
public final class ForgeChannel {

    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(DeathLog.id("channel"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    private ForgeChannel() {}

    public static void register() {
        clientbound(DeathLogPacket.OpenScreen.class, DeathLogPacket.OpenScreen.STREAM_CODEC);
        clientbound(DeathLogPacket.DeathInfoData.class, DeathLogPacket.DeathInfoData.STREAM_CODEC);

        serverbound(DeathLogPacket.InfoRequest.class, DeathLogPacket.InfoRequest.STREAM_CODEC);
        serverbound(DeathLogPacket.RestoreRequest.class, DeathLogPacket.RestoreRequest.STREAM_CODEC);
        serverbound(DeathLogPacket.DeletionRequest.class, DeathLogPacket.DeletionRequest.STREAM_CODEC);
    }

    private static <M extends DeathLogPacket> void clientbound(
            Class<M> type, net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, M> codec
    ) {
        CHANNEL.messageBuilder(type, net.minecraftforge.network.NetworkProtocol.PLAY)
                .direction(PacketFlow.CLIENTBOUND)
                .codec(codec)
                .consumerMainThread((message, context) -> {
                    DeathLogClientNetworking.handleClientbound(message);
                    context.setPacketHandled(true);
                })
                .add();
    }

    private static <M extends DeathLogPacket> void serverbound(
            Class<M> type, net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, M> codec
    ) {
        CHANNEL.messageBuilder(type, net.minecraftforge.network.NetworkProtocol.PLAY)
                .direction(PacketFlow.SERVERBOUND)
                .codec(codec)
                .consumerMainThread(ForgeChannel::handleServerbound)
                .add();
    }

    private static void handleServerbound(DeathLogPacket message, CustomPayloadEvent.Context context) {
        var sender = context.getSender();
        if (sender != null) DeathLogNetworking.handleServerbound(sender, message);
        context.setPacketHandled(true);
    }
}
