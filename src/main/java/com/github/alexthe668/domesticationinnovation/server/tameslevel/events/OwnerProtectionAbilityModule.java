package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public final class OwnerProtectionAbilityModule {

    public interface Hooks {
        void debugAbilityUse(TamableAnimal tame, String ability);

        void applySupportActivationVisual(TamableAnimal tame, String source);
    }

    private OwnerProtectionAbilityModule() {}

    public static void onOwnerHurt(ServerPlayer owner, LivingHurtEvent event, Hooks hooks) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();

        List<TamableAnimal> nearbyTames = collectOwnedNearbyTames(level, owner, 3.0D);
        if (nearbyTames.isEmpty()) return;

        for (TamableAnimal tame : nearbyTames) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;

            handleGuardianRepulse(owner, tame, data, now, hooks);
            handleSkyLaunch(owner, tame, data, now, hooks);
        }
    }

    public static void onTick(TamableAnimal tame, TameData data) {
        if (!LevelSystem.hasAbility(data, "last_stand_fury")) return;
        LivingEntity owner = tame.getOwner();
        if (owner == null || !owner.isAlive()) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "last_stand_fury"));
        float ownerHpPct = Mth.clamp(owner.getHealth() / Math.max(1.0F, owner.getMaxHealth()), 0.0F, 1.0F);
        float missing = 1.0F - ownerHpPct;
        if (missing <= 0.01F) return;

        int strengthAmp = Math.min(4, Math.max(0, Mth.floor(missing * (levelValue + 1))));
        int speedAmp = Math.min(4, Math.max(0, Mth.floor(missing * (levelValue + 2))));
        if (strengthAmp <= 0 && speedAmp <= 0) return;

        tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, strengthAmp, false, false, true));
        tame.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedAmp, false, false, true));
    }

    public static void onTameHurt(TamableAnimal tame, TameData data, LivingHurtEvent event, Hooks hooks) {
        handleShieldBlock(tame, data, event, hooks);
    }

    private static void handleGuardianRepulse(ServerPlayer owner, TamableAnimal tame, TameData data, long now, Hooks hooks) {
        if (!LevelSystem.hasAbility(data, "guardian_repulse")) return;
        if (!tame.isAlive() || tame.distanceToSqr(owner) > 9.0D) return;
        if (!isReady(data, "guardian_repulse_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_repulse"));
        double radius = 2.8D + levelValue * 0.25D;
        int affected = 0;

        for (LivingEntity nearby : owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(radius))) {
            if (!(nearby instanceof Monster)) continue;
            if (!nearby.isAlive()) continue;

            Vec3 push = nearby.position().subtract(owner.position());
            if (push.lengthSqr() < 1.0E-4D) {
                push = new Vec3(0.001D, 0.0D, 0.001D);
            }
            Vec3 knock = push.normalize().scale(0.9D + levelValue * 0.08D);
            nearby.push(knock.x, 0.35D + levelValue * 0.03D, knock.z);
            nearby.hurtMarked = true;
            affected++;
        }

        long cooldownTicks = Math.max(20L, 800L - (long) Math.max(0, levelValue - 1) * 60L);
        setCooldown(data, "guardian_repulse_tick", now + cooldownTicks);
        if (affected > 0 && tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY(0.8D), owner.getZ(), 16, radius * 0.20D, 0.3D, radius * 0.20D, 0.03D);
            level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 0.9F, 1.0F);
            hooks.debugAbilityUse(tame, "guardian_repulse");
        }
    }

    private static void handleSkyLaunch(ServerPlayer owner, TamableAnimal tame, TameData data, long now, Hooks hooks) {
        if (!LevelSystem.hasAbility(data, "sky_launch")) return;
        if (!tame.isAlive() || tame.distanceToSqr(owner) > 9.0D) return;
        if (!isReady(data, "sky_launch_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "sky_launch"));
        double radius = 2.5D + levelValue * 0.20D;
        double lift = 0.30D + levelValue * 0.15D;
        int affected = 0;

        for (LivingEntity nearby : owner.level().getEntitiesOfClass(LivingEntity.class, owner.getBoundingBox().inflate(radius))) {
            if (!(nearby instanceof Monster)) continue;
            if (!nearby.isAlive()) continue;

            Vec3 push = nearby.position().subtract(owner.position());
            Vec3 horizontal = push.lengthSqr() > 1.0E-4D ? push.normalize().scale(0.20D + levelValue * 0.03D) : Vec3.ZERO;
            nearby.push(horizontal.x, lift, horizontal.z);
            nearby.fallDistance = Math.max(nearby.fallDistance, 3.0F + levelValue * 1.5F);
            nearby.hurtMarked = true;
            affected++;
        }

        setCooldown(data, "sky_launch_tick", now + 240L);
        if (affected > 0 && tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, owner.getX(), owner.getY(0.7D), owner.getZ(), 6, radius * 0.15D, 0.2D, radius * 0.15D, 0.0D);
            level.playSound(null, owner.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 0.8F, 1.1F);
            hooks.debugAbilityUse(tame, "sky_launch");
        }
    }

    private static void handleShieldBlock(TamableAnimal tame, TameData data, LivingHurtEvent event, Hooks hooks) {
        if (!LevelSystem.hasAbility(data, "shield_block")) return;
        if (event.getAmount() <= 0.0F) return;

        long now = tame.level().getGameTime();
        if (!isReady(data, "shield_block_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shield_block"));
        float reduction = Math.min(0.95F, 0.65F + (levelValue - 1) * 0.03F);
        event.setAmount(event.getAmount() * (1.0F - reduction));

        long cooldownTicks = Math.max(20L, 200L - (long) Math.max(0, levelValue - 1) * 20L);
        setCooldown(data, "shield_block_tick", now + cooldownTicks);
        if (tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CRIT, tame.getX(), tame.getY(0.6D), tame.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
            level.playSound(null, tame.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        hooks.debugAbilityUse(tame, "shield_block");
    }

    private static List<TamableAnimal> collectOwnedNearbyTames(ServerLevel level, ServerPlayer owner, double radius) {
        if (owner == null) {
            return List.of();
        }
        AABB box = owner.getBoundingBox().inflate(radius);
        List<TamableAnimal> tames = level.getEntitiesOfClass(TamableAnimal.class, box, tame ->
                tame.isTame()
                        && tame.isAlive()
                        && owner.getUUID().equals(tame.getOwnerUUID())
                        && TameRegistry.get(tame.getUUID()) != null
        );
        return tames == null ? List.of() : tames;
    }

    private static boolean isReady(TameData data, String key, long now) {
        return now >= data.cooldowns.getOrDefault(key, 0L);
    }

    private static void setCooldown(TameData data, String key, long tick) {
        data.cooldowns.put(key, tick);
    }
}
