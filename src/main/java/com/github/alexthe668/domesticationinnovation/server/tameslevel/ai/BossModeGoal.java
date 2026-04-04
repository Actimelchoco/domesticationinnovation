package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;

final class BossModeGoal extends AbstractModeGoal {
    private static final int BOSS_SCAN_INTERVAL = 40;

    BossModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.BOSS;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        ServerPlayer owner = TameGoalSupport.owner(tame);
        TameGoalSupport.setBossTarget(level, tame, owner);
    }

    @Override
    protected int tickInterval() {
        return BOSS_SCAN_INTERVAL;
    }

    @Override
    protected boolean retainLiveTarget() {
        return false;
    }
}
