package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HunterBeltCompat;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "it.hurts.sskirillss.relics.items.relics.belt.HunterBeltItem$HunterBeltEvents", remap = false)
public abstract class RelicsHunterBeltEventsMixin {
    @Inject(method = "onLivingDamage", at = @At("HEAD"), cancellable = true, require = 0)
    private static void tl$stackWornBelts(LivingHurtEvent event, CallbackInfo ci) {
        if (HunterBeltCompat.deferNativeDamage(event)) ci.cancel();
    }
}
