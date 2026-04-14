package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class TimedTameSnowball extends Snowball {
    private static final int MAX_LIFETIME_TICKS = 60;

    public TimedTameSnowball(Level level, LivingEntity shooter) {
        super(level, shooter);
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
    protected void onHitBlock(BlockHitResult result) {
        TimedTameImpactExplosion.explodeOnBlockImpact(this, 1.0F, 1.6D, 0.25D, 0.05D);
        this.discard();
    }
}
