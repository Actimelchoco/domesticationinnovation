package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimDisplayProtection;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Both overrides bypass LivingAttackEvent; frames can pop their contents before super.hurt.
@Mixin({ArmorStand.class, ItemFrame.class})
public abstract class ClaimDisplayDamageMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"), cancellable = true)
    private void ftbc$guardDisplay(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (ClaimDisplayProtection.blockDamage((Entity)(Object)this, source)) cir.setReturnValue(false);
    }
}
