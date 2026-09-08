package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class TimedTameLlamaSpit extends LlamaSpit {
    private static final int MAX_LIFETIME_TICKS = 80;

    public TimedTameLlamaSpit(Level level, LivingEntity shooter) {
        super(net.minecraft.world.entity.EntityType.LLAMA_SPIT, level);
        this.setOwner(shooter);
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
        super.onHitBlock(result);
    }
}
