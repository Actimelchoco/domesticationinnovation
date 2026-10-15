package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

/** Server-wide limits, shared by maintenance, entity ticks and owner login. */
public final class GuardianWorkBudget {
    public static final int MAX_RECORDS_PER_TICK = 32;
    public static final int MAX_TRANSITIONS_PER_TICK = 2;
    public static final int CHUNK_REQUEST_INTERVAL = 10;
    public static final int MAX_PENDING_CHUNK_REQUESTS = 8;
    public static final int MAX_REST_WALK_GUARDIANS = 4;
    private int tick = Integer.MIN_VALUE, records, transitions;
    private long lastChunkRequest = Long.MIN_VALUE / 2;

    private void useTick(int now) {
        if (tick == now) return;
        tick = now;
        records = transitions = 0;
    }

    public boolean tryInspect(int now) {
        useTick(now);
        if (records >= MAX_RECORDS_PER_TICK) return false;
        records++;
        return true;
    }

    public boolean tryTransition(int now) {
        useTick(now);
        if (transitions >= MAX_TRANSITIONS_PER_TICK) return false;
        transitions++;
        return true;
    }

    public boolean tryChunkRequest(int now) {
        useTick(now);
        if ((long) now - lastChunkRequest < CHUNK_REQUEST_INTERVAL) return false;
        lastChunkRequest = now;
        return true;
    }
}
