package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;

public class ArmageddonCompatUtil {

    private ArmageddonCompatUtil() {
    }

    public static boolean isValidBossTameTarget(Mob boss, LivingEntity target) {
        if (!(target instanceof TamableAnimal tame)) {
            return false;
        }
        return isValidBossTameTarget(boss, tame);
    }

    public static boolean isValidBossTameTarget(Mob boss, TamableAnimal tame) {
        return boss != null
                && tame != null
                && tame.isTame()
                && tame.isAlive()
                && !tame.isOrderedToSit()
                && !TameDuelManager.isTameInDuel(tame.getUUID())
                && !TameRegistry.isProtectedAttackTarget(tame, boss);
    }

    public static List<TamableAnimal> findNearbyBossTames(LevelAccessor level, Entity source, double radius) {
        if (!(source instanceof Mob boss)) {
            return List.of();
        }
        Vec3 center = source.position();
        return level.getEntitiesOfClass(TamableAnimal.class, new AABB(center, center).inflate(radius),
                tame -> isValidBossTameTarget(boss, tame)).stream()
                .sorted(Comparator.comparingDouble(tame -> tame.distanceToSqr(center)))
                .toList();
    }

    public static TamableAnimal findNearestBossTame(LevelAccessor level, Entity source, double radius) {
        List<TamableAnimal> tames = findNearbyBossTames(level, source, radius);
        return tames.isEmpty() ? null : tames.get(0);
    }

    public static void damageNearbyBossTames(LevelAccessor level, Entity source, double radius, float damage, double yMotion) {
        for (TamableAnimal tame : findNearbyBossTames(level, source, radius)) {
            damageBossTame(level, source, tame, damage, yMotion);
        }
    }

    public static void damageNearestBossTame(LevelAccessor level, Entity source, double radius, float damage, double yMotion) {
        TamableAnimal tame = findNearestBossTame(level, source, radius);
        if (tame != null) {
            damageBossTame(level, source, tame, damage, yMotion);
        }
    }

    public static void damageBossTame(LevelAccessor level, Entity source, TamableAnimal tame, float damage, double yMotion) {
        if (!(source instanceof Mob boss) || !isValidBossTameTarget(boss, tame)) {
            return;
        }
        tame.hurt(createMobAttackDamage(level, source), damage);
        if (yMotion != 0.0D) {
            tame.setDeltaMovement(0.0D, yMotion, 0.0D);
        }
    }

    public static boolean lookAtNearestBossTameIfNoPlayers(LevelAccessor level, Entity source, double x, double y, double z, double radius) {
        if (!(source instanceof Mob)) {
            return false;
        }
        List<net.minecraft.world.entity.player.Player> players = level.getEntitiesOfClass(
                net.minecraft.world.entity.player.Player.class,
                AABB.ofSize(new Vec3(x, y, z), radius * 2.0D, radius * 2.0D, radius * 2.0D),
                player -> true
        );
        if (!players.isEmpty()) {
            return false;
        }
        TamableAnimal tame = findNearestBossTame(level, source, radius);
        if (tame == null) {
            return false;
        }
        source.lookAt(EntityAnchorArgument.Anchor.EYES, tame.position());
        return true;
    }

    public static void spawnEntityAtNearbyBossTames(LevelAccessor level, Entity source, double radius, ResourceLocation entityId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        EntityType<?> entityType = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null) {
            return;
        }
        for (TamableAnimal tame : findNearbyBossTames(level, source, radius)) {
            Entity spawned = entityType.spawn(serverLevel, BlockPos.containing(tame.position()), MobSpawnType.MOB_SUMMONED);
            if (spawned != null) {
                spawned.moveTo(tame.getX(), tame.getY(), tame.getZ(), spawned.getYRot(), spawned.getXRot());
            }
        }
    }

    public static void repeatLightningDamageNearbyBossTames(LevelAccessor level, Entity source, double radius, int repeats, int intervalTicks, float damage) {
        List<TamableAnimal> tames = findNearbyBossTames(level, source, radius);
        for (TamableAnimal tame : tames) {
            for (int i = 1; i <= repeats; i++) {
                int delay = i * intervalTicks;
                queueArmageddonServerWork(delay, () -> {
                    if (!(source instanceof Mob boss) || !isValidBossTameTarget(boss, tame)) {
                        return;
                    }
                    damageBossTame(level, source, tame, damage, 0.0D);
                    spawnLightningAt(level, tame);
                });
            }
        }
    }

    public static void queueArmageddonServerWork(int delay, Runnable runnable) {
        try {
            Class<?> clazz = Class.forName("net.mcreator.armageddonmod.ArmageddonModMod");
            Method method = clazz.getMethod("queueServerWork", int.class, Runnable.class);
            method.invoke(null, delay, runnable);
        } catch (ReflectiveOperationException ignored) {
            runnable.run();
        }
    }

    private static DamageSource createMobAttackDamage(LevelAccessor level, Entity source) {
        return new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK),
                source
        );
    }

    private static void spawnLightningAt(LevelAccessor level, Entity target) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning == null) {
            return;
        }
        lightning.moveTo(target.getX(), target.getY(), target.getZ(), lightning.getYRot(), lightning.getXRot());
        serverLevel.addFreshEntity(lightning);
    }
}
