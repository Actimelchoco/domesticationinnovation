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
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TameDrumEvents {
    private static final String TAG_BONE_COMMAND = "DIBoneCommand";
    private static final int DEFAULT_BONE_COMMAND = 2;
    private static final int LEFT_CLICK_HOLD_TICKS = 60;
    private static final int RIGHT_CLICK_HOLD_TICKS = 100;
    private static final Map<UUID, LeftClickHold> LEFT_CLICK_HOLDS = new HashMap<>();
    private static final Map<UUID, RightClickHold> RIGHT_CLICK_HOLDS = new HashMap<>();
    private static final Map<UUID, PendingPassiveSit> PENDING_PASSIVE_SITS = new HashMap<>();

    private record LeftClickHold(BlockPos blockPos, boolean sneaking, long startTick, ResourceKey<Level> dimension) {
    }

    private record PendingPassiveSit(BlockPos blockPos, ResourceKey<Level> dimension) {
    }

    private record RightClickHold(BlockPos blockPos, ResourceKey<Level> dimension, long startTick, InteractionHand hand) {
    }

    public static boolean isControllerBone(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.BONE);
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!isControllerBone(stack)) {
            return;
        }
        int affected = TameCommands.drumCycleHeldMode(player, stack);
        if (affected > 0) {
            consumeOne(player, stack);
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isControllerBone(event.getItemStack())) return;

        if (!player.isShiftKeyDown()) {
            int cleared = TameCommands.drumClearTargets(player, event.getItemStack());
            if (cleared > 0) {
                consumeOne(player, event.getItemStack());
            }
        }
        LEFT_CLICK_HOLDS.put(player.getUUID(), new LeftClickHold(
                event.getPos().immutable(),
                player.isShiftKeyDown(),
                player.level().getGameTime(),
                player.level().dimension()
        ));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = player.getMainHandItem();
        if (!isControllerBone(stack)) return;
        Entity target = event.getTarget();

        if (target instanceof TamableAnimal tame && tame.isTame() && player.getUUID().equals(tame.getOwnerUUID())) {
            if (TameCommands.drumRemoveGroupTarget(player, stack, tame)) {
                consumeOne(player, stack);
            }
            event.setCanceled(true);
            return;
        }
        if (target instanceof LivingEntity living) {
            int count = TameCommands.drumSetTargets(player, stack, living);
            if (count > 0) {
                consumeOne(player, stack);
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        handleGroupInteract(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        handleGroupInteract(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget(), event);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isControllerBone(event.getItemStack())) return;
        startRightClickHold(player, null, event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isControllerBone(event.getItemStack())) return;
        startRightClickHold(player, event.getPos(), event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setUseBlock(Event.Result.DENY);
        event.setUseItem(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public static void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        resolveRightClickHold(event.getEntity(), event.getItem());
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        resolveRightClickHold(event.getEntity(), event.getItem());
    }

    private static void resolveRightClickHold(LivingEntity entity, ItemStack stack) {
        if (!(entity instanceof ServerPlayer player) || !isControllerBone(stack)) {
            return;
        }
        RightClickHold hold = RIGHT_CLICK_HOLDS.remove(player.getUUID());
        if (hold == null || hold.dimension() != player.level().dimension()) {
            return;
        }
        long heldTicks = Math.max(0L, player.level().getGameTime() - hold.startTick());
        int result;
        if (heldTicks >= RIGHT_CLICK_HOLD_TICKS) {
            result = hold.blockPos() == null
                    ? TameCommands.drumTeleportHome(player, stack)
                    : TameCommands.drumTeleportToBlock(player, stack, hold.blockPos());
            if (result > 0) {
                consumeOne(player, stack);
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Bone: teleport triggered.")
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE), true);
            }
            return;
        }
        int command = currentBoneCommand(player);
        result = TameCommands.drumIssueMovementCommand(player, stack, command);
        if (result > 0) {
            consumeOne(player, stack);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Bone: " + TameCommands.drumCommandLabel(command) + " (" + result + ")")
                    .withStyle(net.minecraft.ChatFormatting.GOLD), true);
        }
    }

    private static void startRightClickHold(ServerPlayer player, BlockPos blockPos, InteractionHand hand) {
        player.startUsingItem(hand);
        RIGHT_CLICK_HOLDS.put(player.getUUID(), new RightClickHold(
                blockPos == null ? null : blockPos.immutable(),
                player.level().dimension(),
                player.level().getGameTime(),
                hand
        ));
    }

    private static void handleGroupInteract(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, ItemStack stack, Entity target, PlayerInteractEvent event) {
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        if (!isControllerBone(stack)) return;
        if (!(target instanceof TamableAnimal tame)) return;
        if (TameCommands.drumAddGroupTarget(player, stack, tame)) {
            consumeOne(player, stack);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!isControllerBone(player.getMainHandItem())) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        LeftClickHold hold = LEFT_CLICK_HOLDS.get(player.getUUID());
        if (hold != null) {
            if (!isControllerBone(player.getMainHandItem()) || player.level().dimension() != hold.dimension()) {
                LEFT_CLICK_HOLDS.remove(player.getUUID());
            } else if (player.level().getGameTime() - hold.startTick() >= LEFT_CLICK_HOLD_TICKS) {
                HitResult hit = player.pick(6.0D, 0.0F, false);
                if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(hold.blockPos())) {
                    if (hold.sneaking()) {
                        for (UUID tameId : TameCommands.drumStartMoveAndSitPassive(player, player.getMainHandItem(), hold.blockPos())) {
                            PENDING_PASSIVE_SITS.put(tameId, new PendingPassiveSit(hold.blockPos(), player.level().dimension()));
                        }
                    } else {
                        TameCommands.drumSetGuardianAnchor(player, player.getMainHandItem(), hold.blockPos());
                    }
                    consumeOne(player, player.getMainHandItem());
                }
                LEFT_CLICK_HOLDS.remove(player.getUUID());
            }
        }

        if (PENDING_PASSIVE_SITS.isEmpty()) {
            return;
        }
        java.util.Iterator<Map.Entry<UUID, PendingPassiveSit>> iterator = PENDING_PASSIVE_SITS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PendingPassiveSit> entry = iterator.next();
            PendingPassiveSit pending = entry.getValue();
            Entity entity = null;
            for (var level : player.getServer().getAllLevels()) {
                entity = level.getEntity(entry.getKey());
                if (entity != null) break;
            }
            if (!(entity instanceof TamableAnimal tame) || !tame.isAlive()) {
                iterator.remove();
                continue;
            }
            if (tame.level().dimension() != pending.dimension()) {
                iterator.remove();
                continue;
            }
            double dx = tame.getX() - (pending.blockPos().getX() + 0.5D);
            double dy = tame.getY() - pending.blockPos().getY();
            double dz = tame.getZ() - (pending.blockPos().getZ() + 0.5D);
            if ((dx * dx) + (dy * dy) + (dz * dz) <= 2.25D) {
                TameCommands.drumFinalizePassiveSit(tame);
                iterator.remove();
            }
        }
    }

    private static int currentBoneCommand(ServerPlayer player) {
        int value = player.getPersistentData().getInt(TAG_BONE_COMMAND);
        return value < 0 || value > 2 ? DEFAULT_BONE_COMMAND : value;
    }

    private static int nextBoneCommand(ServerPlayer player) {
        int next = (currentBoneCommand(player) + 1) % 3;
        player.getPersistentData().putInt(TAG_BONE_COMMAND, next);
        return next;
    }

    private static void consumeOne(ServerPlayer player, ItemStack stack) {
        if (player.getAbilities().instabuild || stack.isEmpty()) {
            return;
        }
        stack.shrink(1);
    }
}
