package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class TameProtectionEvents {
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        var direct = event.getSource().getDirectEntity();
        if (victim instanceof TamableAnimal targetTame && targetTame.isTame() && isMutantCreeperMinionSelfExplosion(targetTame, event)) {
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
        if (victim instanceof TamableAnimal target && target.isTame() && attacker instanceof Player) {
            if (!TameDuelManager.areDuelOpponents(attacker.getUUID(), target.getUUID())) {
                event.setCanceled(true);
            }
            return;
        }

        TamableAnimal tameAttacker = resolveTameAttacker(attacker, direct);
        if (tameAttacker == null || !tameAttacker.isTame()) {
            return;
        }

        if (TameRegistry.isProtectedAttackTarget(tameAttacker, victim)) {
            event.setCanceled(true);
            return;
        }

        if (!TLAdminRuntimeSettings.friendlyFireEnabled()) {
            if (victim instanceof Player) {
                event.setCanceled(true);
                return;
            }
            if (victim instanceof TamableAnimal targetTame && targetTame.isTame()) {
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
        if (victim instanceof TamableAnimal targetTame && targetTame.isTame()) {
            if (TameDuelManager.areDuelOpponents(tameAttacker.getUUID(), targetTame.getUUID())) {
                return;
            }
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) {
            return;
        }
        if (!isMutantCreeperMinionSelfExplosion(tame, event.getSource())) {
            return;
        }
        event.setCanceled(true);
        tame.setHealth(Math.max(1.0F, Math.min(tame.getMaxHealth(), 1.0F)));
        tame.setTarget(null);
        tame.setLastHurtByMob(null);
        tame.setLastHurtMob(null);
        tame.getNavigation().stop();
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame)) return;
        if (!tame.isTame()) return;

        net.minecraft.world.entity.LivingEntity target = tame.getTarget();
        if (target == null) {
            refreshLegendaryMonstersDuelTarget(tame);
            return;
        }
        if (TameDuelManager.isTameInDuel(tame.getUUID())) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID())) {
                return;
            }
            tame.setTarget(null);
            refreshLegendaryMonstersDuelTarget(tame);
            return;
        }
        if (!TLAdminRuntimeSettings.friendlyFireEnabled()) {
            if (target instanceof Player) {
                tame.setTarget(null);
                return;
            }
            if (target instanceof TamableAnimal targetTame && targetTame.isTame()) {
                tame.setTarget(null);
                return;
            }
        }
        if (target instanceof TamableAnimal targetTame && targetTame.isTame()) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), targetTame.getUUID())) {
                return;
            }
            tame.setTarget(null);
            return;
        }
        if (TameRegistry.isProtectedAttackTarget(tame, target)) {
            tame.setTarget(null);
        }
    }

    private static void refreshLegendaryMonstersDuelTarget(TamableAnimal tame) {
        if (tame == null || !isLegendaryMonstersPet(tame) || !TameDuelManager.isTameInDuel(tame.getUUID()) || tame.level().getServer() == null) {
            return;
        }
        LivingEntity nearest = TameDuelManager.findNearestLoadedOpponent(tame.level().getServer(), tame);
        if (nearest == null || !nearest.isAlive() || nearest.level() != tame.level()) {
            return;
        }
        if (tame.getTarget() != nearest) {
            tame.setTarget(nearest);
        }
        tame.getNavigation().moveTo(nearest, 1.15D);
    }

    private static boolean isLegendaryMonstersPet(TamableAnimal tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null && "legendary_monsters".equals(key.getNamespace());
    }

    private static TamableAnimal resolveTameAttacker(net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct) {
        if (attacker instanceof TamableAnimal tame) {
            return tame;
        }
        if (direct instanceof Projectile projectile && projectile.getOwner() instanceof TamableAnimal tame) {
            return tame;
        }
        return null;
    }

    private static UUID resolveParticipantId(net.minecraft.world.entity.Entity attacker, net.minecraft.world.entity.Entity direct) {
        if (attacker instanceof Player player) {
            return player.getUUID();
        }
        TamableAnimal tame = resolveTameAttacker(attacker, direct);
        return tame == null ? null : tame.getUUID();
    }

    private static boolean isMutantCreeperMinionSelfExplosion(TamableAnimal tame, LivingAttackEvent event) {
        if (tame == null || event == null || !isMutantCreeperMinionSelfExplosion(tame, event.getSource())) {
            return false;
        }
        return true;
    }

    private static boolean isMutantCreeperMinionSelfExplosion(TamableAnimal tame, net.minecraft.world.damagesource.DamageSource source) {
        if (tame == null || source == null || source.getEntity() != tame || !source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null
                && "mutantmonsters".equals(key.getNamespace())
                && "creeper_minion".equals(key.getPath());
    }
}
