package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public final class LegendaryMonstersDuelCompat {
    private LegendaryMonstersDuelCompat() {
    }

    public static boolean shouldAllowFriendlyFireInDuel(Entity attacker, Entity target) {
        UUID attackerId = resolveParticipantId(attacker);
        UUID targetId = resolveParticipantId(target);
        return attackerId != null && targetId != null && TameDuelManager.areDuelOpponents(attackerId, targetId);
    }

    private static UUID resolveParticipantId(Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID();
        }
        if (entity instanceof TamableAnimal tame && tame.isTame()) {
            return tame.getUUID();
        }
        return entity == null ? null : entity.getUUID();
    }
}
