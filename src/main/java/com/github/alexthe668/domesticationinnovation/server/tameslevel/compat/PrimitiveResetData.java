package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** An epoch also invalidates ownership in unloaded chunks and old entity snapshots. */
public final class PrimitiveResetData extends SavedData {
    private long generation;

    public static PrimitiveResetData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(PrimitiveResetData::load,
                PrimitiveResetData::new, "tames_primitive_reset");
    }

    public static PrimitiveResetData load(CompoundTag tag) {
        PrimitiveResetData data = new PrimitiveResetData();
        data.generation = tag.getLong("Generation");
        return data;
    }

    public long generation() { return generation; }

    public void advance() { generation++; setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("Generation", generation);
        return tag;
    }
}
