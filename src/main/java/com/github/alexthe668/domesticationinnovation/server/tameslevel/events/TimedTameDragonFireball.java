package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class TimedTameDragonFireball extends DragonFireball {
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final double SPLASH_RADIUS = 2.0D;
    private static final double SPLASH_KNOCKBACK = 0.45D;
    private static final double SPLASH_LIFT = 0.12D;
    private static final float DIRECT_HIT_DAMAGE = 1.0F;

    public TimedTameDragonFireball(Level level, LivingEntity shooter, double offsetX, double offsetY, double offsetZ) {
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
        if (this.level().isClientSide || result.getType() == HitResult.Type.MISS) {
            return;
        }

        LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        float damage = DIRECT_HIT_DAMAGE;
        int levelValue = 1;
        float ghastCastDamage = 2.75F;
        if (shooter instanceof TamableAnimal tame) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "dragon_fireball"));
                ghastCastDamage = TameAbilityEvents.offensiveAbilityCastDamage(data, "ghast_fireball", levelValue);
            }
        }

        if (shooter != null) {
            for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(SPLASH_RADIUS))) {
                if (!nearby.isAlive() || nearby == shooter) continue;
                if (TameableUtils.shouldBlockOffensiveDiTarget(shooter, nearby)) continue;

                if (shooter instanceof TamableAnimal tame) {
                    LevelSystem.trackDamage(nearby, tame);
                }
                nearby.hurt(this.damageSources().mobProjectile(this, shooter), damage);
                Vec3 push = nearby.position().subtract(this.position());
                if (push.lengthSqr() > 0.0001D) {
                    double resistanceScale = Math.max(0.1D, 1.0D - Mth.clamp(nearby.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D));
                    Vec3 knockback = push.normalize().scale(SPLASH_KNOCKBACK * resistanceScale);
                    nearby.push(knockback.x, SPLASH_LIFT * resistanceScale, knockback.z);
                    nearby.hurtMarked = true;
                }
            }
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, this.getX(), this.getY(), this.getZ(), 24, 0.35D, 0.15D, 0.35D, 0.02D);
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        if (shooter != null) {
            float cloudDamage = dragonCloudDamagePerPulse(ghastCastDamage, levelValue);
            TimedTameDragonBreathCloud cloud = new TimedTameDragonBreathCloud(this.level(), shooter, this.getX(), this.getY(), this.getZ(), levelValue, cloudDamage);
            this.level().addFreshEntity(cloud);
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 0.9F, 0.9F);
        this.discard();
    }

    private static float dragonCloudDamagePerPulse(float ghastCastDamage, int levelValue) {
        int interval = 10;
        int lifetime = MAX_LIFETIME_TICKS + Math.max(0, Math.max(1, levelValue) - 30);
        int pulses = Math.max(1, (lifetime - 1) / interval);
        float targetTotalCloudDamage = ghastCastDamage * 1.4F + 2.5F;
        return targetTotalCloudDamage / pulses;
    }
}
