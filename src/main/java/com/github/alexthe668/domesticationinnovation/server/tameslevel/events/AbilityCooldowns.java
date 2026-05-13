package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;

final class AbilityCooldowns {
    private AbilityCooldowns() {
    }

    static long scaledCooldownTicks(TameData data, String sourceId, long baseTicks) {
        long ticks = Math.max(1L, baseTicks);
        return Math.max(1L, Math.round(ticks * cooldownMultiplier(data, sourceId)));
    }

    private static double cooldownMultiplier(TameData data, String sourceId) {
        double attackNerfMultiplier = 1.0D;
        if (LevelSystem.isAttackAbility(sourceId)) {
            int abilityCount = countOwnedAttackAbilities(data);
            if (abilityCount > 1) {
                double percent = TLAdminRuntimeSettings.abilityCountCooldownNerfPercent() / 100.0D;
                attackNerfMultiplier = 1.0D + percent * (Math.log(abilityCount) / Math.log(2.0D));
            }
        }
        return attackNerfMultiplier * LevelSystem.quickyCooldownMultiplier(data);
    }

    private static int countOwnedAttackAbilities(TameData data) {
        if (data == null) {
            return 0;
        }
        int count = 0;
        for (var entry : data.abilityLevels.entrySet()) {
            if (entry.getValue() > 0 && LevelSystem.isAttackAbility(entry.getKey())) {
                count++;
            }
        }
        if (count == 0 && !data.abilities.isEmpty()) {
            for (String abilityId : data.abilities) {
                if (LevelSystem.isAttackAbility(abilityId)) {
                    count++;
                }
            }
        }
        return count;
    }
}
