package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Prevents Animights from performing its irreversible wolf conversion before TL confirmation. */
public final class AnimightsCatinoCompat {
    private static final ResourceLocation GREAT_ESSENCE = new ResourceLocation("knightlib", "great_essence");

    private AnimightsCatinoCompat() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onWolfInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !(event.getTarget() instanceof LivingEntity host)
                || ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()) == null
                || !GREAT_ESSENCE.equals(ForgeRegistries.ITEMS.getKey(event.getItemStack().getItem()))) {
            return;
        }
        String resultId;
        String resultName;
        if (host instanceof Cat) {
            resultId = "animights:catwain";
            resultName = "Catwain";
        } else if (host instanceof Wolf) {
            resultId = "animights:canito";
            resultName = "Canito";
        } else if (host instanceof Axolotl) {
            resultId = "animights:lancelotl";
            resultName = "Lancelotl";
        } else if (host instanceof Rabbit) {
            resultId = "animights:jumpy";
            resultName = "Jumpy";
        } else {
            return;
        }
        if (!player.level().isClientSide) {
            TameCommands.requestAnimightsConversion(player, host, resultId, resultName);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
    }
}
