package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.SpawnerTriggerSupport;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.UUID;

public class TameProtectionEvents {
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        var direct = event.getSource().getDirectEntity();
        if (TameEntityAdapter.isTame(victim) && isMutantCreeperMinionSelfExplosion(victim, event)) {
            event.setCanceled(true);
            return;
        }
        UUID attackerParticipantId = resolveParticipantId(attacker, direct);
        UUID victimParticipantId = victim == null ? null : victim.getUUID();

        if (attackerParticipantId != null && TameDuelManager.isEntityInDuel(attackerParticipantId)) {
            if (victimParticipantId != null && TameDuelManager.areDuelOpponents(attackerParticipantId, victimParticipantId)) {
                return;
            }
            event.setCanceled(true);
            return;
        }

        // Block direct player attacks against tamed animals.
        if (TameEntityAdapter.isTame(victim) && attacker instanceof Player) {
            LivingEntity target = victim;
            if (!TameDuelManager.areDuelOpponents(attacker.getUUID(), target.getUUID())) {
                event.setCanceled(true);
            }
            return;
        }

        LivingEntity tameAttacker = resolveTameAttacker(attacker, direct);
        if (tameAttacker == null || !TameEntityAdapter.isTame(tameAttacker)) {
            return;
        }

        if (TameRegistry.isProtectedAttackTarget(TameEntityAdapter.ownerUuid(tameAttacker), victim)) {
            event.setCanceled(true);
            return;
        }

        if (!TLAdminRuntimeSettings.friendlyFireEnabled(TameEntityAdapter.ownerUuid(tameAttacker))) {
            if (victim instanceof Player) {
                clearForbiddenPlayerAggression(tameAttacker);
                event.setCanceled(true);
                return;
            }
            if (TameEntityAdapter.isTame(victim)) {
                event.setCanceled(true);
                return;
            }
        }

        if (TameDuelManager.isTameInDuel(tameAttacker.getUUID())) {
            if (victimParticipantId != null && TameDuelManager.areDuelOpponents(tameAttacker.getUUID(), victimParticipantId)) {
                return;
            }
            event.setCanceled(true);
            return;
        }

        // Prevent tamed entities (including projectile abilities) from damaging players or other tamed entities.
        if (victim instanceof Player) {
            event.setCanceled(true);
            return;
        }
        if (TameEntityAdapter.isTame(victim)) {
            if (TameDuelManager.areDuelOpponents(tameAttacker.getUUID(), victim.getUUID())) {
                return;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackAllowDuelPlayerPvp(LivingAttackEvent event) {
        if (event == null || !event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker == null) {
            return;
        }
        if (canPlayersBypassFriendlyFire(attacker, victim)) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onHurtAllowDuelPlayerPvp(LivingHurtEvent event) {
        if (event == null || !event.isCanceled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = resolvePlayerAttacker(event.getSource());
        if (attacker == null) {
            return;
        }
        if (canPlayersBypassFriendlyFire(attacker, victim)) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onAttackEntityAllowDuelPlayerPvp(AttackEntityEvent event) {
        if (event == null || !event.isCanceled()) {
            return;
        }
        Player attacker = event.getEntity();
        if (!(event.getTarget() instanceof Player victim)) {
            return;
        }
        if (canPlayersBypassFriendlyFire(attacker, victim)) {
            event.setCanceled(false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onMobSpawnAllowSpawnerTrigger(MobSpawnEvent.FinalizeSpawn event) {
        if (event == null || !event.isCanceled()) {
            return;
        }
        if (event.getSpawnType() != net.minecraft.world.entity.MobSpawnType.SPAWNER) {
            return;
        }
        if (event.getEntity() == null || event.getEntity().level() == null || event.getEntity().level().isClientSide()) {
            return;
        }
        if (!SpawnerTriggerSupport.hasSpawnerTriggerTameInRange(event.getEntity().level(), new net.minecraft.world.phys.Vec3(event.getX(), event.getY(), event.getZ()), 16.0D)) {
            return;
        }
        event.setSpawnCancelled(false);
        event.setCanceled(false);
    }

    private static boolean canPlayersBypassFriendlyFire(Player attacker, Player victim) {
        if (attacker == null || victim == null) {
            return false;
        }
        UUID attackerId = attacker.getUUID();
        UUID victimId = victim.getUUID();
        return TameDuelManager.areDuelOpponents(attackerId, victimId)
                || TameDuelManager.areDuelOwnersOpponents(attackerId, victimId);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity tame = event.getEntity();
        if (!TameEntityAdapter.isTame(tame)) {
            return;
        }
        if (!isMutantCreeperMinionSelfExplosion(tame, event.getSource())) {
            return;
        }
        event.setCanceled(true);
        tame.setHealth(Math.max(1.0F, Math.min(tame.getMaxHealth(), 1.0F)));
        TameEntityAdapter.setTarget(tame, null);
        tame.setLastHurtByMob(null);
        tame.setLastHurtMob(null);
        if (tame instanceof net.minecraft.world.entity.Mob mob) mob.getNavigation().stop();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity tame = event.getEntity();
        if (!TameEntityAdapter.isTame(tame)) return;
        if (isMutantCreeperMinionExploding(tame)) return;

        net.minecraft.world.entity.LivingEntity target = TameEntityAdapter.target(tame);
        if (target == null) {
            return;
        }
        if (TameDuelManager.isTameInDuel(tame.getUUID())) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID())) {
                return;
            }
            TameEntityAdapter.setTarget(tame, null);
            return;
        }
        if (!TLAdminRuntimeSettings.friendlyFireEnabled(TameEntityAdapter.ownerUuid(tame))) {
            if (target instanceof Player) {
                clearForbiddenPlayerAggression(tame);
                return;
            }
            if (TameEntityAdapter.isTame(target)) {
                TameEntityAdapter.setTarget(tame, null);
                return;
            }
        }
        if (TameEntityAdapter.isTame(target)) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID())) {
                return;
            }
            TameEntityAdapter.setTarget(tame, null);
            return;
        }
        if (TameRegistry.isProtectedAttackTarget(TameEntityAdapter.ownerUuid(tame), target)) {
            TameEntityAdapter.setTarget(tame, null);
        }
    }

    private static void clearForbiddenPlayerAggression(LivingEntity tame) {
        if (tame == null) return;
        TameEntityAdapter.setTarget(tame, null);
        tame.setLastHurtByMob(null);
        tame.setLastHurtMob(null);
        tame.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        tame.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        tame.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (tame instanceof Mob mob) mob.getNavigation().stop();
        if (tame instanceof NeutralMob neutral) {
            neutral.setPersistentAngerTarget(null);
            neutral.setRemainingPersistentAngerTime(0);
        }
    }

    private static LivingEntity resolveTameAttacker(net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct) {
        if (attacker instanceof LivingEntity tame) {
            return tame;
        }
        if (direct instanceof Projectile projectile) {
            return TameEntityAdapter.owner(projectile);
        }
        return null;
    }

    private static UUID resolveParticipantId(net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct) {
        if (attacker instanceof Player player) {
            return player.getUUID();
        }
        if (direct instanceof Projectile projectile && TameEntityAdapter.owner(projectile) instanceof Player player) {
            return player.getUUID();
        }
        LivingEntity tame = resolveTameAttacker(attacker, direct);
        return tame == null ? null : tame.getUUID();
    }

    private static Player resolvePlayerAttacker(net.minecraft.world.damagesource.DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getEntity() instanceof Player player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && TameEntityAdapter.owner(projectile) instanceof Player owner) {
            return owner;
        }
        return null;
    }

    private static boolean isMutantCreeperMinionSelfExplosion(LivingEntity tame, LivingAttackEvent event) {
        if (tame == null || event == null || !isMutantCreeperMinionSelfExplosion(tame, event.getSource())) {
            return false;
        }
        return true;
    }

    private static boolean isMutantCreeperMinionSelfExplosion(LivingEntity tame, net.minecraft.world.damagesource.DamageSource source) {
        if (tame == null || source == null || source.getEntity() != tame || !source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        return isMutantCreeperMinion(tame);
    }

    private static boolean isMutantCreeperMinionExploding(LivingEntity tame) {
        if (!isMutantCreeperMinion(tame)) {
            return false;
        }
        try {
            Method hasIgnited = tame.getClass().getMethod("hasIgnited");
            Object ignited = hasIgnited.invoke(tame);
            if (ignited instanceof Boolean flag && flag) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getExplodeState = tame.getClass().getMethod("getExplodeState");
            Object state = getExplodeState.invoke(tame);
            return state instanceof Number number && number.intValue() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMutantCreeperMinion(LivingEntity tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null
                && "mutantmonsters".equals(key.getNamespace())
                && "creeper_minion".equals(key.getPath());
    }
}
