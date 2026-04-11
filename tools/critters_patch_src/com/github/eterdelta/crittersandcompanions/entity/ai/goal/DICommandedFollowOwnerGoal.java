package com.github.eterdelta.crittersandcompanions.entity.ai.goal;

import com.github.eterdelta.crittersandcompanions.compat.DIServerPetCommandCompat;
import com.github.eterdelta.crittersandcompanions.entity.DragonflyEntity;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.TamableAnimal;

public class DICommandedFollowOwnerGoal extends FollowOwnerGoal {
    private final DragonflyEntity dragonfly;

    public DICommandedFollowOwnerGoal(TamableAnimal dragonfly, double speedModifier, float startDistance, float stopDistance, boolean canFly) {
        super(dragonfly, speedModifier, startDistance, stopDistance, canFly);
        this.dragonfly = (DragonflyEntity) dragonfly;
    }

    @Override
    public boolean m_8036_() {
        return DIServerPetCommandCompat.shouldFollow(this.dragonfly) && super.m_8036_();
    }

    @Override
    public boolean m_8045_() {
        return DIServerPetCommandCompat.shouldFollow(this.dragonfly) && super.m_8045_();
    }

    @Override
    public void m_8041_() {
        super.m_8041_();
        if (!DIServerPetCommandCompat.shouldFollow(this.dragonfly)) {
            this.dragonfly.m_21573_().m_26573_();
        }
    }
}
