package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TamePersistenceEvents {
    private static final java.util.regex.Pattern LEVEL_PREFIX =
            java.util.regex.Pattern.compile("^\\[lvl\\s*\\d+\\]\\s*", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final long LOCATION_SAVE_INTERVAL_TICKS = 200L; // 10 seconds
    private static final long FULL_SNAPSHOT_INTERVAL_TICKS = 1200L; // 60 seconds
    private static final long QUEUE_SCRUB_INTERVAL_TICKS = 100L; // 5 seconds
    private static final long BACKFILL_SCAN_INTERVAL_TICKS = 600L; // 30 seconds

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        TameRegistry.init(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        TameRegistry.markDirty();
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        boolean maintenanceTick = (level.getGameTime() % 40L) == 0L;
        boolean saveLocationTick = (level.getGameTime() % LOCATION_SAVE_INTERVAL_TICKS) == 0L;
        boolean saveSnapshotTick = (level.getGameTime() % FULL_SNAPSHOT_INTERVAL_TICKS) == 0L;
        boolean queueScrubTick = (level.getGameTime() % QUEUE_SCRUB_INTERVAL_TICKS) == 0L;
        boolean backfillScanTick = (level.getGameTime() % BACKFILL_SCAN_INTERVAL_TICKS) == 0L;
        if (maintenanceTick && level.getServer() != null && level == level.getServer().overworld()) {
            TameDuelManager.tick(level.getServer());
        }

        boolean changed = false;
        Set<UUID> loadedAliveTameIds = queueScrubTick ? new HashSet<>() : Set.of();
        Set<UUID> seenLoaded = maintenanceTick ? new HashSet<>() : Set.of();
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof TamableAnimal tame)) continue;
            if (!tame.isTame() || !tame.isAlive()) continue;
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) {
                if (!backfillScanTick) {
                    continue;
                }
                data = TameSpawnEvents.registerOrRestoreTame(tame, false);
                if (data == null) {
                    continue;
                }
                changed = true;
            }
            if (maintenanceTick) {
                seenLoaded.add(data.uuid);
            }
            if (queueScrubTick) {
                loadedAliveTameIds.add(data.uuid);
                if (data.tlId != null) {
                    loadedAliveTameIds.add(data.tlId);
                }
            }
            if (syncLoadedTame(level, tame, data, maintenanceTick, saveLocationTick, saveSnapshotTick)) {
                changed = true;
            }
        }

        if (maintenanceTick && backfillScanTick) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal tame)) continue;
                if (!tame.isTame() || !tame.isAlive()) continue;
                UUID tameId = tame.getUUID();
                if (seenLoaded.contains(tameId)) continue;

                TameData data = TameRegistry.get(tameId);
                if (data == null) {
                    data = TameSpawnEvents.registerOrRestoreTame(tame, false);
                    if (data == null) continue;
                    changed = true;
                }
                if (queueScrubTick) {
                    loadedAliveTameIds.add(tameId);
                    if (data.tlId != null) {
                        loadedAliveTameIds.add(data.tlId);
                    }
                }
                if (syncLoadedTame(level, tame, data, true, saveLocationTick, saveSnapshotTick)) {
                    changed = true;
                }
            }
        }

        if (queueScrubTick && !loadedAliveTameIds.isEmpty()) {
            DIWorldData worldData = DIWorldData.get(level);
            if (worldData != null) {
                worldData.removeLanternRequestsForPets(loadedAliveTameIds);
                worldData.removeRespawnRequestsForPets(loadedAliveTameIds);
            }
        }

        if (changed) {
            TameRegistry.markDirty();
        }
    }

    private static boolean syncLoadedTame(ServerLevel level, TamableAnimal tame, TameData data, boolean maintenanceTick, boolean saveLocationTick, boolean saveSnapshotTick) {
        boolean changed = false;
        if (data.dead) {
            tame.remove(Entity.RemovalReason.DISCARDED);
            return false;
        }
        boolean locationChanged = updateLiveLocation(level, tame, data);
        if (saveLocationTick && locationChanged) {
            data.lastKnownGameTime = level.getGameTime();
            changed = true;
        }
        if (maintenanceTick) {
            if (data.bornDayTime <= 0L) {
                data.bornDayTime = level.getDayTime();
                changed = true;
            }
            if (TameBedRegistrySync.syncFromEntity(tame, data)) {
                changed = true;
            }
            if (tame.hasCustomName() && tame.getCustomName() != null) {
                String currentName = tame.getCustomName().getString();
                String normalized = stripLevelPrefix(currentName);
                if (!normalized.isBlank() && !normalized.equals(data.name)) {
                    data.name = normalized;
                    changed = true;
                }
            }
        }
        if (saveSnapshotTick || data.entitySnapshot == null || data.entitySnapshot.isEmpty()) {
            net.minecraft.nbt.CompoundTag snapshot = new net.minecraft.nbt.CompoundTag();
            TameRegistry.bindEntityToData(tame, data);
            tame.save(snapshot);
            if (!snapshot.equals(data.entitySnapshot)) {
                data.entitySnapshot = snapshot;
                changed = true;
            }
        }
        TameableUtils.syncAbilityAttributeProgressPreview(tame, data);
        return changed;
    }

    private static boolean updateLiveLocation(ServerLevel level, TamableAnimal tame, TameData data) {
        String dim = level.dimension().location().toString();
        int x = tame.blockPosition().getX();
        int y = tame.blockPosition().getY();
        int z = tame.blockPosition().getZ();
        if (dim.equals(data.lastKnownDimension)
                && x == data.lastKnownX
                && y == data.lastKnownY
                && z == data.lastKnownZ) {
            return false;
        }
        data.lastKnownDimension = dim;
        data.lastKnownX = x;
        data.lastKnownY = y;
        data.lastKnownZ = z;
        return true;
    }

    private static String stripLevelPrefix(String name) {
        if (name == null) {
            return "";
        }
        return LEVEL_PREFIX.matcher(name).replaceFirst("");
    }
}
