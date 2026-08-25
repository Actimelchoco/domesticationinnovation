package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

import java.util.UUID;

final class MonsterHunterModeGoal extends AbstractModeGoal {
    private int scanTicks;
    private int combatRescanTicks;
    private int lastRetaliationTimestamp = Integer.MIN_VALUE;
    private UUID trackedLivingTarget;

    MonsterHunterModeGoal(TamableAnimal tame) {
        super(tame);
    }

    @Override
    protected TameMode mode() {
        return TameMode.MONSTER_HUNTER;
    }

    @Override
    protected int tickInterval() {
        return 10;
    }

    @Override
    protected void tickMode(ServerLevel level, TameData data) {
        TameGoalSupport.setHunterTarget(level, tame, 10.0D);
    }

    @Override
    public void tick() {
        if (!(tame.level() instanceof ServerLevel level)) return;
        TameData data = TameGoalSupport.data(tame);
        if (data == null) return;

        int hurtTimestamp = tame.getLastHurtByMobTimestamp();
        LivingEntity attacker = tame.getLastHurtByMob();
        if (hurtTimestamp != lastRetaliationTimestamp) {
            lastRetaliationTimestamp = hurtTimestamp;
            if (attacker != null && attacker.level() == level && TameGoalSupport.isHunterHostile(attacker)) {
                tame.setTarget(attacker);
                trackedLivingTarget = attacker.getUUID();
                scanTicks = 0;
                combatRescanTicks = 0;
                return;
            }
        }

        LivingEntity current = tame.getTarget();
        boolean currentUnloaded = current != null
                && (current.isRemoved() || level.getEntity(current.getUUID()) != current);
        if (trackedLivingTarget != null && (current == null || !current.isAlive() || currentUnloaded)) {
            trackedLivingTarget = null;
            scanTicks = 0;
            combatRescanTicks = 0;
            if (currentUnloaded) tame.setTarget(null);
            tickMode(level, data);
            LivingEntity replacement = tame.getTarget();
            if (replacement != null && replacement.isAlive()) {
                trackedLivingTarget = replacement.getUUID();
            }
            return;
        }
        if (current != null && current.isAlive()) {
            trackedLivingTarget = current.getUUID();
            if (++combatRescanTicks >= 200) {
                combatRescanTicks = 0;
                LivingEntity closer = TameGoalSupport.nearestHunterHostile(level, tame, 10.0D);
                if (closer != null && closer != current && tame.distanceToSqr(closer) < tame.distanceToSqr(current)) {
                    tame.setTarget(closer);
                    trackedLivingTarget = closer.getUUID();
                }
            }
            return;
        }
        combatRescanTicks = 0;
        trackedLivingTarget = null;
        if (++scanTicks < tickInterval()) return;
        scanTicks = 0;
        tickMode(level, data);
        LivingEntity acquired = tame.getTarget();
        if (acquired != null && acquired.isAlive()) {
            trackedLivingTarget = acquired.getUUID();
        }
    }
}
