package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexmodguy.alexscaves.server.entity.ACEntityRegistry;
import com.github.alexmodguy.alexscaves.server.entity.ai.VallumraptorMeleeGoal;
import com.github.alexmodguy.alexscaves.server.entity.living.VallumraptorEntity;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.VallumraptorWeapons;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class VallumraptorWeaponsRegression {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final VallumraptorEntity raptor;final FakePlayer owner;
        Fixture(GameTestHelper helper){
            h=helper;owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"RaptorOwner"));
            raptor=ACEntityRegistry.VALLUMRAPTOR.get().create(h.getLevel());
            raptor.tame(owner);raptor.setNoAi(true);raptor.setCommand(0);raptor.setOrderedToSit(false);
            var p=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));raptor.setPos(p.getX(),p.getY(),p.getZ());owner.setPos(p.getX(),p.getY(),p.getZ());
            h.getLevel().addFreshEntity(raptor);TameRegistry.register(new TameData(raptor));
        }
        void grab(ItemEntity item){raptor.setAnimation(VallumraptorEntity.ANIMATION_GRAB);raptor.setAnimationTick(16);raptor.onGetItem(item);}
        public void close(){TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());TameRegistry.remove(raptor.getUUID());raptor.discard();}
    }

    @GameTest(template="empty")
    public static void nativeGrabPreservesWeaponsAndFoodCannotReplaceThem(GameTestHelper h){
        try(var f=new Fixture(h)){
            ItemStack weapon=new ItemStack(Items.DIAMOND_SWORD);weapon.enchant(Enchantments.SHARPNESS,4);weapon.setDamageValue(100);
            weapon.getOrCreateTag().putString("Marker","keep");
            var drop=new ItemEntity(h.getLevel(),f.raptor.getX(),f.raptor.getY(),f.raptor.getZ(),weapon.copy());
            var food=new ItemEntity(h.getLevel(),f.raptor.getX(),f.raptor.getY(),f.raptor.getZ(),new ItemStack(Items.BEEF));
            try{
                check(f.raptor.canTargetItem(weapon),"tamed raptor targets dropped sword");
                check(f.raptor.canTargetItem(new ItemStack(Items.IRON_AXE)),"tamed raptor targets dropped axe");
                check(!f.raptor.canTargetItem(new ItemStack(Items.BOW))&&!f.raptor.canTargetItem(new ItemStack(Items.DIAMOND_PICKAXE)),"unsupported weapons/tools remain excluded");
                f.grab(drop);
                check(drop.getItem().isEmpty()&&ItemStack.matches(f.raptor.getMainHandItem(),weapon),"native grab moves exactly one weapon with damage and all NBT");
                check(!f.raptor.canTargetItem(f.raptor.getMainHandItem()),"held weapon must never pass native food/eating predicate");
                f.grab(food);
                for(int i=0;i<260;i++)f.raptor.tick();
                check(ItemStack.matches(f.raptor.getMainHandItem(),weapon)&&food.getItem().getCount()==1,"weapon survives eating timer and queued food grab");
                f.raptor.getMainHandItem().hurtAndBreak(10000,f.raptor,e->{throw new AssertionError("weapon must not break");});
                check(ItemStack.matches(f.raptor.getMainHandItem(),weapon),"held weapon never takes durability damage");
                var saved=new net.minecraft.nbt.CompoundTag();f.raptor.saveWithoutId(saved);
                var loaded=ACEntityRegistry.VALLUMRAPTOR.get().create(h.getLevel());loaded.load(saved);
                check(ItemStack.matches(loaded.getMainHandItem(),weapon),"native entity saving retains weapon");loaded.discard();
            }finally{drop.discard();food.discard();}
        }h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=240)
    public static void nativeItemGoalCollectsDroppedAxe(GameTestHelper h){
        var f=new Fixture(h);
        var drop=new ItemEntity(h.getLevel(),f.raptor.getX()+0.5,f.raptor.getY(),f.raptor.getZ(),new ItemStack(Items.IRON_AXE));
        h.getLevel().addFreshEntity(drop);f.raptor.setNoAi(false);
        h.runAtTickTime(180,()->{
            try{
                check(f.raptor.getMainHandItem().is(Items.IRON_AXE)&&drop.isRemoved(),"native item goal must find and grab a dropped axe");
            }finally{drop.discard();f.close();}
            h.succeed();
        });
    }

    @GameTest(template="empty")
    public static void foodAndWildRaptorsKeepNativeBehavior(GameTestHelper h){
        try(var f=new Fixture(h)){
            check(f.raptor.canTargetItem(new ItemStack(Items.BEEF)),"unarmed tame still targets meat");
            f.raptor.setTame(false);
            check(!f.raptor.canTargetItem(new ItemStack(Items.IRON_SWORD))&&!f.raptor.canTargetItem(new ItemStack(Items.IRON_AXE)),"wild raptors do not gain weapons");
            check(f.raptor.canTargetItem(new ItemStack(Items.BEEF)),"wild food theft stays available");
            var food=new ItemEntity(h.getLevel(),f.raptor.getX(),f.raptor.getY(),f.raptor.getZ(),new ItemStack(Items.BEEF));
            try{f.grab(food);for(int i=0;i<230;i++)f.raptor.tick();check(f.raptor.getMainHandItem().isEmpty(),"native food still gets eaten");}finally{food.discard();}
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void nativeMeleeUsesWeaponEnchantmentsWithoutWear(GameTestHelper h) throws Exception {
        try(var f=new Fixture(h)){
            var victim=EntityType.ZOMBIE.create(h.getLevel());victim.setNoAi(true);
            victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);victim.setHealth(200);victim.getAttribute(Attributes.ARMOR).setBaseValue(0);
            victim.setPos(f.raptor.getX()+1,f.raptor.getY(),f.raptor.getZ());h.getLevel().addFreshEntity(victim);
            try{
                var goal=new VallumraptorMeleeGoal(f.raptor);
                var hit=VallumraptorMeleeGoal.class.getDeclaredMethod("checkAndDealDamage",LivingEntity.class);hit.setAccessible(true);
                hit.invoke(goal,victim);float bare=200-victim.getHealth();
                for(Item item:new Item[]{Items.DIAMOND_SWORD,Items.DIAMOND_AXE}){
                    ItemStack weapon=new ItemStack(item);weapon.enchant(Enchantments.SMITE,3);weapon.enchant(Enchantments.FIRE_ASPECT,2);weapon.enchant(Enchantments.KNOCKBACK,2);weapon.setDamageValue(17);
                    f.raptor.setItemSlot(EquipmentSlot.MAINHAND,weapon);f.raptor.tick();
                    victim.invulnerableTime=0;victim.setHealth(200);victim.clearFire();victim.setDeltaMovement(0,0,0);
                    hit.invoke(goal,victim);
                    check(200-victim.getHealth()>bare+7,"native melee must add weapon attack and Smite damage: "+item+" damage="+(200-victim.getHealth()));
                    check(victim.isOnFire(),"Fire Aspect must ignite target");
                    check(victim.getDeltaMovement().horizontalDistanceSqr()>0,"Knockback must move target");
                    check(weapon.getDamageValue()==17&&weapon.getCount()==1,"native weapon attacks cause no wear");
                }
                victim.invulnerableTime=0;victim.setHealth(200);victim.clearFire();
                victim.setPos(f.raptor.getX()+30,f.raptor.getY(),f.raptor.getZ());hit.invoke(goal,victim);
                check(victim.getHealth()==200&&!victim.isOnFire(),"native range checks remain enforced");
            }finally{victim.discard();}
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void duelKeepsEquipmentLockedAndFriendlyHitsBlocked(GameTestHelper h) throws Exception {
        try(var a=new Fixture(h);var b=new Fixture(h)){
            a.raptor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            a.raptor.tick();
            var friend=EntityType.WOLF.create(h.getLevel());friend.setOwnerUUID(a.owner.getUUID());friend.setTame(true);friend.setNoAi(true);
            friend.setPos(a.raptor.getX()+1,a.raptor.getY(),a.raptor.getZ());h.getLevel().addFreshEntity(friend);
            try{
                var hit=VallumraptorMeleeGoal.class.getDeclaredMethod("checkAndDealDamage",LivingEntity.class);hit.setAccessible(true);
                float health=friend.getHealth();hit.invoke(new VallumraptorMeleeGoal(a.raptor),friend);
                check(friend.getHealth()==health,"armed native attacks respect friendly owner protection");
                TameDuelManager.startTeamDuel(h.getLevel().getServer(),a.owner.getUUID(),java.util.Set.of(a.raptor.getUUID()),b.owner.getUUID(),java.util.Set.of(b.raptor.getUUID()));
                check(TameDuelManager.isEntityInDuel(a.raptor.getUUID()),"duel fixture must be active");
                a.owner.setSprinting(true);
                check(!VallumraptorWeapons.retrieve(new PlayerInteractEvent.EntityInteract(a.owner,InteractionHand.MAIN_HAND,a.raptor),a.raptor),"equipment retrieval blocked during duel");
                check(!b.raptor.canTargetItem(new ItemStack(Items.IRON_AXE)),"unarmed duelist cannot collect new weapons");
            }finally{friend.discard();}
        }h.succeed();
    }

    @GameTest(template="empty")
    public static void retrieveDoesNotStealCommandsOrHungerShortcut(GameTestHelper h){
        try(var f=new Fixture(h)){
            ItemStack weapon=new ItemStack(Items.IRON_AXE);weapon.enchant(Enchantments.SHARPNESS,2);
            f.raptor.setItemSlot(EquipmentSlot.MAINHAND,weapon);
            var normal=new PlayerInteractEvent.EntityInteract(f.owner,InteractionHand.MAIN_HAND,f.raptor);
            check(!VallumraptorWeapons.retrieve(normal,f.raptor),"ordinary right click remains native command");
            f.owner.setShiftKeyDown(true);f.owner.setSprinting(true);
            check(!VallumraptorWeapons.retrieve(normal,f.raptor),"sneak remains hunger/stat interaction");f.owner.setShiftKeyDown(false);
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF));
            check(!VallumraptorWeapons.retrieve(normal,f.raptor),"held food keeps feeding interaction");f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            var other=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"OtherRaptorOwner"));other.setSprinting(true);
            check(!VallumraptorWeapons.retrieve(new PlayerInteractEvent.EntityInteract(other,InteractionHand.MAIN_HAND,f.raptor),f.raptor),"only owner retrieves items");
            int command=f.raptor.getCommand();
            var event=new PlayerInteractEvent.EntityInteractSpecific(f.owner,InteractionHand.MAIN_HAND,f.raptor,net.minecraft.world.phys.Vec3.ZERO);
            TameRegistry.get(f.raptor.getUUID()).hungerSaturation=0;
            DomesticationMod.PROXY.onHungryTameEntityInteractSpecific(event);
            check(!event.isCanceled(),"hunger lock must still allow owner to retrieve equipment");
            DomesticationMod.PROXY.onDuelEntityInteractSpecific(event);
            check(event.isCanceled()&&f.raptor.getMainHandItem().isEmpty()&&ItemStack.matches(f.owner.getMainHandItem(),weapon),"sprint interaction returns exact weapon directly to empty main hand");
            check(f.raptor.getCommand()==command,"retrieval never changes command");
            check(!VallumraptorWeapons.retrieve(normal,f.raptor),"repeat callback cannot duplicate item");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.raptor.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BEEF));
            check(VallumraptorWeapons.retrieve(normal,f.raptor)&&f.owner.getMainHandItem().is(Items.BEEF),"owner can also retrieve held food");
        }h.succeed();
    }
}
