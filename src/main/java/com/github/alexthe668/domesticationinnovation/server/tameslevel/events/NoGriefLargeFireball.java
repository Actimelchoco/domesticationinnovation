package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class NoGriefLargeFireball extends LargeFireball {
    private static final int MAX_LIFETIME_TICKS = 80;
    private final int noGriefExplosionPower;

    public NoGriefLargeFireball(Level level, LivingEntity shooter, double offsetX, double offsetY, double offsetZ, int explosionPower) {
        super(level, shooter, offsetX, offsetY, offsetZ, explosionPower);
        this.noGriefExplosionPower = explosionPower;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        float damage = 6.0F + this.noGriefExplosionPower * 2.0F;
        if (TameEntityAdapter.isTame(shooter)) {
            TameData data = TameRegistry.get(shooter.getUUID());
            if (data != null) {
                int level = Math.max(1, LevelSystem.getAbilityLevel(data, "ghast_fireball"));
                damage = TameAbilityEvents.offensiveAbilityCastDamage(data, "ghast_fireball", level);
            }
        }
        for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D))) {
            if (!nearby.isAlive()) continue;
            if (nearby == shooter) continue;
            if (shooter != null && TameableUtils.shouldBlockOffensiveDiTarget(shooter, nearby)) continue;

            if (TameEntityAdapter.isTame(shooter)) {
                LevelSystem.trackDamage(nearby, shooter);
            }
            nearby.hurt(this.damageSources().mobProjectile(this, shooter), damage);
            Vec3 push = nearby.position().subtract(this.position());
            if (push.lengthSqr() > 0.0001D) {
                double resistanceScale = Math.max(0.1D, 1.0D - Mth.clamp(nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D));
                Vec3 knockback = push.normalize().scale((0.85D + this.noGriefExplosionPower * 0.15D) * resistanceScale);
                nearby.push(knockback.x, 0.22D * resistanceScale, knockback.z);
                nearby.hurtMarked = true;
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);
        this.discard();
    }

}
