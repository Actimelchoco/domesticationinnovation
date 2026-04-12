package net.miauczel.legendary_monsters.entity.ai.goal;

import net.miauczel.legendary_monsters.compat.DIServerPetCommandCompat;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

import java.lang.reflect.Method;

public class DICommandedFollowOwnerGoal extends FollowOwnerGoal {
    private final TamableAnimal tame;

    public DICommandedFollowOwnerGoal(TamableAnimal tame, double speedModifier, float startDistance, float stopDistance, boolean canFly) {
        super(tame, speedModifier, startDistance, stopDistance, canFly);
        this.tame = tame;
    }

    @Override
    public boolean canUse() {
        return DIServerPetCommandCompat.shouldFollow(this.tame) && super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return DIServerPetCommandCompat.shouldFollow(this.tame) && super.canContinueToUse();
    }

    @Override
    public void start() {
        super.start();
        if (!DIServerPetCommandCompat.shouldFollow(this.tame)) {
            stopNavigation(this.tame);
        }
    }

    private static void stopNavigation(TamableAnimal tame) {
        try {
            Method getNavigation = tame.getClass().getMethod("m_21573_");
            getNavigation.setAccessible(true);
            Object navigation = getNavigation.invoke(tame);
            if (navigation != null) {
                invokeStop(navigation);
            }
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method getNavigation = tame.getClass().getMethod("getNavigation");
            getNavigation.setAccessible(true);
            Object navigation = getNavigation.invoke(tame);
            if (navigation != null) {
                invokeStop(navigation);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeStop(Object navigation) {
        try {
            Method stop = navigation.getClass().getMethod("m_26573_");
            stop.setAccessible(true);
            stop.invoke(navigation);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method stop = navigation.getClass().getMethod("stop");
            stop.setAccessible(true);
            stop.invoke(navigation);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
