package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TameAbilityEvents {

    private static final int ABILITY_TICK_RATE = 5;
    private static final int HEAVY_ABILITY_STAGGER_TICKS = 20;
    private static final float LIGHTNING_DAMAGE_MULTIPLIER = 5.0F;
    private static final double LIGHTNING_PROC_CHANCE = 0.20D; // 5x less frequent
    private static final int MAX_WARDEN_BEAM_PARTICLES = 64;
    private static final int MAX_SWEEP_TARGETS = 6;
    private static final ThreadLocal<Boolean> INTERNAL_BONUS_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final float ARROW_TO_GUARDIAN_DPS_RATIO = 0.40F;
    private static final float ELDER_DPS_ABOVE_GUARDIAN = 1.125F; // Slightly higher DPS than guardian.
    private static final float SINGLE_TARGET_DAMAGE_MULTIPLIER = 0.70F;
    private static final float AOE_DAMAGE_MULTIPLIER = 0.50F;
    private static final int MAX_WARDEN_BEAM_TARGETS = 12;
    private static final OwnerProtectionAbilityModule.Hooks OWNER_PROTECTION_HOOKS = new OwnerProtectionAbilityModule.Hooks() {
        @Override
        public void debugAbilityUse(TamableAnimal tame, String ability) {
            TameAbilityEvents.debugAbilityUse(tame, ability);
        }

        @Override
        public void applySupportActivationVisual(TamableAnimal tame, String source) {
            TameAbilityEvents.applySupportActivationVisual(tame, source);
        }
    };

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel level)) return;

            if (level.getGameTime() % ABILITY_TICK_RATE != 0) return;

            for (TamableAnimal tame : collectLoadedRegistryTames(level)) {
                if (!tame.isTame()) continue;

                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || (data.abilityLevels.isEmpty() && data.abilities.isEmpty() && data.attributeLevels.isEmpty())) continue;

                if (TameMode.byId(data.mode) == TameMode.PASSIVE) {
                    if (tame.getTarget() != null) {
                        tame.setTarget(null);
                    }
                    continue;
                }

                LivingEntity target = tame.getTarget();
                long now = level.getGameTime();
                boolean heavyPass = shouldRunHeavyPass(tame, now);
                boolean allowOffensive = shouldUseOffensiveAbilities(tame, data, target);
                renderGuardianLockOnBeam(level, tame, data, target, now);

                handleNamedAttributeEffects(tame, data);
                OwnerProtectionAbilityModule.onTick(tame, data);
                handlePassiveHeal(tame, data, now);
                handleAttributeRegeneration(tame, data, now);
                handleRejuvenation(tame, data);
                if (heavyPass) {
                    handleGuardianRepulse(level, tame, data, now);
                }
                if (allowOffensive && heavyPass) {
                    handleCreeperExplosion(level, tame, data, target, now);
                }
                if (allowOffensive) {
                    handleArrowShot(level, tame, data, target, now);
                    handleFishing(level, tame, data, target, now);
                    handleDash(level, tame, data, target, now);
                }
                if (allowOffensive && heavyPass) {
                    handleHealingBottle(level, tame, data, now);
                    handleGhastFireball(level, tame, data, target, now);
                    handleWitherSkull(level, tame, data, target, now);
                    handleBlazeAttack(level, tame, data, target, now);
                    handleGuardianBeam(level, tame, data, target, now);
                    handleElderGuardianBeam(level, tame, data, target, now);
                    handleTrident(level, tame, data, target, now);
                    handleCrossbow(level, tame, data, target, now);
                    handleEvokerFangs(level, tame, data, target, now);
                    handleDragonFireball(level, tame, data, target, now);
                    handleLlamaSpit(level, tame, data, target, now);
                    handleBerserker(tame, data, now);
                    handleShulkerBullet(level, tame, data, target, now);
                }
                if (allowOffensive) {
                    handleSnowballShot(level, tame, data, target, now);
                    handleEnderPearlJump(tame, data, target, now);
                    handleSkyLaunchOnOffense(level, tame, data, target, now);
                }
                if (allowOffensive && heavyPass) {
                    handleLightningStrike(level, tame, data, target, now);
                    handleWardenScream(level, tame, data, target, now);
                }
            }
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onLevelTick error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        try {
            if (event.getEntity() instanceof ServerPlayer owner && event.getAmount() > 0.0F) {
                OwnerProtectionAbilityModule.onOwnerHurt(owner, event, OWNER_PROTECTION_HOOKS);
            }

            TamableAnimal attackerTame = resolveTameAttacker(event);
            if (attackerTame != null && attackerTame.isTame()) {
                TameData attackerData = TameRegistry.get(attackerTame.getUUID());
                if (attackerData != null
                        && TameMode.byId(attackerData.mode) != TameMode.PASSIVE
                        && shouldUseOffensiveAbilities(attackerTame, attackerData, event.getEntity())
                        && !INTERNAL_BONUS_DAMAGE.get()) {
                    applyProjectileAbilityDamageScaling(event);
                    applyAttributeDamageBonuses(attackerTame, attackerData, event);
                    handleBattleStrength(attackerTame, attackerData, event);
                }
            }

            if (!(event.getEntity() instanceof TamableAnimal targetTame) || !targetTame.isTame()) return;
            TameData targetData = TameRegistry.get(targetTame.getUUID());
            if (targetData == null) return;

            handleRetaliationSlow(targetTame, targetData, event);
            handleTotem(targetTame, targetData, event);
            handleDefensiveAura(targetTame, targetData, event);
            OwnerProtectionAbilityModule.onTameHurt(targetTame, targetData, event, OWNER_PROTECTION_HOOKS);
            handleDefensiveAttributeMitigation(targetTame, targetData, event);
            handleSkyLaunchOnDefend(targetTame, targetData, event);
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onHurt error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void handleCreeperExplosion(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "creeper_explosion")) return;
        if (target == null || !target.isAlive()) return;
        if (!isReady(data, "explode", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "creeper_explosion"));
        float damage = aoeDamage(((8.0F + (levelValue - 1) * 1.5F) + tameBaseDamage(tame) * 0.80F) * abilityPowerMultiplier(data));
        double radius = 3.0D + (levelValue - 1) * 0.3D;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, tame.getBoundingBox().inflate(3))) {
            if (nearby == tame || !nearby.isAlive()) continue;

            LevelSystem.trackDamage(nearby, tame);
            nearby.hurt(tame.damageSources().mobAttack(tame), damage);

            Vec3 push = nearby.position().subtract(tame.position());
            if (push.lengthSqr() > 0.0001D) {
                Vec3 knockback = push.normalize().scale(0.9D + levelValue * 0.15D);
                nearby.push(knockback.x, 0.25D, knockback.z);
                nearby.hurtMarked = true;
            }
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, tame.getX(), tame.getY(0.5D), tame.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.POOF, tame.getX(), tame.getY(0.5D), tame.getZ(), capParticles(tame, 20), radius * 0.2D, 0.5D, radius * 0.2D, 0.02D);
        level.playSound(null, tame.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        setAbilityCooldown(tame, data, "explode", now, 200);
        debugAbilityUse(tame, "creeper_explosion");
    }

    private static void handleArrowShot(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "arrow_shot")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "arrow", now)) return;

        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "arrow_shot"));
        float velocity = 1.6F + (levelValue - 1) * 0.1F;

        Arrow arrow = new Arrow(level, tame);
        arrow.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        arrow.setBaseDamage(arrowShotDamageForScaling(tame, data, levelValue));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(direction.x, direction.y, direction.z, velocity, 0.0F);
        level.addFreshEntity(arrow);

        setAbilityCooldown(tame, data, "arrow", now, 60);
        debugAbilityUse(tame, "arrow_shot");
    }

    private static void handleGhastFireball(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "ghast_fireball")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "fireball", now)) return;

        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "ghast_fireball"));
        int power = Math.max(1, Math.min(3, levelValue));

        NoGriefLargeFireball fireball = new NoGriefLargeFireball(level, tame, direction.x, direction.y, direction.z, power);
        fireball.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        level.addFreshEntity(fireball);

        setAbilityCooldown(tame, data, "fireball", now, 100);
        debugAbilityUse(tame, "ghast_fireball");
    }

    private static void handleWitherSkull(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "wither_skull")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "wither_skull_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "wither_skull"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        WitherSkull skull = new WitherSkull(level, tame, direction.x, direction.y, direction.z);
        skull.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        // Keep pet wither skull non-griefing.
        skull.setDangerous(false);
        level.addFreshEntity(skull);

        setAbilityCooldown(tame, data, "wither_skull_tick", now, 80);
        debugAbilityUse(tame, "wither_skull");
    }

    private static void handleBlazeAttack(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "blaze_attack")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "blaze_attack_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "blaze_attack"));
        float damage = singleTargetDamage(arrowShotDamageForScaling(tame, data, levelValue) * 0.60F); // Slightly less DPS than arrow_shot due to shorter cooldown.
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);
        target.setSecondsOnFire(2 + Math.max(0, levelValue - 1));
        level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 8), 0.25D, 0.25D, 0.25D, 0.01D);
        level.playSound(null, tame.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "blaze_attack_tick", now, 40);
        debugAbilityUse(tame, "blaze_attack");
    }

    private static void handleGuardianBeam(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "guardian_beam")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "guardian_beam_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_beam"));
        // DPS target: guardian_beam DPS = 40% of arrow_shot DPS.
        // With different cooldowns, per-cast damage must be scaled by (guardianCd / arrowCd).
        float damage = singleTargetDamage(arrowShotDamageForScaling(tame, data, levelValue) * ARROW_TO_GUARDIAN_DPS_RATIO * (70.0F / 60.0F));
        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), damage);
        Vec3 start = tame.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 beam = end.subtract(start);
        int points = Math.min(32, Math.max(8, (int) Math.ceil(beam.length() * 1.6D)));
        Vec3 step = beam.scale(1.0D / points);
        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(step.scale(i));
            level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, capParticles(tame, 2), 0.05D, 0.05D, 0.05D, 0.0D);
        }
        level.playSound(null, tame.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "guardian_beam_tick", now, 70);
        debugAbilityUse(tame, "guardian_beam");
    }

    private static void handleElderGuardianBeam(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "elder_guardian_beam")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "elder_guardian_beam_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "elder_guardian_beam"));
        // DPS target: elder is only slightly above guardian DPS.
        float elderDpsRatio = ARROW_TO_GUARDIAN_DPS_RATIO * ELDER_DPS_ABOVE_GUARDIAN;
        float damage = singleTargetDamage(arrowShotDamageForScaling(tame, data, levelValue) * elderDpsRatio * (120.0F / 60.0F));
        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), damage);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100 + levelValue * 20, Math.max(0, levelValue - 1)));
        level.sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 14), 0.5D, 0.4D, 0.5D, 0.02D);

        setAbilityCooldown(tame, data, "elder_guardian_beam_tick", now, 120);
        debugAbilityUse(tame, "elder_guardian_beam");
    }

    private static void renderGuardianLockOnBeam(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;

        boolean guardian = LevelSystem.hasAbility(data, "guardian_beam");
        boolean elder = LevelSystem.hasAbility(data, "elder_guardian_beam");
        if (!guardian && !elder) return;
        if (!tame.hasLineOfSight(target)) return;

        Vec3 start = tame.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 beam = end.subtract(start);
        int points = Math.max(10, Math.min(44, (int) Math.ceil(beam.length() * (elder ? 2.0D : 1.6D))));
        Vec3 step = beam.scale(1.0D / points);

        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(step.scale(i));
            if (elder) {
                level.sendParticles(ParticleTypes.BUBBLE_POP, p.x, p.y, p.z, capParticles(tame, 1), 0.01D, 0.01D, 0.01D, 0.0D);
            } else {
                level.sendParticles(ParticleTypes.BUBBLE, p.x, p.y, p.z, capParticles(tame, 1), 0.01D, 0.01D, 0.01D, 0.0D);
            }
        }

        if (elder) {
            level.sendParticles(ParticleTypes.BUBBLE_POP, end.x, end.y, end.z, capParticles(tame, 10), 0.25D, 0.25D, 0.25D, 0.01D);
        } else {
            level.sendParticles(ParticleTypes.BUBBLE, end.x, end.y, end.z, capParticles(tame, 8), 0.2D, 0.2D, 0.2D, 0.01D);
        }

        if (now % 20L == 0L) {
            level.playSound(null, tame.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.HOSTILE, elder ? 1.2F : 0.9F, elder ? 0.8F : 1.0F);
        }
    }

    private static void handleTrident(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "trident")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "trident_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "trident"));
        float damage = singleTargetDamage(arrowShotDamageForScaling(tame, data, levelValue) * 1.70F); // Slightly more DPS than arrow_shot at longer cooldown.
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);
        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 8), 0.3D, 0.25D, 0.3D, 0.02D);
        level.playSound(null, tame.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "trident_tick", now, 90);
        debugAbilityUse(tame, "trident");
    }

    private static void handleCrossbow(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "crossbow")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "crossbow_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "crossbow"));
        Vec3 dir = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int arrowCount = levelValue; // Every level adds one arrow.
        int center = (arrowCount - 1) / 2;
        for (int i = 0; i < arrowCount; i++) {
            int spreadIndex = i - center;
            Arrow arrow = new Arrow(level, tame);
            arrow.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(singleTargetDamage((float) ((3.5D + tameBaseDamage(tame) * 0.50D) * abilityPowerMultiplier(data))));
            arrow.shoot(dir.x, dir.y, dir.z, 2.0F, 4.0F * spreadIndex);
            level.addFreshEntity(arrow);
        }

        setAbilityCooldown(tame, data, "crossbow_tick", now, 80);
        debugAbilityUse(tame, "crossbow");
    }

    private static void handleFishing(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "fishing")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "fishing_hook_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "fishing"));
        Vec3 fromTargetToTame = tame.position().subtract(target.position());
        if (fromTargetToTame.lengthSqr() < 0.001D) return;

        Vec3 pull = fromTargetToTame.normalize().scale(0.35D + levelValue * 0.04D);
        double up = 0.45D + levelValue * 0.05D;
        Vec3 newMotion = target.getDeltaMovement().scale(0.35D).add(pull.x, up, pull.z);
        target.setDeltaMovement(newMotion);
        target.hurtMarked = true;

        float damage = singleTargetDamage(((2.0F + levelValue * 0.6F) + tameBaseDamage(tame) * 0.35F) * abilityPowerMultiplier(data));
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);

        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 10), 0.3D, 0.2D, 0.3D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.HOSTILE, 0.9F, 1.0F);

        long cooldown = Math.max(20L, 90L - (levelValue - 1L) * 3L);
        setAbilityCooldown(tame, data, "fishing_hook_tick", now, cooldown);
        debugAbilityUse(tame, "fishing");
    }

    private static void handleDash(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "dash")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (target instanceof Player || target instanceof TamableAnimal) return;
        if (!isReady(data, "dash_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "dash"));
        Vec3 start = tame.position();
        Vec3 toTarget = target.position().subtract(start);
        double distance = toTarget.length();
        if (distance < 2.0D) return;
        Vec3 dir = toTarget.normalize();
        double dashDistance = Math.min(4.0D + levelValue * 0.8D, Math.max(2.0D, distance));
        Vec3 end = start.add(dir.scale(dashDistance));

        AABB sweep = new AABB(start, end).inflate(1.1D, 0.8D, 1.1D);
        float damage = aoeDamage(((3.0F + levelValue * 1.2F) + tameBaseDamage(tame) * 0.80F) * abilityPowerMultiplier(data));
        int hits = 0;
        Set<java.util.UUID> hitIds = new HashSet<>();
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!nearby.isAlive()) continue;
            if (nearby == tame) continue;
            if (nearby instanceof Player || nearby instanceof TamableAnimal) continue;
            if (isFriendly(tame, nearby)) continue;
            if (!hitIds.add(nearby.getUUID())) continue;
            LevelSystem.trackDamage(nearby, tame);
            applyInternalBonusDamage(nearby, tame, damage);
            hits++;
            if (hits >= 8) break;
        }

        tame.teleportTo(end.x, Math.max(level.getMinBuildHeight() + 1, end.y), end.z);
        tame.setDeltaMovement(dir.x * 0.9D, 0.15D, dir.z * 0.9D);
        tame.hurtMarked = true;
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, tame.getX(), tame.getY(0.6D), tame.getZ(), capParticles(tame, 6), 0.25D, 0.1D, 0.25D, 0.0D);
        level.playSound(null, tame.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.2F);

        long cooldown = Math.max(20L, 90L - (levelValue - 1L) * 4L);
        setAbilityCooldown(tame, data, "dash_tick", now, cooldown);
        debugAbilityUse(tame, "dash");
    }

    private static void handleEvokerFangs(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "evoker_fangs")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "evoker_fangs_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "evoker_fangs"));
        EvokerFangs fangs = new EvokerFangs(level, target.getX(), target.getY(), target.getZ(), tame.getYRot(), 0, tame);
        level.addFreshEntity(fangs);

        float lineChance = 0.0F;
        if (levelValue >= 3) {
            lineChance = Math.min(0.40F, 0.10F + Math.max(0, levelValue - 3) * 0.05F);
        }
        float ringChance = 0.0F;
        if (levelValue >= 4) {
            ringChance = Math.min(0.40F, 0.10F + Math.max(0, levelValue - 4) * 0.05F);
        }

        if (lineChance > 0.0F && tame.getRandom().nextFloat() < lineChance) {
            spawnEvokerFangLine(level, tame, target);
        }
        if (ringChance > 0.0F && tame.getRandom().nextFloat() < ringChance) {
            spawnEvokerFangRing(level, tame);
        }

        setAbilityCooldown(tame, data, "evoker_fangs_tick", now, 100);
        debugAbilityUse(tame, "evoker_fangs");
    }

    private static void handleDragonFireball(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "dragon_fireball")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "dragon_fireball_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "dragon_fireball"));
        float baseDamage = aoeDamage(((6.0F + levelValue * 2.0F) + tameBaseDamage(tame) * 1.10F) * abilityPowerMultiplier(data));
        double radius = 3.0D + levelValue * 0.35D;
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(radius))) {
            if (!nearby.isAlive()) continue;
            if (nearby == tame) continue;
            if (isFriendly(tame, nearby)) continue;
            LevelSystem.trackDamage(nearby, tame);
            nearby.hurt(tame.damageSources().mobAttack(tame), baseDamage);
            nearby.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 0));
        }
        level.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 24), 1.0D, 0.6D, 1.0D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "dragon_fireball_tick", now, 140);
        debugAbilityUse(tame, "dragon_fireball");
    }

    private static void handleLlamaSpit(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "llama_spit")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "llama_spit_tick", now)) return;

        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), singleTargetDamage(((3.0F + LevelSystem.getAbilityLevel(data, "llama_spit")) + tameBaseDamage(tame) * 0.60F) * abilityPowerMultiplier(data)));
        level.sendParticles(ParticleTypes.SPIT, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 8), 0.3D, 0.3D, 0.3D, 0.02D);

        setAbilityCooldown(tame, data, "llama_spit_tick", now, 50);
        debugAbilityUse(tame, "llama_spit");
    }

    private static void handleBerserker(TamableAnimal tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "berserker")) return;
        if (!isReady(data, "berserker_tick", now)) return;
        float hpPercent = tame.getHealth() / Math.max(1.0F, tame.getMaxHealth());
        if (hpPercent > 0.20F) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "berserker"));
        tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, Math.max(0, levelValue - 1)));
        tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, Math.max(0, levelValue - 1)));
        setAbilityCooldown(tame, data, "berserker_tick", now, 400);
        debugAbilityUse(tame, "berserker");
    }

    private static void handleShulkerBullet(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "shulker_bullet")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "shulker_bullet_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shulker_bullet"));
        Direction.Axis axis = Math.abs(target.getX() - tame.getX()) > Math.abs(target.getZ() - tame.getZ())
                ? Direction.Axis.X
                : Direction.Axis.Z;
        ShulkerBullet bullet = new ShulkerBullet(level, tame, target, axis);
        bullet.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        level.addFreshEntity(bullet);
        if (target.getHealth() <= 50.0F) {
            target.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60 + levelValue * 20, Math.max(0, levelValue / 3)));
        }
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 16), 0.4D, 0.5D, 0.4D, 0.01D);
        setAbilityCooldown(tame, data, "shulker_bullet_tick", now, 100);
        debugAbilityUse(tame, "shulker_bullet");
    }

    private static void handleSnowballShot(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "snowball_shot")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "snow", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "snowball_shot"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        Snowball snowball = new Snowball(level, tame);
        snowball.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        snowball.shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
        level.addFreshEntity(snowball);
        float damage = singleTargetDamage(arrowShotDamageForScaling(tame, data, levelValue) * 0.30F); // Slightly less DPS than arrow_shot at 1s cooldown.
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);

        setAbilityCooldown(tame, data, "snow", now, 20);
        debugAbilityUse(tame, "snowball_shot");
    }

    private static void handleEnderPearlJump(TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "ender_pearl_jump")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "pearl", now)) return;

        double distance = tame.distanceTo(target);
        if (distance <= 5.0D) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "ender_pearl_jump"));
        double maxStep = 4.0D + levelValue * 2.0D;
        double stepDistance = Math.min(maxStep, Math.max(2.0D, distance - 1.5D));
        Vec3 direction = target.position().subtract(tame.position()).normalize();
        double destX = tame.getX() + direction.x * stepDistance;
        double destY = target.getY();
        double destZ = tame.getZ() + direction.z * stepDistance;

        boolean teleported = tame.randomTeleport(destX, destY, destZ, true);
        if (!teleported) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2.0D * i) / 8.0D;
                double radius = 1.5D;
                double ox = Math.cos(angle) * radius;
                double oz = Math.sin(angle) * radius;
                if (tame.randomTeleport(destX + ox, destY, destZ + oz, true)) {
                    teleported = true;
                    break;
                }
            }
        }
        if (!teleported) return;

        if (tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.PORTAL, tame.getX(), tame.getY(0.5D), tame.getZ(), capParticles(tame, 20), 0.35D, 0.4D, 0.35D, 0.02D);
        }
        tame.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        setAbilityCooldown(tame, data, "pearl", now, 100);
        debugAbilityUse(tame, "ender_pearl_jump");
    }

    private static void handleBattleStrength(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        if (!LevelSystem.hasAbility(data, "battle_strength")) return;
        if (tame.getRandom().nextDouble() > 0.10D) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "battle_strength"));
        int amplifier = Math.max(0, levelValue - 1);
        for (LivingEntity nearby : tame.level().getEntitiesOfClass(LivingEntity.class, tame.getBoundingBox().inflate(8))) {
            if (!isFriendly(tame, nearby)) continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, amplifier));
        }
        debugAbilityUse(tame, "battle_strength");
    }

    private static void handleDefensiveAura(TamableAnimal targetTame, TameData data, LivingHurtEvent event) {
        if (!LevelSystem.hasAbility(data, "defensive_aura")) return;
        if (targetTame.getRandom().nextDouble() > 0.10D) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "defensive_aura"));
        int amplifier = Math.max(0, levelValue - 1);
        int duration = 100 + (Math.max(0, levelValue - 1) * 20);
        for (LivingEntity nearby : targetTame.level().getEntitiesOfClass(LivingEntity.class, targetTame.getBoundingBox().inflate(8))) {
            if (!isFriendly(targetTame, nearby)) continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
        }
        applySupportActivationVisual(targetTame, "defensive_aura");
        debugAbilityUse(targetTame, "defensive_aura");
    }

    private static void handleTotem(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        int totemLevel = attributeLevel(data, "totem");
        if (totemLevel <= 0) return;
        float postDamageHealth = tame.getHealth() - event.getAmount();
        if (postDamageHealth > 0.0F) return;

        long now = tame.level().getGameTime();
        if (!isReady(data, "totem_attr", now)) return;

        event.setAmount(0.0F);
        if (event.isCancelable()) {
            event.setCanceled(true);
        }

        tame.removeAllEffects();
        tame.setSecondsOnFire(0);
        if (totemLevel >= 3) {
            tame.setHealth(1.0F);
            tame.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            tame.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            tame.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
        } else {
            float restored = Math.max(2.0F, tame.getMaxHealth() * (0.25F + 0.10F * totemLevel));
            tame.setHealth(Math.min(tame.getMaxHealth(), restored));
            tame.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200 + totemLevel * 100, 0));
            tame.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 60 + totemLevel * 20, 0));
        }

        long cooldownTicks = (10L - Math.max(0, totemLevel - 1)) * 60L * 20L;
        setCooldown(data, "totem_attr", now + Math.max(60L, cooldownTicks));
        applySupportActivationVisual(tame, "totem");
        if (tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, tame.getX(), tame.getY(0.6D), tame.getZ(), capParticles(tame, 32), 0.4D, 0.5D, 0.4D, 0.1D);
            level.playSound(null, tame.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        debugAbilityUse(tame, "totem");
    }

    private static void handleLightningStrike(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "lightning_strike")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "lightning_strike_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "lightning_strike"));
        float damage = singleTargetDamage((((5.0F + (levelValue - 1) * 2.0F) + tameBaseDamage(tame) * 0.80F) * abilityPowerMultiplier(data))
                * LIGHTNING_DAMAGE_MULTIPLIER);
        spawnLightningVisual(level, target.getX(), target.getY(), target.getZ(), tame);
        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), damage);
        setAbilityCooldown(tame, data, "lightning_strike_tick", now, 500);
        debugAbilityUse(tame, "lightning_strike");
    }

    private static void handleWardenScream(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "warden_scream")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "warden_scream_tick", now)) return;
        if (!tame.hasLineOfSight(target)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "warden_scream"));
        double maxRange = 16.0D + levelValue * 2.0D;
        if (tame.distanceTo(target) > maxRange) return;

        float damage = aoeDamage(((6.0F + (levelValue - 1) * 2.0F) + tameBaseDamage(tame)) * abilityPowerMultiplier(data));
        Vec3 start = tame.getEyePosition();
        Vec3 end = target.getEyePosition();
        Vec3 beam = end.subtract(start);
        double beamHitRadius = 1.1D;
        AABB beamBounds = new AABB(start, end).inflate(beamHitRadius + 0.5D);
        Set<Integer> hitIds = new HashSet<>();
        List<LivingEntity> beamTargets = level.getEntitiesOfClass(LivingEntity.class, beamBounds, living ->
                living != null
                        && living.isAlive()
                        && living != tame
                        && !isFriendly(tame, living)
        );
        for (LivingEntity victim : beamTargets) {
            if (hitIds.size() >= MAX_WARDEN_BEAM_TARGETS) {
                break;
            }
            Vec3 center = victim.getBoundingBox().getCenter();
            if (distancePointToSegmentSqr(center, start, end) > beamHitRadius * beamHitRadius) continue;
            if (!hitIds.add(victim.getId())) continue;
            LevelSystem.trackDamage(victim, tame);
            victim.hurt(tame.damageSources().mobAttack(tame), damage);
            applyWardenScreamPush(tame, victim, levelValue);
        }
        if (hitIds.isEmpty()) {
            LevelSystem.trackDamage(target, tame);
            target.hurt(tame.damageSources().mobAttack(tame), damage);
            applyWardenScreamPush(tame, target, levelValue);
        }

        int points = Math.min(MAX_WARDEN_BEAM_PARTICLES, Math.max(16, (int) Math.ceil(beam.length() * 2.0D)));
        Vec3 step = beam.scale(1.0D / points);
        level.sendParticles(ParticleTypes.SONIC_BOOM, start.x, start.y, start.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        for (int i = 0; i <= points; i++) {
            Vec3 p = start.add(step.scale(i));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }

        level.playSound(null, tame.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 0.9F);
        setAbilityCooldown(tame, data, "warden_scream_tick", now, 120);
        debugAbilityUse(tame, "warden_scream");
    }

    private static void handleGuardianRepulse(ServerLevel level, TamableAnimal tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "guardian_repulse")) return;
        if (!isReady(data, "guardian_repulse_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_repulse"));
        double radius = 6.0D + levelValue * 0.6D;
        double chance = Math.min(0.90D, 0.12D + levelValue * 0.04D);
        int affected = 0;

        for (Monster monster : level.getEntitiesOfClass(Monster.class, tame.getBoundingBox().inflate(radius))) {
            if (monster == null || !monster.isAlive()) continue;
            if (tame.getRandom().nextDouble() > chance) continue;
            monster.setTarget(tame);
            affected++;
        }
        if (affected > 0) {
            applySupportActivationVisual(tame, "guardian_repulse");
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, tame.getX(), tame.getY(0.9D), tame.getZ(), capParticles(tame, 6 + affected), 0.3D, 0.4D, 0.3D, 0.01D);
            level.playSound(null, tame.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.7F, 1.0F);
            debugAbilityUse(tame, "guardian_repulse");
        }
        setAbilityCooldown(tame, data, "guardian_repulse_tick", now, 60L);
    }

    private static void spawnEvokerFangLine(ServerLevel level, TamableAnimal tame, LivingEntity target) {
        Vec3 dir = target.position().subtract(tame.position());
        if (dir.lengthSqr() < 1.0E-4D) return;
        Vec3 norm = dir.normalize();
        for (int i = 1; i <= 10; i++) {
            double x = tame.getX() + norm.x * i * 1.25D;
            double z = tame.getZ() + norm.z * i * 1.25D;
            int warmup = i * 2;
            EvokerFangs f = new EvokerFangs(level, x, tame.getY(), z, tame.getYRot(), warmup, tame);
            level.addFreshEntity(f);
        }
    }

    private static void spawnEvokerFangRing(ServerLevel level, TamableAnimal tame) {
        int points = 10;
        double radius = 3.0D;
        for (int i = 0; i < points; i++) {
            double angle = (Math.PI * 2.0D * i) / points;
            double x = tame.getX() + Math.cos(angle) * radius;
            double z = tame.getZ() + Math.sin(angle) * radius;
            int warmup = 4;
            EvokerFangs f = new EvokerFangs(level, x, tame.getY(), z, tame.getYRot(), warmup, tame);
            level.addFreshEntity(f);
        }
    }

    private static void handleAttributeRegeneration(TamableAnimal tame, TameData data, long now) {
        int regenLevel = attributeLevel(data, "regeneration");
        if (regenLevel <= 0) return;
        if (!isReady(data, "attr_regen", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) return;

        tame.heal(0.6F * regenLevel);
        setAbilityCooldown(tame, data, "attr_regen", now, 40);
        debugAbilityUse(tame, "regeneration");
    }

    private static void handlePassiveHeal(TamableAnimal tame, TameData data, long now) {
        if (!isReady(data, "passive_heal_tick", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) {
            setCooldown(data, "passive_heal_tick", now + 100L);
            return;
        }
        tame.heal(1.0F);
        setCooldown(data, "passive_heal_tick", now + 100L);
    }

    private static void handleRejuvenation(TamableAnimal tame, TameData data) {
        int rejuvenationLevel = attributeLevel(data, "rejuvenation");
        if (rejuvenationLevel <= 0) {
            return;
        }
        TameableUtils.absorbExpOrbs(tame, rejuvenationLevel);
    }

    private static void handleHealingBottle(ServerLevel level, TamableAnimal tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "healing_bottle")) return;
        if (!isReady(data, "healing_bottle_tick", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "healing_bottle"));
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(potion, levelValue >= 4 ? Potions.STRONG_HEALING : Potions.HEALING);
        int regenerationDuration = 60 + (Math.max(0, levelValue - 1) * 40);
        int regenerationAmplifier = Math.min(4, Math.max(0, (levelValue - 1) / 3));
        PotionUtils.setCustomEffects(potion, List.of(new MobEffectInstance(MobEffects.REGENERATION, regenerationDuration, regenerationAmplifier)));

        ThrownPotion thrownPotion = new ThrownPotion(level, tame);
        thrownPotion.setItem(potion);
        thrownPotion.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        thrownPotion.setDeltaMovement(0.0D, 0.65D + (Math.min(6, levelValue) * 0.03D), 0.0D);
        level.addFreshEntity(thrownPotion);

        level.playSound(null, tame.blockPosition(), SoundEvents.SPLASH_POTION_THROW, SoundSource.NEUTRAL, 0.7F, 1.0F);
        long cooldown = Math.max(60L, 220L - (Math.max(0, levelValue - 1) * 15L));
        setAbilityCooldown(tame, data, "healing_bottle_tick", now, cooldown);
        applySupportActivationVisual(tame, "healing_bottle");
        debugAbilityUse(tame, "healing_bottle");
    }

    private static void handleSkyLaunchOnOffense(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now) {
        trySkyLaunch(level, tame, data, target, now, 0.10D);
    }

    private static void handleSkyLaunchOnDefend(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        if (!(tame.level() instanceof ServerLevel level)) return;
        if (event.getAmount() <= 0.0F) return;
        LivingEntity attacker = null;
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            attacker = living;
        } else if (event.getSource().getDirectEntity() instanceof LivingEntity living) {
            attacker = living;
        }
        if (attacker == null || !attacker.isAlive()) return;
        trySkyLaunch(level, tame, data, attacker, level.getGameTime(), 0.16D);
    }

    private static void trySkyLaunch(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity target, long now, double baseChance) {
        if (!LevelSystem.hasAbility(data, "sky_launch")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "sky_launch_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "sky_launch"));
        double chance = Math.min(0.90D, baseChance + levelValue * 0.04D);
        if (tame.getRandom().nextDouble() > chance) return;

        float damage = singleTargetDamage(((1.5F + levelValue * 0.8F) + tameBaseDamage(tame) * 0.45F) * abilityPowerMultiplier(data));
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);
        Vec3 motion = target.getDeltaMovement();
        double launchY = Math.min(2.1D, 0.55D + levelValue * 0.12D);
        target.setDeltaMovement(motion.x * 0.6D, launchY, motion.z * 0.6D);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.2D), target.getZ(), capParticles(tame, 10), 0.25D, 0.1D, 0.25D, 0.03D);
        level.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.7F, 1.2F);
        setAbilityCooldown(tame, data, "sky_launch_tick", now, 70L);
        debugAbilityUse(tame, "sky_launch");
    }

    private static void applyAttributeDamageBonuses(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        float damage = event.getAmount();
        if (damage <= 0.0F) return;
        LivingEntity target = event.getEntity();

        int killerLevel = attributeLevel(data, "killer");
        if (killerLevel > 0) {
            float hpPct = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
            float missing = Mth.clamp(1.0F - hpPct, 0.0F, 1.0F);
            float multiplier = 1.0F + missing * killerLevel;
            event.setAmount(event.getAmount() * multiplier);
            damage = event.getAmount();
            debugAbilityUse(tame, "killer");
        }

        int pacifistLevel = attributeLevel(data, "pacifist");
        if (pacifistLevel > 0) {
            float hpPct = Mth.clamp(target.getHealth() / Math.max(1.0F, target.getMaxHealth()), 0.0F, 1.0F);
            float multiplier = 1.0F + hpPct * pacifistLevel;
            event.setAmount(event.getAmount() * multiplier);
            damage = event.getAmount();
            debugAbilityUse(tame, "pacifist");
        }

        int bossKillerLevel = attributeLevel(data, "bosskiller");
        if (bossKillerLevel > 0) {
            int threshold = Math.max(10, 100 - (bossKillerLevel - 1) * 10);
            int stacks = Math.max(0, (int) (target.getMaxHealth() / threshold));
            if (stacks > 0) {
                int amp = Math.min(4, stacks - 1);
                tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, amp, false, false, true));
                tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, amp, false, false, true));
                tame.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, amp, false, false, true));
                debugAbilityUse(tame, "bosskiller");
            }
        }

        int lifestealLevel = attributeLevel(data, "lifesteal");
        if (lifestealLevel > 0) {
            float heal = event.getAmount() * (0.05F + (lifestealLevel * 0.02F));
            tame.heal(heal);
            debugAbilityUse(tame, "lifesteal");
        }

        int firefangLevel = attributeLevel(data, "firefang");
        if (firefangLevel > 0) {
            event.getEntity().setSecondsOnFire(2 + firefangLevel);
            debugAbilityUse(tame, "firefang");
        }

        int poisonFangLevel = attributeLevel(data, "poison_fang");
        if (poisonFangLevel > 0) {
            int duration = 40 + poisonFangLevel * 20;
            int amplifier = Math.min(4, Math.max(0, poisonFangLevel / 3));
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
            debugAbilityUse(tame, "poison_fang");
        }

        int witherfangLevel = attributeLevel(data, "witherfang");
        if (witherfangLevel > 0) {
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, 60 + witherfangLevel * 20, Math.max(0, witherfangLevel - 1)));
            debugAbilityUse(tame, "witherfang");
        }

        int frostFangLevel = attributeLevel(data, "frost_fang");
        if (frostFangLevel > 0) {
            float hpPct = Mth.clamp(target.getHealth() / Math.max(1.0F, target.getMaxHealth()), 0.0F, 1.0F);
            double baseChance = Math.min(0.85D, 0.15D * frostFangLevel);
            double lowHpBonus = (1.0D - hpPct) * 0.20D;
            double totalChance = Math.min(0.95D, baseChance + lowHpBonus);
            if (tame.getRandom().nextDouble() < totalChance) {
                int slownessAmp = Math.max(0, (frostFangLevel - 1) / 2);
                int duration = 40 + frostFangLevel * 20;
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slownessAmp));
                debugAbilityUse(tame, "frost_fang");
            }
        }

        int magneticLevel = attributeLevel(data, "magnetic");
        if (magneticLevel > 0) {
            applyMagneticPull(tame, target, magneticLevel);
        }

        int chainLightningLevel = attributeLevel(data, "chain_lightning");
        if (chainLightningLevel > 0 && tame.level() instanceof ServerLevel serverLevel) {
            applyChainLightning(serverLevel, tame, data, target, event.getAmount(), chainLightningLevel);
        }

        int smiteLevel = attributeLevel(data, "smite");
        if (smiteLevel > 0 && target.getMobType() == MobType.UNDEAD) {
            float extra = event.getAmount() * (0.20F + 0.08F * smiteLevel);
            event.setAmount(event.getAmount() + extra);
            debugAbilityUse(tame, "smite");
        }

        int arthropodLevel = attributeLevel(data, "bane_of_arthropods");
        if (arthropodLevel > 0 && target.getMobType() == MobType.ARTHROPOD) {
            float extra = event.getAmount() * (0.20F + 0.08F * arthropodLevel);
            event.setAmount(event.getAmount() + extra);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + arthropodLevel * 10, Math.max(0, arthropodLevel / 2)));
            debugAbilityUse(tame, "bane_of_arthropods");
        }

        int lightningfangLevel = attributeLevel(data, "lightningfang");
        if (lightningfangLevel > 0 && tame.level() instanceof ServerLevel serverLevel) {
            if (tame.getRandom().nextDouble() <= LIGHTNING_PROC_CHANCE) {
                spawnLightningVisual(serverLevel, target.getX(), target.getY(), target.getZ(), tame);
                LevelSystem.trackDamage(target, tame);
                applyInternalBonusDamage(
                        target,
                        tame,
                        singleTargetDamage((((2.0F + lightningfangLevel) + tameBaseDamage(tame) * 0.35F) * abilityPowerMultiplier(data))
                                * LIGHTNING_DAMAGE_MULTIPLIER
                        )
                );
                debugAbilityUse(tame, "lightningfang");
            }
        }

        int stealLevel = attributeLevel(data, "positive_effect_steal");
        if (stealLevel > 0 && tame.getRandom().nextDouble() < Math.min(0.80D, 0.10D * stealLevel)) {
            stealPositiveEffect(target, tame);
            debugAbilityUse(tame, "positive_effect_steal");
        }

        int transferLevel = attributeLevel(data, "negative_effect_transfer");
        if (transferLevel > 0) {
            transferNegativeEffects(tame, target, transferLevel);
            debugAbilityUse(tame, "negative_effect_transfer");
        }

        int sweepLevel = attributeLevel(data, "sweeping_edge");
        if (sweepLevel > 0) {
            applySweepingEdge(tame, data, target, event.getAmount(), sweepLevel);
        }
    }

    private static void handleNamedAttributeEffects(TamableAnimal tame, TameData data) {
        applyNamedAttributeEffect(tame, data, "speed", MobEffects.MOVEMENT_SPEED);
        applyNamedAttributeEffect(tame, data, "strength", MobEffects.DAMAGE_BOOST);
        applyNamedAttributeEffect(tame, data, "resistance", MobEffects.DAMAGE_RESISTANCE);
        applyNamedAttributeEffect(tame, data, "fire_resistance", MobEffects.FIRE_RESISTANCE);
        handlePoisonResistance(tame, data);
        applyNamedAttributeEffect(tame, data, "jump_boost", MobEffects.JUMP);
    }

    private static void applyNamedAttributeEffect(TamableAnimal tame, TameData data, String attribute, net.minecraft.world.effect.MobEffect effect) {
        int level = attributeLevel(data, attribute);
        if (level <= 0) return;
        int amplifier = Math.max(0, level - 1);
        MobEffectInstance current = tame.getEffect(effect);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > 20) {
            return;
        }
        tame.addEffect(new MobEffectInstance(effect, 200, amplifier, false, false, true));
    }

    private static void handlePoisonResistance(TamableAnimal tame, TameData data) {
        int level = attributeLevel(data, "poison_resistance");
        if (level <= 0) return;
        if (tame.hasEffect(MobEffects.POISON)) {
            tame.removeEffect(MobEffects.POISON);
            debugAbilityUse(tame, "poison_resistance");
        }
    }

    private static void handleDefensiveAttributeMitigation(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        float amount = event.getAmount();
        if (amount <= 0.0F) return;

        int feather = attributeLevel(data, "feather_falling");
        if (feather > 0 && event.getSource().is(DamageTypeTags.IS_FALL)) {
            float reduce = Math.min(0.90F, 0.12F * feather);
            amount *= (1.0F - reduce);
            event.setAmount(amount);
            debugAbilityUse(tame, "feather_falling");
        }

        int explosion = attributeLevel(data, "explosion_resistance");
        if (explosion > 0 && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            float reduce = Math.min(0.80F, 0.10F * explosion);
            amount *= (1.0F - reduce);
            event.setAmount(amount);
            debugAbilityUse(tame, "explosion_resistance");
        }
    }

    private static void handleRetaliationSlow(TamableAnimal tame, TameData data, LivingHurtEvent event) {
        if (!LevelSystem.hasAbility(data, "retaliation_slow")) return;
        if (event.getAmount() <= 0.0F) return;
        long now = tame.level().getGameTime();
        if (!isReady(data, "retaliation_slow_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "retaliation_slow"));
        double chance = Math.min(0.85D, 0.20D + levelValue * 0.04D);
        if (tame.getRandom().nextDouble() > chance) return;

        int slownessAmp = Math.max(0, levelValue / 3); // every 3rd level -> slowness+
        int nonThirdLevels = Math.max(0, levelValue - (levelValue / 3));
        double radius = 2.0D + nonThirdLevels * 0.35D; // otherwise -> range+
        int duration = 40 + levelValue * 10;

        int affected = 0;
        for (LivingEntity nearby : tame.level().getEntitiesOfClass(LivingEntity.class, tame.getBoundingBox().inflate(radius))) {
            if (!nearby.isAlive() || nearby == tame) continue;
            if (isFriendly(tame, nearby)) continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, slownessAmp));
            affected++;
        }
        if (affected > 0) {
            if (tame.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.SNOWFLAKE, tame.getX(), tame.getY(0.6D), tame.getZ(), capParticles(tame, 12), radius * 0.2D, 0.4D, radius * 0.2D, 0.01D);
            }
            debugAbilityUse(tame, "retaliation_slow");
        }
        setAbilityCooldown(tame, data, "retaliation_slow_tick", now, 60L);
    }


    private static void stealPositiveEffect(LivingEntity from, TamableAnimal to) {
        List<MobEffectInstance> positive = new ArrayList<>();
        for (MobEffectInstance fx : from.getActiveEffects()) {
            if (fx.getEffect().getCategory() == MobEffectCategory.BENEFICIAL) {
                positive.add(fx);
            }
        }
        if (positive.isEmpty()) return;
        MobEffectInstance picked = positive.get(to.getRandom().nextInt(positive.size()));
        to.addEffect(new MobEffectInstance(picked.getEffect(), picked.getDuration(), picked.getAmplifier()));
        from.removeEffect(picked.getEffect());
    }

    private static void transferNegativeEffects(TamableAnimal from, LivingEntity to, int level) {
        for (MobEffectInstance fx : from.getActiveEffects()) {
            if (fx.getEffect().getCategory() != MobEffectCategory.HARMFUL) continue;
            int duration = Math.max(20, fx.getDuration() + level * 20);
            int amp = Math.max(0, fx.getAmplifier());
            to.addEffect(new MobEffectInstance(fx.getEffect(), duration, amp));
        }
    }

    private static void applyMagneticPull(TamableAnimal tame, LivingEntity target, int level) {
        Vec3 delta = tame.position().subtract(target.position());
        if (delta.lengthSqr() < 1.0E-4D) {
            return;
        }

        double strength;
        if (level <= 2) {
            strength = 0.60D;
        } else if (level == 3) {
            strength = 1.00D;
        } else if (level == 4) {
            strength = 1.20D;
        } else {
            strength = Math.min(2.50D, 1.40D + (level - 5) * 0.10D);
        }

        Vec3 pull = delta.normalize().scale((0.10D + 0.08D * strength));
        Vec3 motion = target.getDeltaMovement().scale(0.60D).add(pull.x, 0.08D + 0.04D * strength, pull.z);
        target.setDeltaMovement(motion);
        target.hurtMarked = true;
        debugAbilityUse(tame, "magnetic");
    }

    private static void applyChainLightning(ServerLevel level, TamableAnimal tame, TameData data, LivingEntity firstTarget, float hitDamage, int levelValue) {
        if (firstTarget == null || !firstTarget.isAlive()) {
            return;
        }
        int maxChains = Math.min(60, 3 + levelValue * 3);
        double chainRadius = 4.0D + levelValue * 0.35D;
        float chainDamage = aoeDamage(Math.max(1.0F, hitDamage * (0.30F + Math.min(0.70F, levelValue * 0.03F))) * abilityPowerMultiplier(data));

        Set<Integer> hitIds = new HashSet<>();
        hitIds.add(firstTarget.getId());
        LivingEntity from = firstTarget;
        int chainsApplied = 0;

        for (int i = 0; i < maxChains; i++) {
            LivingEntity next = findNearestChainTarget(level, tame, from, hitIds, chainRadius);
            if (next == null) {
                break;
            }
            hitIds.add(next.getId());
            LevelSystem.trackDamage(next, tame);
            applyInternalBonusDamage(next, tame, chainDamage);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, next.getX(), next.getY(0.6D), next.getZ(), capParticles(tame, 8), 0.25D, 0.25D, 0.25D, 0.02D);
            from = next;
            chainsApplied++;
        }

        if (chainsApplied > 0) {
            level.playSound(null, firstTarget.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.HOSTILE, 0.35F, 1.4F);
            debugAbilityUse(tame, "chain_lightning");
        }
    }

    private static LivingEntity findNearestChainTarget(ServerLevel level, TamableAnimal tame, LivingEntity origin, Set<Integer> excludeIds, double radius) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, origin.getBoundingBox().inflate(radius))) {
            if (candidate == tame || candidate == origin || !candidate.isAlive()) continue;
            if (excludeIds.contains(candidate.getId())) continue;
            if (isFriendly(tame, candidate)) continue;
            double dist = candidate.distanceToSqr(origin);
            if (dist < bestDistance) {
                bestDistance = dist;
                best = candidate;
            }
        }
        return best;
    }

    private static void applySweepingEdge(TamableAnimal tame, TameData data, LivingEntity primaryTarget, float hitDamage, int level) {
        int hits = 0;
        double radius = 1.8D + level * 0.25D;
        float scaling = Math.min(1.25F, 0.15F + (0.08F * level));
        float splash = aoeDamage((hitDamage + tameBaseDamage(tame)) * scaling);
        for (LivingEntity nearby : tame.level().getEntitiesOfClass(LivingEntity.class, primaryTarget.getBoundingBox().inflate(radius))) {
            if (nearby == tame || nearby == primaryTarget || !nearby.isAlive()) continue;
            if (isFriendly(tame, nearby)) continue;
            LevelSystem.trackDamage(nearby, tame);
            applyInternalBonusDamage(nearby, tame, splash);
            hits++;
            if (hits >= MAX_SWEEP_TARGETS) {
                break;
            }
        }
        if (hits > 0 && tame.level() instanceof ServerLevel levelWorld) {
            levelWorld.sendParticles(ParticleTypes.SWEEP_ATTACK, primaryTarget.getX(), primaryTarget.getY(0.5D), primaryTarget.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
            debugAbilityUse(tame, "sweeping_edge");
        }
    }

    public static void onKillOrAssist(TamableAnimal tame, TameData data, LivingEntity dead, boolean wasKiller) {
        if (tame == null || data == null || dead == null) return;
        long now = tame.level().getGameTime();

        if (LevelSystem.hasAbility(data, "bloodlust") && wasKiller && isReady(data, "bloodlust_tick", now)) {
            int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "bloodlust"));
            tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, Math.max(0, levelValue - 1)));
            tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, Math.max(0, levelValue - 1)));
            applySupportActivationVisual(tame, "bloodlust");
            if (tame.level() instanceof ServerLevel level) {
                DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
                level.sendParticles(red, tame.getX(), tame.getY(0.7D), tame.getZ(), capParticles(tame, 24), 0.35D, 0.45D, 0.35D, 0.02D);
            }
            setAbilityCooldown(tame, data, "bloodlust_tick", now, 400);
            debugAbilityUse(tame, "bloodlust");
        }

        int explodeLevel = attributeLevel(data, "killexploder");
        if (explodeLevel > 0 && tame.level() instanceof ServerLevel level) {
            float damage = aoeDamage((4.0F + explodeLevel * 2.0F) + tameBaseDamage(tame) * 0.90F);
            double radius = 2.5D + explodeLevel * 0.35D;
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, dead.getBoundingBox().inflate(radius))) {
                if (!nearby.isAlive() || nearby == tame) continue;
                LevelSystem.trackDamage(nearby, tame);
                nearby.hurt(tame.damageSources().mobAttack(tame), damage);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, dead.getX(), dead.getY(0.5D), dead.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, dead.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.8F, 1.1F);
            debugAbilityUse(tame, "killexploder");
        }
    }

    private static int attributeLevel(TameData data, String id) {
        return Math.max(0, data.attributeLevels.getOrDefault(id, 0));
    }

    private static double chanceByLevel(int level, double perLevel, double cap) {
        return Math.min(cap, level * perLevel);
    }

    private static float abilityPowerMultiplier(TameData data) {
        int level = attributeLevel(data, "ability_power");
        return 1.0F + (float) level * 0.12F;
    }

    private static float arrowShotDamageForScaling(TamableAnimal tame, TameData data, int abilityLevel) {
        int lvl = Math.max(1, abilityLevel);
        return singleTargetDamage((float) (((2.0D + lvl) + tameBaseDamage(tame) * 0.65D) * abilityPowerMultiplier(data)));
    }

    private static float tameBaseDamage(TamableAnimal tame) {
        if (tame == null) {
            return 2.0F;
        }
        if (tame.getAttribute(Attributes.ATTACK_DAMAGE) == null) {
            return 2.0F;
        }
        return (float) tame.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private static void setAbilityCooldown(TamableAnimal tame, TameData data, String key, long now, long baseTicks) {
        long ticks = Math.max(1L, baseTicks);

        int emergency = attributeLevel(data, "emergency_cooldown_reduction");
        if (emergency > 0) {
            float hp = tame.getHealth() / Math.max(1.0F, tame.getMaxHealth());
            if (hp <= 0.30F) {
                double chance = chanceByLevel(emergency, 0.12D, 0.60D);
                if (tame.getRandom().nextDouble() < chance) {
                    ticks = 20L;
                    debugAbilityUse(tame, "emergency_cooldown_reduction");
                }
            }
        }
        setCooldown(data, key, now + ticks);
    }

    private static void spawnLightningVisual(ServerLevel level, double x, double y, double z, TamableAnimal tame) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) return;
        lightning.moveTo(x, y, z);
        lightning.setVisualOnly(true);
        if (tame.getOwner() instanceof ServerPlayer player) {
            lightning.setCause(player);
        }
        level.addFreshEntity(lightning);
    }

    private static void debugAbilityUse(TamableAnimal tame, String ability) {
        if (!(tame.level() instanceof ServerLevel level)) return;
        if (tame.getOwnerUUID() == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(tame.getOwnerUUID());
        if (owner == null) return;
        boolean attribute = LevelSystem.knownAttributeIds().contains(ability);
        if (attribute && !PlayerDebugSettings.attributeUsed(owner.getUUID())) return;
        if (!attribute && !PlayerDebugSettings.abilityUsed(owner.getUUID())) return;
        String tameName = tame.hasCustomName() && tame.getCustomName() != null ? tame.getCustomName().getString() : tame.getName().getString();
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(tameName + ": " + ability));
    }

    private static TamableAnimal resolveTameAttacker(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof TamableAnimal tame) {
            return tame;
        }
        if (event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() instanceof TamableAnimal tame) {
            return tame;
        }
        return null;
    }

    private static boolean isFriendly(TamableAnimal tame, Entity entity) {
        if (entity == tame) return true;
        if (entity instanceof Player player) {
            return true;
        }
        if (entity instanceof TamableAnimal otherTame && otherTame.isTame()) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), otherTame.getUUID())) {
                return false;
            }
            return true;
        }
        return false;
    }

    private static boolean isReady(TameData data, String key, long now) {
        return now >= data.cooldowns.getOrDefault(key, 0L);
    }

    private static void setCooldown(TameData data, String key, long tick) {
        data.cooldowns.put(key, tick);
    }


    private static int capParticles(TamableAnimal tame, int requested) {
        return Math.max(1, requested);
    }

    private static void applySupportActivationVisual(TamableAnimal tame, String source) {
        tame.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
    }

    private static void applyInternalBonusDamage(LivingEntity target, TamableAnimal attacker, float amount) {
        if (target == null || attacker == null) return;
        if (isFriendly(attacker, target)) return;
        INTERNAL_BONUS_DAMAGE.set(true);
        try {
            target.hurt(attacker.damageSources().mobAttack(attacker), amount);
        } finally {
            INTERNAL_BONUS_DAMAGE.set(false);
        }
    }

    private static void applyProjectileAbilityDamageScaling(LivingHurtEvent event) {
        if (event == null || event.getSource() == null) return;
        Entity direct = event.getSource().getDirectEntity();
        if (direct == null) return;

        if (direct instanceof NoGriefLargeFireball || direct instanceof WitherSkull || direct instanceof EvokerFangs) {
            event.setAmount(aoeDamage(event.getAmount()));
            return;
        }
        if (direct instanceof ShulkerBullet) {
            event.setAmount(singleTargetDamage(event.getAmount()));
        }
    }

    private static float singleTargetDamage(float amount) {
        return amount * SINGLE_TARGET_DAMAGE_MULTIPLIER;
    }

    private static float aoeDamage(float amount) {
        return amount * AOE_DAMAGE_MULTIPLIER;
    }

    private static void applyWardenScreamPush(TamableAnimal tame, LivingEntity target, int levelValue) {
        Vec3 push = target.position().subtract(tame.position());
        if (push.lengthSqr() > 0.0001D) {
            push = push.normalize().scale(0.8D + levelValue * 0.1D);
            target.push(push.x, 0.2D, push.z);
            target.hurtMarked = true;
        }
    }

    private static double distancePointToSegmentSqr(Vec3 point, Vec3 a, Vec3 b) {
        if (point == null || a == null || b == null) return Double.MAX_VALUE;
        Vec3 ab = b.subtract(a);
        double abLenSqr = ab.lengthSqr();
        if (abLenSqr <= 1.0E-7D) {
            return point.distanceToSqr(a);
        }
        double t = (point.subtract(a)).dot(ab) / abLenSqr;
        t = Mth.clamp(t, 0.0D, 1.0D);
        Vec3 closest = a.add(ab.scale(t));
        return point.distanceToSqr(closest);
    }

    private static Set<TamableAnimal> collectLoadedRegistryTames(ServerLevel level) {
        Set<TamableAnimal> result = new HashSet<>();
        ResourceLocation levelId = level.dimension().location();
        for (TameData data : TameRegistry.TAMES.values()) {
            if (data == null || data.uuid == null || data.dead) continue;
            if (data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
                ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
                if (lastKnown != null && !lastKnown.equals(levelId)) {
                    continue;
                }
            }
            Entity entity = level.getEntity(data.uuid);
            if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || !tame.isAlive()) continue;
            result.add(tame);
        }
        return result;
    }

    private static boolean shouldRunHeavyPass(TamableAnimal tame, long now) {
        int buckets = Math.max(1, HEAVY_ABILITY_STAGGER_TICKS / ABILITY_TICK_RATE);
        int phase = Math.floorMod(tame.getUUID().hashCode(), buckets);
        long slice = now / ABILITY_TICK_RATE;
        return (slice % buckets) == phase;
    }

    private static boolean shouldUseOffensiveAbilities(TamableAnimal tame, TameData data, LivingEntity target) {
        if (tame == null || data == null || target == null || !target.isAlive()) return false;
        if (isFriendly(tame, target)) return false;
        if (target instanceof TamableAnimal otherTame && TameDuelManager.areDuelOpponents(tame.getUUID(), otherTame.getUUID())) {
            return true;
        }

        TameMode mode = TameMode.byId(data.mode);
        return switch (mode) {
            case PASSIVE -> false;
            // DEFAULT should still allow abilities against the tame's current valid target.
            case DEFAULT -> true;
            case MONSTER_HUNTER, BOSS -> target instanceof Monster;
            case BODYGUARD -> (target instanceof Monster) || isOwnerCombatPriorityTarget(tame, target);
            case DEFAULT_PLUS, AGGRESSIVE -> true;
        };
    }

    private static boolean isOwnerCombatPriorityTarget(TamableAnimal tame, LivingEntity target) {
        if (tame == null || target == null) return false;
        if (!(tame.getOwner() instanceof ServerPlayer owner)) return false;
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker == target && attacker.isAlive()) return true;
        LivingEntity attacked = owner.getLastHurtMob();
        return attacked == target && attacked.isAlive();
    }

}
