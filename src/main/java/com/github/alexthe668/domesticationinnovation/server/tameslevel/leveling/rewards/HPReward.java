package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.rewards;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;


public class HPReward implements LevelReward {

    @Override
    public void apply(LivingEntity entity, TameData data) {

        entity.getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(
                        entity.getAttributeBaseValue(Attributes.MAX_HEALTH) + 2
                );

        entity.setHealth(entity.getMaxHealth());
    }

    public String name() {
        return "HP";
    }
}


