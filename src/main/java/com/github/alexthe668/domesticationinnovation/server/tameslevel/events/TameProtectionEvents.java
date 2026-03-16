package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.UUID;

public class TameProtectionEvents {

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        var victim = event.getEntity();
        var attacker = event.getSource().getEntity();
        var direct = event.getSource().getDirectEntity();
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
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame)) return;
        if (!tame.isTame()) return;

        net.minecraft.world.entity.LivingEntity target = tame.getTarget();
        if (target == null) {
            return;
        }
        if (TameDuelManager.isTameInDuel(tame.getUUID())) {
            if (TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID())) {
                return;
            }
            tame.setTarget(null);
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
        }
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
}
