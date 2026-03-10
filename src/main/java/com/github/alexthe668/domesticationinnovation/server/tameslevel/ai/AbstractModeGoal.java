package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

abstract class AbstractModeGoal extends Goal {
    protected final TamableAnimal tame;
    private int tickGate = 0;

    protected AbstractModeGoal(TamableAnimal tame) {
        this.tame = tame;
        // Mode goals should primarily choose targets; owning MOVE would block vanilla follow/attack pathing goals.
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    protected abstract TameMode mode();

    protected abstract void tickMode(ServerLevel level, TameData data);

    @Override
    public boolean canUse() {
        if (!(tame.level() instanceof ServerLevel)) return false;
        if (!tame.isTame()) return false;
        if (tame.isOrderedToSit()) return false;
        TameData data = TameGoalSupport.data(tame);
        if (data == null) return false;
        return TameMode.byId(data.mode) == mode();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (!(tame.level() instanceof ServerLevel level)) return;
        if (++tickGate < 20) return;
        tickGate = 0;
        TameData data = TameGoalSupport.data(tame);
        if (data == null) return;
        tickMode(level, data);
    }
}
