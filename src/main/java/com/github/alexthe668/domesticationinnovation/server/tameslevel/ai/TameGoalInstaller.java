package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

public final class TameGoalInstaller {
    private TameGoalInstaller() {
    }

    public static void installIfMissing(TamableAnimal tame) {
        addTargetIfMissing(tame, ProtectionZoneGoal.class, 1, new ProtectionZoneGoal(tame));
        addTargetIfMissing(tame, PassiveModeGoal.class, 2, new PassiveModeGoal(tame));
        addTargetIfMissing(tame, DefaultPlusModeGoal.class, 2, new DefaultPlusModeGoal(tame));
        addTargetIfMissing(tame, BossModeGoal.class, 2, new BossModeGoal(tame));
        addTargetIfMissing(tame, BodyguardModeGoal.class, 2, new BodyguardModeGoal(tame));
        addTargetIfMissing(tame, MonsterHunterModeGoal.class, 2, new MonsterHunterModeGoal(tame));
        addTargetIfMissing(tame, ArenaModeGoal.class, 2, new ArenaModeGoal(tame));
        addTargetIfMissing(tame, AggressiveModeGoal.class, 2, new AggressiveModeGoal(tame));
    }

    private static void addTargetIfMissing(TamableAnimal tame, Class<? extends Goal> goalType, int priority, Goal goal) {
        boolean alreadyPresent = tame.targetSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!alreadyPresent) {
            tame.targetSelector.addGoal(priority, goal);
        }
    }

}
