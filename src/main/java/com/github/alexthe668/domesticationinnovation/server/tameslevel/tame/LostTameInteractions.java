package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class LostTameInteractions {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract event) { handle(event, event.getTarget()); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void specific(PlayerInteractEvent.EntityInteractSpecific event) { handle(event, event.getTarget()); }
    private static void handle(PlayerInteractEvent event, net.minecraft.world.entity.Entity target) {
        if (!(target instanceof LivingEntity living) || !TameLimitService.isReleased(living)) return;
        // Always intercept vanilla/mod taming so no alternate food bypasses the cooldown.
        event.setCanceled(true);
        event.setCancellationResult(event.getItemStack().isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (TameDuelManager.isEntityInDuel(player.getUUID())) {
            player.displayClientMessage(Component.literal("Finish your duel before retaming a lost creature."), true);return;
        }
        String error = TameLimitService.retameLost(player, living, event.getHand());
        player.displayClientMessage(Component.literal(error == null ? "Retamed " + living.getName().getString() + "." : error), true);
    }
}
