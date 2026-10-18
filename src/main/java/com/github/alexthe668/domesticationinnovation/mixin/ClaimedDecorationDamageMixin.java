package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.ClaimDecorationProtection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** These hurt overrides bypass LivingAttackEvent; frames also pop their item before super. */
@Mixin({ArmorStand.class, HangingEntity.class, ItemFrame.class, Boat.class, AbstractMinecart.class, EndCrystal.class})
public abstract class ClaimedDecorationDamageMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void tl$protectClaimedDecoration(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (ClaimDecorationProtection.blockDamage((Entity)(Object)this, source)) cir.setReturnValue(false);
    }
}
