package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

public final class TLAdminRuntimeSettings {
    private TLAdminRuntimeSettings() {
    }

    private static volatile boolean friendlyFireEnabled = true;
    private static volatile float singleTargetAbilityDamageMultiplier = 1.0F;
    private static volatile float aoeAbilityDamageMultiplier = 1.0F;
    private static volatile float abilityCountCooldownNerfPercent = 25.0F;

    public static boolean friendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    public static void setFriendlyFireEnabled(boolean enabled) {
        friendlyFireEnabled = enabled;
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

    private static float clampMultiplier(float multiplier) {
        if (Float.isNaN(multiplier) || Float.isInfinite(multiplier)) {
            return 0.0F;
        }
        return Math.max(0.0F, multiplier);
    }
}
