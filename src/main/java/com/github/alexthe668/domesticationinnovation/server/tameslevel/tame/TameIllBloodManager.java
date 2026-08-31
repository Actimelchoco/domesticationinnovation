package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Runtime owner conflicts created by one owner attacking another owner's tame. */
public final class TameIllBloodManager {
    private static final long CONFLICT_TIMEOUT_NANOS = 60_000_000_000L;
    private static final Map<OwnerPair, Long> CONFLICTS = new HashMap<>();

    private TameIllBloodManager() {
    }

    public static boolean add(UUID firstOwner, UUID secondOwner) {
        OwnerPair pair = OwnerPair.of(firstOwner, secondOwner);
        if (pair == null) return false;
        long now = System.nanoTime();
        Long previous = CONFLICTS.get(pair);
        if (previous != null && now - previous < CONFLICT_TIMEOUT_NANOS) return false;
        CONFLICTS.put(pair, now);
        return true;
    }

    public static void recordDamage(UUID firstOwner, UUID secondOwner) {
        OwnerPair pair = OwnerPair.of(firstOwner, secondOwner);
        if (pair != null && has(firstOwner, secondOwner)) CONFLICTS.put(pair, System.nanoTime());
    }

    public static boolean has(UUID firstOwner, UUID secondOwner) {
        OwnerPair pair = OwnerPair.of(firstOwner, secondOwner);
        if (pair == null) return false;
        Long lastDamage = CONFLICTS.get(pair);
        if (lastDamage == null) return false;
        if (System.nanoTime() - lastDamage >= CONFLICT_TIMEOUT_NANOS) {
            CONFLICTS.remove(pair);
            return false;
        }
        return true;
    }

    public static void clear(UUID owner) {
        if (owner != null) CONFLICTS.keySet().removeIf(pair -> pair.includes(owner));
    }

    public static void clearBetween(UUID firstOwner, UUID secondOwner) {
        OwnerPair pair = OwnerPair.of(firstOwner, secondOwner);
        if (pair != null) CONFLICTS.remove(pair);
    }

    private record OwnerPair(UUID first, UUID second) {
        private static OwnerPair of(UUID a, UUID b) {
            if (a == null || b == null || a.equals(b)) return null;
            return a.compareTo(b) <= 0 ? new OwnerPair(a, b) : new OwnerPair(b, a);
        }

        private boolean includes(UUID owner) {
            return first.equals(owner) || second.equals(owner);
        }
    }
}
