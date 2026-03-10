package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

final class EscapeGoal extends Goal {
    private final TamableAnimal tame;
    private int tickGate = 0;

    EscapeGoal(TamableAnimal tame) {
        this.tame = tame;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!tame.isTame()) return false;
        TameData data = TameGoalSupport.data(tame);
        if (data == null) return false;
        TameGoalSupport.refreshEscapeState(tame, data);
        return data.escapeActive && TameGoalSupport.owner(tame) != null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (++tickGate < 20) return;
        tickGate = 0;
        TameData data = TameGoalSupport.data(tame);
        ServerPlayer owner = TameGoalSupport.owner(tame);
        if (data == null || owner == null) return;
        TameGoalSupport.handleEscapeMode(tame, owner, data);
    }
}
