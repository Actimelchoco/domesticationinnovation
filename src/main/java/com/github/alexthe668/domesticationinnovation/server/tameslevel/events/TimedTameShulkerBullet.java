package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public class TimedTameShulkerBullet extends ShulkerBullet {
    private static final int MAX_LIFETIME_TICKS = 80;
    private static final float LEVITATION_CHANCE = 0.10F;
    private final int abilityLevel;

    public TimedTameShulkerBullet(Level level, LivingEntity owner, Entity finalTarget, Direction.Axis axis, int abilityLevel) {
        super(level, owner, finalTarget, axis);
        this.abilityLevel = Math.max(1, abilityLevel);
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
    protected void onHitEntity(EntityHitResult result) {
        LivingEntity target = result.getEntity() instanceof LivingEntity living ? living : null;
        MobEffectInstance previous = target == null ? null : target.getEffect(MobEffects.LEVITATION);
        MobEffectInstance previousCopy = previous == null ? null : new MobEffectInstance(previous);
        super.onHitEntity(result);
        if (target == null || this.level().isClientSide) return;

        boolean proc = target.getMaxHealth() <= 50.0F && this.random.nextFloat() < LEVITATION_CHANCE;
        target.removeEffect(MobEffects.LEVITATION);
        if (proc) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION,
                    60 + this.abilityLevel * 20, Math.max(0, this.abilityLevel / 3)));
        } else if (previousCopy != null) {
            target.addEffect(previousCopy);
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        TimedTameImpactExplosion.explodeOnBlockImpact(this, 1.5F, 1.8D, 0.32D, 0.08D);
        this.discard();
    }
}
