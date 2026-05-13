package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameBedCommands {
    private TameBedCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("bed")
                .executes(ctx -> TameCommands.listOwnedBeds(ctx.getSource()))
                .then(Commands.literal("long")
                        .executes(ctx -> TameCommands.listOwnedBedsLong(ctx.getSource())))
                .then(Commands.literal("noBed")
                        .executes(ctx -> TameCommands.listOwnedBedsWithoutBed(ctx.getSource())))
                .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestOwnedPetNames(ctx.getSource(), b))
                                .executes(ctx -> TameCommands.setOwnedBed(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))))
                .then(Commands.literal("remove")
                        .then(Commands.literal("all")
                                .executes(ctx -> TameCommands.removeOwnedBedsAll(ctx.getSource())))
                        .then(Commands.literal("group")
                                .then(Commands.argument("group", StringArgumentType.string())
                                        .suggests((ctx, b) -> TameCommands.suggestOwnedGroups(ctx.getSource(), b))
                                        .executes(ctx -> TameCommands.removeOwnedBedsGroup(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "group")
                                        ))))
                        .then(Commands.literal("type")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> TameCommands.suggestOwnedTypes(ctx.getSource(), b))
                                        .executes(ctx -> TameCommands.removeOwnedBedsType(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "type")
                                        ))))
                        .then(Commands.argument("name", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestOwnedPetNamesWithBeds(ctx.getSource(), b))
                                .executes(ctx -> TameCommands.removeOwnedBed(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name")
                                ))));
    }
}
