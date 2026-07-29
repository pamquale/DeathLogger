package com.glisco.deathlog.storage;

import com.glisco.deathlog.DeathInfo;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * A death log. The {@code profile} argument selects whose log to act on and is
 * null on the client, which only ever has one log in scope - see
 * {@link DirectDeathLogStorage}.
 */
public interface DeathLogStorage {

    List<DeathInfo> getDeathInfoList(UUID profile);

    void delete(DeathInfo info, UUID profile);

    void store(Component deathMessage, Player player);

    void restore(int index, UUID profile);

    /** True once a disk failure has permanently disabled writes for this session. */
    boolean isErrored();

    RegistryAccess registries();
}
