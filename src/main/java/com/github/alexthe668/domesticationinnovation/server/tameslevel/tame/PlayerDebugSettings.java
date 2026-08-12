package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerDebugSettings {
    private PlayerDebugSettings() {
    }

    private static final Map<UUID, Boolean> ENEMY_KILLED = new HashMap<>();
    private static final Map<UUID, Boolean> ABILITY_USED = new HashMap<>();
    private static final Map<UUID, Boolean> ATTRIBUTE_USED = new HashMap<>();
    private static final Map<UUID, Boolean> LEVEL_UP = new HashMap<>();
    private static final Map<UUID, Boolean> DAMAGE = new HashMap<>();
    private static final Map<UUID, Boolean> TELEPORT = new HashMap<>();
    private static final Map<UUID, Boolean> SHADOW_HANDS = new HashMap<>();
    private static final Map<UUID, Boolean> INVENTORY_LOW_ON_FOOD = new HashMap<>();
    private static final Map<UUID, Boolean> INVENTORY_NO_FOOD = new HashMap<>();
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

    public static boolean enemyKilled(UUID player) {
        return ENEMY_KILLED.getOrDefault(player, false);
    }

    public static void setEnemyKilled(UUID player, boolean enabled) {
        ENEMY_KILLED.put(player, enabled);
    }

    public static boolean abilityUsed(UUID player) {
        return ABILITY_USED.getOrDefault(player, false);
    }

    public static boolean attributeUsed(UUID player) {
        return ATTRIBUTE_USED.getOrDefault(player, false);
    }

    public static boolean levelUp(UUID player) {
        return LEVEL_UP.getOrDefault(player, true);
    }

    public static boolean damage(UUID player) {
        return DAMAGE.getOrDefault(player, false);
    }

    public static boolean teleport(UUID player) {
        return TELEPORT.getOrDefault(player, false);
    }

    public static boolean shadowHands(UUID player) {
        return SHADOW_HANDS.getOrDefault(player, false);
    }

    public static boolean inventoryLowOnFood(UUID player) {
        return INVENTORY_LOW_ON_FOOD.getOrDefault(player, false);
    }

    public static boolean inventoryNoFood(UUID player) {
        return INVENTORY_NO_FOOD.getOrDefault(player, false);
    }

    public static boolean duelAssistMessages(UUID player) {
        return DUEL_ASSIST_MESSAGES.getOrDefault(player, true);
    }

    public static boolean duelKillNotifications(UUID player) {
        return DUEL_KILL_NOTIFICATIONS.getOrDefault(player, true);
    }

    public static boolean duelKillNotifications(UUID player, boolean ranked) {
        return ranked ? RANKED_DUEL_KILL_NOTIFICATIONS.getOrDefault(player, true) : duelKillNotifications(player);
    }

    public static boolean duelSessionMessages(UUID player) {
        return DUEL_SESSION_MESSAGES.getOrDefault(player, true);
    }

    public static boolean duelStartMessages(UUID player) {
        return duelSessionMessages(player);
    }

    public static boolean duelStartMessages(UUID player, boolean ranked) {
        return ranked ? RANKED_DUEL_START_MESSAGES.getOrDefault(player, true) : duelStartMessages(player);
    }

    public static boolean duelMessages(UUID player) {
        return DUEL_MESSAGES.getOrDefault(player, true);
    }

    public static boolean duelResultMessages(UUID player) {
        return DUEL_RESULT_MESSAGES.getOrDefault(player, true);
    }

    public static boolean duelResultMessages(UUID player, boolean ranked) {
        return ranked ? RANKED_DUEL_RESULT_MESSAGES.getOrDefault(player, true) : duelResultMessages(player);
    }

    public static boolean duelSummaryMessages(UUID player) {
        return DUEL_SUMMARY_MESSAGES.getOrDefault(player, false);
    }

    public static boolean duelSummaryMessages(UUID player, boolean ranked) {
        return ranked ? RANKED_DUEL_SUMMARY_MESSAGES.getOrDefault(player, false) : duelSummaryMessages(player);
    }

    public static void setAbilityUsed(UUID player, boolean enabled) {
        ABILITY_USED.put(player, enabled);
    }

    public static void setAttributeUsed(UUID player, boolean enabled) {
        ATTRIBUTE_USED.put(player, enabled);
    }

    public static void setLevelUp(UUID player, boolean enabled) {
        LEVEL_UP.put(player, enabled);
    }

    public static void setDamage(UUID player, boolean enabled) {
        DAMAGE.put(player, enabled);
    }

    public static void setTeleport(UUID player, boolean enabled) {
        TELEPORT.put(player, enabled);
    }

    public static void setShadowHands(UUID player, boolean enabled) {
        SHADOW_HANDS.put(player, enabled);
    }

    public static void setInventoryLowOnFood(UUID player, boolean enabled) {
        INVENTORY_LOW_ON_FOOD.put(player, enabled);
    }

    public static void setInventoryNoFood(UUID player, boolean enabled) {
        INVENTORY_NO_FOOD.put(player, enabled);
    }

    public static void setDuelAssistMessages(UUID player, boolean enabled) {
        DUEL_ASSIST_MESSAGES.put(player, enabled);
    }

    public static void setDuelKillNotifications(UUID player, boolean enabled) {
        DUEL_KILL_NOTIFICATIONS.put(player, enabled);
    }

    public static void setDuelKillNotifications(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            RANKED_DUEL_KILL_NOTIFICATIONS.put(player, enabled);
        } else {
            setDuelKillNotifications(player, enabled);
        }
    }

    public static void setDuelSessionMessages(UUID player, boolean enabled) {
        DUEL_SESSION_MESSAGES.put(player, enabled);
    }

    public static void setDuelStartMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            RANKED_DUEL_START_MESSAGES.put(player, enabled);
        } else {
            setDuelSessionMessages(player, enabled);
        }
    }

    public static void setDuelMessages(UUID player, boolean enabled) {
        DUEL_MESSAGES.put(player, enabled);
        setDuelAllMessages(player, false, enabled);
    }

    public static void setDuelResultMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            RANKED_DUEL_RESULT_MESSAGES.put(player, enabled);
        } else {
            DUEL_RESULT_MESSAGES.put(player, enabled);
        }
    }

    public static void setDuelSummaryMessages(UUID player, boolean enabled) {
        DUEL_SUMMARY_MESSAGES.put(player, enabled);
    }

    public static void setDuelSummaryMessages(UUID player, boolean ranked, boolean enabled) {
        if (ranked) {
            RANKED_DUEL_SUMMARY_MESSAGES.put(player, enabled);
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
}
