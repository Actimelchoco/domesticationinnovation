package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;

public class TimedTameDragonBreathCloud extends AreaEffectCloud {
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final int DAMAGE_INTERVAL_TICKS = 10;
    private static final float CLOUD_DAMAGE = 1.0F;

    public TimedTameDragonBreathCloud(Level level, LivingEntity owner, double x, double y, double z) {
        super(EntityType.AREA_EFFECT_CLOUD, level);
        this.setPos(x, y, z);
        this.setOwner(owner);
        this.setParticle(ParticleTypes.DRAGON_BREATH);
        this.setRadius(2.0F);
        this.setDuration(MAX_LIFETIME_TICKS);
        this.setRadiusPerTick(0.0F);
        this.setWaitTime(0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount >= MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        if (this.tickCount % DAMAGE_INTERVAL_TICKS != 0) {
            return;
        }

        LivingEntity owner = this.getOwner();
        if (owner == null) {
            return;
        }

        for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(this.getRadius()))) {
            if (!nearby.isAlive() || nearby == owner) continue;
            if (TameableUtils.shouldBlockOffensiveDiTarget(owner, nearby)) continue;

            if (owner instanceof TamableAnimal tame) {
                LevelSystem.trackDamage(nearby, tame);
            }
            nearby.hurt(this.damageSources().indirectMagic(this, owner), CLOUD_DAMAGE);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
