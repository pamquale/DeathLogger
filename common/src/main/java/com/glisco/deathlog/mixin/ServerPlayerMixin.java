package com.glisco.deathlog.mixin;

import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.storage.ServerDeathLogStorage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side death capture. Only a dedicated server installs a storage, so on
 * an integrated server this finds none and does nothing - the client path
 * records that death instead.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    /**
     * Only a {@link ServerDeathLogStorage} is recorded into here. On an integrated
     * server the slot holds the <em>client's</em> storage, and recording into it
     * would file a second copy of every singleplayer death alongside the one the
     * client-side hook already wrote.
     */
    @Inject(method = "die", at = @At("HEAD"))
    private void deathlogger$onServerDeath(DamageSource source, CallbackInfo ci) {
        if (!(DeathLogCommon.getStorage() instanceof ServerDeathLogStorage storage)) return;

        var player = (Player) (Object) this;
        storage.store(player.getCombatTracker().getDeathMessage(), player);
    }
}
