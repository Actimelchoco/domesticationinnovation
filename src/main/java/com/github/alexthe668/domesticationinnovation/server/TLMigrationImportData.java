package com.github.alexthe668.domesticationinnovation.server;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TLMigrationImportData extends SavedData {

    public static final String DATA_NAME = "domesticationinnovation_tameslevel_migration";
    private final Map<UUID, CompoundTag> entries = new HashMap<>();

    public static TLMigrationImportData get(Level world) {
        if (world instanceof ServerLevel serverLevel) {
            ServerLevel overworld = serverLevel.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return null;
            }
            DimensionDataStorage storage = overworld.getDataStorage();
            return storage.computeIfAbsent(
                    TLMigrationImportData::load,
                    TLMigrationImportData::new,
                    DATA_NAME
            );
        }
        return null;
    }

    public static TLMigrationImportData load(CompoundTag tag) {
        TLMigrationImportData data = new TLMigrationImportData();
        if (!tag.contains("entries", Tag.TAG_LIST)) {
            return data;
        }
        ListTag list = tag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!row.hasUUID("uuid")) {
                continue;
            }
            UUID uuid = row.getUUID("uuid");
            CompoundTag payload = row.contains("payload", Tag.TAG_COMPOUND)
                    ? row.getCompound("payload").copy()
                    : new CompoundTag();
            data.entries.put(uuid, payload);
        }
        return data;
    }

    public CompoundTag getPayload(UUID uuid) {
        CompoundTag payload = entries.get(uuid);
        return payload == null ? null : payload.copy();
    }

    public boolean hasEntry(UUID uuid) {
        return entries.containsKey(uuid);
    }

    public boolean hasSamePayload(UUID uuid, CompoundTag payload) {
        CompoundTag existing = entries.get(uuid);
        return existing != null && existing.equals(payload);
    }

    public void upsert(UUID uuid, CompoundTag payload) {
        if (uuid == null) {
            return;
        }
        entries.put(uuid, payload == null ? new CompoundTag() : payload.copy());
        setDirty();
    }

    public int size() {
        return entries.size();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : entries.entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("uuid", entry.getKey());
            row.put("payload", entry.getValue().copy());
            list.add(row);
        }
        tag.put("entries", list);
        return tag;
    }
}
