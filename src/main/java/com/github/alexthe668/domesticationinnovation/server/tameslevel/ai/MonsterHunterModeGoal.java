package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

final class MonsterHunterModeGoal extends AbstractModeGoal {
    private int scanTicks;
    private int combatRescanTicks;
    private int lastRetaliationTimestamp = Integer.MIN_VALUE;

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
                scanTicks = 0;
                combatRescanTicks = 0;
                return;
            }
        }

        LivingEntity current = tame.getTarget();
        if (current != null && current.isAlive()) {
            if (++combatRescanTicks >= 200) {
                combatRescanTicks = 0;
                LivingEntity closer = TameGoalSupport.nearestHunterHostile(level, tame, 10.0D);
                if (closer != null && closer != current && tame.distanceToSqr(closer) < tame.distanceToSqr(current)) {
                    tame.setTarget(closer);
                }
            }
            return;
        }
        combatRescanTicks = 0;
        if (++scanTicks < tickInterval()) return;
        scanTicks = 0;
        tickMode(level, data);
    }
}
