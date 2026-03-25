package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TamePortalStabilizeEvents {
    private static final int STABILIZE_INITIAL_DELAY_TICKS = 20;
    private static final int STABILIZE_RETRY_INTERVAL_TICKS = 20;
    private static final int STABILIZE_RETRIES = 10;
    private static final Map<UUID, StabilizeState> PENDING_STABILIZE = new HashMap<>();
    private static long serverTick = 0L;

    private static final class StabilizeState {
        private long dueTick;
        private int retriesLeft;

        private StabilizeState(long dueTick, int retriesLeft) {
            this.dueTick = dueTick;
            this.retriesLeft = retriesLeft;
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_STABILIZE.put(player.getUUID(), new StabilizeState(
                serverTick + STABILIZE_INITIAL_DELAY_TICKS,
                STABILIZE_RETRIES
        ));
    }

    @SubscribeEvent
    public static void onTameTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) {
            return;
        }
        if (tame.getOwnerUUID() == null) {
            return;
        }
        if (TameRegistry.canEnterPortalsByThemselves(tame.getOwnerUUID())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;
        if (PENDING_STABILIZE.isEmpty()) return;

        Iterator<Map.Entry<UUID, StabilizeState>> it = PENDING_STABILIZE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, StabilizeState> entry = it.next();
            StabilizeState state = entry.getValue();
            if (state == null || state.retriesLeft <= 0) {
                it.remove();
                continue;
            }
            if (state.dueTick > serverTick) continue;

            ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                stabilizeOwnerTamesInCurrentDimension(player);
            }
            state.retriesLeft--;
            if (state.retriesLeft <= 0) {
                it.remove();
            } else {
                state.dueTick = serverTick + STABILIZE_RETRY_INTERVAL_TICKS;
            }
        }
    }

    private static void stabilizeOwnerTamesInCurrentDimension(ServerPlayer owner) {
        if (owner == null || owner.server == null) return;
        UUID ownerId = owner.getUUID();
        for (TameData data : TameRegistry.getOwned(ownerId)) {
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

