package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
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
                .then(Commands.literal("openArmorSlots")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests((ctx, builder) -> TameCommands.suggestOwnedPetNamesAll(ctx.getSource(), builder))
                                .executes(ctx -> TameCommands.armorInventoryOpen(
                                        ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("armorSystem")
                        .executes(ctx -> TameCommands.infoDetail(ctx.getSource(), "armor")));
    }
}
