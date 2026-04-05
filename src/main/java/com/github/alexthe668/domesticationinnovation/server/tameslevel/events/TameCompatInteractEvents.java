package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class TameCompatInteractEvents {
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleCompatInteraction(event.getEntity(), event.getHand(), event.getItemStack().isEmpty(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleCompatInteraction(event.getEntity(), event.getHand(), event.getItemStack().isEmpty(), event.getTarget(), event);
    }

    private static void handleCompatInteraction(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, boolean emptyHand, Entity target, PlayerInteractEvent event) {
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        if (hand != InteractionHand.MAIN_HAND || !emptyHand || player.isShiftKeyDown()) return;
        if (!(target instanceof TamableAnimal tame) || !tame.isTame()) return;
        if (!isCompatCycleType(tame)) return;

        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || data.ownerUUID == null || !data.ownerUUID.equals(player.getUUID())) return;

        if (TameCommands.cycleCompatInteractionMovementOrder(player, tame)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setResult(Event.Result.ALLOW);
        }
    }

    private static boolean isCompatCycleType(TamableAnimal tame) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (key == null) {
            return false;
        }
        String namespace = key.getNamespace();
        return "crittersandcompanions".equals(namespace) || "legendary_monsters".equals(namespace);
    }
}
