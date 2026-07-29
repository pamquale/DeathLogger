package com.glisco.deathlog.death_info;

import com.glisco.deathlog.DeathInfo;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Hook for properties that only apply when some optional mod is present. The
 * loader modules register these during setup, so nothing here has to know which
 * mods exist.
 */
public final class SpecialPropertyProvider {

    private static final List<BiConsumer<DeathInfo, Player>> APPLY_FUNCTIONS = new ArrayList<>();

    private SpecialPropertyProvider() {}

    public static void register(BiConsumer<DeathInfo, Player> applyFunction) {
        APPLY_FUNCTIONS.add(applyFunction);
    }

    public static void apply(DeathInfo info, Player player) {
        APPLY_FUNCTIONS.forEach(consumer -> consumer.accept(info, player));
    }
}
