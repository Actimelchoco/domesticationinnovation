package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class TimedTameTrident extends ThrownTrident {
    private static final int MAX_LIFETIME_TICKS = 80;

    public TimedTameTrident(Level level, LivingEntity shooter, ItemStack stack) {
        super(level, shooter, stack);
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
        TimedTameImpactExplosion.explodeOnBlockImpact(this, 2.5F, 2.0D, 0.45D, 0.10D);
        this.discard();
    }
}
