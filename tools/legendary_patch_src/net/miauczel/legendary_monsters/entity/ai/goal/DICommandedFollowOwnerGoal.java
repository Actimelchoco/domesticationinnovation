package net.miauczel.legendary_monsters.entity.ai.goal;

import net.miauczel.legendary_monsters.compat.DIServerPetCommandCompat;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

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
            this.tame.getNavigation().stop();
        }
    }
}
