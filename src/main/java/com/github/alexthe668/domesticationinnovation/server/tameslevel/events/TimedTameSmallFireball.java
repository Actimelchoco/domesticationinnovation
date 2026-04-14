package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class TimedTameSmallFireball extends SmallFireball {
    private static final int MAX_LIFETIME_TICKS = 80;

    public TimedTameSmallFireball(Level level, LivingEntity shooter, double offsetX, double offsetY, double offsetZ) {
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
    protected void onHitBlock(BlockHitResult result) {
        // Keep no-grief behavior, but still create an impact explosion effect.
        TimedTameImpactExplosion.explodeOnBlockImpact(this, 1.5F, 1.8D, 0.35D, 0.08D);
        this.discard();
    }
}
