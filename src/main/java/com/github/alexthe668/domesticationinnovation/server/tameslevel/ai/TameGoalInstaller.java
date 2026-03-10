package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

public final class TameGoalInstaller {
    private TameGoalInstaller() {
    }

    public static void installIfMissing(TamableAnimal tame) {
        addIfMissing(tame, PassiveModeGoal.class, 3, new PassiveModeGoal(tame));
        addIfMissing(tame, DefaultPlusModeGoal.class, 3, new DefaultPlusModeGoal(tame));
        addIfMissing(tame, BossModeGoal.class, 3, new BossModeGoal(tame));
        addIfMissing(tame, BodyguardModeGoal.class, 3, new BodyguardModeGoal(tame));
        addIfMissing(tame, MonsterHunterModeGoal.class, 3, new MonsterHunterModeGoal(tame));
        addIfMissing(tame, AggressiveModeGoal.class, 3, new AggressiveModeGoal(tame));
    }

    private static void addIfMissing(TamableAnimal tame, Class<? extends Goal> goalType, int priority, Goal goal) {
        boolean alreadyPresent = tame.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!alreadyPresent) {
            tame.goalSelector.addGoal(priority, goal);
        }
    }
}
