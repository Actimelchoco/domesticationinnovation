package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
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
        if (tame.isOrderedToSit()) return;
        if (tame.tickCount % 20 != 0) return;

        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) return;
        if (TameDuelManager.isTameInDuel(tame.getUUID())) return;

        TameMode mode = TameMode.byId(data.mode);
        if (mode == TameMode.MONSTER_HUNTER) {
            LivingEntity nearest = findNearestMonster(tame, 10.0D);
            if (nearest != null) {
                tame.setTarget(nearest);
            }
            return;
        }
        if (mode == TameMode.AGGRESSIVE) {
            LivingEntity nearest = findNearestAggressiveTarget(tame, 10.0D);
            if (nearest != null) {
                tame.setTarget(nearest);
            }
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
            applyRetargetByMode(tame, data, victim, attacker);
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
            applyRetargetByMode(tame, data, owner, victim);
        }
    }

    private static void applyRetargetByMode(TamableAnimal tame, TameData data, ServerPlayer owner, LivingEntity ownerCombatTarget) {
        if (ownerCombatTarget == null || !ownerCombatTarget.isAlive()) return;
        if (TameDuelManager.isTameInDuel(tame.getUUID())) return;
        if (ownerCombatTarget instanceof TamableAnimal otherTame && otherTame.isTame()) return;
        TameMode mode = TameMode.byId(data.mode);
        if (mode == TameMode.PASSIVE) return;

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
            case MONSTER_HUNTER, AGGRESSIVE -> {
                if (tame.distanceTo(ownerCombatTarget) <= 7.0D) {
                    tame.setTarget(ownerCombatTarget);
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
        for (Monster monster : owner.serverLevel().getEntitiesOfClass(Monster.class, box)) {
            if (!monster.isAlive()) continue;
            double hp = monster.getMaxHealth();
            if (hp > bestHp) {
                bestHp = hp;
                best = monster;
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

    private static LivingEntity findNearestMonster(TamableAnimal tame, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = tame.getBoundingBox().inflate(radius);
        for (Monster monster : tame.level().getEntitiesOfClass(Monster.class, box)) {
            if (!monster.isAlive()) continue;
            double d2 = monster.distanceToSqr(tame);
            if (d2 < bestDist) {
                bestDist = d2;
                best = monster;
            }
        }
        return best;
    }

    private static LivingEntity findNearestAggressiveTarget(TamableAnimal tame, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        AABB box = tame.getBoundingBox().inflate(radius);
        for (LivingEntity entity : tame.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (!entity.isAlive()) continue;
            if (entity == tame) continue;
            if (entity instanceof Player) continue;
            if (entity instanceof TamableAnimal otherTame && otherTame.isTame()) continue;
            if (entity instanceof TamableAnimal otherTame && otherTame.getOwnerUUID() != null && otherTame.getOwnerUUID().equals(tame.getOwnerUUID())) continue;
            double d2 = entity.distanceToSqr(tame);
            if (d2 < bestDist) {
                bestDist = d2;
                best = entity;
            }
        }
        return best;
    }
}
