package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TameArenaRegistry {

    private static final Map<String, TameArena> ARENAS = new LinkedHashMap<>();
    private static TameArenaSavedData savedData;

    private TameArenaRegistry() {
    }

    public static void init(MinecraftServer server) {
        if (server == null || savedData != null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        if (overworld == null) {
            return;
        }
        savedData = overworld.getDataStorage().computeIfAbsent(
                TameArenaSavedData::load,
                TameArenaSavedData::new,
                TameArenaSavedData.DATA_NAME
        );
        ARENAS.clear();
        ARENAS.putAll(savedData.getArenas());
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> getArenaNames() {
        List<String> names = new ArrayList<>(ARENAS.keySet());
        names.sort(String::compareToIgnoreCase);
        return names;
    }

    public static TameArena getArena(String name) {
        TameArena arena = ARENAS.get(normalizeName(name));
        return arena == null ? null : arena.copy();
    }

    public static boolean createArena(String name) {
        String normalized = normalizeName(name);
        if (normalized.isBlank() || ARENAS.containsKey(normalized)) {
            return false;
        }
        ARENAS.put(normalized, new TameArena(normalized, null, null, null, null));
        markDirty();
        return true;
    }

    public static boolean deleteArena(String name) {
        String normalized = normalizeName(name);
        if (normalized.isBlank() || ARENAS.remove(normalized) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    public static TameArena setSpawnA(String name, ArenaPoint point) {
        return update(name, arena -> arena.withSpawnA(point));
    }

    public static TameArena setSpawnB(String name, ArenaPoint point) {
        return update(name, arena -> arena.withSpawnB(point));
    }

    public static TameArena setWaitingA(String name, ArenaPoint point) {
        return update(name, arena -> arena.withWaitingA(point));
    }

    public static TameArena setWaitingB(String name, ArenaPoint point) {
        return update(name, arena -> arena.withWaitingB(point));
    }

    private static TameArena update(String name, java.util.function.UnaryOperator<TameArena> updater) {
        String normalized = normalizeName(name);
        TameArena arena = ARENAS.get(normalized);
        if (arena == null || updater == null) {
            return null;
        }
        TameArena updated = updater.apply(arena.copy());
        ARENAS.put(normalized, updated);
        markDirty();
        return updated.copy();
    }

    public static void markDirty() {
        if (savedData == null) {
            return;
        }
        savedData.setArenas(ARENAS);
        savedData.setDirty();
    }

    public record ArenaPoint(String dimensionId, double x, double y, double z, float yRot, float xRot) {
        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("dimension", dimensionId == null ? "" : dimensionId);
            tag.putDouble("x", x);
            tag.putDouble("y", y);
            tag.putDouble("z", z);
            tag.putFloat("yRot", yRot);
            tag.putFloat("xRot", xRot);
            return tag;
        }

        public static ArenaPoint fromTag(CompoundTag tag) {
            if (tag == null) {
                return null;
            }
            String dimensionId = tag.getString("dimension");
            if (dimensionId == null || dimensionId.isBlank()) {
                return null;
            }
            return new ArenaPoint(dimensionId, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yRot"), tag.getFloat("xRot"));
        }

        public boolean isValid() {
            return dimensionId != null && !dimensionId.isBlank() && ResourceLocation.tryParse(dimensionId) != null;
        }
    }

    public record TameArena(String name, ArenaPoint spawnA, ArenaPoint spawnB, ArenaPoint waitingA, ArenaPoint waitingB) {
        public TameArena copy() {
            return new TameArena(name, spawnA, spawnB, waitingA, waitingB);
        }

        public TameArena withSpawnA(ArenaPoint point) {
            return new TameArena(name, point, spawnB, waitingA, waitingB);
        }

        public TameArena withSpawnB(ArenaPoint point) {
            return new TameArena(name, spawnA, point, waitingA, waitingB);
        }

        public TameArena withWaitingA(ArenaPoint point) {
            return new TameArena(name, spawnA, spawnB, point, waitingB);
        }

        public TameArena withWaitingB(ArenaPoint point) {
            return new TameArena(name, spawnA, spawnB, waitingA, point);
        }

        public boolean isConfiguredForDuel() {
            return spawnA != null && spawnA.isValid()
                    && spawnB != null && spawnB.isValid()
                    && waitingA != null && waitingA.isValid()
                    && waitingB != null && waitingB.isValid();
        }

        public CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putString("name", name == null ? "" : name);
            if (spawnA != null && spawnA.isValid()) {
                tag.put("spawnA", spawnA.toTag());
            }
            if (spawnB != null && spawnB.isValid()) {
                tag.put("spawnB", spawnB.toTag());
            }
            if (waitingA != null && waitingA.isValid()) {
                tag.put("waitingA", waitingA.toTag());
            }
            if (waitingB != null && waitingB.isValid()) {
                tag.put("waitingB", waitingB.toTag());
            }
            return tag;
        }

        public static TameArena fromTag(CompoundTag tag) {
            if (tag == null) {
                return null;
            }
            String name = normalizeName(tag.getString("name"));
            if (name.isBlank()) {
                return null;
            }
            ArenaPoint spawnA = tag.contains("spawnA", net.minecraft.nbt.Tag.TAG_COMPOUND) ? ArenaPoint.fromTag(tag.getCompound("spawnA")) : null;
            ArenaPoint spawnB = tag.contains("spawnB", net.minecraft.nbt.Tag.TAG_COMPOUND) ? ArenaPoint.fromTag(tag.getCompound("spawnB")) : null;
            ArenaPoint waitingA = tag.contains("waitingA", net.minecraft.nbt.Tag.TAG_COMPOUND) ? ArenaPoint.fromTag(tag.getCompound("waitingA")) : null;
            ArenaPoint waitingB = tag.contains("waitingB", net.minecraft.nbt.Tag.TAG_COMPOUND) ? ArenaPoint.fromTag(tag.getCompound("waitingB")) : null;
            return new TameArena(name, spawnA, spawnB, waitingA, waitingB);
        }
    }
}
