package com.glisco.deathlog.client;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.client.gui.DeathLogScreen;
import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.network.RemoteDeathLogStorage;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;

/**
 * Clientbound packet handling, shared by all three loaders. Each loader module
 * decodes its own payload and calls in here on the client thread.
 */
public final class DeathLogClientNetworking {

    private DeathLogClientNetworking() {}

    public static void handleClientbound(DeathLogPacket packet) {
        switch (packet) {
            case DeathLogPacket.OpenScreen open -> handleOpenScreen(open);
            case DeathLogPacket.DeathInfoData data -> handleDeathInfoData(data);
            default -> DeathLog.LOGGER.warn("Received serverbound packet {} on the client", packet.getClass().getSimpleName());
        }
    }

    private static void handleOpenScreen(DeathLogPacket.OpenScreen packet) {
        var client = Minecraft.getInstance();
        if (client.level == null) return;

        var storage = new RemoteDeathLogStorage(
                client.level.registryAccess(), new ArrayList<>(packet.partialInfos()), packet.profile()
        );
        DeathLogScreen.open(client.screen, storage, packet.canRestore());
    }

    private static void handleDeathInfoData(DeathLogPacket.DeathInfoData packet) {
        if (!(Minecraft.getInstance().screen instanceof DeathLogScreen screen)) {
            DeathLog.LOGGER.warn("Received death info while no death log screen was open");
            return;
        }

        screen.updateInfo(packet.infoIndex(), packet.info());
    }
}
