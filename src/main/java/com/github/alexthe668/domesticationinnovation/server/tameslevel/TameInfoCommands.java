package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameInfoCommands {
    private TameInfoCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("info")
                .executes(ctx -> TameCommands.infoOverview(ctx.getSource()))
                .then(Commands.literal("tool")
                        .then(Commands.literal("guardian")
                                .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "tool guardian"))))
                .then(Commands.literal("attribute")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "attribute"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                .executes(ctx -> TameCommands.infoAttribute(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> TameCommands.infoAttribute(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                IntegerArgumentType.getInteger(ctx, "level")
                                        )))))
                .then(Commands.literal("ability")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "ability"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAbilities(b))
                                .executes(ctx -> TameCommands.infoAbility(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))
                                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                        .executes(ctx -> TameCommands.infoAbility(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                IntegerArgumentType.getInteger(ctx, "level")
                                        )))))
                .then(Commands.literal("class")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "class"))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestClasses(b))
                                .executes(ctx -> TameCommands.infoClass(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))))
                .then(Commands.argument("command", StringArgumentType.greedyString())
                        .suggests((ctx, b) -> TameCommands.suggestInfoTopics(b))
                        .executes(ctx -> TameCommands.infoDetail(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "command")
                        )));
    }
}
