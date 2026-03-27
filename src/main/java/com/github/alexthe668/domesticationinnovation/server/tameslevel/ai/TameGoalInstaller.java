package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;

public final class TameGoalInstaller {
    private TameGoalInstaller() {
    }

    public static void installIfMissing(TamableAnimal tame) {
        // Mode targeting now runs from TameBehaviorEvents.
    }

    private static void addTargetIfMissing(TamableAnimal tame, Class<? extends Goal> goalType, int priority, Goal goal) {
        boolean alreadyPresent = tame.targetSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> goalType.isInstance(wrapped.getGoal()));
        if (!alreadyPresent) {
            tame.targetSelector.addGoal(priority, goal);
        }
    }

}
