package com.glisco.deathlog.platform.services;

import java.nio.file.Path;

/**
 * The handful of things the shared code needs that each loader answers
 * differently. Implementations are supplied per module through
 * {@link java.util.ServiceLoader}.
 */
public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /** The instance directory, i.e. where {@code mods} and {@code saves} live. */
    Path getGameDirectory();
}
