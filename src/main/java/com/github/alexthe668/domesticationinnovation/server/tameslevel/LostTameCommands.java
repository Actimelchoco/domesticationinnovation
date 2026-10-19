package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class LostTameCommands {
    private LostTameCommands() {}
    public static LiteralArgumentBuilder<CommandSourceStack> playerBuild() {
        return Commands.literal("lost").executes(ctx -> playerList(ctx.getSource(), 1))
                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> playerList(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page"))));
    }
    private static int playerList(CommandSourceStack source, int page) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        UUID owner = source.getPlayerOrException().getUUID();
        var registry = LostTameRegistry.get(source.getServer());
        var ids = playerEntries(source.getServer(), owner);
        int pages = Math.max(1, (ids.size() + 19) / 20);
        if (page > pages) { source.sendFailure(Component.literal("Page must be between 1 and " + pages + "."));return 0; }
        source.sendSuccess(() -> Component.literal("Your lost tames: " + ids.size() + " | page " + page + "/" + pages
                + ". Locations are recorded at loss."), false);
        for (UUID id : ids.subList((page - 1) * 20, Math.min(page * 20, ids.size()))) {
            var data = TameData.fromTag(registry.entry(id).getCompound("tame"));
            source.sendSuccess(() -> Component.literal(data.name + " [" + data.type + "] | " + data.lastKnownDimension
                    + " | " + data.lastKnownX + ", " + data.lastKnownY + ", " + data.lastKnownZ), false);
        }
        return 1;
    }
    public static List<UUID> playerEntries(net.minecraft.server.MinecraftServer server, UUID owner) {
        var registry = LostTameRegistry.get(server);
        return registry.ids(true).stream().filter(id -> {
            var data = TameData.fromTag(registry.entry(id).getCompound("tame"));
            return owner.equals(data.ownerUUID) || owner.equals(data.imprintedOwnerUUID());
        }).toList();
    }
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        var root = Commands.literal("lost").requires(source -> source.hasPermission(2))
                .executes(ctx -> list(ctx.getSource(), 1))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource(), 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1)).executes(ctx -> list(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "page")))));
        for (String action : List.of("info", "recover")) root.then(Commands.literal(action)
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(LostTameRegistry.get(ctx.getSource().getServer())
                                .ids(action.equals("recover")).stream().limit(100).map(UUID::toString).toList(), b))
                        .executes(ctx -> entry(ctx.getSource(), StringArgumentType.getString(ctx, "id"), action.equals("recover")))));
        return root;
    }
    private static int list(CommandSourceStack source, int page) {
        var registry = LostTameRegistry.get(source.getServer());
        var ids = registry.ids(false);
        int pages = Math.max(1, (ids.size() + 19) / 20);
        if (page > pages) { source.sendFailure(Component.literal("Page must be between 1 and " + pages + "."));return 0; }
        source.sendSuccess(() -> Component.literal("Lost tame registry: " + ids.size() + " entries; page " + page + "/" + pages + "."), false);
        int start = (page - 1) * 20;
        for (UUID id : ids.subList(start, Math.min(start + 20, ids.size()))) {
            var row = registry.entry(id);var data = TameData.fromTag(row.getCompound("tame"));
            source.sendSuccess(() -> Component.literal(id + " | " + data.name + " | owner " + ownerName(source, data.ownerUUID)
                    + " | " + row.getString("reason") + (row.contains("recoveredAt") ? " | recovered" : " | recoverable")), false);
        }
        return 1;
    }
    private static int entry(CommandSourceStack source, String text, boolean recover) {
        UUID id;
        try { id = UUID.fromString(text); }
        catch (IllegalArgumentException exception) { source.sendFailure(Component.literal("Select a lost-registry entry UUID from /tames admin lost list."));return 0; }
        var row = LostTameRegistry.get(source.getServer()).entry(id);
        if (row == null) { source.sendFailure(Component.literal("Lost tame entry not found."));return 0; }
        var data = TameData.fromTag(row.getCompound("tame"));
        if (!recover) {
            source.sendSuccess(() -> Component.literal(data.name + " [" + data.type + "] | level " + data.level
                    + " | owner " + ownerName(source, data.ownerUUID) + " | imprint " + ownerName(source, data.imprintedOwnerUUID())
                    + " | " + row.getString("reason") + " | lost " + java.time.Instant.ofEpochMilli(row.getLong("lostAt"))
                    + " | " + data.lastKnownDimension + " " + data.lastKnownX + " " + data.lastKnownY + " " + data.lastKnownZ
                    + (row.contains("recoveredAt") ? " | recovered" : " | recoverable")), false);
            return 1;
        }
        var result = TameLimitService.recoverLost(source.getServer(), id, source.getLevel(), source.getPosition());
        if (result.entity() == null) { source.sendFailure(Component.literal(result.error()));return 0; }
        source.sendSuccess(() -> Component.literal("Recovered " + data.name + " for " + ownerName(source, data.ownerUUID)
                + " here, sitting. Original imprint and progress preserved; the limit grace timer has been reset."), true);
        return 1;
    }
    private static String ownerName(CommandSourceStack source, UUID owner) {
        if (owner == null) return "unknown";
        var player = source.getServer().getPlayerList().getPlayer(owner);
        return player == null ? owner.toString() : player.getGameProfile().getName();
    }
}
