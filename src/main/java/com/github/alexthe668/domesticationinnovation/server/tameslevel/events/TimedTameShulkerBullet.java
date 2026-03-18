package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;

public class TimedTameShulkerBullet extends ShulkerBullet {
    private static final int MAX_LIFETIME_TICKS = 80;

    public TimedTameShulkerBullet(Level level, LivingEntity owner, Entity finalTarget, Direction.Axis axis) {
        super(level, owner, finalTarget, axis);
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
