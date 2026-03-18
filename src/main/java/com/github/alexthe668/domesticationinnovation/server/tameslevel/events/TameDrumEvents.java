package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlock;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TameDrumEvents {
    private static final int LEFT_CLICK_HOLD_TICKS = 60;
    private static final int PLACED_REPORT_HOLD_TICKS = 60;
    private static final int PLACED_CLEAR_GROUP_HOLD_TICKS = 200;
    private static final Map<UUID, LeftClickHold> LEFT_CLICK_HOLDS = new HashMap<>();
    private static final Map<UUID, RightClickHold> RIGHT_CLICK_HOLDS = new HashMap<>();
    private static final Map<UUID, PendingPassiveSit> PENDING_PASSIVE_SITS = new HashMap<>();
    private static final Map<UUID, PlacedDrumHold> PLACED_DRUM_HOLDS = new HashMap<>();

    public record RightClickHold(BlockPos blockPos) {
    }

    private record LeftClickHold(BlockPos blockPos, boolean sneaking, long startTick, ResourceKey<Level> dimension) {
    }

    private record PendingPassiveSit(BlockPos blockPos, ResourceKey<Level> dimension) {
    }

    private record PlacedDrumHold(BlockPos blockPos, ResourceKey<Level> dimension, long startTick, boolean sneaking, boolean reported) {
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DrumBlock) {
            if (!player.isShiftKeyDown()) {
                PLACED_DRUM_HOLDS.remove(player.getUUID());
                return;
            }
            PLACED_DRUM_HOLDS.put(player.getUUID(), new PlacedDrumHold(
                    event.getPos().immutable(),
                    player.level().dimension(),
                    player.level().getGameTime(),
                    false,
                    false
            ));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setUseBlock(Event.Result.DENY);
            return;
        }
        if (!isMainHandDrum(event.getItemStack())) return;
        if (player.isShiftKeyDown()) {
            TameCommands.drumCycleHeldMode(player, event.getItemStack());
        } else {
            TameCommands.drumClearTargets(player, event.getItemStack());
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
        if (!isMainHandDrum(stack)) return;
        Entity target = event.getTarget();

        if (player.isShiftKeyDown() && target instanceof TamableAnimal tame) {
            if (TameCommands.drumRemoveGroupTarget(player, stack, tame)) {
                event.setCanceled(true);
            }
            return;
        }
        if (target instanceof TamableAnimal tame && tame.isTame() && player.getUUID().equals(tame.getOwnerUUID())) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof LivingEntity living) {
            TameCommands.drumSetTargets(player, stack, living);
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
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof DrumBlock)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof DrumBlockEntity drum)) return;
        if (!player.isShiftKeyDown()) return;
        TameCommands.drumCyclePlacedMode(player, drum.getSelectorName());
        PLACED_DRUM_HOLDS.put(player.getUUID(), new PlacedDrumHold(
                event.getPos().immutable(),
                player.level().dimension(),
                player.level().getGameTime(),
                true,
                false
        ));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setUseBlock(Event.Result.DENY);
    }

    private static void handleGroupInteract(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, ItemStack stack, Entity target, PlayerInteractEvent event) {
        if (hand != InteractionHand.MAIN_HAND) return;
        if (!(rawPlayer instanceof ServerPlayer player)) return;
        if (!player.isShiftKeyDown()) return;
        if (!isMainHandDrum(stack)) return;
        if (!(target instanceof TamableAnimal tame)) return;
        if (TameCommands.drumAddGroupTarget(player, stack, tame)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer)) return;
        if (!isMainHandDrum(event.getPlayer().getMainHandItem())) return;
        if (event.getState().getBlock() instanceof DrumBlock) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        LeftClickHold hold = LEFT_CLICK_HOLDS.get(player.getUUID());
        if (hold != null) {
            if (!isMainHandDrum(player.getMainHandItem()) || player.level().dimension() != hold.dimension()) {
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
                }
                LEFT_CLICK_HOLDS.remove(player.getUUID());
            }
        }

        PlacedDrumHold placedHold = PLACED_DRUM_HOLDS.get(player.getUUID());
        if (placedHold != null) {
            if (player.level().dimension() != placedHold.dimension()) {
                PLACED_DRUM_HOLDS.remove(player.getUUID());
            } else {
                HitResult hit = player.pick(6.0D, 0.0F, false);
                boolean stillLooking = hit instanceof BlockHitResult bhr && bhr.getBlockPos().equals(placedHold.blockPos());
                if (!stillLooking) {
                    PLACED_DRUM_HOLDS.remove(player.getUUID());
                } else {
                    long heldTicks = player.level().getGameTime() - placedHold.startTick();
                    if (!placedHold.sneaking() && !placedHold.reported() && heldTicks >= PLACED_REPORT_HOLD_TICKS) {
                        if (player.level().getBlockEntity(placedHold.blockPos()) instanceof DrumBlockEntity drum) {
                            TameCommands.drumReportSelector(player, drum.getSelectorName());
                        }
                        PLACED_DRUM_HOLDS.put(player.getUUID(), new PlacedDrumHold(placedHold.blockPos(), placedHold.dimension(), placedHold.startTick(), false, true));
                    } else if (placedHold.sneaking() && heldTicks >= PLACED_CLEAR_GROUP_HOLD_TICKS) {
                        if (player.level().getBlockEntity(placedHold.blockPos()) instanceof DrumBlockEntity drum) {
                            TameCommands.drumClearGroupSelector(player, drum.getSelectorName());
                        }
                        PLACED_DRUM_HOLDS.remove(player.getUUID());
                    }
                }
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

    public static void beginRightClickHold(ServerPlayer player, BlockPos blockPos) {
        RIGHT_CLICK_HOLDS.put(player.getUUID(), new RightClickHold(blockPos == null ? null : blockPos.immutable()));
    }

    public static RightClickHold finishRightClickHold(ServerPlayer player) {
        return RIGHT_CLICK_HOLDS.remove(player.getUUID());
    }

    private static boolean isMainHandDrum(ItemStack stack) {
        return !stack.isEmpty() && stack.is(DIBlockRegistry.DRUM.get().asItem());
    }
}
