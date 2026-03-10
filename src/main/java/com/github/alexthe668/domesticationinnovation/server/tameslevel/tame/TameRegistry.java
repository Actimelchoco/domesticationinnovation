package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;

public class TameRegistry {

    public static final Map<UUID, TameData> TAMES = new HashMap<>();
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
        TAMES.put(data.uuid, data);
        markDirty();
    }

    public static TameData get(UUID id) {
        return TAMES.get(id);
    }

    public static void remove(UUID id) {
        if (TAMES.remove(id) != null) {
            markDirty();
        }
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
}
