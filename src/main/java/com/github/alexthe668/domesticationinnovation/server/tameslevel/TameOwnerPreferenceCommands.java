package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameOwnerPreferenceCommands {
    private TameOwnerPreferenceCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> doNotAttack() {
        return Commands.literal("doNotAttack")
                .executes(ctx -> TameCommands.listDoNotAttack(ctx.getSource()))
                .then(Commands.literal("remove")
                        .then(Commands.argument("mobtype", StringArgumentType.word())
                                .suggests((ctx, b) -> TameCommands.suggestCurrentPlayerDoNotAttackTypes(ctx.getSource(), b))
                                .executes(ctx -> TameCommands.removeDoNotAttackType(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "mobtype")
                                ))))
                .then(Commands.argument("mobtype", StringArgumentType.word())
                        .suggests((ctx, b) -> TameCommands.suggestEntityTypes(b))
                        .executes(ctx -> TameCommands.toggleDoNotAttackType(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "mobtype")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> doNotAttackAnimals() {
        return Commands.literal("doNotAttackAnimals")
                .executes(ctx -> TameCommands.listDoNotAttack(ctx.getSource()))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> TameCommands.setDoNotAttackAnimals(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, "enabled")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> healthSiphon() {
        return Commands.literal("healthSiphon")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> TameCommands.setHealthSiphonEnabled(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, "enabled")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> herding() {
        return Commands.literal("herding")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> TameCommands.setHerdingAffectsTames(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, "enabled")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> enterPortalsByThemselves() {
        return Commands.literal("enterPortalsByThemselves")
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> TameCommands.setEnterPortalsByThemselves(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, "enabled")
                        )));
    }

    static LiteralArgumentBuilder<CommandSourceStack> sitOnChairs() {
        return Commands.literal("sitOnChairs")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(ctx -> TameCommands.setSitOnChairs(
                                ctx.getSource(),
                                BoolArgumentType.getBool(ctx, "enabled")
                        )));
    }
}
