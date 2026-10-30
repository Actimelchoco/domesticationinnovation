package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.SkeloraptorAttacks;

/** Preserve native attack damage and timing while allowing assigned duel opponents. */
@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.SkeloraptorEntity")
public abstract class LegendaryMonstersSkeloraptorMixin extends TamableAnimal {
    protected LegendaryMonstersSkeloraptorMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Group(name="tl$nativeBase", min=1, max=1)
    @Redirect(method="AreaAttack", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$baseDev(LivingEntity target, DamageSource source, float damage) {
        return SkeloraptorAttacks.hurtNativeBase(target, source, damage);
    }

    @Group(name="tl$nativeBase", min=1, max=1)
    @Redirect(method="AreaAttack", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z", remap=false), remap=false, require=0)
    private boolean tl$baseRelease(LivingEntity target, DamageSource source, float damage) {
        return SkeloraptorAttacks.hurtNativeBase(target, source, damage);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        if (other != null && TameDuelManager.areDuelOpponents(this.getUUID(), other.getUUID())) {
            return false;
        }
        return super.isAlliedTo(other);
    }
}
