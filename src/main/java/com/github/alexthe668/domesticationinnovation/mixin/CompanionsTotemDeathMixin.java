package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.CompanionsRespawnTotem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.event.CompanionsServerEvents", remap = false)
public class CompanionsTotemDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"), cancellable = true)
    private static void tl$coordinateTameRevival(@Coerce Object event, CallbackInfo ci) {
        if (CompanionsRespawnTotem.handlesNativeDeath(event)) ci.cancel();
    }
}
