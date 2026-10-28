package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Temporary combat orders; never overwrite the owner's saved mode or movement. */
@Mod.EventBusSubscriber(modid="domesticationinnovation")
public final class BodyguardWhipOrders {
    private BodyguardWhipOrders() { }
    private static final TagKey<net.minecraft.world.item.Item> WHIPS = TagKey.create(Registries.ITEM,new ResourceLocation("forge","tools/whips"));

    public static boolean isWhip(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.is(WHIPS)) return true;
        ResourceLocation id=ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id!=null && (id.getPath().equals("whip") || id.getPath().endsWith("_whip"));
    }
    public static boolean active(TameData data) {
        return data!=null && data.rosterBodyguard && data.bodyguardWhipTarget!=null && !data.dead && !data.stored
                && !TameDuelManager.isEntityInDuel(data.uuid) && !TameDuelManager.isEntityInDuel(data.ownerUUID)
                && !DuelFollowerPause.shouldWait(data);
    }
    public static boolean matches(TameData data, LivingEntity entity) {
        return active(data) && entity!=null && entity.isAlive() && data.bodyguardWhipTarget.equals(entity.getUUID());
    }
    public static LivingEntity target(ServerLevel level,TameData data) {
        if (!active(data)) return null;
        for (ServerLevel world:level.getServer().getAllLevels()) {
            Entity entity=world.getEntity(data.bodyguardWhipTarget);
            if (!(entity instanceof LivingEntity living)) continue;
            if (!living.isAlive()) { clear(data,false,level.getServer());return null; }
            if (protectedTarget(data.ownerUUID,living)) {
                clear(data,false,level.getServer());return null;
            }
            return living.level()==level ? living : null;
        }
        // Missing chunks are not evidence of death. Keep the UUID until it loads.
        return null;
    }
    public static boolean holdsPosition(LivingEntity body) {
        if (!(body.level() instanceof ServerLevel level)) return false;
        return target(level,TameRegistry.get(body.getUUID()))!=null;
    }
    private static boolean protectedTarget(UUID owner,LivingEntity target) {
        if (TameRegistry.isProtectedAttackTarget(owner,target) || owner.equals(target.getUUID())
                || owner.equals(TameEntityAdapter.ownerUuid(target))) return true;
        if (target instanceof net.minecraft.world.entity.player.Player)
            return PlayerDebugSettings.tamesFriendly(owner)
                    || !com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameProtectionEvents.ownersMayFight(owner,target.getUUID());
        return TameEntityAdapter.isTame(target)
                && !com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameProtectionEvents.ownersMayFight(owner,TameEntityAdapter.ownerUuid(target));
    }
    public static int assign(ServerPlayer owner,LivingEntity victim) {
        if (!victim.isAlive() || victim==owner || victim.isSpectator()
                || TameDuelManager.isEntityInDuel(owner.getUUID()) || TameDuelManager.isEntityInDuel(victim.getUUID())
                || protectedTarget(owner.getUUID(),victim)) return 0;
        int count=0;
        for (TameData data:BodyguardService.members(BodyguardService.roster(owner))) {
            if (data.dead || data.stored || TameDuelManager.isEntityInDuel(data.uuid)) continue;
            data.bodyguardWhipTarget=victim.getUUID();count++;
            LivingEntity body=LoadedTameIndex.find(owner.getServer(),data.uuid,data.tlId);
            if (body instanceof Mob mob) enforce(mob,data);
        }
        if (count>0) TameRegistry.markDirty();
        return count;
    }
    public static void clear(TameData data,boolean all,net.minecraft.server.MinecraftServer server) {
        UUID old=data.bodyguardWhipTarget;data.bodyguardWhipTarget=null;
        LivingEntity body=LoadedTameIndex.find(server,data.uuid,data.tlId);
        if (body instanceof Mob mob) {
            LivingEntity current=mob.getTarget();
            if (all || current!=null && current.getUUID().equals(old)) {
                mob.setTarget(null);mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);mob.getNavigation().stop();
            }
            if (all || mob.getLastHurtMob()!=null && mob.getLastHurtMob().getUUID().equals(old)) mob.setLastHurtMob(null);
            if (all || mob.getLastHurtByMob()!=null && mob.getLastHurtByMob().getUUID().equals(old)) mob.setLastHurtByMob(null);
        }
        TameRegistry.markDirty();
    }
    public static int clear(ServerPlayer owner,boolean all) {
        int count=0;
        for (TameData data:BodyguardService.members(BodyguardService.roster(owner))) {
            if (TameDuelManager.isEntityInDuel(data.uuid)) continue;
            UUID old=data.bodyguardWhipTarget;
            if (!all && old==null) continue;
            if (owner.getLastHurtMob()!=null && (all || owner.getLastHurtMob().getUUID().equals(old))) owner.setLastHurtMob(null);
            if (all || old!=null && legacyTargetMatches(owner,old))
                com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport.clearAssasinTarget(owner.getUUID());
            clear(data,all,owner.getServer());count++;
        }
        if (all) owner.setLastHurtByMob(null);
        return count;
    }
    private static boolean legacyTargetMatches(ServerPlayer owner,UUID id) {
        for (ServerLevel world:owner.getServer().getAllLevels()) {
            if (world.getEntity(id) instanceof LivingEntity target)
                return com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport.isAssasinTarget(owner.getUUID(),target);
        }
        return false;
    }
    private static void enforce(Mob mob,TameData data) {
        if (!(mob.level() instanceof ServerLevel level) || !active(data)
                || com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.isHungerBlockingMovement(mob)) return;
        LivingEntity target=target(level,data);
        if (target!=null && !TameRegistry.isProtectedAttackTarget(mob,target)) {
            if (mob.getTarget()!=target) mob.setTarget(target);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide) enforce(mob,TameRegistry.get(mob.getUUID()));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingDamageEvent event) {
        if (event.isCanceled() || event.getAmount()<=0 || !(event.getSource().getEntity() instanceof ServerPlayer owner)) return;
        Entity direct=event.getSource().getDirectEntity();
        ResourceLocation id=direct==null?null:ForgeRegistries.ENTITY_TYPES.getKey(direct.getType());
        boolean whipEntity=id!=null && (id.getPath().endsWith("_whip") || id.getPath().endsWith("_whip_proj"));
        if ((direct==owner && isWhip(owner.getMainHandItem())) || whipEntity) assign(owner,event.getEntity());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void died(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        UUID victim=event.getEntity().getUUID();
        for (PlayerDuelStats roster:TameRegistry.getPlayerDuelStats().values())
            for (TameData data:BodyguardService.members(roster))
                if (victim.equals(data.bodyguardWhipTarget)) clear(data,false,event.getEntity().getServer());
    }
    @SubscribeEvent(priority=EventPriority.HIGH,receiveCanceled=true)
    public static void click(PlayerInteractEvent.RightClickItem event) { clearClick(event); }
    @SubscribeEvent(priority=EventPriority.HIGH,receiveCanceled=true)
    public static void clickBlock(PlayerInteractEvent.RightClickBlock event) { clearClick(event); }
    @SubscribeEvent(priority=EventPriority.HIGH,receiveCanceled=true)
    public static void clickEntity(PlayerInteractEvent.EntityInteract event) { clearClick(event); }
    @SubscribeEvent(priority=EventPriority.HIGH,receiveCanceled=true)
    public static void clickSpecific(PlayerInteractEvent.EntityInteractSpecific event) { clearClick(event); }
    private static void clearClick(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer owner) || !isWhip(event.getItemStack())
                || TameDuelManager.isEntityInDuel(owner.getUUID()) || BodyguardService.roster(owner).bodyguards.isEmpty()) return;
        int count=clear(owner,owner.isShiftKeyDown());
        if (count>0) owner.displayClientMessage(Component.literal(owner.isShiftKeyDown()?"All bodyguard targets cleared.":"Bodyguard assassination target cleared."),true);
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
