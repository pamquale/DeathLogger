package com.glisco.deathlog;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public final class DeathLog {

    public static final String MOD_ID = "deathlogger";
    public static final Logger LOGGER = LogUtils.getLogger();

    private DeathLog() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
