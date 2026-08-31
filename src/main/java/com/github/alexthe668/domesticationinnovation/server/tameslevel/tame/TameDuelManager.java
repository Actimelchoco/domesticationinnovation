package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameStoredArmorEvents;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.world.BossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TameDuelManager {
    private static final double DUEL_MMR_K = 48.0D;
    private static final UUID DUEL_FOLLOW_RANGE_MOD = UUID.fromString("2b8f74de-3733-4f92-bad9-3d5f8a8d3f91");
    private static final double DUEL_FOLLOW_RANGE_BONUS = 64.0D;
    private static final String DUEL_MOVEMENT_LOCK_KEY = "duel_movement_lock";
    private static final double DUEL_PARTICIPANT_MAX_DRIFT_SQR = 96.0D * 96.0D;
    // Duel maintenance runs every 40 ticks. Keep enough overlap that latency cannot make the outline blink.
    private static final int DUEL_GLOW_DURATION_TICKS = 100;

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
        private final Map<UUID, CompoundTag> playerSnapshots = new HashMap<>();
        private final Map<UUID, DuelStats> duelStats = new HashMap<>();
        private final Map<String, String> originalScoreboardTeams = new HashMap<>();
        private final Map<UUID, List<ServerBossEvent>> ownerBossBars = new HashMap<>();
        private final Set<UUID> spectatorIds = new HashSet<>();
        private final boolean broadcastToServer;
        private final boolean ranked;
        private final boolean teamColoredKillMessages;

        private DuelBattle(UUID battleId, UUID ownerA, UUID ownerB, Set<UUID> teamA, Set<UUID> teamB, Set<UUID> spectatorIds, boolean broadcastToServer, boolean ranked, boolean teamColoredKillMessages) {
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
            if (spectatorIds != null) {
                this.spectatorIds.addAll(spectatorIds);
            }
            this.spectatorIds.remove(ownerA);
            this.spectatorIds.remove(ownerB);
            this.broadcastToServer = broadcastToServer;
            this.ranked = ranked;
            this.teamColoredKillMessages = teamColoredKillMessages;
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
    private static final Set<UUID> TEAM_ONE_GLOWED_ENTITIES = new HashSet<>();
    private static final Map<UUID, Set<String>> BLUE_GLOW_ENTRIES_BY_VIEWER = new HashMap<>();
    private static final Map<UUID, Set<String>> ORANGE_GLOW_ENTRIES_BY_VIEWER = new HashMap<>();
    private static final Map<UUID, List<GoalSnapshotEntry>> MOSSY_GOLEM_TARGET_GOAL_BACKUPS = new HashMap<>();
    private static final Map<UUID, List<GoalSnapshotEntry>> DUEL_FOLLOW_GOAL_BACKUPS = new HashMap<>();

    private static final class GoalSnapshotEntry {
        private final int priority;
        private final Goal goal;

        private GoalSnapshotEntry(int priority, Goal goal) {
            this.priority = priority;
            this.goal = goal;
        }
    }

    private TameDuelManager() {
    }

    public static synchronized void startGroupDuel(MinecraftServer server, UUID ownerA, Set<UUID> tamesA, UUID ownerB, Set<UUID> tamesB) {
        startTeamDuel(server, ownerA, tamesA, ownerB, tamesB);
    }

    public static synchronized void startGroupDuel(MinecraftServer server, UUID ownerA, Set<UUID> tamesA, UUID ownerB, Set<UUID> tamesB, Set<UUID> spectatorIds, boolean broadcastToServer) {
        startTeamDuel(server, ownerA, tamesA, ownerB, tamesB, spectatorIds, broadcastToServer);
    }

    public static synchronized void startTeamDuel(MinecraftServer server, UUID ownerA, Set<UUID> teamA, UUID ownerB, Set<UUID> teamB) {
        startTeamDuel(server, ownerA, teamA, ownerB, teamB, Set.of(), false);
    }

    public static synchronized void startTeamDuel(MinecraftServer server, UUID ownerA, Set<UUID> teamA, UUID ownerB, Set<UUID> teamB, Set<UUID> spectatorIds, boolean broadcastToServer) {
        startTeamDuel(server, ownerA, teamA, ownerB, teamB, spectatorIds, broadcastToServer, false);
    }

    public static synchronized void startTeamDuel(MinecraftServer server, UUID ownerA, Set<UUID> teamA, UUID ownerB, Set<UUID> teamB, Set<UUID> spectatorIds, boolean broadcastToServer, boolean ranked) {
        startTeamDuel(server, ownerA, teamA, ownerB, teamB, spectatorIds, broadcastToServer, ranked, ranked);
    }

    public static synchronized void startTeamDuel(MinecraftServer server, UUID ownerA, Set<UUID> teamA, UUID ownerB, Set<UUID> teamB, Set<UUID> spectatorIds, boolean broadcastToServer, boolean ranked, boolean teamColoredKillMessages) {
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
            TameData participantData = TameRegistry.get(participantId);
            if (participantData == null) participantData = TameRegistry.getByTlId(participantId);
            if (participantData != null && participantData.horseType) continue;
            if (!hasDuelFood(participantId)) continue;
            cleanA.add(participantId);
        }
        for (UUID participantId : teamB) {
            if (participantId == null) continue;
            TameData participantData = TameRegistry.get(participantId);
            if (participantData == null) participantData = TameRegistry.getByTlId(participantId);
            if (participantData != null && participantData.horseType) continue;
            if (cleanA.contains(participantId)) continue;
            if (!hasDuelFood(participantId)) continue;
            cleanB.add(participantId);
        }
        if (cleanA.isEmpty() || cleanB.isEmpty()) return;

        UUID battleId = UUID.randomUUID();
        DuelBattle battle = new DuelBattle(battleId, ownerA, ownerB, cleanA, cleanB, spectatorIds, broadcastToServer, ranked, teamColoredKillMessages);
        BATTLE_BY_ID.put(battleId, battle);
        for (UUID participantId : cleanA) {
            BATTLE_ID_BY_ENTITY.put(participantId, battleId);
            TEAM_A_BY_ENTITY.put(participantId, true);
            setDuelMovementLock(participantId, true);
            capturePreDuelParticipantState(server, battle, participantId);
            prepareParticipantForDuel(server, participantId);
        }
        for (UUID participantId : cleanB) {
            BATTLE_ID_BY_ENTITY.put(participantId, battleId);
            TEAM_A_BY_ENTITY.put(participantId, false);
            setDuelMovementLock(participantId, true);
            capturePreDuelParticipantState(server, battle, participantId);
            prepareParticipantForDuel(server, participantId);
        }
        // Ensure participants have a valid target immediately after duel-start prep.
        maintainTargets(server, battle.teamA, battle.teamB);
        maintainTargets(server, battle.teamB, battle.teamA);
        // Do not leave a restored/snapshotted glow visible for the first duel
        // maintenance interval when the owners have disabled duelGlow.
        clearTeamOneGlow(server, battle);
        syncPlayerEnemyGlow(server, battle);
    }

    public static synchronized boolean areDuelOpponents(UUID attackerId, UUID targetId) {
        if (attackerId == null || targetId == null) return false;
        attackerId = resolveActiveParticipantId(attackerId);
        targetId = resolveActiveParticipantId(targetId);
        if (attackerId == null || targetId == null) return false;
        UUID attackerBattleId = BATTLE_ID_BY_ENTITY.get(attackerId);
        UUID targetBattleId = BATTLE_ID_BY_ENTITY.get(targetId);
        if (attackerBattleId == null || !attackerBattleId.equals(targetBattleId)) return false;
        Boolean attackerTeamA = TEAM_A_BY_ENTITY.get(attackerId);
        Boolean targetTeamA = TEAM_A_BY_ENTITY.get(targetId);
        if (attackerTeamA == null || targetTeamA == null) return false;
        return attackerTeamA != targetTeamA;
    }

    public static synchronized boolean areDuelOwnersOpponents(UUID attackerId, UUID targetId) {
        if (attackerId == null || targetId == null || attackerId.equals(targetId)) {
            return false;
        }
        for (DuelBattle battle : BATTLE_BY_ID.values()) {
            if (battle == null) {
                continue;
            }
            if ((attackerId.equals(battle.ownerA) && targetId.equals(battle.ownerB))
                    || (attackerId.equals(battle.ownerB) && targetId.equals(battle.ownerA))) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean isEntityInDuel(UUID entityId) {
        return resolveActiveParticipantId(entityId) != null;
    }

    public static synchronized boolean isOwnerInDuel(UUID ownerId) {
        if (ownerId == null) return false;
        for (DuelBattle battle : BATTLE_BY_ID.values()) {
            if (battle != null && (ownerId.equals(battle.ownerA) || ownerId.equals(battle.ownerB))) return true;
        }
        return false;
    }

    public static synchronized boolean isTameInDuel(UUID tameId) {
        return isEntityInDuel(tameId);
    }

    public static synchronized boolean consumeRecentDuelElimination(UUID entityId) {
        UUID resolved = resolveActiveParticipantId(entityId);
        boolean consumed = entityId != null && RECENT_DUEL_ELIMINATIONS.remove(entityId);
        if (resolved != null && !resolved.equals(entityId)) {
            consumed |= RECENT_DUEL_ELIMINATIONS.remove(resolved);
        }
        return consumed;
    }

    public static synchronized boolean isSameDuelTeam(UUID firstId, UUID secondId) {
        if (firstId == null || secondId == null) {
            return false;
        }
        firstId = resolveActiveParticipantId(firstId);
        secondId = resolveActiveParticipantId(secondId);
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

    public static synchronized boolean isTeamAEntity(UUID entityId) {
        if (entityId == null) {
            return false;
        }
        entityId = resolveActiveParticipantId(entityId);
        if (entityId == null) {
            return false;
        }
        Boolean teamA = TEAM_A_BY_ENTITY.get(entityId);
        return teamA != null && teamA;
    }

    public static synchronized boolean canProvideSupport(UUID supporterId, UUID beneficiaryId) {
        if (supporterId == null || beneficiaryId == null) {
            return true;
        }
        if (supporterId.equals(beneficiaryId)) {
            return true;
        }
        UUID resolvedSupporterId = resolveActiveParticipantId(supporterId);
        UUID resolvedBeneficiaryId = resolveActiveParticipantId(beneficiaryId);
        if (resolvedSupporterId != null) {
            supporterId = resolvedSupporterId;
        }
        if (resolvedBeneficiaryId != null) {
            beneficiaryId = resolvedBeneficiaryId;
        }
        boolean supporterInDuel = isEntityInDuel(supporterId);
        boolean beneficiaryInDuel = isEntityInDuel(beneficiaryId);
        if (!supporterInDuel && !beneficiaryInDuel) {
            return true;
        }
        return isSameDuelTeam(supporterId, beneficiaryId);
    }

    private static UUID resolveActiveParticipantId(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        if (BATTLE_ID_BY_ENTITY.containsKey(entityId)) {
            return entityId;
        }
        TameData byEntityUuid = TameRegistry.get(entityId);
        if (byEntityUuid != null) {
            if (byEntityUuid.tlId != null && BATTLE_ID_BY_ENTITY.containsKey(byEntityUuid.tlId)) {
                return byEntityUuid.tlId;
            }
            if (byEntityUuid.uuid != null && BATTLE_ID_BY_ENTITY.containsKey(byEntityUuid.uuid)) {
                return byEntityUuid.uuid;
            }
        }
        TameData byTlId = TameRegistry.getByTlId(entityId);
        if (byTlId != null && byTlId.uuid != null && BATTLE_ID_BY_ENTITY.containsKey(byTlId.uuid)) {
            return byTlId.uuid;
        }
        return null;
    }

    private static boolean hasDuelFood(UUID participantId) {
        return participantId != null;
    }

    public static synchronized boolean endDuelForTame(MinecraftServer server, UUID tameId) {
        return endDuelForEntity(server, tameId);
    }

    public static synchronized boolean endDuelForEntity(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) return false;
        UUID resolvedId = resolveActiveParticipantId(entityId);
        if (resolvedId == null) {
            return false;
        }
        UUID battleId = BATTLE_ID_BY_ENTITY.remove(resolvedId);
        if (battleId == null) return false;
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        TEAM_A_BY_ENTITY.remove(resolvedId);
        if (battle == null) return true;

        battle.teamA.remove(resolvedId);
        battle.teamB.remove(resolvedId);
        battle.participants.remove(resolvedId);
        RECENT_DUEL_ELIMINATIONS.add(resolvedId);
        if (!resolvedId.equals(entityId)) {
            RECENT_DUEL_ELIMINATIONS.add(entityId);
        }
        setDuelMovementLock(resolvedId, false);
        clearTargetForParticipant(server, resolvedId);

        if (battle.teamA.isEmpty() || battle.teamB.isEmpty()) {
            String reason = battle.teamA.isEmpty() ? "Team A eliminated." : "Team B eliminated.";
            finishBattle(server, battle, reason);
        } else {
            // Resolve replacement targets immediately after an elimination. Owner-follow
            // goals remain suppressed independently, so targetless participants stay mobile.
            maintainTargets(server, battle.teamA, battle.teamB);
            maintainTargets(server, battle.teamB, battle.teamA);
        }
        return true;
    }

    public static synchronized boolean eliminateParticipantForNoFood(MinecraftServer server, UUID entityId) {
        if (server == null || entityId == null) {
            return false;
        }
        UUID resolvedId = resolveActiveParticipantId(entityId);
        if (resolvedId == null) {
            return false;
        }
        recordElimination(server, resolvedId, Set.of(), null);
        return endDuelForEntity(server, resolvedId);
    }

    public static synchronized void recordElimination(MinecraftServer server, UUID victimId, Set<UUID> contributors, UUID killerId) {
        if (server == null || victimId == null) return;
        UUID originalVictimId = victimId;
        victimId = resolveActiveParticipantId(victimId);
        if (victimId == null) return;
        killerId = resolveActiveParticipantId(killerId);
        UUID battleId = BATTLE_ID_BY_ENTITY.get(victimId);
        if (battleId == null) return;
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        if (battle == null) return;

        UUID duelKiller = killerId != null && battle.roster.contains(killerId) ? killerId : null;
        List<UUID> assisters = new ArrayList<>();
        if (contributors != null && !contributors.isEmpty()) {
            for (UUID contributorId : contributors) {
                contributorId = resolveActiveParticipantId(contributorId);
                if (contributorId == null || contributorId.equals(victimId)) continue;
                if (killerId != null && killerId.equals(contributorId)) continue;
                if (!battle.roster.contains(contributorId)) continue;
                if (!canAssistElimination(battle, contributorId, victimId, duelKiller)) continue;
                assisters.add(contributorId);
            }
            assisters.sort(Comparator.comparing(id -> entityLabel(server, id)));
        }
        DuelElimination elimination = new DuelElimination(victimId, duelKiller, assisters);
        battle.eliminations.add(elimination);
        RECENT_DUEL_ELIMINATIONS.add(victimId);
        if (!victimId.equals(originalVictimId)) {
            RECENT_DUEL_ELIMINATIONS.add(originalVictimId);
        }
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

    private static boolean canAssistElimination(DuelBattle battle, UUID assisterId, UUID victimId, UUID killerId) {
        if (battle == null || assisterId == null || victimId == null) {
            return false;
        }
        Boolean assisterTeam = participantTeamA(battle, assisterId);
        Boolean victimTeam = participantTeamA(battle, victimId);
        if (assisterTeam == null || victimTeam == null || assisterTeam.equals(victimTeam)) {
            return false;
        }
        if (killerId == null) {
            return true;
        }
        Boolean killerTeam = participantTeamA(battle, killerId);
        return killerTeam != null && assisterTeam.equals(killerTeam);
    }

    private static Boolean participantTeamA(DuelBattle battle, UUID participantId) {
        if (battle == null || participantId == null) {
            return null;
        }
        if (battle.originalTeamA.contains(participantId) || battle.teamA.contains(participantId)) {
            return true;
        }
        if (battle.originalTeamB.contains(participantId) || battle.teamB.contains(participantId)) {
            return false;
        }
        return null;
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
            // Remove any legacy vanilla glow, then synchronize only the harmless
            // team marker. Rendering is controlled by each extension client.
            clearTeamOneGlow(server, battle);
            // Player clients also use these private team assignments to distinguish an
            // opposing owned tame from a normal pet. Keep them synced even when the
            // optional visual glow is disabled.
            syncPlayerEnemyGlow(server, battle);
        }
    }

    private static void maintainTeamOneGlow(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) return;
        if (!teamOneGlowEnabled(battle)) return;
        // The synchronized duel-team marker is rendered as glow only by clients
        // carrying the extension. Never apply vanilla's global Glowing effect:
        // it would force unmodified clients to see an uncolored white outline.
    }

    private static void clearTeamOneGlow(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) return;
        // The tracking set is transient and can be lost across a restart while Glowing
        // remains in an entity snapshot. Clear every duel participant.
        for (UUID participantId : battle.roster) {
            TEAM_ONE_GLOWED_ENTITIES.remove(participantId);
            LivingEntity participant = findLoadedLivingParticipant(server, participantId);
            if (participant != null) {
                participant.removeEffect(MobEffects.GLOWING);
            }
        }
    }

    private static void clearBattleViewerGlow(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) return;
        Set<UUID> viewers = new HashSet<>(battle.roster);
        if (battle.ownerA != null) viewers.add(battle.ownerA);
        if (battle.ownerB != null) viewers.add(battle.ownerB);
        viewers.addAll(battle.spectatorIds);
        for (UUID viewerId : viewers) {
            if (server.getPlayerList().getPlayer(viewerId) != null
                    && (BLUE_GLOW_ENTRIES_BY_VIEWER.containsKey(viewerId)
                    || ORANGE_GLOW_ENTRIES_BY_VIEWER.containsKey(viewerId))) {
                clearViewerEnemyGlow(server, viewerId);
            }
        }
    }

    private static boolean teamOneGlowEnabled(DuelBattle battle) {
        if (battle == null) return false;
        // The vanilla glowing flag is global, not viewer-specific. Require both owners to
        // opt in so a player on Team 2 cannot have their false setting ignored merely because
        // Team 1 owns the entity effect.
        return PlayerDebugSettings.duelGlow(battle.ownerA)
                && PlayerDebugSettings.duelGlow(battle.ownerB);
    }

    public static synchronized void refreshGlowSettings(MinecraftServer server) {
        if (server == null) return;
        for (DuelBattle battle : BATTLE_BY_ID.values()) {
            if (battle == null) continue;
            clearTeamOneGlow(server, battle);
            syncPlayerEnemyGlow(server, battle);
        }
    }

    public static synchronized LivingEntity findNearestLoadedOpponent(MinecraftServer server, LivingEntity tame) {
        if (server == null || tame == null) {
            return null;
        }
        UUID tameId = resolveActiveParticipantId(tame.getUUID());
        if (tameId == null) {
            tameId = resolveActiveParticipantId(TameData.getTlId(tame));
        }
        UUID battleId = tameId == null ? null : BATTLE_ID_BY_ENTITY.get(tameId);
        if (battleId == null) {
            return null;
        }
        DuelBattle battle = BATTLE_BY_ID.get(battleId);
        if (battle == null) {
            return null;
        }
        Boolean tameTeamA = TEAM_A_BY_ENTITY.get(tameId);
        if (tameTeamA == null) {
            return null;
        }
        Set<UUID> opponents = tameTeamA ? battle.teamB : battle.teamA;
        return nearestLoadedOpponent(server, tame, opponents);
    }

    public static synchronized void assignDuelTarget(LivingEntity tame, LivingEntity target) {
        setDuelCombatTarget(tame, target);
    }

    public static synchronized void refreshLoadedDuelParticipant(MinecraftServer server, LivingEntity tame) {
        if (server == null || tame == null || !TameEntityAdapter.isTame(tame) || !tame.isAlive()) {
            return;
        }
        UUID tameId = tame.getUUID();
        UUID participantId = resolveActiveParticipantId(tameId);
        if (participantId == null) {
            participantId = resolveActiveParticipantId(TameData.getTlId(tame));
        }
        if (participantId == null) {
            if (tame instanceof TamableAnimal tamable) restoreMossyGolemTargetGoalsIfNeeded(tamable);
            removeDuelFollowRangeBoost(tame);
            return;
        }
        if (tame instanceof TamableAnimal tamable) ensureMossyGolemDuelTargetGoalsIfNeeded(tamable);
        suppressOwnerFollowGoals(tame);
        if (tame instanceof TamableAnimal tamable) forceMossyGolemCombatCommand(tamable);
        applyDuelFollowRangeBoost(tame);
        LivingEntity current = TameEntityAdapter.target(tame);
        if (isUsableCurrentDuelTarget(tame, current)) {
            TameCommands.applyDuelMovementOrder(tame, true);
            setDuelCombatTarget(tame, current);
            return;
        }
        UUID battleId = BATTLE_ID_BY_ENTITY.get(participantId);
        DuelBattle battle = battleId == null ? null : BATTLE_BY_ID.get(battleId);
        Boolean teamA = TEAM_A_BY_ENTITY.get(participantId);
        if (battle == null || teamA == null) {
            clearDuelCombatTarget(tame);
            return;
        }
        Set<UUID> opponents = teamA ? battle.teamB : battle.teamA;
        LivingEntity nearest = nearestLoadedOpponent(server, tame, opponents);
        if (nearest != null) {
            TameCommands.applyDuelMovementOrder(tame, true);
            setDuelCombatTarget(tame, nearest);
        } else {
            clearDuelCombatTarget(tame);
            TameCommands.applyDuelMovementOrder(tame, true);
        }
    }

    private static void maintainTargets(MinecraftServer server, Set<UUID> ownTeam, Set<UUID> enemyTeam) {
        if (ownTeam == null || ownTeam.isEmpty()) return;
        for (UUID ownId : ownTeam) {
            LivingEntity own = findLoadedTame(server, ownId);
            if (own == null || !own.isAlive()) continue;
            applyDuelFollowRangeBoost(own);
            if (enemyTeam != null && !enemyTeam.isEmpty()) {
                keepParticipantNearBattle(server, own, enemyTeam);
            }
            LivingEntity current = TameEntityAdapter.target(own);
            if (isUsableCurrentDuelTarget(own, current)) {
                TameCommands.applyDuelMovementOrder(own, true);
                setDuelCombatTarget(own, current);
                continue;
            }
            LivingEntity nearest = nearestLoadedOpponent(server, own, enemyTeam);
            if (nearest == null) {
                clearDuelCombatTarget(own);
                TameCommands.applyDuelMovementOrder(own, true);
                continue;
            }
            TameCommands.applyDuelMovementOrder(own, true);
            setDuelCombatTarget(own, nearest);
        }
    }

    private static void keepParticipantNearBattle(MinecraftServer server, LivingEntity participant, Set<UUID> enemyTeam) {
        if (server == null || participant == null || enemyTeam == null || enemyTeam.isEmpty()) {
            return;
        }
        LivingEntity nearestSameLevel = nearestLoadedOpponent(server, participant, enemyTeam);
        LivingEntity anchor = nearestSameLevel != null ? nearestSameLevel : firstLoadedOpponent(server, enemyTeam);
        if (anchor == null || !anchor.isAlive() || !(anchor.level() instanceof net.minecraft.server.level.ServerLevel targetLevel)) {
            return;
        }
        boolean wrongDimension = participant.level() != anchor.level();
        boolean tooFar = !wrongDimension && participant.distanceToSqr(anchor) > DUEL_PARTICIPANT_MAX_DRIFT_SQR;
        if (!wrongDimension && !tooFar) {
            return;
        }
        int slot = Math.floorMod(participant.getUUID().hashCode(), 8);
        double angle = (Math.PI * 2.0D / 8.0D) * slot;
        Vec3 targetPos = anchor.position().add(Math.cos(angle) * 3.0D, 0.0D, Math.sin(angle) * 3.0D);
        TameCommands.autoFollowTeleportLoadedToLocation(participant, targetLevel, targetPos, participant.getYRot(), participant.getXRot());
        if (participant instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
        setDuelCombatTarget(participant, anchor);
    }

    private static LivingEntity firstLoadedOpponent(MinecraftServer server, Set<UUID> enemyTeam) {
        if (server == null || enemyTeam == null || enemyTeam.isEmpty()) {
            return null;
        }
        for (UUID enemyId : enemyTeam) {
            LivingEntity living = findLoadedLivingParticipant(server, enemyId);
            if (living != null && living.isAlive()) {
                return living;
            }
        }
        return null;
    }

    private static boolean isUsableCurrentDuelTarget(LivingEntity own, LivingEntity current) {
        if (own == null || current == null || !current.isAlive()) {
            return false;
        }
        if (own.level() != current.level()) {
            return false;
        }
        return areDuelOpponents(own.getUUID(), current.getUUID());
    }

    private static void prepareParticipantForDuel(MinecraftServer server, UUID participantId) {
        resetParticipantCooldownsForDuel(participantId);
        LivingEntity tame = findLoadedLivingParticipant(server, participantId);
        if (tame == null || !tame.isAlive()) {
            return;
        }
        tame.setHealth(tame.getMaxHealth());
        suppressOwnerFollowGoals(tame);
        applyDuelFollowRangeBoost(tame);
        if (tame instanceof TamableAnimal tamable) {
            ensureMossyGolemDuelTargetGoalsIfNeeded(tamable);
            forceMossyGolemCombatCommand(tamable);
        }
        if (tame instanceof IComandableMob commandableMob) {
            commandableMob.setCommand(0);
        }
        // Owner-follow goals are suppressed separately, so participants can start mobile
        // without getting a one-tick opportunity to teleport back to their owner.
        TameCommands.applyDuelMovementOrder(tame, true);
    }

    private static void clearDuelRestingState(LivingEntity tame) {
        if (tame instanceof TamableAnimal tamable) {
            tamable.setOrderedToSit(false);
            tamable.setInSittingPose(false);
        }
        if (tame instanceof Fox fox) {
            fox.setSitting(false);
        }
        tryInvokeDuelBooleanSetter(tame, "setOrderedToSit", false);
        tryInvokeDuelBooleanSetter(tame, "setInSittingPose", false);
        tryInvokeDuelBooleanSetter(tame, "setSitting", false);
        tryInvokeDuelBooleanSetter(tame, "setSleeping", false);
        tryInvokeDuelBooleanSetter(tame, "setPlayingDead", false);
    }

    private static void forceDuelAwakeAndMobile(LivingEntity tame) {
        if (tame == null || !isEntityInDuel(tame.getUUID())) {
            return;
        }
        if (tame instanceof IComandableMob commandableMob) {
            commandableMob.setCommand(0);
        }
        clearDuelRestingState(tame);
    }

    private static void tryInvokeDuelBooleanSetter(LivingEntity tame, String methodName, boolean value) {
        if (tame == null || methodName == null || methodName.isBlank()) {
            return;
        }
        try {
            tame.getClass().getMethod(methodName, boolean.class).invoke(tame, value);
        } catch (ReflectiveOperationException ignored) {
            // Optional interface tames expose different subsets of resting-state setters.
        }
    }

    private static boolean isLegendaryMonstersMossyGolem(TamableAnimal tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (key != null
                && "legendary_monsters".equals(key.getNamespace())
                && "mossy_golem".equals(key.getPath())) {
            return true;
        }
        String className = tame.getClass().getName();
        return className != null && className.toLowerCase(java.util.Locale.ROOT).contains("mossygolem");
    }

    private static void forceMossyGolemCombatCommand(TamableAnimal tame) {
        if (!isLegendaryMonstersMossyGolem(tame)) return;
        try {
            // Legendary Monsters' attack goal refuses to start for command 1 or 2,
            // but MossyGolemEntity does not expose that command through IComandableMob.
            tame.getClass().getMethod("setCommand", int.class).invoke(tame, 0);
        } catch (ReflectiveOperationException exception) {
            System.err.println("[TamesLevel] Could not enable Mossy Golem duel attacks: " + exception.getMessage());
        }
    }

    public static synchronized void restorePostDuelTargetGoalsIfNeeded(TamableAnimal tame) {
        restoreMossyGolemTargetGoalsIfNeeded(tame);
    }

    private static void ensureMossyGolemDuelTargetGoalsIfNeeded(TamableAnimal tame) {
        if (tame == null || !isLegendaryMonstersMossyGolem(tame)) {
            return;
        }
        UUID tameId = tame.getUUID();
        if (tameId == null || MOSSY_GOLEM_TARGET_GOAL_BACKUPS.containsKey(tameId)) {
            return;
        }
        List<GoalSnapshotEntry> backup = new ArrayList<>();
        for (WrappedGoal wrapped : new ArrayList<>(tame.targetSelector.getAvailableGoals())) {
            Goal goal = wrapped.getGoal();
            if (goal == null) {
                continue;
            }
            backup.add(new GoalSnapshotEntry(wrapped.getPriority(), goal));
            tame.targetSelector.removeGoal(goal);
        }
        MOSSY_GOLEM_TARGET_GOAL_BACKUPS.put(tameId, backup);
        tame.targetSelector.addGoal(1, new DuelOpponentNearestTargetGoal(tame));
    }

    private static void restoreMossyGolemTargetGoalsIfNeeded(TamableAnimal tame) {
        if (tame == null) {
            return;
        }
        UUID tameId = tame.getUUID();
        if (tameId == null) {
            return;
        }
        List<GoalSnapshotEntry> backup = MOSSY_GOLEM_TARGET_GOAL_BACKUPS.remove(tameId);
        if (backup == null) {
            return;
        }
        for (WrappedGoal wrapped : new ArrayList<>(tame.targetSelector.getAvailableGoals())) {
            Goal goal = wrapped.getGoal();
            if (goal != null) {
                tame.targetSelector.removeGoal(goal);
            }
        }
        for (GoalSnapshotEntry entry : backup) {
            if (entry == null || entry.goal == null) {
                continue;
            }
            tame.targetSelector.addGoal(Math.max(0, entry.priority), entry.goal);
        }
    }

    private static final class DuelOpponentNearestTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {
        private DuelOpponentNearestTargetGoal(TamableAnimal tame) {
            super(tame, LivingEntity.class, 10, true, false, target ->
                    tame != null
                            && target != null
                            && target.isAlive()
                            && TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID()));
            this.targetConditions.ignoreLineOfSight();
        }
    }

    private static void resetParticipantCooldownsForDuel(UUID participantId) {
        if (participantId == null) {
            return;
        }
        TameData data = TameRegistry.get(participantId);
        if (data == null) {
            data = TameRegistry.getByTlId(participantId);
        }
        if (data == null || data.cooldowns.isEmpty()) {
            return;
        }
        data.cooldowns.clear();
        TameRegistry.markDirty();
    }

    private static LivingEntity nearestLoadedOpponent(MinecraftServer server, LivingEntity from, Set<UUID> opponentIds) {
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
        persistBattleStatsAndMmr(server, battle, forfeitingOwner);
        refreshStoredDuelSnapshotsFromRegistry(battle);
        List<Component> leaderboardSummary = buildDuelLeaderboardSummary(server, battle);
        List<Component> resultSummary = buildDuelResultSummary(server, battle, forfeitingOwner);

        Set<UUID> allParticipants = new HashSet<>(battle.roster);
        clearTeamOneGlow(server, battle);
        clearBattleViewerGlow(server, battle);
        restoreDuelScoreboardTeams(server, battle);
        int restoredCount = 0;
        for (UUID participantId : allParticipants) {
            BATTLE_ID_BY_ENTITY.remove(participantId);
            TEAM_A_BY_ENTITY.remove(participantId);
            setDuelMovementLock(participantId, false);
            clearViewerEnemyGlow(server, participantId);
            clearTargetForParticipant(server, participantId);
            CompoundTag playerSnapshot = battle.playerSnapshots.get(participantId);
            if (playerSnapshot != null && tryRestoreDuelPlayerSnapshot(server, participantId, playerSnapshot)) {
                restoredCount++;
                continue;
            }
            CompoundTag tameSnapshot = battle.tameSnapshots.get(participantId);
            if (tameSnapshot != null) {
                if (tryRestoreDuelParticipantSnapshot(server, tameSnapshot)
                        || tryResetDuelCombatState(server, participantId, 2)
                        || tryEnsureDuelParticipantRestored(server, participantId, tameSnapshot)) {
                    restoredCount++;
                }
            } else if (playerSnapshot == null && tryResetDuelCombatState(server, participantId, 2)) {
                restoredCount++;
            }
        }
        // Restoration can replace an entity after the earlier cleanup. Clear the
        // final instances and persist clean snapshots so duel glow cannot return
        // while the tame is outside a duel.
        clearDuelVisualState(server, allParticipants);
        notifyBattleAudience(server, battle, resultSummary, leaderboardSummary);
    }

    private static boolean tryRestoreDuelPlayerSnapshot(MinecraftServer server, UUID participantId, CompoundTag playerSnapshot) {
        if (playerSnapshot == null || playerSnapshot.isEmpty()) {
            return false;
        }
        try {
            return TameCommands.restoreDuelPlayerSnapshot(server, participantId, playerSnapshot.copy());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryRestoreDuelParticipantSnapshot(MinecraftServer server, CompoundTag tameSnapshot) {
        if (tameSnapshot == null || tameSnapshot.isEmpty()) {
            return false;
        }
        try {
            return TameCommands.restoreDuelParticipantSnapshot(server, tameSnapshot.copy());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean tryResetDuelCombatState(MinecraftServer server, UUID participantId, int attempts) {
        int tries = Math.max(1, attempts);
        for (int i = 0; i < tries; i++) {
            try {
                if (TameCommands.resetDuelCombatState(server, participantId)) {
                    return true;
                }
            } catch (Throwable ignored) {
                // Keep duel cleanup alive even if one reset attempt blows up.
            }
        }
        return false;
    }

    private static boolean tryEnsureDuelParticipantRestored(MinecraftServer server, UUID participantId, CompoundTag tameSnapshot) {
        if (tameSnapshot == null || tameSnapshot.isEmpty()) {
            return false;
        }
        try {
            return TameCommands.ensureDuelParticipantRestored(server, participantId, tameSnapshot.copy());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void syncPlayerEnemyGlow(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) {
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam blueTeam = getOrCreateDuelScoreboardTeam(scoreboard, battle, true);
        PlayerTeam redTeam = getOrCreateDuelScoreboardTeam(scoreboard, battle, false);
        boolean glowEnabled = true;
        // These teams remain useful for client-side duel opponent recognition,
        // but outline color is supplied directly by the extension.
        blueTeam.setColor(ChatFormatting.WHITE);
        redTeam.setColor(ChatFormatting.WHITE);
        syncDuelScoreboardEntries(server, battle, scoreboard, blueTeam, battle.originalTeamA);
        syncDuelScoreboardEntries(server, battle, scoreboard, redTeam, battle.originalTeamB);
        syncDirectDuelOutlineColors(server, battle, glowEnabled);
    }

    private static void syncDirectDuelOutlineColors(MinecraftServer server, DuelBattle battle, boolean enabled) {
        for (UUID participantId : battle.originalTeamA) {
            LivingEntity participant = findLoadedLivingParticipant(server, participantId);
            if (participant != null) TameableUtils.setTamesLevelDuelGlowTeam(participant, enabled ? 1 : 0);
        }
        for (UUID participantId : battle.originalTeamB) {
            LivingEntity participant = findLoadedLivingParticipant(server, participantId);
            if (participant != null) TameableUtils.setTamesLevelDuelGlowTeam(participant, enabled ? 2 : 0);
        }
    }

    private static void clearDuelVisualState(MinecraftServer server, Set<UUID> participantIds) {
        if (server == null || participantIds == null) return;
        for (UUID participantId : participantIds) {
            LivingEntity participant = findLoadedLivingParticipant(server, participantId);
            if (participant == null) continue;
            participant.removeEffect(MobEffects.GLOWING);
            TameableUtils.setTamesLevelDuelGlowTeam(participant, 0);
            TameData data = TameRegistry.get(participantId);
            if (data == null) data = TameRegistry.getByTlId(participantId);
            if (data != null) {
                CompoundTag refreshed = new CompoundTag();
                participant.save(refreshed);
                data.entitySnapshot = refreshed;
                TameRegistry.markDirty();
            }
        }
    }

    private static PlayerTeam getOrCreateDuelScoreboardTeam(Scoreboard scoreboard, DuelBattle battle, boolean teamA) {
        String name = duelGlowTeamName(battle.battleId, teamA);
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);
        }
        team.setAllowFriendlyFire(true);
        return team;
    }

    private static void syncDuelScoreboardEntries(MinecraftServer server, DuelBattle battle, Scoreboard scoreboard,
                                                   PlayerTeam duelTeam, Set<UUID> participantIds) {
        for (String entry : collectGlowEntries(server, participantIds)) {
            if (!battle.originalScoreboardTeams.containsKey(entry)) {
                PlayerTeam original = scoreboard.getPlayersTeam(entry);
                battle.originalScoreboardTeams.put(entry, original == null ? "" : original.getName());
            }
            scoreboard.addPlayerToTeam(entry, duelTeam);
        }
    }

    private static void restoreDuelScoreboardTeams(MinecraftServer server, DuelBattle battle) {
        if (server == null || battle == null) return;
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam blueTeam = scoreboard.getPlayerTeam(duelGlowTeamName(battle.battleId, true));
        PlayerTeam redTeam = scoreboard.getPlayerTeam(duelGlowTeamName(battle.battleId, false));
        for (Map.Entry<String, String> entry : battle.originalScoreboardTeams.entrySet()) {
            String scoreboardEntry = entry.getKey();
            PlayerTeam current = scoreboard.getPlayersTeam(scoreboardEntry);
            if (current == blueTeam || current == redTeam) {
                scoreboard.removePlayerFromTeam(scoreboardEntry, current);
            }
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                PlayerTeam original = scoreboard.getPlayerTeam(entry.getValue());
                if (original != null) scoreboard.addPlayerToTeam(scoreboardEntry, original);
            }
        }
        if (blueTeam != null) scoreboard.removePlayerTeam(blueTeam);
        if (redTeam != null) scoreboard.removePlayerTeam(redTeam);
        battle.originalScoreboardTeams.clear();
    }

    private static void syncViewerTeamGlow(MinecraftServer server, UUID viewerId, Set<UUID> blueIds, Set<UUID> orangeIds) {
        if (server == null || viewerId == null) {
            return;
        }
        ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
        if (viewer == null) {
            BLUE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
            ORANGE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
            return;
        }
        PlayerTeam blueTeam = duelGlowTeam(viewerId, true);
        PlayerTeam orangeTeam = duelGlowTeam(viewerId, false);
        sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(blueTeam, true));
        sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(orangeTeam, true));

        Set<String> desiredBlue = collectGlowEntries(server, blueIds);
        Set<String> desiredOrange = collectGlowEntries(server, orangeIds);
        syncViewerGlowEntries(viewer, blueTeam, BLUE_GLOW_ENTRIES_BY_VIEWER.getOrDefault(viewerId, Set.of()), desiredBlue);
        syncViewerGlowEntries(viewer, orangeTeam, ORANGE_GLOW_ENTRIES_BY_VIEWER.getOrDefault(viewerId, Set.of()), desiredOrange);

        if (desiredBlue.isEmpty()) {
            BLUE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
        } else {
            BLUE_GLOW_ENTRIES_BY_VIEWER.put(viewerId, desiredBlue);
        }
        if (desiredOrange.isEmpty()) {
            ORANGE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
        } else {
            ORANGE_GLOW_ENTRIES_BY_VIEWER.put(viewerId, desiredOrange);
        }
    }

    private static void clearViewerEnemyGlow(MinecraftServer server, UUID viewerId) {
        if (server == null || viewerId == null) {
            return;
        }
        ServerPlayer viewer = server.getPlayerList().getPlayer(viewerId);
        Set<String> previousBlue = BLUE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
        Set<String> previousOrange = ORANGE_GLOW_ENTRIES_BY_VIEWER.remove(viewerId);
        if (viewer == null) {
            return;
        }
        PlayerTeam blueTeam = duelGlowTeam(viewerId, true);
        PlayerTeam orangeTeam = duelGlowTeam(viewerId, false);
        if (previousBlue != null) {
            for (String entry : previousBlue) {
                sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(blueTeam, entry, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
        }
        if (previousOrange != null) {
            for (String entry : previousOrange) {
                sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(orangeTeam, entry, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
        }
        sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(blueTeam));
        sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createRemovePacket(orangeTeam));
    }

    private static Set<String> collectGlowEntries(MinecraftServer server, Set<UUID> ids) {
        Set<String> desired = new HashSet<>();
        if (ids == null) {
            return desired;
        }
        for (UUID id : ids) {
            LivingEntity living = findLoadedLivingParticipant(server, id);
            if (living == null || !living.isAlive()) {
                continue;
            }
            // Players use their profile name as the scoreboard entry; non-player
            // entities use their UUID. Asking the entity avoids a team that exists
            // client-side but never actually contains player participants.
            desired.add(living.getScoreboardName());
        }
        return desired;
    }

    private static void syncViewerGlowEntries(ServerPlayer viewer, PlayerTeam team, Set<String> previous, Set<String> desired) {
        Set<String> prev = new HashSet<>(previous == null ? Set.of() : previous);
        for (String entry : prev) {
            if (!desired.contains(entry)) {
                sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(team, entry, ClientboundSetPlayerTeamPacket.Action.REMOVE));
            }
        }
        for (String entry : desired) {
            if (!prev.contains(entry)) {
                sendClientPacket(viewer, ClientboundSetPlayerTeamPacket.createPlayerPacket(team, entry, ClientboundSetPlayerTeamPacket.Action.ADD));
            }
        }
    }

    private static void sendClientPacket(ServerPlayer player, Packet<?> packet) {
        if (player == null || packet == null || player.connection == null) {
            return;
        }
        player.connection.send(packet);
    }

    private static PlayerTeam duelGlowTeam(UUID viewerId, boolean teamA) {
        Scoreboard scoreboard = new Scoreboard();
        PlayerTeam team = new PlayerTeam(scoreboard, duelGlowTeamName(viewerId, teamA));
        team.setColor(teamA ? ChatFormatting.BLUE : ChatFormatting.RED);
        team.setAllowFriendlyFire(true);
        return team;
    }

    private static String duelGlowTeamName(UUID viewerId, boolean teamA) {
        String compact = viewerId == null ? "viewer" : viewerId.toString().replace("-", "");
        return (teamA ? "tldga" : "tldgb") + compact.substring(0, Math.min(11, compact.length()));
    }

    private static void refreshStoredDuelSnapshotsFromRegistry(DuelBattle battle) {
        if (battle == null || battle.tameSnapshots.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, CompoundTag> entry : battle.tameSnapshots.entrySet()) {
            UUID participantId = entry.getKey();
            CompoundTag snapshotTag = entry.getValue();
            if (participantId == null || snapshotTag == null || snapshotTag.isEmpty()) {
                continue;
            }
            TameData persisted = TameRegistry.get(participantId);
            if (persisted == null) {
                persisted = TameRegistry.getByTlId(participantId);
            }
            if (persisted == null) {
                continue;
            }
            TameData snapshot = TameData.fromTag(snapshotTag.copy());
            TameDuelSnapshots.copyPersistentStats(persisted, snapshot);
            entry.setValue(snapshot.toTag());
        }
    }

    private static void clearTargetForParticipant(MinecraftServer server, UUID participantId) {
        LivingEntity tame = findLoadedTame(server, participantId);
        if (tame == null) return;
        clearDuelCombatTarget(tame);
        removeDuelFollowRangeBoost(tame);
        restoreOwnerFollowGoals(tame);
        if (tame instanceof TamableAnimal tamable) restoreMossyGolemTargetGoalsIfNeeded(tamable);
    }

    private static void suppressOwnerFollowGoals(LivingEntity tame) {
        if (!(tame instanceof net.minecraft.world.entity.Mob mob)
                || DUEL_FOLLOW_GOAL_BACKUPS.containsKey(tame.getUUID())) {
            return;
        }
        List<GoalSnapshotEntry> removed = new ArrayList<>();
        for (WrappedGoal wrapped : new ArrayList<>(mob.goalSelector.getAvailableGoals())) {
            Goal goal = wrapped.getGoal();
            if (goal == null || !isOwnerFollowGoal(goal)) continue;
            removed.add(new GoalSnapshotEntry(wrapped.getPriority(), goal));
            mob.goalSelector.removeGoal(goal);
        }
        if (!removed.isEmpty()) {
            DUEL_FOLLOW_GOAL_BACKUPS.put(tame.getUUID(), removed);
        }
    }

    private static void restoreOwnerFollowGoals(LivingEntity tame) {
        if (!(tame instanceof net.minecraft.world.entity.Mob mob)) return;
        List<GoalSnapshotEntry> removed = DUEL_FOLLOW_GOAL_BACKUPS.remove(tame.getUUID());
        if (removed == null) return;
        for (GoalSnapshotEntry entry : removed) {
            if (entry != null && entry.goal != null) {
                mob.goalSelector.addGoal(Math.max(0, entry.priority), entry.goal);
            }
        }
    }

    private static boolean isOwnerFollowGoal(Goal goal) {
        String name = goal.getClass().getName().toLowerCase(java.util.Locale.ROOT)
                .replace("_", "").replace("$", "");
        return name.contains("followowner") || name.contains("ownerfollow");
    }

    private static void setDuelCombatTarget(LivingEntity tame, LivingEntity target) {
        if (tame == null) {
            return;
        }
        if (target == null || !target.isAlive()) {
            clearDuelCombatTarget(tame);
            return;
        }
        if (tame instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != target) {
            mob.setTarget(target);
            // Some modded animals only expose HurtByTargetGoal. This starts that
            // native combat path without applying damage to the tame.
            tame.setLastHurtByMob(target);
        }
        maintainMossyGolemDuelAttack(tame, target);
        try {
            tame.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, target.getUUID(), 600L);
            tame.getBrain().setMemoryWithExpiry(MemoryModuleType.ATTACK_TARGET, target, 600L);
        } catch (Throwable ignored) {
        }
    }

    private static void maintainMossyGolemDuelAttack(LivingEntity tame, LivingEntity target) {
        if (!(tame instanceof TamableAnimal tamable)
                || !isLegendaryMonstersMossyGolem(tamable)
                || target == null
                || !target.isAlive()
                || !areDuelOpponents(tame.getUUID(), target.getUUID())) {
            return;
        }
        forceMossyGolemCombatCommand(tamable);
        try {
            int attackState = (int) tame.getClass().getMethod("getAttackState").invoke(tame);
            int attackTicks = (int) tame.getClass().getMethod("getAttackTicks").invoke(tame);
            // Its mod attack goal can be displaced after the first animation, leaving state 2
            // running forever and preventing canUse() from starting another attack.
            if (attackState == 2 && attackTicks >= 35) {
                tame.getClass().getMethod("setAttackState", int.class).invoke(tame, 0);
                attackState = 0;
            }
            if (attackState != 0) {
                return;
            }
            if (tame.distanceTo(target) < 3.0F) {
                if (tame instanceof net.minecraft.world.entity.Mob mob) {
                    mob.getNavigation().stop();
                }
                tame.getClass().getMethod("setAttackState", int.class).invoke(tame, 2);
            } else if (tame instanceof net.minecraft.world.entity.Mob mob) {
                mob.getNavigation().moveTo(target, 1.0D);
            }
        } catch (ReflectiveOperationException ignored) {
            // Optional-mod compatibility: leave the original goal untouched if its API changes.
        }
    }

    private static void clearDuelCombatTarget(LivingEntity tame) {
        if (tame == null) {
            return;
        }
        if (tame instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        tame.setLastHurtByMob(null);
        try {
            tame.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
            tame.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        } catch (Throwable ignored) {
        }
    }

    private static void applyDuelFollowRangeBoost(LivingEntity tame) {
        if (tame == null) {
            return;
        }
        AttributeInstance follow = tame.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow == null || follow.getModifier(DUEL_FOLLOW_RANGE_MOD) != null) {
            return;
        }
        follow.addTransientModifier(new AttributeModifier(
                DUEL_FOLLOW_RANGE_MOD,
                "tl_duel_follow_range",
                DUEL_FOLLOW_RANGE_BONUS,
                AttributeModifier.Operation.ADDITION
        ));
    }

    private static void removeDuelFollowRangeBoost(LivingEntity tame) {
        if (tame == null) {
            return;
        }
        AttributeInstance follow = tame.getAttribute(Attributes.FOLLOW_RANGE);
        if (follow != null) {
            follow.removeModifier(DUEL_FOLLOW_RANGE_MOD);
        }
    }

    private static void notifyOwner(MinecraftServer server, DuelBattle battle, UUID ownerId, List<Component> resultSummary, List<Component> leaderboardSummary) {
        if (server == null || battle == null || ownerId == null) return;
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            if (PlayerDebugSettings.duelSummaryMessages(ownerId, battle.ranked)) {
                for (Component line : resultSummary) {
                    owner.sendSystemMessage(line);
                }
            }
            if (PlayerDebugSettings.duelResultMessages(ownerId, battle.ranked)) {
                for (Component line : leaderboardSummary) {
                    owner.sendSystemMessage(line);
                }
            }
        }
    }

    private static void notifyBattleAudience(MinecraftServer server, DuelBattle battle, List<Component> resultSummary, List<Component> leaderboardSummary) {
        if (server == null || battle == null) {
            return;
        }
        Set<UUID> recipients = new HashSet<>();
        if (battle.ownerA != null) {
            recipients.add(battle.ownerA);
        }
        if (battle.ownerB != null) {
            recipients.add(battle.ownerB);
        }
        recipients.addAll(battle.spectatorIds);
        if (battle.broadcastToServer) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                recipients.add(player.getUUID());
            }
        }
        for (UUID recipientId : recipients) {
            notifyOwner(server, battle, recipientId, resultSummary, leaderboardSummary);
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
            SummaryParticipant participant = participantForSummary(server, battle, participantId);
            if (participant == null) {
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
            SummaryParticipant left = participantForSummary(server, battle, a);
            SummaryParticipant right = participantForSummary(server, battle, b);
            int leftLevel = left == null ? 1 : left.level();
            int rightLevel = right == null ? 1 : right.level();
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
        UUID killerOwner = participantOwner(battle, elimination.killerId);
        UUID victimOwner = participantOwner(battle, elimination.victimId);
        sendBattleMessage(server, battle, elimination, killerOwner != null && killerOwner.equals(victimOwner));
    }

    private static void sendBattleMessage(MinecraftServer server, DuelBattle battle, DuelElimination elimination, boolean positive) {
        if (server == null || battle == null || elimination == null) {
            return;
        }
        Set<UUID> recipients = new HashSet<>();
        if (battle.ownerA != null) {
            recipients.add(battle.ownerA);
        }
        if (battle.ownerB != null) {
            recipients.add(battle.ownerB);
        }
        recipients.addAll(battle.spectatorIds);
        if (battle.broadcastToServer) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                recipients.add(player.getUUID());
            }
        }
        for (UUID recipientId : recipients) {
            ServerPlayer player = server.getPlayerList().getPlayer(recipientId);
            if (player != null) {
                if (!PlayerDebugSettings.duelKillNotifications(recipientId, battle.ranked)) {
                    continue;
                }
                Component line = eliminationLine(server, battle, elimination, positive, PlayerDebugSettings.duelAssistMessages(recipientId));
                if (line == null) {
                    continue;
                }
                player.sendSystemMessage(line);
            }
        }
    }

    private static Component eliminationLine(MinecraftServer server, DuelBattle battle, DuelElimination elimination, boolean positive, boolean includeAssists) {
        if (elimination == null || elimination.victimId == null) {
            return null;
        }
        ChatFormatting prefixColor = battle != null && battle.teamColoredKillMessages && elimination.killerId != null
                ? participantDisplayColor(battle, elimination.killerId, positive ? ChatFormatting.BLUE : ChatFormatting.YELLOW)
                : positive ? ChatFormatting.BLUE : ChatFormatting.YELLOW;
        MutableComponent line = Component.literal("Duel: ").withStyle(prefixColor);
        if (elimination.killerId != null) {
            line.append(Component.literal(entityLabel(server, elimination.killerId)).withStyle(participantNameColor(battle, elimination.killerId, ChatFormatting.GREEN)))
                    .append(Component.literal(" killed ").withStyle(ChatFormatting.WHITE));
        } else {
            line.append(Component.literal("A tame killed ").withStyle(ChatFormatting.WHITE));
        }
        line.append(Component.literal(entityLabel(server, elimination.victimId)).withStyle(participantNameColor(battle, elimination.victimId, ChatFormatting.RED)));
        if (includeAssists && elimination.assisterIds != null && !elimination.assisterIds.isEmpty()) {
            line.append(Component.literal(" assists: ").withStyle(ChatFormatting.WHITE));
            for (int i = 0; i < elimination.assisterIds.size(); i++) {
                if (i > 0) {
                    line.append(Component.literal(", ").withStyle(ChatFormatting.WHITE));
                }
                UUID assisterId = elimination.assisterIds.get(i);
                line.append(Component.literal(entityLabel(server, assisterId)).withStyle(participantNameColor(battle, assisterId, ChatFormatting.GRAY)));
            }
        }
        return line;
    }

    private static ChatFormatting participantNameColor(DuelBattle battle, UUID participantId, ChatFormatting fallback) {
        if (battle == null || !battle.teamColoredKillMessages) {
            return fallback;
        }
        return participantDisplayColor(battle, participantId, fallback);
    }

    private static ChatFormatting participantDisplayColor(DuelBattle battle, UUID participantId, ChatFormatting fallback) {
        Boolean teamA = participantTeamA(battle, participantId);
        if (teamA == null) {
            return fallback;
        }
        return teamA ? ChatFormatting.AQUA : ChatFormatting.RED;
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
        if (data == null) {
            data = TameRegistry.getByTlId(participantId);
        }
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
        Map<UUID, Integer> mmrDeltas = calculateBattleMmrDeltas(server, battle, null);
        List<UUID> ordered = new ArrayList<>();
        for (UUID participantId : battle.roster) {
            if (participantForSummary(server, battle, participantId) != null) {
                ordered.add(participantId);
            }
        }
        ordered.sort((a, b) -> compareBattlePlacement(server, battle, a, b));

        lines.add(Component.literal("_____duel results_____").withStyle(ChatFormatting.GOLD));
        int rank = 1;
        for (UUID participantId : ordered) {
            SummaryParticipant participant = participantForSummary(server, battle, participantId);
            if (participant == null) {
                continue;
            }
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            double resultPoints = duelResultPoints(stats);
            boolean died = stats.deaths > 0;
            LivingEntity currentEntity = died ? null : findLoadedLivingParticipant(server, participantId);
            boolean teamAEntry = battle.originalTeamA.contains(participantId);
            ChatFormatting rankColor = teamAEntry ? ChatFormatting.AQUA : ChatFormatting.RED;
            MutableComponent row = Component.literal(rank + ". ").withStyle(rankColor)
                    .append(Component.literal("(" + ownerInitials(server, participant.ownerId()) + ") ").withStyle(ChatFormatting.GRAY));
            if (participant.player()) {
                row = row.append(Component.literal("[P] ").withStyle(ChatFormatting.BLUE));
            } else {
                row = row.append(Component.literal("[" + participant.level() + "] ").withStyle(ChatFormatting.YELLOW));
            }
            row = row.append(Component.literal(participant.displayName() + " ").withStyle(died ? ChatFormatting.DARK_RED : ChatFormatting.AQUA));
            if (!died && currentEntity != null) {
                row = row.append(Component.literal("(" + formatHealth(currentEntity) + ") ").withStyle(ChatFormatting.RED));
            }
            row = row
                    .append(Component.literal("(").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("p:" + formatPoints(resultPoints)).withStyle(ChatFormatting.LIGHT_PURPLE))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("k:" + stats.kills).withStyle(ChatFormatting.RED))
                    .append(Component.literal("/").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal("a:" + stats.assists).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(" mmr:").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(formatSignedMmrDelta(mmrDeltas.getOrDefault(participantId, 0))).withStyle(mmrDeltas.getOrDefault(participantId, 0) >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
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
        if (teamA.totalMmr != teamB.totalMmr) {
            return teamA.totalMmr >= teamB.totalMmr ? teamA : teamB;
        }
        return teamA;
    }

    private static MutableComponent buildWinnerLoserLine(String label, TeamResult team, boolean winner) {
        MutableComponent line = Component.literal(label + ": ").withStyle(winner ? ChatFormatting.GREEN : ChatFormatting.RED)
                .append(Component.literal(team.displayName).withStyle(winner ? ChatFormatting.AQUA : ChatFormatting.GRAY))
                .append(Component.literal(" participants Count:").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(team.participantCount)).withStyle(winner ? ChatFormatting.YELLOW : ChatFormatting.RED))
                .append(Component.literal(" total mmr:").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.valueOf(team.totalMmr)).withStyle(ChatFormatting.YELLOW));
        if (winner) {
            line.append(Component.literal(" Survived:").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(team.survived)).withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(" Died:").withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(String.valueOf(team.died)).withStyle(ChatFormatting.RED));
        }
        return line;
    }

    private static TeamResult summarizeTeam(MinecraftServer server, DuelBattle battle, Set<UUID> teamIds, UUID ownerId, String fallbackName) {
        int participantCount = 0;
        int totalMmr = 0;
        int survived = 0;
        int died = 0;
        UUID highestId = null;
        int highestLevel = Integer.MIN_VALUE;
        for (UUID participantId : teamIds) {
            SummaryParticipant participant = participantForSummary(server, battle, participantId);
            if (participant == null) {
                continue;
            }
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            if (stats.deaths > 0) {
                died++;
            } else {
                survived++;
            }
            participantCount++;
            int evaluation = participantStoredDuelMmr(participantId);
            totalMmr += Math.max(1, evaluation);
            if (!participant.player() && evaluation > highestLevel) {
                highestLevel = evaluation;
                highestId = participantId;
            }
        }
        String displayName = teamDisplayName(server, battle, ownerId, highestId, fallbackName);
        return new TeamResult(ownerId, displayName, participantCount, totalMmr, survived, died);
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

    private record TeamResult(UUID ownerId, String displayName, int participantCount, int totalMmr, int survived, int died) {
    }

    private record TeamBossStats(int total, int alive, int dead, List<UUID> top) {
    }

    private record SummaryParticipant(String displayName, UUID ownerId, int level, boolean player) {
    }

    private static SummaryParticipant participantForSummary(MinecraftServer server, DuelBattle battle, UUID participantId) {
        if (participantId == null) {
            return null;
        }
        ServerPlayer player = server == null ? null : server.getPlayerList().getPlayer(participantId);
        if (player != null) {
            return new SummaryParticipant(player.getName().getString(), player.getUUID(), Math.max(1, player.experienceLevel), true);
        }
        TameData live = TameRegistry.get(participantId);
        if (live == null) {
            live = TameRegistry.getByTlId(participantId);
        }
        if (live != null) {
            String displayName = live.name == null || live.name.isBlank() ? entityLabel(server, participantId) : live.name;
            return new SummaryParticipant(displayName, live.ownerUUID, Math.max(1, live.level), false);
        }
        CompoundTag snapshot = battle == null ? null : battle.tameSnapshots.get(participantId);
        if (snapshot != null && !snapshot.isEmpty()) {
            TameData data = TameData.fromTag(snapshot.copy());
            String displayName = data.name == null || data.name.isBlank() ? entityLabel(server, participantId) : data.name;
            return new SummaryParticipant(displayName, data.ownerUUID, Math.max(1, data.level), false);
        }
        LivingEntity loaded = findLoadedLivingParticipant(server, participantId);
        if (loaded instanceof ServerPlayer loadedPlayer) {
            return new SummaryParticipant(loadedPlayer.getName().getString(), loadedPlayer.getUUID(), Math.max(1, loadedPlayer.experienceLevel), true);
        }
        if (loaded != null) {
            return new SummaryParticipant(entityLabel(server, participantId), participantOwner(battle, participantId), 1, false);
        }
        return null;
    }

    private static TameData tameDataForSummary(MinecraftServer server, DuelBattle battle, UUID participantId) {
        TameData live = TameRegistry.get(participantId);
        if (live == null) {
            live = TameRegistry.getByTlId(participantId);
        }
        if (live != null) {
            return live;
        }
        CompoundTag snapshot = battle.tameSnapshots.get(participantId);
        if (snapshot != null && !snapshot.isEmpty()) {
            return TameData.fromTag(snapshot.copy());
        }
        return null;
    }

    private static void capturePreDuelParticipantState(MinecraftServer server, DuelBattle battle, UUID participantId) {
        if (server == null || battle == null || participantId == null) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(participantId);
        if (player != null) {
            CompoundTag playerSnapshot = new CompoundTag();
            player.saveWithoutId(playerSnapshot);
            battle.playerSnapshots.put(participantId, playerSnapshot);
            battle.duelStats.putIfAbsent(participantId, new DuelStats());
            return;
        }
        TameData data = TameRegistry.get(participantId);
        if (data == null) {
            data = TameRegistry.getByTlId(participantId);
        }
        if (data == null) {
            return;
        }
        TameCommands.cancelPendingImmediateChunkTeleport(server, data);
        TameData snapshot = TameData.fromTag(data.toTag().copy());
        LivingEntity loaded = findLoadedLivingParticipant(server, participantId);
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
        if (data == null) {
            data = TameRegistry.getByTlId(entityId);
        }
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

    private static int compareBattlePlacement(MinecraftServer server, DuelBattle battle, UUID leftId, UUID rightId) {
        DuelStats left = battle.duelStats.getOrDefault(leftId, new DuelStats());
        DuelStats right = battle.duelStats.getOrDefault(rightId, new DuelStats());
        int scoreCompare = Double.compare(duelResultPoints(right), duelResultPoints(left));
        if (scoreCompare != 0) {
            return scoreCompare;
        }
        int killsCompare = Integer.compare(right.kills, left.kills);
        if (killsCompare != 0) {
            return killsCompare;
        }
        int assistsCompare = Integer.compare(right.assists, left.assists);
        if (assistsCompare != 0) {
            return assistsCompare;
        }
        return entityLabel(server, leftId).compareToIgnoreCase(entityLabel(server, rightId));
    }

    private static double duelResultPoints(DuelStats stats) {
        if (stats == null) {
            return 0.0D;
        }
        double adjusted = Math.max(0.0D, stats.points);
        if (stats.deaths > 0) {
            adjusted *= 0.7D;
        }
        return adjusted;
    }

    private static void persistBattleStatsAndMmr(MinecraftServer server, DuelBattle battle, UUID forfeitingOwner) {
        if (server == null || battle == null || battle.roster.isEmpty()) {
            return;
        }
        Map<UUID, Integer> deltas = calculateBattleMmrDeltas(server, battle, forfeitingOwner);
        if (deltas.isEmpty()) {
            return;
        }
        boolean teamAWon = didTeamWin(server, battle, true, forfeitingOwner);
        int poolMagnitude = computeTeamMmrPool(server, battle, teamAWon);

        for (UUID participantId : battle.roster) {
            DuelStats stats = battle.duelStats.getOrDefault(participantId, new DuelStats());
            int mmrDelta = deltas.getOrDefault(participantId, 0);
            boolean won = battle.originalTeamA.contains(participantId) ? teamAWon : !teamAWon;
            applyPersistentParticipantStats(server, battle, participantId, stats, mmrDelta, won, poolMagnitude);
        }
        TameRegistry.markDirty();
    }

    private static Map<UUID, Integer> calculateBattleMmrDeltas(MinecraftServer server, DuelBattle battle, UUID forfeitingOwner) {
        Map<UUID, Integer> deltas = new HashMap<>();
        if (server == null || battle == null || battle.roster.isEmpty()) {
            return deltas;
        }
        List<UUID> placements = new ArrayList<>(battle.roster);
        placements.removeIf(id -> participantForSummary(server, battle, id) == null);
        placements.sort((a, b) -> compareBattlePlacement(server, battle, a, b));
        if (placements.isEmpty()) {
            return deltas;
        }
        Map<UUID, Integer> placementIndex = new HashMap<>();
        for (int i = 0; i < placements.size(); i++) {
            placementIndex.put(placements.get(i), i + 1);
        }
        boolean teamAWon = didTeamWin(server, battle, true, forfeitingOwner);
        int poolMagnitude = computeTeamMmrPool(server, battle, teamAWon);
        distributeTeamMmr(server, battle, battle.originalTeamA, placementIndex, placements.size(), teamAWon, poolMagnitude, deltas);
        distributeTeamMmr(server, battle, battle.originalTeamB, placementIndex, placements.size(), !teamAWon, poolMagnitude, deltas);
        return deltas;
    }

    private static String formatSignedMmrDelta(int delta) {
        return (delta >= 0 ? "+" : "") + delta;
    }

    private static boolean didTeamWin(MinecraftServer server, DuelBattle battle, boolean teamA, UUID forfeitingOwner) {
        if (battle == null) {
            return false;
        }
        UUID ownerId = teamA ? battle.ownerA : battle.ownerB;
        UUID otherOwnerId = teamA ? battle.ownerB : battle.ownerA;
        if (forfeitingOwner != null) {
            if (forfeitingOwner.equals(ownerId) && !forfeitingOwner.equals(otherOwnerId)) {
                return false;
            }
            if (forfeitingOwner.equals(otherOwnerId) && !forfeitingOwner.equals(ownerId)) {
                return true;
            }
        }
        if (battle.teamA.isEmpty() != battle.teamB.isEmpty()) {
            return teamA ? !battle.teamA.isEmpty() : !battle.teamB.isEmpty();
        }
        TeamResult teamAResult = summarizeTeam(server, battle, battle.originalTeamA, battle.ownerA, "A");
        TeamResult teamBResult = summarizeTeam(server, battle, battle.originalTeamB, battle.ownerB, "B");
        TeamResult winner = determineWinningTeam(teamAResult, teamBResult, forfeitingOwner);
        return teamA ? winner == teamAResult : winner == teamBResult;
    }

    private static int computeTeamMmrPool(MinecraftServer server, DuelBattle battle, boolean teamAWon) {
        if (battle == null) {
            return 0;
        }
        double teamATotal = teamMmrTotal(server, battle, battle.originalTeamA);
        double teamBTotal = teamMmrTotal(server, battle, battle.originalTeamB);
        double expectedA = expectedTeamScore(teamATotal, teamBTotal);
        double actualA = teamAWon ? 1.0D : 0.0D;
        double swing = Math.abs(actualA - expectedA);
        int participants = Math.max(2, battle.roster.size());
        return Math.max(1, (int) Math.round(DUEL_MMR_K * swing * (participants / 2.0D)));
    }

    private static double teamMmrTotal(MinecraftServer server, DuelBattle battle, Set<UUID> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return 0.0D;
        }
        double total = 0.0D;
        for (UUID participantId : participantIds) {
            total += participantCurrentMmr(server, battle, participantId);
        }
        return total;
    }

    private static double expectedTeamScore(double ownMmr, double otherMmr) {
        return 1.0D / (1.0D + Math.pow(10.0D, (otherMmr - ownMmr) / 400.0D));
    }

    private static int participantCurrentMmr(MinecraftServer server, DuelBattle battle, UUID participantId) {
        if (participantId == null) {
            return PlayerDuelStats.DEFAULT_MMR;
        }
        TameData tame = TameRegistry.get(participantId);
        if (tame != null) {
            return Math.max(0, tame.duelMmr);
        }
        ServerPlayer livePlayer = server == null ? null : server.getPlayerList().getPlayer(participantId);
        if (livePlayer != null) {
            PlayerDuelStats playerStats = TameRegistry.getPlayerDuelStats().get(participantId);
            if (playerStats != null) {
                return Math.max(0, playerStats.duelMmr);
            }
        }
        CompoundTag snapshot = battle == null ? null : battle.playerSnapshots.get(participantId);
        if (snapshot != null && !snapshot.isEmpty()) {
            PlayerDuelStats playerStats = TameRegistry.getPlayerDuelStats().get(participantId);
            if (playerStats != null) {
                return Math.max(0, playerStats.duelMmr);
            }
        }
        PlayerDuelStats player = TameRegistry.getPlayerDuelStats().get(participantId);
        if (player != null) {
            return Math.max(0, player.duelMmr);
        }
        return PlayerDuelStats.DEFAULT_MMR;
    }

    private static int duelEvaluationForParticipant(MinecraftServer server, DuelBattle battle, UUID participantId) {
        return Math.max(1, participantStoredDuelMmr(participantId));
    }

    private static int participantStoredDuelMmr(UUID participantId) {
        if (participantId == null) {
            return 0;
        }
        TameData tame = TameRegistry.get(participantId);
        if (tame != null) {
            return Math.max(0, tame.duelMmr);
        }
        PlayerDuelStats player = TameRegistry.getPlayerDuelStats().get(participantId);
        if (player != null) {
            return Math.max(0, player.duelMmr);
        }
        return 0;
    }

    private static int playerMmrWeightFromSnapshot(CompoundTag snapshot) {
        return playerMmrWeightFromStats(
                readPlayerAttribute(snapshot, "minecraft:generic.max_health", 20.0D),
                readPlayerAttribute(snapshot, "minecraft:generic.armor", 0.0D),
                readPlayerAttribute(snapshot, "minecraft:generic.armor_toughness", 0.0D)
        );
    }

    private static int playerMmrWeightFromStats(double maxHealth, double armor, double armorToughness) {
        double clampedHealth = Math.max(1.0D, maxHealth);
        double clampedArmor = Math.max(0.0D, armor);
        double clampedToughness = Math.max(0.0D, armorToughness);
        return Math.max(1, (int) Math.round((clampedHealth * 2.0D) + ((clampedArmor + clampedToughness) * 2.0D)));
    }

    private static double readPlayerAttribute(CompoundTag snapshot, String attributeName, double fallback) {
        if (snapshot != null && snapshot.contains("Attributes", 9)) {
            var attributes = snapshot.getList("Attributes", 10);
            for (int i = 0; i < attributes.size(); i++) {
                CompoundTag row = attributes.getCompound(i);
                if (!row.contains("Name", 8)) {
                    continue;
                }
                String name = row.getString("Name");
                String shortName = attributeName.startsWith("minecraft:") ? attributeName.substring("minecraft:".length()) : attributeName;
                if (!attributeName.equals(name) && !shortName.equals(name)) {
                    continue;
                }
                if (row.contains("Base", 99)) {
                    return row.getDouble("Base");
                }
            }
        }
        return fallback;
    }

    private static void distributeTeamMmr(MinecraftServer server, DuelBattle battle, Set<UUID> teamIds, Map<UUID, Integer> placementIndex, int participantCount, boolean won, int poolMagnitude, Map<UUID, Integer> deltas) {
        if (teamIds == null || teamIds.isEmpty() || poolMagnitude <= 0) {
            return;
        }
        List<UUID> ordered = new ArrayList<>();
        for (UUID participantId : teamIds) {
            if (placementIndex.containsKey(participantId)) {
                ordered.add(participantId);
            }
        }
        if (ordered.isEmpty()) {
            return;
        }
        double totalWeight = 0.0D;
        for (UUID participantId : ordered) {
            totalWeight += placementWeight(placementIndex.getOrDefault(participantId, participantCount), participantCount, won);
        }
        if (totalWeight <= 0.0D) {
            return;
        }
        int signedPool = won ? poolMagnitude : -poolMagnitude;
        int applied = 0;
        for (int i = 0; i < ordered.size(); i++) {
            UUID participantId = ordered.get(i);
            double weight = placementWeight(placementIndex.getOrDefault(participantId, participantCount), participantCount, won);
            int share = (i == ordered.size() - 1)
                    ? (signedPool - applied)
                    : (int) Math.round((signedPool * weight) / totalWeight);
            applied += share;
            deltas.merge(participantId, share, Integer::sum);
        }
    }

    private static double placementWeight(int placement, int participantCount, boolean won) {
        int normalizedPlacement = Math.max(1, Math.min(participantCount, placement));
        return won ? (participantCount - normalizedPlacement + 1) : normalizedPlacement;
    }

    private static void applyPersistentParticipantStats(MinecraftServer server, DuelBattle battle, UUID participantId, DuelStats stats, int mmrDelta, boolean won, int poolMagnitude) {
        if (participantId == null || stats == null) {
            return;
        }
        TameData tame = TameRegistry.get(participantId);
        if (tame != null) {
            double creditedPoints = Math.max(0.0D, stats.points);
            if (stats.deaths > 0) {
                creditedPoints *= 0.9D;
            }
            tame.duelMmr = tame.duelMmr + mmrDelta;
            tame.duelKills += Math.max(0, stats.kills);
            tame.duelAssists += Math.max(0, stats.assists);
            tame.duelDeaths += Math.max(0, stats.deaths);
            tame.duelWins += won ? 1 : 0;
            tame.duelLosses += won ? 0 : 1;
            tame.duelCount += 1;
            tame.duelPoints += creditedPoints;
            grantDuelXp(server, participantId, tame, mmrDelta, poolMagnitude, won, battle != null && battle.ranked);
            return;
        }
        String resolvedName = entityLabel(server, participantId);
        PlayerDuelStats playerStats = TameRegistry.getOrCreatePlayerDuelStats(participantId, resolvedName);
        if (playerStats == null) {
            return;
        }
        playerStats.duelMmr = playerStats.duelMmr + mmrDelta;
        playerStats.duelKills += Math.max(0, stats.kills);
        playerStats.duelAssists += Math.max(0, stats.assists);
        playerStats.duelDeaths += Math.max(0, stats.deaths);
        playerStats.duelWins += won ? 1 : 0;
        playerStats.duelLosses += won ? 0 : 1;
        playerStats.duelCount += 1;
        playerStats.duelPoints += Math.max(0.0D, stats.points);
    }

    private static void grantDuelXp(MinecraftServer server, UUID participantId, TameData tame, int mmrDelta, int poolMagnitude, boolean won, boolean ranked) {
        if (participantId == null || tame == null) {
            return;
        }
        boolean dailyBonus = false;
        if (ranked && server != null && server.overworld() != null) {
            long day = server.overworld().getGameTime() / 24000L;
            if (tame.rankedDailyBonusDay != day) {
                tame.rankedDailyBonusDay = day;
                tame.rankedDailyMatches = 0;
            }
            dailyBonus = tame.rankedDailyMatches < 3;
            tame.rankedDailyMatches++;
            TameRegistry.markDirty();
        }
        if (!won && !dailyBonus) return;
        int xpReward = duelXpReward(mmrDelta, poolMagnitude);
        if (ranked && !dailyBonus) {
            int rankedTameCount = TameRegistry.getRankedParticipants().size();
            xpReward = Math.min(xpReward, Math.min(rankedTameCount, Math.max(1, tame.level)));
        }
        if (xpReward <= 0) {
            return;
        }
        LivingEntity loadedTame = findLoadedLivingParticipant(server, participantId);
        if (loadedTame != null) {
            LevelSystem.grantXP(loadedTame, tame, xpReward);
            return;
        }
        tame.xp = Math.max(0, tame.xp + TameStoredArmorEvents.repairWithMending(server, null, tame, xpReward));
        tame.xpToNext = Math.max(1, LevelSystem.xpRequiredForLevel(Math.max(1, tame.level)));
        TameRegistry.markDirty();
    }

    private static int duelXpReward(int mmrDelta, int poolMagnitude) {
        int poolBonus = Math.max(0, poolMagnitude) / 10;
        return Math.max(0, mmrDelta) + poolBonus + 3;
    }

    private static void awardDuelPoints(MinecraftServer server, DuelBattle battle, DuelElimination elimination) {
        if (battle == null || elimination == null || elimination.victimId == null) {
            return;
        }
        double pointsPool = Math.max(1.0D, duelEvaluationForParticipant(server, battle, elimination.victimId));
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

    private static LivingEntity findLoadedTame(MinecraftServer server, UUID tameId) {
        LivingEntity entity = findLoadedLivingParticipant(server, tameId);
        return TameEntityAdapter.isTame(entity) ? entity : null;
    }

    public static synchronized boolean hasDuelMovementLock(UUID entityId) {
        UUID participantId = resolveActiveParticipantId(entityId);
        if (participantId == null) {
            return false;
        }
        TameData data = TameRegistry.get(participantId);
        if (data == null) {
            data = TameRegistry.getByTlId(participantId);
        }
        return data != null && data.cooldowns.containsKey(DUEL_MOVEMENT_LOCK_KEY);
    }

    private static void setDuelMovementLock(UUID entityId, boolean locked) {
        if (entityId == null) {
            return;
        }
        TameData data = TameRegistry.get(entityId);
        if (data == null) {
            data = TameRegistry.getByTlId(entityId);
        }
        if (data == null) {
            return;
        }
        if (locked) {
            data.cooldowns.put(DUEL_MOVEMENT_LOCK_KEY, Long.MAX_VALUE);
        } else {
            data.cooldowns.remove(DUEL_MOVEMENT_LOCK_KEY);
        }
        TameRegistry.markDirty();
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
        TameData byTlId = TameRegistry.getByTlId(entityId);
        if (byTlId != null && byTlId.uuid != null) {
            for (var level : server.getAllLevels()) {
                Entity entity = level.getEntity(byTlId.uuid);
                if (entity instanceof LivingEntity living) {
                    return living;
                }
            }
        }
        return null;
    }
}
