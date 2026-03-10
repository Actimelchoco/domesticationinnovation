package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.rewards;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.world.entity.LivingEntity;

public interface LevelReward {

    void apply(LivingEntity entity, TameData data);

    String name();

}
