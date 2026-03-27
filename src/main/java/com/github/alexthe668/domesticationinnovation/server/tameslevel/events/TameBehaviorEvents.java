package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TamePerformanceProfiler;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

public class TameBehaviorEvents {
    @SubscribeEvent
    public static void onTameTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) return;
        if (tame.level().isClientSide) return;
        if (!tame.isAlive()) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            data = TameSpawnEvents.registerOrRestoreTame(tame, true);
            if (data == null) {
                return;
            }
        }
        TameSpawnEvents.processDeferredStatRefresh(tame, data);
        final TameData activeData = data;
        if (TameDuelManager.isTameInDuel(tame.getUUID())) return;
        if (hasInvalidTarget(tame)) {
            tame.setTarget(null);
        }

        if (activeData.closeMovement && tame.tickCount % 10 == 0) {
            TamePerformanceProfiler.run("behavior.close_owner", () -> handleCloseOwner(tame, activeData));
        }

        if (activeData.hasHome) {
            TamePerformanceProfiler.run("behavior.guardian_return_timer", () -> updateGuardianReturnTimer(tame, activeData));
            TamePerformanceProfiler.run("behavior.guardian_target_timeout", () -> updateGuardianTargetTimeout(tame, activeData));
        }

        int scanInterval = getBehaviorScanInterval(tame);
        if (tame.tickCount % scanInterval != 0) return;
        if (tame.isOrderedToSit()) return;

        if (data.skeletonMovement && tame.getTarget() != null && tame.getTarget().isAlive() && tame.tickCount % 100 == 0) {
            TamePerformanceProfiler.run("behavior.skeleton_spacing", () -> handleSkeletonSpacing(tame));
        }

        if (tame.tickCount % getGuardianReturnInterval(tame) == 0) {
            TamePerformanceProfiler.run("behavior.guardian_return", () -> handleGuardianMovement(tame, activeData));
        }

    }

    @SubscribeEvent
    public static void onTameMount(EntityMountEvent event) {
        if (!event.isMounting()) return;
        if (!(event.getEntityMounting() instanceof TamableAnimal tame) || !tame.isTame()) return;

        Entity mount = event.getEntityBeingMounted();
        ResourceLocation mountedType = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        if (mountedType == null) return;
        String namespace = mountedType.getNamespace();
        boolean valhelsiaNamespace = "valhelsia_structures".equals(namespace)
                || "valhelsia_furniture".equals(namespace)
                || "valhelsia_furnitures".equals(namespace);
        if (!valhelsiaNamespace) return;

        String path = mountedType.getPath();
        if (!path.contains("seat") && !path.contains("chair")) return;

        event.setCanceled(true);
        tame.stopRiding();
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer victim)) return;
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof LivingEntity attacker)) return;
        if (attacker instanceof TamableAnimal ta && ta.isTame()) return;

        for (TameData data : TameRegistry.getOwned(victim.getUUID())) {
            if (!victim.getUUID().equals(data.ownerUUID)) continue;
            Entity raw = victim.serverLevel().getEntity(data.uuid);
            if (!(raw instanceof TamableAnimal tame) || !tame.isTame()) continue;
            if (tame.level() != victim.level()) continue;
            if (tame.isOrderedToSit()) continue;
            TamePerformanceProfiler.run("behavior.retarget_on_owner_hurt", () -> applyRetargetByMode(tame, data, victim, attacker));
        }
    }

    @SubscribeEvent
    public static void onOwnerAttack(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        ServerPlayer owner = resolveOwnerAttacker(event);
        if (owner == null) return;
        if (victim instanceof TamableAnimal ta && ta.isTame()) return;

        for (TameData data : TameRegistry.getOwned(owner.getUUID())) {
            if (!owner.getUUID().equals(data.ownerUUID)) continue;
            Entity raw = owner.serverLevel().getEntity(data.uuid);
            if (!(raw instanceof TamableAnimal tame) || !tame.isTame()) continue;
            if (tame.level() != owner.level()) continue;
            if (tame.isOrderedToSit()) continue;
            TamePerformanceProfiler.run("behavior.retarget_on_owner_attack", () -> applyRetargetByMode(tame, data, owner, victim));
        }
    }

    @SubscribeEvent
    public static void onTameHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) return;
        if (!tame.isAlive() || !tame.isOrderedToSit()) return;
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        if (!isValidCombatTarget(tame, attacker)) return;
        tame.setTarget(attacker);
    }

    private static void applyRetargetByMode(TamableAnimal tame, TameData data, ServerPlayer owner, LivingEntity ownerCombatTarget) {
        if (ownerCombatTarget == null || !ownerCombatTarget.isAlive()) return;
        if (TameDuelManager.isTameInDuel(tame.getUUID())) return;
        if (ownerCombatTarget instanceof TamableAnimal otherTame && otherTame.isTame()) return;
        TameMode mode = TameMode.byId(data.mode);
        if (mode == TameMode.PASSIVE) return;
        if (hasValidCurrentTarget(tame)) return;

        switch (mode) {
            case DEFAULT -> {
                // Vanilla-like default mode: no forced retargeting from this mod.
            }
            case DEFAULT_PLUS -> tame.setTarget(ownerCombatTarget);
            case BODYGUARD -> tame.setTarget(ownerCombatTarget);
            case BOSS -> {
                LivingEntity highest = findHighestHpHostile(owner, 96.0D);
                if (highest != null) {
                    tame.setTarget(highest);
                } else {
                    tame.setTarget(ownerCombatTarget);
                }
            }
            case MONSTER_HUNTER -> {
                LivingEntity nearest = findNearestMonster(tame, 10.0D);
                if (shouldSwitchTarget(tame, nearest)) {
                    tame.setTarget(nearest);
                }
            }
            case ARENA -> {
                LivingEntity nearest = findNearestAggressiveTarget(tame, 64.0D);
                if (shouldSwitchTarget(tame, nearest)) {
                    tame.setTarget(nearest);
                }
            }
            case AGGRESSIVE -> {
                LivingEntity nearest = findNearestAggressiveTarget(tame, 10.0D);
                if (shouldSwitchTarget(tame, nearest)) {
                    tame.setTarget(nearest);
                }
            }
        }
    }

    private static LivingEntity findHighestHpHostile(ServerPlayer owner, double radius) {
        LivingEntity best = null;
        double bestHp = -1.0D;
        AABB box = new AABB(
                owner.getX() - radius, owner.getY() - radius, owner.getZ() - radius,
                owner.getX() + radius, owner.getY() + radius, owner.getZ() + radius
        );
        for (LivingEntity living : owner.serverLevel().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!(living instanceof Enemy) || !isValidCombatTarget(null, living, owner.serverLevel())) continue;
            double hp = living.getMaxHealth();
            if (hp > bestHp) {
                bestHp = hp;
                best = living;
            }
        }
        return best;
    }

    private static ServerPlayer resolveOwnerAttacker(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer p) {
            return p;
        }
        if (event.getSource().getDirectEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof ServerPlayer p) {
            return p;
        }
        return null;
    }

    private static LivingEntity findNearestAggressiveTarget(TamableAnimal tame, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = tame.getBoundingBox().inflate(radius);
        for (LivingEntity entity : tame.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!isValidCombatTarget(tame, entity)) continue;
            double d2 = entity.distanceToSqr(tame);
            if (d2 < bestDist) {
                bestDist = d2;
                best = entity;
            }
        }
        return best;
    }

    private static LivingEntity findNearestMonster(TamableAnimal tame, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = tame.getBoundingBox().inflate(radius);
        for (LivingEntity living : tame.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!(living instanceof Enemy)) continue;
            if (!isValidCombatTarget(tame, living)) continue;
            double d2 = living.distanceToSqr(tame);
            if (d2 < bestDist) {
                bestDist = d2;
                best = living;
            }
        }
        return best;
    }

    private static int getBehaviorScanInterval(TamableAnimal tame) {
        if (isIdleOrSitting(tame)) {
            return 40;
        }
        return hasValidCurrentTarget(tame) ? 15 : 8;
    }

    private static int getGuardianReturnInterval(TamableAnimal tame) {
        return isIdleOrSitting(tame) ? 80 : 40;
    }

    private static boolean isIdleOrSitting(TamableAnimal tame) {
        if (tame.isOrderedToSit()) {
            return true;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return false;
        }
        return tame.getLastHurtByMob() == null
                && tame.getLastHurtMob() == null
                && tame.tickCount - tame.getLastHurtByMobTimestamp() >= 100
                && tame.tickCount - tame.getLastHurtMobTimestamp() >= 100;
    }

    private static void handleSkeletonSpacing(TamableAnimal tame) {
        LivingEntity target = tame.getTarget();
        if (target == null || !target.isAlive()) {
            return;
        }
        double desiredDistance = 5.0D;
        if (tame.distanceToSqr(target) > desiredDistance * desiredDistance) {
            return;
        }
        double dx = tame.getX() - target.getX();
        double dz = tame.getZ() - target.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001D) {
            return;
        }
        double scale = desiredDistance / len;
        double targetX = tame.getX() + dx * scale;
        double targetZ = tame.getZ() + dz * scale;
        tame.getNavigation().moveTo(targetX, tame.getY(), targetZ, 1.1D);
    }

    private static void handleGuardianMovement(TamableAnimal tame, TameData data) {
        updateGuardianPhase(tame, data);
        if (data.guardianRelaxing) {
            handleGuardianRelaxedWander(tame, data);
        } else {
            handleGuardianReturn(tame, data);
        }
    }

    private static void handleGuardianReturn(TamableAnimal tame, TameData data) {
        if (data == null || !data.hasHome) {
            return;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return;
        }
        if (data.homeDimension == null || data.homeDimension.isBlank()) {
            return;
        }
        if (!tame.level().dimension().location().toString().equals(data.homeDimension)) {
            return;
        }
        BlockPos home = new BlockPos(data.homeX, data.homeY, data.homeZ);
        double distanceSqr = tame.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (distanceSqr <= 4.0D) {
            tame.getNavigation().stop();
            data.guardianReturnTicks = 0;
            return;
        }
        tame.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D);
    }

    private static void handleGuardianRelaxedWander(TamableAnimal tame, TameData data) {
        if (data == null || !data.hasHome) {
            return;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return;
        }
        if (data.homeDimension == null || data.homeDimension.isBlank()) {
            return;
        }
        if (!tame.level().dimension().location().toString().equals(data.homeDimension)) {
            return;
        }
        BlockPos home = new BlockPos(data.homeX, data.homeY, data.homeZ);
        double distanceSqr = tame.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
        if (distanceSqr > 12.0D * 12.0D) {
            tame.getNavigation().moveTo(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D, 1.0D);
            return;
        }
        if (!tame.getNavigation().isDone()) {
            return;
        }
        if (tame.tickCount % 80 != 0) {
            return;
        }
        int radius = 3 + tame.getRandom().nextInt(4);
        int offsetX = tame.getRandom().nextInt(radius * 2 + 1) - radius;
        int offsetZ = tame.getRandom().nextInt(radius * 2 + 1) - radius;
        double targetX = home.getX() + 0.5D + offsetX;
        double targetZ = home.getZ() + 0.5D + offsetZ;
        tame.getNavigation().moveTo(targetX, home.getY(), targetZ, 0.95D);
    }

    private static void handleCloseOwner(TamableAnimal tame, TameData data) {
        if (data == null || !data.closeMovement || data.hasHome || tame.isOrderedToSit()) {
            return;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return;
        }
        if (!(tame.getOwner() instanceof ServerPlayer owner)) {
            return;
        }
        if (owner.level() != tame.level()) {
            return;
        }
        double distanceSqr = tame.distanceToSqr(owner);
        if (distanceSqr <= 1.5D * 1.5D) {
            tame.getNavigation().stop();
            return;
        }
        if (distanceSqr >= 12.0D * 12.0D) {
            tame.teleportTo(owner.getX(), owner.getY(), owner.getZ());
            tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
            tame.getNavigation().stop();
            return;
        }
        if (distanceSqr > 2.5D * 2.5D) {
            tame.getNavigation().moveTo(owner, 1.15D);
        }
    }

    private static boolean hasInvalidTarget(TamableAnimal tame) {
        LivingEntity target = tame.getTarget();
        return target != null && !isValidCombatTarget(tame, target);
    }

    private static boolean hasValidCurrentTarget(TamableAnimal tame) {
        LivingEntity target = tame.getTarget();
        return target != null && isValidCombatTarget(tame, target);
    }

    private static boolean shouldSwitchTarget(TamableAnimal tame, LivingEntity candidate) {
        if (tame == null || candidate == null) {
            return false;
        }
        LivingEntity current = tame.getTarget();
        if (current == null || !isValidCombatTarget(tame, current)) {
            return true;
        }
        if (current == candidate) {
            return false;
        }
        double currentDist = tame.distanceToSqr(current);
        double candidateDist = tame.distanceToSqr(candidate);
        return candidateDist + 4.0D < currentDist;
    }

    private static boolean isValidCombatTarget(TamableAnimal tame, LivingEntity target) {
        return isValidCombatTarget(tame, target, tame == null ? null : tame.level());
    }

    private static boolean isValidCombatTarget(TamableAnimal tame, LivingEntity target, net.minecraft.world.level.Level sourceLevel) {
        if (tame == null || target == null) return false;
        if (!target.isAlive()) return false;
        if (target == tame) return false;
        if (sourceLevel != null && target.level() != sourceLevel) return false;
        if (target instanceof Player) return false;
        if (target instanceof TamableAnimal otherTame && otherTame.isTame()) return false;
        if (tame != null && TameRegistry.isProtectedAttackTarget(tame, target)) return false;
        return true;
    }

    private static void updateGuardianReturnTimer(TamableAnimal tame, TameData data) {
        if (data == null || !data.hasHome) {
            return;
        }
        updateGuardianPhase(tame, data);
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            data.guardianReturnTicks = 0;
            return;
        }
        if (data.homeDimension == null || data.homeDimension.isBlank()) {
            data.guardianReturnTicks = 0;
            return;
        }
        if (!tame.level().dimension().location().toString().equals(data.homeDimension)) {
            data.guardianReturnTicks++;
        } else {
            BlockPos home = new BlockPos(data.homeX, data.homeY, data.homeZ);
            double distanceSqr = tame.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);
            double allowedRadius = data.guardianRelaxing ? 12.0D : 2.0D;
            if (distanceSqr <= allowedRadius * allowedRadius) {
                data.guardianReturnTicks = 0;
                return;
            }
            data.guardianReturnTicks++;
        }
        if (data.guardianReturnTicks < 1200) {
            return;
        }
        if (data.homeDimension == null || data.homeDimension.isBlank()) {
            data.guardianReturnTicks = 0;
            return;
        }
        if (!tame.level().dimension().location().toString().equals(data.homeDimension)) {
            data.guardianReturnTicks = 0;
            return;
        }
        tame.teleportTo(data.homeX + 0.5D, data.homeY, data.homeZ + 0.5D);
        tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
        tame.getNavigation().stop();
        data.guardianReturnTicks = 0;
    }

    private static void updateGuardianTargetTimeout(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !data.hasHome) {
            return;
        }
        LivingEntity target = tame.getTarget();
        if (target == null || !target.isAlive() || target.level() != tame.level()) {
            resetGuardianTargetTimeout(data);
            return;
        }
        double distanceSq = tame.distanceToSqr(target);
        if (data.guardianTargetUuid == null || !data.guardianTargetUuid.equals(target.getUUID())) {
            data.guardianTargetUuid = target.getUUID();
            data.guardianTargetStuckTicks = 0;
            data.guardianTargetBestDistanceSq = distanceSq;
            TameRegistry.markDirty();
            return;
        }
        if (distanceSq <= 16.0D) {
            data.guardianTargetStuckTicks = 0;
            data.guardianTargetBestDistanceSq = distanceSq;
            return;
        }
        if (distanceSq + 4.0D < data.guardianTargetBestDistanceSq) {
            data.guardianTargetBestDistanceSq = distanceSq;
            data.guardianTargetStuckTicks = 0;
            return;
        }
        data.guardianTargetStuckTicks++;
        if (data.guardianTargetStuckTicks < 1200) {
            return;
        }
        tame.setTarget(null);
        tame.getNavigation().stop();
        data.guardianReturnTicks = 0;
        resetGuardianTargetTimeout(data);
        TameRegistry.markDirty();
    }

    private static void resetGuardianTargetTimeout(TameData data) {
        if (data == null) {
            return;
        }
        data.guardianTargetUuid = null;
        data.guardianTargetStuckTicks = 0;
        data.guardianTargetBestDistanceSq = 0.0D;
    }

    private static void updateGuardianPhase(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !data.hasHome || tame.level().isClientSide) {
            return;
        }
        long now = tame.level().getGameTime();
        if (data.guardianNextPhaseTick <= 0L) {
            data.guardianRelaxing = false;
            data.guardianNextPhaseTick = now + randomGuardianStrictDuration(tame);
            TameRegistry.markDirty();
            return;
        }
        if (now < data.guardianNextPhaseTick) {
            return;
        }
        data.guardianRelaxing = !data.guardianRelaxing;
        data.guardianNextPhaseTick = now + (data.guardianRelaxing ? randomGuardianRelaxDuration(tame) : randomGuardianStrictDuration(tame));
        data.guardianReturnTicks = 0;
        TameRegistry.markDirty();
    }

    private static int randomGuardianStrictDuration(TamableAnimal tame) {
        return 2400 + tame.getRandom().nextInt(2401);
    }

    private static int randomGuardianRelaxDuration(TamableAnimal tame) {
        return 600 + tame.getRandom().nextInt(1801);
    }
}
