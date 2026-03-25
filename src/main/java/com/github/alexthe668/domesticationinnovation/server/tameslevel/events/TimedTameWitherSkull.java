package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class TimedTameWitherSkull extends WitherSkull {
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final double SPLASH_RADIUS = 2.0D;
    private static final float SPLASH_DAMAGE = 0.5F;
    private static final double SPLASH_KNOCKBACK = 0.45D;
    private static final double SPLASH_LIFT = 0.12D;

    public TimedTameWitherSkull(Level level, LivingEntity shooter, double offsetX, double offsetY, double offsetZ) {
        super(level, shooter, offsetX, offsetY, offsetZ);
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
        if (result.getType() == HitResult.Type.MISS) {
            return;
        }
        Entity excluded = result instanceof EntityHitResult entityHitResult ? entityHitResult.getEntity() : null;
        if (result instanceof EntityHitResult entityHitResult) {
            this.onHitEntity(entityHitResult);
        } else if (result instanceof BlockHitResult blockHitResult) {
            this.onHitBlock(blockHitResult);
        }
        if (!this.level().isClientSide) {
            this.applySplashDamage(excluded);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
            this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.8F, 1.15F);
            this.discard();
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        // Keep vanilla skull visuals/flight, but don't invoke the vanilla explosion path.
    }

    private void applySplashDamage(Entity excluded) {
        LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        if (shooter == null) {
            return;
        }
        for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SPLASH_RADIUS))) {
            if (!nearby.isAlive()) continue;
            if (nearby == shooter || nearby == excluded) continue;
            if (TameableUtils.shouldBlockOffensiveDiTarget(shooter, nearby)) continue;

            if (shooter instanceof TamableAnimal tame) {
                LevelSystem.trackDamage(nearby, tame);
            }
            nearby.hurt(this.damageSources().mobProjectile(this, shooter), SPLASH_DAMAGE);
            Vec3 push = nearby.position().subtract(this.position());
            if (push.lengthSqr() > 0.0001D) {
                double resistanceScale = Math.max(0.1D, 1.0D - Mth.clamp(nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D));
                Vec3 knockback = push.normalize().scale(SPLASH_KNOCKBACK * resistanceScale);
                nearby.push(knockback.x, SPLASH_LIFT * resistanceScale, knockback.z);
                nearby.hurtMarked = true;
            }
        }
    }
}
