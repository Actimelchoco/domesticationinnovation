package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        private final Set<UUID> originalTeamA;
        private final Set<UUID> originalTeamB;
        private final Set<UUID> roster;
        private final Set<UUID> participants;
        private final List<DuelElimination> eliminations = new ArrayList<>();
        private final Map<UUID, CompoundTag> tameSnapshots = new HashMap<>();
        private final Map<UUID, DuelStats> duelStats = new HashMap<>();
        private final Map<UUID, List<ServerBossEvent>> ownerBossBars = new HashMap<>();

        private DuelBattle(UUID battleId, UUID ownerA, UUID ownerB, Set<UUID> teamA, Set<UUID> teamB) {
            this.battleId = battleId;
            this.ownerA = ownerA;
            this.ownerB = ownerB;
            this.teamA = teamA;
            this.teamB = teamB;
            this.originalTeamA = new HashSet<>(teamA);
            this.originalTeamB = new HashSet<>(teamB);
            this.roster = new HashSet<>();
            this.roster.addAll(teamA);
            this.roster.addAll(teamB);
            this.participants = new HashSet<>();
            this.participants.addAll(this.roster);
        }
    }

    private static final class DuelStats {
        private double points;
        private int kills;
        private int assists;
        private int deaths;
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
    private static final Set<UUID> RECENT_DUEL_ELIMINATIONS = new HashSet<>();

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
            capturePreDuelTameState(server, battle, participantId);
        }
        for (UUID participantId : cleanB) {
            BATTLE_ID_BY_ENTITY.put(participantId, battleId);
            TEAM_A_BY_ENTITY.put(participantId, false);
            capturePreDuelTameState(server, battle, participantId);
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

    public static synchronized boolean consumeRecentDuelElimination(UUID entityId) {
        return entityId != null && RECENT_DUEL_ELIMINATIONS.remove(entityId);
    }

    public static synchronized boolean isSameDuelTeam(UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null) {
            return false;
        }
        UUID firstBattleId = BATTLE_ID_BY_ENTITY.get(firstId);
        UUID secondBattleId = BATTLE_ID_BY_ENTITY.get(secondId);
        if (firstBattleId == null || !firstBattleId.equals(secondBattleId)) {
            return false;
        }
        Boolean firstTeamA = TEAM_A_BY_ENTITY.get(firstId);
        Boolean secondTeamA = TEAM_A_BY_ENTITY.get(secondId);
        return firstTeamA != null && firstTeamA.equals(secondTeamA);
    }

    public static synchronized boolean canProvideSupport(UUID supporterId, UUID beneficiaryId) {
        if (supporterId == null || beneficiaryId == null) {
            return true;
        }
        if (supporterId.equals(beneficiaryId)) {
            return true;
        }
        boolean supporterInDuel = isEntityInDuel(supporterId);
        boolean beneficiaryInDuel = isEntityInDuel(beneficiaryId);
        if (!supporterInDuel && !beneficiaryInDuel) {
            return true;
        }
        return isSameDuelTeam(supporterId, beneficiaryId);
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
                if (!battle.roster.contains(contributorId)) continue;
                assisters.add(contributorId);
            }
            assisters.sort(Comparator.comparing(id -> entityLabel(server, id)));
        }
        UUID duelKiller = killerId != null && battle.roster.contains(killerId) ? killerId : null;
        DuelElimination elimination = new DuelElimination(victimId, duelKiller, assisters);
        battle.eliminations.add(elimination);
        RECENT_DUEL_ELIMINATIONS.add(victimId);
        DuelStats victimStats = battle.duelStats.get(victimId);
        if (victimStats != null) {
            victimStats.deaths++;
        }
        if (duelKiller != null) {
            DuelStats killerStats = battle.duelStats.computeIfAbsent(duelKiller, ignored -> new DuelStats());
            killerStats.kills++;
        }
        for (UUID assisterId : assisters) {
            DuelStats assisterStats = battle.duelStats.computeIfAbsent(assisterId, ignored -> new DuelStats());
            assisterStats.assists++;
        }
        awardDuelPoints(server, battle, elimination);
        notifyElimination(server, battle, elimination);
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
            finishBattle(server, battle, "Forfeit.", ownerId);
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
        finishBattle(server, battle, reason, null);
    }

    private static void finishBattle(MinecraftServer server, DuelBattle battle, String reason, UUID forfeitingOwner) {
        if (battle == null) return;
        BATTLE_BY_ID.remove(battle.battleId);
        List<Component> leaderboardSummary = buildDuelLeaderboardSummary(server, battle);
        List<Component> resultSummary = buildDuelResultSummary(server, battle, forfeitingOwner);

        Set<UUID> allParticipants = new HashSet<>(battle.roster);
        int restoredCount = 0;
        for (UUID participantId : allParticipants) {
            BATTLE_ID_BY_ENTITY.remove(participantId);
            TEAM_A_BY_ENTITY.remove(participantId);
            RECENT_DUEL_ELIMINATIONS.remove(participantId);
            clearTargetForParticipant(server, participantId);
            CompoundTag snapshot = battle.tameSnapshots.get(participantId);
            if (snapshot != null && TameCommands.restoreDuelParticipantSnapshot(server, snapshot.copy())) {
                restoredCount++;
            } else if (snapshot == null && TameCommands.resetDuelCombatState(server, participantId)) {
                restoredCount++;
            }
        }
        notifyOwner(server, battle.ownerA, resultSummary, leaderboardSummary);
        if (!battle.ownerA.equals(battle.ownerB)) {
            notifyOwner(server, battle.ownerB, resultSummary, leaderboardSummary);
        }
    }

    private static void clearTargetForParticipant(MinecraftServer server, UUID participantId) {
        TamableAnimal tame = findLoadedTame(server, participantId);
        if (tame == null) return;
        tame.setTarget(null);
        tame.getNavigation().stop();
    }

    private static void notifyOwner(MinecraftServer server, UUID ownerId, List<Component> resultSummary, List<Component> leaderboardSummary) {
        if (server == null || ownerId == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            for (Component line : resultSummary) {
                owner.sendSystemMessage(line);
            }
            for (Component line : leaderboardSummary) {
                owner.sendSystemMessage(line);
            }
        }
    }

    private static void setupBossBars(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) {
            return;
        }
        createOwnerBossBars(server, battle, battle.ownerA);
        if (!battle.ownerA.equals(battle.ownerB)) {
            createOwnerBossBars(server, battle, battle.ownerB);
        }
        updateBossBars(server, battle);
    }

    private static void createOwnerBossBars(MinecraftServer server, DuelBattle battle, UUID ownerId) {
        if (server == null || battle == null || ownerId == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
        if (player == null) {
            return;
        }
        List<ServerBossEvent> bars = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ServerBossEvent bar = new ServerBossEvent(Component.literal("Duel"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.PROGRESS);
            bar.setVisible(true);
            bar.addPlayer(player);
            bars.add(bar);
        }
        battle.ownerBossBars.put(ownerId, bars);
    }

    private static void updateBossBars(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) {
            return;
        }
        updateOwnerBossBars(server, battle, battle.ownerA);
        if (!battle.ownerA.equals(battle.ownerB)) {
            updateOwnerBossBars(server, battle, battle.ownerB);
        }
    }

    private static void updateOwnerBossBars(MinecraftServer server, DuelBattle battle, UUID ownerId) {
        if (server == null || battle == null || ownerId == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(ownerId);
        if (player == null) {
            return;
        }
        List<ServerBossEvent> bars = battle.ownerBossBars.get(ownerId);
        if (bars == null || bars.size() < 6) {
            createOwnerBossBars(server, battle, ownerId);
            bars = battle.ownerBossBars.get(ownerId);
        }
        if (bars == null || bars.size() < 6) {
            return;
        }

        boolean ownerIsTeamA = ownerId.equals(battle.ownerA);
        Set<UUID> ownTeam = ownerIsTeamA ? battle.originalTeamA : battle.originalTeamB;
        Set<UUID> enemyTeam = ownerIsTeamA ? battle.originalTeamB : battle.originalTeamA;

        TeamBossStats ownStats = summarizeBossTeam(server, battle, ownTeam);
        TeamBossStats enemyStats = summarizeBossTeam(server, battle, enemyTeam);

        applyCountBar(bars.get(0), Component.literal("Your team ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(ownStats.alive + " alive").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(ownStats.dead + " dead").withStyle(ChatFormatting.RED)),
                ownStats.total <= 0 ? 0.0F : (float) ownStats.alive / (float) ownStats.total,
                BossEvent.BossBarColor.BLUE);
        String opposingLabel = battle.ownerA.equals(battle.ownerB) ? "Opposing team " : "Enemy team ";
        applyCountBar(bars.get(1), Component.literal(opposingLabel).withStyle(ChatFormatting.GOLD)
                .append(Component.literal(enemyStats.alive + " alive").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" / ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(enemyStats.dead + " dead").withStyle(ChatFormatting.RED)),
                enemyStats.total <= 0 ? 0.0F : (float) enemyStats.alive / (float) enemyStats.total,
                BossEvent.BossBarColor.YELLOW);

        applyTameBar(server, battle, bars.get(2), ownStats.top.get(0), true);
        applyTameBar(server, battle, bars.get(3), ownStats.top.get(1), true);
        applyTameBar(server, battle, bars.get(4), enemyStats.top.get(0), false);
        applyTameBar(server, battle, bars.get(5), enemyStats.top.get(1), false);
    }

    private static void applyCountBar(ServerBossEvent bar, Component name, float progress, BossEvent.BossBarColor color) {
        if (bar == null) {
            return;
        }
        bar.setName(name);
        bar.setColor(color);
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
    }

    private static void applyTameBar(MinecraftServer server, DuelBattle battle, ServerBossEvent bar, UUID tameId, boolean friendly) {
        if (bar == null) {
            return;
        }
        if (tameId == null) {
            bar.setName(Component.literal(friendly ? "No team tame" : "No enemy tame").withStyle(ChatFormatting.DARK_GRAY));
            bar.setColor(BossEvent.BossBarColor.WHITE);
            bar.setProgress(0.0F);
            return;
        }
        TameData data = tameDataForSummary(server, battle, tameId);
        LivingEntity current = findLoadedLivingParticipant(server, tameId);
        boolean alive = current != null && current.isAlive() && battle.duelStats.getOrDefault(tameId, new DuelStats()).deaths <= 0;
        String label = data == null ? entityLabel(server, tameId) : "[Lvl " + Math.max(1, data.level) + "] " + entityLabel(server, tameId);
        MutableComponent name = Component.literal(label + " ").withStyle(alive ? (friendly ? ChatFormatting.AQUA : ChatFormatting.GOLD) : ChatFormatting.DARK_RED);
        if (alive && current != null) {
            name.append(Component.literal("(" + formatHealth(current) + ")").withStyle(ChatFormatting.RED));
        } else {
            name.append(Component.literal("(dead)").withStyle(ChatFormatting.DARK_RED));
        }
        bar.setName(name);
        bar.setColor(alive ? (friendly ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.YELLOW) : BossEvent.BossBarColor.RED);
        float progress = alive && current != null && current.getMaxHealth() > 0.0F ? current.getHealth() / current.getMaxHealth() : 0.0F;
        bar.setProgress(Math.max(0.0F, Math.min(1.0F, progress)));
    }

    private static void removeBossBars(DuelBattle battle) {
        if (battle == null) {
            return;
        }
        for (List<ServerBossEvent> bars : battle.ownerBossBars.values()) {
            if (bars == null) {
                continue;
            }
            for (ServerBossEvent bar : bars) {
                if (bar != null) {
                    bar.removeAllPlayers();
                    bar.setVisible(false);
                }
            }
        }
        battle.ownerBossBars.clear();
    }

    private static TeamBossStats summarizeBossTeam(MinecraftServer server, DuelBattle battle, Set<UUID> teamIds) {
        int total = 0;
        int alive = 0;
        int dead = 0;
        List<UUID> ranked = new ArrayList<>();
        for (UUID participantId : teamIds) {
            TameData data = tameDataForSummary(server, battle, participantId);
            if (data == null) {
                continue;
            }
            total++;
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            if (stats.deaths > 0) {
                dead++;
            } else {
                alive++;
            }
            ranked.add(participantId);
        }
        ranked.sort((a, b) -> {
            TameData left = tameDataForSummary(server, battle, a);
            TameData right = tameDataForSummary(server, battle, b);
            int leftLevel = left == null ? 1 : Math.max(1, left.level);
            int rightLevel = right == null ? 1 : Math.max(1, right.level);
            int compare = Integer.compare(rightLevel, leftLevel);
            if (compare != 0) {
                return compare;
            }
            return entityLabel(server, a).compareToIgnoreCase(entityLabel(server, b));
        });
        while (ranked.size() < 2) {
            ranked.add(null);
        }
        return new TeamBossStats(total, alive, dead, ranked.subList(0, 2));
    }

    private static void notifyElimination(MinecraftServer server, DuelBattle battle, DuelElimination elimination) {
        if (server == null || battle == null || elimination == null || elimination.victimId == null) {
            return;
        }
        Component positiveLine = eliminationLine(server, elimination, true);
        Component negativeLine = eliminationLine(server, elimination, false);
        if (positiveLine == null || negativeLine == null) {
            return;
        }
        UUID killerOwner = participantOwner(battle, elimination.killerId);
        UUID victimOwner = participantOwner(battle, elimination.victimId);

        if (killerOwner != null && killerOwner.equals(victimOwner)) {
            ServerPlayer sameOwner = server.getPlayerList().getPlayer(killerOwner);
            if (sameOwner != null) {
                sameOwner.sendSystemMessage(positiveLine);
            }
            return;
        }

        if (killerOwner != null) {
            ServerPlayer killerPlayer = server.getPlayerList().getPlayer(killerOwner);
            if (killerPlayer != null) {
                killerPlayer.sendSystemMessage(positiveLine);
            }
        }
        if (victimOwner != null && !victimOwner.equals(killerOwner)) {
            ServerPlayer victimPlayer = server.getPlayerList().getPlayer(victimOwner);
            if (victimPlayer != null) {
                victimPlayer.sendSystemMessage(negativeLine);
            }
        }
    }

    private static Component eliminationLine(MinecraftServer server, DuelElimination elimination, boolean positive) {
        if (elimination == null || elimination.victimId == null) {
            return null;
        }
        MutableComponent line = Component.literal("Duel: ").withStyle(positive ? ChatFormatting.BLUE : ChatFormatting.YELLOW);
        if (elimination.killerId != null) {
            line.append(Component.literal(entityLabel(server, elimination.killerId)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" killed ").withStyle(ChatFormatting.WHITE));
        } else {
            line.append(Component.literal("A tame killed ").withStyle(ChatFormatting.WHITE));
        }
        line.append(Component.literal(entityLabel(server, elimination.victimId)).withStyle(ChatFormatting.RED));
        if (elimination.assisterIds != null && !elimination.assisterIds.isEmpty()) {
            line.append(Component.literal(" assists: ").withStyle(ChatFormatting.WHITE));
            for (int i = 0; i < elimination.assisterIds.size(); i++) {
                if (i > 0) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
                }
                line.append(Component.literal(entityLabel(server, elimination.assisterIds.get(i))).withStyle(ChatFormatting.GRAY));
            }
        }
        return line;
    }

    private static UUID participantOwner(DuelBattle battle, UUID participantId) {
        if (battle == null || participantId == null) {
            return null;
        }
        if (participantId.equals(battle.ownerA)) {
            return battle.ownerA;
        }
        if (participantId.equals(battle.ownerB)) {
            return battle.ownerB;
        }
        TameData data = TameRegistry.get(participantId);
        if (data != null && data.ownerUUID != null) {
            return data.ownerUUID;
        }
        if (battle.teamA.contains(participantId)) {
            return battle.ownerA;
        }
        if (battle.teamB.contains(participantId)) {
            return battle.ownerB;
        }
        return null;
    }

    private static List<Component> buildDuelLeaderboardSummary(MinecraftServer server, DuelBattle battle) {
        List<Component> lines = new ArrayList<>();
        if (server == null || battle == null || battle.roster.isEmpty()) {
            return lines;
        }
        List<UUID> ordered = new ArrayList<>();
        for (UUID participantId : battle.roster) {
            if (tameDataForSummary(server, battle, participantId) != null) {
                ordered.add(participantId);
            }
        }
        ordered.sort((a, b) -> {
            DuelStats statsA = battle.duelStats.getOrDefault(a, new DuelStats());
            DuelStats statsB = battle.duelStats.getOrDefault(b, new DuelStats());
            int scoreCompare = Double.compare(statsB.points, statsA.points);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            int killsCompare = Integer.compare(statsB.kills, statsA.kills);
            if (killsCompare != 0) {
                return killsCompare;
            }
            int assistsCompare = Integer.compare(statsB.assists, statsA.assists);
            if (assistsCompare != 0) {
                return assistsCompare;
            }
            return entityLabel(server, a).compareToIgnoreCase(entityLabel(server, b));
        });

        lines.add(Component.literal("Duel results:").withStyle(ChatFormatting.GOLD));
        int rank = 1;
        for (UUID participantId : ordered) {
            TameData data = tameDataForSummary(server, battle, participantId);
            if (data == null) {
                continue;
            }
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            boolean died = stats.deaths > 0;
            String displayName = data.name == null || data.name.isBlank() ? entityLabel(server, participantId) : data.name;
            LivingEntity currentEntity = died ? null : findLoadedLivingParticipant(server, participantId);
            MutableComponent row = Component.literal(rank + ". ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("(" + ownerInitials(server, data.ownerUUID) + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[" + Math.max(1, data.level) + "] ").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(displayName + " ").withStyle(died ? ChatFormatting.DARK_RED : ChatFormatting.AQUA));
            if (!died && currentEntity != null) {
                row = row.append(Component.literal("(" + formatHealth(currentEntity) + ") ").withStyle(ChatFormatting.RED));
            }
            row = row
                    .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("p:" + formatPoints(stats.points)).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("k:" + stats.kills).withStyle(ChatFormatting.RED))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("a:" + stats.assists).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(row);
            rank++;
        }
        return lines;
    }

    private static List<Component> buildDuelResultSummary(MinecraftServer server, DuelBattle battle, UUID forfeitingOwner) {
        List<Component> lines = new ArrayList<>();
        if (server == null || battle == null) {
            return lines;
        }
        TeamResult teamA = summarizeTeam(server, battle, battle.originalTeamA, battle.ownerA, "A");
        TeamResult teamB = summarizeTeam(server, battle, battle.originalTeamB, battle.ownerB, "B");
        TeamResult winner = determineWinningTeam(teamA, teamB, forfeitingOwner);
        TeamResult loser = winner == teamA ? teamB : teamA;

        lines.add(Component.literal("_________Duel ended_________:").withStyle(ChatFormatting.GOLD));
        lines.add(buildWinnerLoserLine("Winner", winner, true));
        lines.add(buildWinnerLoserLine("Loser", loser, false));
        return lines;
    }

    private static TeamResult determineWinningTeam(TeamResult teamA, TeamResult teamB, UUID forfeitingOwner) {
        if (forfeitingOwner != null) {
            if (forfeitingOwner.equals(teamA.ownerId) && !forfeitingOwner.equals(teamB.ownerId)) {
                return teamB;
            }
            if (forfeitingOwner.equals(teamB.ownerId) && !forfeitingOwner.equals(teamA.ownerId)) {
                return teamA;
            }
        }
        if (teamA.survived != teamB.survived) {
            return teamA.survived > teamB.survived ? teamA : teamB;
        }
        if (teamA.died != teamB.died) {
            return teamA.died < teamB.died ? teamA : teamB;
        }
        if (teamA.totalLevel != teamB.totalLevel) {
            return teamA.totalLevel >= teamB.totalLevel ? teamA : teamB;
        }
        return teamA;
    }

    private static MutableComponent buildWinnerLoserLine(String label, TeamResult team, boolean winner) {
        MutableComponent line = Component.literal(label + ": ").withStyle(winner ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(team.displayName).withStyle(winner ? ChatFormatting.AQUA : ChatFormatting.GRAY))
                .append(Component.literal(" tames:").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(team.tameCount)).withStyle(winner ? ChatFormatting.YELLOW : ChatFormatting.RED))
                .append(Component.literal(" total level:").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(team.totalLevel)).withStyle(ChatFormatting.YELLOW));
        if (winner) {
            line.append(Component.literal(" Survived:").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(team.survived)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" Died:").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(team.died)).withStyle(ChatFormatting.RED));
        }
        return line;
    }

    private static TeamResult summarizeTeam(MinecraftServer server, DuelBattle battle, Set<UUID> teamIds, UUID ownerId, String fallbackName) {
        int tameCount = 0;
        int totalLevel = 0;
        int survived = 0;
        int died = 0;
        UUID highestId = null;
        int highestLevel = Integer.MIN_VALUE;
        for (UUID participantId : teamIds) {
            TameData data = tameDataForSummary(server, battle, participantId);
            if (data == null) {
                continue;
            }
            tameCount++;
            int level = Math.max(1, data.level);
            totalLevel += level;
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            if (stats.deaths > 0) {
                died++;
            } else {
                survived++;
            }
            if (level > highestLevel) {
                highestLevel = level;
                highestId = participantId;
            }
        }
        String displayName = teamDisplayName(server, battle, ownerId, highestId, fallbackName);
        return new TeamResult(ownerId, displayName, tameCount, totalLevel, survived, died);
    }

    private static String teamDisplayName(MinecraftServer server, DuelBattle battle, UUID ownerId, UUID highestId, String fallbackName) {
        if (battle != null && battle.ownerA != null && battle.ownerB != null && !battle.ownerA.equals(battle.ownerB)) {
            ServerPlayer owner = server == null ? null : server.getPlayerList().getPlayer(ownerId);
            if (owner != null) {
                return owner.getName().getString();
            }
            if (server != null && server.getProfileCache() != null && ownerId != null) {
                java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(ownerId);
                if (profile.isPresent()) {
                    return profile.get().getName();
                }
            }
        }
        TameData highest = highestId == null ? null : tameDataForSummary(server, battle, highestId);
        if (highest != null && highest.name != null && !highest.name.isBlank()) {
            return highest.name + "'s team";
        }
        return "Team " + fallbackName;
    }

    private record TeamResult(UUID ownerId, String displayName, int tameCount, int totalLevel, int survived, int died) {
    }

    private record TeamBossStats(int total, int alive, int dead, List<UUID> top) {
    }

    private static TameData tameDataForSummary(MinecraftServer server, DuelBattle battle, UUID participantId) {
        TameData live = TameRegistry.get(participantId);
        if (live != null) {
            return live;
        }
        CompoundTag snapshot = battle.tameSnapshots.get(participantId);
        if (snapshot != null && !snapshot.isEmpty()) {
            return TameData.fromTag(snapshot.copy());
        }
        return null;
    }

    private static void capturePreDuelTameState(MinecraftServer server, DuelBattle battle, UUID participantId) {
        if (server == null || battle == null || participantId == null) {
            return;
        }
        TameData data = TameRegistry.get(participantId);
        if (data == null) {
            return;
        }
        TameData snapshot = TameData.fromTag(data.toTag().copy());
        TamableAnimal loaded = findLoadedTame(server, participantId);
        if (loaded != null) {
            CompoundTag entitySnapshot = new CompoundTag();
            loaded.save(entitySnapshot);
            snapshot.entitySnapshot = entitySnapshot;
            snapshot.lastKnownDimension = loaded.level().dimension().location().toString();
            snapshot.lastKnownX = loaded.blockPosition().getX();
            snapshot.lastKnownY = loaded.blockPosition().getY();
            snapshot.lastKnownZ = loaded.blockPosition().getZ();
        }
        snapshot.dead = false;
        snapshot.stored = false;
        battle.tameSnapshots.put(participantId, snapshot.toTag());
        battle.duelStats.putIfAbsent(participantId, new DuelStats());
    }

    private static String ownerInitials(MinecraftServer server, UUID ownerUuid) {
        if (ownerUuid == null) {
            return "--";
        }
        String name = null;
        if (server != null) {
            ServerPlayer live = server.getPlayerList().getPlayer(ownerUuid);
            if (live != null) {
                name = live.getGameProfile().getName();
            } else if (server.getProfileCache() != null) {
                java.util.Optional<com.mojang.authlib.GameProfile> profile = server.getProfileCache().get(ownerUuid);
                if (profile.isPresent()) {
                    name = profile.get().getName();
                }
            }
        }
        if (name == null || name.isBlank()) {
            return "??";
        }
        String trimmed = name.trim();
        return trimmed.substring(0, Math.min(2, trimmed.length()));
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

    private static String formatPoints(double value) {
        double rounded = Math.rint(value);
        if (Math.abs(value - rounded) < 0.0001D) {
            return Integer.toString((int) rounded);
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private static void awardDuelPoints(MinecraftServer server, DuelBattle battle, DuelElimination elimination) {
        if (battle == null || elimination == null || elimination.victimId == null) {
            return;
        }
        TameData victimData = tameDataForSummary(server, battle, elimination.victimId);
        double pointsPool = victimData == null ? 0.0D : Math.max(1, victimData.level);
        UUID killerId = elimination.killerId;
        int assisterSlots = elimination.assisterIds == null ? 0 : elimination.assisterIds.size();
        double killerBasePoints = 0.0D;
        double assisterPoolPoints = 0.0D;
        if (killerId != null) {
            if (assisterSlots <= 0) {
                killerBasePoints = pointsPool;
            } else if (assisterSlots == 1) {
                killerBasePoints = pointsPool * 0.75D;
                assisterPoolPoints = pointsPool * 0.25D;
            } else {
                killerBasePoints = pointsPool * 0.50D;
                assisterPoolPoints = pointsPool * 0.50D;
            }
        } else if (assisterSlots > 0) {
            assisterPoolPoints = pointsPool * 0.50D;
        }
        double assisterShare = assisterSlots <= 0 ? 0.0D : assisterPoolPoints / assisterSlots;
        if (killerId != null) {
            DuelStats killerStats = battle.duelStats.computeIfAbsent(killerId, ignored -> new DuelStats());
            killerStats.points += killerBasePoints;
        }
        if (elimination.assisterIds != null) {
            for (UUID assisterId : elimination.assisterIds) {
                DuelStats assisterStats = battle.duelStats.computeIfAbsent(assisterId, ignored -> new DuelStats());
                assisterStats.points += assisterShare;
            }
        }
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
