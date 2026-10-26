package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Inject(method = "hurtArmor", at = @At("HEAD"))
    private void tl$wearAnimightDuelArmor(net.minecraft.world.damagesource.DamageSource source, float damage,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents.damageDuelArmor((LivingEntity)(Object)this, source, damage);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void tl$protectFriendlyOwnerDamage(net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.blocks(this, source)
                || com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.RankedRefereeService.blocks(this, source)) cir.setReturnValue(false);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void tl$protectFriendlyOwnerEffects(net.minecraft.world.effect.MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!effect.getEffect().isBeneficial()
                && (com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.blocks(this, source)
                    || com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.RankedRefereeService.blocks(this, source))) cir.setReturnValue(false);
    }

    public LivingEntityMixin(EntityType<?> entityType, Level lvl) {
        super(entityType, lvl);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/LivingEntity;getWaterSlowDown()F"},
            remap = true,
            at = @At(value = "TAIL"),
            cancellable = true
    )
    private void di_getWaterSlowdown(CallbackInfoReturnable<Float> cir) {
        if(TameableUtils.isTamed(this) && isLandAndSea()){
            cir.setReturnValue(0.98F);
        }
    }

    private boolean isLandAndSea(){
        return TameableUtils.hasEnchant(((LivingEntity) (Entity)this), DIEnchantmentRegistry.AMPHIBIOUS);
    }


}
