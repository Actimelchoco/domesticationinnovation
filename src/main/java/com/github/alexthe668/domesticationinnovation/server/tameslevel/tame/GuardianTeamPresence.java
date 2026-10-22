package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.server.MinecraftServer;
import java.lang.reflect.Method;
import java.util.*;

/** Optional FTB Teams bridge. One lookup per player/owner per snapshot, never per tame. */
final class GuardianTeamPresence {
    private static Method api, manager, teamForPlayer, teamId;
    private static boolean initialized;
    private static final Set<UUID> ONLINE_TEAMS = new HashSet<>();
    private static final Map<UUID, Optional<UUID>> OWNER_TEAMS = new HashMap<>();

    private static UUID team(UUID player) {
        try {
            if (!initialized) {
                initialized = true;
                var apiClass = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI");
                api = apiClass.getMethod("api");
                manager = Class.forName("dev.ftb.mods.ftbteams.api.FTBTeamsAPI$API").getMethod("getManager");
                teamForPlayer = Class.forName("dev.ftb.mods.ftbteams.api.TeamManager").getMethod("getTeamForPlayerID", UUID.class);
                teamId = Class.forName("dev.ftb.mods.ftbteams.api.Team").getMethod("getId");
            }
            if (teamId == null) return null;
            var result = (Optional<?>) teamForPlayer.invoke(manager.invoke(api.invoke(null)), player);
            return result.isPresent() ? (UUID) teamId.invoke(result.get()) : null;
        } catch (ReflectiveOperationException | LinkageError ignored) { return null; }
    }

    static void snapshot(MinecraftServer server) {
        clear();
        for (var player : server.getPlayerList().getPlayers()) {
            UUID id = team(player.getUUID());
            OWNER_TEAMS.put(player.getUUID(), Optional.ofNullable(id));
            if (id != null) ONLINE_TEAMS.add(id);
        }
    }

    static boolean hasOnlineTeammate(UUID owner) {
        if (owner == null || ONLINE_TEAMS.isEmpty()) return false;
        return OWNER_TEAMS.computeIfAbsent(owner, id -> Optional.ofNullable(team(id)))
                .map(ONLINE_TEAMS::contains).orElse(false);
    }

    static void clear() { ONLINE_TEAMS.clear(); OWNER_TEAMS.clear(); }
}
