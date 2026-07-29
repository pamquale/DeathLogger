package com.glisco.deathlog.network;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.permission.DeathLogPermissions;
import com.glisco.deathlog.platform.Services;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Server-side packet handling, shared by all three loaders. Each loader module
 * only has to decode its own payload and call in here on the server thread.
 * <p>
 * Every handler checks the permission <em>and</em> bounds-checks the index before
 * touching the list - the index arrives straight from a client.
 */
public final class DeathLogNetworking {

    private DeathLogNetworking() {}

    public static void handleServerbound(ServerPlayer sender, DeathLogPacket packet) {
        switch (packet) {
            case DeathLogPacket.InfoRequest request -> handleInfoRequest(sender, request);
            case DeathLogPacket.RestoreRequest request -> handleRestoreRequest(sender, request);
            case DeathLogPacket.DeletionRequest request -> handleDeletionRequest(sender, request);
            default -> DeathLog.LOGGER.warn(
                    "Received clientbound packet {} from '{}'", packet.getClass().getSimpleName(), sender.getName().getString()
            );
        }
    }

    private static void handleInfoRequest(ServerPlayer sender, DeathLogPacket.InfoRequest request) {
        if (!DeathLogPermissions.has(sender, DeathLogPermissions.VIEW)) {
            warnUnauthorized(sender, "info request");
            return;
        }

        var infoList = infoListOrNull(request.profile());
        if (infoList == null || outOfBounds(sender, infoList, request.index(), "info request")) return;

        Services.NETWORK.sendToPlayer(sender, new DeathLogPacket.DeathInfoData(request.index(), infoList.get(request.index())));
    }

    private static void handleRestoreRequest(ServerPlayer sender, DeathLogPacket.RestoreRequest request) {
        if (!DeathLogPermissions.has(sender, DeathLogPermissions.RESTORE)) {
            warnUnauthorized(sender, "restore packet");
            return;
        }

        var server = sender.level() instanceof ServerLevel serverLevel ? serverLevel.getServer() : null;
        var target = server != null ? server.getPlayerList().getPlayer(request.profile()) : null;
        if (target == null) {
            DeathLog.LOGGER.warn("Received restore packet for a player that is not online");
            return;
        }

        var infoList = infoListOrNull(request.profile());
        if (infoList == null || outOfBounds(sender, infoList, request.index(), "restore packet")) return;

        rebindToServerRegistries(infoList.get(request.index()), target).restore(target);
    }

    private static void handleDeletionRequest(ServerPlayer sender, DeathLogPacket.DeletionRequest request) {
        if (!DeathLogPermissions.has(sender, DeathLogPermissions.DELETE)) {
            warnUnauthorized(sender, "delete packet");
            return;
        }

        var storage = DeathLogCommon.getStorage();
        if (storage == null) return;

        var infoList = storage.getDeathInfoList(request.profile());
        if (outOfBounds(sender, infoList, request.index(), "deletion packet")) return;

        storage.delete(infoList.get(request.index()), request.profile());
    }

    /**
     * Moves a DeathInfo from the storage's registry manager into the server's before
     * restoring it. The round trip looks redundant but is not: item stacks carry
     * registry-bound components and the two managers are not necessarily the same.
     */
    private static DeathInfo rebindToServerRegistries(DeathInfo info, ServerPlayer target) {
        var storage = DeathLogCommon.getStorage();
        if (storage == null) return info;

        var from = RegistryOps.create(NbtOps.INSTANCE, storage.registries());
        var to = RegistryOps.create(NbtOps.INSTANCE, target.registryAccess());

        return DeathInfo.CODEC.encodeStart(from, info)
                .flatMap(tag -> DeathInfo.CODEC.parse(to, tag))
                .resultOrPartial(error -> DeathLog.LOGGER.error("Failed to rebind death info to the server registries: {}", error))
                .orElse(info);
    }

    private static List<DeathInfo> infoListOrNull(java.util.UUID profile) {
        var storage = DeathLogCommon.getStorage();
        return storage == null ? null : storage.getDeathInfoList(profile);
    }

    private static boolean outOfBounds(ServerPlayer sender, List<DeathInfo> infoList, int index, String what) {
        if (index >= 0 && index < infoList.size()) return false;

        DeathLog.LOGGER.warn("Received {} with an invalid index from '{}'", what, sender.getName().getString());
        return true;
    }

    private static void warnUnauthorized(ServerPlayer sender, String what) {
        DeathLog.LOGGER.warn("Received unauthorized {} from '{}'", what, sender.getName().getString());
    }
}
