package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class TimedTameArrow extends Arrow {
    private static final int MAX_LIFETIME_TICKS = 60;

    public TimedTameArrow(Level level, LivingEntity shooter) {
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
        TimedTameImpactExplosion.explodeOnBlockImpact(this, (float) Math.max(1.0D, this.getBaseDamage()), 1.8D, 0.35D, 0.08D);
        this.discard();
    }
}
