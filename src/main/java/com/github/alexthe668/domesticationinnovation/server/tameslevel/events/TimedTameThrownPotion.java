package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

import java.util.UUID;

public class TimedTameThrownPotion extends ThrownPotion {
    private static final int MAX_LIFETIME_TICKS = 60;

    public TimedTameThrownPotion(Level level, LivingEntity shooter) {
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
    protected void onHit(HitResult result) {
        if (!this.level().isClientSide) {
            applyFriendlySplash();
            this.level().levelEvent(2002, BlockPos.containing(this.position()), PotionUtils.getColor(this.getItem()));
            this.discard();
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    private void applyFriendlySplash() {
        ItemStack potion = this.getItem();
        LivingEntity thrower = this.getOwner() instanceof LivingEntity living ? living : null;
        AABB box = this.getBoundingBox().inflate(4.0D, 2.0D, 4.0D);

        for (LivingEntity target : this.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!canAffectFriendly(thrower, target)) {
                continue;
            }
            double distanceSq = this.distanceToSqr(target);
            if (distanceSq >= 16.0D) {
                continue;
            }
            double scale = target == thrower ? 1.0D : 1.0D - Math.sqrt(distanceSq) / 4.0D;
            if (scale <= 0.0D) {
                continue;
            }
            for (MobEffectInstance effect : PotionUtils.getMobEffects(potion)) {
                if (effect.getEffect().isInstantenous()) {
                    effect.getEffect().applyInstantenousEffect(this, this.getOwner(), target, effect.getAmplifier(), scale);
                    continue;
                }
                int duration = (int) (scale * effect.getDuration() + 0.5D);
                if (duration <= 20) {
                    continue;
                }
                target.addEffect(new MobEffectInstance(
                        effect.getEffect(),
                        duration,
                        effect.getAmplifier(),
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ), this.getEffectSource());
            }
        }
    }

    private boolean canAffectFriendly(LivingEntity thrower, LivingEntity target) {
        if (target == null || !target.isAlive()) {
            return false;
        }
        if (target == thrower) {
            return true;
        }
        if (target instanceof Player player) {
            return !areDuelOpponents(thrower, player);
        }
        if (target instanceof TamableAnimal tameTarget && tameTarget.isTame()) {
            return !areDuelOpponents(thrower, tameTarget);
        }
        return false;
    }

    private boolean areDuelOpponents(LivingEntity thrower, LivingEntity target) {
        if (thrower == null || target == null) {
            return false;
        }
        return TameDuelManager.areDuelOpponents(thrower.getUUID(), target.getUUID());
    }
}
