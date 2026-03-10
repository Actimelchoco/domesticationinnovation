package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.Objects;

public class TamePersistenceEvents {
    private static final java.util.regex.Pattern LEVEL_PREFIX =
            java.util.regex.Pattern.compile("^\\[lvl\\s*\\d+\\]\\s*", java.util.regex.Pattern.CASE_INSENSITIVE);
    private static final long LOCATION_SAVE_INTERVAL_TICKS = 200L; // 10 seconds

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
        if (level.getGameTime() % 40 != 0) return;
        boolean saveLocationTick = (level.getGameTime() % LOCATION_SAVE_INTERVAL_TICKS) == 0L;
        if (level.getServer() != null && level == level.getServer().overworld()) {
            TameDuelManager.tick(level.getServer());
        }

        boolean changed = false;
        for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
            if (!(entity instanceof TamableAnimal tame)) continue;
            if (!tame.isTame()) continue;
            if (!tame.isAlive()) continue;

            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) {
                data = TameSpawnEvents.registerOrRestoreTame(tame, false);
                if (data == null) {
                    continue;
                }
                changed = true;
            }
            if (data.dead) {
                data.dead = false;
                data.deadGameTime = 0L;
                data.deadUnixMillis = 0L;
                data.deathDimension = "";
                data.deathX = 0;
                data.deathY = 0;
                data.deathZ = 0;
                changed = true;
            }
            if (!Objects.equals(data.ownerUUID, tame.getOwnerUUID())) {
                java.util.UUID previousOwner = data.ownerUUID;
                java.util.UUID newOwner = tame.getOwnerUUID();
                data.ownerUUID = newOwner;
                if (newOwner != null && data.uuid != null) {
                    migrateDeathOwnership(data.uuid, previousOwner, newOwner);
                }
                changed = true;
            }
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

            net.minecraft.nbt.CompoundTag snapshot = new net.minecraft.nbt.CompoundTag();
            tame.save(snapshot);
            if (saveLocationTick) {
                String dim = level.dimension().location().toString();
                int x = tame.blockPosition().getX();
                int y = tame.blockPosition().getY();
                int z = tame.blockPosition().getZ();
                if (!dim.equals(data.lastKnownDimension)
                        || x != data.lastKnownX
                        || y != data.lastKnownY
                        || z != data.lastKnownZ
                        || data.lastKnownGameTime != level.getGameTime()
                        || !snapshot.equals(data.entitySnapshot)) {
                    data.lastKnownDimension = dim;
                    data.lastKnownX = x;
                    data.lastKnownY = y;
                    data.lastKnownZ = z;
                    data.lastKnownGameTime = level.getGameTime();
                    data.entitySnapshot = snapshot;
                    changed = true;
                }
            } else if (!snapshot.equals(data.entitySnapshot)) {
                data.entitySnapshot = snapshot;
                changed = true;
            }
        }

        if (changed) {
            TameRegistry.markDirty();
        }
    }

    private static String stripLevelPrefix(String name) {
        if (name == null) {
            return "";
        }
        return LEVEL_PREFIX.matcher(name).replaceFirst("");
    }

    private static void migrateDeathOwnership(java.util.UUID tameUuid, java.util.UUID oldOwner, java.util.UUID newOwner) {
        if (tameUuid == null || newOwner == null) {
            return;
        }

        for (var record : TameRegistry.DEATH_HISTORY) {
            if (record == null || record.uuid == null) continue;
            if (!tameUuid.equals(record.uuid)) continue;
            if (oldOwner != null && !Objects.equals(oldOwner, record.ownerUUID)) continue;
            record.ownerUUID = newOwner;
            if (record.snapshot != null && !record.snapshot.isEmpty()) {
                record.snapshot.putUUID("ownerUUID", newOwner);
            }
        }

        var last = TameRegistry.LAST_DEATHS.get(tameUuid);
        if (last != null) {
            if (oldOwner == null || Objects.equals(oldOwner, last.ownerUUID)) {
                last.ownerUUID = newOwner;
                if (last.snapshot != null && !last.snapshot.isEmpty()) {
                    last.snapshot.putUUID("ownerUUID", newOwner);
                }
            }
        }
    }
}
