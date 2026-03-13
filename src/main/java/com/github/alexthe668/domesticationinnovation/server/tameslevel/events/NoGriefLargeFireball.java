package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public class NoGriefLargeFireball extends LargeFireball {
    private final int noGriefExplosionPower;

    public NoGriefLargeFireball(Level level, LivingEntity shooter, double offsetX, double offsetY, double offsetZ, int explosionPower) {
        super(level, shooter, offsetX, offsetY, offsetZ, explosionPower);
        this.noGriefExplosionPower = explosionPower;
    }

    @Override
    protected void onHit(HitResult result) {
        if (this.level().isClientSide) {
            return;
        }

        LivingEntity shooter = this.getOwner() instanceof LivingEntity living ? living : null;
        float damage = 6.0F + this.noGriefExplosionPower * 2.0F;
        if (shooter instanceof TamableAnimal tame) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                int level = Math.max(1, LevelSystem.getAbilityLevel(data, "ghast_fireball"));
                damage = TameAbilityEvents.offensiveAbilityCastDamage(data, "ghast_fireball", level);
            }
        }
        for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0D))) {
            if (!nearby.isAlive()) continue;
            if (nearby == shooter) continue;
            if (shooter instanceof TamableAnimal tame && isFriendly(tame, nearby)) continue;

            if (shooter instanceof TamableAnimal tame) {
                LevelSystem.trackDamage(nearby, tame);
            }
            nearby.hurt(this.damageSources().mobProjectile(this, shooter), damage);
        }

        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.0F, 1.0F);
        this.discard();
    }

    private static boolean isFriendly(TamableAnimal tame, Entity entity) {
        if (!TLAdminRuntimeSettings.friendlyFireEnabled() && (entity instanceof Player || entity instanceof TamableAnimal)) {
            return true;
        }
        if (entity == tame) return true;
        if (entity instanceof Player player) {
            return tame.getOwnerUUID() != null && tame.getOwnerUUID().equals(player.getUUID());
        }
        if (entity instanceof TamableAnimal otherTame && otherTame.isTame()) {
            return tame.getOwnerUUID() != null && tame.getOwnerUUID().equals(otherTame.getOwnerUUID());
        }
        return false;
    }
}
