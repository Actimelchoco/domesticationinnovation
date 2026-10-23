package de.ellpeck.naturesaura.compat;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

/** All Nature's Aura internal accesses to a level's active-chunk index use its monitor. */
public final class AuraChunkIndexGuard {
    private AuraChunkIndexGuard() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object put(Long2ObjectOpenHashMap index, long key, Object value) {
        synchronized (index) { return index.put(key, value); }
    }

    public static Object get(Long2ObjectOpenHashMap<?> index, long key) {
        synchronized (index) { return index.get(key); }
    }

    public static Object remove(Long2ObjectOpenHashMap<?> index, long key) {
        synchronized (index) { return index.remove(key); }
    }
}
