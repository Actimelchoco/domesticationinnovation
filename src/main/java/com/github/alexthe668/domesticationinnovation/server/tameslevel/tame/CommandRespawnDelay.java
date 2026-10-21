package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.server.MinecraftServer;

/** A saved Overworld day-time deadline, checked only when a command is used. */
public final class CommandRespawnDelay {
    private CommandRespawnDelay() {}

    public static void onDeath(MinecraftServer server, TameData data) {
        if (server == null || data == null) return;
        long dayTime = server.overworld().getDayTime();
        data.commandRespawnAfterDayTime = (Math.floorDiv(dayTime, 24000L) + 1) * 24000L;
    }

    public static String failure(MinecraftServer server, TameData data) {
        if (server == null || data == null) return null;
        TameData current = TameRegistry.get(data.uuid);
        if (current == null && data.tlId != null) current = TameRegistry.getByTlId(data.tlId);
        TameDeathRecord latest = TameRegistry.LAST_DEATHS.get(current == null ? data.uuid : current.uuid);
        boolean dead = data.dead || current != null && current.dead
                || current == null && latest != null && latest.snapshot != null && latest.snapshot.getBoolean("dead");
        if (!dead) return null;
        long deadline = data.commandRespawnAfterDayTime;
        if (current != null && current.dead) deadline = Math.max(deadline, current.commandRespawnAfterDayTime);
        if (latest != null && latest.snapshot != null && latest.snapshot.contains("commandRespawnAfterDayTime"))
            deadline = Math.max(deadline, latest.snapshot.getLong("commandRespawnAfterDayTime"));
        if (deadline < 0) return null; // Legacy deaths have no reliable day-time timestamp.
        long remaining = deadline - server.overworld().getDayTime();
        if (remaining <= 0) return null;
        return "wait until the next morning to respawn (" + ((remaining + 19) / 20) + " seconds of world time remaining; sleeping counts)";
    }
}
