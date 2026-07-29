package com.glisco.deathlog;

import net.minecraft.resources.Identifier;

public final class DeathLog {

    public static final String MOD_ID = "deathlogger";

    private DeathLog() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
