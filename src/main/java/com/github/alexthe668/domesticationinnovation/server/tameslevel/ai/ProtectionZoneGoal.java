package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

final class ProtectionZoneGoal extends Goal {
    private final TamableAnimal tame;
    private int tickGate = 0;

    ProtectionZoneGoal(TamableAnimal tame) {
        this.tame = tame;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!(tame.level() instanceof ServerLevel)) return false;
        if (!tame.isTame()) return false;
        TameData data = TameGoalSupport.data(tame);
        if (data == null) return false;
        return data.hasProtectionZone;
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
        TameGoalSupport.handleProtectionZone(level, tame, data);
    }
}
