package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TameDuelManager {

    private static final class DuelBattle {
        private final UUID battleId;
        private final UUID ownerA;
        private final UUID ownerB;
        private final Set<UUID> teamA;
        private final Set<UUID> teamB;

        private DuelBattle(UUID battleId, UUID ownerA, UUID ownerB, Set<UUID> teamA, Set<UUID> teamB) {
            this.battleId = battleId;
            this.ownerA = ownerA;
            this.ownerB = ownerB;
            this.teamA = teamA;
            this.teamB = teamB;
        }
    }

    private static final Map<UUID, DuelBattle> BATTLE_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> BATTLE_ID_BY_TAME = new HashMap<>();
    private static final Map<UUID, Boolean> TEAM_A_BY_TAME = new HashMap<>();

    private TameDuelManager() {
    }

    public static synchronized void startGroupDuel(MinecraftServer server, UUID ownerA, Set<UUID> tamesA, UUID ownerB, Set<UUID> tamesB) {
        if (server == null || ownerA == null || ownerB == null) return;
        if (tamesA == null || tamesB == null || tamesA.isEmpty() || tamesB.isEmpty()) return;

        endDuelsForOwner(server, ownerA);
        if (!ownerA.equals(ownerB)) {
            endDuelsForOwner(server, ownerB);
        }

        Set<UUID> cleanA = new HashSet<>();
        Set<UUID> cleanB = new HashSet<>();
        for (UUID tameId : tamesA) {
            if (tameId == null) continue;
            if (tamesB.contains(tameId)) continue;
            cleanA.add(tameId);
        }
        for (UUID tameId : tamesB) {
            if (tameId == null) continue;
            if (cleanA.contains(tameId)) continue;
            cleanB.add(tameId);
        }
        if (cleanA.isEmpty() || cleanB.isEmpty()) return;

        UUID battleId = UUID.randomUUID();
        DuelBattle battle = new DuelBattle(battleId, ownerA, ownerB, cleanA, cleanB);
        BATTLE_BY_ID.put(battleId, battle);
        for (UUID tameId : cleanA) {
            BATTLE_ID_BY_TAME.put(tameId, battleId);
            TEAM_A_BY_TAME.put(tameId, true);
        }
        for (UUID tameId : cleanB) {
            BATTLE_ID_BY_TAME.put(tameId, battleId);
            TEAM_A_BY_TAME.put(tameId, false);
        }
    }

    public static synchronized boolean areDuelOpponents(UUID attackerTameId, UUID targetTameId) {
        if (attackerTameId == null || targetTameId == null) return false;
        UUID attackerBattleId = BATTLE_ID_BY_TAME.get(attackerTameId);
        UUID targetBattleId = BATTLE_ID_BY_TAME.get(targetTameId);
        if (attackerBattleId == null || !attackerBattleId.equals(targetBattleId)) return false;
        Boolean attackerTeamA = TEAM_A_BY_TAME.get(attackerTameId);
        Boolean targetTeamA = TEAM_A_BY_TAME.get(targetTameId);
        if (attackerTeamA == null || targetTeamA == null) return false;
        return attackerTeamA != targetTeamA;
    }

    public static synchronized boolean isTameInDuel(UUID tameId) {
        if (tameId == null) return false;
        return BATTLE_ID_BY_TAME.containsKey(tameId);
    }

    public static synchronized boolean endDuelForTame(MinecraftServer server, UUID tameId) {
        if (server == null || tameId == null) return false;
        UUID battleId = BATTLE_ID_BY_TAME.remove(tameId);
        if (battleId == null) return false;
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        TEAM_A_BY_TAME.remove(tameId);
        if (battle == null) return true;

        battle.teamA.remove(tameId);
        battle.teamB.remove(tameId);
        clearTargetForTame(server, tameId);

        if (battle.teamA.isEmpty() || battle.teamB.isEmpty()) {
            String reason = battle.teamA.isEmpty() ? "Team A eliminated." : "Team B eliminated.";
            finishBattle(server, battle, reason);
        }
        return true;
    }

    public static synchronized int endDuelsForOwner(MinecraftServer server, UUID ownerId) {
        if (server == null || ownerId == null) return 0;
        Set<UUID> battleIdsToEnd = new HashSet<>();
        for (DuelBattle battle : BATTLE_BY_ID.values()) {
            if (ownerId.equals(battle.ownerA) || ownerId.equals(battle.ownerB)) {
                battleIdsToEnd.add(battle.battleId);
            }
        }
        int ended = 0;
        for (UUID battleId : battleIdsToEnd) {
            DuelBattle battle = BATTLE_BY_ID.get(battleId);
            if (battle == null) continue;
            finishBattle(server, battle, "Forfeit.");
            ended++;
        }
        return ended;
    }

    public static synchronized void tick(MinecraftServer server) {
        if (server == null || BATTLE_BY_ID.isEmpty()) return;
        for (DuelBattle battle : new ArrayList<>(BATTLE_BY_ID.values())) {
            if (battle == null) continue;
            maintainTargets(server, battle.teamA, battle.teamB);
            maintainTargets(server, battle.teamB, battle.teamA);
        }
    }

    private static void maintainTargets(MinecraftServer server, Set<UUID> ownTeam, Set<UUID> enemyTeam) {
        if (ownTeam == null || ownTeam.isEmpty() || enemyTeam == null || enemyTeam.isEmpty()) return;
        for (UUID ownId : ownTeam) {
            TamableAnimal own = findLoadedTame(server, ownId);
            if (own == null || !own.isAlive()) continue;
            // Duel tames are forced into non-sit roaming state.
            own.setOrderedToSit(false);
            if (own instanceof IComandableMob commandable) {
                commandable.setCommand(0);
            }
            TamableAnimal nearest = nearestLoadedOpponent(server, own, enemyTeam);
            if (nearest == null) {
                own.setTarget(null);
                own.getNavigation().stop();
                continue;
            }
            if (own.getTarget() != null) {
                var current = own.getTarget();
                if (!(current instanceof TamableAnimal currentTame) || !areDuelOpponents(own.getUUID(), currentTame.getUUID())) {
                    own.setTarget(null);
                }
            }
            if (own.getTarget() != nearest) {
                own.setTarget(nearest);
            }
        }
    }

    private static TamableAnimal nearestLoadedOpponent(MinecraftServer server, TamableAnimal from, Set<UUID> opponentIds) {
        if (server == null || from == null || opponentIds == null || opponentIds.isEmpty()) return null;
        TamableAnimal best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID opponentId : opponentIds) {
            TamableAnimal candidate = findLoadedTame(server, opponentId);
            if (candidate == null || !candidate.isAlive()) continue;
            if (candidate.level() != from.level()) continue;
            double dist = from.distanceToSqr(candidate);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static void finishBattle(MinecraftServer server, DuelBattle battle, String reason) {
        if (battle == null) return;
        BATTLE_BY_ID.remove(battle.battleId);

        Set<UUID> allTames = new HashSet<>();
        allTames.addAll(battle.teamA);
        allTames.addAll(battle.teamB);
        for (UUID tameId : allTames) {
            BATTLE_ID_BY_TAME.remove(tameId);
            TEAM_A_BY_TAME.remove(tameId);
            clearTargetForTame(server, tameId);
        }
        String message = "Group duel ended" + (reason == null || reason.isBlank() ? "." : ": " + reason);
        notifyOwner(server, battle.ownerA, message);
        if (!battle.ownerA.equals(battle.ownerB)) {
            notifyOwner(server, battle.ownerB, message);
        }
    }

    private static void clearTargetForTame(MinecraftServer server, UUID tameId) {
        TamableAnimal tame = findLoadedTame(server, tameId);
        if (tame == null) return;
        tame.setTarget(null);
        tame.getNavigation().stop();
    }

    private static void notifyOwner(MinecraftServer server, UUID ownerId, String message) {
        if (server == null || ownerId == null || message == null || message.isBlank()) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
        }
    }

    private static TamableAnimal findLoadedTame(MinecraftServer server, UUID tameId) {
        if (server == null || tameId == null) return null;
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(tameId);
            if (entity instanceof TamableAnimal tame && tame.isTame()) {
                return tame;
            }
        }
        return null;
    }
}
