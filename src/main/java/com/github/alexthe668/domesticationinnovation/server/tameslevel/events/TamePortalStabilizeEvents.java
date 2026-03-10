package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TamePortalStabilizeEvents {
    private static final int STABILIZE_DELAY_TICKS = 60;
    private static final Map<UUID, Long> PENDING_STABILIZE = new HashMap<>();
    private static long serverTick = 0L;

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_STABILIZE.put(player.getUUID(), serverTick + STABILIZE_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;
        if (PENDING_STABILIZE.isEmpty()) return;

        Iterator<Map.Entry<UUID, Long>> it = PENDING_STABILIZE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            if (entry.getValue() > serverTick) continue;

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                stabilizeOwnerTamesInCurrentDimension(player);
            }
            it.remove();
        }
    }

    private static void stabilizeOwnerTamesInCurrentDimension(ServerPlayer owner) {
        if (owner == null || owner.server == null) return;
        UUID ownerId = owner.getUUID();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null) continue;
            if (!ownerId.equals(data.ownerUUID)) continue;

            TamableAnimal tame = findLoadedOwnedTame(owner, data.uuid);
            if (tame == null || !tame.isAlive()) continue;
            if (!tame.level().dimension().equals(owner.level().dimension())) continue;

            if (tame.isNoAi()) {
                tame.setNoAi(false);
            }
            tame.setDeltaMovement(Vec3.ZERO);
            tame.getNavigation().stop();
            tame.setTarget(null);
            TameGoalInstaller.installIfMissing(tame);
            if (!tame.isOrderedToSit()) {
                tame.getNavigation().moveTo(owner, 1.0D);
            }
        }
    }

    private static TamableAnimal findLoadedOwnedTame(ServerPlayer owner, UUID tameUuid) {
        for (var level : owner.server.getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
            if (!owner.getUUID().equals(tame.getOwnerUUID())) continue;
            return tame;
        }
        return null;
    }
}

