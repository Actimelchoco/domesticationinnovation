package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.HunterInterestEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.DuelFollowerPause;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.LivingEntity;

/** Native owner-follow AI must yield immediately to the selected tame assignment. */
public final class OwnerFollowRules {
    private OwnerFollowRules() { }

    public static boolean blocked(LivingEntity tame) {
        if (tame == null) return false;
        if (DuelFollowerPause.isPaused(tame) || HunterInterestEvents.holdsCombatPosition(tame)
                || TameCommands.isDuelSessionLocked(tame.getUUID())) return true;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) data = TameRegistry.getByTlId(TameData.getTlId(tame));
        return data != null && (data.hasHome || data.guardianResting || data.wanderLock
                || data.movementOrder != 0 || DuelFollowerPause.shouldWait(data)
                || data.tlId != null && TameCommands.isDuelSessionLocked(data.tlId));
    }
}
