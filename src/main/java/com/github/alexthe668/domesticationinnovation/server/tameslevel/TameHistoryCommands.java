package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameHistoryCommands {
    private TameHistoryCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> graveyard() {
        return Commands.literal("graveyard")
                .executes(ctx -> TameCommands.graveyard(ctx.getSource(), 10))
                .then(Commands.argument("limit", IntegerArgumentType.integer(1))
                        .executes(ctx -> TameCommands.graveyard(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "limit")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> deaths() {
        return Commands.literal("deaths")
                .executes(ctx -> TameCommands.recentDeaths(ctx.getSource(), 10))
                .then(Commands.argument("number", IntegerArgumentType.integer(1, 200))
                        .executes(ctx -> TameCommands.recentDeaths(
                                ctx.getSource(),
                                IntegerArgumentType.getInteger(ctx, "number")
                        )));
    }
}
