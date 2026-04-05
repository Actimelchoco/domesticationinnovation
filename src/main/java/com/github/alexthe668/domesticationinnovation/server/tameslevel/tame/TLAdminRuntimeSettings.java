package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

public final class TLAdminRuntimeSettings {
    private TLAdminRuntimeSettings() {
    }

    private static volatile boolean friendlyFireEnabled = true;
    private static volatile boolean healthSiphonEnabled = true;
    private static volatile boolean herdingAffectsTames = false;
    private static volatile boolean postTpStabilizationEnabled = true;
    private static volatile float singleTargetAbilityDamageMultiplier = 1.0F;
    private static volatile float aoeAbilityDamageMultiplier = 1.0F;
    private static volatile float abilityCountCooldownNerfPercent = 25.0F;
    private static volatile int duelSessionLengthMinutes = 5;

    public static boolean friendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    public static void setFriendlyFireEnabled(boolean enabled) {
        friendlyFireEnabled = enabled;
    }

    public static boolean healthSiphonEnabled() {
        return healthSiphonEnabled;
    }

    public static void setHealthSiphonEnabled(boolean enabled) {
        healthSiphonEnabled = enabled;
    }

    public static boolean herdingAffectsTames() {
        return herdingAffectsTames;
    }

    public static void setHerdingAffectsTames(boolean enabled) {
        herdingAffectsTames = enabled;
    }

    public static boolean postTpStabilizationEnabled() {
        return postTpStabilizationEnabled;
    }

    public static void setPostTpStabilizationEnabled(boolean enabled) {
        postTpStabilizationEnabled = enabled;
    }

    public static float singleTargetAbilityDamageMultiplier() {
        return singleTargetAbilityDamageMultiplier;
    }

    public static void setSingleTargetAbilityDamageMultiplier(float multiplier) {
        singleTargetAbilityDamageMultiplier = clampMultiplier(multiplier);
    }

    public static float aoeAbilityDamageMultiplier() {
        return aoeAbilityDamageMultiplier;
    }

    public static void setAoeAbilityDamageMultiplier(float multiplier) {
        aoeAbilityDamageMultiplier = clampMultiplier(multiplier);
    }

    public static float abilityCountCooldownNerfPercent() {
        return abilityCountCooldownNerfPercent;
    }

    public static void setAbilityCountCooldownNerfPercent(float percent) {
        abilityCountCooldownNerfPercent = clampMultiplier(percent);
    }

    public static int duelSessionLengthMinutes() {
        return duelSessionLengthMinutes;
    }

    public static void setDuelSessionLengthMinutes(int minutes) {
        duelSessionLengthMinutes = Math.max(1, minutes);
    }

    private static float clampMultiplier(float multiplier) {
        if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return 0.0F;
        }
        return Math.max(0.0F, multiplier);
    }
}
