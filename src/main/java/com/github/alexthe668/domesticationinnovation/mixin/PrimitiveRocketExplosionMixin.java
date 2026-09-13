package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "com.misanthropy.primitive_mobs.entity.monster.RocketCreeper", remap = false)
public class PrimitiveRocketExplosionMixin {
    @Redirect(method = "explodeOnImpact", at = @At(value = "INVOKE", target = "Lcom/misanthropy/primitive_mobs/entity/monster/RocketCreeper;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean tl$skipTamedSelfDamage(@Coerce Object rocket, DamageSource source, float damage) {
        LivingEntity living = (LivingEntity) rocket;
        return !TameEntityAdapter.isTame(living) && living.hurt(source, damage);
    }

    @Redirect(method = "explodeOnImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_254849_(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion tl$petBlast(Level level, Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction interaction) {
        float petRadius = source instanceof LivingEntity living
                ? com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat.petBlastRadius(living, radius) : radius;
        return level.explode(source, x, y, z, petRadius,
                TameEntityAdapter.isTame(source) ? Level.ExplosionInteraction.NONE : interaction);
    }
}
