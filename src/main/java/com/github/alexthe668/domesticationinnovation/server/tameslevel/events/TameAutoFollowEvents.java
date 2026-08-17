package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TameAutoFollowEvents {
    private static final int DIMENSION_FOLLOW_DELAY_TICKS = 20;
    private static final int OWNER_PULL_INTERVAL_TICKS = 40;
    private static final Map<UUID, PendingDimensionFollow> PENDING_DIMENSION_FOLLOW = new HashMap<>();
    private static final Map<UUID, Long> SUPPRESSED_OWNER_TELEPORTS = new HashMap<>();
    private static long serverTick = 0L;

    private record PendingDimensionFollow(
            UUID ownerUuid,
            String targetDimension,
            Vec3 targetPos,
            float yRot,
            float xRot,
            long dueTick
    ) {
    }

    @SubscribeEvent
    public static void onPlayerTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (isOwnerTeleportSuppressed(player.getUUID())) return;
        if (!(player.level() instanceof ServerLevel fromLevel)) return;
        ServerLevel targetLevel = fromLevel.getServer().getLevel(event.getDimension());
        if (targetLevel == null) return;
        scheduleDimensionTpFollow(player, targetLevel, player.position(), player.getYRot(), player.getXRot(), DIMENSION_FOLLOW_DELAY_TICKS);
    }

    public static void scheduleDimensionTpFollow(ServerPlayer player, ServerLevel targetLevel, Vec3 targetPos, float yRot, float xRot, int delayTicks) {
        if (player == null || targetLevel == null || targetPos == null) {
            return;
        }
        PENDING_DIMENSION_FOLLOW.put(player.getUUID(), new PendingDimensionFollow(
                player.getUUID(),
                targetLevel.dimension().location().toString(),
                targetPos,
                yRot,
                xRot,
                serverTick + Math.max(1, delayTicks)
        ));
    }

    public static void suppressOwnerTeleportFollow(UUID ownerUuid, int ticks) {
        if (ownerUuid == null) {
            return;
        }
        SUPPRESSED_OWNER_TELEPORTS.put(ownerUuid, serverTick + Math.max(1, ticks));
    }

    public static boolean isFollowing(TameData data) {
        if (data == null || data.isInactive()) {
            return false;
        }
        if (data.movementOrder == 1 || data.movementOrder == 2 || data.movementOrder == 3) {
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
                return isFollowCommand(command, data.type);
            }
        }
        return true;
    }

    public static boolean isFollowingForTeleportCompat(TamableAnimal tame) {
        return TameCommands.isLiveFollowing(tame);
    }

    private static boolean isFollowCommand(int command, String typeId) {
        return command == (usesInvertedGenericCallOrder(typeId) ? 0 : 2);
    }

    private static boolean usesInvertedGenericCallOrder(String typeId) {
        String normalized = normalizeTypeId(typeId);
        if (normalized == null || normalized.isBlank()) {
            return true;
        }
        return TameRegistry.isCallOrderInvertedType(normalized);
    }

    private static String normalizeTypeId(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return null;
        }
        String normalized = typeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
        }
        return normalized;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        serverTick++;

        if (!PENDING_DIMENSION_FOLLOW.isEmpty()) {
            PENDING_DIMENSION_FOLLOW.entrySet().removeIf(entry -> processPendingDimensionFollow(event, entry.getValue()));
        }

        if (serverTick % OWNER_PULL_INTERVAL_TICKS == 0L) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                if (player != null && player.isAlive()) {
                    pullUnloadedFollowingTamesToOwner(player);
                }
            }
        }

        if (!SUPPRESSED_OWNER_TELEPORTS.isEmpty()) {
            SUPPRESSED_OWNER_TELEPORTS.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue() < serverTick);
        }
    }

    private static boolean processPendingDimensionFollow(TickEvent.ServerTickEvent event, PendingDimensionFollow pending) {
        if (pending == null || pending.dueTick() > serverTick) {
            return false;
        }
        ServerPlayer owner = event.getServer().getPlayerList().getPlayer(pending.ownerUuid());
        if (owner == null) {
            return true;
        }
        ServerLevel targetLevel = resolveTargetLevel(event, owner, pending.targetDimension());
        if (targetLevel == null) {
            return true;
        }
        performDimensionFollow(owner, targetLevel, pending.targetPos(), pending.yRot(), pending.xRot());
        return true;
    }

    private static void performDimensionFollow(ServerPlayer owner, ServerLevel targetLevel, Vec3 targetPos, float yRot, float xRot) {
        if (owner == null || targetLevel == null || targetPos == null) {
            return;
        }
        UUID ownerId = owner.getUUID();
        for (TameData data : TameRegistry.getOwned(ownerId)) {
            TamableAnimal loaded = findLoadedOwnedTame(owner, data.uuid);
            if (loaded != null && loaded.isAlive()) {
                TameCommands.syncLiveMovementStateFor(loaded);
                if (!isAutoFollowEligibleLoaded(loaded, data, ownerId)) continue;
                teleportLoadedTame(owner, loaded, data, targetLevel, targetPos, yRot, xRot);
                continue;
            }
            if (!isAutoFollowEligible(data, ownerId)) continue;
            if (!TameCommands.hasPendingImmediateChunkTeleport(data)) {
                TameCommands.autoFollowTeleportViaTpPath(owner, data, targetLevel, targetPos, yRot, xRot);
            }
        }
    }

    private static void pullUnloadedFollowingTamesToOwner(ServerPlayer owner) {
        if (owner == null || owner.server == null) return;
        UUID ownerId = owner.getUUID();
        for (TameData data : TameRegistry.getOwned(ownerId)) {
            if (!isAutoFollowEligible(data, ownerId)) continue;
            if (findLoadedOwnedTame(owner, data.uuid) != null) continue;
            if (TameCommands.hasPendingImmediateChunkTeleport(data)) continue;
            TameCommands.autoFollowTeleportViaTpPath(owner, data, owner.serverLevel(), owner.position(), owner.getYRot(), owner.getXRot());
        }
    }

    private static boolean isAutoFollowEligible(TameData data, UUID ownerId) {
        return data != null
                && data.uuid != null
                && ownerId != null
                && ownerId.equals(data.ownerUUID)
                && !data.hasHome
                && !data.wanderLock
                && !TameDuelManager.isTameInDuel(data.uuid)
                && !TameCommands.isDuelSessionLocked(data.uuid)
                && isFollowing(data)
                && hasTeleportCapability(data);
    }

    private static boolean isAutoFollowEligibleLoaded(TamableAnimal tame, TameData data, UUID ownerId) {
        return data != null
                && tame != null
                && data.uuid != null
                && ownerId != null
                && ownerId.equals(data.ownerUUID)
                && !data.hasHome
                && !data.wanderLock
                && !TameDuelManager.isTameInDuel(data.uuid)
                && !TameCommands.isDuelSessionLocked(data.uuid)
                && TameCommands.isLiveFollowing(tame)
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

    private static TamableAnimal findLoadedOwnedTame(ServerPlayer owner, UUID tameUuid) {
        if (owner == null || owner.server == null || tameUuid == null) {
            return null;
        }
        for (ServerLevel level : owner.server.getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) continue;
            if (!owner.getUUID().equals(tame.getOwnerUUID())) continue;
            return tame;
        }
        return null;
    }

    private static void teleportLoadedTame(ServerPlayer owner, TamableAnimal tame, TameData data, ServerLevel targetLevel, Vec3 targetPos, float yRot, float xRot) {
        if (owner == null || tame == null || data == null || targetLevel == null || targetPos == null) {
            return;
        }
        if (tame.level().dimension().equals(targetLevel.dimension())) {
            tame.teleportTo(targetPos.x, targetPos.y, targetPos.z);
            tame.setYRot(yRot);
            tame.setXRot(xRot);
            tame.fallDistance = 0.0F;
            tame.setPortalCooldown();
            TameGoalInstaller.installIfMissing(tame);
            refreshData(data, tame, targetLevel);
            TameCommands.queueClientReloadForTame(tame);
            return;
        }

        CompoundTag snapshot = new CompoundTag();
        tame.save(snapshot);
        UUID ownerId = data.ownerUUID != null ? data.ownerUUID : tame.getOwnerUUID();
        DyeColor collar = tame instanceof Wolf wolf ? wolf.getCollarColor() : null;

        Entity created = tame.getType().create(targetLevel);
        if (!(created instanceof TamableAnimal moved)) {
            return;
        }
        moved.load(snapshot.copy());
        moved.setUUID(tame.getUUID());
        moved.moveTo(targetPos.x, targetPos.y, targetPos.z, yRot, xRot);
        moved.setYHeadRot(yRot);
        moved.fallDistance = 0.0F;
        moved.setPortalCooldown();
        moved.setDeltaMovement(0.0D, 0.0D, 0.0D);
        enforceTamedOwnerPreserveCollar(moved, ownerId, collar);
        if (moved.isNoAi()) {
            moved.setNoAi(false);
        }
        TameGoalInstaller.installIfMissing(moved);
        if (!targetLevel.addFreshEntity(moved)) {
            return;
        }
        tame.discard();
        TameCommands.refreshLoadedTameStatsAfterRebuild(moved, data, false);
        refreshData(data, moved, targetLevel);
        TameCommands.queueClientReloadForTame(moved);
    }

    private static void refreshData(TameData data, TamableAnimal tame, ServerLevel level) {
        if (data == null || tame == null || level == null) {
            return;
        }
        TameRegistry.bindEntityToData(tame, data);
        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = level.getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        tame.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
        TameRegistry.markDirty();
    }

    private static void enforceTamedOwnerPreserveCollar(TamableAnimal tame, UUID ownerId, DyeColor collar) {
        if (tame == null) return;
        if (ownerId != null) {
            tame.setTame(true);
            if (!ownerId.equals(tame.getOwnerUUID())) {
                tame.setOwnerUUID(ownerId);
            }
        } else if (!tame.isTame()) {
            tame.setTame(true);
        }
        if (collar != null && tame instanceof Wolf wolf) {
            wolf.setCollarColor(collar);
        }
    }

    private static boolean isOwnerTeleportSuppressed(UUID ownerUuid) {
        if (ownerUuid == null) {
            return false;
        }
        Long until = SUPPRESSED_OWNER_TELEPORTS.get(ownerUuid);
        return until != null && until >= serverTick;
    }

    private static ServerLevel resolveTargetLevel(TickEvent.ServerTickEvent event, ServerPlayer player, String targetDimension) {
        if (event == null || event.getServer() == null) {
            return player == null ? null : player.serverLevel();
        }
        if (targetDimension == null || targetDimension.isBlank()) {
            return player == null ? null : player.serverLevel();
        }
        ResourceLocation id = ResourceLocation.tryParse(targetDimension);
        if (id == null) {
            return player == null ? null : player.serverLevel();
        }
        ResourceKey<Level> key = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id);
        ServerLevel level = event.getServer().getLevel(key);
        return level != null ? level : (player == null ? null : player.serverLevel());
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
}
