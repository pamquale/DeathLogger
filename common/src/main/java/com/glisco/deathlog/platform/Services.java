package com.glisco.deathlog.platform;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.network.services.INetworkHelper;
import com.glisco.deathlog.platform.services.IPlatformHelper;

import java.util.ServiceLoader;

public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);

    private Services() {}

    public static <T> T load(Class<T> clazz) {
        final var loaded = ServiceLoader.load(clazz)
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));

        DeathLog.LOGGER.debug("Loaded {} for service {}", loaded, clazz);
        return loaded;
    }
}
