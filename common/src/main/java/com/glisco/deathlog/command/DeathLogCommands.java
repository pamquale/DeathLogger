package com.glisco.deathlog.command;

import com.glisco.deathlog.DeathInfo;
import com.glisco.deathlog.DeathLogCommon;
import com.glisco.deathlog.network.DeathLogPacket;
import com.glisco.deathlog.permission.DeathLogPermissions;
import com.glisco.deathlog.platform.Services;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.players.NameAndId;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class DeathLogCommands {

    private static final DynamicCommandExceptionType INVALID_INDEX =
            new DynamicCommandExceptionType(index -> Component.literal("No death info found for index " + index));
    private static final SimpleCommandExceptionType NO_DEATHS =
            new SimpleCommandExceptionType(Component.literal("No death info found"));

    private DeathLogCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("deathlogger")
                .then(Commands.literal("list")
                        .requires(DeathLogPermissions.require(DeathLogPermissions.LIST))
                        .then(profileArgument()
                                .executes(context -> executeList(context, null))
                                .then(Commands.argument("search_term", StringArgumentType.string())
                                        .executes(context -> executeList(context, StringArgumentType.getString(context, "search_term"))))))
                .then(Commands.literal("view")
                        .requires(DeathLogPermissions.require(DeathLogPermissions.VIEW))
                        .then(profileArgument().executes(DeathLogCommands::executeView)))
                .then(Commands.literal("restore")
                        .requires(DeathLogPermissions.require(DeathLogPermissions.RESTORE))
                        .then(profileArgument()
                                .then(Commands.argument("index", IntegerArgumentType.integer(0))
                                        .executes(context -> executeRestore(context, IntegerArgumentType.getInteger(context, "index"))))
                                .then(Commands.literal("latest").executes(context -> executeRestore(context, -1))))));
    }

    private static int executeList(CommandContext<CommandSourceStack> context, String filter) throws CommandSyntaxException {
        var profile = profileOf(context);
        var infoList = infoList(profile);

        if (filter != null) {
            var needle = filter.toLowerCase(java.util.Locale.ROOT);
            infoList = infoList.stream().filter(info -> info.createSearchString().contains(needle)).toList();
        }

        for (int i = 0; i < infoList.size(); i++) {
            var info = infoList.get(i);
            var left = info.getLeftColumnText().iterator();
            var right = info.getRightColumnText().iterator();
            int index = i;

            context.getSource().sendSuccess(() -> Component.literal("-- Death Info Entry [" + index + "] --")
                    .withStyle(ChatFormatting.GRAY), false);
            while (left.hasNext() && right.hasNext()) {
                var line = ((MutableComponent) left.next()).append(Component.literal(": "))
                        .append(((MutableComponent) right.next()).withStyle(ChatFormatting.WHITE));
                context.getSource().sendSuccess(() -> line, false);
            }
        }

        final int size = infoList.size();
        context.getSource().sendSuccess(
                () -> Component.literal("Queried " + size + " death info entries for " + profile.name()), false
        );
        return size;
    }

    private static int executeView(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var profile = profileOf(context);
        var server = context.getSource().getServer();

        Services.NETWORK.sendToPlayer(player, new DeathLogPacket.OpenScreen(
                profile.id(),
                server.getPlayerList().getPlayer(profile.id()) != null,
                infoList(profile)
        ));
        return 1;
    }

    private static int executeRestore(CommandContext<CommandSourceStack> context, int requestedIndex) throws CommandSyntaxException {
        var profile = profileOf(context);
        var infoList = infoList(profile);

        var target = context.getSource().getServer().getPlayerList().getPlayer(profile.id());
        if (target == null) throw NO_DEATHS.create();

        int index = requestedIndex < 0 ? infoList.size() - 1 : requestedIndex;
        if (infoList.isEmpty()) throw NO_DEATHS.create();
        if (index >= infoList.size()) throw INVALID_INDEX.create(index);

        infoList.get(index).restore(target);
        return 1;
    }

    private static List<DeathInfo> infoList(NameAndId profile) {
        var storage = DeathLogCommon.getStorage();
        return storage == null ? List.of() : storage.getDeathInfoList(profile.id());
    }

    private static NameAndId profileOf(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return GameProfileArgument.getGameProfiles(context, "player").iterator().next();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, GameProfileArgument.Result> profileArgument() {
        return Commands.argument("player", GameProfileArgument.gameProfile())
                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                        context.getSource().getServer().getPlayerList().getPlayers().stream()
                                .map(player -> player.getName().getString()),
                        builder
                ));
    }
}
