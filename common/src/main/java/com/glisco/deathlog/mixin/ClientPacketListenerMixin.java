package com.glisco.deathlog.mixin;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.client.DeathLogConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-side death capture. The render-thread guard is what the Fabric build
 * used; without it the packet thread can race the storage.
 */
@Mixin(net.minecraft.client.multiplayer.ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {

    @Inject(method = "handlePlayerCombatKill", at = @At("HEAD"))
    private void deathlogger$onClientDeath(ClientboundPlayerCombatKillPacket packet, CallbackInfo ci) {
        if (!RenderSystem.isOnRenderThread()) return;
        deathlogger$record(packet.message());
    }

    /**
     * Fallback for servers that suppress the combat-kill packet. Off by default
     * because it fires on any zero-health update.
     */
    @Inject(method = "handleSetHealth", at = @At("HEAD"))
    private void deathlogger$onLegacyClientDeath(ClientboundSetHealthPacket packet, CallbackInfo ci) {
        if (!DeathLogConfig.get().useLegacyDeathDetection() || !RenderSystem.isOnRenderThread()) return;

        var player = Minecraft.getInstance().player;
        if (packet.getHealth() > 0 || player == null || player.isDeadOrDying()) return;

        deathlogger$record(Component.empty());
    }

    private static void deathlogger$record(Component deathMessage) {
        var storage = DeathLogCommon.getStorage();
        var player = Minecraft.getInstance().player;
        if (storage == null || player == null) return;

        storage.store(deathMessage, player);
    }
}
