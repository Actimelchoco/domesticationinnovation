package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Shared pursuit rules for native, interface and brain-based monster hunters. */
@Mod.EventBusSubscriber(modid = "domesticationinnovation")
public final class HunterInterestEvents {
    public static final int INTEREST_TICKS = 30 * 20;
    public static final int RETRY_TICKS = 60 * 20;
    private static final Map<Mob, Interest> INTEREST = new WeakHashMap<>();
    private static final class Interest {
        UUID target;
        int lastContact;
        final Map<UUID, Integer> ignoredUntil = new HashMap<>();
    }

    private static boolean isHunter(Mob mob) {
        if (mob.level().isClientSide || !TameEntityAdapter.isTame(mob)
                || TameDuelManager.isEntityInDuel(mob.getUUID())) return false;
        TameData data = TameRegistry.get(mob.getUUID());
        if (data == null) data = TameRegistry.getByTlId(TameData.getTlId(mob));
        return data != null && TameMode.byId(data.mode) == TameMode.MONSTER_HUNTER;
    }

    private static LivingEntity target(Mob mob) {
        if (mob.getTarget() != null) return mob.getTarget();
        return mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                ? mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null) : null;
    }

    public static boolean isIgnored(Mob mob, LivingEntity candidate) {
        if (candidate == null || !isHunter(mob)) return false;
        Interest state = INTEREST.get(mob);
        return state != null && state.ignoredUntil.getOrDefault(candidate.getUUID(), Integer.MIN_VALUE) > mob.tickCount;
    }

    public static boolean holdsCombatPosition(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || !isHunter(mob)) return false;
        LivingEntity target = target(mob);
        return target != null && target.isAlive() && !target.isRemoved() && !isIgnored(mob, target);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide) return;
        if (!isHunter(mob)) { INTEREST.remove(mob); return; }
        Interest state = INTEREST.computeIfAbsent(mob, ignored -> new Interest());
        state.ignoredUntil.values().removeIf(until -> until <= mob.tickCount);
        LivingEntity target = target(mob);
        if (target == null) { state.target = null; return; }
        if (!target.getUUID().equals(state.target)) {
            state.target = target.getUUID();
            state.lastContact = mob.tickCount;
        }
        boolean invalid = !target.isAlive() || target.isRemoved() || target.level() != mob.level();
        if (!invalid && !isIgnored(mob, target) && mob.tickCount - state.lastContact < INTEREST_TICKS) return;
        if (!invalid && !isIgnored(mob, target)) state.ignoredUntil.put(target.getUUID(), mob.tickCount + RETRY_TICKS);
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        if (mob.getLastHurtByMob() == target) mob.setLastHurtByMob(null);
        if (mob.getLastHurtMob() == target) mob.setLastHurtMob(null);
        mob.getNavigation().stop();
        state.target = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void changeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Mob mob && isIgnored(mob, event.getNewTarget())) event.setNewTarget(null);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount() <= 0 || event.getEntity().level().isClientSide) return;
        LivingEntity attacker = TameCombatEvents.resolveTameAttacker(event.getSource());
        if (attacker == null && event.getSource().getEntity() instanceof LivingEntity living) attacker = living;
        if (attacker == null) return;
        contact(event.getEntity(), attacker);
        contact(attacker, event.getEntity());
    }

    private static void contact(LivingEntity entity, LivingEntity other) {
        if (!(entity instanceof Mob mob) || !isHunter(mob)) return;
        Interest state = INTEREST.computeIfAbsent(mob, ignored -> new Interest());
        state.ignoredUntil.remove(other.getUUID());
        if (target(mob) == other) {
            state.target = other.getUUID();
            state.lastContact = mob.tickCount;
        }
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof Mob mob) INTEREST.remove(mob);
    }
}
