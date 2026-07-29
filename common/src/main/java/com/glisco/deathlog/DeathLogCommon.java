package com.glisco.deathlog;

import com.glisco.deathlog.storage.DeathLogStorage;

/**
 * Holds the one storage that is in scope right now. The client fills it on join
 * and clears it on disconnect; a dedicated server fills it on start. It is
 * therefore null outside a world and every caller must handle that.
 */
public final class DeathLogCommon {

    private static DeathLogStorage currentStorage = null;

    private DeathLogCommon() {}

    public static void setStorage(DeathLogStorage storage) {
        currentStorage = storage;
    }

    public static DeathLogStorage getStorage() {
        return currentStorage;
    }
}
