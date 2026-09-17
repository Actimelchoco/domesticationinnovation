package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import static com.github.alexthe668.domesticationinnovation.server.tameslevel.ChunkDeletionSelection.*;

public final class ChunkDeletionSelectionTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Point negative = new Point(-1, -17, "minecraft:overworld");
        check(negative.chunkX() == -1 && negative.chunkZ() == -2, "Negative coordinates must floor");
        Area reversed = new Area(new Point(31, 31, "minecraft:overworld"), negative);
        check(reversed.count() == 12, "Reversed corners and inclusive rectangle");
        check(reversed.contains(new Point(-16, -32, "minecraft:overworld")), "Minimum boundary included");
        check(reversed.contains(new Point(31, 31, "minecraft:overworld")), "Maximum boundary included");
        check(!reversed.contains(new Point(32, 0, "minecraft:overworld")), "Outside boundary excluded");
        check(!reversed.contains(new Point(0, 0, "minecraft:the_nether")), "Other dimensions excluded");
        try {
            new Area(negative, new Point(0, 0, "minecraft:the_nether"));
            throw new AssertionError("Mismatched dimensions accepted");
        } catch (IllegalArgumentException expected) { }
        Area hundred = new Area(new Point(0, 0, "d"), new Point(1599, 0, "d"));
        Area hundredOne = new Area(new Point(0, 0, "d"), new Point(1600, 0, "d"));
        check(hundred.count() == 100 && hundred.confirmationsRequired() == 1, "100 requires one confirmation");
        check(hundredOne.count() == 101 && hundredOne.confirmationsRequired() == 2, "101 requires two confirmations");
        check(Pending.start(hundred, 0).confirm(1).complete(), "Small selection completes on first confirmation");
        Pending pending = Pending.start(hundredOne, 0);
        check(!pending.expired(TIMEOUT_NANOS - 1), "Request valid before deadline");
        check(pending.expired(TIMEOUT_NANOS), "Request expires at deadline");
        Pending first = pending.confirm(30_000_000_000L);
        check(!first.complete() && first.secondsLeft(30_000_000_000L) == 60, "First confirmation renews timeout");
        check(!first.expired(TIMEOUT_NANOS), "Renewed deadline outlives original");
        check(first.confirm(60_000_000_000L).complete(), "Second confirmation completes");
        Area large = new Area(new Point(-30_000_000, -30_000_000, "d"), new Point(30_000_000, 30_000_000, "d"));
        check(large.count() == 14_062_507_500_001L, "Chunk count must not overflow int");
        check(pending.area() == hundredOne && pending.confirmations() == 0, "Snapshots remain immutable");
        System.out.println("Chunk deletion selection tests passed.");
    }
}
