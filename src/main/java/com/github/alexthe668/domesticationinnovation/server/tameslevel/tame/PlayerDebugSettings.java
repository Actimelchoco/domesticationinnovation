package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

public final class PlayerDebugSettings {
    public static final int DEFAULT_INVENTORY_SUMMARY_MINUTES = 10;
    public static final int DEFAULT_CHEST_DRUM_BLOCK_RANGE = 20;
    public static final int DEFAULT_CHEST_DRUM_HEIGHT = 2;

    private PlayerDebugSettings() {
    }

    private static final Map<UUID, Boolean> ENEMY_KILLED = new HashMap<>();
    private static final Map<UUID, Boolean> COMBAT_ASSISTS = new HashMap<>();
    private static final Map<UUID, Boolean> COMBAT_KILLS = new HashMap<>();
    private static final Map<UUID, Boolean> COMBAT_DEATH = new HashMap<>();
    private static final Map<UUID, Boolean> CONFLICT_NOTY = new HashMap<>();
    private static final Map<UUID, Boolean> ABILITY_USED = new HashMap<>();
    private static final Map<UUID, Boolean> ATTRIBUTE_USED = new HashMap<>();
    private static final Map<UUID, Boolean> LEVEL_UP = new HashMap<>();
    private static final Map<UUID, Boolean> DAMAGE = new HashMap<>();
    private static final Map<UUID, Boolean> TELEPORT = new HashMap<>();
    private static final Map<UUID, Boolean> SHADOW_HANDS = new HashMap<>();
    private static final Map<UUID, Boolean> INVENTORY_LOW_ON_FOOD = new HashMap<>();
    private static final Map<UUID, Boolean> INVENTORY_NO_FOOD = new HashMap<>();
    private static final Map<UUID, Boolean> INVENTORY_SUMMARY = new HashMap<>();
    private static final Map<UUID, Integer> INVENTORY_SUMMARY_MINUTES = new HashMap<>();
    private static final Map<UUID, List<ChestDrumRange>> CHEST_DRUM_RANGES = new HashMap<>();
    private static final Map<UUID, Set<String>> CHEST_DRUM_PULL_NON_PREFERRED_TYPES = new HashMap<>();
    private static final Map<UUID, Set<String>> CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> DO_NOT_ATTACK_OWNERS = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_ASSIST_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_KILL_NOTIFICATIONS = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_SESSION_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_RESULT_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_SUMMARY_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> RANKED_DUEL_KILL_NOTIFICATIONS = new HashMap<>();
    private static final Map<UUID, Boolean> RANKED_DUEL_START_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> RANKED_DUEL_RESULT_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> RANKED_DUEL_SUMMARY_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> NEW_TAME_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> AUTO_RESPAWN_MESSAGES = new HashMap<>();
    private static final Map<UUID, Boolean> NO_AUTO_SET_BED = new HashMap<>();
    private static final Map<UUID, Boolean> DUEL_GLOW = new HashMap<>();
    private static final Map<UUID, Boolean> FRIENDLY_FIRE = new HashMap<>();
    private static final Map<UUID, Boolean> TAMES_FRIENDLY = new HashMap<>();
    private static final Map<UUID, Boolean> HIDE_LEVEL_IN_NAME = new HashMap<>();
    private static final Map<UUID, Boolean> ENABLE_MENDING = new HashMap<>();
    private static final Map<UUID, Boolean> ENABLE_CHEST_DRUM_FOOD_PREFERENCES = new HashMap<>();

    private record BooleanSetting(String key, Map<UUID, Boolean> values, boolean defaultValue) {
    }

    private static final List<BooleanSetting> BOOLEAN_SETTINGS = List.of(
            new BooleanSetting("enemyKilled", ENEMY_KILLED, false),
            new BooleanSetting("combatAssists", COMBAT_ASSISTS, false),
            new BooleanSetting("combatKills", COMBAT_KILLS, false),
            new BooleanSetting("combatDeath", COMBAT_DEATH, true),
            new BooleanSetting("conflictNoty", CONFLICT_NOTY, true),
            new BooleanSetting("abilityUsed", ABILITY_USED, false),
            new BooleanSetting("attributeUsed", ATTRIBUTE_USED, false),
            new BooleanSetting("levelUp", LEVEL_UP, true),
            new BooleanSetting("damage", DAMAGE, false),
            new BooleanSetting("teleport", TELEPORT, false),
            new BooleanSetting("shadowHands", SHADOW_HANDS, false),
            new BooleanSetting("inventoryLowOnFood", INVENTORY_LOW_ON_FOOD, false),
            new BooleanSetting("inventoryNoFood", INVENTORY_NO_FOOD, false),
            new BooleanSetting("inventorySummary", INVENTORY_SUMMARY, true),
            new BooleanSetting("duelAssistMessages", DUEL_ASSIST_MESSAGES, true),
            new BooleanSetting("duelKillNotifications", DUEL_KILL_NOTIFICATIONS, true),
            new BooleanSetting("duelSessionMessages", DUEL_SESSION_MESSAGES, true),
            new BooleanSetting("duelMessages", DUEL_MESSAGES, true),
            new BooleanSetting("duelResultMessages", DUEL_RESULT_MESSAGES, true),
            new BooleanSetting("duelSummaryMessages", DUEL_SUMMARY_MESSAGES, false),
            new BooleanSetting("rankedDuelKillNotifications", RANKED_DUEL_KILL_NOTIFICATIONS, true),
            new BooleanSetting("rankedDuelStartMessages", RANKED_DUEL_START_MESSAGES, true),
            new BooleanSetting("rankedDuelResultMessages", RANKED_DUEL_RESULT_MESSAGES, true),
            new BooleanSetting("rankedDuelSummaryMessages", RANKED_DUEL_SUMMARY_MESSAGES, false),
            new BooleanSetting("newTameMessages", NEW_TAME_MESSAGES, true),
            new BooleanSetting("autoRespawnMessages", AUTO_RESPAWN_MESSAGES, true),
            new BooleanSetting("noAutoSetBed", NO_AUTO_SET_BED, false),
            new BooleanSetting("friendlyFire", FRIENDLY_FIRE, false),
            new BooleanSetting("tamesFriendly", TAMES_FRIENDLY, true),
            new BooleanSetting("hideLevelInName", HIDE_LEVEL_IN_NAME, false),
            new BooleanSetting("enableChestXDrumFoodPreferences", ENABLE_CHEST_DRUM_FOOD_PREFERENCES, true)
    );

    public static boolean enemyKilled(UUID player) {
        return getBoolean(ENEMY_KILLED, player, false);
    }

    public static boolean enableMending(UUID player) {
        return getBoolean(ENABLE_MENDING, player, true);
    }

    public static boolean enableChestXDrumFoodPreferences(UUID player) {
        return getBoolean(ENABLE_CHEST_DRUM_FOOD_PREFERENCES, player, true);
    }

    public static boolean combatAssists(UUID player) {
        return getBoolean(COMBAT_ASSISTS, player, false);
    }

    public static boolean combatKills(UUID player) {
        return getBoolean(COMBAT_KILLS, player, false);
    }

    public static boolean combatDeath(UUID player) {
        return getBoolean(COMBAT_DEATH, player, true);
    }

    public static boolean conflictNoty(UUID player) {
        return getBoolean(CONFLICT_NOTY, player, true);
    }

    public static boolean abilityUsed(UUID player) {
        return getBoolean(ABILITY_USED, player, false);
    }

    public static boolean attributeUsed(UUID player) {
        return getBoolean(ATTRIBUTE_USED, player, false);
    }

    public static boolean levelUp(UUID player) {
        return getBoolean(LEVEL_UP, player, true);
    }

    public static boolean damage(UUID player) {
        return getBoolean(DAMAGE, player, false);
    }

    public static boolean teleport(UUID player) {
        return getBoolean(TELEPORT, player, false);
    }

    public static boolean shadowHands(UUID player) {
        return getBoolean(SHADOW_HANDS, player, false);
    }

    public static boolean inventoryLowOnFood(UUID player) {
        return getBoolean(INVENTORY_LOW_ON_FOOD, player, false);
    }

    public static boolean inventoryNoFood(UUID player) {
        return getBoolean(INVENTORY_NO_FOOD, player, false);
    }

    public static boolean inventorySummary(UUID player) {
        return getBoolean(INVENTORY_SUMMARY, player, true);
    }

    public static int inventorySummaryMinutes(UUID player) {
        return Math.max(1, INVENTORY_SUMMARY_MINUTES.getOrDefault(player, DEFAULT_INVENTORY_SUMMARY_MINUTES));
    }

    public static long inventorySummaryIntervalTicks(UUID player) {
        return 20L * 60L * inventorySummaryMinutes(player);
    }

    public record ChestDrumRange(String dimension, int x, int y, int z, int blockRange, int height) {
    }

    public static List<ChestDrumRange> chestDrumRanges(UUID player) {
        return player == null ? List.of() : List.copyOf(CHEST_DRUM_RANGES.getOrDefault(player, List.of()));
    }

    public static Set<String> chestDrumPullNonPreferredTypes(UUID player) {
        return player == null ? Set.of() : Set.copyOf(CHEST_DRUM_PULL_NON_PREFERRED_TYPES.getOrDefault(player, Set.of()));
    }

    public static Set<String> chestDrumPullPreferredFoodOfTypes(UUID player) {
        return player == null ? Set.of() : Set.copyOf(CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES.getOrDefault(player, Set.of()));
    }

    public static void setChestDrumPullNonPreferred(UUID player, String type, boolean enabled) {
        setTypeRule(CHEST_DRUM_PULL_NON_PREFERRED_TYPES, player, type, enabled);
    }

    public static void setChestDrumPullPreferredFoodOf(UUID player, String type, boolean enabled) {
        setTypeRule(CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES, player, type, enabled);
    }

    public static Set<UUID> doNotAttackOwners(UUID player) {
        return player == null ? Set.of() : Set.copyOf(DO_NOT_ATTACK_OWNERS.getOrDefault(player, Set.of()));
    }

    public static boolean isDoNotAttackOwner(UUID player, UUID otherOwner) {
        return player != null && otherOwner != null
                && DO_NOT_ATTACK_OWNERS.getOrDefault(player, Set.of()).contains(otherOwner);
    }

    public static void setDoNotAttackOwner(UUID player, UUID otherOwner, boolean protectedOwner) {
        if (player == null || otherOwner == null || player.equals(otherOwner)) return;
        Set<UUID> values = DO_NOT_ATTACK_OWNERS.computeIfAbsent(player, ignored -> new LinkedHashSet<>());
        if (protectedOwner) values.add(otherOwner);
        else values.remove(otherOwner);
        if (values.isEmpty()) DO_NOT_ATTACK_OWNERS.remove(player);
        markDirty();
    }

    public static boolean duelAssistMessages(UUID player) {
        return getBoolean(DUEL_ASSIST_MESSAGES, player, true);
    }

    public static boolean duelKillNotifications(UUID player) {
        return getBoolean(DUEL_KILL_NOTIFICATIONS, player, true);
    }

    public static boolean duelKillNotifications(UUID player, boolean ranked) {
        return ranked ? getBoolean(RANKED_DUEL_KILL_NOTIFICATIONS, player, true) : duelKillNotifications(player);
    }

    public static boolean duelSessionMessages(UUID player) {
        return getBoolean(DUEL_SESSION_MESSAGES, player, true);
    }

    public static boolean duelStartMessages(UUID player) {
        return duelSessionMessages(player);
    }

    public static boolean duelStartMessages(UUID player, boolean ranked) {
        return ranked ? getBoolean(RANKED_DUEL_START_MESSAGES, player, true) : duelStartMessages(player);
    }

    public static boolean duelMessages(UUID player) {
        return getBoolean(DUEL_MESSAGES, player, true);
    }

    public static boolean duelResultMessages(UUID player) {
        return getBoolean(DUEL_RESULT_MESSAGES, player, true);
    }

    public static boolean duelResultMessages(UUID player, boolean ranked) {
        return ranked ? getBoolean(RANKED_DUEL_RESULT_MESSAGES, player, true) : duelResultMessages(player);
    }

    public static boolean duelSummaryMessages(UUID player) {
        return getBoolean(DUEL_SUMMARY_MESSAGES, player, false);
    }

    public static boolean duelSummaryMessages(UUID player, boolean ranked) {
        return ranked ? getBoolean(RANKED_DUEL_SUMMARY_MESSAGES, player, false) : duelSummaryMessages(player);
    }

    public static boolean newTameMessages(UUID player) {
        return getBoolean(NEW_TAME_MESSAGES, player, true);
    }

    public static boolean autoRespawnMessages(UUID player) {
        return getBoolean(AUTO_RESPAWN_MESSAGES, player, true);
    }

    public static boolean noAutoSetBed(UUID player) {
        return getBoolean(NO_AUTO_SET_BED, player, false);
    }

    public static boolean duelGlow(UUID player) {
        return getBoolean(DUEL_GLOW, player, true);
    }

    public static boolean friendlyFire(UUID player) {
        return getBoolean(FRIENDLY_FIRE, player, false);
    }

    public static boolean tamesFriendly(UUID player) {
        return getBoolean(TAMES_FRIENDLY, player, true);
    }

    public static boolean hideLevelInName(UUID player) {
        return getBoolean(HIDE_LEVEL_IN_NAME, player, false);
    }

    public static void setEnemyKilled(UUID player, boolean enabled) {
        setBoolean(ENEMY_KILLED, player, enabled, false);
    }

    public static void setCombatAssists(UUID player, boolean enabled) {
        setBoolean(COMBAT_ASSISTS, player, enabled, false);
    }

    public static void setCombatKills(UUID player, boolean enabled) {
        setBoolean(COMBAT_KILLS, player, enabled, false);
    }

    public static void setCombatDeath(UUID player, boolean enabled) {
        setBoolean(COMBAT_DEATH, player, enabled, true);
    }

    public static void setConflictNoty(UUID player, boolean enabled) {
        setBoolean(CONFLICT_NOTY, player, enabled, true);
    }

    public static void setAbilityUsed(UUID player, boolean enabled) {
        setBoolean(ABILITY_USED, player, enabled, false);
    }

    public static void setAttributeUsed(UUID player, boolean enabled) {
        setBoolean(ATTRIBUTE_USED, player, enabled, false);
    }

    public static void setLevelUp(UUID player, boolean enabled) {
        setBoolean(LEVEL_UP, player, enabled, true);
    }

    public static void setDamage(UUID player, boolean enabled) {
        setBoolean(DAMAGE, player, enabled, false);
    }

    public static void setTeleport(UUID player, boolean enabled) {
        setBoolean(TELEPORT, player, enabled, false);
    }

    public static void setShadowHands(UUID player, boolean enabled) {
        setBoolean(SHADOW_HANDS, player, enabled, false);
    }

    public static void setHideLevelInName(UUID player, boolean enabled) {
        setBoolean(HIDE_LEVEL_IN_NAME, player, enabled, false);
    }

    public static void setEnableMending(UUID player, boolean enabled) {
        setBoolean(ENABLE_MENDING, player, enabled, true);
    }

    public static void setEnableChestXDrumFoodPreferences(UUID player, boolean enabled) {
        setBoolean(ENABLE_CHEST_DRUM_FOOD_PREFERENCES, player, enabled, true);
    }

    public static void setInventoryLowOnFood(UUID player, boolean enabled) {
        setBoolean(INVENTORY_LOW_ON_FOOD, player, enabled, false);
    }

    public static void setInventoryNoFood(UUID player, boolean enabled) {
        setBoolean(INVENTORY_NO_FOOD, player, enabled, false);
    }

    public static void setInventorySummary(UUID player, boolean enabled) {
        setBoolean(INVENTORY_SUMMARY, player, enabled, true);
    }

    public static void setInventorySummaryMinutes(UUID player, int minutes) {
        if (player == null) {
            return;
        }
        int normalized = Math.max(1, minutes);
        if (normalized == DEFAULT_INVENTORY_SUMMARY_MINUTES) {
            INVENTORY_SUMMARY_MINUTES.remove(player);
        } else {
            INVENTORY_SUMMARY_MINUTES.put(player, normalized);
        }
        markDirty();
    }

    public static void setChestDrumRange(UUID player, String dimension, int x, int y, int z, int blockRange, int height) {
        if (player == null) {
            return;
        }
        int normalizedRange = Math.max(1, Math.min(50, blockRange));
        int normalizedHeight = Math.max(0, Math.min(10, height));
        List<ChestDrumRange> ranges = CHEST_DRUM_RANGES.computeIfAbsent(player, ignored -> new ArrayList<>());
        ranges.removeIf(range -> range.dimension().equals(dimension) && range.x() == x && range.y() == y && range.z() == z);
        ranges.add(new ChestDrumRange(dimension, x, y, z, normalizedRange, normalizedHeight));
        markDirty();
    }

    public static void removeChestDrumRange(UUID player, ChestDrumRange range) {
        List<ChestDrumRange> ranges = CHEST_DRUM_RANGES.get(player);
        if (ranges == null || !ranges.remove(range)) return;
        if (ranges.isEmpty()) CHEST_DRUM_RANGES.remove(player);
        markDirty();
    }

    public static void setDuelAssistMessages(UUID player, boolean enabled) {
        setBoolean(DUEL_ASSIST_MESSAGES, player, enabled, true);
    }

    public static void setDuelKillNotifications(UUID player, boolean enabled) {
        setBoolean(DUEL_KILL_NOTIFICATIONS, player, enabled, true);
    }

    public static void setDuelKillNotifications(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            setBoolean(RANKED_DUEL_KILL_NOTIFICATIONS, player, enabled, true);
        } else {
            setDuelKillNotifications(player, enabled);
        }
    }

    public static void setDuelSessionMessages(UUID player, boolean enabled) {
        setBoolean(DUEL_SESSION_MESSAGES, player, enabled, true);
    }

    public static void setDuelStartMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            setBoolean(RANKED_DUEL_START_MESSAGES, player, enabled, true);
        } else {
            setDuelSessionMessages(player, enabled);
        }
    }

    public static void setDuelMessages(UUID player, boolean enabled) {
        setBoolean(DUEL_MESSAGES, player, enabled, true);
        setDuelAllMessages(player, false, enabled);
    }

    public static void setDuelResultMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            setBoolean(RANKED_DUEL_RESULT_MESSAGES, player, enabled, true);
        } else {
            setBoolean(DUEL_RESULT_MESSAGES, player, enabled, true);
        }
    }

    public static void setDuelSummaryMessages(UUID player, boolean enabled) {
        setBoolean(DUEL_SUMMARY_MESSAGES, player, enabled, false);
    }

    public static void setDuelSummaryMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            setBoolean(RANKED_DUEL_SUMMARY_MESSAGES, player, enabled, false);
        } else {
            setDuelSummaryMessages(player, enabled);
        }
    }

    public static void setDuelAllMessages(UUID player, boolean ranked, boolean enabled) {
        setDuelKillNotifications(player, ranked, enabled);
        setDuelStartMessages(player, ranked, enabled);
        setDuelResultMessages(player, ranked, enabled);
        setDuelSummaryMessages(player, ranked, enabled);
    }

    public static void setDuelGeneral(UUID player, boolean enabled) {
        setDuelAllMessages(player, false, enabled);
        setDuelAssistMessages(player, enabled);
        setDuelMessages(player, enabled);
    }

    public static void setRankedDuelGeneral(UUID player, boolean enabled) {
        setDuelAllMessages(player, true, enabled);
    }

    public static void setNewTameMessages(UUID player, boolean enabled) {
        setBoolean(NEW_TAME_MESSAGES, player, enabled, true);
    }

    public static void setAutoRespawnMessages(UUID player, boolean enabled) {
        setBoolean(AUTO_RESPAWN_MESSAGES, player, enabled, true);
    }

    public static void setNoAutoSetBed(UUID player, boolean enabled) {
        setBoolean(NO_AUTO_SET_BED, player, enabled, false);
    }

    public static void setDuelGlow(UUID player, boolean enabled) {
        setBoolean(DUEL_GLOW, player, enabled, true);
    }

    public static void setFriendlyFire(UUID player, boolean enabled) {
        setBoolean(FRIENDLY_FIRE, player, enabled, false);
    }

    public static void setTamesFriendly(UUID player, boolean enabled) {
        setBoolean(TAMES_FRIENDLY, player, enabled, true);
    }

    public static void setOtherGeneral(UUID player, boolean enabled) {
        setNewTameMessages(player, enabled);
        setAutoRespawnMessages(player, enabled);
        setLevelUp(player, enabled);
    }

    public static Map<UUID, CompoundTag> saveAll() {
        Map<UUID, CompoundTag> out = new HashMap<>();
        for (BooleanSetting setting : BOOLEAN_SETTINGS) {
            for (Map.Entry<UUID, Boolean> entry : setting.values().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                CompoundTag row = out.computeIfAbsent(entry.getKey(), ignored -> new CompoundTag());
                row.putBoolean(setting.key(), entry.getValue());
            }
        }
        for (Map.Entry<UUID, Integer> entry : INVENTORY_SUMMARY_MINUTES.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            CompoundTag row = out.computeIfAbsent(entry.getKey(), ignored -> new CompoundTag());
            row.putInt("inventorySummaryMinutes", Math.max(1, entry.getValue()));
        }
        for (Map.Entry<UUID, List<ChestDrumRange>> entry : CHEST_DRUM_RANGES.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            ListTag list = new ListTag();
            for (ChestDrumRange range : entry.getValue()) {
                CompoundTag saved = new CompoundTag();
                saved.putString("dimension", range.dimension());
                saved.putInt("x", range.x());
                saved.putInt("y", range.y());
                saved.putInt("z", range.z());
                saved.putInt("blockRange", range.blockRange());
                saved.putInt("height", range.height());
                list.add(saved);
            }
            out.computeIfAbsent(entry.getKey(), ignored -> new CompoundTag()).put("chestDrumRanges", list);
        }
        saveTypeRules(out, "chestDrumPullNonPreferredTypes", CHEST_DRUM_PULL_NON_PREFERRED_TYPES);
        saveTypeRules(out, "chestDrumPullPreferredFoodOfTypes", CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES);
        for (Map.Entry<UUID, Set<UUID>> entry : DO_NOT_ATTACK_OWNERS.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            ListTag list = new ListTag();
            for (UUID ownerId : entry.getValue()) {
                CompoundTag value = new CompoundTag();
                value.putUUID("owner", ownerId);
                list.add(value);
            }
            out.computeIfAbsent(entry.getKey(), ignored -> new CompoundTag()).put("doNotAttackOwners", list);
        }
        return out;
    }

    public static void loadAll(Map<UUID, CompoundTag> rows) {
        clearAll();
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, CompoundTag> entry : rows.entrySet()) {
            UUID player = entry.getKey();
            CompoundTag tag = entry.getValue();
            if (player == null || tag == null) {
                continue;
            }
            // Migrate the former separate duel/ranked glow settings. A previous
            // false value wins so an opted-out player is never silently re-enabled.
            if (!tag.contains("duelGlow", Tag.TAG_BYTE)
                    && (tag.contains("duelsGlow", Tag.TAG_BYTE) || tag.contains("rankedGlow", Tag.TAG_BYTE))) {
                boolean migrated = (!tag.contains("duelsGlow", Tag.TAG_BYTE) || tag.getBoolean("duelsGlow"))
                        && (!tag.contains("rankedGlow", Tag.TAG_BYTE) || tag.getBoolean("rankedGlow"));
                if (!migrated) DUEL_GLOW.put(player, false);
            }
            for (BooleanSetting setting : BOOLEAN_SETTINGS) {
                if (tag.contains(setting.key(), Tag.TAG_BYTE)) {
                    setting.values().put(player, tag.getBoolean(setting.key()));
                }
            }
            if (tag.contains("inventorySummaryMinutes", Tag.TAG_INT)) {
                INVENTORY_SUMMARY_MINUTES.put(player, Math.max(1, tag.getInt("inventorySummaryMinutes")));
            }
            if (tag.contains("chestDrumRanges", Tag.TAG_LIST)) {
                ListTag list = tag.getList("chestDrumRanges", Tag.TAG_COMPOUND);
                List<ChestDrumRange> ranges = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag saved = list.getCompound(i);
                    ranges.add(new ChestDrumRange(saved.getString("dimension"), saved.getInt("x"), saved.getInt("y"), saved.getInt("z"),
                            Math.max(1, Math.min(50, saved.getInt("blockRange"))), Math.max(0, Math.min(10, saved.getInt("height")))));
                }
                if (!ranges.isEmpty()) CHEST_DRUM_RANGES.put(player, ranges);
            }
            loadTypeRules(tag, "chestDrumPullNonPreferredTypes", player, CHEST_DRUM_PULL_NON_PREFERRED_TYPES);
            loadTypeRules(tag, "chestDrumPullPreferredFoodOfTypes", player, CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES);
            if (tag.contains("doNotAttackOwners", Tag.TAG_LIST)) {
                ListTag list = tag.getList("doNotAttackOwners", Tag.TAG_COMPOUND);
                Set<UUID> values = new LinkedHashSet<>();
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag saved = list.getCompound(i);
                    if (saved.hasUUID("owner")) values.add(saved.getUUID("owner"));
                }
                if (!values.isEmpty()) DO_NOT_ATTACK_OWNERS.put(player, values);
            }
        }
    }

    private static boolean getBoolean(Map<UUID, Boolean> map, UUID player, boolean defaultValue) {
        return player == null ? defaultValue : map.getOrDefault(player, defaultValue);
    }

    private static void setBoolean(Map<UUID, Boolean> map, UUID player, boolean enabled, boolean defaultValue) {
        if (player == null) {
            return;
        }
        if (enabled == defaultValue) {
            map.remove(player);
        } else {
            map.put(player, enabled);
        }
        markDirty();
    }

    private static void clearAll() {
        for (BooleanSetting setting : BOOLEAN_SETTINGS) {
            setting.values().clear();
        }
        INVENTORY_SUMMARY_MINUTES.clear();
        CHEST_DRUM_RANGES.clear();
        CHEST_DRUM_PULL_NON_PREFERRED_TYPES.clear();
        CHEST_DRUM_PULL_PREFERRED_FOOD_OF_TYPES.clear();
        DO_NOT_ATTACK_OWNERS.clear();
    }

    private static void setTypeRule(Map<UUID, Set<String>> rules, UUID player, String type, boolean enabled) {
        if (player == null || type == null || type.isBlank()) return;
        String normalized = type.trim().toLowerCase(java.util.Locale.ROOT);
        Set<String> values = rules.computeIfAbsent(player, ignored -> new LinkedHashSet<>());
        if (enabled) values.add(normalized);
        else values.remove(normalized);
        if (values.isEmpty()) rules.remove(player);
        markDirty();
    }

    private static void saveTypeRules(Map<UUID, CompoundTag> out, String key, Map<UUID, Set<String>> rules) {
        for (Map.Entry<UUID, Set<String>> entry : rules.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue().isEmpty()) continue;
            ListTag list = new ListTag();
            for (String type : entry.getValue()) {
                CompoundTag value = new CompoundTag();
                value.putString("type", type);
                list.add(value);
            }
            out.computeIfAbsent(entry.getKey(), ignored -> new CompoundTag()).put(key, list);
        }
    }

    private static void loadTypeRules(CompoundTag tag, String key, UUID player, Map<UUID, Set<String>> rules) {
        if (!tag.contains(key, Tag.TAG_LIST)) return;
        ListTag list = tag.getList(key, Tag.TAG_COMPOUND);
        Set<String> values = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String type = list.getCompound(i).getString("type").trim().toLowerCase(java.util.Locale.ROOT);
            if (!type.isBlank()) values.add(type);
        }
        if (!values.isEmpty()) rules.put(player, values);
    }

    private static void markDirty() {
        if (TameRegistry.isInitialized()) {
            TameRegistry.markDirty();
        }
    }
}
