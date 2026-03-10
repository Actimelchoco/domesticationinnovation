package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.rewards;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DamageReward implements LevelReward {

    @Override
    public void apply(LivingEntity entity, TameData data) {

        entity.getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(
                        entity.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) + 1
                );
    }

    public String name() {
        return "Damage";
    }
}
