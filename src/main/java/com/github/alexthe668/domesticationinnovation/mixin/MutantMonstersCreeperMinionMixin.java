package com.github.alexthe668.domesticationinnovation.mixin;

import net.minecraft.world.entity.TamableAnimal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "fuzs.mutantmonsters.world.entity.CreeperMinion", remap = false)
public abstract class MutantMonstersCreeperMinionMixin {
    // Native tick discards the minion directly when this is false, bypassing death events.
    // Use its existing surviving explosion path, including the normal fuse reset.
    @Inject(method = "canExplodeContinuously", at = @At("HEAD"), cancellable = true)
    private void tl$surviveTamedExplosion(CallbackInfoReturnable<Boolean> cir) {
        if (((TamableAnimal) (Object) this).isTame()) {
            cir.setReturnValue(true);
        }
    }
}
