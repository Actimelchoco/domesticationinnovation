package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Navigation only: native animation goals still choose and perform every attack. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public class SkeloraptorDuelChaseGoal extends Goal {
    private static final Map<Mob, SkeloraptorDuelChaseGoal> ACTIVE = new IdentityHashMap<>();
    private static final int PATH_INTERVAL = 20;
    // Legendary Monsters uses 4.0 for this species' native follow-owner goal.
    // Its base movement attribute is only 0.1; a 1.0 path request crawls.
    private static final double CHASE_SPEED = 4.0D;
    protected final Mob mob;
    private final Method attackState;
    private int nextPathTick;

    protected SkeloraptorDuelChaseGoal(Mob mob) throws NoSuchMethodException {
        this.mob = mob;
        this.attackState = mob.getClass().getMethod("getAttackState");
        setFlags(EnumSet.of(Flag.MOVE));
    }

    public static void ensure(LivingEntity entity) {
        if (!(entity instanceof Mob mob) || ACTIVE.containsKey(mob)) return;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null || !id.toString().equals("legendary_monsters:skeloraptor")) return;
        try {
            var goal = new SkeloraptorDuelChaseGoal(mob);
            ACTIVE.put(mob, goal);
            // Float retains equal priority; native attacks run once we close to range.
            mob.goalSelector.addGoal(0, goal);
        } catch (NoSuchMethodException ignored) {
            // Unknown mod versions retain their native AI rather than forcing movement.
        }
    }

    public static void remove(UUID id) {
        if (id == null || ACTIVE.isEmpty()) return;
        var entries = ACTIVE.entrySet().iterator();
        while (entries.hasNext()) {
            var entry = entries.next();
            Mob body = entry.getKey();
            if (!id.equals(body.getUUID()) && !id.equals(TameData.getTlId(body))) continue;
            body.goalSelector.removeGoal(entry.getValue());
            entries.remove();
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) { ACTIVE.clear(); }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || target.level() != mob.level()
                || mob.isNoAi() || mob.isPassenger() || mob.isVehicle()
                || mob instanceof TamableAnimal pet && pet.isOrderedToSit()
                || mob.distanceToSqr(target) <= 9.0D
                || !allowsTarget(target)) return false;
        try { return ((Number) attackState.invoke(mob)).intValue() == 0; }
        catch (ReflectiveOperationException ignored) { return false; }
    }

    protected boolean allowsTarget(LivingEntity target) {
        return TameDuelManager.areDuelOpponents(mob.getUUID(), target.getUUID());
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { nextPathTick = Math.max(nextPathTick, mob.tickCount); }
    @Override public void stop() { mob.getNavigation().stop(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) return;
        mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        if (mob.tickCount < nextPathTick) return;
        nextPathTick = mob.tickCount + PATH_INTERVAL;
        mob.getNavigation().moveTo(target, CHASE_SPEED);
    }
}
