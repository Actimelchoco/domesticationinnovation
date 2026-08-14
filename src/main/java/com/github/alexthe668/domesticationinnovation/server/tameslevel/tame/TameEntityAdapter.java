package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;

import java.util.UUID;

public final class TameEntityAdapter {
    private TameEntityAdapter() {
    }

    public static boolean isTame(Entity entity) {
        if (entity instanceof TamableAnimal tamable) return tamable.isTame();
        return entity instanceof ModifedToBeTameable modified && modified.isTame();
    }

    public static UUID ownerUuid(Entity entity) {
        if (entity instanceof TamableAnimal tamable) return tamable.getOwnerUUID();
        return entity instanceof ModifedToBeTameable modified ? modified.getTameOwnerUUID() : null;
    }

    public static void setOwner(Entity entity, UUID ownerUuid) {
        if (entity instanceof TamableAnimal tamable) {
            tamable.setTame(true);
            tamable.setOwnerUUID(ownerUuid);
        } else if (entity instanceof ModifedToBeTameable modified) {
            modified.setTame(true);
            modified.setTameOwnerUUID(ownerUuid);
        }
    }

    public static boolean isHorseType(Entity entity) {
        return entity instanceof AbstractHorse;
    }

    public static LivingEntity findLoaded(MinecraftServer server, UUID entityUuid, UUID tlId) {
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (entityUuid != null) {
                Entity exact = level.getEntity(entityUuid);
                if (exact instanceof LivingEntity living && living.isAlive() && isTame(living)) return living;
            }
            if (tlId == null) continue;
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || !living.isAlive() || !isTame(living)) continue;
                if (tlId.equals(TameData.getTlId(living))) return living;
            }
        }
        return null;
    }

    public static LivingEntity findLoadedOwned(MinecraftServer server, TameData data, UUID ownerUuid, boolean repairIdentity) {
        if (data == null || ownerUuid == null || !ownerUuid.equals(data.ownerUUID)) return null;
        LivingEntity living = findLoaded(server, data.uuid, data.tlId);
        if (living == null) return null;
        if (!ownerUuid.equals(ownerUuid(living))) {
            if (!repairIdentity) return null;
            setOwner(living, ownerUuid);
        }
        if (repairIdentity && data.tlId != null) TameData.syncTlIdToEntity(living, data.tlId);
        return living;
    }
}
