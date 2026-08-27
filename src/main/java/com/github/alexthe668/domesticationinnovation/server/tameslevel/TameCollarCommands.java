package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

final class TameCollarCommands {
    private TameCollarCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("collar")
                .executes(ctx -> TameCommands.collarList(ctx.getSource(), false))
                .then(Commands.literal("notag")
                        .executes(ctx -> TameCommands.collarList(ctx.getSource(), true)))
                .then(Commands.literal("compatibleArmorEnchantments")
                        .executes(ctx -> TameCommands.compatibleArmorEnchantments(ctx.getSource())))
                .then(Commands.literal("amorSystem")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "armor")))
                .then(Commands.literal("armorSystem")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "armor")));
    }
}
