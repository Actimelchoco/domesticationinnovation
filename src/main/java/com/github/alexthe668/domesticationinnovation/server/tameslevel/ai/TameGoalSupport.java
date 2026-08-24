package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class TameGoalSupport {
    private static final Map<BossTargetKey, UUID> SHARED_BOSS_TARGETS = new HashMap<>();

    private record BossTargetKey(UUID ownerUuid, String dimensionId) {
    }

    private TameGoalSupport() {
    }

    static TameData data(TamableAnimal tame) {
        return TameRegistry.get(tame.getUUID());
    }

    static ServerPlayer owner(TamableAnimal tame) {
        return tame.getOwner() instanceof ServerPlayer p ? p : null;
    }

    static boolean handleProtectionZone(ServerLevel level, TamableAnimal tame, TameData data) {
        if (!level.dimension().location().toString().equals(data.protectionDimension)) return false;

        double centerX = data.protectionX + 0.5D;
        double centerY = data.protectionY;
        double centerZ = data.protectionZ + 0.5D;

        LivingEntity target = findBestHostile(level, centerX, centerY, centerZ, data.protectionRadius, false);
        if (target != null) {
            tame.setTarget(target);
            if (tame.distanceToSqr(centerX, centerY, centerZ) > Math.pow(data.protectionRadius + 6, 2)) {
                tame.getNavigation().moveTo(centerX, centerY, centerZ, 1.2D);
            }
            return true;
        }

        if (tame.distanceToSqr(centerX, centerY, centerZ) > Math.pow(data.protectionRadius, 2)) {
            tame.getNavigation().moveTo(centerX, centerY, centerZ, 1.1D);
            return true;
        }
        return false;
    }

    static void setBossTarget(ServerLevel level, TamableAnimal tame, ServerPlayer owner) {
        BossTargetKey key = bossTargetKey(level, tame, owner);
        if (key == null) {
            return;
        }

        double x = owner != null ? owner.getX() : tame.getX();
        double y = owner != null ? owner.getY() : tame.getY();
        double z = owner != null ? owner.getZ() : tame.getZ();
        LivingEntity locked = resolveSharedBossTarget(level, key);
        LivingEntity current = isValidBossTarget(level, tame.getTarget()) ? tame.getTarget() : null;
        LivingEntity target = locked != null ? locked : current;
        LivingEntity candidate = findBestHostile(level, x, y, z, 96.0D, true);
        if (target == null || !isValidBossTarget(level, target)) {
            target = candidate;
        } else if (candidate != null && candidate != target && candidate.getMaxHealth() > target.getMaxHealth()) {
            target = candidate;
        }

        if (target == null || !isValidBossTarget(level, target)) {
            SHARED_BOSS_TARGETS.remove(key);
            tame.setTarget(null);
            return;
        }

        SHARED_BOSS_TARGETS.put(key, target.getUUID());
        if (tame.getTarget() != target) {
            tame.setTarget(target);
        }
    }

    public static boolean hasSharedBossTarget(ServerLevel level, TamableAnimal tame) {
        if (level == null || tame == null) {
            return false;
        }
        return resolveSharedBossTarget(level, bossTargetKey(level, tame, owner(tame))) != null;
    }

    static void setBodyguardTarget(ServerLevel level, TamableAnimal tame, ServerPlayer owner, double aggroRadius, double leashDistance) {
        if (tame.distanceTo(owner) > leashDistance) {
            tame.setTarget(null);
            tame.getNavigation().moveTo(owner, 1.25D);
            return;
        }

        LivingEntity current = tame.getTarget();
        if (current != null && current.isAlive()) {
            return;
        }

        LivingEntity target = prioritizeOwnerCombatTarget(owner, tame);
        if (target == null) {
            // Passive bodyguard sweep for close threats hugging the owner.
            target = findNearestHostile(level, owner.getX(), owner.getY(), owner.getZ(), 5.0D);
        }
        if (target == null) {
            target = findBestHostile(level, owner.getX(), owner.getY(), owner.getZ(), aggroRadius, false);
        }
        if (target != null) tame.setTarget(target);
    }

    private static LivingEntity prioritizeOwnerCombatTarget(ServerPlayer owner, TamableAnimal tame) {
        if (owner == null) return null;

        LivingEntity attacker = owner.getLastHurtByMob();
        if (isValidBodyguardTarget(attacker, owner, tame)) {
            return attacker;
        }

        LivingEntity attacked = owner.getLastHurtMob();
        if (isValidBodyguardTarget(attacked, owner, tame)) {
            return attacked;
        }
        return null;
    }

    private static boolean isValidBodyguardTarget(LivingEntity target, ServerPlayer owner, TamableAnimal tame) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        if (target == owner || target == tame) return false;
        if (target instanceof net.minecraft.world.entity.player.Player player && player.isCreative()) return false;
        if (target instanceof TamableAnimal otherTame && otherTame.isTame() && owner.getUUID().equals(otherTame.getOwnerUUID())) return false;
        return true;
    }

    static void setHunterTarget(ServerLevel level, TamableAnimal tame, double huntRadius) {
        LivingEntity current = tame.getTarget();
        if (current != null && current.isAlive()) {
            return;
        }
        LivingEntity target = findNearestHostile(level, tame.getX(), tame.getY(), tame.getZ(), huntRadius);
        if (target != null) tame.setTarget(target);
    }

    static LivingEntity nearestHunterHostile(ServerLevel level, TamableAnimal tame, double huntRadius) {
        if (level == null || tame == null) return null;
        return findNearestHostile(level, tame.getX(), tame.getY(), tame.getZ(), huntRadius);
    }

    static void setAggressiveTarget(ServerLevel level, TamableAnimal tame, double huntRadius) {
        LivingEntity current = tame.getTarget();
        if (current != null && current.isAlive()) {
            return;
        }
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = new AABB(
                tame.getX() - huntRadius, tame.getY() - huntRadius, tame.getZ() - huntRadius,
                tame.getX() + huntRadius, tame.getY() + huntRadius, tame.getZ() + huntRadius
        );
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive()) continue;
            if (entity == tame) continue;
            if (entity instanceof net.minecraft.world.entity.player.Player) continue;
            if (entity instanceof TamableAnimal otherTame && otherTame.isTame()) continue;
            double d2 = entity.distanceToSqr(tame);
            if (d2 < bestDist) {
                bestDist = d2;
                best = entity;
            }
        }
        if (best != null) {
            tame.setTarget(best);
        }
    }

    static void setDistantBodyguardTarget(ServerLevel level, TamableAnimal tame, ServerPlayer owner) {
        double dist = tame.distanceTo(owner);
        if (dist < 8.0D) {
            double dx = tame.getX() - owner.getX();
            double dz = tame.getZ() - owner.getZ();
            double len = Math.max(0.01D, Math.sqrt(dx * dx + dz * dz));
            tame.getNavigation().moveTo(owner.getX() + (dx / len) * 14.0D, owner.getY(), owner.getZ() + (dz / len) * 14.0D, 1.2D);
        } else if (dist > 30.0D) {
            tame.getNavigation().moveTo(owner, 1.25D);
        }

        LivingEntity target = findPreferredForDistant(level, owner.getX(), owner.getY(), owner.getZ(), 30.0D);
        if (target != null) tame.setTarget(target);
    }

    private static LivingEntity findPreferredForDistant(ServerLevel level, double x, double y, double z, double radius) {
        LivingEntity skeleton = null;
        LivingEntity fallback = null;
        double bestSkeleton = Double.MAX_VALUE;
        double bestFallback = Double.MAX_VALUE;

        for (Monster monster : level.getEntitiesOfClass(Monster.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius))) {
            if (!monster.isAlive()) continue;
            double d2 = monster.distanceToSqr(x, y, z);
            if (monster instanceof Skeleton && d2 < bestSkeleton) {
                bestSkeleton = d2;
                skeleton = monster;
            }
            if (d2 < bestFallback) {
                bestFallback = d2;
                fallback = monster;
            }
        }
        return skeleton != null ? skeleton : fallback;
    }

    private static LivingEntity findNearestHostile(ServerLevel level, double x, double y, double z, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius), TameGoalSupport::isHostileTarget)) {
            double d2 = living.distanceToSqr(x, y, z);
            if (d2 < bestDist) {
                bestDist = d2;
                best = living;
            }
        }
        return best;
    }

    private static LivingEntity findBestHostile(ServerLevel level, double x, double y, double z, double radius, boolean preferHighHealth) {
        LivingEntity best = null;
        double bestScore = -Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius), TameGoalSupport::isHostileTarget)) {
            double score = priorityScore(living);
            if (preferHighHealth) score += living.getMaxHealth() * 8.0D;
            else score -= Math.sqrt(living.distanceToSqr(x, y, z)) * 0.2D;
            if (score > bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }

    private static double priorityScore(LivingEntity living) {
        double score = living.getMaxHealth();
        if (living.getMaxHealth() > 70.0F) score += 1000.0D;
        if (living instanceof Skeleton) score += 800.0D;
        else if (living instanceof Creeper) score += 700.0D;
        else score += 500.0D;
        return score;
    }

    private static boolean isHostileTarget(LivingEntity living) {
        if (living == null || !living.isAlive()) return false;
        if (!(living instanceof Enemy)) return false;
        if (living instanceof TamableAnimal tame && tame.isTame()) return false;
        return !(living instanceof net.minecraft.world.entity.player.Player);
    }

    static boolean isHunterHostile(LivingEntity living) {
        return isHostileTarget(living);
    }

    private static BossTargetKey bossTargetKey(ServerLevel level, TamableAnimal tame, ServerPlayer owner) {
        if (level == null || tame == null) {
            return null;
        }
        UUID ownerUuid = owner != null ? owner.getUUID() : tame.getOwnerUUID();
        if (ownerUuid == null) {
            return null;
        }
        return new BossTargetKey(ownerUuid, level.dimension().location().toString());
    }

    private static LivingEntity resolveSharedBossTarget(ServerLevel level, BossTargetKey key) {
        if (level == null || key == null) {
            return null;
        }
        UUID targetUuid = SHARED_BOSS_TARGETS.get(key);
        if (targetUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(targetUuid);
        if (!(entity instanceof LivingEntity living) || !isValidBossTarget(level, living)) {
            SHARED_BOSS_TARGETS.remove(key);
            return null;
        }
        return living;
    }

    private static boolean isValidBossTarget(ServerLevel level, LivingEntity target) {
        return target != null && target.level() == level && isHostileTarget(target);
    }
}
