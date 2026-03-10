package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class TameAutoFollowEvents {
    private static final boolean ENABLED = false;
    private static final int DIMENSION_FOLLOW_DELAY_TICKS = 100;
    private static final int COMMAND_TP_FOLLOW_DELAY_TICKS = 10;
    private static final int STABILIZE_RETRY_DELAY_TICKS = 20;
    private static final int STABILIZE_MAX_RETRIES = 20;
    private static final Map<UUID, Long> PENDING_FOLLOW = new HashMap<>();
    private static final Map<UUID, StabilizeFollow> STABILIZE_FOLLOW = new HashMap<>();
    private static long serverTick = 0L;

    private static final class StabilizeFollow {
        private final UUID ownerUuid;
        private long dueTick;
        private int retriesLeft;

        private StabilizeFollow(UUID ownerUuid, long dueTick, int retriesLeft) {
            this.ownerUuid = ownerUuid;
            this.dueTick = dueTick;
            this.retriesLeft = retriesLeft;
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!ENABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_FOLLOW.put(player.getUUID(), serverTick + DIMENSION_FOLLOW_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onPlayerTeleportedByCommand(EntityTeleportEvent.TeleportCommand event) {
        if (!ENABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        PENDING_FOLLOW.put(player.getUUID(), serverTick + COMMAND_TP_FOLLOW_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED) return;
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;
        if (!PENDING_FOLLOW.isEmpty()) {
            Iterator<Map.Entry<UUID, Long>> iterator = PENDING_FOLLOW.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                if (entry.getValue() > serverTick) continue;

                ServerPlayer player = event.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
                    teleportFollowingLoadedTamesToOwner(player);
                }
                iterator.remove();
            }
        }

        if (!STABILIZE_FOLLOW.isEmpty()) {
            Iterator<Map.Entry<UUID, StabilizeFollow>> stabilizeIt = STABILIZE_FOLLOW.entrySet().iterator();
            while (stabilizeIt.hasNext()) {
                Map.Entry<UUID, StabilizeFollow> entry = stabilizeIt.next();
                UUID tameId = entry.getKey();
                StabilizeFollow follow = entry.getValue();
                if (follow == null || follow.retriesLeft <= 0 || follow.dueTick > serverTick) continue;

                ServerPlayer owner = event.getServer().getPlayerList().getPlayer(follow.ownerUuid);
                if (owner == null) {
                    stabilizeIt.remove();
                    continue;
                }
                TamableAnimal tame = findLoadedOwnedTame(owner, tameId);
                if (tame == null || !tame.isAlive()) {
                    follow.retriesLeft--;
                    follow.dueTick = serverTick + STABILIZE_RETRY_DELAY_TICKS;
                    if (follow.retriesLeft <= 0) {
                        stabilizeIt.remove();
                    }
                    continue;
                }
                stabilizeTameAfterDimensionFollow(tame, owner);
                follow.retriesLeft--;
                if (follow.retriesLeft <= 0) {
                    stabilizeIt.remove();
                } else {
                    follow.dueTick = serverTick + STABILIZE_RETRY_DELAY_TICKS;
                }
            }
        }
    }

    private static void teleportFollowingLoadedTamesToOwner(ServerPlayer owner) {
        if (owner == null || owner.server == null) return;
        UUID ownerId = owner.getUUID();

        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null) continue;
            if (!ownerId.equals(data.ownerUUID)) continue;

            TamableAnimal tame = findLoadedOwnedTame(owner, data.uuid);
            if (tame == null) continue;
            if (tame.isOrderedToSit()) continue;
            teleportTameToPlayer(tame, owner);
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

    private static void teleportTameToPlayer(TamableAnimal tame, ServerPlayer player) {
        if (tame == null || player == null) return;
        if (tame.level().dimension().equals(player.level().dimension())) {
            tame.teleportTo(player.getX(), player.getY(), player.getZ());
            stabilizeTameAfterDimensionFollow(tame, player);
            STABILIZE_FOLLOW.put(tame.getUUID(), new StabilizeFollow(
                    player.getUUID(),
                    serverTick + STABILIZE_RETRY_DELAY_TICKS,
                    STABILIZE_MAX_RETRIES
            ));
            return;
        }

        TamableAnimal movedTame = cloneAcrossDimension(tame, player);
        if (movedTame != null) {
            stabilizeTameAfterDimensionFollow(movedTame, player);
            STABILIZE_FOLLOW.put(movedTame.getUUID(), new StabilizeFollow(
                    player.getUUID(),
                    serverTick + STABILIZE_RETRY_DELAY_TICKS,
                    STABILIZE_MAX_RETRIES
            ));
        }
    }

    private static TamableAnimal cloneAcrossDimension(TamableAnimal tame, ServerPlayer player) {
        if (tame == null || player == null) return null;
        if (!(tame.level() instanceof ServerLevel sourceLevel)) return null;
        ServerLevel destinationLevel = player.serverLevel();
        if (destinationLevel == null) return null;

        try {
            CompoundTag snapshot = new CompoundTag();
            tame.save(snapshot);

            Entity spawned = tame.getType().create(destinationLevel);
            if (!(spawned instanceof TamableAnimal movedTame)) {
                return null;
            }
            if (!snapshot.isEmpty()) {
                movedTame.load(snapshot);
            }

            movedTame.setUUID(tame.getUUID());
            movedTame.moveTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
            movedTame.setDeltaMovement(Vec3.ZERO);
            enforceTamedOwnerPreserveCollar(movedTame, tame.getOwnerUUID());

            if (!destinationLevel.addFreshEntity(movedTame)) {
                return null;
            }

            tame.discard();

            TameData data = TameRegistry.get(movedTame.getUUID());
            if (data != null) {
                data.lastKnownDimension = destinationLevel.dimension().location().toString();
                data.lastKnownX = movedTame.blockPosition().getX();
                data.lastKnownY = movedTame.blockPosition().getY();
                data.lastKnownZ = movedTame.blockPosition().getZ();
                data.lastKnownGameTime = destinationLevel.getGameTime();
                CompoundTag refreshedSnapshot = new CompoundTag();
                movedTame.save(refreshedSnapshot);
                data.entitySnapshot = refreshedSnapshot;
                TameRegistry.markDirty();
            }

            return movedTame;
        } catch (Throwable t) {
            System.err.println("[TamesLevel] Cross-dimension clone-follow failed for tame " + tame.getUUID() + ": " + t.getClass().getSimpleName() + ": " + t.getMessage());
            return null;
        }
    }

    private static void stabilizeTameAfterDimensionFollow(TamableAnimal tame, ServerPlayer player) {
        if (tame == null || player == null) return;
        if (tame.isNoAi()) {
            tame.setNoAi(false);
        }
        if (tame.distanceToSqr(player) > (24.0D * 24.0D)) {
            tame.teleportTo(player.getX(), player.getY(), player.getZ());
        }
        tame.setDeltaMovement(Vec3.ZERO);
        tame.setTarget(null);
        tame.getNavigation().stop();
        tame.setOrderedToSit(false);
        TameGoalInstaller.installIfMissing(tame);
        tame.getNavigation().moveTo(player, 1.0D);
    }

    private static void enforceTamedOwnerPreserveCollar(TamableAnimal tame, UUID ownerId) {
        if (tame == null) return;
        DyeColor collar = null;
        if (tame instanceof Wolf wolf) {
            collar = wolf.getCollarColor();
        }
        if (ownerId == null && !tame.isTame()) {
            tame.setTame(true);
        }
        if (ownerId != null && !ownerId.equals(tame.getOwnerUUID())) {
            tame.setOwnerUUID(ownerId);
        }
        if (collar != null && tame instanceof Wolf wolf) {
            wolf.setCollarColor(collar);
        }
    }
}
