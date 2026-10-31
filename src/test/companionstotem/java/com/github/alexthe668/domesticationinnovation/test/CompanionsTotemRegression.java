package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@net.minecraftforge.fml.common.Mod("companions_totem_tests")
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="companions_totem_tests")
public class CompanionsTotemRegression {
    static final org.apache.logging.log4j.Logger LOG=org.apache.logging.log4j.LogManager.getLogger("CompanionsTotemRegression");
    static void check(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
    static Object call(Object object,String name,Class<?>[] types,Object...args)throws Exception{return object.getClass().getMethod(name,types).invoke(object,args);}
    static TameData tracked(LivingEntity pet,UUID owner){var data=new TameData(pet,owner,false);data.hungerSaturation=100000;data.abilities.add("lifesteal");data.abilityLevels.put("lifesteal",2);data.rankedMatches=17;TameRegistry.register(data);TameRegistry.bindEntityToData(pet,data);return data;}
    static Wolf wolf(ServerLevel level,UUID owner,int x){var wolf=EntityType.WOLF.create(level);wolf.setTame(true);wolf.setOwnerUUID(owner);wolf.setNoAi(true);wolf.setPos(x,-57,2);level.addFreshEntity(wolf);return wolf;}
    static void tick(ServerLevel level,BlockPos pos,BlockEntity totem)throws Exception{
        ((net.minecraft.world.level.storage.ServerLevelData)level.getLevelData()).setGameTime(level.getGameTime()+21);
        totem.getClass().getMethod("tick",Level.class,BlockPos.class,BlockState.class,BlockEntity.class).invoke(null,level,pos,totem.getBlockState(),totem);
    }
    @SubscribeEvent public static void run(ServerStartedEvent event){
        try{verify(event.getServer().overworld());LOG.info("COMPANIONS_TOTEM_TESTS_PASS");}
        catch(Throwable error){LOG.error("COMPANIONS_TOTEM_TESTS_FAIL",error);}
        finally{event.getServer().halt(false);}
    }
    @SuppressWarnings("unchecked") static void verify(ServerLevel level)throws Exception{
        var pos=new BlockPos(2,-57,2);
        var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("companions","respawn_totem_block"));
        check(block!=null,"native totem exists");level.setBlock(pos,block.defaultBlockState(),3);
        var totem=level.getBlockEntity(pos);check(totem!=null,"native totem block entity exists");
        var saved=(Map<UUID,net.minecraft.nbt.CompoundTag>)totem.getClass().getField("savedEntities").get(totem);
        var pending=totem.getClass().getDeclaredField("pending");pending.setAccessible(true);
        UUID owner=UUID.randomUUID();var vanilla=wolf(level,owner,3);var vd=tracked(vanilla,owner);
        var primitive=(Mob)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs","chameleon")).create(level);
        var fake=net.minecraftforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(owner,"TotemOwner"));
        PrimitiveMobsCompat.tame(primitive,fake);primitive.setNoAi(true);primitive.setPos(4,-57,2);level.addFreshEntity(primitive);var pd=tracked(primitive,owner);
        var nativePet=(TamableAnimal)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("companions","croissant_dragon")).create(level);
        nativePet.setTame(true);nativePet.setOwnerUUID(owner);nativePet.setNoAi(true);nativePet.setPos(3,-57,3);level.addFreshEntity(nativePet);tracked(nativePet,owner);
        var wild=EntityType.WOLF.create(level);wild.setNoAi(true);wild.setPos(5,-57,2);level.addFreshEntity(wild);
        check((boolean)call(totem,"captureNearby",new Class<?>[]{}),"native capture succeeds");
        check(saved.containsKey(vanilla.getUUID())&&saved.containsKey(primitive.getUUID()),"capture includes vanilla and interface-based tames");
        check(saved.containsKey(nativePet.getUUID()),"native companion captured");
        check(!saved.containsKey(wild.getUUID()),"wild animals are excluded");wild.discard();
        call(totem,"setCharges",new Class<?>[]{int.class},4);
        for(var pet:List.of(vanilla,primitive,nativePet)){
            var data=TameRegistry.get(pet.getUUID());var id=pet.getUUID();var tlId=data.tlId;
            pet.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
            check(pet.hurt(level.damageSources().genericKill(),100000),"actual lethal damage accepted");
            check(data.dead,"DI records death before totem respawn");
            int postDeathLevel=data.level,postDeaths=data.deaths;
            check(((List<?>)pending.get(totem)).size()==1,"one revival queued despite native death listener");
            var persisted=totem.saveWithFullMetadata();totem.load(persisted);
            check(((List<?>)pending.get(totem)).size()==1,"pending revival survives block entity save/load");
            tick(level,pos,totem);
            var restored=(LivingEntity)level.getEntity(id);
            check(restored!=null&&restored.isAlive(),"native totem restores the same entity UUID");
            check(TameData.getTlId(restored).equals(tlId),"stable TL identity preserved");
            check(TameRegistry.get(id)==data&&!data.dead,"existing registry row revived");
            check(data.level==postDeathLevel&&data.deaths==postDeaths&&data.rankedMatches==17&&data.abilityLevels.get("lifesteal")==2,"post-death progression, ranked history and abilities preserved");
            check(restored.getMainHandItem().is(Items.IRON_SWORD),"held equipment restored");
            check(data.commandRespawnAfterDayTime==-1&&TameRegistry.getLastDeath(data)==null,"revival clears pending command/death identity");
            check(((List<?>)pending.get(totem)).isEmpty(),"pending revival consumed");
            restored.discard();TameRegistry.remove(id);
        }
        check((int)call(totem,"getCharges",new Class<?>[]{})==1,"one charge consumed per successful revival");
        var race=wolf(level,owner,3);var rd=tracked(race,owner);call(totem,"captureNearby",new Class<?>[]{});
        race.hurt(level.damageSources().genericKill(),100000);check(rd.dead,"race fixture dies");
        var earlier=wolf(level,owner,4);earlier.discard();
        rd.dead=false; // A command or another respawn already restored this identity.
        tick(level,pos,totem);
        check(level.getEntity(race.getUUID())==null,"queued totem cannot recreate an already revived identity");
        check((int)call(totem,"getCharges",new Class<?>[]{})==1,"skipped duplicate consumes no charge");TameRegistry.remove(rd.uuid);
        var empty=wolf(level,owner,3);var ed=tracked(empty,owner);call(totem,"captureNearby",new Class<?>[]{});call(totem,"setCharges",new Class<?>[]{int.class},0);
        empty.hurt(level.damageSources().genericKill(),100000);check(ed.dead&&((List<?>)pending.get(totem)).isEmpty(),"empty totem leaves tame dead with no queue");TameRegistry.remove(ed.uuid);
        var duel=wolf(level,owner,3);var dd=tracked(duel,owner);var opponent=wolf(level,UUID.randomUUID(),4);var od=tracked(opponent,opponent.getOwnerUUID());
        call(totem,"captureNearby",new Class<?>[]{});call(totem,"setCharges",new Class<?>[]{int.class},3);
        TameDuelManager.startTeamDuel(level.getServer(),owner,Set.of(duel.getUUID()),od.ownerUUID,Set.of(opponent.getUUID()),Set.of(),false,false);
        duel.hurt(level.damageSources().genericKill(),100000);
        check(((List<?>)pending.get(totem)).isEmpty(),"duel elimination never queues a totem revival");
        check((int)call(totem,"getCharges",new Class<?>[]{})==3,"duels consume no totem charges");
        TameDuelManager.endDuelsForOwner(level.getServer(),owner);
        for(var data:List.of(dd,od)){var live=level.getEntity(data.uuid);if(live!=null)live.discard();TameRegistry.remove(data.uuid);}
        var first=wolf(level,owner,3);var fd=tracked(first,owner);var second=wolf(level,owner,4);var sd=tracked(second,owner);
        call(totem,"captureNearby",new Class<?>[]{});call(totem,"setCharges",new Class<?>[]{int.class},1);
        first.hurt(level.damageSources().genericKill(),100000);second.hurt(level.damageSources().genericKill(),100000);
        check(((List<?>)pending.get(totem)).size()==2,"simultaneous deaths queued");tick(level,pos,totem);
        check((fd.dead?0:1)+(sd.dead?0:1)==1,"one remaining charge revives exactly one tame");
        check((int)call(totem,"getCharges",new Class<?>[]{})==0,"charges never overdraw on concurrent deaths");
        for(var data:List.of(fd,sd)){var live=level.getEntity(data.uuid);if(live!=null)live.discard();TameRegistry.remove(data.uuid);}
        level.removeBlock(pos,false);
    }
}
