package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameAdminRewardCommands {
    private TameAdminRewardCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> ability() {
        return Commands.literal("ability")
                .then(Commands.literal("add")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> TameCommands.suggestAdminAbilities(b))
                                        .executes(ctx -> TameCommands.abilityAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.abilityAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels"))))))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAdminAbilities(b))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                        .executes(ctx -> TameCommands.abilityAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.abilityAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> TameCommands.suggestAdminAbilities(b))
                                        .executes(ctx -> TameCommands.abilityRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.abilityRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels"))))))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAdminAbilities(b))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                        .executes(ctx -> TameCommands.abilityRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.abilityRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels")))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .executes(ctx -> TameCommands.abilityClear(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))))
                .then(Commands.literal("list")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .executes(ctx -> TameCommands.abilityList(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))));
    }

    static LiteralArgumentBuilder<CommandSourceStack> attribute() {
        return Commands.literal("attribute")
                .then(Commands.literal("add")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                        .executes(ctx -> TameCommands.attributeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.attributeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels"))))))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                        .executes(ctx -> TameCommands.attributeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.attributeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels")))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                        .executes(ctx -> TameCommands.attributeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.attributeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels"))))))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestAttributes(b))
                                .then(Commands.argument("pet", StringArgumentType.string())
                                        .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                        .executes(ctx -> TameCommands.attributeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), 1))
                                        .then(Commands.argument("levels", IntegerArgumentType.integer(1))
                                                .executes(ctx -> TameCommands.attributeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "pet"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "levels")))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .executes(ctx -> TameCommands.attributeClear(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))))
                .then(Commands.literal("list")
                        .then(Commands.argument("pet", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestAllAliveTameNames(b))
                                .executes(ctx -> TameCommands.attributeList(ctx.getSource(), StringArgumentType.getString(ctx, "pet")))));
    }
}
