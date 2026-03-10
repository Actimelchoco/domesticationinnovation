package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.rewards;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;


public class ArmorReward implements LevelReward {

    @Override
    public void apply(LivingEntity entity, TameData data) {

        entity.getAttribute(Attributes.ARMOR)
                .setBaseValue(
                        entity.getAttributeBaseValue(Attributes.ARMOR) + 1
                );

        entity.setHealth(entity.getMaxHealth());
    }

    public String name() {
        return "Armor";
    }
}


