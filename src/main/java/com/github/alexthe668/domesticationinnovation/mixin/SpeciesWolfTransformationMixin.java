package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.ninni.species.server.entity.ai.goal.TransformDuringFullMoonGoal", remap = false)
public abstract class SpeciesWolfTransformationMixin {
    @Shadow protected Wolf wolf;

    @Inject(method = "m_8056_", at = @At("HEAD"), cancellable = true, require = 0)
    private void tl$transform(CallbackInfo ci) {
        if (wolf.isTame()) {
            BewereagerCompat.transform(wolf);
            ci.cancel();
        }
    }
}
