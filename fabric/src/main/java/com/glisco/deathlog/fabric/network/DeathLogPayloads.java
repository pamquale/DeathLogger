package com.glisco.deathlog.fabric.network;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.network.DeathLogPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Wraps the loader-agnostic {@link DeathLogPacket} types in Fabric payloads.
 */
public final class DeathLogPayloads {

    private DeathLogPayloads() {}

    public record OpenScreen(DeathLogPacket.OpenScreen packet) implements CustomPacketPayload {
        public static final Type<OpenScreen> TYPE = new Type<>(id("open_screen"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> CODEC =
                DeathLogPacket.OpenScreen.STREAM_CODEC.map(OpenScreen::new, OpenScreen::packet);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DeathInfoData(DeathLogPacket.DeathInfoData packet) implements CustomPacketPayload {
        public static final Type<DeathInfoData> TYPE = new Type<>(id("death_info_data"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeathInfoData> CODEC =
                DeathLogPacket.DeathInfoData.STREAM_CODEC.map(DeathInfoData::new, DeathInfoData::packet);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record InfoRequest(DeathLogPacket.InfoRequest packet) implements CustomPacketPayload {
        public static final Type<InfoRequest> TYPE = new Type<>(id("info_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, InfoRequest> CODEC =
                DeathLogPacket.InfoRequest.STREAM_CODEC.map(InfoRequest::new, InfoRequest::packet);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RestoreRequest(DeathLogPacket.RestoreRequest packet) implements CustomPacketPayload {
        public static final Type<RestoreRequest> TYPE = new Type<>(id("restore_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RestoreRequest> CODEC =
                DeathLogPacket.RestoreRequest.STREAM_CODEC.map(RestoreRequest::new, RestoreRequest::packet);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record DeletionRequest(DeathLogPacket.DeletionRequest packet) implements CustomPacketPayload {
        public static final Type<DeletionRequest> TYPE = new Type<>(id("deletion_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DeletionRequest> CODEC =
                DeathLogPacket.DeletionRequest.STREAM_CODEC.map(DeletionRequest::new, DeletionRequest::packet);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static Identifier id(String path) {
        return DeathLog.id(path);
    }
}
