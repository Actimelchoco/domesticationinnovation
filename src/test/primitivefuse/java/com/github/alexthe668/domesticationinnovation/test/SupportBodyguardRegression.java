package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
public final class SupportBodyguardRegression {
 static void verify(PrimitiveFuseRegression.Fixture h,ServerPlayer owner)throws Exception {
  java.lang.reflect.Field clock;
  try{clock=net.minecraft.server.MinecraftServer.class.getDeclaredField("tickCount");}catch(NoSuchFieldException e){clock=net.minecraft.server.MinecraftServer.class.getDeclaredField("f_129766_");}
  clock.setAccessible(true);
  for(boolean low:List.of(false,true)) {
   owner.setPos(0,-57,2);
   for(int x=-8;x<=24;x++)for(int z=-7;z<=10;z++)h.level.setBlock(new net.minecraft.core.BlockPos(x,-58,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
   var pet=(Creeper)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs","support_creeper")).create(h.level);
   PrimitiveMobsCompat.tame(pet,owner);pet.setPos(2,-57,2);h.level.addFreshEntity(pet);
   var data=new TameData(pet,owner.getUUID(),false);data.rosterBodyguard=true;data.mode=TameMode.BODYGUARD.id();data.movementOrder=0;data.hungerSaturation=100000;data.bodyguardRange=12;TameRegistry.register(data);LoadedTameIndex.refresh(pet);
   var roster=BodyguardService.roster(owner);roster.bodyguards.add(data.ensureTlId());roster.bodyguardRange=12;
   var enemy=EntityType.HUSK.create(h.level);enemy.setNoAi(true);enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);enemy.setPos(7,-57,2);h.level.addFreshEntity(enemy);
   pet.setHealth(low?pet.getMaxHealth()/4:pet.getMaxHealth());
   var blasts=new PrimitiveFuseRegression.Blasts(pet);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(blasts);
   int targetTicks=0,chargeTicks=0;
   try {
    for(int i=0;i<320;i++) {
     // GoalSelector scheduling uses server ticks, not level game time.
     clock.setInt(h.level.getServer(),clock.getInt(h.level.getServer())+1);
     ((net.minecraft.world.level.storage.ServerLevelData)h.level.getLevelData()).setGameTime(h.level.getGameTime()+1);
     pet.tickCount++;enemy.tickCount++;owner.tickCount++;
     if(i%60==0)BodyguardService.refresh(h.level.getServer());
     pet.tick();enemy.tick();
     if(pet.getTarget()==enemy)targetTicks++;if(pet.getSwellDir()>0)chargeTicks++;
    }
    org.apache.logging.log4j.LogManager.getLogger("SupportBodyguardRegression").info("SUPPORT_BODYGUARD_BASELINE low={} blasts={} targetTicks={} chargeTicks={} pet={} enemy={} command={}",low,blasts.count,targetTicks,chargeTicks,pet.position(),enemy.position(),PrimitiveMobsCompat.getCommand(pet));
    h.assertTrue(targetTicks>200,"bodyguard support keeps hostile target");
    h.assertTrue(blasts.count>=2,"bodyguard support chases and repeatedly blasts without forced targets or ignition");
   }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(blasts);roster.bodyguards.remove(data.tlId);TameRegistry.remove(data.uuid);pet.discard();enemy.discard();}
  }
 }
}
