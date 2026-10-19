package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import java.util.Map;
import java.util.WeakHashMap;

/** Transient, lazy budgets for released hunters only. Never loads chunks or changes persistent AI flags. */
public final class LostHunterActivity {
    public static final int PLAYER_CHECK_TICKS = 20;
    public static final int IDLE_SEARCH_TICKS = 100;
    public static final int COMBAT_PATH_TICKS = 20;
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();
    private static final class State {
        long nextPlayerCheck = Long.MIN_VALUE, nextPath = Long.MIN_VALUE;
        boolean active, combatPath;
    }
    private LostHunterActivity() {}

    public static boolean active(LivingEntity entity) {
        if (!TameLimitService.isReleased(entity)) return true;
        State state = STATES.computeIfAbsent(entity, ignored -> new State());
        long now = entity.level().getGameTime();
        if (state.nextPlayerCheck == Long.MIN_VALUE || now >= state.nextPlayerCheck
                || state.nextPlayerCheck - now > PLAYER_CHECK_TICKS) {
            // Align subsequent checks to different ticks for different creatures.
            int phase = Math.floorMod(entity.getUUID().hashCode(), PLAYER_CHECK_TICKS);
            state.nextPlayerCheck = now + 1 + Math.floorMod(phase - (int)Math.floorMod(now + 1, PLAYER_CHECK_TICKS), PLAYER_CHECK_TICKS);
            boolean nearby = entity.level().getNearestPlayer(entity.getX(), entity.getY(), entity.getZ(), 64,
                    player -> player.isAlive() && !player.isSpectator()) != null;
            if (nearby != state.active) state.nextPath = Long.MIN_VALUE;
            state.active = nearby;
        }
        if (!state.active && entity instanceof Mob mob) {
            if (mob.getTarget() != null) mob.setTarget(null);
            if (!mob.getNavigation().isDone()) mob.getNavigation().stop();
        }
        return state.active;
    }

    public static boolean claimPath(Mob mob) {
        if (!TameLimitService.isReleased(mob)) return true;
        if (!active(mob)) return false;
        State state = STATES.get(mob);
        long now = mob.level().getGameTime();
        boolean combat = mob.getTarget() != null && mob.getTarget().isAlive();
        if (combat != state.combatPath) { state.nextPath = Long.MIN_VALUE;state.combatPath = combat; }
        int interval = combat ? COMBAT_PATH_TICKS : IDLE_SEARCH_TICKS;
        if (state.nextPath != Long.MIN_VALUE && now < state.nextPath && state.nextPath - now <= IDLE_SEARCH_TICKS) return false;
        state.nextPath = now + interval;
        return true;
    }

    public static boolean abilityPass(LivingEntity entity, long now) {
        if (!TameLimitService.isReleased(entity)) return true;
        if (!active(entity)) return false;
        LivingEntity target = TameEntityAdapter.target(entity);
        int interval = target != null && target.isAlive() ? 20 : 100;
        // The shared ability loop runs every five ticks. Match its staggering so
        // existing heavy/periodic handlers still get their scheduled passes.
        return Math.floorMod(now / 5, interval / 5) == Math.floorMod(entity.getUUID().hashCode(), interval / 5);
    }

    public static void forget(LivingEntity entity) { STATES.remove(entity); }
}
