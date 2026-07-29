package com.glisco.deathlog.storage;

import com.glisco.deathlog.DeathInfo;

import java.util.List;

/**
 * A storage with exactly one log in scope, so callers need not pass a profile.
 */
public interface DirectDeathLogStorage extends DeathLogStorage {

    default List<DeathInfo> getDeathInfoList() {
        return getDeathInfoList(null);
    }

    default void delete(DeathInfo info) {
        delete(info, null);
    }

    default void restore(int index) {
        restore(index, null);
    }
}
