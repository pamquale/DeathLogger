package com.glisco.deathlog.mixin;

import com.glisco.deathlog.DeathLogCommon;
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

    @Inject(method = "die", at = @At("HEAD"))
    private void deathlogger$onServerDeath(DamageSource source, CallbackInfo ci) {
        var storage = DeathLogCommon.getStorage();
        if (storage == null) return;

        var player = (Player) (Object) this;
        storage.store(player.getCombatTracker().getDeathMessage(), player);
    }
}
