package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;

import java.util.List;

@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.FHauntedGuardEntity")
public abstract class LegendaryMonstersHauntedGuardMixin extends TamableAnimal {

    protected LegendaryMonstersHauntedGuardMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /**
     * @author Codex
     * @reason Allow same-owner duel opponents to be damaged by Haunted Guard area attack.
     */
    @Overwrite(remap = false)
    private void AreaAttack(float range, float height, float arc, float damage, int unusedCooldownTicks) {
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
                LivingEntity.class,
                this.getBoundingBox().inflate(range, height, range),
                entity -> entity != this && entity.isAlive() && entity.getY() <= this.getY() + height
        );
        float currentYaw = this.getYRot() % 360.0F;
        if (currentYaw < 0.0F) {
            currentYaw += 360.0F;
        }
        for (LivingEntity target : nearby) {
            float targetYaw = (float) ((Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * 57.29577951308232D) - 90.0D) % 360.0F;
            if (targetYaw < 0.0F) {
                targetYaw += 360.0F;
            }
            float relativeYaw = targetYaw - currentYaw;
            float distance = (float) Math.sqrt(
                    (target.getZ() - this.getZ()) * (target.getZ() - this.getZ())
                            + (target.getX() - this.getX()) * (target.getX() - this.getX())
            );
            if (!isWithinArcAndRange(distance, range, relativeYaw, arc)) {
                continue;
            }

            boolean duelOpponents = TameDuelManager.areDuelOpponents(this.getUUID(), target.getUUID());
            if (this.isAlliedTo(target) && !duelOpponents) {
                continue;
            }
            if (target == this) {
                continue;
            }
            if (target.getClass() == this.getClass() && !duelOpponents) {
                continue;
            }
            if (target instanceof TamableAnimal tameTarget
                    && tameTarget.isTame()
                    && tameTarget.getOwner() == this.getOwner()
                    && !duelOpponents) {
                continue;
            }

            float appliedDamage = Math.max(damage, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            DamageSource source = this.damageSources().mobAttack(this);
            target.hurt(source, appliedDamage);
        }
    }

    private static boolean isWithinArcAndRange(float distance, float maxDistance, float relativeYaw, float arc) {
        if (distance > maxDistance) {
            return false;
        }
        if (relativeYaw <= arc / 2.0F && relativeYaw >= -arc / 2.0F) {
            return true;
        }
        return relativeYaw < 360.0F - arc / 2.0F && relativeYaw > -360.0F + arc / 2.0F;
    }
}
