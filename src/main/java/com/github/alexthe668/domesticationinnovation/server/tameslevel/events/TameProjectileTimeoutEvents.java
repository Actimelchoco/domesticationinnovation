package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.DragonFireball;
import net.minecraft.world.entity.projectile.LlamaSpit;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TameProjectileTimeoutEvents {
    private static final int SWEEP_INTERVAL_TICKS = 3600;
    private static final Map<UUID, TrackedProjectile> TRACKED = new ConcurrentHashMap<>();

    private record TrackedProjectile(Entity entity, int maxTicks) {}

    public static void track(Entity entity, int maxTicks) {
        if (entity == null || maxTicks <= 0) {
            return;
        }
        TRACKED.put(entity.getUUID(), new TrackedProjectile(entity, maxTicks));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || TRACKED.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, TrackedProjectile>> iterator = TRACKED.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TrackedProjectile> entry = iterator.next();
            TrackedProjectile tracked = entry.getValue();
            Entity entity = tracked.entity();
            if (entity == null || entity.isRemoved()) {
                iterator.remove();
                continue;
            }
            if (entity.tickCount >= tracked.maxTicks()) {
                entity.discard();
                iterator.remove();
            }
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % SWEEP_INTERVAL_TICKS != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity == null || entity.isRemoved()) {
                    continue;
                }
                int maxTicks = forcedLifetime(entity);
                if (maxTicks > 0 && entity.tickCount >= maxTicks) {
                    entity.discard();
                    TRACKED.remove(entity.getUUID());
                }
            }
        }
    }

    private static int forcedLifetime(Entity entity) {
        if (entity instanceof TimedTameArrow || entity instanceof AbstractArrow && entity.getClass() == TimedTameArrow.class) {
            return 60;
        }
        if (entity instanceof TimedTameSnowball || entity instanceof Snowball && entity.getClass() == TimedTameSnowball.class) {
            return 60;
        }
        if (entity instanceof TimedTameThrownPotion || entity instanceof ThrownPotion && entity.getClass() == TimedTameThrownPotion.class) {
            return 60;
        }
        if (entity instanceof NoGriefLargeFireball) {
            return 80;
        }
        if (entity instanceof TimedTameWitherSkull || entity instanceof WitherSkull && entity.getClass() == TimedTameWitherSkull.class) {
            return 80;
        }
        if (entity instanceof TimedTameSmallFireball || entity instanceof SmallFireball && entity.getClass() == TimedTameSmallFireball.class) {
            return 80;
        }
        if (entity instanceof TimedTameTrident || entity instanceof ThrownTrident && entity.getClass() == TimedTameTrident.class) {
            return 80;
        }
        if (entity instanceof TimedTameDragonFireball || entity instanceof DragonFireball && entity.getClass() == TimedTameDragonFireball.class) {
            return 80;
        }
        if (entity instanceof TimedTameLlamaSpit || entity instanceof LlamaSpit && entity.getClass() == TimedTameLlamaSpit.class) {
            return 80;
        }
        if (entity instanceof TimedTameShulkerBullet || entity instanceof ShulkerBullet && entity.getClass() == TimedTameShulkerBullet.class) {
            return 80;
        }
        return 0;
    }
}
