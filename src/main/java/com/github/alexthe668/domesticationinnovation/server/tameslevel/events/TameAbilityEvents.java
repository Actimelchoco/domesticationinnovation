package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.ChainLightningEntity;
import com.github.alexthe668.domesticationinnovation.server.entity.DIEntityRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TamePerformanceProfiler;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.misc.DISoundRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatRules;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TameAbilityEvents {
    private record SupportCacheKey(UUID centerId, UUID ownerId, int radiusMillis) {}

    private static final int ABILITY_TICK_RATE = 5;
    private static final int HEAVY_ABILITY_STAGGER_TICKS = 20;
    private static final int NAMED_EFFECT_TICK_RATE = 10;
    private static final int OWNER_PROTECTION_TICK_RATE = 10;
    private static final int GUARDIAN_REPULSE_CHECK_RATE = 40;
    private static final int GUARDIAN_LOCK_ON_PARTICLE_RATE = 10;
    private static final int FLASH_DASH_COUNT = 5;
    private static final long FLASH_DASH_INTERVAL_TICKS = 10L;
    private static final long FLASH_BASE_COOLDOWN_TICKS = 90L * 6L;
    private static final String FLASH_CHAIN_REMAINING_KEY = "flash_chain_remaining";
    private static final String FLASH_CHAIN_NEXT_TICK_KEY = "flash_chain_next_tick";
    private static final String PROJECTILE_DAMAGE_TAG = "TamesLevelProjectileDamage";
    private static final String PROJECTILE_SOURCE_TAG = "TamesLevelProjectileSource";
    private static final int SHORT_PROJECTILE_TICKS = 60;
    private static final int LONG_PROJECTILE_TICKS = 80;
    private static final float LIGHTNING_DAMAGE_MULTIPLIER = 5.0F;
    private static final double LIGHTNING_PROC_CHANCE = 0.20D; // 5x less frequent
    private static final int MAX_WARDEN_BEAM_PARTICLES = 64;
    private static final int MAX_SWEEP_TARGETS = 6;
    private static final ThreadLocal<Boolean> INTERNAL_BONUS_DAMAGE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> INTERNAL_SUPPORT_REDIRECT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<List<String>> DAMAGE_DEBUG_CONTRIBUTORS = ThreadLocal.withInitial(ArrayList::new);
    private static final int MAX_WARDEN_BEAM_TARGETS = 12;
    private static final Set<String> HEALING_SUPPORT_VISUAL_SOURCES = Set.of(
            "healing_bottle",
            "triage_pulse",
            "battlefield_medic",
            "life_gift"
    );
    private static final OwnerProtectionAbilityModule.Hooks OWNER_PROTECTION_HOOKS = new OwnerProtectionAbilityModule.Hooks() {
        @Override
        public void debugAbilityUse(LivingEntity tame, String ability) {
            TameAbilityEvents.debugAbilityUse(tame, ability);
        }

        @Override
        public void applySupportActivationVisual(LivingEntity tame, String source) {
            TameAbilityEvents.applySupportActivationVisual(tame, source);
        }

        @Override
        public void grantSupportXp(LivingEntity supporter, TameData data, LivingEntity beneficiary, long now, float effectiveAmount, float scale) {
            TameAbilityEvents.grantSupportXp(supporter, data, beneficiary, now, effectiveAmount, scale);
        }
    };

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel level)) return;

            if (level.getGameTime() % ABILITY_TICK_RATE != 0) return;

            boolean revivedDeadEntry = false;
            Map<SupportCacheKey, List<LivingEntity>> supportCache = new HashMap<>();
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity tame)) continue;
                if (!TameEntityAdapter.isTame(tame) || !tame.isAlive()) continue;
                TameData data = TameRegistry.get(tame.getUUID());
                if (data == null || (data.abilityLevels.isEmpty() && data.abilities.isEmpty() && data.attributeLevels.isEmpty())) continue;
                if (reviveDeadEntry(data)) {
                    revivedDeadEntry = true;
                }

                LivingEntity target = TameEntityAdapter.target(tame);
                if ((target == null || !target.isAlive() || isFriendly(tame, target))
                        && level.getServer() != null
                        && TameDuelManager.isTameInDuel(tame.getUUID())) {
                    LivingEntity duelTarget = TameDuelManager.findNearestLoadedOpponent(level.getServer(), tame);
                    if (duelTarget != null && duelTarget.isAlive() && !isFriendly(tame, duelTarget)) {
                        TameDuelManager.assignDuelTarget(tame, duelTarget);
                        target = duelTarget;
                    }
                }
                if (TameMode.byId(data.mode) == TameMode.PASSIVE && !isDuelOpponent(tame, target)) {
                    if (TameEntityAdapter.target(tame) != null) {
                        TameEntityAdapter.setTarget(tame, null);
                    }
                    continue;
                }
                final LivingEntity currentTarget = target;
                long now = level.getGameTime();
                boolean heavyPass = shouldRunHeavyPass(tame, now);
                boolean namedEffectsPass = shouldRunPeriodicPass(tame, now, NAMED_EFFECT_TICK_RATE);
                boolean ownerProtectionPass = shouldRunPeriodicPass(tame, now, OWNER_PROTECTION_TICK_RATE);
                boolean guardianRepulsePass = shouldRunPeriodicPass(tame, now, GUARDIAN_REPULSE_CHECK_RATE);
                boolean allowOffensive = shouldUseOffensiveAbilities(tame, data, currentTarget);
                boolean abilitiesBlocked = TameableUtils.getImmuneTime(tame) > 0 || TameCommands.isHungerBlockingAbilities(data);
                TamePerformanceProfiler.run("feature.guardian_lock_on_beam", () -> renderGuardianLockOnBeam(level, tame, data, currentTarget, now));

                if (namedEffectsPass) {
                    TamePerformanceProfiler.run("attribute.named_effects", () -> handleNamedAttributeEffects(tame, data));
                }
                if (!abilitiesBlocked && ownerProtectionPass) {
                    TamePerformanceProfiler.run("feature.owner_protection_tick", () -> OwnerProtectionAbilityModule.onTick(tame, data));
                }
                TamePerformanceProfiler.run("attribute.passive_heal", () -> handlePassiveHeal(tame, data, now));
                TamePerformanceProfiler.run("attribute.comfort", () -> handleComfort(tame, data, now));
                TamePerformanceProfiler.run("attribute.regeneration", () -> handleAttributeRegeneration(tame, data, now));
                TamePerformanceProfiler.run("attribute.rejuvenation", () -> handleRejuvenation(tame, data));
                if (TameEntityAdapter.isStayingStill(tame)) {
                    continue;
                }
                if (!abilitiesBlocked && needsNearbyAllySupportScan(data)) {
                    List<LivingEntity> nearbySupportTames = collectOwnedNearbySupportTames(level, tame, TameEntityAdapter.ownerUuid(tame), 10.0D, supportCache);
                    TamePerformanceProfiler.run("ability.triage_pulse", () -> handleTriagePulse(level, tame, data, now, nearbySupportTames));
                    TamePerformanceProfiler.run("ability.cleanse_touch", () -> handleCleanseTouch(level, tame, data, now, nearbySupportTames));
                }
                if (!abilitiesBlocked && heavyPass && guardianRepulsePass) {
                    TamePerformanceProfiler.run("ability.guardian_repulse", () -> handleGuardianRepulse(level, tame, data, now));
                }
                if (!abilitiesBlocked && allowOffensive && heavyPass) {
                    TamePerformanceProfiler.run("ability.creeper_explosion", () -> handleCreeperExplosion(level, tame, data, currentTarget, now));
                }
                if (!abilitiesBlocked && allowOffensive) {
                    TamePerformanceProfiler.run("ability.arrow_shot", () -> handleArrowShot(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.fishing", () -> handleFishing(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.dash", () -> handleDash(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.flash", () -> handleFlash(level, tame, data, currentTarget, now));
                }
                if (!abilitiesBlocked && allowOffensive && heavyPass) {
                    TamePerformanceProfiler.run("ability.healing_bottle", () -> handleHealingBottle(level, tame, data, now));
                    TamePerformanceProfiler.run("ability.ghast_fireball", () -> handleGhastFireball(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.wither_skull", () -> handleWitherSkull(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.blaze_attack", () -> handleBlazeAttack(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.guardian_beam", () -> handleGuardianBeam(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.elder_guardian_beam", () -> handleElderGuardianBeam(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.trident", () -> handleTrident(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.crossbow", () -> handleCrossbow(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.evoker_fangs", () -> handleEvokerFangs(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.dragon_fireball", () -> handleDragonFireball(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.llama_spit", () -> handleLlamaSpit(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.berserker", () -> handleBerserker(tame, data, now));
                    TamePerformanceProfiler.run("ability.shulker_bullet", () -> handleShulkerBullet(level, tame, data, currentTarget, now));
                }
                if (!abilitiesBlocked && allowOffensive) {
                    TamePerformanceProfiler.run("ability.snowball_shot", () -> handleSnowballShot(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.ender_pearl_jump", () -> handleEnderPearlJump(tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.sky_launch_offense", () -> handleSkyLaunchOnOffense(level, tame, data, currentTarget, now));
                }
                if (!abilitiesBlocked && allowOffensive && heavyPass) {
                    TamePerformanceProfiler.run("ability.lightning_strike", () -> handleLightningStrike(level, tame, data, currentTarget, now));
                    TamePerformanceProfiler.run("ability.warden_scream", () -> handleWardenScream(level, tame, data, currentTarget, now));
                }
            }
            if (revivedDeadEntry) {
                TameRegistry.markDirty();
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
                TamePerformanceProfiler.run("feature.owner_protection_owner_hurt", () -> OwnerProtectionAbilityModule.onOwnerHurt(owner, event, OWNER_PROTECTION_HOOKS));
                if (!INTERNAL_SUPPORT_REDIRECT.get() && ownerHasNearbyReactiveSupport(owner)) {
                    TamePerformanceProfiler.run("feature.owner_support_responses", () -> handleOwnerSupportResponses(owner, event));
                }
            }

            LivingEntity attackerTame = resolveTameAttacker(event);
            if (attackerTame != null && TameEntityAdapter.isTame(attackerTame)) {
                TameData attackerData = TameRegistry.get(attackerTame.getUUID());
                if (attackerData != null
                        && (TameMode.byId(attackerData.mode) != TameMode.PASSIVE || isDuelOpponent(attackerTame, event.getEntity()))
                        && shouldUseOffensiveAbilities(attackerTame, attackerData, event.getEntity())
                        && !isProtectedPassiveWildlife(event.getEntity())
                        && !INTERNAL_BONUS_DAMAGE.get()) {
                    float beforeDamage = event.getAmount();
                    DAMAGE_DEBUG_CONTRIBUTORS.get().clear();
                    if (!TameEntityAdapter.isStayingStill(attackerTame)) {
                        TamePerformanceProfiler.run("feature.projectile_ability_damage_scaling", () -> applyProjectileAbilityDamageScaling(event));
                    }
                    TamePerformanceProfiler.run("feature.external_projectile_bonus_damage", () -> applyExternalProjectileBonusDamage(attackerData, event));
                    TamePerformanceProfiler.run("attribute.damage_bonuses", () -> applyAttributeDamageBonuses(attackerTame, attackerData, event));
                    if (!TameEntityAdapter.isStayingStill(attackerTame)) {
                        TamePerformanceProfiler.run("ability.battle_strength", () -> handleBattleStrength(attackerTame, attackerData, event));
                    }
                    debugDamage(attackerTame, attackerData, event, beforeDamage, event.getAmount());
                    DAMAGE_DEBUG_CONTRIBUTORS.get().clear();
                }
            }

            LivingEntity targetTame = event.getEntity();
            if (!TameEntityAdapter.isTame(targetTame)) return;
            TameData targetData = TameRegistry.get(targetTame.getUUID());
            if (targetData == null) return;

            if (!TameEntityAdapter.isStayingStill(targetTame) && !INTERNAL_SUPPORT_REDIRECT.get() && tameHasNearbyReactiveSupport(targetData)) {
                TamePerformanceProfiler.run("feature.ally_support_responses", () -> handleAllyTameSupportResponses(targetTame, targetData, event));
            }

            TamePerformanceProfiler.run("attribute.totem", () -> handleTotem(targetTame, targetData, event));
            TamePerformanceProfiler.run("attribute.defensive_mitigation", () -> handleDefensiveAttributeMitigation(targetTame, targetData, event));
            if (!TameEntityAdapter.isStayingStill(targetTame)) {
                TamePerformanceProfiler.run("ability.retaliation_slow", () -> handleRetaliationSlow(targetTame, targetData, event));
                TamePerformanceProfiler.run("ability.defensive_aura", () -> handleDefensiveAura(targetTame, targetData, event));
                TamePerformanceProfiler.run("feature.owner_protection_hurt", () -> OwnerProtectionAbilityModule.onTameHurt(targetTame, targetData, event, OWNER_PROTECTION_HOOKS));
                TamePerformanceProfiler.run("ability.sky_launch_defend", () -> handleSkyLaunchOnDefend(targetTame, targetData, event));
            }
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onHurt error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        try {
            if (event.getAmount() <= 0.0F) return;
            LivingEntity healed = event.getEntity();
            if (!(healed.level() instanceof ServerLevel level)) return;
            if (!healEventNeedsNearbySupport(healed)) return;
            Map<SupportCacheKey, List<LivingEntity>> supportCache = new HashMap<>();

            if (healed instanceof ServerPlayer owner) {
                for (LivingEntity supporter : collectOwnedNearbySupportTames(level, owner, owner.getUUID(), 10.0D, supportCache)) {
                    TameData data = TameRegistry.get(supporter.getUUID());
                    if (data == null) continue;
                    TamePerformanceProfiler.run("ability.revitalizing_presence", () -> handleRevitalizingPresence(level, supporter, data, owner, event.getAmount(), level.getGameTime(), collectOwnedNearbySupportTames(level, supporter, TameEntityAdapter.ownerUuid(supporter), 10.0D, supportCache)));
                }
                return;
            }

            if (TameEntityAdapter.isTame(healed) && TameEntityAdapter.ownerUuid(healed) != null) {
                LivingEntity healedTame = healed;
                for (LivingEntity supporter : collectOwnedNearbySupportTames(level, healedTame, TameEntityAdapter.ownerUuid(healedTame), 10.0D, supportCache)) {
                    if (supporter == healedTame) continue;
                    TameData data = TameRegistry.get(supporter.getUUID());
                    if (data == null) continue;
                    TamePerformanceProfiler.run("ability.revitalizing_presence", () -> handleRevitalizingPresence(level, supporter, data, healedTame, event.getAmount(), level.getGameTime(), collectOwnedNearbySupportTames(level, supporter, TameEntityAdapter.ownerUuid(supporter), 10.0D, supportCache)));
                }
            }
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onHeal error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void handleCreeperExplosion(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "creeper_explosion")) return;
        if (target == null || !target.isAlive()) return;
        if (!isReady(data, "explode", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "creeper_explosion"));
        float damage = offensiveAbilityCastDamage(data, "creeper_explosion", levelValue);
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
        setAbilityCooldown(tame, data, "creeper_explosion", "explode", now, 200);
        debugAbilityUse(tame, "creeper_explosion");
    }

    private static void handleArrowShot(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "arrow_shot")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "arrow", now)) return;

        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "arrow_shot"));
        float velocity = 1.6F + (levelValue - 1) * 0.1F;

        Arrow arrow = new TimedTameArrow(level, tame);
        arrow.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        arrow.setBaseDamage(offensiveAbilityCastDamage(data, "arrow_shot", levelValue));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.shoot(direction.x, direction.y, direction.z, velocity, 0.0F);
        TameProjectileTimeoutEvents.track(arrow, SHORT_PROJECTILE_TICKS);
        level.addFreshEntity(arrow);

        setAbilityCooldown(tame, data, "arrow_shot", "arrow", now, 60);
        debugAbilityUse(tame, "arrow_shot");
    }

    private static void handleGhastFireball(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "ghast_fireball")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "fireball", now)) return;

        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "ghast_fireball"));
        int power = Math.max(1, Math.min(3, levelValue));

        NoGriefLargeFireball fireball = new NoGriefLargeFireball(level, tame, direction.x, direction.y, direction.z, power);
        fireball.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        TameProjectileTimeoutEvents.track(fireball, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(fireball);

        setAbilityCooldown(tame, data, "ghast_fireball", "fireball", now, 100);
        debugAbilityUse(tame, "ghast_fireball");
    }

    private static void handleWitherSkull(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "wither_skull")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "wither_skull_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "wither_skull"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        WitherSkull skull = new TimedTameWitherSkull(level, tame, direction.x, direction.y, direction.z);
        skull.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        // Keep pet wither skull non-griefing.
        skull.setDangerous(false);
        TameProjectileTimeoutEvents.track(skull, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(skull);

        setAbilityCooldown(tame, data, "wither_skull", "wither_skull_tick", now, 80);
        debugAbilityUse(tame, "wither_skull");
    }

    private static void handleBlazeAttack(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "blaze_attack")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "blaze_attack_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "blaze_attack"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        SmallFireball fireball = new TimedTameSmallFireball(level, tame, direction.x, direction.y, direction.z);
        fireball.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        setProjectileDamage(fireball, "blaze_attack", offensiveAbilityCastDamage(data, "blaze_attack", levelValue));
        TameProjectileTimeoutEvents.track(fireball, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(fireball);
        level.playSound(null, tame.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "blaze_attack", "blaze_attack_tick", now, 40);
        debugAbilityUse(tame, "blaze_attack");
    }

    private static void handleGuardianBeam(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "guardian_beam")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "guardian_beam_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_beam"));
        float damage = offensiveAbilityCastDamage(data, "guardian_beam", levelValue);
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

        setAbilityCooldown(tame, data, "guardian_beam", "guardian_beam_tick", now, 70);
        debugAbilityUse(tame, "guardian_beam");
    }

    private static void handleElderGuardianBeam(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "elder_guardian_beam")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "elder_guardian_beam_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "elder_guardian_beam"));
        float damage = offensiveAbilityCastDamage(data, "elder_guardian_beam", levelValue);
        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), damage);
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100 + levelValue * 20, Math.max(0, levelValue - 1)));
        level.sendParticles(ParticleTypes.BUBBLE_POP, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 14), 0.5D, 0.4D, 0.5D, 0.02D);

        setAbilityCooldown(tame, data, "elder_guardian_beam", "elder_guardian_beam_tick", now, 120);
        debugAbilityUse(tame, "elder_guardian_beam");
    }

    private static void renderGuardianLockOnBeam(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;

        boolean guardian = LevelSystem.hasAbility(data, "guardian_beam");
        boolean elder = LevelSystem.hasAbility(data, "elder_guardian_beam");
        if (!guardian && !elder) return;
        if (!tame.hasLineOfSight(target)) return;
        if (now % GUARDIAN_LOCK_ON_PARTICLE_RATE != 0L) return;

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

    private static void handleTrident(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "trident")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "trident_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "trident"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        ThrownTrident trident = new TimedTameTrident(level, tame, new ItemStack(Items.TRIDENT));
        trident.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        trident.pickup = AbstractArrow.Pickup.DISALLOWED;
        trident.setBaseDamage(offensiveAbilityCastDamage(data, "trident", levelValue));
        trident.shoot(direction.x, direction.y, direction.z, 2.5F, 0.0F);
        TameProjectileTimeoutEvents.track(trident, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(trident);
        level.playSound(null, tame.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "trident", "trident_tick", now, 90);
        debugAbilityUse(tame, "trident");
    }

    private static void handleCrossbow(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "crossbow")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "crossbow_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "crossbow"));
        Vec3 dir = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        int arrowCount = Math.max(1, levelValue);
        float totalCastDamage = offensiveAbilityCastDamage(data, "crossbow", levelValue);
        double perArrowDamage = totalCastDamage / arrowCount;
        int center = (arrowCount - 1) / 2;
        for (int i = 0; i < arrowCount; i++) {
            int spreadIndex = i - center;
            Arrow arrow = new TimedTameArrow(level, tame);
            arrow.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            arrow.setBaseDamage(perArrowDamage);
            arrow.shoot(dir.x, dir.y, dir.z, 2.0F, 4.0F * spreadIndex);
            TameProjectileTimeoutEvents.track(arrow, SHORT_PROJECTILE_TICKS);
            level.addFreshEntity(arrow);
        }

        setAbilityCooldown(tame, data, "crossbow", "crossbow_tick", now, 80);
        debugAbilityUse(tame, "crossbow");
    }

    private static void handleFishing(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "fishing")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "fishing_hook_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "fishing"));
        Vec3 fromTargetToTame = tame.position().subtract(target.position());
        if (fromTargetToTame.lengthSqr() < 0.001D) return;

        double resistanceScale = magneticResistanceMultiplier(target);
        Vec3 pull = fromTargetToTame.normalize().scale((0.35D + levelValue * 0.04D) * resistanceScale);
        double up = (0.45D + levelValue * 0.05D) * resistanceScale;
        Vec3 newMotion = target.getDeltaMovement().scale(0.35D).add(pull.x, up, pull.z);
        target.setDeltaMovement(newMotion);
        target.hurtMarked = true;

        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 10), 0.3D, 0.2D, 0.3D, 0.02D);
        level.playSound(null, target.blockPosition(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.HOSTILE, 0.9F, 1.0F);

        setAbilityCooldown(tame, data, "fishing", "fishing_hook_tick", now, 90L);
        debugAbilityUse(tame, "fishing");
    }

    private static void handleDash(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "dash")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if ((target instanceof Player || TameEntityAdapter.isTame(target)) && !isDuelOpponent(tame, target)) return;
        if (!isReady(data, "dash_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "dash"));
        float damage = offensiveAbilityCastDamage(data, "dash", levelValue);
        if (!performDashStrike(level, tame, target, levelValue, damage)) return;

        setAbilityCooldown(tame, data, "dash", "dash_tick", now, 90L);
        debugAbilityUse(tame, "dash");
    }

    private static void handleFlash(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "flash")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)
                || ((target instanceof Player || TameEntityAdapter.isTame(target)) && !isDuelOpponent(tame, target))) {
            clearFlashChain(data);
            return;
        }

        long remaining = data.cooldowns.getOrDefault(FLASH_CHAIN_REMAINING_KEY, 0L);
        long nextStrikeAt = data.cooldowns.getOrDefault(FLASH_CHAIN_NEXT_TICK_KEY, 0L);

        if (remaining <= 0L) {
            if (!isReady(data, "flash_tick", now)) return;
            remaining = FLASH_DASH_COUNT;
            nextStrikeAt = now;
            setCooldown(data, FLASH_CHAIN_REMAINING_KEY, remaining);
            setCooldown(data, FLASH_CHAIN_NEXT_TICK_KEY, nextStrikeAt);
            setAbilityCooldown(tame, data, "flash", "flash_tick", now, FLASH_BASE_COOLDOWN_TICKS);
            debugAbilityUse(tame, "flash");
        }

        if (now < nextStrikeAt) {
            return;
        }

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "flash"));
        float damage = offensiveAbilityCastDamage(data, "dash", levelValue);
        performDashStrike(level, tame, target, levelValue, damage);

        remaining = Math.max(0L, remaining - 1L);
        if (remaining > 0L) {
            setCooldown(data, FLASH_CHAIN_REMAINING_KEY, remaining);
            setCooldown(data, FLASH_CHAIN_NEXT_TICK_KEY, now + FLASH_DASH_INTERVAL_TICKS);
        } else {
            clearFlashChain(data);
        }
    }

    private static void clearFlashChain(TameData data) {
        if (data == null) {
            return;
        }
        data.cooldowns.remove(FLASH_CHAIN_REMAINING_KEY);
        data.cooldowns.remove(FLASH_CHAIN_NEXT_TICK_KEY);
    }

    private static boolean performDashStrike(ServerLevel level, LivingEntity tame, LivingEntity target, int levelValue, float damage) {
        if (level == null || tame == null || target == null || !target.isAlive() || isFriendly(tame, target)) {
            return false;
        }
        if ((target instanceof Player || TameEntityAdapter.isTame(target)) && !isDuelOpponent(tame, target)) {
            return false;
        }
        Vec3 start = tame.position();
        Vec3 toTarget = target.position().subtract(start);
        double distance = toTarget.length();
        if (distance < 2.0D) return false;
        Vec3 dir = toTarget.normalize();
        double dashDistance = Math.min(4.0D + levelValue * 0.8D, Math.max(2.0D, distance));
        Vec3 end = resolveDashEndpoint(level, tame, start, dir, dashDistance);
        if (end.distanceToSqr(start) < 0.25D) return false;

        AABB sweep = new AABB(start, end).inflate(1.1D, 0.8D, 1.1D);
        for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, sweep)) {
            if (!nearby.isAlive()) continue;
            if (nearby == tame) continue;
            if ((nearby instanceof Player || TameEntityAdapter.isTame(nearby)) && !isDuelOpponent(tame, nearby)) continue;
            if (isFriendly(tame, nearby)) continue;
            LevelSystem.trackDamage(nearby, tame);
            applyInternalBonusDamage(nearby, tame, damage);
        }

        tame.teleportTo(end.x, Math.max(level.getMinBuildHeight() + 1, end.y), end.z);
        tame.setDeltaMovement(dir.x * 0.9D, 0.15D, dir.z * 0.9D);
        tame.hurtMarked = true;
        TameCommands.queueClientReloadForTame(tame);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, tame.getX(), tame.getY(0.6D), tame.getZ(), capParticles(tame, 6), 0.25D, 0.1D, 0.25D, 0.0D);
        level.playSound(null, tame.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.2F);
        return true;
    }

    private static Vec3 resolveDashEndpoint(ServerLevel level, LivingEntity tame, Vec3 start, Vec3 dir, double dashDistance) {
        Vec3 desired = start.add(dir.scale(dashDistance));
        Vec3 rayStart = start.add(0.0D, Math.max(0.25D, tame.getBbHeight() * 0.5D), 0.0D);
        Vec3 rayEnd = desired.add(0.0D, Math.max(0.25D, tame.getBbHeight() * 0.5D), 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(rayStart, rayEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, tame));
        if (hit.getType() != BlockHitResult.Type.BLOCK) {
            return desired;
        }
        double standOff = Math.max(0.6D, tame.getBbWidth() * 0.5D + 0.1D);
        Vec3 blocked = hit.getLocation().subtract(dir.scale(standOff));
        Vec3 candidate = new Vec3(blocked.x, start.y, blocked.z);
        BlockPos candidatePos = BlockPos.containing(candidate.x, Math.max(level.getMinBuildHeight() + 1, candidate.y), candidate.z);
        if (!level.getBlockState(candidatePos).canBeReplaced()) {
            candidate = candidate.add(0.0D, 1.0D, 0.0D);
        }
        return candidate;
    }

    private static void handleEvokerFangs(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
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

        setAbilityCooldown(tame, data, "evoker_fangs", "evoker_fangs_tick", now, 100);
        debugAbilityUse(tame, "evoker_fangs");
    }

    private static void handleDragonFireball(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "dragon_fireball")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "dragon_fireball_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "dragon_fireball"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        DragonFireball fireball = new TimedTameDragonFireball(level, tame, direction.x, direction.y, direction.z);
        fireball.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        setProjectileDamage(fireball, "dragon_fireball", offensiveAbilityCastDamage(data, "dragon_fireball", levelValue));
        TameProjectileTimeoutEvents.track(fireball, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(fireball);
        level.playSound(null, tame.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 1.0F, 1.0F);

        setAbilityCooldown(tame, data, "dragon_fireball", "dragon_fireball_tick", now, 140);
        debugAbilityUse(tame, "dragon_fireball");
    }

    private static void handleLlamaSpit(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "llama_spit")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "llama_spit_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "llama_spit"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        LlamaSpit spit = new TimedTameLlamaSpit(level, tame);
        spit.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        spit.shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
        setProjectileDamage(spit, "llama_spit", offensiveAbilityCastDamage(data, "llama_spit", levelValue));
        TameProjectileTimeoutEvents.track(spit, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(spit);

        setAbilityCooldown(tame, data, "llama_spit", "llama_spit_tick", now, 50);
        debugAbilityUse(tame, "llama_spit");
    }

    private static void handleBerserker(LivingEntity tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "berserker")) return;
        if (!isReady(data, "berserker_tick", now)) return;
        float hpPercent = tame.getHealth() / Math.max(1.0F, tame.getMaxHealth());
        if (hpPercent > 0.20F) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "berserker"));
        tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 400, Math.max(0, levelValue - 1)));
        tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 400, defensiveAuraAmplifier(levelValue)));
        setAbilityCooldown(tame, data, "berserker", "berserker_tick", now, 2000);
        grantSupportUtilityXp(tame, data, tame, now, 1, 2.0F + levelValue, 0.5F);
        debugAbilityUse(tame, "berserker");
    }

    private static void handleShulkerBullet(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "shulker_bullet")) return;
        if (target == null || !target.isAlive() || isFriendly(tame, target)) return;
        if (!isReady(data, "shulker_bullet_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "shulker_bullet"));
        Direction.Axis axis = Math.abs(target.getX() - tame.getX()) > Math.abs(target.getZ() - tame.getZ())
                ? Direction.Axis.X
                : Direction.Axis.Z;
        ShulkerBullet bullet = new TimedTameShulkerBullet(level, tame, target, axis, levelValue);
        bullet.setPos(tame.getX(), tame.getEyeY(), tame.getZ());
        TameProjectileTimeoutEvents.track(bullet, LONG_PROJECTILE_TICKS);
        level.addFreshEntity(bullet);
        level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5D), target.getZ(), capParticles(tame, 16), 0.4D, 0.5D, 0.4D, 0.01D);
        setAbilityCooldown(tame, data, "shulker_bullet", "shulker_bullet_tick", now, 200L);
        debugAbilityUse(tame, "shulker_bullet");
    }

    private static void handleSnowballShot(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "snowball_shot")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "snow", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "snowball_shot"));
        Vec3 direction = target.getEyePosition().subtract(tame.getEyePosition()).normalize();
        Snowball snowball = new TimedTameSnowball(level, tame);
        snowball.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        snowball.shoot(direction.x, direction.y, direction.z, 1.5F, 0.0F);
        TameProjectileTimeoutEvents.track(snowball, SHORT_PROJECTILE_TICKS);
        level.addFreshEntity(snowball);
        float damage = offensiveAbilityCastDamage(data, "snowball_shot", levelValue);
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);

        setAbilityCooldown(tame, data, "snowball_shot", "snow", now, 20);
        debugAbilityUse(tame, "snowball_shot");
    }

    private static void handleEnderPearlJump(LivingEntity tame, TameData data, LivingEntity target, long now) {
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
        TameCommands.queueClientReloadForTame(tame);
        tame.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        setAbilityCooldown(tame, data, "ender_pearl_jump", "pearl", now, 100);
        debugAbilityUse(tame, "ender_pearl_jump");
    }

    private static void handleBattleStrength(LivingEntity tame, TameData data, LivingHurtEvent event) {
        if (!LevelSystem.hasAbility(data, "battle_strength")) return;
        if (tame.getRandom().nextDouble() > 0.10D) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "battle_strength"));
        int amplifier = Math.max(0, levelValue - 1);
        int affected = 0;
        for (LivingEntity nearby : tame.level().getEntitiesOfClass(LivingEntity.class, tame.getBoundingBox().inflate(3))) {
            if (!isFriendly(tame, nearby)) continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, amplifier));
            affected++;
        }
        grantSupportUtilityXp(tame, data, tame, tame.level().getGameTime(), affected, 1.0F + amplifier, 0.5F);
        debugAbilityUse(tame, "battle_strength");
    }

    private static void handleDefensiveAura(LivingEntity targetTame, TameData data, LivingHurtEvent event) {
        if (!LevelSystem.hasAbility(data, "defensive_aura")) return;
        if (targetTame.getRandom().nextDouble() > 0.10D) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "defensive_aura"));
        int amplifier = defensiveAuraAmplifier(levelValue);
        int duration = 100;
        int affected = 0;
        for (LivingEntity nearby : targetTame.level().getEntitiesOfClass(LivingEntity.class, targetTame.getBoundingBox().inflate(3))) {
            if (!isFriendly(targetTame, nearby)) continue;
            nearby.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amplifier));
            affected++;
        }
        grantSupportUtilityXp(targetTame, data, targetTame, targetTame.level().getGameTime(), affected, 1.0F + amplifier, 0.5F);
        applySupportActivationVisual(targetTame, "defensive_aura");
        debugAbilityUse(targetTame, "defensive_aura");
    }

    private static int defensiveAuraAmplifier(int levelValue) {
        int displayedResistance = 1;
        int threshold = 1;
        while (displayedResistance < 4) {
            int nextLevel = displayedResistance + 1;
            threshold += nextLevel * nextLevel;
            if (levelValue < threshold) {
                break;
            }
            displayedResistance = nextLevel;
        }
        return displayedResistance - 1;
    }

    private static void handleTotem(LivingEntity tame, TameData data, LivingHurtEvent event) {
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
        grantSupportXp(tame, data, tame, now, tame.getHealth(), 0.75F);
        applySupportActivationVisual(tame, "totem");
        if (tame.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, tame.getX(), tame.getY(0.6D), tame.getZ(), capParticles(tame, 32), 0.4D, 0.5D, 0.4D, 0.1D);
            level.playSound(null, tame.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
        debugAbilityUse(tame, "totem");
    }

    private static void handleLightningStrike(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "lightning_strike")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "lightning_strike_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "lightning_strike"));
        float damage = offensiveAbilityCastDamage(data, "lightning_strike", levelValue);
        spawnLightningVisual(level, target.getX(), target.getY(), target.getZ(), tame);
        LevelSystem.trackDamage(target, tame);
        target.hurt(tame.damageSources().mobAttack(tame), damage);
        setAbilityCooldown(tame, data, "lightning_strike", "lightning_strike_tick", now, 500);
        debugAbilityUse(tame, "lightning_strike");
    }

    private static void handleWardenScream(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        if (!LevelSystem.hasAbility(data, "warden_scream")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "warden_scream_tick", now)) return;
        if (!tame.hasLineOfSight(target)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "warden_scream"));
        double maxRange = 16.0D + levelValue * 2.0D;
        if (tame.distanceTo(target) > maxRange) return;

        float damage = offensiveAbilityCastDamage(data, "warden_scream", levelValue);
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
        setAbilityCooldown(tame, data, "warden_scream", "warden_scream_tick", now, 120);
        debugAbilityUse(tame, "warden_scream");
    }

    private static void handleGuardianRepulse(ServerLevel level, LivingEntity tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "guardian_repulse")) return;
        if (!isReady(data, "guardian_repulse_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_repulse"));
        double radius = 6.0D + levelValue * 0.6D;
        double chance = Math.min(0.90D, 0.12D + levelValue * 0.04D);
        int affected = 0;

        for (Monster monster : level.getEntitiesOfClass(Monster.class, tame.getBoundingBox().inflate(radius))) {
            if (monster == null || !monster.isAlive()) continue;
            if (tame.getRandom().nextDouble() > chance) continue;
            TameEntityAdapter.setTarget(monster, tame);
            affected++;
        }
        if (affected > 0) {
            grantSupportUtilityXp(tame, data, tame, now, affected, 1.0F + 0.25F * levelValue, 0.5F);
            applySupportActivationVisual(tame, "guardian_repulse");
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, tame.getX(), tame.getY(0.9D), tame.getZ(), capParticles(tame, 6 + affected), 0.3D, 0.4D, 0.3D, 0.01D);
            level.playSound(null, tame.blockPosition(), SoundEvents.IRON_GOLEM_HURT, SoundSource.NEUTRAL, 0.7F, 1.0F);
            debugAbilityUse(tame, "guardian_repulse");
        }
        setAbilityCooldown(tame, data, "guardian_repulse", "guardian_repulse_tick", now, 400L);
    }

    private static void spawnEvokerFangLine(ServerLevel level, LivingEntity tame, LivingEntity target) {
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

    private static void spawnEvokerFangRing(ServerLevel level, LivingEntity tame) {
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

    private static void handleAttributeRegeneration(LivingEntity tame, TameData data, long now) {
        int regenLevel = attributeLevel(data, "regeneration");
        if (regenLevel <= 0) return;
        if (!isReady(data, "attr_regen", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) return;

        float healAmount = 0.5F + 0.5F * regenLevel;
        if (!TameCommands.consumeHungerForNaturalHeal(data, tame, healAmount)) {
            return;
        }
        tame.heal(healAmount);
        long cooldown = regenLevel >= 5 ? 60L : regenLevel >= 3 ? 70L : 80L;
        setAbilityCooldown(tame, data, "regeneration", "attr_regen", now, cooldown);
        grantSupportXp(tame, data, tame, now, healAmount, 0.5F);
        debugAbilityUse(tame, "regeneration");
    }

    private static void handlePassiveHeal(LivingEntity tame, TameData data, long now) {
        if (!isReady(data, "passive_heal_tick", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) {
            setCooldown(data, "passive_heal_tick", now + passiveHealInterval(tame));
            return;
        }
        float heal = 1.0F;
        if (tame.level() instanceof ServerLevel level && TameEntityAdapter.ownerUuid(tame) != null) {
            for (LivingEntity supporter : collectOwnedNearbySupportTames(level, tame, TameEntityAdapter.ownerUuid(tame), 2.0D)) {
                if (supporter == tame) continue;
                TameData supporterData = TameRegistry.get(supporter.getUUID());
                if (supporterData == null || !LevelSystem.hasAbility(supporterData, "revitalizing_presence")) continue;
                heal += Math.max(1, LevelSystem.getAbilityLevel(supporterData, "revitalizing_presence"));
            }
        }
        if (!TameCommands.consumeHungerForNaturalHeal(data, tame, heal)) {
            return;
        }
        tame.heal(heal);
        setCooldown(data, "passive_heal_tick", now + passiveHealInterval(tame));
    }

    private static long passiveHealInterval(LivingEntity tame) {
        LivingEntity target = TameEntityAdapter.target(tame);
        return target != null && target.isAlive() ? 400L : 100L;
    }

    private static void handleComfort(LivingEntity tame, TameData data, long now) {
        int comfortLevel = attributeLevel(data, "comfort");
        if (comfortLevel <= 0) return;
        if (!isReady(data, "comfort_tick", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) {
            setCooldown(data, "comfort_tick", now + 100L);
            return;
        }
        if (isInBattle(tame)) {
            setCooldown(data, "comfort_tick", now + 40L);
            return;
        }
        tame.heal(comfortLevel);
        setCooldown(data, "comfort_tick", now + 100L);
        grantSupportXp(tame, data, tame, now, comfortLevel, 0.5F);
        debugAbilityUse(tame, "comfort");
    }

    private static void handleRejuvenation(LivingEntity tame, TameData data) {
        // Rejuvenation now uses the legacy DI server-side runtime path in CommonProxy.
    }

    private static void handleHealingBottle(ServerLevel level, LivingEntity tame, TameData data, long now) {
        if (!LevelSystem.hasAbility(data, "healing_bottle")) return;
        if (!isReady(data, "healing_bottle_tick", now)) return;
        if (tame.getHealth() >= tame.getMaxHealth()) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "healing_bottle"));
        int effectiveLevel = Math.min(levelValue, 4);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(potion, effectiveLevel >= 4 ? Potions.STRONG_HEALING : Potions.HEALING);

        ThrownPotion thrownPotion = new TimedTameThrownPotion(level, tame);
        thrownPotion.setItem(potion);
        thrownPotion.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        thrownPotion.setDeltaMovement(0.0D, 0.65D + (effectiveLevel * 0.03D), 0.0D);
        TameProjectileTimeoutEvents.track(thrownPotion, SHORT_PROJECTILE_TICKS);
        level.addFreshEntity(thrownPotion);

        level.playSound(null, tame.blockPosition(), SoundEvents.SPLASH_POTION_THROW, SoundSource.NEUTRAL, 0.7F, 1.0F);
        long cooldown = Math.max(60L, 220L - (Math.max(0, effectiveLevel - 1) * 15L));
        setAbilityCooldown(tame, data, "healing_bottle", "healing_bottle_tick", now, cooldown);
        grantSupportXp(tame, data, tame, now, effectiveLevel >= 4 ? 8.0F : 4.0F, 0.5F);
        applySupportActivationVisual(tame, "healing_bottle");
        debugAbilityUse(tame, "healing_bottle");
    }

    private static void handleTriagePulse(ServerLevel level, LivingEntity tame, TameData data, long now, List<LivingEntity> nearbySupportTames) {
        if (!LevelSystem.hasAbility(data, "triage_pulse")) return;
        if (!isReady(data, "triage_pulse_tick", now)) return;
        LivingEntity patient = findLowestHealthAlly(level, tame, 10.0D, true, nearbySupportTames);
        if (patient == null || patient.getHealth() >= patient.getMaxHealth()) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "triage_pulse"));
        float heal = 1.5F + 0.75F * levelValue;
        float before = patient.getHealth();
        patient.heal(heal);
        float actualHealed = Math.max(0.0F, patient.getHealth() - before);
        setAbilityCooldown(tame, data, "triage_pulse", "triage_pulse_tick", now, 120L);
        grantSupportXp(tame, data, patient, now, actualHealed, 0.5F);
        applySupportActivationVisual(tame, "triage_pulse");
        if (level != null) {
            level.sendParticles(ParticleTypes.HEART, patient.getX(), patient.getY(0.6D), patient.getZ(), capParticles(tame, 4 + levelValue), 0.25D, 0.25D, 0.25D, 0.02D);
        }
        debugAbilityUse(tame, "triage_pulse");
    }

    private static void handleCleanseTouch(ServerLevel level, LivingEntity tame, TameData data, long now, List<LivingEntity> nearbySupportTames) {
        if (!LevelSystem.hasAbility(data, "cleanse_touch")) return;
        if (!isReady(data, "cleanse_touch_tick", now)) return;
        LivingEntity patient = findFirstDebuffedAlly(level, tame, 10.0D, nearbySupportTames);
        if (patient == null) return;

        MobEffectInstance harmful = firstHarmfulEffect(patient);
        if (harmful == null) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "cleanse_touch"));
        patient.removeEffect(harmful.getEffect());
        long cooldown = Math.max(60L, 180L - Math.max(0, levelValue - 1) * 15L);
        setAbilityCooldown(tame, data, "cleanse_touch", "cleanse_touch_tick", now, cooldown);
        grantSupportXp(tame, data, patient, now, 2.0F + harmful.getAmplifier(), 0.5F);
        applySupportActivationVisual(tame, "cleanse_touch");
        if (level != null) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getY(0.6D), patient.getZ(), capParticles(tame, 5), 0.25D, 0.25D, 0.25D, 0.02D);
        }
        debugAbilityUse(tame, "cleanse_touch");
    }

    private static void handleSkyLaunchOnOffense(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now) {
        trySkyLaunch(level, tame, data, target, now, 0.10D);
    }

    private static void handleSkyLaunchOnDefend(LivingEntity tame, TameData data, LivingHurtEvent event) {
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

    private static void trySkyLaunch(ServerLevel level, LivingEntity tame, TameData data, LivingEntity target, long now, double baseChance) {
        if (!LevelSystem.hasAbility(data, "sky_launch")) return;
        if (target == null || !target.isAlive()) return;
        if (isFriendly(tame, target)) return;
        if (!isReady(data, "sky_launch_tick", now)) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "sky_launch"));
        double chance = Math.min(0.90D, baseChance + levelValue * 0.04D);
        if (tame.getRandom().nextDouble() > chance) return;

        float damage = offensiveAbilityCastDamage(data, "sky_launch", levelValue);
        LevelSystem.trackDamage(target, tame);
        applyInternalBonusDamage(target, tame, damage);
        Vec3 motion = target.getDeltaMovement();
        double launchY = scaledSkyLaunchVelocity(levelValue, target);
        target.setDeltaMovement(motion.x * 0.6D, launchY, motion.z * 0.6D);
        target.fallDistance = 0.0F;
        target.hurtMarked = true;
        level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY(0.2D), target.getZ(), capParticles(tame, 10), 0.25D, 0.1D, 0.25D, 0.03D);
        level.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 0.7F, 1.2F);
        setAbilityCooldown(tame, data, "sky_launch", "sky_launch_tick", now, 70L);
        debugAbilityUse(tame, "sky_launch");
    }

    public static double scaledSkyLaunchVelocity(int levelValue, LivingEntity target) {
        int safeLevel = Math.max(1, levelValue);
        double baseLaunch = Math.min(2.1D, 0.55D + safeLevel * 0.12D);
        if (target == null) {
            return baseLaunch;
        }
        double maxHealth = Math.max(1.0D, target.getMaxHealth());
        double healthPenalty = 1.0D + (maxHealth / 55.0D);
        return Math.max(0.03D, baseLaunch / healthPenalty);
    }

    private static void applyAttributeDamageBonuses(LivingEntity tame, TameData data, LivingHurtEvent event) {
        float damage = event.getAmount();
        if (damage <= 0.0F) return;
        LivingEntity target = event.getEntity();

        int killerLevel = attributeLevel(data, "killer");
        if (killerLevel > 0) {
            float hpPct = target.getHealth() / Math.max(1.0F, target.getMaxHealth());
            float missing = Mth.clamp(1.0F - hpPct, 0.0F, 1.0F);
            float multiplier = 1.0F + missing * attributeConditionalMultiplier(killerLevel);
            event.setAmount(event.getAmount() * multiplier);
            damage = event.getAmount();
            noteDamageContributor("killer");
            debugAbilityUse(tame, "killer");
        }

        int pacifistLevel = attributeLevel(data, "pacifist");
        if (pacifistLevel > 0) {
            float hpPct = Mth.clamp(target.getHealth() / Math.max(1.0F, target.getMaxHealth()), 0.0F, 1.0F);
            float multiplier = 1.0F + hpPct * attributeConditionalMultiplier(pacifistLevel);
            event.setAmount(event.getAmount() * multiplier);
            damage = event.getAmount();
            noteDamageContributor("pacifist");
            debugAbilityUse(tame, "pacifist");
        }

        int bossKillerLevel = attributeLevel(data, "bosskiller");
        if (bossKillerLevel > 0) {
            int threshold = Math.max(40, 100 - (bossKillerLevel - 1) * 10);
            int stacks = Math.max(0, (int) (target.getMaxHealth() / threshold));
            if (stacks > 0) {
                int amp = Math.min(2, Math.max(0, (bossKillerLevel - 1) / 2));
                int duration = 60 + bossKillerLevel * 20;
                tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, amp, false, false, true));
                tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, amp, false, false, true));
                tame.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, amp, false, false, true));
                debugAbilityUse(tame, "bosskiller");
            }
        }

        int lifestealLevel = attributeLevel(data, "lifesteal");
        if (lifestealLevel > 0 && isBaseAttackHit(tame, event)) {
            float heal = event.getAmount() * (0.04F + (lifestealLevel * 0.02F));
            tame.heal(heal);
            debugAbilityUse(tame, "lifesteal");
        }

        int pierceLevel = attributeLevel(data, "pierce");
        if (pierceLevel > 0 && !event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) {
            float pierced = applyArmorPierce(event.getAmount(), target, pierceLevel);
            if (pierced > event.getAmount()) {
                event.setAmount(pierced);
                damage = event.getAmount();
                noteDamageContributor("pierce");
                debugAbilityUse(tame, "pierce");
            }
        }

        int firefangLevel = attributeLevel(data, "firefang");
        if (firefangLevel > 0) {
            event.getEntity().setSecondsOnFire(2 + firefangLevel);
            float bonus = firefangLevel >= 5 ? 2.0F : firefangLevel >= 3 ? 1.0F : 0.0F;
            if (bonus > 0.0F) {
                event.setAmount(event.getAmount() + bonus);
                damage = event.getAmount();
                noteDamageContributor("firefang");
            }
            debugAbilityUse(tame, "firefang");
        }

        int poisonFangLevel = attributeLevel(data, "poison_fang");
        if (poisonFangLevel > 0) {
            int duration = 40 + poisonFangLevel * 20;
            int amplifier = poisonFangLevel >= 3 ? 1 : 0;
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON, duration, amplifier));
            debugAbilityUse(tame, "poison_fang");
        }

        int witherfangLevel = attributeLevel(data, "witherfang");
        if (witherfangLevel > 0) {
            int duration = 40 + witherfangLevel * 20;
            int amplifier = witherfangLevel >= 4 ? 1 : 0;
            event.getEntity().addEffect(new MobEffectInstance(MobEffects.WITHER, duration, amplifier));
            debugAbilityUse(tame, "witherfang");
        }

        int smiteLevel = attributeLevel(data, "smite");
        if (smiteLevel > 0 && target.getMobType() == MobType.UNDEAD) {
            float extra = event.getAmount() * (0.15F + 0.10F * smiteLevel);
            event.setAmount(event.getAmount() + extra);
            noteDamageContributor("smite");
            debugAbilityUse(tame, "smite");
        }

        int arthropodLevel = attributeLevel(data, "bane_of_arthropods");
        if (arthropodLevel > 0 && target.getMobType() == MobType.ARTHROPOD) {
            float extra = event.getAmount() * (0.15F + 0.10F * arthropodLevel);
            event.setAmount(event.getAmount() + extra);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 + arthropodLevel * 10, arthropodLevel >= 5 ? 2 : arthropodLevel >= 3 ? 1 : 0));
            noteDamageContributor("bane_of_arthropods");
            debugAbilityUse(tame, "bane_of_arthropods");
        }

        int lightningfangLevel = attributeLevel(data, "lightningfang");
        if (lightningfangLevel > 0 && tame.level() instanceof ServerLevel serverLevel) {
            if (tame.getRandom().nextDouble() <= lightningfangChance(lightningfangLevel)) {
                noteDamageContributor("lightningfang");
                spawnLightningVisual(serverLevel, target.getX(), target.getY(), target.getZ(), tame);
                LevelSystem.trackDamage(target, tame);
                applyInternalBonusDamage(
                        target,
                        tame,
                        singleTargetDamage(lightningfangDamage(data, lightningfangLevel))
                );
                debugAbilityUse(tame, "lightningfang");
            }
        }

        int chainLightningLevel = attributeLevel(data, "chain_lightning");
        if (chainLightningLevel > 0 && tame.level() instanceof ServerLevel serverLevel && isBaseAttackHit(tame, event) && !isFriendly(tame, target)) {
            applyChainLightning(serverLevel, tame, data, target, event.getAmount(), chainLightningLevel);
        }

        int stealLevel = attributeLevel(data, "positive_effect_steal");
        if (stealLevel > 0 && tame.getRandom().nextDouble() < Math.min(0.38D, 0.08D + 0.06D * stealLevel)) {
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

    private static void handleNamedAttributeEffects(LivingEntity tame, TameData data) {
        applyNamedAttributeEffect(tame, data, "speed", MobEffects.MOVEMENT_SPEED);
        applyNamedAttributeEffect(tame, data, "strength", MobEffects.DAMAGE_BOOST);
        applyNamedAttributeEffect(tame, data, "resistance", MobEffects.DAMAGE_RESISTANCE);
        applyNamedAttributeEffect(tame, data, "fire_resistance", MobEffects.FIRE_RESISTANCE);
        handlePoisonResistance(tame, data);
        applyNamedAttributeEffect(tame, data, "jump_boost", MobEffects.JUMP);
        handleWallClimber(tame, data);
    }

    private static void applyNamedAttributeEffect(LivingEntity tame, TameData data, String attribute, net.minecraft.world.effect.MobEffect effect) {
        int level = attributeLevel(data, attribute);
        if (level <= 0) return;
        int amplifier = binaryAttribute(attribute) ? 0 : "resistance".equals(attribute) ? defensiveAuraAmplifier(Math.min(level, 30)) : breakpointAmplifier(level);
        MobEffectInstance current = tame.getEffect(effect);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() > 20) {
            return;
        }
        tame.addEffect(new MobEffectInstance(effect, 200, amplifier, false, false, true));
    }

    private static void handlePoisonResistance(LivingEntity tame, TameData data) {
        int level = attributeLevel(data, "poison_resistance");
        if (level <= 0) return;
        if (tame.hasEffect(MobEffects.POISON)) {
            tame.removeEffect(MobEffects.POISON);
            debugAbilityUse(tame, "poison_resistance");
        }
    }

    private static void handleWallClimber(LivingEntity tame, TameData data) {
        int level = attributeLevel(data, "wall_climber");
        if (level <= 0) return;
        if (!tame.horizontalCollision || tame.onGround() || tame.isInWaterOrBubble() || tame.isPassenger()) return;

        Vec3 motion = tame.getDeltaMovement();
        boolean pushingIntoWall = motion.horizontalDistanceSqr() > 1.0E-4D || TameEntityAdapter.target(tame) != null
                || (tame instanceof Mob mob && !mob.getNavigation().isDone());
        if (!pushingIntoWall) return;

        double climbSpeed = Math.min(0.32D, 0.20D + Math.max(0, level - 1) * 0.03D);
        if (motion.y < climbSpeed) {
            tame.setDeltaMovement(motion.x, climbSpeed, motion.z);
            tame.fallDistance = 0.0F;
        }
    }

    private static void handleDefensiveAttributeMitigation(LivingEntity tame, TameData data, LivingHurtEvent event) {
        float amount = event.getAmount();
        if (amount <= 0.0F) return;

        int feather = attributeLevel(data, "feather_falling");
        if (feather > 0 && event.getSource().is(DamageTypeTags.IS_FALL)) {
            float reduce = (float) Math.min(0.70D, 0.20D + Math.max(0, feather - 1) * 0.125D);
            amount *= (1.0F - reduce);
            event.setAmount(amount);
            debugAbilityUse(tame, "feather_falling");
        }

        int explosion = attributeLevel(data, "explosion_resistance");
        if (explosion > 0 && event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            float reduce = (float) Math.min(0.55D, 0.15D + Math.max(0, explosion - 1) * 0.10D);
            amount *= (1.0F - reduce);
            event.setAmount(amount);
            debugAbilityUse(tame, "explosion_resistance");
        }
    }

    private static void handleRetaliationSlow(LivingEntity tame, TameData data, LivingHurtEvent event) {
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
        setAbilityCooldown(tame, data, "retaliation_slow", "retaliation_slow_tick", now, 60L);
    }


    private static void stealPositiveEffect(LivingEntity from, LivingEntity to) {
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

    private static void transferNegativeEffects(LivingEntity from, LivingEntity to, int level) {
        for (MobEffectInstance fx : from.getActiveEffects()) {
            if (fx.getEffect().getCategory() != MobEffectCategory.HARMFUL) continue;
            int duration = Math.max(20, Math.round(fx.getDuration() * attributeLevelMultiplier(level)));
            int amp = Math.max(0, fx.getAmplifier());
            to.addEffect(new MobEffectInstance(fx.getEffect(), duration, amp));
        }
    }

    private static void applyMagneticPull(LivingEntity tame, LivingEntity target, int level) {
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

    private static double magneticResistanceMultiplier(LivingEntity target) {
        double knockbackResistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        return Math.max(0.15D, 1.0D - knockbackResistance);
    }

    private static void applyChainLightning(ServerLevel level, LivingEntity tame, TameData data, LivingEntity firstTarget, float hitDamage, int levelValue) {
        if (firstTarget == null || !firstTarget.isAlive()) {
            return;
        }
        int maxChains = 1 + levelValue;
        double chainRadius = 4.0D + Math.max(0, levelValue - 1) * 0.25D;
        float chainDamage = aoeDamage(Math.max(1.0F, hitDamage * 0.33F));
        spawnChainLightningVisual(level, tame, firstTarget, maxChains, chainDamage);

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
            level.playSound(null, firstTarget.blockPosition(), DISoundRegistry.CHAIN_LIGHTNING.get(), SoundSource.HOSTILE, 0.9F, 1.0F);
            debugAbilityUse(tame, "chain_lightning");
        }
    }

    private static void spawnChainLightningVisual(ServerLevel level, LivingEntity tame, LivingEntity firstTarget, int maxChains, float chainDamage) {
        if (level == null || tame == null || firstTarget == null || !firstTarget.isAlive()) {
            return;
        }
        ChainLightningEntity lightning = DIEntityRegistry.CHAIN_LIGHTNING.get().create(level);
        if (lightning == null) {
            return;
        }
        lightning.setCreatorEntityID(tame.getId());
        lightning.setFromEntityID(tame.getId());
        lightning.setToEntityID(firstTarget.getId());
        lightning.setShockDamage(chainDamage);
        lightning.setShockCurrentTarget(false);
        lightning.setChainsLeft(maxChains);
        lightning.copyPosition(firstTarget);
        level.addFreshEntity(lightning);
        level.playSound(null, firstTarget.blockPosition(), DISoundRegistry.CHAIN_LIGHTNING.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private static LivingEntity findNearestChainTarget(ServerLevel level, LivingEntity tame, LivingEntity origin, Set<Integer> excludeIds, double radius) {
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

    private static void applySweepingEdge(LivingEntity tame, TameData data, LivingEntity primaryTarget, float hitDamage, int level) {
        int hits = 0;
        double radius = 1.4D + Math.max(0, level - 1) * 0.20D;
        float scaling = (float) Math.min(0.50D, 0.20D + Math.max(0, level - 1) * 0.075D);
        float splash = aoeDamage(hitDamage * scaling * attributeDamageBonusMultiplier(data, 0.50F));
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

    public static void onKillOrAssist(LivingEntity tame, TameData data, LivingEntity dead, boolean wasKiller) {
        if (tame == null || data == null || dead == null) return;
        long now = tame.level().getGameTime();

        if (!TameEntityAdapter.isStayingStill(tame) && LevelSystem.hasAbility(data, "bloodlust") && wasKiller) {
            int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "bloodlust"));
            MobEffectInstance activeStrength = tame.getEffect(MobEffects.DAMAGE_BOOST);
            int baseAmplifier = Math.max(0, levelValue / 5);
            int nextAmplifier = activeStrength == null ? baseAmplifier : Math.max(baseAmplifier, activeStrength.getAmplifier() + 1);
            int duration = 200 + levelValue * 20;
            tame.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, nextAmplifier));
            grantSupportUtilityXp(tame, data, tame, now, 1, 2.0F + levelValue, 0.5F);
            applySupportActivationVisual(tame, "bloodlust");
            if (tame.level() instanceof ServerLevel level) {
                DustParticleOptions red = new DustParticleOptions(new Vector3f(1.0F, 0.1F, 0.1F), 1.2F);
                level.sendParticles(red, tame.getX(), tame.getY(0.7D), tame.getZ(), capParticles(tame, 24), 0.35D, 0.45D, 0.35D, 0.02D);
            }
            debugAbilityUse(tame, "bloodlust");
        }

        int explodeLevel = attributeLevel(data, "killexploder");
        if (explodeLevel > 0 && tame.level() instanceof ServerLevel level) {
            float damage = aoeDamage(attributeDamageFromLevelOneBase((float) (4.0D + Math.max(0, explodeLevel - 1) * 1.5D), 4.0F, data, 0.50F));
            double radius = 2.0D + Math.max(0, explodeLevel - 1) * 0.40D;
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, dead.getBoundingBox().inflate(radius))) {
                if (!nearby.isAlive() || nearby == tame) continue;
                LevelSystem.trackDamage(nearby, tame);
                nearby.hurt(tame.damageSources().mobAttack(tame), damage);
            }
            level.sendParticles(ParticleTypes.EXPLOSION, dead.getX(), dead.getY(0.5D), dead.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, dead.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.NEUTRAL, 0.8F, 1.1F);
            debugAbilityUse(tame, "killexploder");
        }

        int victimSiphonLevel = attributeLevel(data, "victim_siphon");
        if (victimSiphonLevel > 0 && wasKiller) {
            float heal = (float) (dead.getMaxHealth() * Math.min(0.35D, 0.04D + 0.04D * victimSiphonLevel));
            if (heal > 0.0F) {
                tame.heal(heal);
                debugAbilityUse(tame, "victim_siphon");
            }
        }

        if (!TameEntityAdapter.isStayingStill(tame)
                && wasKiller
                && LevelSystem.hasAbility(data, "battlefield_medic")
                && !TameDuelManager.isEntityInDuel(tame.getUUID())
                && tame.level() instanceof ServerLevel level) {
            int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "battlefield_medic"));
            int procLevel = Math.min(levelValue, 8);
            double procChance = Math.min(1.0D, 0.30D + Math.max(0, procLevel - 1) * 0.10D);
            if (tame.getRandom().nextDouble() < procChance) {
                throwBattlefieldMedicBottle(level, tame, data, now, levelValue);
            }
        }

    }

    private static void throwBattlefieldMedicBottle(ServerLevel level, LivingEntity tame, TameData data, long now, int levelValue) {
        int effectiveLevel = Math.min(levelValue, 4);
        ItemStack potion = new ItemStack(Items.SPLASH_POTION);
        PotionUtils.setPotion(potion, effectiveLevel >= 4 ? Potions.STRONG_HEALING : Potions.HEALING);

        ThrownPotion thrownPotion = new TimedTameThrownPotion(level, tame);
        thrownPotion.setItem(potion);
        thrownPotion.setPos(tame.getX(), tame.getEyeY() - 0.1D, tame.getZ());
        thrownPotion.setDeltaMovement(0.0D, 0.65D + (effectiveLevel * 0.03D), 0.0D);
        TameProjectileTimeoutEvents.track(thrownPotion, SHORT_PROJECTILE_TICKS);
        level.addFreshEntity(thrownPotion);

        level.playSound(null, tame.blockPosition(), SoundEvents.SPLASH_POTION_THROW, SoundSource.NEUTRAL, 0.7F, 1.0F);
        grantSupportXp(tame, data, tame, now, effectiveLevel >= 4 ? 8.0F : 4.0F, 0.5F);
        applySupportActivationVisual(tame, "battlefield_medic");
        debugAbilityUse(tame, "battlefield_medic");
    }

    private static void handleOwnerSupportResponses(ServerPlayer owner, LivingHurtEvent event) {
        if (!(owner.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Map<SupportCacheKey, List<LivingEntity>> supportCache = new HashMap<>();
        for (LivingEntity supporter : collectOwnedNearbySupportTames(level, owner, owner.getUUID(), 10.0D, supportCache)) {
            TameData data = TameRegistry.get(supporter.getUUID());
            if (data == null) continue;
            if (handleLifeGiftSupport(level, supporter, data, owner, event, now)) break;
            if (handleGuardianInterceptSupport(level, supporter, data, owner, event, now)) break;
            if (handleBodyBlockSupport(level, supporter, data, owner, event, now)) break;
            handleEmergencyShieldSupport(level, supporter, data, owner, event, now);
            handlePackGuardSupport(level, supporter, data, owner, event, now);
        }
    }

    private static void handleAllyTameSupportResponses(LivingEntity victim, TameData victimData, LivingHurtEvent event) {
        if (!(victim.level() instanceof ServerLevel level)) return;
        UUID ownerId = TameEntityAdapter.ownerUuid(victim);
        if (ownerId == null) return;
        long now = level.getGameTime();
        Map<SupportCacheKey, List<LivingEntity>> supportCache = new HashMap<>();
        for (LivingEntity supporter : collectOwnedNearbySupportTames(level, victim, ownerId, 10.0D, supportCache)) {
            if (supporter == victim) continue;
            TameData data = TameRegistry.get(supporter.getUUID());
            if (data == null) continue;
            if (handleLifeGiftSupport(level, supporter, data, victim, event, now)) break;
            if (handleGuardianInterceptSupport(level, supporter, data, victim, event, now)) break;
            if (handleBodyBlockSupport(level, supporter, data, victim, event, now)) break;
            handleEmergencyShieldSupport(level, supporter, data, victim, event, now);
            handlePackGuardSupport(level, supporter, data, victim, event, now);
        }
    }

    private static boolean handleGuardianInterceptSupport(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity ally, LivingHurtEvent event, long now) {
        if (!LevelSystem.hasAbility(data, "guardian_intercept")) return false;
        if (TameableUtils.getImmuneTime(supporter) > 0) return false;
        if (TameEntityAdapter.isStayingStill(supporter)) return false;
        if (ally == supporter || event.getAmount() <= 0.0F) return false;
        if (!isReady(data, "guardian_intercept_tick", now)) return false;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "guardian_intercept"));
        float redirected = event.getAmount() * (float) Math.min(0.60D, 0.20D + 0.10D * levelValue);
        if (redirected <= 0.0F) return false;
        event.setAmount(Math.max(0.0F, event.getAmount() - redirected));
        applyRedirectDamage(supporter, event, redirected);
        setAbilityCooldown(supporter, data, "guardian_intercept", "guardian_intercept_tick", now, Math.max(40L, 410L - levelValue * 10L));
        grantSupportXp(supporter, data, ally, now, redirected, 0.75F);
        applySupportActivationVisual(supporter, "guardian_intercept");
        applyProtectedTargetIFrameVisual(ally);
        level.sendParticles(ParticleTypes.CRIT, ally.getX(), ally.getY(0.6D), ally.getZ(), capParticles(supporter, 4 + levelValue), 0.25D, 0.25D, 0.25D, 0.02D);
        debugAbilityUse(supporter, "guardian_intercept");
        return true;
    }

    private static boolean handleBodyBlockSupport(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity ally, LivingHurtEvent event, long now) {
        if (!LevelSystem.hasAbility(data, "body_block")) return false;
        if (TameableUtils.getImmuneTime(supporter) > 0) return false;
        if (TameEntityAdapter.isStayingStill(supporter)) return false;
        if (ally == supporter || event.getAmount() <= 0.0F) return false;
        if (!isProjectileDamage(event)) return false;
        if (!isReady(data, "body_block_tick", now)) return false;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "body_block"));
        float prevented = event.getAmount() * (float) Math.min(0.90D, 0.45D + 0.10D * levelValue);
        float backlash = prevented * 0.50F;
        event.setAmount(Math.max(0.0F, event.getAmount() - prevented));
        applyRedirectDamage(supporter, event, backlash);
        setAbilityCooldown(supporter, data, "body_block", "body_block_tick", now, Math.max(40L, 415L - levelValue * 15L));
        grantSupportXp(supporter, data, ally, now, prevented, 0.75F);
        applySupportActivationVisual(supporter, "body_block");
        applyProtectedTargetIFrameVisual(ally);
        level.playSound(null, supporter.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.NEUTRAL, 1.0F, 1.0F);
        debugAbilityUse(supporter, "body_block");
        return true;
    }

    private static void handleEmergencyShieldSupport(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity ally, LivingHurtEvent event, long now) {
        if (!LevelSystem.hasAbility(data, "emergency_shield")) return;
        if (TameableUtils.getImmuneTime(supporter) > 0) return;
        if (TameEntityAdapter.isStayingStill(supporter)) return;
        if (event.getAmount() <= 0.0F) return;
        if (supporter.distanceToSqr(ally) > 25.0D) return;
        if (!isReady(data, "emergency_shield_tick", now)) return;
        float postDamageHealth = ally.getHealth() - event.getAmount();
        float threshold = ally.getMaxHealth() * 0.35F;
        if (postDamageHealth > threshold) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "emergency_shield"));
        float before = event.getAmount();
        event.setAmount(event.getAmount() * (1.0F - Math.min(0.60F, 0.20F + levelValue * 0.08F)));
        ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 80 + levelValue * 20, Math.max(0, (levelValue - 1) / 2), false, false, true));
        ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40 + levelValue * 5, defensiveAuraAmplifier(levelValue), false, false, true));
        setAbilityCooldown(supporter, data, "emergency_shield", "emergency_shield_tick", now, 200L);
        grantSupportXp(supporter, data, ally, now, Math.max(0.0F, before - event.getAmount()), 0.75F);
        applySupportActivationVisual(supporter, "emergency_shield");
        applyProtectedTargetIFrameVisual(ally);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, ally.getX(), ally.getY(0.6D), ally.getZ(), capParticles(supporter, 6 + levelValue), 0.3D, 0.35D, 0.3D, 0.02D);
        debugAbilityUse(supporter, "emergency_shield");
    }

    private static void applyProtectedTargetIFrameVisual(LivingEntity target) {
        if (target == null) {
            return;
        }
        TameableUtils.setImmuneTime(target, Math.max(TameableUtils.getImmuneTime(target), 20));
    }

    private static void handlePackGuardSupport(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity ally, LivingHurtEvent event, long now) {
        if (!LevelSystem.hasAbility(data, "pack_guard")) return;
        if (TameableUtils.getImmuneTime(supporter) > 0) return;
        if (TameEntityAdapter.isStayingStill(supporter)) return;
        if (!isReady(data, "pack_guard_tick", now)) return;
        LivingEntity attacker = resolveLivingAttacker(event);
        if (!(attacker instanceof Monster monster) || !monster.isAlive()) return;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "pack_guard"));
        monster.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 + levelValue * 20, Math.max(0, levelValue >= 4 ? 1 : 0), false, true, true));
        TameEntityAdapter.setTarget(monster, supporter);
        setAbilityCooldown(supporter, data, "pack_guard", "pack_guard_tick", now, Math.max(40L, 140L - levelValue * 10L));
        grantSupportUtilityXp(supporter, data, ally, now, 1, 2.0F + Math.max(0, levelValue >= 4 ? 1 : 0), 0.5F);
        applySupportActivationVisual(supporter, "pack_guard");
        debugAbilityUse(supporter, "pack_guard");
    }

    private static boolean handleLifeGiftSupport(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity ally, LivingHurtEvent event, long now) {
        if (!LevelSystem.hasAbility(data, "life_gift")) return false;
        if (TameableUtils.getImmuneTime(supporter) > 0) return false;
        if (TameEntityAdapter.isStayingStill(supporter)) return false;
        if (!TameEntityAdapter.isTame(ally)) return false;
        if (!isReady(data, "life_gift_tick", now)) return false;
        float lethalOverflow = event.getAmount() - ally.getHealth();
        if (lethalOverflow < 0.0F) return false;

        float donorSpendCap = Math.max(0.0F, supporter.getHealth() - 5.0F);
        if (donorSpendCap <= 0.0F) return false;

        int levelValue = Math.max(1, LevelSystem.getAbilityLevel(data, "life_gift"));
        float desiredRecoveryHp = Math.min(5.0F, 2.0F + levelValue);
        float required = lethalOverflow + desiredRecoveryHp;
        float transfer = Math.min(required, donorSpendCap);
        if (transfer <= lethalOverflow) return false;

        event.setAmount(Math.max(0.0F, event.getAmount() - transfer));
        supporter.setHealth(Math.max(5.0F, supporter.getHealth() - transfer));
        setAbilityCooldown(supporter, data, "life_gift", "life_gift_tick", now, Math.max(2000L, 10000L - levelValue * 200L));
        grantSupportXp(supporter, data, ally, now, transfer, 0.75F);
        applySupportActivationVisual(supporter, "life_gift");
        level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(0.6D), ally.getZ(), capParticles(supporter, 6), 0.25D, 0.25D, 0.25D, 0.02D);
        debugAbilityUse(supporter, "life_gift");
        return true;
    }

    private static void handleRevitalizingPresence(ServerLevel level, LivingEntity supporter, TameData data, LivingEntity healed, float amount, long now, List<LivingEntity> nearbySupportTames) {
        // Revitalizing presence now boosts passive self-healing in handlePassiveHeal.
    }

    private static int attributeLevel(TameData data, String id) {
        return Math.max(0, LevelSystem.getAttributeLevel(data, id));
    }

    private static double chanceByLevel(int level, double perLevel, double cap) {
        return Math.min(cap, level * perLevel);
    }

    private static float attributeLevelMultiplier(int level) {
        return offensiveAbilityLevelMultiplier(level);
    }

    private static float attributeConditionalMultiplier(int level) {
        return 0.20F + 0.10F * Math.max(1, level);
    }

    private static float attributeDamageBonusMultiplier(TameData data, float scaling) {
        return 1.0F + offensiveDamageBonusBonusFromLevelOne(data, scaling);
    }

    private static float attributeDamageFromLevelOneBase(float scaledDamage, float levelOneBaseDamage, TameData data, float scaling) {
        return scaledDamage + levelOneBaseDamage * offensiveDamageBonusBonusFromLevelOne(data, scaling);
    }

    private static int breakpointAmplifier(int level) {
        if (level >= 5) {
            return 2;
        }
        if (level >= 3) {
            return 1;
        }
        return 0;
    }

    private static boolean binaryAttribute(String attribute) {
        return "fire_resistance".equals(attribute) || "poison_resistance".equals(attribute);
    }

    private static boolean isBaseAttackHit(LivingEntity tame, LivingHurtEvent event) {
        if (tame == null || event == null || event.getSource() == null) {
            return false;
        }
        if (isMutantCreeperMinionExplosion(tame, event)) {
            return true;
        }
        if (INTERNAL_BONUS_DAMAGE.get()) {
            return false;
        }
        return (event.getSource().getEntity() == tame && event.getSource().getDirectEntity() == tame)
                || isAlexsMobsProjectileBaseAttack(tame, event);
    }

    private static boolean isAlexsMobsProjectileBaseAttack(LivingEntity tame, LivingHurtEvent event) {
        if (tame == null || event == null || event.getSource() == null) {
            return false;
        }
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        if (sourceEntity != tame || directEntity == null || directEntity == tame) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(directEntity.getType());
        return key != null && "alexsmobs".equals(key.getNamespace());
    }

    private static boolean isMutantCreeperMinionExplosion(LivingEntity tame, LivingHurtEvent event) {
        return tame != null
                && event != null
                && event.getSource() != null
                && event.getSource().getEntity() == tame
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION)
                && isMutantCreeperMinion(tame);
    }

    private static boolean isMutantCreeperMinion(LivingEntity tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(tame.getType());
        return key != null
                && "mutantmonsters".equals(key.getNamespace())
                && "creeper_minion".equals(key.getPath());
    }

    private static float lightningfangChance(int level) {
        return (float) Math.min(0.35D, 0.10D + 0.05D * Math.max(1, level));
    }

    private static float lightningfangDamage(TameData data, int level) {
        int safeLevel = Math.max(1, level);
        return attributeDamageFromLevelOneBase(2.0F + 2.0F * safeLevel, 4.0F, data, 1.0F);
    }

    private static float abilityPowerBaseDamageBonus(TameData data) {
        int level = attributeLevel(data, "ability_power");
        return (float) level * 0.15F;
    }

    public static float offensiveAbilityCastDamage(TameData data, String abilityId, int abilityLevel) {
        int level = Math.max(1, abilityLevel);
        float baseDps = offensiveAbilityBaseDps(abilityId);
        float cooldownSeconds = offensiveAbilityBudgetCooldownTicks(abilityId, level) / 20.0F;
        float damageBonusScaling = offensiveAbilityDamageBonusScaling(abilityId);
        float levelOneBaseDamage = cooldownSeconds * baseDps;
        float castDamage = levelOneBaseDamage
                * (offensiveAbilityLevelMultiplier(level)
                + offensiveDamageBonusBonusFromLevelOne(data, damageBonusScaling)
                + abilityPowerBaseDamageBonus(data));
        return offensiveAbilityUsesAoeScaling(abilityId) ? aoeDamage(castDamage) : singleTargetDamage(castDamage);
    }

    public static long offensiveAbilityBudgetCooldownTicks(String abilityId, int abilityLevel) {
        return switch (abilityId) {
            case "arrow_shot" -> 60L;
            case "creeper_explosion" -> 200L;
            case "ghast_fireball" -> 100L;
            case "wither_skull" -> 80L;
            case "blaze_attack" -> 40L;
            case "guardian_beam" -> 70L;
            case "elder_guardian_beam" -> 120L;
            case "trident" -> 90L;
            case "crossbow" -> 80L;
            case "fishing" -> 90L;
            case "dash" -> 90L;
            case "flash" -> FLASH_BASE_COOLDOWN_TICKS;
            case "evoker_fangs" -> 100L;
            case "dragon_fireball" -> 140L;
            case "llama_spit" -> 50L;
            case "shulker_bullet" -> 200L;
            case "snowball_shot" -> 20L;
            case "lightning_strike" -> 500L;
            case "warden_scream" -> 120L;
            case "sky_launch" -> 70L;
            default -> 0L;
        };
    }

    private static float offensiveAbilityBaseDps(String abilityId) {
        return switch (abilityId) {
            case "trident" -> 1.15F;
            case "arrow_shot" -> 1.00F;
            case "crossbow", "llama_spit" -> 0.95F;
            case "blaze_attack" -> 0.90F;
            case "elder_guardian_beam", "shulker_bullet" -> 0.85F;
            case "lightning_strike", "shadow_hands" -> 0.80F;
            case "wither_skull" -> 0.75F;
            case "guardian_beam", "snowball_shot" -> 0.70F;
            case "fishing", "dash", "flash" -> 0.65F;
            case "warden_scream" -> 0.60F;
            case "ghast_fireball" -> 0.55F;
            case "evoker_fangs", "dragon_fireball" -> 0.50F;
            case "creeper_explosion" -> 0.45F;
            case "sky_launch" -> 0.65F;
            default -> 0.0F;
        };
    }

    private static float offensiveAbilityDamageBonusScaling(String abilityId) {
        return switch (abilityId) {
            case "arrow_shot", "trident", "crossbow", "llama_spit", "blaze_attack", "snowball_shot" -> 1.00F;
            case "elder_guardian_beam", "guardian_beam", "lightning_strike", "shulker_bullet" -> 0.85F;
            case "fishing", "dash", "flash", "wither_skull", "sky_launch" -> 0.75F;
            case "creeper_explosion", "ghast_fireball", "evoker_fangs", "dragon_fireball", "warden_scream" -> 0.50F;
            default -> 1.00F;
        };
    }

    private static boolean offensiveAbilityUsesAoeScaling(String abilityId) {
        return switch (abilityId) {
            case "creeper_explosion", "ghast_fireball", "wither_skull", "evoker_fangs", "dragon_fireball", "warden_scream" -> true;
            default -> false;
        };
    }

    public static float offensiveAbilityLevelMultiplier(int abilityLevel) {
        int level = Math.max(1, abilityLevel);
        return (float) (1.0D + Math.max(0, level - 1) * 0.20D);
    }

    public static float offensiveDamageBonusBonusFromLevelOne(TameData data, float scaling) {
        double bonusDamage = data == null ? 0.0D : Math.max(0.0D, data.bonusDamage);
        return (float) (bonusDamage * 0.05D * scaling);
    }

    private static void applyExternalProjectileBonusDamage(TameData data, LivingHurtEvent event) {
        if (data == null || event == null || event.getSource() == null) {
            return;
        }
        Entity direct = event.getSource().getDirectEntity();
        if (!isAlexsMobsPollenBall(direct)) {
            return;
        }
        float bonus = (float) Math.max(0.0D, data.bonusDamage);
        if (bonus <= 0.0F) {
            return;
        }
        event.setAmount(event.getAmount() + bonus);
        noteDamageContributor("bonus_damage");
    }

    private static float tameBaseDamage(LivingEntity tame) {
        if (tame == null || tame.getAttribute(Attributes.ATTACK_DAMAGE) == null) {
            return 2.0F;
        }
        return (float) tame.getAttributeValue(Attributes.ATTACK_DAMAGE);
    }

    private static float applyArmorPierce(float rawDamage, LivingEntity target, int level) {
        if (rawDamage <= 0.0F || target == null || level <= 0) {
            return rawDamage;
        }
        float armor = target.getArmorValue();
        if (armor <= 0.0F) {
            return rawDamage;
        }
        float toughness = target.getAttribute(Attributes.ARMOR_TOUGHNESS) == null
                ? 0.0F
                : (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float pierceFraction = (float) Math.min(1.0D, 0.50D + 0.125D * Math.max(0, level - 1));
        float effectiveArmor = armor * (1.0F - pierceFraction);
        float desiredFinal = CombatRules.getDamageAfterAbsorb(rawDamage, effectiveArmor, toughness);
        return solvePreArmorDamageForFinal(desiredFinal, armor, toughness);
    }

    private static float solvePreArmorDamageForFinal(float desiredFinal, float armor, float toughness) {
        if (desiredFinal <= 0.0F) {
            return 0.0F;
        }
        float low = desiredFinal;
        float high = Math.max(desiredFinal, desiredFinal * 4.0F + armor * 2.0F + toughness);
        while (CombatRules.getDamageAfterAbsorb(high, armor, toughness) < desiredFinal && high < 1000000.0F) {
            high *= 2.0F;
        }
        for (int i = 0; i < 20; i++) {
            float mid = (low + high) * 0.5F;
            float mitigated = CombatRules.getDamageAfterAbsorb(mid, armor, toughness);
            if (mitigated < desiredFinal) {
                low = mid;
            } else {
                high = mid;
            }
        }
        return high;
    }

    private static void setAbilityCooldown(LivingEntity tame, TameData data, String sourceId, String key, long now, long baseTicks) {
        long ticks = AbilityCooldowns.scaledCooldownTicks(data, sourceId, baseTicks);

        int emergency = attributeLevel(data, "emergency_cooldown_reduction");
        if (emergency > 0) {
            float hp = tame.getHealth() / Math.max(1.0F, tame.getMaxHealth());
            double threshold = 0.25D + Math.max(0, emergency - 1) * 0.025D;
            if (hp <= threshold) {
                double chance = Math.min(0.38D, 0.08D + 0.06D * emergency);
                if (tame.getRandom().nextDouble() < chance) {
                    ticks = 20L;
                    debugAbilityUse(tame, "emergency_cooldown_reduction");
                }
            }
        }
        setCooldown(data, key, now + ticks);
    }

    private static void spawnLightningVisual(ServerLevel level, double x, double y, double z, LivingEntity tame) {
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) return;
        lightning.moveTo(x, y, z);
        lightning.setVisualOnly(true);
        if (TameEntityAdapter.owner(tame) instanceof ServerPlayer player) {
            lightning.setCause(player);
        }
        level.addFreshEntity(lightning);
    }

    private static void debugAbilityUse(LivingEntity tame, String ability) {
        if (!(tame.level() instanceof ServerLevel level)) return;
        if (TameEntityAdapter.ownerUuid(tame) == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(TameEntityAdapter.ownerUuid(tame));
        if (owner == null) return;
        boolean attribute = LevelSystem.knownAttributeIds().contains(ability);
        if (attribute && !PlayerDebugSettings.attributeUsed(owner.getUUID())) return;
        if (!attribute && !PlayerDebugSettings.abilityUsed(owner.getUUID())) return;
        String tameName = tame.hasCustomName() && tame.getCustomName() != null ? tame.getCustomName().getString() : tame.getName().getString();
        owner.sendSystemMessage(net.minecraft.network.chat.Component.literal(tameName + ": " + ability));
    }

    private static void debugDamage(LivingEntity tame, TameData data, LivingHurtEvent event, float before, float after) {
        if (!(tame.level() instanceof ServerLevel level)) return;
        if (TameEntityAdapter.ownerUuid(tame) == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(TameEntityAdapter.ownerUuid(tame));
        if (owner == null || !PlayerDebugSettings.damage(owner.getUUID())) return;
        String tameName = data != null && data.name != null && !data.name.isBlank()
                ? data.name
                : (tame.hasCustomName() && tame.getCustomName() != null ? tame.getCustomName().getString() : tame.getName().getString());
        String targetName = event.getEntity().getName().getString();
        String sourceName = event.getSource().getDirectEntity() == null
                ? event.getSource().type().msgId()
                : event.getSource().getDirectEntity().getType().toShortString();
        List<String> contributors = new ArrayList<>(DAMAGE_DEBUG_CONTRIBUTORS.get());
        owner.sendSystemMessage(
                Component.literal("[DMG] ").withStyle(ChatFormatting.RED)
                        .append(Component.literal(tameName).withStyle(ChatFormatting.GOLD))
                        .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(targetName).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" [" + sourceName + "] ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(String.format(java.util.Locale.ROOT, "%.2f", before)).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" -> ").withStyle(ChatFormatting.DARK_GRAY))
                        .append(Component.literal(String.format(java.util.Locale.ROOT, "%.2f", after)).withStyle(ChatFormatting.GREEN))
                        .append(contributors.isEmpty()
                                ? Component.empty()
                                : Component.literal(" {" + String.join(", ", contributors) + "}").withStyle(ChatFormatting.LIGHT_PURPLE))
        );
    }

    private static void noteDamageContributor(String id) {
        if (id == null || id.isBlank()) {
            return;
        }
        List<String> contributors = DAMAGE_DEBUG_CONTRIBUTORS.get();
        if (!contributors.contains(id)) {
            contributors.add(id);
        }
    }

    private static LivingEntity resolveTameAttacker(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity tame) {
            return tame;
        }
        if (event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() != null) {
            return ownable.getOwner();
        }
        return null;
    }

    private static LivingEntity resolveLivingAttacker(LivingHurtEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        if (event.getSource().getEntity() instanceof LivingEntity living) {
            return living;
        }
        if (event.getSource().getDirectEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static boolean isProjectileDamage(LivingHurtEvent event) {
        if (event == null || event.getSource() == null) {
            return false;
        }
        Entity direct = event.getSource().getDirectEntity();
        return direct instanceof net.minecraft.world.entity.projectile.Projectile;
    }

    private static void applyRedirectDamage(LivingEntity supporter, LivingHurtEvent originalEvent, float amount) {
        if (supporter == null || amount <= 0.0F) return;
        INTERNAL_SUPPORT_REDIRECT.set(true);
        try {
            supporter.hurt(originalEvent.getSource(), amount);
        } finally {
            INTERNAL_SUPPORT_REDIRECT.set(false);
        }
    }

    private static List<LivingEntity> collectOwnedNearbySupportTames(ServerLevel level, Entity center, UUID ownerId, double radius) {
        return collectOwnedNearbySupportTames(level, center, ownerId, radius, null);
    }

    private static List<LivingEntity> collectOwnedNearbySupportTames(ServerLevel level, Entity center, UUID ownerId, double radius, Map<SupportCacheKey, List<LivingEntity>> cache) {
        if (level == null || center == null || ownerId == null) {
            return List.of();
        }
        UUID centerId = center.getUUID();
        if (centerId != null && TameDuelManager.isEntityInDuel(centerId)) {
            return List.of();
        }
        SupportCacheKey key = null;
        if (cache != null && centerId != null) {
            key = new SupportCacheKey(centerId, ownerId, (int) Math.round(radius * 1000.0D));
            List<LivingEntity> cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
        }
        AABB box = center.getBoundingBox().inflate(radius);
        List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box, tame ->
                TameEntityAdapter.isTame(tame)
                        && tame.isAlive()
                        && ownerId.equals(TameEntityAdapter.ownerUuid(tame))
                        && TameDuelManager.canProvideSupport(tame.getUUID(), centerId)
        );
        if (cache != null && key != null) {
            cache.put(key, found);
        }
        return found;
    }

    private static LivingEntity findLowestHealthAlly(ServerLevel level, LivingEntity supporter, double radius, boolean includeOwner) {
        return findLowestHealthAlly(level, supporter, radius, includeOwner, collectOwnedNearbySupportTames(level, supporter, supporter == null ? null : TameEntityAdapter.ownerUuid(supporter), radius));
    }

    private static LivingEntity findLowestHealthAlly(ServerLevel level, LivingEntity supporter, double radius, boolean includeOwner, List<LivingEntity> nearbySupportTames) {
        if (level == null || supporter == null || TameEntityAdapter.ownerUuid(supporter) == null) return null;
        LivingEntity best = null;
        float bestRatio = 1.01F;

        LivingEntity owner = TameEntityAdapter.owner(supporter);
        if (includeOwner && owner != null && owner.isAlive() && owner.distanceToSqr(supporter) <= radius * radius
                && TameDuelManager.canProvideSupport(supporter.getUUID(), owner.getUUID())) {
            float ratio = owner.getHealth() / Math.max(1.0F, owner.getMaxHealth());
            if (ratio < bestRatio && owner.getHealth() < owner.getMaxHealth()) {
                bestRatio = ratio;
                best = owner;
            }
        }

        for (LivingEntity ally : nearbySupportTames) {
            if (!TameDuelManager.canProvideSupport(supporter.getUUID(), ally.getUUID())) continue;
            float ratio = ally.getHealth() / Math.max(1.0F, ally.getMaxHealth());
            if (ratio < bestRatio && ally.getHealth() < ally.getMaxHealth()) {
                bestRatio = ratio;
                best = ally;
            }
        }
        return best;
    }

    private static LivingEntity findFirstDebuffedAlly(ServerLevel level, LivingEntity supporter, double radius) {
        return findFirstDebuffedAlly(level, supporter, radius, collectOwnedNearbySupportTames(level, supporter, supporter == null ? null : TameEntityAdapter.ownerUuid(supporter), radius));
    }

    private static LivingEntity findFirstDebuffedAlly(ServerLevel level, LivingEntity supporter, double radius, List<LivingEntity> nearbySupportTames) {
        if (level == null || supporter == null || TameEntityAdapter.ownerUuid(supporter) == null) return null;
        LivingEntity owner = TameEntityAdapter.owner(supporter);
        if (owner != null && owner.isAlive() && owner.distanceToSqr(supporter) <= radius * radius
                && TameDuelManager.canProvideSupport(supporter.getUUID(), owner.getUUID())
                && firstHarmfulEffect(owner) != null) {
            return owner;
        }
        for (LivingEntity ally : nearbySupportTames) {
            if (!TameDuelManager.canProvideSupport(supporter.getUUID(), ally.getUUID())) continue;
            if (firstHarmfulEffect(ally) != null) {
                return ally;
            }
        }
        return null;
    }

    private static MobEffectInstance firstHarmfulEffect(LivingEntity target) {
        if (target == null) {
            return null;
        }
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect != null && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                return effect;
            }
        }
        return null;
    }

    private static boolean isInBattle(LivingEntity tame) {
        if (tame == null) {
            return false;
        }
        if (TameEntityAdapter.target(tame) != null && TameEntityAdapter.target(tame).isAlive()) {
            return true;
        }
        if (tame.hurtTime > 0) {
            return true;
        }
        LivingEntity attacker = tame.getLastHurtByMob();
        return attacker != null && attacker.isAlive() && tame.distanceToSqr(attacker) < 20.0D * 20.0D;
    }

    private static int healNearbyAllies(ServerLevel level, LivingEntity source, double radius, float amount, boolean includeOwner) {
        if (level == null || source == null || TameEntityAdapter.ownerUuid(source) == null || amount <= 0.0F) {
            return 0;
        }
        int healed = 0;
        LivingEntity owner = TameEntityAdapter.owner(source);
        if (includeOwner && owner != null && owner.isAlive() && owner.distanceToSqr(source) <= radius * radius
                && owner.getHealth() < owner.getMaxHealth()
                && TameDuelManager.canProvideSupport(source.getUUID(), owner.getUUID())) {
            owner.heal(amount);
            healed++;
        }
        for (LivingEntity ally : collectOwnedNearbySupportTames(level, source, TameEntityAdapter.ownerUuid(source), radius)) {
            if (!TameDuelManager.canProvideSupport(source.getUUID(), ally.getUUID())) continue;
            if (ally.getHealth() >= ally.getMaxHealth()) continue;
            ally.heal(amount);
            healed++;
        }
        return healed;
    }

    private static void grantSupportXp(LivingEntity supporter, TameData data, LivingEntity beneficiary, long now, float effectiveAmount, float scale) {
        if (supporter == null || data == null || effectiveAmount <= 0.0F) {
            return;
        }
        if (!isSupportCombatRelevant(supporter, beneficiary)) {
            return;
        }
        if (!isReady(data, "support_xp_tick", now)) {
            return;
        }
        int xp = Math.max(1, Mth.ceil(effectiveAmount * Math.max(0.0F, scale)));
        LevelSystem.grantXP(supporter, data, xp);
        setCooldown(data, "support_xp_tick", now + 20L);
    }

    private static void grantSupportUtilityXp(LivingEntity supporter, TameData data, LivingEntity beneficiary, long now, int affectedCount, float perTargetAmount, float scale) {
        if (affectedCount <= 0 || perTargetAmount <= 0.0F) {
            return;
        }
        grantSupportXp(supporter, data, beneficiary, now, affectedCount * perTargetAmount, scale);
    }

    private static boolean isSupportCombatRelevant(LivingEntity supporter, LivingEntity beneficiary) {
        if (supporter == null) {
            return false;
        }
        if (isInBattle(supporter)) {
            return true;
        }
        return isInBattle(beneficiary);
    }

    private static boolean needsNearbyAllySupportScan(TameData data) {
        return data != null && (LevelSystem.hasAbility(data, "triage_pulse") || LevelSystem.hasAbility(data, "cleanse_touch"));
    }

    private static boolean tameHasNearbyReactiveSupport(TameData data) {
        return data != null && (
                LevelSystem.hasAbility(data, "life_gift")
                        || LevelSystem.hasAbility(data, "guardian_intercept")
                        || LevelSystem.hasAbility(data, "body_block")
                        || LevelSystem.hasAbility(data, "emergency_shield")
                        || LevelSystem.hasAbility(data, "pack_guard")
        );
    }

    private static boolean ownerHasNearbyReactiveSupport(ServerPlayer owner) {
        if (owner == null) {
            return false;
        }
        for (TameData data : TameRegistry.getOwned(owner.getUUID())) {
            if (tameHasNearbyReactiveSupport(data)) {
                return true;
            }
        }
        return false;
    }

    private static boolean healEventNeedsNearbySupport(LivingEntity healed) {
        if (healed instanceof ServerPlayer owner) {
            return ownerHasNearbyReactiveHealSupport(owner);
        }
        if (TameEntityAdapter.isTame(healed) && TameEntityAdapter.ownerUuid(healed) != null) {
            return ownerHasNearbyReactiveHealSupport(TameEntityAdapter.ownerUuid(healed));
        }
        return false;
    }

    private static boolean ownerHasNearbyReactiveHealSupport(ServerPlayer owner) {
        return owner != null && ownerHasNearbyReactiveHealSupport(owner.getUUID());
    }

    private static boolean ownerHasNearbyReactiveHealSupport(UUID ownerId) {
        return false;
    }

    private static boolean isFriendly(LivingEntity tame, Entity entity) {
        if (entity == tame) return true;
        if (entity instanceof Player player) {
            return !TameDuelManager.areDuelOpponents(tame.getUUID(), player.getUUID());
        }
        if (entity instanceof LivingEntity otherTame && TameEntityAdapter.isTame(otherTame)) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), otherTame.getUUID())) {
                return false;
            }
            if (!TLAdminRuntimeSettings.friendlyFireEnabled(TameEntityAdapter.ownerUuid(tame))) {
                return true;
            }
            return true;
        }
        return false;
    }

    private static boolean isProtectedPassiveWildlife(LivingEntity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (TameEntityAdapter.isTame(entity)) {
            return false;
        }
        if (!(entity instanceof Mob mob)) {
            return false;
        }
        if (!entity.getType().getCategory().isFriendly()) {
            return false;
        }
        if (mob instanceof Enemy) {
            return false;
        }
        if (mob instanceof NeutralMob neutral && neutral.getRemainingPersistentAngerTime() > 0) {
            return false;
        }
        LivingEntity target = TameEntityAdapter.target(mob);
        return target == null || !target.isAlive();
    }

    private static boolean isReady(TameData data, String key, long now) {
        return now >= data.cooldowns.getOrDefault(key, 0L);
    }

    private static void setCooldown(TameData data, String key, long tick) {
        data.cooldowns.put(key, tick);
    }


    private static int capParticles(LivingEntity tame, int requested) {
        return Math.max(1, requested);
    }

    private static void applySupportActivationVisual(LivingEntity tame, String source) {
        if (!(tame.level() instanceof ServerLevel level) || !HEALING_SUPPORT_VISUAL_SOURCES.contains(source)) {
            return;
        }
        for (int i = 0; i < 3; ++i) {
            double d0 = tame.getRandom().nextGaussian() * 0.02D;
            double d1 = tame.getRandom().nextGaussian() * 0.02D;
            double d2 = tame.getRandom().nextGaussian() * 0.02D;
            level.sendParticles(ParticleTypes.HEART, tame.getRandomX(1.0D), tame.getY(0.5D), tame.getRandomZ(1.0D), 3, d0, d1, d2, 0.02F);
        }
    }

    private static void applyInternalBonusDamage(LivingEntity target, LivingEntity attacker, float amount) {
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
        if (applyTaggedProjectileDamage(direct, event)) {
            return;
        }

        if (direct instanceof NoGriefLargeFireball || direct instanceof WitherSkull || direct instanceof EvokerFangs) {
            if (direct instanceof NoGriefLargeFireball) noteDamageContributor("ghast_fireball");
            if (direct instanceof WitherSkull) noteDamageContributor("wither_skull");
            if (direct instanceof EvokerFangs) noteDamageContributor("evoker_fangs");
            LivingEntity tame = resolveProjectileOwner(direct);
            TameData data = tame == null ? null : TameRegistry.get(tame.getUUID());
            int level = data == null ? 1 : Math.max(1, LevelSystem.getAbilityLevel(data,
                    direct instanceof NoGriefLargeFireball ? "ghast_fireball"
                            : direct instanceof WitherSkull ? "wither_skull"
                            : "evoker_fangs"));
            String abilityId = direct instanceof NoGriefLargeFireball ? "ghast_fireball"
                    : direct instanceof WitherSkull ? "wither_skull"
                    : "evoker_fangs";
            if (data != null) {
                event.setAmount(offensiveAbilityCastDamage(data, abilityId, level));
            } else {
                event.setAmount(aoeDamage(event.getAmount()));
            }
            return;
        }
        if (direct instanceof ShulkerBullet) {
            noteDamageContributor("shulker_bullet");
            LivingEntity tame = resolveProjectileOwner(direct);
            TameData data = tame == null ? null : TameRegistry.get(tame.getUUID());
            int level = data == null ? 1 : Math.max(1, LevelSystem.getAbilityLevel(data, "shulker_bullet"));
            if (data != null) {
                event.setAmount(offensiveAbilityCastDamage(data, "shulker_bullet", level));
            } else {
                event.setAmount(singleTargetDamage(event.getAmount()));
            }
        }
    }

    private static LivingEntity resolveProjectileOwner(Entity direct) {
        if (direct == null) {
            return null;
        }
        Entity owner = null;
        if (direct instanceof net.minecraft.world.entity.projectile.Projectile projectile) {
            owner = TameEntityAdapter.owner(projectile);
        }
        return owner instanceof LivingEntity tame && TameEntityAdapter.isTame(tame) ? tame : null;
    }

    private static void setProjectileDamage(Entity projectile, String abilityId, float damage) {
        if (projectile == null) {
            return;
        }
        projectile.getPersistentData().putFloat(PROJECTILE_DAMAGE_TAG, damage);
        projectile.getPersistentData().putString(PROJECTILE_SOURCE_TAG, abilityId == null ? "" : abilityId);
    }

    private static boolean applyTaggedProjectileDamage(Entity direct, LivingHurtEvent event) {
        if (direct == null || event == null) {
            return false;
        }
        if (!direct.getPersistentData().contains(PROJECTILE_DAMAGE_TAG)) {
            return false;
        }
        float damage = direct.getPersistentData().getFloat(PROJECTILE_DAMAGE_TAG);
        if (damage <= 0.0F) {
            return false;
        }
        String source = direct.getPersistentData().getString(PROJECTILE_SOURCE_TAG);
        noteDamageContributor(source);
        LivingEntity tame = resolveProjectileOwner(direct);
        LivingEntity living = event.getEntity();
        if (tame != null) {
            LevelSystem.trackDamage(living, tame);
        }
        event.setAmount(damage);
        return true;
    }

    private static boolean isAlexsMobsPollenBall(Entity entity) {
        if (entity == null) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(entity.getType());
        return key != null && "alexsmobs".equals(key.getNamespace()) && "pollen_ball".equals(key.getPath());
    }

    private static float singleTargetDamage(float amount) {
    return amount * TLAdminRuntimeSettings.singleTargetAbilityDamageMultiplier();
}

private static float aoeDamage(float amount) {
    return amount * TLAdminRuntimeSettings.aoeAbilityDamageMultiplier();
}

private static void applyWardenScreamPush(LivingEntity tame, LivingEntity target, int levelValue) {
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

    private static LivingEntity resolveLoadedRegistryTame(ServerLevel level, ResourceLocation levelId, TameData data) {
        if (data == null || data.uuid == null) return null;
        if (data.lastKnownDimension != null && !data.lastKnownDimension.isBlank()) {
            ResourceLocation lastKnown = ResourceLocation.tryParse(data.lastKnownDimension);
            if (lastKnown != null && !lastKnown.equals(levelId)) {
                return null;
            }
        }
        Entity entity = level.getEntity(data.uuid);
        if (!(entity instanceof LivingEntity tame) || !TameEntityAdapter.isTame(tame) || !tame.isAlive()) {
            return null;
        }
        return tame;
    }

    private static boolean reviveDeadEntry(TameData data) {
        if (data == null || !data.dead) {
            return false;
        }
        data.dead = false;
        data.deadGameTime = 0L;
        data.deadUnixMillis = 0L;
        data.deathDimension = "";
        data.deathX = 0;
        data.deathY = 0;
        data.deathZ = 0;
        return true;
    }

    private static boolean shouldRunHeavyPass(LivingEntity tame, long now) {
        int buckets = Math.max(1, HEAVY_ABILITY_STAGGER_TICKS / ABILITY_TICK_RATE);
        int phase = Math.floorMod(tame.getUUID().hashCode(), buckets);
        long slice = now / ABILITY_TICK_RATE;
        return (slice % buckets) == phase;
    }

    private static boolean shouldRunPeriodicPass(LivingEntity tame, long now, int intervalTicks) {
        if (tame == null || intervalTicks <= ABILITY_TICK_RATE) {
            return true;
        }
        int buckets = Math.max(1, intervalTicks / ABILITY_TICK_RATE);
        int phase = Math.floorMod(tame.getUUID().hashCode(), buckets);
        long slice = now / ABILITY_TICK_RATE;
        return (slice % buckets) == phase;
    }

    private static boolean shouldUseOffensiveAbilities(LivingEntity tame, TameData data, LivingEntity target) {
        if (tame == null || data == null || target == null || !target.isAlive()) return false;
        if (isFriendly(tame, target)) return false;
        if (isDuelOpponent(tame, target)) {
            return true;
        }

        TameMode mode = TameMode.byId(data.mode);
        return switch (mode) {
            case PASSIVE -> false;
            // DEFAULT should still allow abilities against the tame's current valid target.
            case DEFAULT -> true;
            case MONSTER_HUNTER, BOSS -> target instanceof Enemy;
            case ARENA -> !(target instanceof Player) && !TameEntityAdapter.isTame(target);
            case BODYGUARD -> (target instanceof Enemy) || isOwnerCombatPriorityTarget(tame, target);
            case DEFAULT_PLUS, AGGRESSIVE -> true;
        };
    }

    private static boolean isOwnerCombatPriorityTarget(LivingEntity tame, LivingEntity target) {
        if (tame == null || target == null) return false;
        if (!(TameEntityAdapter.owner(tame) instanceof ServerPlayer owner)) return false;
        LivingEntity attacker = owner.getLastHurtByMob();
        if (attacker == target && attacker.isAlive()) return true;
        LivingEntity attacked = owner.getLastHurtMob();
        return attacked == target && attacked.isAlive();
    }

    private static boolean isDuelOpponent(LivingEntity tame, LivingEntity target) {
        return tame != null
                && target != null
                && TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID());
    }

}
