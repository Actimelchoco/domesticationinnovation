package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.EnumSet;

/** Slow navigation for passive Critters pets that receive combat targets from tame modes. */
public final class CrittersSlowChaseGoal extends Goal {
    // These species already have low native movement attributes (snail: 0.1).
    public static final double SPEED = 1.0D;
    private static final double STOP_DISTANCE_SQR = 2.25D;
    private static final int PATH_INTERVAL = 20;
    private final TamableAnimal pet;
    private LivingEntity pathTarget;
    private Vec3 pathTargetPosition;
    private int nextPathTick;

    private CrittersSlowChaseGoal(TamableAnimal pet) {
        this.pet = pet;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static void ensure(TamableAnimal pet) {
        if (pet.level().isClientSide || !pet.isTame()) return;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(pet.getType());
        if (id == null || !id.getNamespace().equals("crittersandcompanions")
                || !(id.getPath().equals("roly_poly") || id.getPath().equals("snail"))) return;
        if (pet.goalSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof CrittersSlowChaseGoal)) return;
        // Float, an open rolypoly chest, and native panic retain higher priority.
        pet.goalSelector.addGoal(3, new CrittersSlowChaseGoal(pet));
    }

    @Override public boolean canUse() {
        LivingEntity target = pet.getTarget();
        if (!pet.isTame() || !pet.isAlive() || pet.isNoAi() || pet.isPassenger() || pet.isVehicle()
                || pet.isLeashed() || pet.isOrderedToSit() || pet.isInSittingPose()
                || DuelFollowerPause.isPaused(pet) || target == null || target == pet
                || !target.isAlive() || target.isRemoved() || target.level() != pet.level()) return false;
        TameData data = TameRegistry.get(pet.getUUID());
        if (data == null || data.dead || data.stored || data.guardianResting || data.hungerForcedSit
                || TameCommands.isHungerBlockingAbilities(data)) return false;
        if (TameDuelManager.isEntityInDuel(pet.getUUID())) {
            return TameDuelManager.areDuelOpponents(pet.getUUID(), target.getUUID());
        }
        if (TameCommands.isDuelSessionLocked(pet.getUUID()) || data.movementOrder == 1
                || TameMode.forData(data) == TameMode.PASSIVE || pet.isAlliedTo(target)
                || TameRegistry.isProtectedAttackTarget(pet, target) || FriendlyOwnerProtection.blocks(target, pet)
                || target instanceof Player && PlayerDebugSettings.tamesFriendly(data.ownerUUID)) return false;
        if (data.hasProtectionZone) {
            if (!pet.level().dimension().location().toString().equals(data.protectionDimension)) return false;
            double x = data.protectionX + .5, y = data.protectionY, z = data.protectionZ + .5;
            double radius = data.protectionRadius;
            if (target.distanceToSqr(x, y, z) > radius * radius
                    || pet.distanceToSqr(x, y, z) > (radius + 6) * (radius + 6)) return false;
        }
        return true;
    }

    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { pathTarget = null; pathTargetPosition = null; nextPathTick = 0; }
    @Override public void stop() { pet.getNavigation().stop(); pathTarget = null; pathTargetPosition = null; nextPathTick = 0; }

    @Override public void tick() {
        if (!canUse()) { stop(); return; }
        LivingEntity target = pet.getTarget();
        pet.getLookControl().setLookAt(target, 30, 30);
        if (pet.distanceToSqr(target) <= STOP_DISTANCE_SQR) {
            pet.getNavigation().stop();
            nextPathTick = 0;
            return;
        }
        if (target != pathTarget || pet.tickCount >= nextPathTick) {
            nextPathTick = pet.tickCount + PATH_INTERVAL;
            // Preserve progress and avoid unnecessary path searches while the
            // current route to a stationary target is still usable.
            if (target != pathTarget || pathTargetPosition == null || pet.getNavigation().isDone()
                    || target.position().distanceToSqr(pathTargetPosition) >= 1.0D) {
                pathTarget = target;
                pathTargetPosition = target.position();
                pet.getNavigation().moveTo(target, SPEED);
            }
        }
    }
}
