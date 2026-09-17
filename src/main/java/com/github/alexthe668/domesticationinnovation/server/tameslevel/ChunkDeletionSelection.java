package com.github.alexthe668.domesticationinnovation.server.tameslevel;

/** Immutable command selection and confirmation state, independent of world storage. */
final class ChunkDeletionSelection {
    static final long TIMEOUT_NANOS = 60_000_000_000L;

    record Point(int blockX, int blockZ, String dimension) {
        int chunkX() { return Math.floorDiv(blockX, 16); }
        int chunkZ() { return Math.floorDiv(blockZ, 16); }
    }

    record Area(Point a, Point b) {
        Area {
            if (!a.dimension().equals(b.dimension())) {
                throw new IllegalArgumentException("A and B are in different dimensions.");
            }
        }
        String dimension() { return a.dimension(); }
        int minX() { return Math.min(a.chunkX(), b.chunkX()); }
        int maxX() { return Math.max(a.chunkX(), b.chunkX()); }
        int minZ() { return Math.min(a.chunkZ(), b.chunkZ()); }
        int maxZ() { return Math.max(a.chunkZ(), b.chunkZ()); }
        long width() { return (long) maxX() - minX() + 1; }
        long height() { return (long) maxZ() - minZ() + 1; }
        long count() { return width() * height(); }
        int confirmationsRequired() { return count() > 100 ? 2 : 1; }
        boolean contains(Point point) {
            return dimension().equals(point.dimension())
                    && point.chunkX() >= minX() && point.chunkX() <= maxX()
                    && point.chunkZ() >= minZ() && point.chunkZ() <= maxZ();
        }
    }

    record Pending(Area area, int confirmations, long expiresAt) {
        static Pending start(Area area, long now) { return new Pending(area, 0, now + TIMEOUT_NANOS); }
        boolean expired(long now) { return now - expiresAt >= 0; }
        Pending confirm(long now) { return new Pending(area, confirmations + 1, now + TIMEOUT_NANOS); }
        boolean complete() { return confirmations >= area.confirmationsRequired(); }
        long secondsLeft(long now) { return Math.max(0, (expiresAt - now + 999_999_999L) / 1_000_000_000L); }
    }
}
