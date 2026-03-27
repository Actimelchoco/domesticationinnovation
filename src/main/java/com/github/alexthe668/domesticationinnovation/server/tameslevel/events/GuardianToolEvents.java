package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuardianToolEvents {
    private static final int LEFT_CLICK_CONFIRM_HOLD_TICKS = 60;
    private static final Map<UUID, LeftClickHold> LEFT_CLICK_HOLDS = new HashMap<>();

    private record LeftClickHold(BlockPos blockPos, ResourceKey<Level> dimension, long startTick) {
    }

    public static boolean isGuardianToolArrow(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.ARROW);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isGuardianToolArrow(event.getItemStack())) return;
        if (player.isShiftKeyDown()) {
            LEFT_CLICK_HOLDS.put(player.getUUID(), new LeftClickHold(
                    event.getPos().immutable(),
                    player.level().dimension(),
                    player.level().getGameTime()
            ));
        } else {
            TameCommands.guardianToolClearAnchorsAtBlock(player, event.getItemStack(), event.getPos());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isGuardianToolArrow(event.getItemStack())) return;
        if (player.isShiftKeyDown()) {
            TameCommands.guardianToolDeploySet(player, event.getItemStack());
        } else {
            TameCommands.guardianToolSetNextAnchor(player, event.getItemStack(), event.getPos().above());
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleEntityInteract(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleEntityInteract(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!isGuardianToolArrow(stack)) return;
        Entity target = event.getTarget();
        if (!(target instanceof TamableAnimal tame) || !tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) {
            return;
        }
        boolean changed = player.isShiftKeyDown()
                ? TameCommands.guardianToolRemoveCurrentAnchorFromSet(player, stack, tame)
                : TameCommands.guardianToolRemoveGroupTarget(player, stack, tame);
        if (changed) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!isGuardianToolArrow(player.getMainHandItem())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        LeftClickHold hold = LEFT_CLICK_HOLDS.get(player.getUUID());
        if (hold == null) {
            return;
        }
        if (!isGuardianToolArrow(player.getMainHandItem()) || player.level().dimension() != hold.dimension() || !player.isShiftKeyDown()) {
            LEFT_CLICK_HOLDS.remove(player.getUUID());
            return;
        }
        if (player.level().getGameTime() - hold.startTick() < LEFT_CLICK_CONFIRM_HOLD_TICKS) {
            return;
        }
        HitResult hit = player.pick(6.0D, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(hold.blockPos())) {
            TameCommands.guardianToolQueueClearConfirm(player, player.getMainHandItem());
        }
        LEFT_CLICK_HOLDS.remove(player.getUUID());
    }

    private static void handleEntityInteract(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, ItemStack stack, Entity target, PlayerInteractEvent event) {
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        if (!isGuardianToolArrow(stack)) return;
        if (!(target instanceof TamableAnimal tame)) return;
        boolean changed = player.isShiftKeyDown()
                ? TameCommands.guardianToolAddCurrentAnchorToSet(player, stack, tame)
                : TameCommands.guardianToolAddGroupTarget(player, stack, tame);
        if (changed) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setResult(Event.Result.ALLOW);
        }
    }
}
