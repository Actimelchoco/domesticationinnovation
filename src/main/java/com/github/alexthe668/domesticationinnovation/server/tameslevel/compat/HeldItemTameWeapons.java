package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameCombatEvents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Shared hand-only equipment; new pickup types require an explicit capability tag. */
@Mod.EventBusSubscriber(modid="domesticationinnovation")
public final class HeldItemTameWeapons {
    private HeldItemTameWeapons() { }
    private record Retrieval(UUID tame, long tick) { }
    private static final Map<Player, Retrieval> RECENT_RETRIEVALS = new WeakHashMap<>();

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void retrieveSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        retrieve(event, event.getTarget());
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void retrieveInteraction(PlayerInteractEvent.EntityInteract event) {
        retrieve(event, event.getTarget());
    }
    public static final TagKey<EntityType<?>> PICKUP_TYPES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("domesticationinnovation","held_item_weapon_tames"));
    public static final TagKey<EntityType<?>> EXCLUDED_TYPES=TagKey.create(Registries.ENTITY_TYPE,new ResourceLocation("domesticationinnovation","held_item_weapon_excluded_tames"));
    private static final Map<Mob,Boolean> INSTALLED=new WeakHashMap<>();

    public static boolean isWeapon(ItemStack stack) { return !stack.isEmpty() && (stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem); }
    /** Equipment surcharge applies to held weapons, including native ranged weapons. */
    public static double weaponSaturationMultiplier(LivingEntity tame) {
        if (tame == null || AnimightEquipmentEvents.isAnimight(tame) || ownerUuid(tame) == null
                || TameEntityAdapter.isSupported(tame) && !TameEntityAdapter.isTame(tame)) return 1.0D;
        return isSaturationWeapon(tame.getMainHandItem()) || isSaturationWeapon(tame.getOffhandItem()) ? 1.5D : 1.0D;
    }
    private static boolean isSaturationWeapon(ItemStack stack) {
        return isWeapon(stack) || !stack.isEmpty()
                && (stack.getItem() instanceof ProjectileWeaponItem || stack.getItem() instanceof TridentItem);
    }
    public static UUID ownerUuid(LivingEntity entity) {
        UUID owner=TameEntityAdapter.ownerUuid(entity);
        return owner!=null?owner:entity instanceof OwnableEntity owned?owned.getOwnerUUID():null;
    }
    public static boolean supportsHeldItems(LivingEntity entity) {
        if (!(entity instanceof Mob) || entity.getType().is(EXCLUDED_TYPES)) return false;
        if (TameEntityAdapter.isSupported(entity)) { if(!TameEntityAdapter.isTame(entity))return false; }
        else if (!(entity instanceof OwnableEntity) || ownerUuid(entity)==null) return false;
        var id=ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        // Animights retain their independent reserve inventory and durability rules.
        return id!=null && !id.getNamespace().equals("animights");
    }
    /** Enumerate declared support without spawning entities or guessing from empty equipment slots. */
    public static List<EntityType<?>> knownWeaponPickupTypes() {
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .filter(type->AnimightEquipmentEvents.supportsWeaponType(type)
                        || type.is(PICKUP_TYPES)&&!type.is(EXCLUDED_TYPES)
                        && !ForgeRegistries.ENTITY_TYPES.getKey(type).getNamespace().equals("animights"))
                .sorted(Comparator.comparing(type->ForgeRegistries.ENTITY_TYPES.getKey(type).toString()))
                .toList();
    }
    public static boolean supportsPickup(Mob mob) { return supportsHeldItems(mob) && mob.getType().is(PICKUP_TYPES); }
    public static boolean isArmed(LivingEntity entity) { return supportsHeldItems(entity) && isWeapon(entity.getMainHandItem()); }
    public static boolean protectHeldItem(LivingEntity entity) {
        return supportsHeldItems(entity) && (isWeapon(entity.getMainHandItem()) || AnimightEquipmentEvents.isSealedItem(entity.getMainHandItem()));
    }
    public static boolean canRetrieve(Player player,InteractionHand hand,Entity target) {
        return hand==InteractionHand.MAIN_HAND && player.isSprinting() && !player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty() && target instanceof LivingEntity tame && supportsHeldItems(tame)
                && tame.isAlive() && player.getUUID().equals(ownerUuid(tame)) && !tame.getMainHandItem().isEmpty()
                && !TameDuelManager.isEntityInDuel(player.getUUID()) && !TameDuelManager.isEntityInDuel(tame.getUUID());
    }
    public static boolean retrieve(PlayerInteractEvent event,Entity target) {
        var player=event.getEntity();
        var recent=RECENT_RETRIEVALS.get(player);
        if(recent!=null){
            long elapsed=player.level().getGameTime()-recent.tick;
            if(elapsed<0 || elapsed>1) RECENT_RETRIEVALS.remove(player);
            else if(target.getUUID().equals(recent.tame)){
                // A click may arrive as specific, ordinary and offhand callbacks.
                // Once the stack has moved, none may fall through to native commands.
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
                return true;
            }
        }
        if(!canRetrieve(player,event.getHand(),target))return false;
        var tame=(LivingEntity)target;
        if(!player.level().isClientSide){
            ItemStack held=tame.getMainHandItem();tame.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.MAIN_HAND,held);
        }
        RECENT_RETRIEVALS.put(player,new Retrieval(target.getUUID(),player.level().getGameTime()));
        event.setCanceled(true);event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));return true;
    }
    @SubscribeEvent(priority=EventPriority.HIGH)
    public static void unsealOnKill(LivingDeathEvent event) {
        if(event.isCanceled()||event.getEntity().level().isClientSide)return;
        LivingEntity killer=TameCombatEvents.resolveTameAttacker(event.getSource());
        if(killer==null && event.getSource().getEntity() instanceof LivingEntity owned && supportsHeldItems(owned))killer=owned;
        if(!supportsHeldItems(killer)||!killer.isAlive())return;
        boolean changed=false;
        for(EquipmentSlot slot:EquipmentSlot.values()){
            ItemStack sealed=killer.getItemBySlot(slot);
            if(!AnimightEquipmentEvents.isSealedItem(sealed)||sealed.getTag()==null||!sealed.getTag().contains("sealedItem",net.minecraft.nbt.Tag.TAG_COMPOUND))continue;
            ItemStack original=ItemStack.of(sealed.getTag().getCompound("sealedItem").copy());
            if(original.isEmpty()||AnimightEquipmentEvents.isRecoveryItem(original))continue;
            killer.setItemSlot(slot,original);changed=true;
        }
        if(changed)TameRegistry.markDirty();
    }
    private static final ClassValue<Optional<java.lang.reflect.Method>> NATIVE_SITTING=new ClassValue<>() {
        protected Optional<java.lang.reflect.Method> computeValue(Class<?> type) {
            try{return Optional.of(type.getMethod("isSitting"));}catch(NoSuchMethodException ignored){return Optional.empty();}
        }
    };
    private static boolean stationary(Mob mob) {
        if(TameEntityAdapter.isStayingStill(mob))return true;
        var method=NATIVE_SITTING.get(mob.getClass());
        try{return method.isPresent()&&Boolean.TRUE.equals(method.get().invoke(mob));}
        catch(ReflectiveOperationException ignored){return true;}
    }
    private static boolean mayPickUp(Mob mob) {
        return supportsPickup(mob)&&mob.isAlive()&&!mob.isNoAi()&&mob.getMainHandItem().isEmpty()
                && !TameDuelManager.isEntityInDuel(mob.getUUID())&&!DuelFollowerPause.isPaused(mob);
    }
    private static boolean validDrop(Mob mob,ItemEntity item) {
        return item.isAlive()&&!item.hasPickUpDelay()&&isWeapon(item.getItem())&&mob.hasLineOfSight(item);
    }
    public static boolean pickUpWeapon(Mob mob,ItemEntity item) {
        if(mob.level().isClientSide||!mayPickUp(mob)||!validDrop(mob,item)||mob.distanceToSqr(item)>=4)return false;
        mob.take(item,1);mob.setItemSlot(EquipmentSlot.MAINHAND,item.getItem().split(1));
        if(item.getItem().isEmpty())item.discard();TameRegistry.markDirty();return true;
    }
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if(!(event.getEntity() instanceof Mob mob)||mob.level().isClientSide||!supportsPickup(mob))return;
        // Vallumraptors retain their native grab animation and native item goal.
        if(VallumraptorWeapons.isTamedRaptor(mob))return;
        if(INSTALLED.putIfAbsent(mob,true)==null)mob.goalSelector.addGoal(1,new WeaponPickupGoal(mob));
        if(!mayPickUp(mob))return;
        ItemEntity closest=nearest(mob,2);
        if(closest!=null)pickUpWeapon(mob,closest);
    }
    private static ItemEntity nearest(Mob mob,double range) {
        return mob.level().getEntitiesOfClass(ItemEntity.class,mob.getBoundingBox().inflate(range)).stream()
                .filter(item->validDrop(mob,item)&&mob.distanceToSqr(item)<range*range)
                .min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
    }
    private static final class WeaponPickupGoal extends Goal {
        private final Mob mob;private ItemEntity item;private int pathTicks;
        WeaponPickupGoal(Mob mob){this.mob=mob;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        private boolean ready(){return mayPickUp(mob)&&!stationary(mob)&&mob.getTarget()==null;}
        public boolean canUse(){if(!ready())return false;item=nearest(mob,8);return item!=null;}
        public boolean canContinueToUse(){return ready()&&item!=null&&validDrop(mob,item)&&mob.distanceToSqr(item)<144;}
        public void tick(){
            if(pickUpWeapon(mob,item))return;
            mob.getLookControl().setLookAt(item,30,30);
            if(pathTicks--<=0){pathTicks=10;mob.getNavigation().moveTo(item,1.1);}
        }
        public void stop(){item=null;mob.getNavigation().stop();pathTicks=0;}
    }
}
