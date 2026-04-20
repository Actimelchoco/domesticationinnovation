package com.github.alexthe668.domesticationinnovation.server.tameslevel;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpawnerTriggerSupport {

    private SpawnerTriggerSupport() {
    }

    public static boolean hasSpawnerTriggerTameInRange(Level level, Vec3 center, double range) {
        if (level == null || center == null || range <= 0.0D) {
            return false;
        }
        AABB box = new AABB(
                center.x - range, center.y - range, center.z - range,
                center.x + range, center.y + range, center.z + range
        );
        for (TamableAnimal tame : level.getEntitiesOfClass(TamableAnimal.class, box)) {
            if (tame == null || !tame.isAlive() || !tame.isTame()) {
                continue;
            }
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null && LevelSystem.getAttributeLevel(data, "spawner_trigger") > 0) {
                return true;
            }
        }
        return false;
    }
}
