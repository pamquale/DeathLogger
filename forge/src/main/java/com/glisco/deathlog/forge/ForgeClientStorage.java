package com.glisco.deathlog.forge;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.client.ClientDeathLogStorage;
import com.glisco.deathlog.mixin.MinecraftServerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.codec.digest.DigestUtils;

public final class ForgeClientStorage {

    private ForgeClientStorage() {}

    /**
     * One log per world or server, named after a hash of the world directory or the
     * server entry, so logs from different places never mix.
     */
    public static ClientDeathLogStorage create(Minecraft client) {
        if (client.level == null) return null;

        final String locationName;
        final boolean multiplayer;

        if (client.hasSingleplayerServer() && client.getSingleplayerServer() != null) {
            locationName = ((MinecraftServerAccessor) client.getSingleplayerServer())
                    .deathlogger$getStorageSource().getLevelId();
            multiplayer = false;
        } else if (client.getCurrentServer() != null) {
            locationName = client.getCurrentServer().name;
            multiplayer = true;
        } else {
            locationName = "unknown";
            multiplayer = false;
        }

        var suffix = DigestUtils.sha1Hex(locationName).substring(0, 10);
        var file = FMLPaths.GAMEDIR.get()
                .resolve(DeathLog.MOD_ID)
                .resolve("deaths_" + suffix + ".dat");

        return new ClientDeathLogStorage(client.level.registryAccess(), file, locationName, multiplayer);
    }
}
