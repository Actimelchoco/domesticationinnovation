package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Optional bridge: keep the native totem's charge, delay and spawn behavior. */
public final class CompanionsRespawnTotem {
    private static final String RESPAWN = "TLCompanionsTotemRespawn";
    private CompanionsRespawnTotem() {}

    private static class Native {
        static final Class<?> TOTEM;
        static final Method PERSISTENT_DATA, CHARGES, QUEUE;
        static final java.lang.reflect.Field SAVED;
        static {
            try {
                TOTEM = Class.forName("dev.xylonity.companions.common.blockentity.RespawnTotemBlockEntity");
                PERSISTENT_DATA = Class.forName("dev.xylonity.knightlib.api.entity.data.PersistentData").getMethod("get", Entity.class);
                CHARGES = TOTEM.getMethod("getCharges");
                QUEUE = TOTEM.getMethod("queueRespawn", CompoundTag.class, int.class);
                SAVED = TOTEM.getField("savedEntities");
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Companions respawn totem API", e); }
        }
    }

    public static List<LivingEntity> captureCandidates(Level level, AABB bounds) {
        return level.getEntitiesOfClass(LivingEntity.class, bounds,
                entity -> TameEntityAdapter.isTame(entity) && TameEntityAdapter.ownerUuid(entity) != null);
    }

    /** DI owns death processing for registered tames, including duel exclusions. */
    public static boolean handlesNativeDeath(Object event) {
        try {
            Object entity = event.getClass().getMethod("getEntity").invoke(event);
            return entity instanceof LivingEntity living && tracked(living) != null;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static TameData tracked(LivingEntity entity) {
        TameData data = TameRegistry.get(entity.getUUID());
        UUID id = TameData.getTlId(entity);
        return data != null ? data : id == null ? null : TameRegistry.getByTlId(id);
    }

    @SuppressWarnings("unchecked")
    public static void onDeath(LivingEntity entity, TameData data, boolean duel) {
        if (duel || data == null || !ModList.get().isLoaded("companions") || entity.getServer() == null) return;
        try {
            CompoundTag binding = (CompoundTag) Native.PERSISTENT_DATA.invoke(null, entity);
            if (!binding.contains("RespawnTotemPos") || !binding.contains("RespawnTotemDim")) return;
            ResourceLocation dimension = ResourceLocation.tryParse(binding.getString("RespawnTotemDim"));
            if (dimension == null) return;
            ServerLevel level = entity.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            BlockPos pos = BlockPos.of(binding.getLong("RespawnTotemPos"));
            if (level == null) return;
            var block = level.getBlockEntity(pos);
            if (!Native.TOTEM.isInstance(block)) return;
            if ((int) Native.CHARGES.invoke(block) <= 0) {
                ((Map<UUID, CompoundTag>) Native.SAVED.get(block)).remove(entity.getUUID());
                block.setChanged();
                return;
            }
            CompoundTag snapshot = new CompoundTag();
            if (!entity.save(snapshot)) return;
            for (String key : List.of("DeathTime", "Fire", "HurtByTimestamp", "HurtTime", "FallFlying", "Motion")) snapshot.remove(key);
            // Keep UUID and TL identity; native respawns normally generate a fresh UUID.
            snapshot.putFloat("Health", 1.0F);
            snapshot.getCompound("ForgeData").putBoolean(RESPAWN, true);
            Native.QUEUE.invoke(block, snapshot, 20);
            block.setChanged();
        } catch (ReflectiveOperationException e) {
            DomesticationMod.LOGGER.error("Could not queue tame at Companions respawn totem", e);
        }
    }

    /** A command or another revival may have already restored this queued tame. */
    public static boolean addRespawn(ServerLevel level, Entity entity, net.minecraft.world.level.block.entity.BlockEntity totem) {
        if (!(entity instanceof LivingEntity living) || !entity.getPersistentData().getBoolean(RESPAWN)) return level.addFreshEntity(entity);
        TameData data = tracked(living);
        if (data == null || !data.dead || data.stored || level.getEntity(entity.getUUID()) != null) return false;
        try {
            // Several deaths can queue while a single native charge remains.
            if ((int) Native.CHARGES.invoke(totem) <= 0) return false;
        } catch (ReflectiveOperationException e) {
            DomesticationMod.LOGGER.error("Could not read Companions respawn totem charges", e);
            return false;
        }
        entity.getPersistentData().remove(RESPAWN);
        if (!level.addFreshEntity(entity)) {
            data.dead = true;
            TameRegistry.markDirty();
            return false;
        }
        data.dead = false;
        data.commandRespawnAfterDayTime = -1L;
        data.deadGameTime = 0L;
        data.deadUnixMillis = 0L;
        data.deathDimension = "";
        data.deathX = data.deathY = data.deathZ = 0;
        TameRegistry.removeDeathsForIdentity(data.uuid, data.tlId);
        TameRegistry.markDirty();
        return true;
    }
}
