package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;

final class MonsterHunterModeGoal extends AbstractModeGoal {
    MonsterHunterModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.MONSTER_HUNTER;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        TameGoalSupport.setHunterTarget(level, tame, 7.0D);
    }
}
