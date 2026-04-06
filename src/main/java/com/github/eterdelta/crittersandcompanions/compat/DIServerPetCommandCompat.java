package com.github.eterdelta.crittersandcompanions.compat;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = "crittersandcompanions")
public final class DIServerPetCommandCompat {
    private static final String COMMAND_TAG = "di_server_pet_command";
    private static final int FOLLOW = 0;
    private static final int SIT = 1;
    private static final int WANDER = 2;

    private DIServerPetCommandCompat() {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleInteract(event.getEntity(), event.getHand(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleInteract(event.getEntity(), event.getHand(), event.getTarget(), event);
    }

    private static void handleInteract(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, Entity target, PlayerInteractEvent event) {
        if (!(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return;
        }
        if (!(target instanceof TamableAnimal tame) || !tame.isTame() || !isCrittersPet(tame)) {
            return;
        }
        UUID owner = tame.getOwnerUUID();
        if (owner == null || !owner.equals(player.getUUID())) {
            return;
        }

        int next = (readCommand(tame) + 1) % 3;
        writeCommand(tame, next);
        applyCommandState(tame, player, next, true);

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame) || tame.level().isClientSide || !tame.isTame() || !isCrittersPet(tame)) {
            return;
        }
        applyCommandState(tame, null, readCommand(tame), false);
    }

    private static void applyCommandState(TamableAnimal tame, ServerPlayer player, int command, boolean sendFeedback) {
        switch (command) {
            case SIT -> {
                tame.setOrderedToSit(true);
                tame.setInSittingPose(true);
                tame.getNavigation().stop();
            }
            case WANDER -> {
                tame.setOrderedToSit(false);
                tame.setInSittingPose(false);
            }
            default -> {
                tame.setOrderedToSit(false);
                tame.setInSittingPose(false);
                followOwner(tame);
            }
        }
        if (sendFeedback && player != null) {
            player.displayClientMessage(Component.literal(labelFor(command)), true);
        }
    }

    private static void followOwner(TamableAnimal tame) {
        LivingEntity owner = tame.getOwner();
        if (owner == null || owner.level() != tame.level()) {
            return;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return;
        }
        double distanceSq = tame.distanceToSqr(owner);
        if (distanceSq <= 3.0D * 3.0D) {
            tame.getNavigation().stop();
            return;
        }
        if (distanceSq >= 16.0D * 16.0D) {
            tame.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
            tame.getNavigation().stop();
            return;
        }
        tame.getNavigation().moveTo(owner, 1.15D);
    }

    private static int readCommand(TamableAnimal tame) {
        return tame.getPersistentData().getInt(COMMAND_TAG);
    }

    private static void writeCommand(TamableAnimal tame, int command) {
        tame.getPersistentData().putInt(COMMAND_TAG, command);
    }

    private static boolean isCrittersPet(TamableAnimal tame) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null && "crittersandcompanions".equals(key.getNamespace());
    }

    private static String labelFor(int command) {
        return switch (command) {
            case SIT -> "Pet command: sit";
            case WANDER -> "Pet command: wander";
            default -> "Pet command: follow";
        };
    }
}
