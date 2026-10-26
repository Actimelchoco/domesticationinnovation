package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.polaris.animights.common.entity.AbstractCompanionEntity", remap = false)
public class AnimightDuelRangedWearMixin {
    // The optional mod uses Mojmap names in development and SRG names in releases.
    @Inject(method = {"performRangedAttack", "m_6504_"}, at = @At("TAIL"), remap = false, require = 1)
    private void tl$wearBow(LivingEntity target, float strength, CallbackInfo ci) {
        AnimightEquipmentEvents.damageDuelWeapon((LivingEntity)(Object)this, true);
    }
}
