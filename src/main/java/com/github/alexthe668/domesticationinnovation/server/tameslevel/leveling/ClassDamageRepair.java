package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.LivingEntity;

public final class ClassDamageRepair {
    private ClassDamageRepair() {}

    public static boolean repair(LivingEntity tame, TameData data) {
        if (data == null || data.classDamageRepaired) return false;
        double ratio = data.tameClass == TameClass.DPS ? 1.25
                : data.tameClass == TameClass.STRIKER ? 2 : 0;
        if (ratio == 0) return false;
        double newCurrent = 0, newSaved = 0;
        for (CompoundTag row : data.levelRewardHistory) {
            if (!isDamage(row) || !row.getBoolean("damageRewardV2")) continue;
            if (!row.contains("active") || row.getBoolean("active")) newCurrent += row.getDouble("rewardAmount");
            if (row.getInt("level") <= data.savedLevel) newSaved += row.getDouble("rewardAmount");
        }
        double delta = Math.max(0, data.bonusDamage - newCurrent) * (ratio - 1);
        if (tame != null && !tame.isRemoved()) {
            CompoundTag live = new CompoundTag();
            if (tame.save(live)) data.entitySnapshot = live;
        }
        data.bonusDamage += delta;
        data.savedBonusDamage += Math.max(0, data.savedBonusDamage - newSaved) * (ratio - 1);
        for (CompoundTag row : data.levelRewardHistory) {
            if (!isDamage(row) || row.getBoolean("damageRewardV2")) continue;
            double amount = row.getDouble("rewardAmount") * ratio;
            row.putDouble("rewardAmount", amount);
            row.putString("reward", "Damage +" + String.format(java.util.Locale.ROOT, "%.2f", amount));
            row.putBoolean("damageRewardV2", true);
        }
        // Snapshot normalization subtracts tracked damage from the saved attribute base.
        // Move both together so respawn never eats the repair or changes the species base.
        if (data.entitySnapshot != null) {
            for (Tag tag : data.entitySnapshot.getList("Attributes", Tag.TAG_COMPOUND)) {
                CompoundTag attribute = (CompoundTag) tag;
                if (!"minecraft:generic.attack_damage".equals(attribute.getString("Name"))) continue;
                boolean managed = false;
                for (Tag modifierTag : attribute.getList("Modifiers", Tag.TAG_COMPOUND)) {
                    CompoundTag modifier = (CompoundTag) modifierTag;
                    if ("tl_legendary_bonus_damage".equals(modifier.getString("Name"))) {
                        modifier.putDouble("Amount", modifier.getDouble("Amount") + delta);
                        managed = true;
                    }
                }
                if (!managed) attribute.putDouble("Base", attribute.getDouble("Base") + delta);
            }
        }
        data.classDamageRepaired = true;
        if (tame != null && tame.isAlive()) {
            LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
            tame.save(data.entitySnapshot);
        }
        TameRegistry.markDirty();
        return true;
    }

    private static boolean isDamage(CompoundTag row) {
        return "BASE_STAT".equals(row.getString("rewardCategory"))
                && "DAMAGE".equalsIgnoreCase(row.getString("rewardId"));
    }
}
