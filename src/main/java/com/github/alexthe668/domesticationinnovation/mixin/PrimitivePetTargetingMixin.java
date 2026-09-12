package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetingConditions.class)
public abstract class PrimitivePetTargetingMixin {
    @Shadow @Final private boolean isCombat;
    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void tl$excludeOwnedPrimitivePrey(LivingEntity attacker, LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (isCombat && PrimitiveMobsCompat.protectFromOtherPets(attacker, target)) cir.setReturnValue(false);
    }
}
