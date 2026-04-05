package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;

public class TameArenaSavedData extends SavedData {

    public static final String DATA_NAME = "tameslevel_arenas";

    private final Map<String, TameArenaRegistry.TameArena> arenas = new LinkedHashMap<>();

    public Map<String, TameArenaRegistry.TameArena> getArenas() {
        return arenas;
    }

    public void setArenas(Map<String, TameArenaRegistry.TameArena> arenas) {
        this.arenas.clear();
        if (arenas == null) {
            return;
        }
        for (Map.Entry<String, TameArenaRegistry.TameArena> entry : arenas.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            this.arenas.put(entry.getKey(), entry.getValue().copy());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag arenaList = new ListTag();
        for (TameArenaRegistry.TameArena arena : arenas.values()) {
            if (arena == null || arena.name() == null || arena.name().isBlank()) {
                continue;
            }
            arenaList.add(arena.toTag());
        }
        tag.put("arenas", arenaList);
        return tag;
    }

    public static TameArenaSavedData load(CompoundTag tag) {
        TameArenaSavedData data = new TameArenaSavedData();
        if (!tag.contains("arenas", Tag.TAG_LIST)) {
            return data;
        }
        ListTag arenaList = tag.getList("arenas", Tag.TAG_COMPOUND);
        for (Tag entry : arenaList) {
            if (!(entry instanceof CompoundTag row)) {
                continue;
            }
            TameArenaRegistry.TameArena arena = TameArenaRegistry.TameArena.fromTag(row);
            if (arena == null || arena.name() == null || arena.name().isBlank()) {
                continue;
            }
            data.arenas.put(TameArenaRegistry.normalizeName(arena.name()), arena);
        }
        return data;
    }
}
