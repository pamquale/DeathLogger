package com.glisco.deathlog.storage;

import com.glisco.deathlog.DeathInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fired once a {@link DeathInfo} has been filled in but before it is stored.
 * Replaces the Fabric {@code Event} the mod used to rely on.
 */
@FunctionalInterface
public interface DeathInfoCreatedCallback {

    List<DeathInfoCreatedCallback> LISTENERS = new ArrayList<>();

    static void register(DeathInfoCreatedCallback listener) {
        LISTENERS.add(listener);
    }

    static void invoke(DeathInfo info) {
        for (var listener : LISTENERS) {
            listener.onDeathInfoCreated(info);
        }
    }

    void onDeathInfoCreated(DeathInfo info);
}
