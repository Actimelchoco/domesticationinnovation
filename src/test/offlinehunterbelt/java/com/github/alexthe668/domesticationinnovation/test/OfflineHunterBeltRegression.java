package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.belt.HunterBeltItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;
import java.util.*;

@Mod("primitive_compat_tests")
@Mod.EventBusSubscriber(modid="primitive_compat_tests")
public class OfflineHunterBeltRegression {
    static void check(boolean value,String message){if(!value)throw new IllegalStateException(message);}
    static void damage(ServerLevel level,LivingEntity pet,float expected){
        var event=new LivingHurtEvent(pet,level.damageSources().mobAttack(pet),10);
        HunterBeltItem.HunterBeltEvents.onLivingDamage(event);
        new CommonProxy().onInterfaceTameHunterBeltDamage(event);
        check(Math.abs(event.getAmount()-expected)<.001,"expected "+expected+" damage, got "+event.getAmount());
    }
    @SubscribeEvent public static void run(ServerStartedEvent event){
        var logger=org.apache.logging.log4j.LogManager.getLogger("OfflineHunterBeltRegression");
        try{test(event.getServer().overworld());logger.info("OFFLINE_HUNTER_BELT_TESTS_PASS");}
        catch(Throwable error){logger.error("OFFLINE_HUNTER_BELT_TESTS_FAIL",error);}
        finally{event.getServer().halt(false);}
    }
    @SuppressWarnings("unchecked") static void test(ServerLevel level)throws Exception{
        var owner=FakePlayerFactory.get(level,new GameProfile(UUID.randomUUID(),"OfflineBelt"));
        var online=(Map<UUID,ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11197_").get(level.getServer().getPlayerList());
        var actualPlayers=(List<ServerPlayer>)ObfuscationReflectionHelper.findField(net.minecraft.server.players.PlayerList.class,"f_11196_").get(level.getServer().getPlayerList());
        online.put(owner.getUUID(),owner);
        var wolf=EntityType.WOLF.create(level);wolf.setTame(true);wolf.setOwnerUUID(owner.getUUID());
        var fox=EntityType.FOX.create(level);((ModifedToBeTameable)fox).setTameOwnerUUID(owner.getUUID());
        var primitive=(Mob)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs:rocket_creeper")).create(level);
        PrimitiveMobsCompat.tame(primitive,owner);
        var pets=List.<LivingEntity>of(wolf,fox,primitive);
        for(var pet:pets){pet.setPos(0,-57,0);level.addFreshEntity(pet);var data=new TameData(pet,owner.getUUID(),false);data.hasHome=true;TameRegistry.register(data);}
        var inventory=CuriosApi.getCuriosInventory(owner).orElseThrow(()->new IllegalStateException("Missing Curios inventory"));var original=new HashMap<>(inventory.getCurios());
        var slots=new CurioStacksHandler(inventory,"belt",3,true,true,true,ICurio.DropRule.DEFAULT);
        inventory.setCurios(new HashMap<>(Map.of("belt",slots)));
        var belt=ForgeRegistries.ITEMS.getValue(new ResourceLocation("relics:hunter_belt"));var relic=(IRelicItem)belt;
        var first=new ItemStack(belt);var second=new ItemStack(belt);var cosmetic=new ItemStack(belt);
        relic.setAbilityValue(first,"training","damage",1.5);relic.setAbilityValue(second,"training","damage",2.0);
        relic.setAbilityValue(cosmetic,"training","damage",9.0);
        var third=cosmetic.copy();slots.getStacks().setStackInSlot(2,third);
        slots.getStacks().setStackInSlot(0,first);slots.getStacks().setStackInSlot(1,second);
        slots.getCosmeticStacks().setStackInSlot(2,cosmetic);owner.getInventory().add(cosmetic.copy());
        try{
            for(var pet:pets)damage(level,pet,25);
            var blastData=TameRegistry.get(primitive.getUUID());blastData.bonusDamage=4;
            blastData.mode=TameMode.MONSTER_HUNTER.id();blastData.hasHome=false;
            var blastVictim=EntityType.HUSK.create(level);
            var blast=new LivingHurtEvent(blastVictim,level.damageSources().explosion(primitive,primitive),10);
            MinecraftForge.EVENT_BUS.post(blast);
            check(blast.getAmount()==35,"native explosion adds damage before the two-belt multiplier on the real event bus");
            blastData.bonusDamage=0;blastData.hasHome=true;
            int xp=relic.getExperience(first);
            int thirdXp=relic.getExperience(third);
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(owner));online.remove(owner.getUUID());
            var saved=HunterBeltOfflineData.get(level.getServer());check(saved.get(owner.getUUID())==2.5,"logout snapshots only the two equipped belts");
            check(relic.getExperience(first)==xp,"logout does not award XP");
            for(var pet:pets)damage(level,pet,25);
            check(relic.getExperience(first)==xp,"offline combat does not mutate offline inventory or grant XP");
            check(relic.getExperience(third)==thirdXp,"third equipped belt contributes neither damage nor XP");
            var arrow=new net.minecraft.world.entity.projectile.Arrow(level,primitive);
            var projectile=new LivingHurtEvent(wolf,level.damageSources().arrow(arrow,primitive),10);
            new CommonProxy().onInterfaceTameHunterBeltDamage(projectile);
            check(projectile.getAmount()==25,"interface tame projectile gets offline bonus exactly once");
            check(HunterBeltOfflineData.load(((net.minecraft.world.level.saveddata.SavedData)saved).save(new CompoundTag())).get(owner.getUUID())==2.5,"snapshot survives serialization and reload");
            var legacy=((net.minecraft.world.level.saveddata.SavedData)saved).save(new CompoundTag());legacy.remove("BeltLimit");
            check(HunterBeltOfflineData.load(legacy).get(owner.getUUID())==1,"old uncapped aggregates cannot bypass the new two-belt limit");
            UUID stranger=UUID.randomUUID();wolf.setOwnerUUID(stranger);TameRegistry.get(wolf.getUUID()).ownerUUID=stranger;
            damage(level,wolf,10);wolf.setOwnerUUID(owner.getUUID());TameRegistry.get(wolf.getUUID()).ownerUUID=owner.getUUID();
            // Invoke our login listener directly: the fake player has no network channel for Forge login sync.
            online.put(owner.getUUID(),owner);HunterBeltCompat.login(new PlayerEvent.PlayerLoggedInEvent(owner));
            check(saved.get(owner.getUUID())==1,"login invalidates offline snapshot");
            slots.getStacks().setStackInSlot(2,ItemStack.EMPTY);
            slots.getStacks().setStackInSlot(1,ItemStack.EMPTY);for(var pet:pets)damage(level,pet,15);
            slots.getStacks().setStackInSlot(0,ItemStack.EMPTY);
            MinecraftForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(owner));online.remove(owner.getUUID());
            check(saved.get(owner.getUUID())==1,"unequipping removes the bonus at logout");for(var pet:pets)damage(level,pet,10);
            slots.getStacks().setStackInSlot(0,first);online.put(owner.getUUID(),owner);
            // Server shutdown snapshots the actual online player list too.
            actualPlayers.add(owner);
            HunterBeltCompat.stopping(new net.minecraftforge.event.server.ServerStoppingEvent(level.getServer()));
            check(saved.get(owner.getUUID())==1.5,"normal shutdown preserves equipped bonus");
            actualPlayers.remove(owner);
        }finally{
            actualPlayers.remove(owner);online.remove(owner.getUUID());inventory.setCurios(original);
            for(var pet:pets){TameRegistry.remove(pet.getUUID());pet.discard();}
        }
    }
}
