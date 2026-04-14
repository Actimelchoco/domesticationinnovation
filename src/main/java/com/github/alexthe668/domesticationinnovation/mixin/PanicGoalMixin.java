package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PanicGoal.class)
public class PanicGoalMixin {

    @Shadow
    @Final
    private PathfinderMob mob;

    @Inject(
            at = {@At("HEAD")},
            remap = true,
            method = {"Lnet/minecraft/world/entity/ai/goal/PanicGoal;canUse()Z"},
            cancellable = true
    )
    private void di_canUse(CallbackInfoReturnable<Boolean> cir){
        if(mob instanceof ModifedToBeTameable mob && mob.isTame()){
            cir.setReturnValue(false);
            return;
        }
        if (shouldBlockFirePanic(mob)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
            at = {@At("HEAD")},
            remap = true,
            method = {"Lnet/minecraft/world/entity/ai/goal/PanicGoal;canContinueToUse()Z"},
            cancellable = true
    )
    private void di_canContinueToUse(CallbackInfoReturnable<Boolean> cir){
        if(mob instanceof ModifedToBeTameable mob && mob.isTame()){
            cir.setReturnValue(false);
            return;
        }
        if (shouldBlockFirePanic(mob)) {
            cir.setReturnValue(false);
        }

    }

    private static boolean shouldBlockFirePanic(PathfinderMob mob) {
        if (!(mob instanceof TamableAnimal tame) || !tame.isTame() || !mob.isOnFire()) {
            return false;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        return data != null && data.attributeLevels.getOrDefault("fire_resistance", 0) > 0;
    }
}
