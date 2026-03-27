package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;

final class AggressiveModeGoal extends AbstractModeGoal {
    AggressiveModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.AGGRESSIVE;
    }

    @Override
    protected int tickInterval() {
        return 10;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        TameGoalSupport.setAggressiveTarget(level, tame, 10.0D);
    }
}
