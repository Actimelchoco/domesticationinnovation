package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;

public final class LegendaryMonstersDuelCompat {

    private LegendaryMonstersDuelCompat() {
    }

    public static boolean shouldAllowFriendlyFireInDuel(Entity attacker, Entity target) {
        if (attacker == null || target == null) {
            return false;
        }
        return TameDuelManager.areDuelOpponents(attacker.getUUID(), target.getUUID());
    }
}
