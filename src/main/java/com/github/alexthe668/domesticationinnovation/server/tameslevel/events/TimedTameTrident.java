package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    protected void onHit(HitResult result) {
        if (this.level().isClientSide || result.getType() == HitResult.Type.MISS) {
            return;
        }
        LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        if (result instanceof EntityHitResult entityHitResult
                && shooter != null
                && entityHitResult.getEntity() instanceof LivingEntity target) {
            if (!TameableUtils.shouldBlockOffensiveDiTarget(shooter, target)) {
                if (shooter instanceof TamableAnimal tame) {
                    LevelSystem.trackDamage(target, tame);
                }
                float damage = (float) Math.max(0.0D, this.getBaseDamage());
                if (damage <= 0.0F) {
                    damage = 4.0F;
                }
                target.hurt(this.damageSources().mobProjectile(this, shooter), damage);
                Vec3 motion = this.getDeltaMovement();
                if (motion.lengthSqr() > 0.0001D) {
                    Vec3 knockback = motion.normalize().scale(0.35D);
                    target.push(knockback.x, 0.08D, knockback.z);
                    target.hurtMarked = true;
                }
            }
            this.discard();
            return;
        }
        if (result instanceof BlockHitResult) {
            TimedTameImpactExplosion.explodeOnBlockImpact(this, 2.5F, 2.0D, 0.45D, 0.10D);
        }
        this.discard();
    }
}
