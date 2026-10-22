package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's missing-owner fallback must not repeatedly stop an awake guardian. */
@Mixin(SitWhenOrderedToGoal.class)
public abstract class GuardianSitGoalMixin {
    @Shadow @Final private TamableAnimal mob;

    @Inject(method={"canUse", "canContinueToUse"},at=@At("HEAD"),cancellable=true)
    private void di_awakeGuardianDoesNotSit(CallbackInfoReturnable<Boolean> cir) {
        if(mob.level().isClientSide)return;
        var data=TameRegistry.get(mob.getUUID());
        if(data!=null&&data.hasHome&&data.movementOrder==3&&!data.rosterBodyguard
                &&!data.guardianResting&&!data.hungerForcedSit&&!data.dead&&!data.stored) cir.setReturnValue(false);
    }
}
