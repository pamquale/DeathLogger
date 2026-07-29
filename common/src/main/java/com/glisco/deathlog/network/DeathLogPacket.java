package com.glisco.deathlog.network;

import com.glisco.deathlog.DeathInfo;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;
import java.util.UUID;

/**
 * The wire types, kept free of any loader's payload interface. Each loader module
 * wraps these in its own packet type and hands them back to
 * {@link DeathLogNetworking} for handling.
 */
public sealed interface DeathLogPacket {

    // ---- clientbound ----

    record OpenScreen(UUID profile, boolean canRestore, List<DeathInfo> partialInfos) implements DeathLogPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenScreen> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, OpenScreen::profile,
                ByteBufCodecs.BOOL, OpenScreen::canRestore,
                ByteBufCodecs.fromCodecWithRegistries(DeathInfo.PARTIAL_CODEC.listOf()), OpenScreen::partialInfos,
                OpenScreen::new
        );
    }

    record DeathInfoData(int infoIndex, DeathInfo info) implements DeathLogPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, DeathInfoData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, DeathInfoData::infoIndex,
                ByteBufCodecs.fromCodecWithRegistries(DeathInfo.CODEC), DeathInfoData::info,
                DeathInfoData::new
        );
    }

    // ---- serverbound ----

    record InfoRequest(UUID profile, int index) implements DeathLogPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, InfoRequest> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, InfoRequest::profile,
                ByteBufCodecs.VAR_INT, InfoRequest::index,
                InfoRequest::new
        );
    }

    record RestoreRequest(UUID profile, int index) implements DeathLogPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, RestoreRequest> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, RestoreRequest::profile,
                ByteBufCodecs.VAR_INT, RestoreRequest::index,
                RestoreRequest::new
        );
    }

    record DeletionRequest(UUID profile, int index) implements DeathLogPacket {
        public static final StreamCodec<RegistryFriendlyByteBuf, DeletionRequest> STREAM_CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, DeletionRequest::profile,
                ByteBufCodecs.VAR_INT, DeletionRequest::index,
                DeletionRequest::new
        );
    }
}
