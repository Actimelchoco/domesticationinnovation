package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

final class DefaultPlusModeGoal extends AbstractModeGoal {
    DefaultPlusModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.DEFAULT_PLUS;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        ServerPlayer owner = TameGoalSupport.owner(tame);
        if (owner != null) {
            TameGoalSupport.setBodyguardTarget(level, tame, owner, 24.0D, 72.0D);
        }
    }
}

