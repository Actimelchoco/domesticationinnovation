package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

final class AssasinModeGoal extends AbstractModeGoal {
    AssasinModeGoal(TamableAnimal tame) { super(tame); }

    @Override protected TameMode mode() { return TameMode.ASSASIN; }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        LivingEntity target = TameGoalSupport.assasinTarget(level, tame.getOwnerUUID());
        if (tame.getTarget() != target) tame.setTarget(target);
    }

    @Override protected boolean retainLiveTarget() { return false; }
    @Override protected int tickInterval() { return 10; }
}
