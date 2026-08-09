package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class TameDuelSnapshots {
    private TameDuelSnapshots() {
    }

    public static void copyPersistentStats(TameData from, TameData into) {
        if (from == null || into == null) {
            return;
        }
        into.level = from.level;
        into.xp = from.xp;
        into.xpToNext = from.xpToNext;
        into.kills = from.kills;
        into.assists = from.assists;
        into.deaths = from.deaths;
        into.bonusHealth = from.bonusHealth;
        into.bonusDamage = from.bonusDamage;
        into.bonusSpeed = from.bonusSpeed;
        into.bonusArmor = from.bonusArmor;
        into.bonusArmorToughness = from.bonusArmorToughness;
        into.bonusKnockback = from.bonusKnockback;
        into.bonusKnockbackResist = from.bonusKnockbackResist;
        into.tameClass = from.tameClass;
        into.classRerollsUsed = from.classRerollsUsed;
        into.levelRewardHistory.clear();
        for (CompoundTag rewardEntry : from.levelRewardHistory) {
            if (rewardEntry != null && !rewardEntry.isEmpty()) {
                into.levelRewardHistory.add(rewardEntry.copy());
            }
        }
        into.abilities.clear();
        into.abilities.addAll(from.abilities);
        into.abilityLevels.clear();
        into.abilityLevels.putAll(from.abilityLevels);
        into.attributeLevels.clear();
        into.attributeLevels.putAll(from.attributeLevels);
        into.hasSavedProgress = from.hasSavedProgress;
        into.savedProgressCost = from.savedProgressCost;
        into.savedLevel = from.savedLevel;
        into.savedXp = from.savedXp;
        into.savedXpToNext = from.savedXpToNext;
        into.savedKills = from.savedKills;
        into.savedAssists = from.savedAssists;
        into.savedBonusHealth = from.savedBonusHealth;
        into.savedBonusDamage = from.savedBonusDamage;
        into.savedBonusSpeed = from.savedBonusSpeed;
        into.savedBonusArmor = from.savedBonusArmor;
        into.savedBonusArmorToughness = from.savedBonusArmorToughness;
        into.savedBonusKnockback = from.savedBonusKnockback;
        into.savedBonusKnockbackResist = from.savedBonusKnockbackResist;
        into.savedAbilities.clear();
        into.savedAbilities.addAll(from.savedAbilities);
        into.savedAbilityLevels.clear();
        into.savedAbilityLevels.putAll(from.savedAbilityLevels);
        into.savedAttributeLevels.clear();
        into.savedAttributeLevels.putAll(from.savedAttributeLevels);
        into.oreScentingOreId = from.oreScentingOreId;
        into.duelMmr = from.duelMmr;
        into.duelKills = from.duelKills;
        into.duelAssists = from.duelAssists;
        into.duelDeaths = from.duelDeaths;
        into.duelWins = from.duelWins;
        into.duelLosses = from.duelLosses;
        into.duelCount = from.duelCount;
        into.duelPoints = from.duelPoints;
        into.hungerSaturation = from.hungerSaturation;
        into.hungerEmptyNotified = from.hungerEmptyNotified;
        into.hungerLowNotified = from.hungerLowNotified;
        into.hungerLastFoodNotified = from.hungerLastFoodNotified;
        into.hungerInventory.clear();
        for (ItemStack stack : from.hungerInventory) {
            if (stack != null && !stack.isEmpty()) {
                into.hungerInventory.add(stack.copy());
            }
        }
    }
}
