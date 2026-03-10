package com.github.alexthe668.domesticationinnovation.server.misc;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistrySavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DITameProgressData extends SavedData {

    public static final String DATA_NAME = TameRegistrySavedData.DATA_NAME;

    public static DITameProgressData get(Level world) {
        if (world instanceof ServerLevel serverLevel) {
            if (!TameRegistry.isInitialized()) {
                TameRegistry.init(serverLevel.getServer());
            }
            return new DITameProgressData();
        }
        return null;
    }

    public static DITameProgressData load(CompoundTag tag) {
        return new DITameProgressData();
    }

    public CompoundTag getPayload(UUID uuid) {
        TameData data = uuid == null ? null : TameRegistry.get(uuid);
        return data == null ? null : data.toTag();
    }

    public void upsert(UUID uuid, CompoundTag payload) {
        if (uuid == null || payload == null || payload.isEmpty()) {
            return;
        }
        CompoundTag copy = payload.copy();
        copy.putUUID("uuid", uuid);
        TameData data = TameData.fromTag(copy);
        data.uuid = uuid;
        TameRegistry.register(data);
    }

    public void remove(UUID uuid) {
        if (uuid == null) {
            return;
        }
        TameRegistry.remove(uuid);
    }

    public int size() {
        return TameRegistry.TAMES.size();
    }

    public Map<UUID, CompoundTag> getEntriesCopy() {
        Map<UUID, CompoundTag> copy = new HashMap<>();
        for (Map.Entry<UUID, TameData> entry : TameRegistry.TAMES.entrySet()) {
            copy.put(entry.getKey(), entry.getValue().toTag());
        }
        return copy;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        // Delegates persistence to TameRegistrySavedData via TameRegistry.markDirty().
        ListTag list = new ListTag();
        for (Map.Entry<UUID, CompoundTag> entry : getEntriesCopy().entrySet()) {
            CompoundTag row = new CompoundTag();
            row.putUUID("uuid", entry.getKey());
            row.put("payload", entry.getValue());
            list.add(row);
        }
        tag.put("entries", list);
        return tag;
    }
}
