package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.VallumraptorWeapons;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Group;

@Pseudo
@Mixin(targets="com.github.alexmodguy.alexscaves.server.entity.ai.VallumraptorMeleeGoal", remap=false)
public class AlexsCavesVallumraptorMeleeMixin {
    @Group(name="tl$weaponHit", min=1, max=1)
    @Redirect(method="checkAndDealDamage", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$weaponHitDev(LivingEntity target, DamageSource source, float damage) {
        return tl$hit(target, source, damage);
    }

    @Group(name="tl$weaponHit", min=1, max=1)
    @Redirect(method="checkAndDealDamage", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$weaponHitRelease(LivingEntity target, DamageSource source, float damage) {
        return tl$hit(target, source, damage);
    }

    private static boolean tl$hit(LivingEntity target, DamageSource source, float damage) {
        // Keep native animation, range, visibility, and hit timing. Mob melee adds
        // weapon attributes, damage enchantments, knockback, fire, and enchant hooks.
        if (source.getEntity() instanceof Mob raptor && VallumraptorWeapons.isArmed(raptor)) return raptor.doHurtTarget(target);
        return target.hurt(source, damage);
    }
}
