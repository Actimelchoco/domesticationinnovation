package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.SkeloraptorAttacks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets="net.miauczel.legendary_monsters.entity.AnimatedMonster.Projectile.ShockwaveEntity", remap=false)
public abstract class LegendaryMonstersShockwaveMixin {
    @Group(name="tl$nativeBase", min=2, max=2)
    @Redirect(method="damage", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$baseDev(LivingEntity target, DamageSource source, float damage) {
        return SkeloraptorAttacks.hurtNativeBase(target, source, damage);
    }

    @Group(name="tl$nativeBase", min=2, max=2)
    @Redirect(method="damage", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$baseRelease(LivingEntity target, DamageSource source, float damage) {
        return SkeloraptorAttacks.hurtNativeBase(target, source, damage);
    }
}
