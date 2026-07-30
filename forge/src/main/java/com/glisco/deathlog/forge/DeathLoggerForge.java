package com.glisco.deathlog.forge;

import com.glisco.deathlog.DeathLog;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.DeathLogClient;
import com.glisco.deathlog.command.DeathLogCommands;
import com.glisco.deathlog.forge.network.ForgeChannel;
import com.glisco.deathlog.storage.ServerDeathLogStorage;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Forge 62 runs EventBus 7, where every event carries its own static {@code BUS}
 * rather than being posted to one global bus, so there is no bus to take in the
 * constructor and nothing to subscribe by annotation.
 * <p>
 * Forge has no build of Better Statistics Screen, so unlike the Fabric and
 * NeoForge modules this one carries no integration with it at all.
 */
@Mod(DeathLog.MOD_ID)
public class DeathLoggerForge {

    public DeathLoggerForge() {
        ForgeChannel.register();

        ServerStartedEvent.BUS.addListener(DeathLoggerForge::onServerStarted);
        RegisterCommandsEvent.BUS.addListener(DeathLoggerForge::onRegisterCommands);

        if (FMLEnvironment.dist == Dist.CLIENT) ClientSetup.init();
    }

    private static void onServerStarted(ServerStartedEvent event) {
        if (!event.getServer().isDedicatedServer()) return;

        DeathLogCommon.setStorage(new ServerDeathLogStorage(
                event.getServer().registryAccess(),
                FMLPaths.GAMEDIR.get().resolve("deaths")
        ));
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        DeathLogCommands.register(event.getDispatcher());
    }

    /** Nested so the client-only classes never load on a dedicated server. */
    private static final class ClientSetup {

        static final KeyMapping OPEN_DEATH_SCREEN = new KeyMapping(
                "key.deathlogger.death_screen", InputConstants.UNKNOWN.getValue(), KeyMapping.Category.MISC
        );

        private ClientSetup() {}

        static void init() {
            RegisterKeyMappingsEvent.BUS.addListener(event -> event.register(OPEN_DEATH_SCREEN));

            ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(
                    event -> DeathLogCommon.setStorage(ForgeClientStorage.create(Minecraft.getInstance()))
            );
            ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> DeathLogCommon.setStorage(null));

            TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
                while (OPEN_DEATH_SCREEN.consumeClick()) DeathLogClient.openScreen();
            });
        }
    }
}
