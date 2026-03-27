package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

public final class TameGoalInstaller {
    private TameGoalInstaller() {
    }

    public static void installIfMissing(TamableAnimal tame) {
        addTargetIfMissing(tame, PassiveModeGoal.class, 3, new PassiveModeGoal(tame));
        addTargetIfMissing(tame, DefaultPlusModeGoal.class, 3, new DefaultPlusModeGoal(tame));
        addTargetIfMissing(tame, BossModeGoal.class, 3, new BossModeGoal(tame));
        addTargetIfMissing(tame, BodyguardModeGoal.class, 3, new BodyguardModeGoal(tame));
        // Monster hunter / aggressive / arena use the event-driven periodic scan instead.
    }

    private static void addTargetIfMissing(TamableAnimal tame, Class<? extends Goal> goalType, int priority, Goal goal) {
        boolean alreadyPresent = tame.targetSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!alreadyPresent) {
            tame.targetSelector.addGoal(priority, goal);
        }
    }

}
