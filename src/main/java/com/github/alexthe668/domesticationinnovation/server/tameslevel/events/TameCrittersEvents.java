package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TameCrittersEvents {
    private static final String DRAGONFLY_CLASS = "com.github.eterdelta.crittersandcompanions.entity.DragonflyEntity";
    private static final Map<UUID, PendingCommandActionBar> PENDING_ACTION_BARS = new HashMap<>();

    private record PendingCommandActionBar(UUID tameUuid, int previousOrderCode, long dueTick) {
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        queueDragonflyCommandActionBar(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget());
    }

    @SubscribeEvent
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        queueDragonflyCommandActionBar(event.getEntity(), event.getHand(), event.getItemStack(), event.getTarget());
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        PendingCommandActionBar pending = PENDING_ACTION_BARS.get(player.getUUID());
        if (pending == null || player.level().getGameTime() < pending.dueTick()) {
            return;
        }
        PENDING_ACTION_BARS.remove(player.getUUID());

        TamableAnimal tame = findLoadedTameByUuid(player.serverLevel(), pending.tameUuid());
        if (tame == null || !tame.isAlive() || !isCrittersDragonfly(tame)) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        int currentOrderCode = TameCommands.captureMovementOrderCode(tame, data);
        if (currentOrderCode == pending.previousOrderCode() || currentOrderCode == 3) {
            return;
        }
        int messageId = switch (currentOrderCode) {
            case 1 -> 1;
            case 2 -> 0;
            default -> 2;
        };
        player.displayClientMessage(
                Component.translatable("message.domesticationinnovation.command_" + messageId, tame.getName()),
                true
        );
    }

    private static void queueDragonflyCommandActionBar(net.minecraft.world.entity.player.Player rawPlayer, InteractionHand hand, net.minecraft.world.item.ItemStack stack, Entity target) {
        if (hand != InteractionHand.MAIN_HAND || !(rawPlayer instanceof ServerPlayer player)) {
            return;
        }
        if (!(target instanceof TamableAnimal tame) || !tame.isTame() || !isCrittersDragonfly(tame)) {
            return;
        }
        if (stack != null && !stack.isEmpty()) {
            return;
        }
        if (tame.getOwnerUUID() == null || !tame.getOwnerUUID().equals(player.getUUID())) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        int previousOrderCode = TameCommands.captureMovementOrderCode(tame, data);
        PENDING_ACTION_BARS.put(player.getUUID(), new PendingCommandActionBar(
                tame.getUUID(),
                previousOrderCode,
                player.level().getGameTime() + 1L
        ));
    }

    private static boolean isCrittersDragonfly(TamableAnimal tame) {
        return tame != null && DRAGONFLY_CLASS.equals(tame.getClass().getName());
    }

    private static TamableAnimal findLoadedTameByUuid(ServerLevel origin, UUID uuid) {
        if (origin == null || uuid == null || origin.getServer() == null) {
            return null;
        }
        for (ServerLevel level : origin.getServer().getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof TamableAnimal tame && tame.isTame()) {
                return tame;
            }
        }
        return null;
    }
}
