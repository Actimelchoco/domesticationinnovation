package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Owned baby spiders must not abandon combat because of the wild daylight roll. */
@Pseudo
@Mixin(targets = "com.misanthropy.primitive_mobs.entity.ai.SpiderAttackGoal")
public abstract class PrimitiveSpiderAttackMixin extends MeleeAttackGoal {
    protected PrimitiveSpiderAttackMixin(PathfinderMob mob, double speed, boolean longMemory) {
        super(mob, speed, longMemory);
    }

    @Inject(method = "m_8045_", at = @At("HEAD"), cancellable = true, remap = false)
    private void tl$keepOwnedCombatTarget(CallbackInfoReturnable<Boolean> cir) {
        if (PrimitiveMobsCompat.isPrimitivePet(mob) && ((ModifedToBeTameable) mob).isTame()) {
            var pet = (ModifedToBeTameable) mob;
            var target = mob.getTarget();
            cir.setReturnValue(!pet.isStayingStill() && target != null && target.isAlive()
                    && pet.isValidAttackTarget(target) && super.canContinueToUse());
        }
    }
}
