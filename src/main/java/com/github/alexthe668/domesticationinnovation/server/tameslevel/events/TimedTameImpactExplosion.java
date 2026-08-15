package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

final class TimedTameImpactExplosion {
    private TimedTameImpactExplosion() {
    }

    static void explodeOnBlockImpact(Projectile projectile, float damage, double radius, double knockback, double lift) {
        if (projectile == null || projectile.level().isClientSide) {
            return;
        }
        if (!(projectile.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        for (LivingEntity nearby : projectile.level().getEntitiesOfClass(LivingEntity.class, projectile.getBoundingBox().inflate(radius))) {
            if (!nearby.isAlive() || nearby == shooter) {
                continue;
            }
            if (TameableUtils.shouldBlockOffensiveDiTarget(shooter, nearby)) {
                continue;
            }
            if (TameEntityAdapter.isTame(shooter)) {
                LevelSystem.trackDamage(nearby, shooter);
            }
            nearby.hurt(projectile.damageSources().mobProjectile(projectile, shooter), damage);
            Vec3 push = nearby.position().subtract(projectile.position());
            if (push.lengthSqr() > 0.0001D) {
                double resistanceScale = Math.max(0.1D, 1.0D - Mth.clamp(nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D));
                Vec3 applied = push.normalize().scale(knockback * resistanceScale);
                nearby.push(applied.x, lift * resistanceScale, applied.z);
                nearby.hurtMarked = true;
            }
        }
        if (projectile.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, projectile.getX(), projectile.getY(), projectile.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        projectile.level().playSound(null, projectile.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.8F, 1.1F);
    }
}
