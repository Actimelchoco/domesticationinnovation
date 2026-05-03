package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class TameRegistry {
    private static final Pattern LEVEL_PREFIX_PATTERN = Pattern.compile("^\\s*\\[(?:(?:lvl|level)\\s*)?\\d+\\]\\s*", Pattern.CASE_INSENSITIVE);

    public static final Map<UUID, TameData> TAMES = new HashMap<>();
    private static final Map<UUID, TameData> TL_IDS = new HashMap<>();
    private static final Map<UUID, Set<UUID>> OWNER_TO_TAMES = new HashMap<>();
    public static final Map<UUID, TameDeathRecord> LAST_DEATHS = new HashMap<>();
    public static final List<TameDeathRecord> DEATH_HISTORY = new ArrayList<>();
    public static final Set<String> APPROVED_REINCARNATE_ITEMS = new HashSet<>();
    public static final Set<String> CHEAP_APPROVED_REINCARNATE_ITEMS = new HashSet<>();
    private static final Map<UUID, String> OWNER_RESPAWN_ORDERS = new HashMap<>();
    private static final Map<UUID, Boolean> OWNER_AUTO_REINCARNATION = new HashMap<>();
    private static final Map<UUID, Set<String>> OWNER_DO_NOT_ATTACK_TYPES = new HashMap<>();
    private static final Map<UUID, Boolean> OWNER_DO_NOT_ATTACK_ANIMALS = new HashMap<>();
    private static final Map<UUID, Boolean> OWNER_HEALTH_SIPHON = new HashMap<>();
    private static final Map<UUID, Boolean> OWNER_ENTER_PORTALS_BY_THEMSELVES = new HashMap<>();
    private static final Map<UUID, Set<String>> OWNER_GROUPS = new HashMap<>();
    private static final Set<String> INVERTED_CALL_ORDER_TYPE_IDS = new HashSet<>();
    private static final Map<UUID, PlayerDuelStats> PLAYER_DUEL_STATS = new HashMap<>();
    private static final Map<UUID, Integer> OWNER_TELEPORT_APPROVED_CREDITS = new HashMap<>();
    private static String RANKED_ARENA_NAME = "";
    private static final Set<UUID> RANKED_PARTICIPANTS = new LinkedHashSet<>();
    private static final List<CompoundTag> TEMPORARY_TAMES = new ArrayList<>();
    private static TameRegistrySavedData savedData;

    public static void init(MinecraftServer server) {
        if (savedData != null) {
            return;
        }
        ServerLevel overworld = server.overworld();
        savedData = overworld.getDataStorage().computeIfAbsent(
                TameRegistrySavedData::load,
                TameRegistrySavedData::new,
                TameRegistrySavedData.DATA_NAME
        );

        TAMES.clear();
        TAMES.putAll(savedData.getTames());
        boolean changed = false;
        Set<UUID> invalidIds = new HashSet<>();
        Set<UUID> invalidTlIds = new HashSet<>();
        for (TameData data : TAMES.values()) {
            if (data == null) {
                continue;
            }
            if (hasLevelPrefixName(data.name)) {
                if (data.uuid != null) {
                    invalidIds.add(data.uuid);
                }
                if (data.tlId != null) {
                    invalidTlIds.add(data.tlId);
                }
                changed = true;
                continue;
            }
            UUID before = data.tlId;
            data.ensureTlId();
            if (!Objects.equals(before, data.tlId)) {
                changed = true;
            }
        }
        TAMES.entrySet().removeIf(entry -> entry.getValue() == null || hasLevelPrefixName(entry.getValue().name));
        rebuildIndexes();
        LAST_DEATHS.clear();
        LAST_DEATHS.putAll(savedData.getLastDeaths());
        DEATH_HISTORY.clear();
        DEATH_HISTORY.addAll(savedData.getDeathHistory());
        APPROVED_REINCARNATE_ITEMS.clear();
        APPROVED_REINCARNATE_ITEMS.addAll(savedData.getApprovedReincarnateItems());
        CHEAP_APPROVED_REINCARNATE_ITEMS.clear();
        CHEAP_APPROVED_REINCARNATE_ITEMS.addAll(savedData.getCheapApprovedReincarnateItems());
        OWNER_RESPAWN_ORDERS.clear();
        OWNER_RESPAWN_ORDERS.putAll(savedData.getRespawnOrders());
        OWNER_AUTO_REINCARNATION.clear();
        OWNER_AUTO_REINCARNATION.putAll(savedData.getAutoReincarnation());
        OWNER_DO_NOT_ATTACK_TYPES.clear();
        OWNER_DO_NOT_ATTACK_TYPES.putAll(savedData.getDoNotAttackTypes());
        OWNER_DO_NOT_ATTACK_ANIMALS.clear();
        OWNER_DO_NOT_ATTACK_ANIMALS.putAll(savedData.getDoNotAttackAnimals());
        OWNER_HEALTH_SIPHON.clear();
        OWNER_HEALTH_SIPHON.putAll(savedData.getHealthSiphon());
        OWNER_ENTER_PORTALS_BY_THEMSELVES.clear();
        OWNER_ENTER_PORTALS_BY_THEMSELVES.putAll(savedData.getEnterPortalsByThemselves());
        OWNER_GROUPS.clear();
        OWNER_GROUPS.putAll(savedData.getOwnerGroups());
        INVERTED_CALL_ORDER_TYPE_IDS.clear();
        INVERTED_CALL_ORDER_TYPE_IDS.addAll(savedData.getInvertedCallOrderTypeIds());
        PLAYER_DUEL_STATS.clear();
        PLAYER_DUEL_STATS.putAll(savedData.getPlayerDuelStats());
        OWNER_TELEPORT_APPROVED_CREDITS.clear();
        OWNER_TELEPORT_APPROVED_CREDITS.putAll(savedData.getOwnerTeleportApprovedCredits());
        RANKED_ARENA_NAME = savedData.getRankedArenaName();
        RANKED_PARTICIPANTS.clear();
        RANKED_PARTICIPANTS.addAll(savedData.getRankedParticipants());
        TEMPORARY_TAMES.clear();
        TEMPORARY_TAMES.addAll(savedData.getTemporaryTames());
        if (!invalidIds.isEmpty() || !invalidTlIds.isEmpty()) {
            LAST_DEATHS.entrySet().removeIf(entry -> {
                TameDeathRecord record = entry.getValue();
                return record != null && ((record.uuid != null && invalidIds.contains(record.uuid))
                        || (record.tlId != null && invalidTlIds.contains(record.tlId)));
            });
            DEATH_HISTORY.removeIf(record -> record != null && ((record.uuid != null && invalidIds.contains(record.uuid))
                    || (record.tlId != null && invalidTlIds.contains(record.tlId))));
        }
        if (DEATH_HISTORY.isEmpty() && !LAST_DEATHS.isEmpty()) {
            DEATH_HISTORY.addAll(LAST_DEATHS.values());
            changed = true;
        }
        if (changed) {
            markDirty();
        }
    }

    public static void register(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        if (hasLevelPrefixName(data.name)) {
            remove(data.uuid);
            return;
        }
        data.ensureTlId();
        TameData previous = TAMES.put(data.uuid, data);
        if (previous != null) {
            removeFromIndexes(previous);
        }
        addToIndexes(data);
        markDirty();
    }

    public static TameData get(UUID id) {
        return TAMES.get(id);
    }

    public static TameData getByTlId(UUID tlId) {
        if (tlId == null) {
            return null;
        }
        return TL_IDS.get(tlId);
    }

    public static void remove(UUID id) {
        TameData removed = TAMES.remove(id);
        if (removed != null) {
            removeFromIndexes(removed);
            markDirty();
        }
    }

    public static boolean transferOwnership(UUID tameUuid, UUID newOwner) {
        if (tameUuid == null || newOwner == null) {
            return false;
        }
        TameData data = TAMES.get(tameUuid);
        if (data == null) {
            return false;
        }
        UUID oldOwner = data.ownerUUID;
        if (Objects.equals(oldOwner, newOwner)) {
            return false;
        }
        if (oldOwner != null) {
            Set<UUID> oldSet = OWNER_TO_TAMES.get(oldOwner);
            if (oldSet != null) {
                oldSet.remove(tameUuid);
                if (oldSet.isEmpty()) {
                    OWNER_TO_TAMES.remove(oldOwner);
                }
            }
        }
        data.ownerUUID = newOwner;
        OWNER_TO_TAMES.computeIfAbsent(newOwner, k -> new HashSet<>()).add(tameUuid);
        migrateDeathOwnership(tameUuid, oldOwner, newOwner);
        markDirty();
        return true;
    }

    public static List<TameData> getOwned(UUID ownerUuid) {
        if (ownerUuid == null) {
            return List.of();
        }
        Set<UUID> ids = OWNER_TO_TAMES.get(ownerUuid);
        if (ids == null || ids.isEmpty()) {
            List<TameData> fallback = new ArrayList<>();
            for (TameData data : TAMES.values()) {
                if (data == null || data.ownerUUID == null) continue;
                if (!ownerUuid.equals(data.ownerUUID)) continue;
                fallback.add(data);
            }
            return fallback;
        }
        List<TameData> out = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            TameData data = TAMES.get(id);
            if (data != null) {
                out.add(data);
            }
        }
        return out;
    }

    public static TameData getTameByPetBed(String dimensionId, BlockPos bedPos) {
        if (dimensionId == null || dimensionId.isBlank() || bedPos == null) {
            return null;
        }
        for (TameData data : TAMES.values()) {
            if (data == null || !data.hasPetBed) {
                continue;
            }
            if (!dimensionId.equals(data.petBedDimension)) {
                continue;
            }
            if (data.petBedX == bedPos.getX() && data.petBedY == bedPos.getY() && data.petBedZ == bedPos.getZ()) {
                return data;
            }
        }
        return null;
    }

    public static void archiveDeath(TameDeathRecord record) {
        if (record == null || record.uuid == null) {
            return;
        }
        if (record.tlId == null) {
            TameData data = get(record.uuid);
            if (data != null) {
                record.tlId = data.ensureTlId();
            }
        }
        LAST_DEATHS.put(record.uuid, record);
        DEATH_HISTORY.add(record);
        markDirty();
    }

    public static void markDirty() {
        if (savedData == null) {
            return;
        }
        savedData.getTames().clear();
        savedData.getTames().putAll(TAMES);
        savedData.getLastDeaths().clear();
        savedData.getLastDeaths().putAll(LAST_DEATHS);
        savedData.getDeathHistory().clear();
        savedData.getDeathHistory().addAll(DEATH_HISTORY);
        savedData.getApprovedReincarnateItems().clear();
        savedData.getApprovedReincarnateItems().addAll(APPROVED_REINCARNATE_ITEMS);
        savedData.getCheapApprovedReincarnateItems().clear();
        savedData.getCheapApprovedReincarnateItems().addAll(CHEAP_APPROVED_REINCARNATE_ITEMS);
        savedData.setRespawnOrders(OWNER_RESPAWN_ORDERS);
        savedData.setAutoReincarnation(OWNER_AUTO_REINCARNATION);
        savedData.setDoNotAttackTypes(OWNER_DO_NOT_ATTACK_TYPES);
        savedData.setDoNotAttackAnimals(OWNER_DO_NOT_ATTACK_ANIMALS);
        savedData.setHealthSiphon(OWNER_HEALTH_SIPHON);
        savedData.setEnterPortalsByThemselves(OWNER_ENTER_PORTALS_BY_THEMSELVES);
        savedData.setOwnerGroups(OWNER_GROUPS);
        savedData.setInvertedCallOrderTypeIds(INVERTED_CALL_ORDER_TYPE_IDS);
        savedData.setPlayerDuelStats(PLAYER_DUEL_STATS);
        savedData.setOwnerTeleportApprovedCredits(OWNER_TELEPORT_APPROVED_CREDITS);
        savedData.setRankedArenaName(RANKED_ARENA_NAME);
        savedData.setRankedParticipants(RANKED_PARTICIPANTS);
        savedData.setTemporaryTames(TEMPORARY_TAMES);
        savedData.setDirty();
    }

    public static String getRankedArenaName() {
        return RANKED_ARENA_NAME == null ? "" : RANKED_ARENA_NAME;
    }

    public static void setRankedArenaName(String arenaName) {
        String normalized = arenaName == null ? "" : arenaName.trim();
        if (Objects.equals(RANKED_ARENA_NAME, normalized)) {
            return;
        }
        RANKED_ARENA_NAME = normalized;
        markDirty();
    }

    public static Set<UUID> getRankedParticipants() {
        return Set.copyOf(RANKED_PARTICIPANTS);
    }

    public static void setRankedParticipants(Set<UUID> participants) {
        RANKED_PARTICIPANTS.clear();
        if (participants != null) {
            for (UUID participantId : participants) {
                if (participantId != null) {
                    RANKED_PARTICIPANTS.add(participantId);
                }
            }
        }
        markDirty();
    }

    public static List<CompoundTag> getTemporaryTames() {
        List<CompoundTag> out = new ArrayList<>(TEMPORARY_TAMES.size());
        for (CompoundTag row : TEMPORARY_TAMES) {
            if (row != null && !row.isEmpty()) {
                out.add(row.copy());
            }
        }
        return out;
    }

    public static void archiveTemporaryTame(CompoundTag row) {
        if (row == null || row.isEmpty()) {
            return;
        }
        TEMPORARY_TAMES.add(0, row.copy());
        while (TEMPORARY_TAMES.size() > 512) {
            TEMPORARY_TAMES.remove(TEMPORARY_TAMES.size() - 1);
        }
        markDirty();
    }

    public static int getTeleportApprovedCredits(UUID ownerUuid) {
        if (ownerUuid == null) {
            return 0;
        }
        return Math.max(0, OWNER_TELEPORT_APPROVED_CREDITS.getOrDefault(ownerUuid, 0));
    }

    public static void setTeleportApprovedCredits(UUID ownerUuid, int credits) {
        if (ownerUuid == null) {
            return;
        }
        int normalized = Math.max(0, credits);
        if (normalized <= 0) {
            OWNER_TELEPORT_APPROVED_CREDITS.remove(ownerUuid);
        } else {
            OWNER_TELEPORT_APPROVED_CREDITS.put(ownerUuid, normalized);
        }
        markDirty();
    }

    public static PlayerDuelStats getOrCreatePlayerDuelStats(UUID playerUuid, String lastKnownName) {
        if (playerUuid == null) {
            return null;
        }
        PlayerDuelStats stats = PLAYER_DUEL_STATS.computeIfAbsent(playerUuid, ignored -> {
            PlayerDuelStats created = new PlayerDuelStats();
            created.playerUuid = playerUuid;
            return created;
        });
        if (lastKnownName != null && !lastKnownName.isBlank() && !lastKnownName.equals(stats.lastKnownName)) {
            stats.lastKnownName = lastKnownName;
            markDirty();
        }
        return stats;
    }

    public static Map<UUID, PlayerDuelStats> getPlayerDuelStats() {
        return Map.copyOf(PLAYER_DUEL_STATS);
    }

    public static void resetAllDuelStats() {
        for (TameData data : TAMES.values()) {
            if (data == null) {
                continue;
            }
            data.duelMmr = PlayerDuelStats.DEFAULT_MMR;
            data.duelKills = 0;
            data.duelAssists = 0;
            data.duelDeaths = 0;
            data.duelWins = 0;
            data.duelLosses = 0;
            data.duelCount = 0;
            data.duelPoints = 0.0D;
        }
        PLAYER_DUEL_STATS.clear();
        markDirty();
    }

    public static Set<String> getOwnerGroups(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Set.of();
        }
        Set<String> groups = OWNER_GROUPS.get(ownerUuid);
        return groups == null ? Set.of() : Set.copyOf(groups);
    }

    public static void rememberGroup(UUID ownerUuid, String group) {
        if (ownerUuid == null || group == null || group.isBlank()) {
            return;
        }
        OWNER_GROUPS.computeIfAbsent(ownerUuid, ignored -> new LinkedHashSet<>()).add(group.trim());
        markDirty();
    }

    public static void forgetGroup(UUID ownerUuid, String group) {
        if (ownerUuid == null || group == null || group.isBlank()) {
            return;
        }
        Set<String> groups = OWNER_GROUPS.get(ownerUuid);
        if (groups == null) {
            return;
        }
        groups.removeIf(existing -> existing != null && existing.equalsIgnoreCase(group.trim()));
        if (groups.isEmpty()) {
            OWNER_GROUPS.remove(ownerUuid);
        }
        markDirty();
    }

    public static boolean isCallOrderInvertedType(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        String normalized = normalizeTypeId(typeId);
        return usesInvertedCallOrderByDefault(normalized) || INVERTED_CALL_ORDER_TYPE_IDS.contains(normalized);
    }

    public static Set<String> getCallOrderInvertedTypes() {
        return Set.copyOf(INVERTED_CALL_ORDER_TYPE_IDS);
    }

    public static boolean usesInvertedCallOrderByDefault(String typeId) {
        String normalized = normalizeTypeId(typeId);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        int separator = normalized.indexOf(':');
        if (separator < 0) {
            return false;
        }
        return !"minecraft".equals(normalized.substring(0, separator));
    }

    public static boolean toggleCallOrderInvertedType(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        String normalized = normalizeTypeId(typeId);
        boolean enabled;
        if (INVERTED_CALL_ORDER_TYPE_IDS.contains(normalized)) {
            INVERTED_CALL_ORDER_TYPE_IDS.remove(normalized);
            enabled = false;
        } else {
            INVERTED_CALL_ORDER_TYPE_IDS.add(normalized);
            enabled = true;
        }
        markDirty();
        return enabled;
    }

    public static boolean addCallOrderInvertedType(String typeId) {
        String normalized = normalizeTypeId(typeId);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        if (!INVERTED_CALL_ORDER_TYPE_IDS.add(normalized)) {
            return false;
        }
        markDirty();
        return true;
    }

    public static boolean removeCallOrderInvertedType(String typeId) {
        String normalized = normalizeTypeId(typeId);
        if (normalized == null || normalized.isBlank()) {
            return false;
        }
        if (!INVERTED_CALL_ORDER_TYPE_IDS.remove(normalized)) {
            return false;
        }
        markDirty();
        return true;
    }

    private static String normalizeTypeId(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return null;
        }
        String normalized = typeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
        }
        return normalized;
    }

    public static boolean canEnterPortalsByThemselves(UUID ownerUuid) {
        if (ownerUuid == null) {
            return false;
        }
        return OWNER_ENTER_PORTALS_BY_THEMSELVES.getOrDefault(ownerUuid, false);
    }

    public static void setEnterPortalsByThemselves(UUID ownerUuid, boolean enabled) {
        if (ownerUuid == null) {
            return;
        }
        if (enabled) {
            OWNER_ENTER_PORTALS_BY_THEMSELVES.put(ownerUuid, true);
        } else {
            OWNER_ENTER_PORTALS_BY_THEMSELVES.remove(ownerUuid);
        }
        markDirty();
    }

    public static String getRespawnOrder(UUID ownerUuid) {
        if (ownerUuid == null) {
            return "default";
        }
        String order = OWNER_RESPAWN_ORDERS.get(ownerUuid);
        return order == null || order.isBlank() ? "default" : order;
    }

    public static void setRespawnOrder(UUID ownerUuid, String order) {
        if (ownerUuid == null) {
            return;
        }
        String normalized = order == null || order.isBlank() ? "default" : order;
        if ("default".equals(normalized)) {
            OWNER_RESPAWN_ORDERS.remove(ownerUuid);
        } else {
            OWNER_RESPAWN_ORDERS.put(ownerUuid, normalized);
        }
        markDirty();
    }

    public static boolean isAutoReincarnationEnabled(UUID ownerUuid) {
        if (ownerUuid == null) {
            return false;
        }
        return OWNER_AUTO_REINCARNATION.getOrDefault(ownerUuid, false);
    }

    public static void setAutoReincarnation(UUID ownerUuid, boolean enabled) {
        if (ownerUuid == null) {
            return;
        }
        if (enabled) {
            OWNER_AUTO_REINCARNATION.put(ownerUuid, true);
        } else {
            OWNER_AUTO_REINCARNATION.remove(ownerUuid);
        }
        markDirty();
    }

    public static Set<String> getDoNotAttackTypes(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Set.of();
        }
        Set<String> values = OWNER_DO_NOT_ATTACK_TYPES.get(ownerUuid);
        return values == null || values.isEmpty() ? Set.of() : Set.copyOf(values);
    }

    public static boolean toggleDoNotAttackType(UUID ownerUuid, String entityTypeId) {
        if (ownerUuid == null || entityTypeId == null || entityTypeId.isBlank()) {
            return false;
        }
        String normalized = entityTypeId.trim().toLowerCase(java.util.Locale.ROOT);
        Set<String> values = OWNER_DO_NOT_ATTACK_TYPES.computeIfAbsent(ownerUuid, ignored -> new HashSet<>());
        boolean added;
        if (values.contains(normalized)) {
            values.remove(normalized);
            added = false;
        } else {
            values.add(normalized);
            added = true;
        }
        if (values.isEmpty()) {
            OWNER_DO_NOT_ATTACK_TYPES.remove(ownerUuid);
        }
        markDirty();
        return added;
    }

    public static boolean removeDoNotAttackType(UUID ownerUuid, String entityTypeId) {
        if (ownerUuid == null || entityTypeId == null || entityTypeId.isBlank()) {
            return false;
        }
        String normalized = entityTypeId.trim().toLowerCase(java.util.Locale.ROOT);
        Set<String> values = OWNER_DO_NOT_ATTACK_TYPES.get(ownerUuid);
        if (values == null || values.isEmpty() || !values.remove(normalized)) {
            return false;
        }
        if (values.isEmpty()) {
            OWNER_DO_NOT_ATTACK_TYPES.remove(ownerUuid);
        }
        markDirty();
        return true;
    }

    public static boolean isDoNotAttackAnimals(UUID ownerUuid) {
        if (ownerUuid == null) {
            return false;
        }
        return OWNER_DO_NOT_ATTACK_ANIMALS.getOrDefault(ownerUuid, false);
    }

    public static void setDoNotAttackAnimals(UUID ownerUuid, boolean enabled) {
        if (ownerUuid == null) {
            return;
        }
        if (enabled) {
            OWNER_DO_NOT_ATTACK_ANIMALS.put(ownerUuid, true);
        } else {
            OWNER_DO_NOT_ATTACK_ANIMALS.remove(ownerUuid);
        }
        markDirty();
    }

    public static boolean isHealthSiphonEnabled(UUID ownerUuid) {
        if (ownerUuid == null) {
            return true;
        }
        return OWNER_HEALTH_SIPHON.getOrDefault(ownerUuid, true);
    }

    public static void setHealthSiphonEnabled(UUID ownerUuid, boolean enabled) {
        if (ownerUuid == null) {
            return;
        }
        if (enabled) {
            OWNER_HEALTH_SIPHON.remove(ownerUuid);
        } else {
            OWNER_HEALTH_SIPHON.put(ownerUuid, false);
        }
        markDirty();
    }

    public static boolean isProtectedAttackTarget(TamableAnimal tame, Entity target) {
        if (tame == null || target == null) {
            return false;
        }
        TameData data = get(tame.getUUID());
        return data != null && isProtectedAttackTarget(data.ownerUUID, target);
    }

    public static boolean isProtectedAttackTarget(UUID ownerUuid, Entity target) {
        if (target == null) {
            return false;
        }
        if (matchesProtectedTargetRules(ownerUuid, target)) {
            return true;
        }
        UUID claimOwner = OpenPartiesClaimsCompat.getClaimOwner(target);
        return claimOwner != null && !claimOwner.equals(ownerUuid) && matchesProtectedTargetRules(claimOwner, target);
    }

    private static boolean matchesProtectedTargetRules(UUID ownerUuid, Entity target) {
        if (ownerUuid == null || target == null) {
            return false;
        }
        if (isDoNotAttackAnimals(ownerUuid) && target instanceof Animal) {
            return true;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (key == null) {
            return false;
        }
        Set<String> blocked = OWNER_DO_NOT_ATTACK_TYPES.get(ownerUuid);
        return blocked != null && blocked.contains(key.toString().toLowerCase(java.util.Locale.ROOT));
    }

    public static boolean isInitialized() {
        return savedData != null;
    }

    public static boolean hasLevelPrefixName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        return LEVEL_PREFIX_PATTERN.matcher(name).lookingAt();
    }

    public static String stripLevelPrefixes(String name) {
        if (name == null) {
            return "";
        }
        String cleaned = name;
        while (true) {
            String next = LEVEL_PREFIX_PATTERN.matcher(cleaned).replaceFirst("");
            if (next.equals(cleaned)) {
                break;
            }
            cleaned = next;
        }
        return cleaned.trim();
    }

    public static void bindEntityToData(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return;
        }
        data.ensureTlId();
        TameData.syncTlIdToEntity(tame, data.tlId);
    }

    public static void rebindEntityUuid(TameData data, UUID newUuid) {
        if (data == null || newUuid == null) {
            return;
        }
        UUID oldUuid = data.uuid;
        if (Objects.equals(oldUuid, newUuid)) {
            return;
        }
        if (oldUuid != null) {
            TameData previous = TAMES.remove(oldUuid);
            if (previous != null) {
                removeFromIndexes(previous);
            }
        }
        data.uuid = newUuid;
        TAMES.put(newUuid, data);
        addToIndexes(data);
    }

    public static TameDeathRecord getLastDeath(TameData data) {
        if (data == null) {
            return null;
        }
        TameDeathRecord byUuid = data.uuid == null ? null : LAST_DEATHS.get(data.uuid);
        if (byUuid != null) {
            return byUuid;
        }
        UUID tlId = data.tlId;
        if (tlId == null) {
            return null;
        }
        for (TameDeathRecord record : LAST_DEATHS.values()) {
            if (record != null && tlId.equals(record.tlId)) {
                return record;
            }
        }
        return null;
    }

    public static boolean removeLastDeath(TameDeathRecord target) {
        if (target == null) {
            return false;
        }
        boolean removed = false;
        if (target.uuid != null) {
            TameDeathRecord mapped = LAST_DEATHS.get(target.uuid);
            if (mapped == target) {
                LAST_DEATHS.remove(target.uuid);
                removed = true;
            }
        }
        if (!removed && target.tlId != null) {
            int before = LAST_DEATHS.size();
            LAST_DEATHS.entrySet().removeIf(e -> {
                TameDeathRecord record = e.getValue();
                return record == target || (record != null && target.tlId.equals(record.tlId));
            });
            removed = before != LAST_DEATHS.size();
        }
        return removed;
    }

    public static int removeDeathsForIdentity(UUID uuid, UUID tlId) {
        int removed = 0;
        if (uuid != null && LAST_DEATHS.remove(uuid) != null) {
            removed++;
        }
        if (tlId != null) {
            int before = LAST_DEATHS.size();
            LAST_DEATHS.entrySet().removeIf(e -> {
                TameDeathRecord record = e.getValue();
                return record != null && tlId.equals(record.tlId);
            });
            removed += Math.max(0, before - LAST_DEATHS.size());
        }
        return removed;
    }

    private static void rebuildIndexes() {
        TL_IDS.clear();
        OWNER_TO_TAMES.clear();
        for (TameData data : TAMES.values()) {
            addToIndexes(data);
        }
    }

    private static void addToIndexes(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        data.ensureTlId();
        TameData conflictingTlId = TL_IDS.get(data.tlId);
        if (conflictingTlId != null && conflictingTlId != data) {
            data.tlId = UUID.randomUUID();
        }
        TL_IDS.put(data.tlId, data);
        if (data.ownerUUID != null) {
            OWNER_TO_TAMES.computeIfAbsent(data.ownerUUID, k -> new HashSet<>()).add(data.uuid);
        }
    }

    private static void removeFromIndexes(TameData data) {
        if (data == null || data.uuid == null) {
            return;
        }
        if (data.tlId != null) {
            TameData current = TL_IDS.get(data.tlId);
            if (current == data) {
                TL_IDS.remove(data.tlId);
            }
        }
        if (data.ownerUUID == null) {
            return;
        }
        Set<UUID> set = OWNER_TO_TAMES.get(data.ownerUUID);
        if (set == null) {
            return;
        }
        set.remove(data.uuid);
        if (set.isEmpty()) {
            OWNER_TO_TAMES.remove(data.ownerUUID);
        }
    }

    private static void migrateDeathOwnership(UUID tameUuid, UUID oldOwner, UUID newOwner) {
        if (tameUuid == null || newOwner == null) {
            return;
        }
        for (TameDeathRecord record : DEATH_HISTORY) {
            if (record == null || record.uuid == null) continue;
            if (!tameUuid.equals(record.uuid)) continue;
            if (oldOwner != null && !Objects.equals(oldOwner, record.ownerUUID)) continue;
            record.ownerUUID = newOwner;
            if (record.snapshot != null && !record.snapshot.isEmpty()) {
                record.snapshot.putUUID("ownerUUID", newOwner);
            }
        }
        TameDeathRecord last = LAST_DEATHS.get(tameUuid);
        if (last == null) {
            return;
        }
        if (oldOwner == null || Objects.equals(oldOwner, last.ownerUUID)) {
            last.ownerUUID = newOwner;
            if (last.snapshot != null && !last.snapshot.isEmpty()) {
                last.snapshot.putUUID("ownerUUID", newOwner);
            }
        }
    }
}
