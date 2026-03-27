package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TameAutoFollowEvents {
    private static final boolean ENABLED = true;
    private static final int DIMENSION_FOLLOW_DELAY_TICKS = 20;
    private static final int COMMAND_TP_FOLLOW_DELAY_TICKS = 10;
    private static final int STABILIZE_RETRY_DELAY_TICKS = 20;
    private static final int STABILIZE_MAX_RETRIES = 20;
    private static final Map<UUID, PendingLoadedPetFollow> PENDING_LOADED_PETS = new HashMap<>();
    private static final Map<UUID, PendingUnloadedFollow> PENDING_UNLOADED_FOLLOW = new HashMap<>();
    private static final Map<UUID, StabilizeFollow> STABILIZE_FOLLOW = new HashMap<>();
    private static final Map<UUID, Long> SUPPRESSED_OWNER_TELEPORTS = new HashMap<>();
    private static long serverTick = 0L;

    private static final class PendingLoadedPetFollow {
        private final UUID tameUuid;
        private final UUID ownerUuid;
        private final String targetDimension;
        private final Vec3 targetPos;
        private final float yRot;
        private final float xRot;
        private final long dueTick;
        private final TamableAnimal tame;

        private PendingLoadedPetFollow(UUID tameUuid, UUID ownerUuid, String targetDimension, Vec3 targetPos, float yRot, float xRot, long dueTick, TamableAnimal tame) {
            this.tameUuid = tameUuid;
            this.ownerUuid = ownerUuid;
            this.targetDimension = targetDimension;
            this.targetPos = targetPos;
            this.yRot = yRot;
            this.xRot = xRot;
            this.dueTick = dueTick;
            this.tame = tame;
        }
    }

    private static final class PendingUnloadedFollow {
        private final UUID ownerUuid;
        private final String targetDimension;
        private final Vec3 targetPos;
        private final float yRot;
        private final float xRot;
        private final long dueTick;
        private final Set<UUID> loadedCandidates;

        private PendingUnloadedFollow(UUID ownerUuid, String targetDimension, Vec3 targetPos, float yRot, float xRot, long dueTick, Set<UUID> loadedCandidates) {
            this.ownerUuid = ownerUuid;
            this.targetDimension = targetDimension;
            this.targetPos = targetPos;
            this.yRot = yRot;
            this.xRot = xRot;
            this.dueTick = dueTick;
            this.loadedCandidates = loadedCandidates;
        }
    }

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
        // Cross-dimension follow capture happens before travel in onPlayerTravelToDimension.
    }

    @SubscribeEvent
    public static void onPlayerTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!ENABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isOwnerTeleportSuppressed(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel fromLevel)) return;
        ServerLevel targetLevel = fromLevel.getServer().getLevel(event.getDimension());
        if (targetLevel == null) return;
        scheduleFollow(player, fromLevel, targetLevel, player.position(), null, player.getYRot(), player.getXRot(), DIMENSION_FOLLOW_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onPlayerTeleportedByCommand(EntityTeleportEvent.TeleportCommand event) {
        if (!ENABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isOwnerTeleportSuppressed(player.getUUID())) return;
        scheduleFollow(player, player.serverLevel(), player.serverLevel(), event.getPrev(), event.getTarget(), player.getYRot(), player.getXRot(), COMMAND_TP_FOLLOW_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onPlayerTeleported(EntityTeleportEvent event) {
        if (!ENABLED) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event instanceof EntityTeleportEvent.TeleportCommand) return;
        if (isOwnerTeleportSuppressed(player.getUUID())) return;
        scheduleFollow(player, player.serverLevel(), player.serverLevel(), event.getPrev(), event.getTarget(), player.getYRot(), player.getXRot(), COMMAND_TP_FOLLOW_DELAY_TICKS);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED) return;
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;
        if (!PENDING_LOADED_PETS.isEmpty()) {
            Iterator<Map.Entry<UUID, PendingLoadedPetFollow>> iterator = PENDING_LOADED_PETS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, PendingLoadedPetFollow> entry = iterator.next();
                PendingLoadedPetFollow follow = entry.getValue();
                if (follow == null || follow.dueTick > serverTick) continue;

                ServerPlayer player = event.getServer().getPlayerList().getPlayer(follow.ownerUuid);
                if (player != null) {
                    ServerLevel targetLevel = resolvePendingTargetLevel(event, player, follow);
                    if (targetLevel != null) {
                        executeQueuedLoadedFollow(player, targetLevel, follow);
                    }
                }
                iterator.remove();
            }
        }

        if (!PENDING_UNLOADED_FOLLOW.isEmpty()) {
            Iterator<Map.Entry<UUID, PendingUnloadedFollow>> iterator = PENDING_UNLOADED_FOLLOW.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, PendingUnloadedFollow> entry = iterator.next();
                PendingUnloadedFollow follow = entry.getValue();
                if (follow == null || follow.dueTick > serverTick) continue;

                ServerPlayer player = event.getServer().getPlayerList().getPlayer(follow.ownerUuid);
                if (player != null) {
                    ServerLevel targetLevel = resolvePendingTargetLevel(event, player, follow);
                    Vec3 targetPos = follow.targetPos == null ? player.position() : follow.targetPos;
                    if (targetLevel != null) {
                        executeQueuedUnloadedFollow(player, targetLevel, targetPos, follow.yRot, follow.xRot, follow.loadedCandidates);
                    }
                }
                iterator.remove();
            }
        }

        if (serverTick % 40L == 0L) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (player != null && player.isAlive()) {
                    pullUnloadedFollowingTamesToOwner(player);
                }
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
        if (!SUPPRESSED_OWNER_TELEPORTS.isEmpty()) {
            SUPPRESSED_OWNER_TELEPORTS.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < serverTick);
        }
    }

    public static void suppressOwnerTeleportFollow(UUID ownerUuid, int ticks) {
        if (ownerUuid == null) {
            return;
        }
        SUPPRESSED_OWNER_TELEPORTS.put(ownerUuid, serverTick + Math.max(1, ticks));
    }

    private static boolean isOwnerTeleportSuppressed(UUID ownerUuid) {
        if (ownerUuid == null) {
            return false;
        }
        Long until = SUPPRESSED_OWNER_TELEPORTS.get(ownerUuid);
        return until != null && until >= serverTick;
    }

    private static void executeQueuedLoadedFollow(ServerPlayer owner, ServerLevel targetLevel, PendingLoadedPetFollow follow) {
        if (owner == null || targetLevel == null || follow == null || follow.tame == null) {
            return;
        }
        TamableAnimal tame = follow.tame;
        if (!tame.isAlive() || tame.isRemoved()) {
            return;
        }
        TameData data = TameRegistry.get(follow.tameUuid);
        if (!isAutoFollowEligible(tame, data)) {
            return;
        }
        Vec3 targetPos = follow.targetPos == null ? owner.position() : follow.targetPos;
        if (TameCommands.autoFollowTeleportLoadedToLocation(tame, targetLevel, targetPos, follow.yRot, follow.xRot)) {
            STABILIZE_FOLLOW.put(follow.tameUuid, new StabilizeFollow(owner.getUUID(), serverTick + STABILIZE_RETRY_DELAY_TICKS, STABILIZE_MAX_RETRIES));
        }
    }

    private static void executeQueuedUnloadedFollow(ServerPlayer owner, ServerLevel targetLevel, Vec3 targetPos, float yRot, float xRot, Set<UUID> loadedCandidates) {
        if (owner == null || owner.server == null || targetLevel == null || targetPos == null) return;
        UUID ownerId = owner.getUUID();
        Set<UUID> captured = loadedCandidates == null ? Set.of() : loadedCandidates;

        for (TameData data : TameRegistry.getOwned(ownerId)) {
            if (data == null || data.uuid == null || data.isInactive()) continue;
            if (!ownerId.equals(data.ownerUUID)) continue;
            if (!isAutoFollowEligible(data)) continue;
            if (captured.contains(data.uuid)) continue;
            if (findLoadedOwnedTame(owner, data.uuid) != null) continue;
            TameCommands.autoFollowTeleportUnloadedToLocation(owner, data, targetLevel, targetPos, yRot, xRot);
        }
    }

    private static void pullUnloadedFollowingTamesToOwner(ServerPlayer owner) {
        if (owner == null || owner.server == null) return;
        UUID ownerId = owner.getUUID();
        for (TameData data : TameRegistry.getOwned(ownerId)) {
            if (data == null || data.uuid == null || data.isInactive()) continue;
            if (!ownerId.equals(data.ownerUUID)) continue;
            if (!isAutoFollowEligible(data)) continue;
            if (TameCommands.hasPendingImmediateChunkTeleport(data)) continue;
            if (findLoadedOwnedTame(owner, data.uuid) != null) continue;
            TameCommands.autoFollowTeleportUnloadedToOwner(owner, data);
        }
    }

    private static void scheduleFollow(ServerPlayer player, ServerLevel sourceLevel, ServerLevel targetLevel, Vec3 sourcePos, Vec3 targetPos, float yRot, float xRot, int delayTicks) {
        if (player == null || sourceLevel == null || targetLevel == null || sourcePos == null) {
            return;
        }
        Set<UUID> loadedCandidates = queueNearbyLoadedPets(player, sourceLevel, targetLevel, sourcePos, targetPos, yRot, xRot, delayTicks);
        PENDING_UNLOADED_FOLLOW.put(player.getUUID(), new PendingUnloadedFollow(
                player.getUUID(),
                targetLevel.dimension().location().toString(),
                targetPos,
                yRot,
                xRot,
                serverTick + Math.max(1, delayTicks),
                loadedCandidates
        ));
    }

    private static Set<UUID> queueNearbyLoadedPets(ServerPlayer player, ServerLevel sourceLevel, ServerLevel targetLevel, Vec3 sourcePos, Vec3 targetPos, float yRot, float xRot, int delayTicks) {
        Set<UUID> queued = new HashSet<>();
        List<TamableAnimal> nearby = new ArrayList<>();
        for (TameData data : TameRegistry.getOwned(player.getUUID())) {
            if (data == null || data.uuid == null) continue;
            Entity entity = sourceLevel.getEntity(data.uuid);
            if (entity instanceof TamableAnimal tame) {
                nearby.add(tame);
            }
        }
        for (TamableAnimal tame : nearby) {
            if (tame == null || !tame.isAlive() || !player.getUUID().equals(tame.getOwnerUUID())) continue;
            TameData data = TameRegistry.get(tame.getUUID());
            if (!isAutoFollowEligible(tame, data)) continue;
            queued.add(tame.getUUID());
            PENDING_LOADED_PETS.put(tame.getUUID(), new PendingLoadedPetFollow(
                    tame.getUUID(),
                    player.getUUID(),
                    targetLevel.dimension().location().toString(),
                    targetPos,
                    yRot,
                    xRot,
                    serverTick + Math.max(1, delayTicks),
                    tame
            ));
        }
        return queued;
    }

    private static ServerLevel resolvePendingTargetLevel(TickEvent.ServerTickEvent event, ServerPlayer player, PendingLoadedPetFollow follow) {
        if (event == null || event.getServer() == null || follow == null) {
            return player == null ? null : player.serverLevel();
        }
        if (follow.targetDimension == null || follow.targetDimension.isBlank()) {
            return player == null ? null : player.serverLevel();
        }
        ResourceLocation id = ResourceLocation.tryParse(follow.targetDimension);
        if (id == null) {
            return player == null ? null : player.serverLevel();
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel level = event.getServer().getLevel(key);
        return level != null ? level : player.serverLevel();
    }

    private static ServerLevel resolvePendingTargetLevel(TickEvent.ServerTickEvent event, ServerPlayer player, PendingUnloadedFollow follow) {
        if (event == null || event.getServer() == null || follow == null) {
            return player == null ? null : player.serverLevel();
        }
        if (follow.targetDimension == null || follow.targetDimension.isBlank()) {
            return player == null ? null : player.serverLevel();
        }
        ResourceLocation id = ResourceLocation.tryParse(follow.targetDimension);
        if (id == null) {
            return player == null ? null : player.serverLevel();
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel level = event.getServer().getLevel(key);
        return level != null ? level : player.serverLevel();
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

    public static boolean isFollowing(TameData data) {
        if (data == null) {
            return false;
        }
        if (data == null || data.isInactive()) {
            return false;
        }
        if (data.movementOrder != 0) {
            return false;
        }
        CompoundTag snapshot = data.entitySnapshot;
        if (snapshot != null) {
            if (snapshot.contains("Sitting") && snapshot.getBoolean("Sitting")) {
                return false;
            }
            if (snapshot.contains("orderedToSit") && snapshot.getBoolean("orderedToSit")) {
                return false;
            }
            int command = extractCommand(snapshot);
            if (command != Integer.MIN_VALUE) {
                return command == 2;
            }
        }
        return true;
    }

    private static boolean isAutoFollowEligible(TameData data) {
        return data != null && !data.hasHome && !data.wanderLock && isFollowing(data) && hasTeleportCapability(data);
    }

    private static boolean isAutoFollowEligible(TamableAnimal tame, TameData data) {
        return tame != null
                && tame.isAlive()
                && data != null
                && !data.hasHome
                && !data.wanderLock
                && isFollowingForTeleportCompat(tame)
                && tame.level() instanceof ServerLevel
                && hasTeleportCapability(data);
    }

    private static boolean hasTeleportCapability(TameData data) {
        if (data == null) {
            return false;
        }
        if (LevelSystem.getAttributeLevel(data, "tethered_teleport") > 0) {
            return true;
        }
        return data.entitySnapshot != null && data.entitySnapshot.toString().contains("tethered_teleport");
    }

    public static boolean isFollowingForTeleportCompat(TamableAnimal tame) {
        if (tame instanceof IComandableMob commandable) {
            return commandable.getCommand() == 2;
        }
        return !tame.isOrderedToSit();
    }

    private static int extractCommand(CompoundTag snapshot) {
        if (snapshot == null) {
            return Integer.MIN_VALUE;
        }
        for (String key : snapshot.getAllKeys()) {
            if (!key.endsWith("Command")) {
                continue;
            }
            try {
                return snapshot.getInt(key);
            } catch (Throwable ignored) {
            }
        }
        String[] keys = {"Command", "command", "PetCommand", "petCommand", "Order", "order", "Mode", "mode"};
        for (String key : keys) {
            if (snapshot.contains(key)) {
                try {
                    return snapshot.getInt(key);
                } catch (Throwable ignored) {
                }
            }
        }
        return Integer.MIN_VALUE;
    }

    private static String recoverEntityTypeId(TameData data) {
        if (data != null && data.entitySnapshot != null && data.entitySnapshot.contains("id")) {
            String fromSnapshot = data.entitySnapshot.getString("id");
            if (fromSnapshot != null && !fromSnapshot.isBlank()) {
                return fromSnapshot.trim().toLowerCase(Locale.ROOT);
            }
        }
        String raw = data == null ? "" : data.type;
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String normalized = raw.trim();
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
            int firstDot = normalized.indexOf('.');
            if (firstDot > 0 && !normalized.contains(":")) {
                normalized = normalized.substring(0, firstDot) + ":" + normalized.substring(firstDot + 1);
            }
        }
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        return normalized.toLowerCase(Locale.ROOT);
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
