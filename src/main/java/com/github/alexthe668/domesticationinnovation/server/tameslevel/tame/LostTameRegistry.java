package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Durable recovery snapshots, kept independently of wild-body lifetime and the active tame registry. */
public final class LostTameRegistry extends SavedData {
    private final Map<UUID, CompoundTag> entries = new LinkedHashMap<>();
    public static LostTameRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(LostTameRegistry::load, LostTameRegistry::new, "tameslevel_lost_registry");
    }
    public UUID archive(TameData original, LivingEntity live) {
        TameData saved = TameData.fromTag(original.toTag());
        if (saved.bewereagerState.getBoolean("Active") && saved.bewereagerState.contains("Wolf", 10)) {
            saved.entitySnapshot = saved.bewereagerState.getCompound("Wolf").copy();
        } else if (live != null && !live.isRemoved()) {
            CompoundTag snapshot = new CompoundTag();
            if (live.save(snapshot)) saved.entitySnapshot = snapshot;
            saved.lastKnownDimension = live.level().dimension().location().toString();
            saved.lastKnownX = live.blockPosition().getX();saved.lastKnownY = live.blockPosition().getY();saved.lastKnownZ = live.blockPosition().getZ();
        }
        UUID id = UUID.randomUUID();
        CompoundTag entry = new CompoundTag();
        entry.put("tame", saved.toTag());
        entry.putString("reason", "tame_type_limit");
        entry.putLong("lostAt", System.currentTimeMillis());
        if (live != null && live.getServer() != null) entry.putLong("lostGameTime", live.getServer().overworld().getGameTime());
        entries.put(id, entry);
        setDirty();
        return id;
    }
    public CompoundTag entry(UUID id) {
        CompoundTag entry = entries.get(id);
        return entry == null ? null : entry.copy();
    }
    public void stampGameTime(UUID id, long time) {
        CompoundTag entry = entries.get(id);
        if (entry != null && !entry.contains("lostGameTime")) { entry.putLong("lostGameTime", time);setDirty(); }
    }
    public List<UUID> ids(boolean unrecoveredOnly) {
        return entries.entrySet().stream().filter(entry -> !unrecoveredOnly || !entry.getValue().contains("recoveredAt"))
                .sorted(Comparator.comparingLong((Map.Entry<UUID, CompoundTag> entry) -> entry.getValue().getLong("lostAt")).reversed())
                .map(Map.Entry::getKey).toList();
    }
    public void recovered(UUID id, UUID body) {
        CompoundTag entry = entries.get(id);
        if (entry == null) return;
        entry.putLong("recoveredAt", System.currentTimeMillis());
        entry.putUUID("recoveredBody", body);
        setDirty();
    }
    public static LostTameRegistry load(CompoundTag tag) {
        LostTameRegistry registry = new LostTameRegistry();
        for (String key : tag.getAllKeys()) try { registry.entries.put(UUID.fromString(key), tag.getCompound(key).copy()); }
        catch (IllegalArgumentException ignored) {}
        return registry;
    }
    @Override public CompoundTag save(CompoundTag tag) {
        entries.forEach((id, entry) -> tag.put(id.toString(), entry.copy()));
        return tag;
    }
}
