package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameCollarCommands {
    private TameCollarCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return build("collar");
    }

    static LiteralArgumentBuilder<CommandSourceStack> buildCollarTag() {
        return build("collarTag");
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String literal) {
        return Commands.literal(literal)
                .executes(ctx -> TameCommands.collarList(ctx.getSource(), false))
                .then(Commands.literal("notag")
                        .executes(ctx -> TameCommands.collarList(ctx.getSource(), true)))
                .then(Commands.literal("compatibleAmorEnchantments")
                        .executes(ctx -> TameCommands.compatibleArmorEnchantments(ctx.getSource())))
                .then(Commands.literal("compatibleArmorEnchantments")
                        .executes(ctx -> TameCommands.compatibleArmorEnchantments(ctx.getSource())));
    }
}
