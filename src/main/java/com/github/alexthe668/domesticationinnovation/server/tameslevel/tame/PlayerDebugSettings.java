package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final Map<UUID, Boolean> DUELS_GLOW = new HashMap<>();
    private static final Map<UUID, Boolean> RANKED_GLOW = new HashMap<>();
    private static final Map<UUID, Boolean> FRIENDLY_FIRE = new HashMap<>();
    private static final Map<UUID, Boolean> ENABLE_MENDING = new HashMap<>();

    private record BooleanSetting(String key, Map<UUID, Boolean> values, boolean defaultValue) {
    }

    private static final List<BooleanSetting> BOOLEAN_SETTINGS = List.of(
            new BooleanSetting("enemyKilled", ENEMY_KILLED, false),
            new BooleanSetting("combatAssists", COMBAT_ASSISTS, false),
            new BooleanSetting("combatKills", COMBAT_KILLS, false),
            new BooleanSetting("combatDeath", COMBAT_DEATH, true),
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
            new BooleanSetting("duelsGlow", DUELS_GLOW, true),
            new BooleanSetting("rankedGlow", RANKED_GLOW, true),
            new BooleanSetting("friendlyFire", FRIENDLY_FIRE, false),
            new BooleanSetting("enableMending", ENABLE_MENDING, true)
    );

    public static boolean enemyKilled(UUID player) {
        return getBoolean(ENEMY_KILLED, player, false);
    }

    public static boolean enableMending(UUID player) {
        return getBoolean(ENABLE_MENDING, player, true);
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

    public static boolean duelsGlow(UUID player) {
        return getBoolean(DUELS_GLOW, player, true);
    }

    public static boolean rankedGlow(UUID player) {
        return getBoolean(RANKED_GLOW, player, true);
    }

    public static boolean friendlyFire(UUID player) {
        return getBoolean(FRIENDLY_FIRE, player, false);
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

    public static void setEnableMending(UUID player, boolean enabled) {
        setBoolean(ENABLE_MENDING, player, enabled, true);
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

    public static void setDuelsGlow(UUID player, boolean enabled) {
        setBoolean(DUELS_GLOW, player, enabled, true);
    }

    public static void setRankedGlow(UUID player, boolean enabled) {
        setBoolean(RANKED_GLOW, player, enabled, true);
    }

    public static void setFriendlyFire(UUID player, boolean enabled) {
        setBoolean(FRIENDLY_FIRE, player, enabled, false);
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
    }

    private static void markDirty() {
        if (TameRegistry.isInitialized()) {
            TameRegistry.markDirty();
        }
    }
}
