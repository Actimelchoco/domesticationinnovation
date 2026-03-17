package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
        private final Set<UUID> roster;
        private final Set<UUID> participants;
        private final List<DuelElimination> eliminations = new ArrayList<>();

        private DuelBattle(UUID battleId, UUID ownerA, UUID ownerB, Set<UUID> teamA, Set<UUID> teamB) {
            this.battleId = battleId;
            this.ownerA = ownerA;
            this.ownerB = ownerB;
            this.teamA = teamA;
            this.teamB = teamB;
            this.roster = new HashSet<>();
            this.roster.addAll(teamA);
            this.roster.addAll(teamB);
            this.participants = new HashSet<>();
            this.participants.addAll(this.roster);
        }
    }

    private static final class DuelElimination {
        private final UUID victimId;
        private final UUID killerId;
        private final List<UUID> assisterIds;

        private DuelElimination(UUID victimId, UUID killerId, List<UUID> assisterIds) {
            this.victimId = victimId;
            this.killerId = killerId;
            this.assisterIds = assisterIds;
        }
    }

    private static final Map<UUID, DuelBattle> BATTLE_BY_ID = new HashMap<>();
    private static final Map<UUID, UUID> BATTLE_ID_BY_ENTITY = new HashMap<>();
    private static final Map<UUID, Boolean> TEAM_A_BY_ENTITY = new HashMap<>();

    private TameDuelManager() {
    }

    public static synchronized void startGroupDuel(MinecraftServer server, UUID ownerA, Set<UUID> tamesA, UUID ownerB, Set<UUID> tamesB) {
        startTeamDuel(server, ownerA, tamesA, ownerB, tamesB);
    }

    public static synchronized void startTeamDuel(MinecraftServer server, UUID ownerA, Set<UUID> teamA, UUID ownerB, Set<UUID> teamB) {
        if (server == null || ownerA == null || ownerB == null) return;
        if (teamA == null || teamB == null || teamA.isEmpty() || teamB.isEmpty()) return;

        endDuelsForOwner(server, ownerA);
        if (!ownerA.equals(ownerB)) {
            endDuelsForOwner(server, ownerB);
        }

        Set<UUID> cleanA = new HashSet<>();
        Set<UUID> cleanB = new HashSet<>();
        for (UUID participantId : teamA) {
            if (participantId == null) continue;
            if (teamB.contains(participantId)) continue;
            cleanA.add(participantId);
        }
        for (UUID participantId : teamB) {
            if (participantId == null) continue;
            if (cleanA.contains(participantId)) continue;
            cleanB.add(participantId);
        }
        if (cleanA.isEmpty() || cleanB.isEmpty()) return;

        UUID battleId = UUID.randomUUID();
        DuelBattle battle = new DuelBattle(battleId, ownerA, ownerB, cleanA, cleanB);
        BATTLE_BY_ID.put(battleId, battle);
        for (UUID participantId : cleanA) {
            BATTLE_ID_BY_ENTITY.put(participantId, battleId);
            TEAM_A_BY_ENTITY.put(participantId, true);
        }
        for (UUID participantId : cleanB) {
            BATTLE_ID_BY_ENTITY.put(participantId, battleId);
            TEAM_A_BY_ENTITY.put(participantId, false);
        }
    }

    public static synchronized boolean areDuelOpponents(UUID attackerId, UUID targetId) {
        if (attackerId == null || targetId == null) return false;
        UUID attackerBattleId = BATTLE_ID_BY_ENTITY.get(attackerId);
        UUID targetBattleId = BATTLE_ID_BY_ENTITY.get(targetId);
        if (attackerBattleId == null || !attackerBattleId.equals(targetBattleId)) return false;
        Boolean attackerTeamA = TEAM_A_BY_ENTITY.get(attackerId);
        Boolean targetTeamA = TEAM_A_BY_ENTITY.get(targetId);
        if (attackerTeamA == null || targetTeamA == null) return false;
        return attackerTeamA != targetTeamA;
    }

    public static synchronized boolean isEntityInDuel(UUID entityId) {
        return entityId != null && BATTLE_ID_BY_ENTITY.containsKey(entityId);
    }

    public static synchronized boolean isTameInDuel(UUID tameId) {
        return isEntityInDuel(tameId);
    }

    public static synchronized boolean endDuelForTame(MinecraftServer server, UUID tameId) {
        return endDuelForEntity(server, tameId);
    }

    public static synchronized boolean endDuelForEntity(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) return false;
        UUID battleId = BATTLE_ID_BY_ENTITY.remove(entityId);
        if (battleId == null) return false;
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        TEAM_A_BY_ENTITY.remove(entityId);
        if (battle == null) return true;

        battle.teamA.remove(entityId);
        battle.teamB.remove(entityId);
        battle.participants.remove(entityId);
        clearTargetForParticipant(server, entityId);

        if (battle.teamA.isEmpty() || battle.teamB.isEmpty()) {
            String reason = battle.teamA.isEmpty() ? "Team A eliminated." : "Team B eliminated.";
            finishBattle(server, battle, reason);
        }
        return true;
    }

    public static synchronized void recordElimination(MinecraftServer server, UUID victimId, Set<UUID> contributors, UUID killerId) {
        if (server == null || victimId == null) return;
        UUID battleId = BATTLE_ID_BY_ENTITY.get(victimId);
        if (battleId == null) return;
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        if (battle == null) return;

        List<UUID> assisters = new ArrayList<>();
        if (contributors != null && !contributors.isEmpty()) {
            for (UUID contributorId : contributors) {
                if (contributorId == null || contributorId.equals(victimId)) continue;
                if (killerId != null && killerId.equals(contributorId)) continue;
                if (!battle.participants.contains(contributorId)) continue;
                assisters.add(contributorId);
            }
            assisters.sort(Comparator.comparing(id -> entityLabel(server, id)));
        }
        UUID duelKiller = killerId != null && battle.participants.contains(killerId) ? killerId : null;
        battle.eliminations.add(new DuelElimination(victimId, duelKiller, assisters));
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

    public static synchronized LivingEntity findNearestLoadedOpponent(MinecraftServer server, TamableAnimal tame) {
        if (server == null || tame == null) {
            return null;
        }
        UUID battleId = BATTLE_ID_BY_ENTITY.get(tame.getUUID());
        if (battleId == null) {
            return null;
        }
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        if (battle == null) {
            return null;
        }
        Boolean tameTeamA = TEAM_A_BY_ENTITY.get(tame.getUUID());
        if (tameTeamA == null) {
            return null;
        }
        Set<UUID> opponents = tameTeamA ? battle.teamB : battle.teamA;
        return nearestLoadedOpponent(server, tame, opponents);
    }

    private static void maintainTargets(MinecraftServer server, Set<UUID> ownTeam, Set<UUID> enemyTeam) {
        if (ownTeam == null || ownTeam.isEmpty() || enemyTeam == null || enemyTeam.isEmpty()) return;
        for (UUID ownId : ownTeam) {
            TamableAnimal own = findLoadedTame(server, ownId);
            if (own == null || !own.isAlive()) continue;
            own.setOrderedToSit(false);
            if (own instanceof IComandableMob commandable) {
                commandable.setCommand(0);
            }
            LivingEntity nearest = nearestLoadedOpponent(server, own, enemyTeam);
            if (nearest == null) {
                own.setTarget(null);
                own.getNavigation().stop();
                continue;
            }
            if (own.getTarget() != null) {
                LivingEntity current = own.getTarget();
                if (current == null || !areDuelOpponents(own.getUUID(), current.getUUID())) {
                    own.setTarget(null);
                }
            }
            if (own.getTarget() != nearest) {
                own.setTarget(nearest);
            }
        }
    }

    private static LivingEntity nearestLoadedOpponent(MinecraftServer server, TamableAnimal from, Set<UUID> opponentIds) {
        if (server == null || from == null || opponentIds == null || opponentIds.isEmpty()) return null;
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID opponentId : opponentIds) {
            LivingEntity candidate = findLoadedLivingParticipant(server, opponentId);
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

        List<String> healthSummary = buildHealthSummary(server, battle);
        List<String> eliminationSummary = buildEliminationSummary(server, battle);

        Set<UUID> allParticipants = new HashSet<>(battle.roster);
        int resetCount = 0;
        for (UUID participantId : allParticipants) {
            BATTLE_ID_BY_ENTITY.remove(participantId);
            TEAM_A_BY_ENTITY.remove(participantId);
            clearTargetForParticipant(server, participantId);
            if (TameCommands.resetDuelCombatState(server, participantId)) {
                resetCount++;
            }
        }
        String message = "Group duel ended" + (reason == null || reason.isBlank() ? "." : ": " + reason);
        if (resetCount > 0) {
            message += " Reset " + resetCount + " tame(s): respawn/heal/effects/cooldowns.";
        }
        notifyOwner(server, battle.ownerA, message, healthSummary, eliminationSummary);
        if (!battle.ownerA.equals(battle.ownerB)) {
            notifyOwner(server, battle.ownerB, message, healthSummary, eliminationSummary);
        }
    }

    private static void clearTargetForParticipant(MinecraftServer server, UUID participantId) {
        TamableAnimal tame = findLoadedTame(server, participantId);
        if (tame == null) return;
        tame.setTarget(null);
        tame.getNavigation().stop();
    }

    private static void notifyOwner(MinecraftServer server, UUID ownerId, String message, List<String> healthSummary, List<String> eliminationSummary) {
        if (server == null || ownerId == null || message == null || message.isBlank()) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            owner.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
            for (String line : healthSummary) {
                owner.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GOLD));
            }
            for (String line : eliminationSummary) {
                owner.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private static List<String> buildHealthSummary(MinecraftServer server, DuelBattle battle) {
        List<String> lines = new ArrayList<>();
        if (server == null || battle == null || battle.roster.isEmpty()) {
            return lines;
        }
        lines.add("Final health:");
        List<UUID> ordered = new ArrayList<>(battle.roster);
        ordered.sort(Comparator.comparing(id -> entityLabel(server, id)));
        for (UUID participantId : ordered) {
            LivingEntity entity = findLoadedLivingParticipant(server, participantId);
            lines.add(" - " + entityLabel(server, participantId) + ": " + formatHealth(entity));
        }
        return lines;
    }

    private static List<String> buildEliminationSummary(MinecraftServer server, DuelBattle battle) {
        List<String> lines = new ArrayList<>();
        if (server == null || battle == null || battle.eliminations.isEmpty()) {
            return lines;
        }
        lines.add("Duel KOs:");
        for (DuelElimination elimination : battle.eliminations) {
            if (elimination == null || elimination.victimId == null) continue;
            StringBuilder line = new StringBuilder();
            line.append(" - ").append(entityLabel(server, elimination.victimId)).append(" was killed");
            if (elimination.killerId != null) {
                line.append(" by ").append(entityLabel(server, elimination.killerId));
            }
            if (!elimination.assisterIds.isEmpty()) {
                line.append(" | assists: ");
                for (int i = 0; i < elimination.assisterIds.size(); i++) {
                    if (i > 0) {
                        line.append(", ");
                    }
                    line.append(entityLabel(server, elimination.assisterIds.get(i)));
                }
            }
            lines.add(line.toString());
        }
        return lines;
    }

    private static String entityLabel(MinecraftServer server, UUID entityId) {
        if (entityId == null) {
            return "unknown";
        }
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(entityId);
        if (player != null) {
            return player.getName().getString();
        }
        TameData data = TameRegistry.get(entityId);
        if (data != null && data.name != null && !data.name.isBlank()) {
            return data.name;
        }
        LivingEntity entity = findLoadedLivingParticipant(server, entityId);
        if (entity != null) {
            return entity.hasCustomName() && entity.getCustomName() != null
                    ? entity.getCustomName().getString()
                    : entity.getName().getString();
        }
        return entityId.toString();
    }

    private static String formatHealth(LivingEntity entity) {
        if (entity == null) {
            return "dead";
        }
        return formatNumber(entity.getHealth()) + "/" + formatNumber(entity.getMaxHealth());
    }

    private static String formatNumber(float value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static TamableAnimal findLoadedTame(MinecraftServer server, UUID tameId) {
        LivingEntity entity = findLoadedLivingParticipant(server, tameId);
        return entity instanceof TamableAnimal tame && tame.isTame() ? tame : null;
    }

    private static LivingEntity findLoadedLivingParticipant(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) return null;
        ServerPlayer player = server.getPlayerList().getPlayer(entityId);
        if (player != null && player.isAlive()) {
            return player;
        }
        for (var level : server.getAllLevels()) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }
}
