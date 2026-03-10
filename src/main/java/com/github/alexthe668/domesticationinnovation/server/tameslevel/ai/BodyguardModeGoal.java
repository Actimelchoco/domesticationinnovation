package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

final class BodyguardModeGoal extends AbstractModeGoal {
    BodyguardModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.BODYGUARD;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        ServerPlayer owner = TameGoalSupport.owner(tame);
        if (owner != null) {
            TameGoalSupport.setBodyguardTarget(level, tame, owner, 16.0D, 36.0D);
        }
    }
}
