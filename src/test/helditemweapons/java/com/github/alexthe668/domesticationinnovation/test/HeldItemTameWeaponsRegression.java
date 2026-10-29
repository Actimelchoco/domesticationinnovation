package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class HeldItemTameWeaponsRegression {
    static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final FakePlayer owner;final List<Entity> entities=new ArrayList<>();
        final Map<UUID,net.minecraft.server.level.ServerPlayer> players;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper)throws Exception{
            h=helper;owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"HandOwner"));
            var p=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));owner.setPos(p.getX(),p.getY(),p.getZ());
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);players=(Map<UUID,net.minecraft.server.level.ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());players.put(owner.getUUID(),owner);h.getLevel().addNewPlayer(owner);
        }
        Mob pet(boolean raccoon){
            Mob mob=raccoon?AMEntityRegistry.RACCOON.get().create(h.getLevel()):EntityType.FOX.create(h.getLevel());
            TameEntityAdapter.setOwner(mob,owner.getUUID());mob.setNoAi(true);mob.setPos(owner.getX()+2,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(mob);entities.add(mob);TameRegistry.register(new TameData(mob,owner.getUUID(),!raccoon));return mob;
        }
        ItemEntity drop(Mob mob,ItemStack stack,double offset){var item=new ItemEntity(h.getLevel(),mob.getX()+offset,mob.getY(),mob.getZ(),stack);item.setDeltaMovement(0,0,0);h.getLevel().addFreshEntity(item);entities.add(item);return item;}
        LivingEntity enemy(Mob mob){var e=EntityType.ZOMBIE.create(h.getLevel());e.setNoAi(true);e.setPos(mob.getX()+1,mob.getY(),mob.getZ());e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);e.setHealth(200);e.getAttribute(Attributes.ARMOR).setBaseValue(0);h.getLevel().addFreshEntity(e);entities.add(e);return e;}
        public void close(){h.getLevel().removePlayerImmediately(owner,Entity.RemovalReason.DISCARDED);players.remove(owner.getUUID());for(var e:entities){TameRegistry.remove(e.getUUID());e.discard();}}
    }
    @GameTest(template="held_item_arena") public static void adminCommandListsLoadedWeaponTypesAndEnforcesPermission(GameTestHelper h)throws Exception{
        var output=new ArrayList<String>();
        var receiver=new net.minecraft.commands.CommandSource(){
            public void sendSystemMessage(net.minecraft.network.chat.Component message){output.add(message.getString());}
            public boolean acceptsSuccess(){return true;}
            public boolean acceptsFailure(){return true;}
            public boolean shouldInformAdmins(){return false;}
        };
        var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
        var source=h.getLevel().getServer().createCommandSourceStack().withSource(receiver).withPermission(2);
        var known=HeldItemTameWeapons.knownWeaponPickupTypes();
        check(known.contains(EntityType.FOX)&&known.contains(AMEntityRegistry.RACCOON.get()),"known types include fox and installed raccoon");
        check(!known.contains(EntityType.WOLF)&&!known.contains(EntityType.COW),"unverified vanilla equipment slots do not imply pickup support");
        int count=dispatcher.execute("tames admin canPickUpWeapons",source);
        check(count==known.size()&&output.size()==count+1,"console command prints count and one line per supported type");
        check(output.stream().anyMatch(line->line.contains("minecraft:fox"))
                &&output.stream().anyMatch(line->line.contains("alexsmobs:raccoon"))
                &&output.stream().anyMatch(line->line.contains("alexscaves:vallumraptor")),"output includes installed supported entity IDs");
        var ids=known.stream().map(type->net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(type).toString()).toList();
        check(ids.equals(ids.stream().sorted().toList()),"output ordering is stable");
        try{dispatcher.execute("tames admin canPickUpWeapons",source.withPermission(0));throw new AssertionError("non-admin was allowed");}
        catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){}
        h.succeed();
    }

    @GameTest(template="held_item_arena") public static void foxAndRaccoonKeepWeaponsAndRetrieveWithoutLosingData(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            for(boolean raccoon:List.of(false,true)){
                Mob pet=f.pet(raccoon);ItemStack weapon=new ItemStack(Items.DIAMOND_SWORD);weapon.enchant(Enchantments.SHARPNESS,3);weapon.setDamageValue(25);weapon.getOrCreateTag().putString("Marker","retained");
                pet.setNoAi(false);var drop=f.drop(pet,weapon.copy(),0.5);
                check(HeldItemTameWeapons.pickUpWeapon(pet,drop),"known item-carrying tame accepts dropped weapon");pet.setNoAi(false);
                var food=f.drop(pet,new ItemStack(Items.BEEF),0.4);
                if(pet instanceof EntityRaccoon r){r.onGetItem(food);r.lookForWaterBeforeEatingTimer=100;r.setWashing(true);}
                else check(!((net.minecraft.world.entity.animal.Fox)pet).canHoldItem(food.getItem()),"fox must not swap a weapon for food");
                for(int i=0;i<650;i++)pet.tick();
                check(ItemStack.matches(pet.getMainHandItem(),weapon),"food/wash/eat timers cannot destroy or replace weapon");
                if(pet instanceof EntityRaccoon r)check(!r.isWashing(),"raccoon does not wash weapon");
                pet.getMainHandItem().hurtAndBreak(10000,pet,e->{throw new AssertionError("weapon must not break");});
                check(ItemStack.matches(pet.getMainHandItem(),weapon),"held weapon keeps durability");
                f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setSprinting(true);
                var click=new PlayerInteractEvent.EntityInteract(f.owner,InteractionHand.MAIN_HAND,pet);DomesticationMod.PROXY.onInteractWithEntity(click);
                check(click.isCanceled()&&pet.getMainHandItem().isEmpty()&&ItemStack.matches(f.owner.getMainHandItem(),weapon),"owner can retrieve exact held item with sprint interaction");
                food.discard();
            }
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void bothNativeAttackPathsUseEnchantmentsAndKeepNormalFood(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            for(boolean raccoon:List.of(false,true)){
                Mob pet=f.pet(raccoon);var victim=f.enemy(pet);
                for(Item item:new Item[]{Items.DIAMOND_SWORD,Items.DIAMOND_AXE}){
                    ItemStack weapon=new ItemStack(item);weapon.enchant(Enchantments.SMITE,3);weapon.enchant(Enchantments.FIRE_ASPECT,2);weapon.enchant(Enchantments.KNOCKBACK,2);weapon.setDamageValue(23);
                    pet.setItemSlot(EquipmentSlot.MAINHAND,weapon);pet.tick();victim.setHealth(200);victim.invulnerableTime=0;victim.clearFire();victim.setDeltaMovement(0,0,0);
                    if(pet instanceof EntityRaccoon r){r.setTarget(victim);r.setAnimation(EntityRaccoon.ANIMATION_ATTACK);r.setAnimationTick(5);r.tick();}
                    else pet.doHurtTarget(victim);
                    check(200-victim.getHealth()>10,"native weapon hit includes weapon attack modifiers and Smite: "+pet.getType()+" damage="+(200-victim.getHealth()));
                    check(victim.isOnFire()&&victim.getDeltaMovement().horizontalDistanceSqr()>0,"Fire Aspect and Knockback apply");
                    check(weapon.getDamageValue()==23&&weapon.getCount()==1,"attacks cause no wear");
                }
                pet.setTarget(null);pet.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);
                if(pet instanceof EntityRaccoon r)check(r.canTargetItem(new ItemStack(Items.BEEF)),"unarmed raccoon still targets food");
                else check(((net.minecraft.world.entity.animal.Fox)pet).canHoldItem(new ItemStack(Items.BEEF)),"unarmed fox still accepts food");
            }
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void nativeFoodConsumptionRemainsAvailableWithoutAWeapon(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            for(boolean raccoon:List.of(false,true)){
                Mob pet=f.pet(raccoon);pet.setNoAi(false);
                if(pet instanceof net.minecraft.world.entity.animal.Fox) ((com.github.alexthe666.citadel.server.entity.IComandableMob)pet).setCommand(2);
                pet.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BEEF));
                if(pet instanceof EntityRaccoon r)r.lookForWaterBeforeEatingTimer=20;
                for(int i=0;i<700;i++)pet.tick();
                check(pet.getMainHandItem().isEmpty(),"unarmed tame still eats native carried food: "+pet.getType());
            }
        }h.succeed();
    }

    @GameTest(template="held_item_arena") public static void raccoonKeepsCommandsAndCosmeticsWhileArmed(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var pet=(EntityRaccoon)f.pet(true);check(pet.isOwnedBy(f.owner),"fixture owner must resolve in native interactions");ItemStack weapon=new ItemStack(Items.IRON_AXE);pet.setItemSlot(EquipmentSlot.MAINHAND,weapon);
            f.owner.setSprinting(false);f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);int before=pet.getCommand();pet.mobInteract(f.owner,InteractionHand.MAIN_HAND);
            check(pet.getMainHandItem()==weapon&&pet.getCommand()!=before,"normal raccoon command changes without dropping weapon");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.RED_CARPET));pet.mobInteract(f.owner,InteractionHand.MAIN_HAND);
            check(pet.getMainHandItem()==weapon&&pet.getColor()==net.minecraft.world.item.DyeColor.RED,"carpet interaction still works while armed");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void foxAndRaccoonUnsealAfterRealKillAndWildBehaviorRemainsNative(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            for(boolean raccoon:List.of(false,true)){
                Mob pet=f.pet(raccoon);ItemStack original=new ItemStack(Items.IRON_SWORD);original.enchant(Enchantments.SHARPNESS,3);
                var seal=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("l2hostility","sealed_item")));seal.getOrCreateTag().put("sealedItem",original.save(new net.minecraft.nbt.CompoundTag()));pet.setItemSlot(EquipmentSlot.MAINHAND,seal);
                var victim=f.enemy(pet);victim.hurt(h.getLevel().damageSources().mobAttack(pet),1000);
                check(ItemStack.matches(pet.getMainHandItem(),original),"native tame kill restores sealed weapon");
                Mob wild=raccoon?AMEntityRegistry.RACCOON.get().create(h.getLevel()):EntityType.FOX.create(h.getLevel());
                check(!HeldItemTameWeapons.supportsHeldItems(wild),"wild or unowned mob does not receive new held-item rules");wild.discard();
            }
        }h.succeed();
    }
    static final class ExternalCarrier extends PathfinderMob implements OwnableEntity {
        final UUID owner;
        ExternalCarrier(net.minecraft.world.level.Level level,UUID owner){super(EntityType.COW,level);this.owner=owner;}
        public UUID getOwnerUUID(){return owner;}
        public LivingEntity getOwner(){return level().getPlayerByUUID(owner);}
    }
    @GameTest(template="held_item_arena") public static void externalOwnedHandIsRecognizedWithoutEnablingUnverifiedPickup(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var carrier=new ExternalCarrier(h.getLevel(),f.owner.getUUID());carrier.setPos(f.owner.getX()+2,f.owner.getY(),f.owner.getZ());h.getLevel().addFreshEntity(carrier);f.entities.add(carrier);
            check(!TameEntityAdapter.isSupported(carrier)&&HeldItemTameWeapons.supportsHeldItems(carrier),"generic OwnableEntity carrier is recognized without a hardcoded mod ID");
            check(!HeldItemTameWeapons.supportsPickup(carrier),"unknown type does not automatically gain weapon pickup");
            var original=new ItemStack(Items.IRON_AXE);original.enchant(Enchantments.SHARPNESS,2);original.setDamageValue(15);
            var seal=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("l2hostility","sealed_item")));seal.getOrCreateTag().put("sealedItem",original.save(new net.minecraft.nbt.CompoundTag()));carrier.setItemSlot(EquipmentSlot.MAINHAND,seal);
            var enemy=f.enemy(carrier);enemy.hurt(h.getLevel().damageSources().mobAttack(carrier),1000);
            check(ItemStack.matches(carrier.getMainHandItem(),original),"external owned carrier unseals after its real kill");
            carrier.getMainHandItem().hurtAndBreak(1000,carrier,e->{throw new AssertionError("external held weapon must not break");});
            check(ItemStack.matches(carrier.getMainHandItem(),original),"external held weapon retains durability");
            f.owner.setSprinting(true);var event=new PlayerInteractEvent.EntityInteract(f.owner,InteractionHand.MAIN_HAND,carrier);
            check(HeldItemTameWeapons.retrieve(event,carrier)&&ItemStack.matches(f.owner.getMainHandItem(),original),"external owner can retrieve exact item");
        }h.succeed();
    }

    @GameTest(template="held_item_arena",timeoutTicks=240,batch="held-item-standing") public static void foxAndRaccoonApproachDroppedWeapons(GameTestHelper h)throws Exception{
        var f=new Fixture(h);var fox=f.pet(false);var raccoon=(EntityRaccoon)f.pet(true);
        fox.setPos(f.owner.getX()-1,fox.getY(),fox.getZ());raccoon.setPos(fox.getX(),fox.getY(),fox.getZ()+12);
        var a=f.drop(fox,new ItemStack(Items.IRON_SWORD),4);var b=f.drop(raccoon,new ItemStack(Items.IRON_AXE),4);
        fox.setNoAi(false);raccoon.setNoAi(false);
        h.runAtTickTime(180,()->{
            try{
                check(fox.getMainHandItem().is(Items.IRON_SWORD)&&raccoon.getMainHandItem().is(Items.IRON_AXE),"standing pets approach weapons: fox="+fox.getMainHandItem()+", raccoon="+raccoon.getMainHandItem()+", pos="+raccoon.position()+", drop="+b.position()+", ticks="+raccoon.tickCount+", sit="+raccoon.isSitting()+", target="+raccoon.getTarget()+", goals="+raccoon.goalSelector.getAvailableGoals().stream().map(g->g.getGoal().getClass().getSimpleName()+":"+g.isRunning()).toList());
                check(a.isRemoved()&&b.isRemoved(),"approach pickup consumes exactly the original dropped weapons");
            }finally{f.close();}h.succeed();
        });
    }

    @GameTest(template="held_item_arena",timeoutTicks=240,batch="held-item-sitting") public static void sittingFoxAndRaccoonGrabNearbyWeaponsWithoutWalking(GameTestHelper h)throws Exception{
        var f=new Fixture(h);var fox=f.pet(false);fox.setPos(f.owner.getX()-1,fox.getY(),fox.getZ());var raccoon=(EntityRaccoon)f.pet(true);raccoon.setPos(fox.getX()+3,fox.getY(),fox.getZ());
        ((com.github.alexthe666.citadel.server.entity.IComandableMob)fox).setCommand(1);raccoon.setCommand(2);raccoon.forcedSit=true;raccoon.setOrderedToSit(true);
        var startFox=fox.position();var startRaccoon=raccoon.position();fox.setNoAi(false);raccoon.setNoAi(false);
        var a=f.drop(fox,new ItemStack(Items.IRON_SWORD),0.5);var b=f.drop(raccoon,new ItemStack(Items.IRON_AXE),0.5);
        h.runAtTickTime(100,()->{
            try{
                check(fox.getMainHandItem().is(Items.IRON_SWORD)&&raccoon.getMainHandItem().is(Items.IRON_AXE),"sitting pets collect nearby weapons: fox="+fox.getMainHandItem()+", raccoon="+raccoon.getMainHandItem()+", drops="+a.getItem()+"/"+b.getItem()+", support="+HeldItemTameWeapons.supportsPickup(fox)+"/"+HeldItemTameWeapons.supportsPickup(raccoon));
                check(fox.position().subtract(startFox).horizontalDistanceSqr()<0.2&&raccoon.position().subtract(startRaccoon).horizontalDistanceSqr()<0.2,"sitting pets do not walk to collect weapons");
                check(((com.github.alexthe666.citadel.server.entity.IComandableMob)fox).getCommand()==1&&raccoon.getCommand()==2,"sitting commands remain unchanged");
            }finally{f.close();}h.succeed();
        });
    }
}
