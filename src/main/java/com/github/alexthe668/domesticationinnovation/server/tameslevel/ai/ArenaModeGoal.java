package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;

final class ArenaModeGoal extends AbstractModeGoal {
    ArenaModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.ARENA;
    }

    @Override
    protected int tickInterval() {
        return 10;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        TameGoalSupport.setAggressiveTarget(level, tame, 64.0D);
    }
}
