package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameAdminDisableTameCommands {
    private TameAdminDisableTameCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build(String literalName) {
        return Commands.literal(literalName)
                .then(Commands.literal("disable")
                        .executes(ctx -> TameCommands.adminDisableTameTypeUsage(ctx.getSource()))
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestKnownTameTypes(b))
                                .executes(ctx -> TameCommands.adminDisableTameType(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")
                                ))))
                .then(Commands.literal("view")
                        .executes(ctx -> TameCommands.adminViewDisabledTameTypes(ctx.getSource())))
                .then(Commands.literal("remove")
                        .executes(ctx -> TameCommands.adminRemoveTameTypeDisableUsage(ctx.getSource()))
                        .then(Commands.argument("type", StringArgumentType.string())
                                .suggests((ctx, b) -> TameCommands.suggestDisabledTameTypes(b))
                                .executes(ctx -> TameCommands.adminRemoveTameTypeDisable(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "type")
                                ))));
    }
}
