package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;

public class TimedTameWitherSkull extends WitherSkull {
    private static final int MAX_LIFETIME_TICKS = 80;

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
}
