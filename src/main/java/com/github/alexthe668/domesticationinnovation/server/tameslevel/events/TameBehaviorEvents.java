package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TamePerformanceProfiler;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;

public class TameBehaviorEvents {
    @SubscribeEvent
    public static void onTameTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof PathfinderMob tame) || !TameEntityAdapter.isTame(tame)) return;
        if (tame.level().isClientSide) return;
        if (!tame.isAlive()) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            data = tame instanceof net.minecraft.world.entity.TamableAnimal tamable
                    ? TameSpawnEvents.registerOrRestoreTame(tamable, true) : null;
            if (data == null) {
                return;
            }
        }
        if (tame instanceof net.minecraft.world.entity.TamableAnimal tamable) TameSpawnEvents.processDeferredStatRefresh(tamable, data);
        final TameData activeData = data;
        if (TameDuelManager.isTameInDuel(tame.getUUID())) return;
        if (hasInvalidTarget(tame)) {
            tame.setTarget(null);
        }

        if (tame.tickCount % 10 == 0) {
            if (tame instanceof net.minecraft.world.entity.TamableAnimal tamable) TameCommands.syncLiveMovementStateFor(tamable);
            TamePerformanceProfiler.run("behavior.boss_movement_override", () -> handleBossMovementOverride(tame, activeData));
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
        if (TameEntityAdapter.isStayingStill(tame)) return;

        if (!(tame instanceof TamableAnimal)) {
            TamePerformanceProfiler.run("behavior.interface_mode_targeting", () -> handleInterfaceModeTargeting(tame, activeData));
        }

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
        if (!(event.getEntityMounting() instanceof PathfinderMob tame) || !TameEntityAdapter.isTame(tame)) return;
        if (TLAdminRuntimeSettings.sitOnChairsEnabled()) return;

        Entity mount = event.getEntityBeingMounted();
        ResourceLocation mountedType = ForgeRegistries.ENTITY_TYPES.getKey(mount.getType());
        if (mountedType == null) return;
        String namespace = mountedType.getNamespace();
        boolean valhelsiaNamespace = "valhelsia_structures".equals(namespace)
                || "valhelsia_furniture".equals(namespace)
                || "valhelsia_furnitures".equals(namespace);
        boolean createNamespace = "create".equals(namespace);
        if (!valhelsiaNamespace && !createNamespace) return;

        String path = mountedType.getPath();
        if (!path.contains("seat") && !path.contains("chair")) return;

        event.setCanceled(true);
        tame.stopRiding();
    }

    private static int getBehaviorScanInterval(PathfinderMob tame) {
        if (isIdleOrSitting(tame)) {
            return 40;
        }
        return hasValidCurrentTarget(tame) ? 15 : 8;
    }

    private static int getGuardianReturnInterval(PathfinderMob tame) {
        return isIdleOrSitting(tame) ? 80 : 40;
    }

    private static boolean isIdleOrSitting(PathfinderMob tame) {
        if (TameEntityAdapter.isStayingStill(tame)) {
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

    private static void handleSkeletonSpacing(PathfinderMob tame) {
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

    private static void handleGuardianMovement(PathfinderMob tame, TameData data) {
        updateGuardianPhase(tame, data);
        if (data.guardianRelaxing) {
            handleGuardianRelaxedWander(tame, data);
        } else {
            handleGuardianReturn(tame, data);
        }
    }

    private static void handleGuardianReturn(PathfinderMob tame, TameData data) {
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

    private static void handleGuardianRelaxedWander(PathfinderMob tame, TameData data) {
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

    private static void handleCloseOwner(PathfinderMob tame, TameData data) {
        if (data == null || !data.closeMovement || data.hasHome || TameEntityAdapter.isStayingStill(tame)) {
            return;
        }
        if (tame.getTarget() != null && tame.getTarget().isAlive()) {
            return;
        }
        if (!(TameEntityAdapter.owner(tame) instanceof ServerPlayer owner)) {
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
            TameCommands.queueClientReloadForTame(tame);
            return;
        }
        if (distanceSqr > 2.5D * 2.5D) {
            tame.getNavigation().moveTo(owner, 1.15D);
        }
    }

    private static void handleBossMovementOverride(PathfinderMob tame, TameData data) {
        if (tame == null || data == null || !(tame.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (TameMode.byId(data.mode) != TameMode.BOSS) {
            return;
        }
        boolean hasBossTarget = (tame instanceof net.minecraft.world.entity.TamableAnimal tamable
                && TameGoalSupport.hasSharedBossTarget(serverLevel, tamable))
                || hasValidCurrentTarget(tame);
        int desiredOrderCode = hasBossTarget ? 2 : 0;
        if (data.movementOrder == desiredOrderCode) {
            return;
        }
        TameCommands.applyMovementOrderCode(tame, desiredOrderCode);
    }

    private static boolean hasInvalidTarget(PathfinderMob tame) {
        LivingEntity target = tame.getTarget();
        if (target == null) {
            return false;
        }
        if (isMutantCreeperMinionExploding(tame)) {
            return false;
        }
        return !isValidCombatTarget(tame, target);
    }

    private static void handleInterfaceModeTargeting(PathfinderMob tame, TameData data) {
        if (tame == null || data == null || tame.getTarget() != null) return;
        TameMode mode = TameMode.byId(data.mode);
        if (mode == TameMode.DEFAULT || mode == TameMode.DEFAULT_PLUS || mode == TameMode.PASSIVE) return;
        LivingEntity owner = TameEntityAdapter.owner(tame);
        LivingEntity priority = owner == null ? null : owner.getLastHurtByMob();
        if (priority == null && owner != null) priority = owner.getLastHurtMob();
        if (priority != null && priority.isAlive() && isValidCombatTarget(tame, priority)) {
            tame.setTarget(priority);
            return;
        }
        double radius = mode == TameMode.BODYGUARD ? Math.max(4, data.bodyguardRange) : 24.0D;
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : tame.level().getEntitiesOfClass(LivingEntity.class, tame.getBoundingBox().inflate(radius))) {
            if (!isValidCombatTarget(tame, candidate)) continue;
            if ((mode == TameMode.MONSTER_HUNTER || mode == TameMode.BODYGUARD || mode == TameMode.BOSS)
                    && !(candidate instanceof Monster)) continue;
            double distance = tame.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        if (best != null) tame.setTarget(best);
    }

    private static boolean hasValidCurrentTarget(PathfinderMob tame) {
        LivingEntity target = tame.getTarget();
        return target != null && isValidCombatTarget(tame, target);
    }

    private static boolean shouldSwitchTarget(PathfinderMob tame, LivingEntity candidate) {
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

    private static boolean isValidCombatTarget(PathfinderMob tame, LivingEntity target) {
        return isValidCombatTarget(tame, target, tame == null ? null : tame.level());
    }

    private static boolean isValidCombatTarget(PathfinderMob tame, LivingEntity target, net.minecraft.world.level.Level sourceLevel) {
        if (tame == null || target == null) return false;
        if (!target.isAlive()) return false;
        if (target == tame) return false;
        if (sourceLevel != null && target.level() != sourceLevel) return false;
        if (target instanceof Player) return false;
        if (TameEntityAdapter.isTame(target)) return false;
        if (TameRegistry.isProtectedAttackTarget(TameEntityAdapter.ownerUuid(tame), target)) return false;
        return true;
    }

    private static void updateGuardianReturnTimer(PathfinderMob tame, TameData data) {
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
        TameCommands.queueClientReloadForTame(tame);
        data.guardianReturnTicks = 0;
    }

    private static void updateGuardianTargetTimeout(PathfinderMob tame, TameData data) {
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

    private static void updateGuardianPhase(PathfinderMob tame, TameData data) {
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

    private static int randomGuardianStrictDuration(PathfinderMob tame) {
        return 2400 + tame.getRandom().nextInt(2401);
    }

    private static int randomGuardianRelaxDuration(PathfinderMob tame) {
        return 600 + tame.getRandom().nextInt(1801);
    }

    private static boolean isMutantCreeperMinionExploding(PathfinderMob tame) {
        if (!isMutantCreeperMinion(tame)) {
            return false;
        }
        try {
            Method hasIgnited = tame.getClass().getMethod("hasIgnited");
            Object ignited = hasIgnited.invoke(tame);
            if (ignited instanceof Boolean flag && flag) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method getExplodeState = tame.getClass().getMethod("getExplodeState");
            Object state = getExplodeState.invoke(tame);
            return state instanceof Number number && number.intValue() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isMutantCreeperMinion(PathfinderMob tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null
                && "mutantmonsters".equals(key.getNamespace())
                && "creeper_minion".equals(key.getPath());
    }
}
