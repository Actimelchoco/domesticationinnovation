package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Event-driven protection for player-placed entities; FTB Chunks stays optional. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class ClaimDecorationProtection {
    private ClaimDecorationProtection() { }

    public static boolean isDecoration(Entity target) {
        return target instanceof ArmorStand || target instanceof HangingEntity
                || target instanceof Boat || target instanceof AbstractMinecart || target instanceof EndCrystal;
    }

    public static boolean blockDamage(Entity target, DamageSource source) {
        return target != null && !target.level().isClientSide && isDecoration(target)
                && source != null && (fromTame(source.getEntity()) || fromTame(source.getDirectEntity()))
                && claimed(target);
    }

    private static boolean fromTame(Entity source) {
        // Resolve real ownership, including direct-only projectile damage and owned explosives.
        for (int depth = 0; source != null && depth < 6; depth++) {
            if (TameEntityAdapter.isTame(source)) return true;
            Entity owner = source instanceof Projectile projectile ? projectile.getOwner()
                    : source instanceof EvokerFangs fangs ? fangs.getOwner()
                    : source instanceof PrimedTnt tnt ? tnt.getOwner()
                    : source instanceof OwnableEntity ownable ? ownable.getOwner() : null;
            if (owner == source) return false;
            source = owner;
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        DamageSource source = event.getExplosion().getDamageSource();
        if (source == null || (!fromTame(source.getEntity()) && !fromTame(source.getDirectEntity()))) return;
        event.getAffectedEntities().removeIf(entity -> isDecoration(entity) && claimed(entity));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target instanceof ArmorStand && TameEntityAdapter.isTame(event.getEntity()) && claimed(target)) event.setCanceled(true);
    }

    private static boolean claimed(Entity target) {
        if (!(target.level() instanceof ServerLevel level) || !ModList.get().isLoaded("ftbchunks")) return false;
        Bridge bridge = FtbAccess.BRIDGE;
        if (bridge == null) return false;
        // Hanging entities can have their center across a chunk boundary from the anchor.
        BlockPos pos = target instanceof HangingEntity hanging ? hanging.getPos() : target.blockPosition();
        try {
            Object api = bridge.api.invoke(null);
            if (!(boolean) bridge.loaded.invoke(api)) return false;
            Object manager = bridge.manager.invoke(api);
            return bridge.chunk.invoke(manager, bridge.position.newInstance(level, pos)) != null;
        } catch (ReflectiveOperationException failure) {
            FtbAccess.warn(failure);
            return false;
        }
    }

    private record Bridge(Method api, Method loaded, Method manager, Method chunk, Constructor<?> position) { }
    private static final class FtbAccess {
        private static boolean warned;
        private static final Bridge BRIDGE = load();
        private static Bridge load() {
            try {
                Method api = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI").getMethod("api");
                Method manager = api.getReturnType().getMethod("getManager");
                Class<?> position = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                return new Bridge(api, api.getReturnType().getMethod("isManagerLoaded"), manager,
                        manager.getReturnType().getMethod("getChunk", position), position.getConstructor(Level.class, BlockPos.class));
            } catch (ReflectiveOperationException failure) {
                warn(failure);
                return null;
            }
        }
        private static void warn(Exception failure) {
            if (warned) return;
            warned = true;
            DomesticationMod.LOGGER.warn("Unable to query FTB claims for tame decoration protection", failure);
        }
    }
}
