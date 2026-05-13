package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameSearchCommands {
    private TameSearchCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("search")
                .then(Commands.literal("ability")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAbilities(b))
                                .executes(ctx -> TameCommands.searchTamesByAbility(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")
                                ))))
                .then(Commands.literal("attribute")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                .executes(ctx -> TameCommands.searchTamesByAttribute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")
                                ))))
                .then(Commands.literal("class")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestClasses(b))
                                .executes(ctx -> TameCommands.searchTamesByClass(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "id")
                                ))));
    }
}
