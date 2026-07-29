package com.glisco.deathlog.network;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.platform.Services;
import com.glisco.deathlog.storage.BaseDeathLogStorage;
import com.glisco.deathlog.storage.DirectDeathLogStorage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Another player's log as seen through {@code /deathlogger view}. Lives only in
 * memory; every mutation is a packet.
 */
public class RemoteDeathLogStorage extends BaseDeathLogStorage implements DirectDeathLogStorage {

    private final List<DeathInfo> deathInfoList;
    private final UUID profileId;

    public RemoteDeathLogStorage(RegistryAccess registries, List<DeathInfo> deathInfoList, UUID profileId) {
        super(registries);
        this.deathInfoList = deathInfoList;
        this.profileId = profileId;
    }

    @Override
    public List<DeathInfo> getDeathInfoList(UUID profile) {
        return this.deathInfoList;
    }

    @Override
    public void delete(DeathInfo info, UUID profile) {
        int index = this.deathInfoList.indexOf(info);
        if (index < 0) return;

        Services.NETWORK.sendToServer(new DeathLogPacket.DeletionRequest(this.profileId, index));
        this.deathInfoList.remove(index);
    }

    @Override
    public void store(Component deathMessage, Player player) {
        // Deaths are recorded by whichever side owns the log, never through this view.
    }

    @Override
    public void restore(int index, UUID profile) {
        Services.NETWORK.sendToServer(new DeathLogPacket.RestoreRequest(this.profileId, index));
    }

    /** Replaces a stripped entry once the full one arrives from the server. */
    public void updateInfo(int index, DeathInfo info) {
        if (index < 0 || index >= this.deathInfoList.size()) return;
        this.deathInfoList.set(index, info);
    }

    public void fetchCompleteInfo(DeathInfo info) {
        if (!info.isPartial()) return;

        int index = this.deathInfoList.indexOf(info);
        if (index < 0) return;

        Services.NETWORK.sendToServer(new DeathLogPacket.InfoRequest(this.profileId, index));
    }
}
