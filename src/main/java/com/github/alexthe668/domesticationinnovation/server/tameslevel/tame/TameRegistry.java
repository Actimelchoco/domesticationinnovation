package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public class TameRegistry {

    public static final Map<UUID, TameData> TAMES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNER_TO_TAMES = new HashMap<>();
    public static final Map<UUID, TameDeathRecord> LAST_DEATHS = new HashMap<>();
    public static final List<TameDeathRecord> DEATH_HISTORY = new ArrayList<>();
    public static final Set<String> APPROVED_REINCARNATE_ITEMS = new HashSet<>();
    private static TameRegistrySavedData savedData;

    public static void init(MinecraftServer server) {
        if (savedData != null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        savedData = overworld.getDataStorage().computeIfAbsent(
                TameRegistrySavedData::load,
                TameRegistrySavedData::new,
                TameRegistrySavedData.DATA_NAME
        );

        TAMES.clear();
        TAMES.putAll(savedData.getTames());
        rebuildIndexes();
        LAST_DEATHS.clear();
        LAST_DEATHS.putAll(savedData.getLastDeaths());
        DEATH_HISTORY.clear();
        DEATH_HISTORY.addAll(savedData.getDeathHistory());
        APPROVED_REINCARNATE_ITEMS.clear();
        APPROVED_REINCARNATE_ITEMS.addAll(savedData.getApprovedReincarnateItems());
        if (DEATH_HISTORY.isEmpty() && !LAST_DEATHS.isEmpty()) {
            DEATH_HISTORY.addAll(LAST_DEATHS.values());
            markDirty();
        }
    }

    public static void register(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        TameData previous = TAMES.put(data.uuid, data);
        if (previous != null) {
            removeFromIndexes(previous);
        }
        addToIndexes(data);
        markDirty();
    }

    public static TameData get(UUID id) {
        return TAMES.get(id);
    }

    public static void remove(UUID id) {
        TameData removed = TAMES.remove(id);
        if (removed != null) {
            removeFromIndexes(removed);
            markDirty();
        }
    }

    public static boolean transferOwnership(UUID tameUuid, UUID newOwner) {
        if (tameUuid == null || newOwner == null) {
            return false;
        }
        TameData data = TAMES.get(tameUuid);
        if (data == null) {
            return false;
        }
        UUID oldOwner = data.ownerUUID;
        if (Objects.equals(oldOwner, newOwner)) {
            return false;
        }
        if (oldOwner != null) {
            Set<UUID> oldSet = OWNER_TO_TAMES.get(oldOwner);
            if (oldSet != null) {
                oldSet.remove(tameUuid);
                if (oldSet.isEmpty()) {
                    OWNER_TO_TAMES.remove(oldOwner);
                }
            }
        }
        data.ownerUUID = newOwner;
        OWNER_TO_TAMES.computeIfAbsent(newOwner, k -> new HashSet<>()).add(tameUuid);
        migrateDeathOwnership(tameUuid, oldOwner, newOwner);
        markDirty();
        return true;
    }

    public static List<TameData> getOwned(UUID ownerUuid) {
        if (ownerUuid == null) {
            return List.of();
        }
        Set<UUID> ids = OWNER_TO_TAMES.get(ownerUuid);
        if (ids == null || ids.isEmpty()) {
            List<TameData> fallback = new ArrayList<>();
            for (TameData data : TAMES.values()) {
                if (data == null || data.ownerUUID == null) continue;
                if (!ownerUuid.equals(data.ownerUUID)) continue;
                fallback.add(data);
            }
            return fallback;
        }
        List<TameData> out = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            TameData data = TAMES.get(id);
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    public static void archiveDeath(TameDeathRecord record) {
        if (record == null || record.uuid == null) {
            return;
        }
        LAST_DEATHS.put(record.uuid, record);
        DEATH_HISTORY.add(record);
        markDirty();
    }

    public static void markDirty() {
        if (savedData == null) {
            return;
        }
        savedData.getTames().clear();
        savedData.getTames().putAll(TAMES);
        savedData.getLastDeaths().clear();
        savedData.getLastDeaths().putAll(LAST_DEATHS);
        savedData.getDeathHistory().clear();
        savedData.getDeathHistory().addAll(DEATH_HISTORY);
        savedData.getApprovedReincarnateItems().clear();
        savedData.getApprovedReincarnateItems().addAll(APPROVED_REINCARNATE_ITEMS);
        savedData.setDirty();
    }

    public static boolean isInitialized() {
        return savedData != null;
    }

    private static void rebuildIndexes() {
        OWNER_TO_TAMES.clear();
        for (TameData data : TAMES.values()) {
            addToIndexes(data);
        }
    }

    private static void addToIndexes(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        if (data.ownerUUID != null) {
            OWNER_TO_TAMES.computeIfAbsent(data.ownerUUID, k -> new HashSet<>()).add(data.uuid);
        }
    }

    private static void removeFromIndexes(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        if (data.ownerUUID == null) {
            return;
        }
        Set<UUID> set = OWNER_TO_TAMES.get(data.ownerUUID);
        if (set == null) {
            return;
        }
        set.remove(data.uuid);
        if (set.isEmpty()) {
            OWNER_TO_TAMES.remove(data.ownerUUID);
        }
    }

    private static void migrateDeathOwnership(UUID tameUuid, UUID oldOwner, UUID newOwner) {
        if (tameUuid == null || newOwner == null) {
            return;
        }
        for (TameDeathRecord record : DEATH_HISTORY) {
            if (record == null || record.uuid == null) continue;
            if (!tameUuid.equals(record.uuid)) continue;
            if (oldOwner != null && !Objects.equals(oldOwner, record.ownerUUID)) continue;
            record.ownerUUID = newOwner;
            if (record.snapshot != null && !record.snapshot.isEmpty()) {
                record.snapshot.putUUID("ownerUUID", newOwner);
            }
        }
        TameDeathRecord last = LAST_DEATHS.get(tameUuid);
        if (last == null) {
            return;
        }
        if (oldOwner == null || Objects.equals(oldOwner, last.ownerUUID)) {
            last.ownerUUID = newOwner;
            if (last.snapshot != null && !last.snapshot.isEmpty()) {
                last.snapshot.putUUID("ownerUUID", newOwner);
            }
        }
    }
}
