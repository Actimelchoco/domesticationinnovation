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
}
