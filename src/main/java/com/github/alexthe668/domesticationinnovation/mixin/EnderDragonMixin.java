package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends Mob {

    protected EnderDragonMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Inject(
            method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void di_allowTamedAttackDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source == null || source.getEntity() == null) {
            return;
        }
        if (TameableUtils.isTamed(source.getEntity())) {
            cir.setReturnValue(super.hurt(source, amount));
        }
    }
}
