package com.github.alexthe668.domesticationinnovation.server.misc;

import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CollarTickTracker {

    private final Map<UUID, Integer> blockedCollarTagUpdates = new HashMap<>();


    public void addBlockedEntityTick(UUID uuid, int duration){
        this.blockedCollarTagUpdates.put(uuid, duration);
    }

    public boolean isEntityBlocked(Entity entity){
        return this.blockedCollarTagUpdates.getOrDefault(entity.getUUID(), 0) > 0;
    }

    public void tick(){
        var iterator = blockedCollarTagUpdates.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            int remaining = entry.getValue() - 1;
            if (remaining < 0) iterator.remove();
            else entry.setValue(remaining);
        }
    }
}
