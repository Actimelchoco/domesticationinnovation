package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One equipped-belt multiplier per offline owner; no player or inventory copies. */
public final class HunterBeltOfflineData extends SavedData {
    private final Map<UUID, Double> multipliers = new HashMap<>();

    public static HunterBeltOfflineData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(HunterBeltOfflineData::load,
                HunterBeltOfflineData::new, "tames_offline_hunter_belts");
    }

    public double get(UUID owner) { return multipliers.getOrDefault(owner, 1D); }

    public void put(UUID owner, double multiplier) {
        if (!Double.isFinite(multiplier) || multiplier < 0) multiplier = 1;
        if (Double.compare(get(owner), multiplier) == 0) return;
        if (multiplier == 1) multipliers.remove(owner);
        else multipliers.put(owner, multiplier);
        setDirty();
    }

    public static HunterBeltOfflineData load(CompoundTag tag) {
        var data = new HunterBeltOfflineData();
        // Old snapshots may contain more than two belts; their components cannot
        // be recovered from an aggregate. Recapture them on the next logout.
        if (tag.getInt("BeltLimit") != 2) { data.setDirty(); return data; }
        CompoundTag values = tag.getCompound("Owners");
        for (String key : values.getAllKeys()) {
            try {
                if (!values.contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) continue;
                double value = values.getDouble(key);
                if (Double.isFinite(value) && value >= 0 && value != 1)
                    data.multipliers.put(UUID.fromString(key), value);
            } catch (IllegalArgumentException ignored) { }
        }
        return data;
    }

    @Override public CompoundTag save(CompoundTag tag) {
        tag.putInt("BeltLimit", 2);
        var owners = new CompoundTag();
        multipliers.forEach((owner, value) -> owners.putDouble(owner.toString(), value));
        tag.put("Owners", owners);
        return tag;
    }
}
