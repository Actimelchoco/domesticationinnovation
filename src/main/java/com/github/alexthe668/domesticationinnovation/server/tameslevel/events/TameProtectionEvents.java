package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Comparator;
import java.util.Set;
import java.util.UUID;

public class TameProtectionEvents {
    private static final TagKey<EntityType<?>> ARMAGEDDON_BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("forge", "armageddon_bosses"));
    private static final Set<String> ARMAGEDDON_BOSS_IDS = Set.of(
            "armageddon_mod:arion_tyrant_of_the_emerald_wrath_soldat",
            "armageddon_mod:arion_tyrantofthe_emerald_wrath_ravager",
            "armageddon_mod:bringer_of_doom",
            "armageddon_mod:bringer_of_doom_p_2",
            "armageddon_mod:eldoraththe_ancient_builder",
            "armageddon_mod:elvenite_paladin",
            "armageddon_mod:nyxaris_the_veil_of_oblivion",
            "armageddon_mod:sanghor_lord_of_blood",
            "armageddon_mod:sanghor_lord_of_bloodp_2",
            "armageddon_mod:the_chaos",
            "armageddon_mod:the_discord",
            "armageddon_mod:the_famine",
            "armageddon_mod:the_gobelin_lord",
            "armageddon_mod:the_iron_colossus",
            "armageddon_mod:vaedricthe_fallen_wanderer",
            "armageddon_mod:zoranth_newborn_of_the_zenith",
            "armageddon_mod:zoranththe_forgotten_one"
    );
    private static final double ARMAGEDDON_TAME_REDIRECT_RANGE = 24.0D;


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
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob) {
            retargetArmageddonBossToTame(mob);
        }
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
            return;
        }
        if (TameRegistry.isProtectedAttackTarget(tame, target)) {
            tame.setTarget(null);
        }
    }

    private static void retargetArmageddonBossToTame(Mob mob) {
        if (mob.level().isClientSide || !isArmageddonBoss(mob)) {
            return;
        }
        LivingEntity currentTarget = mob.getTarget();
        if (currentTarget instanceof TamableAnimal targetTame && targetTame.isTame() && targetTame.isAlive()) {
            return;
        }
        TamableAnimal redirect = findArmageddonRedirectTarget(mob, currentTarget);
        if (redirect == null || redirect == currentTarget) {
            return;
        }
        mob.setTarget(redirect);
    }

    private static TamableAnimal findArmageddonRedirectTarget(Mob mob, LivingEntity currentTarget) {
        LivingEntity recentAttacker = mob.getLastHurtByMob();
        if (recentAttacker instanceof TamableAnimal recentTame && isValidArmageddonRedirectTarget(mob, recentTame)) {
            return recentTame;
        }

        Player focusPlayer = null;
        if (currentTarget instanceof Player player) {
            focusPlayer = player;
        } else if (recentAttacker instanceof Player player) {
            focusPlayer = player;
        } else if (currentTarget instanceof TamableAnimal tame && tame.isTame() && tame.getOwner() instanceof Player owner) {
            focusPlayer = owner;
        }
        if (focusPlayer == null || focusPlayer.isCreative() || focusPlayer.isSpectator()) {
            return null;
        }

        LivingEntity bossAttacked = mob.getLastHurtMob();
        UUID ownerUuid = focusPlayer.getUUID();
        return mob.level().getEntitiesOfClass(TamableAnimal.class, mob.getBoundingBox().inflate(ARMAGEDDON_TAME_REDIRECT_RANGE), tame ->
                        isValidArmageddonRedirectTarget(mob, tame) && ownerUuid.equals(tame.getOwnerUUID()))
                .stream()
                .min(Comparator.<TamableAnimal>comparingInt(tame -> armageddonRedirectPriority(tame, mob, bossAttacked))
                        .thenComparingDouble(tame -> tame.distanceToSqr(mob)))
                .orElse(null);
    }

    private static int armageddonRedirectPriority(TamableAnimal tame, Mob boss, LivingEntity bossAttacked) {
        int priority = 0;
        if (tame.getTarget() == boss) {
            priority -= 1000;
        }
        if (tame.getLastHurtMob() == boss || tame.getLastHurtByMob() == boss) {
            priority -= 500;
        }
        if (bossAttacked == tame) {
            priority -= 250;
        }
        if (tame.isOrderedToSit()) {
            priority += 10_000;
        }
        return priority;
    }

    private static boolean isValidArmageddonRedirectTarget(Mob boss, TamableAnimal tame) {
        if (tame == null || !tame.isTame() || !tame.isAlive() || tame.isOrderedToSit()) {
            return false;
        }
        if (tame.getOwnerUUID() == null || TameRegistry.isProtectedAttackTarget(tame, boss)) {
            return false;
        }
        return !TameDuelManager.isTameInDuel(tame.getUUID());
    }

    private static boolean isArmageddonBoss(Mob mob) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (key == null) {
            return false;
        }
        return mob.getType().is(ARMAGEDDON_BOSS_TAG) || ARMAGEDDON_BOSS_IDS.contains(key.toString());
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
