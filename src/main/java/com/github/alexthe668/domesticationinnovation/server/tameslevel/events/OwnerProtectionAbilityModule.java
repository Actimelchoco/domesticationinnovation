package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.List;

public final class OwnerProtectionAbilityModule {
    public interface Hooks {
        void debugAbilityUse(LivingEntity tame, String ability);

        void applySupportActivationVisual(LivingEntity tame, String source);

        void grantSupportXp(LivingEntity supporter, TameData data, LivingEntity beneficiary, long now, float effectiveAmount, float scale);
    }

    private OwnerProtectionAbilityModule() {}

    public static void onOwnerHurt(ServerPlayer owner, LivingHurtEvent event, Hooks hooks) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();

        List<LivingEntity> nearbyTames = collectOwnedNearbyTames(level, owner, 3.0D);
        if (nearbyTames.isEmpty()) return;

        LivingEntity bestShieldBlocker = null;
        TameData bestShieldData = null;
        int bestShieldLevel = 0;
        double bestShieldDistance = Double.MAX_VALUE;

        for (LivingEntity tame : nearbyTames) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            if (!LevelSystem.hasAbility(data, "shield_block")) continue;
            if (!isReady(data, "shield_block_tick", now)) continue;
            int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shield_block"));
            double distance = tame.distanceToSqr(owner);
            if (bestShieldBlocker == null || levelValue > bestShieldLevel || (levelValue == bestShieldLevel && distance < bestShieldDistance)) {
                bestShieldBlocker = tame;
                bestShieldData = data;
                bestShieldLevel = levelValue;
                bestShieldDistance = distance;
            }
        }

        if (bestShieldBlocker != null && bestShieldData != null) {
            handleShieldBlockOwner(level, owner, bestShieldBlocker, bestShieldData, event, now, hooks);
        }

        for (LivingEntity tame : nearbyTames) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) continue;
            handleGuardianRepulse(owner, tame, data, now, hooks);
            handleSkyLaunch(owner, tame, data, now, hooks);
        }
    }

    public static void onTick(LivingEntity tame, TameData data) {
        if (!LevelSystem.hasAbility(data, "last_stand_fury")) return;
        LivingEntity owner = TameEntityAdapter.owner(tame);
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

    public static void onTameHurt(LivingEntity tame, TameData data, LivingHurtEvent event, Hooks hooks) {
        handleShieldBlock(tame, data, event, hooks);
    }

    private static void handleGuardianRepulse(ServerPlayer owner, LivingEntity tame, TameData data, long now, Hooks hooks) {
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

        long cooldownTicks = 400L;
        setAbilityCooldown(data, "guardian_repulse_tick", now, cooldownTicks);
        if (affected > 0 && tame.level() instanceof ServerLevel level) {
            hooks.grantSupportXp(tame, data, owner, now, affected * (1.0F + 0.25F * levelValue), 0.5F);
            level.sendParticles(ParticleTypes.CLOUD, owner.getX(), owner.getY(0.8D), owner.getZ(), 16, radius * 0.20D, 0.3D, radius * 0.20D, 0.03D);
            level.playSound(null, owner.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 0.9F, 1.0F);
            hooks.debugAbilityUse(tame, "guardian_repulse");
        }
    }

    private static void handleSkyLaunch(ServerPlayer owner, LivingEntity tame, TameData data, long now, Hooks hooks) {
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

        setAbilityCooldown(data, "sky_launch_tick", now, 240L);
        if (affected > 0 && tame.level() instanceof ServerLevel level) {
            hooks.grantSupportXp(tame, data, owner, now, affected * (1.5F + 0.25F * levelValue), 0.5F);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, owner.getX(), owner.getY(0.7D), owner.getZ(), 6, radius * 0.15D, 0.2D, radius * 0.15D, 0.0D);
            level.playSound(null, owner.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.NEUTRAL, 0.8F, 1.1F);
            hooks.debugAbilityUse(tame, "sky_launch");
        }
    }

    private static void handleShieldBlock(LivingEntity tame, TameData data, LivingHurtEvent event, Hooks hooks) {
        if (!LevelSystem.hasAbility(data, "shield_block")) return;
        if (event.getAmount() <= 0.0F) return;

        long now = tame.level().getGameTime();
        if (!isReady(data, "shield_block_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shield_block"));
        float reduction = Math.min(0.95F, 0.65F + (levelValue - 1) * 0.03F);
        float before = event.getAmount();
        event.setAmount(event.getAmount() * (1.0F - reduction));

        long cooldownTicks = Math.max(30L, 300L - (long) Math.max(0, levelValue - 1) * 20L);
        setAbilityCooldown(data, "shield_block_tick", now, cooldownTicks);
        TameEntityAdapter.setTarget(tame, null);
        if (tame instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
        TameableUtils.setImmuneTime(tame, Math.max(TameableUtils.getImmuneTime(tame), 20));
        if (tame.level() instanceof ServerLevel level) {
            hooks.grantSupportXp(tame, data, tame, now, Math.max(0.0F, before - event.getAmount()), 0.75F);
            level.sendParticles(ParticleTypes.CRIT, tame.getX(), tame.getY(0.6D), tame.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
            level.playSound(null, tame.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        hooks.debugAbilityUse(tame, "shield_block");
    }

    private static void handleShieldBlockOwner(ServerLevel level, ServerPlayer owner, LivingEntity tame, TameData data, LivingHurtEvent event, long now, Hooks hooks) {
        if (event.getAmount() <= 0.0F) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shield_block"));
        float reduction = Math.min(0.95F, 0.65F + (levelValue - 1) * 0.03F);
        float before = event.getAmount();
        event.setAmount(event.getAmount() * (1.0F - reduction));

        long cooldownTicks = Math.max(30L, 300L - (long) Math.max(0, levelValue - 1) * 20L);
        setAbilityCooldown(data, "shield_block_tick", now, cooldownTicks);
        dashShieldBlockToOwner(level, owner, tame, data, levelValue);
        TameEntityAdapter.setTarget(tame, null);
        if (tame instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
        TameableUtils.setImmuneTime(tame, Math.max(TameableUtils.getImmuneTime(tame), 20));
        hooks.applySupportActivationVisual(tame, "shield_block");
        hooks.grantSupportXp(tame, data, owner, now, Math.max(0.0F, before - event.getAmount()), 0.75F);
        level.sendParticles(ParticleTypes.CRIT, owner.getX(), owner.getY(0.6D), owner.getZ(), 8, 0.3D, 0.3D, 0.3D, 0.02D);
        level.playSound(null, owner.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        hooks.debugAbilityUse(tame, "shield_block");
    }

    private static void dashShieldBlockToOwner(ServerLevel level, ServerPlayer owner, LivingEntity tame, TameData data, int levelValue) {
        Vec3 start = tame.position();
        Vec3 ownerPos = owner.position();
        Vec3 toOwner = ownerPos.subtract(start);
        double distance = toOwner.length();
        if (distance < 0.2D) {
            return;
        }

        Vec3 dir = toOwner.normalize();
        Vec3 end = ownerPos.subtract(dir.scale(0.8D));
        AABB sweep = new AABB(start, end).inflate(1.1D, 0.8D, 1.1D);
        float damage = TameAbilityEvents.offensiveAbilityCastDamage(data, "dash", levelValue);

        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!nearby.isAlive()) continue;
            if (nearby == tame || nearby == owner) continue;
            if (!(nearby instanceof Monster)) continue;
            if (TameRegistry.isProtectedAttackTarget(TameEntityAdapter.ownerUuid(tame), nearby)) continue;
            LevelSystem.trackDamage(nearby, tame);
            nearby.hurt(tame.damageSources().mobAttack(tame), damage);
        }

        tame.teleportTo(end.x, Math.max(level.getMinBuildHeight() + 1, end.y), end.z);
        tame.setDeltaMovement(dir.x * 0.9D, 0.10D, dir.z * 0.9D);
        tame.hurtMarked = true;
        TameCommands.queueClientReloadForTame(tame);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, tame.getX(), tame.getY(0.6D), tame.getZ(), 6, 0.25D, 0.1D, 0.25D, 0.0D);
        level.playSound(null, tame.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.8F, 1.2F);
    }

    private static List<LivingEntity> collectOwnedNearbyTames(ServerLevel level, ServerPlayer owner, double radius) {
        if (owner == null) {
            return List.of();
        }
        AABB box = owner.getBoundingBox().inflate(radius);
        List<LivingEntity> tames = level.getEntitiesOfClass(LivingEntity.class, box, tame ->
                TameEntityAdapter.isTame(tame)
                        && tame.isAlive()
                        && !TameEntityAdapter.isStayingStill(tame)
                        && owner.getUUID().equals(TameEntityAdapter.ownerUuid(tame))
                        && TameRegistry.get(tame.getUUID()) != null
        );
        return tames == null ? List.of() : tames;
    }

    private static boolean isReady(TameData data, String key, long now) {
        return now >= data.cooldowns.getOrDefault(key, 0L);
    }

    private static void setAbilityCooldown(TameData data, String key, long now, long baseTicks) {
        long ticks = Math.max(1L, baseTicks);
        ticks = Math.max(1L, Math.round(ticks * LevelSystem.quickyCooldownMultiplier(data)));
        setCooldown(data, key, now + ticks);
    }

    private static void setCooldown(TameData data, String key, long tick) {
        data.cooldowns.put(key, tick);
    }
}
