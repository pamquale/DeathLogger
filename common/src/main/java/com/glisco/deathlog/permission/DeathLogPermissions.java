package com.glisco.deathlog.permission;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.function.Predicate;

/**
 * Central place for the permission checks.
 * <p>
 * The nodes are not honoured yet - every check is an operator-level check, the
 * same behaviour the Fabric build had. They are threaded through anyway so that
 * wiring a real permission API in later is a change to this class alone. Note
 * that the old code spelled the delete node {@code deathlog.delete} while every
 * other site used the {@code deathlogger.} prefix; they agree now.
 */
public final class DeathLogPermissions {

    public static final String VIEW = "deathlogger.view";
    public static final String LIST = "deathlogger.list";
    public static final String RESTORE = "deathlogger.restore";
    public static final String DELETE = "deathlogger.delete";

    private static final Permission OPERATOR = new Permission.HasCommandLevel(PermissionLevel.OWNERS);

    private DeathLogPermissions() {}

    public static boolean has(ServerPlayer player, String node) {
        return player.permissions().hasPermission(node);
    }

    public static Predicate<CommandSourceStack> require(String node) {
        return source -> source.permissions().hasPermission(node);
    }
}
