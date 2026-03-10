package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class TameWorldLoadEvents {

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {

        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!TameRegistry.isInitialized()) {
            TameRegistry.init(level.getServer());
        }

        for (TamableAnimal tame : level.getEntitiesOfClass(
                TamableAnimal.class,
                level.getWorldBorder().getCollisionShape().bounds()
        )) {

            if (!tame.isTame()) continue;
            TameData data = TameSpawnEvents.registerOrRestoreTame(tame, false);
            if (data != null) {
                System.out.println("[TamesLevel] Loaded tame: " + tame.getName().getString());
            }
        }
    }
}
