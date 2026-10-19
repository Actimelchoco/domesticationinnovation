package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.mixin.ProjectileOwnerAccessor;
import com.github.alexthe668.domesticationinnovation.server.entity.ChainLightningEntity;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Event-driven protection shared by players, owned mobs and attributed indirect attacks. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class FriendlyOwnerProtection {
    private FriendlyOwnerProtection() {}
    public static boolean friendly(UUID first, UUID second) {
        return first != null && second != null && !first.equals(second)
                && (PlayerDebugSettings.isDoNotAttackOwner(first, second) || PlayerDebugSettings.isDoNotAttackOwner(second, first));
    }
    public static UUID owner(Entity entity) {
        for (int depth = 0; entity != null && depth < 8; depth++) {
            if (entity instanceof Player player) return player.getUUID();
            UUID tameOwner = TameEntityAdapter.ownerUuid(entity);
            if (tameOwner != null && TameEntityAdapter.isTame(entity)) return tameOwner;
            if (entity instanceof OwnableEntity owned && owned.getOwnerUUID() != null) return owned.getOwnerUUID();
            Entity next = entity instanceof Projectile projectile ? projectile.getOwner()
                    : entity instanceof PrimedTnt tnt ? tnt.getOwner()
                    : entity instanceof EvokerFangs fangs ? fangs.getOwner()
                    : entity instanceof AreaEffectCloud cloud ? cloud.getOwner()
                    : entity instanceof LightningBolt bolt ? bolt.getCause()
                    : entity instanceof ChainLightningEntity lightning ? lightning.getCreatorEntity() : null;
            if (next == null && entity instanceof ProjectileOwnerAccessor projectile) {
                UUID id = projectile.tl$getOwnerUUID();
                if (id != null) {
                    TameData data = TameRegistry.get(id);
                    return data == null ? id : data.ownerUUID;
                }
            }
            if (next == null) {
                TameData data = TameRegistry.get(entity.getUUID());
                return data == null ? null : data.ownerUUID;
            }
            if (next == entity) return null;
            entity = next;
        }
        return null;
    }
    public static boolean blocks(Entity target, Entity source) {
        if (target == null || target.level().isClientSide || source == null) return false;
        UUID victimOwner = owner(target);
        return victimOwner != null && friendly(owner(source), victimOwner);
    }
    public static boolean blocks(Entity target, DamageSource source) {
        return source != null && (blocks(target, source.getEntity()) || blocks(target, source.getDirectEntity()));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(LivingAttackEvent event) { if (blocks(event.getEntity(), event.getSource())) event.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hurt(LivingHurtEvent event) { if (blocks(event.getEntity(), event.getSource())) event.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void damage(LivingDamageEvent event) { if (blocks(event.getEntity(), event.getSource())) event.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void melee(AttackEntityEvent event) { if (blocks(event.getTarget(), event.getEntity())) event.setCanceled(true); }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void impact(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult hit && blocks(hit.getEntity(), event.getProjectile())) event.setCanceled(true);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Detonate event) {
        if (!event.getLevel().isClientSide) event.getAffectedEntities().removeIf(target -> blocks(target, event.getExplosion().getDamageSource()));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(LivingChangeTargetEvent event) {
        if (blocks(event.getNewTarget(), event.getEntity())) event.setNewTarget(null);
    }
}
