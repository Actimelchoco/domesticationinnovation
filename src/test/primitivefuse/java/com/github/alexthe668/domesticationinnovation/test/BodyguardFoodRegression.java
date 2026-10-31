package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
public final class BodyguardFoodRegression {
 static void verify(PrimitiveFuseRegression.Fixture h,ServerPlayer owner)throws Exception {
  var process=TameCommands.class.getDeclaredMethod("processTameHunger",net.minecraft.server.MinecraftServer.class);process.setAccessible(true);
  float saved=owner.getFoodData().getSaturationLevel();
  try {
   for(boolean roster:List.of(false,true))for(boolean primitive:List.of(false,true)) {
    var pet=(Mob)(primitive?ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs","support_creeper")).create(h.level):EntityType.WOLF.create(h.level));
    if(pet instanceof TamableAnimal animal){animal.setTame(true);animal.setOwnerUUID(owner.getUUID());}else PrimitiveMobsCompat.tame(pet,owner);
    pet.setNoAi(true);pet.setPos(2,-57,2);h.level.addFreshEntity(pet);
    var data=new TameData(pet,owner.getUUID(),false);data.rosterBodyguard=roster;data.mode=roster?TameMode.DEFAULT.id():TameMode.BODYGUARD.id();data.hungerSaturation=0;TameRegistry.register(data);LoadedTameIndex.refresh(pet);
    try {
     data.hungerInventory.add(new ItemStack(Items.BEEF,3));owner.getFoodData().setSaturation(2);
     h.assertTrue(TameCommands.prepareHungerForUltimine(data,pet,50),"roster and mode bodyguards feed from owner");
     h.assertTrue(data.hungerSaturation==50&&Math.abs(owner.getFoodData().getSaturationLevel()-1.5F)<.0001,"100-to-1 transfer charges exactly the deficit");
     h.assertTrue(data.hungerInventory.get(0).getCount()==3,"owner saturation is used before stored food");
     data.hungerSaturation=0;TameFoodManager.tick(pet,data);
     h.assertTrue(data.hungerSaturation==1,"zero-saturation guard receives food before combat checks, including non-Animal creepers");
     data.hungerInventory.clear();data.hungerSaturation=0;owner.getFoodData().setSaturation(.5F);
     h.assertTrue(!TameCommands.prepareHungerForUltimine(data,pet,100),"insufficient owner saturation cannot fund a larger action");
     h.assertTrue(data.hungerSaturation==0&&owner.getFoodData().getSaturationLevel()==.5F,"failed transfer does not create or spend saturation");
     owner.getFoodData().setSaturation(0);
     h.assertTrue(!BodyguardService.feedFromOwner(data,pet,1)&&TameCommands.isHungerBlockingAbilities(data),"empty owner still respects resource limits");
     owner.getFoodData().setSaturation(2);data.hungerForcedSit=true;
     if(pet instanceof TamableAnimal animal){animal.setOrderedToSit(true);animal.setInSittingPose(true);}else PrimitiveMobsCompat.setCommand(pet,1);
     ((net.minecraft.world.level.storage.ServerLevelData)h.level.getLevelData()).setGameTime((h.level.getGameTime()/20+1)*20);
     process.invoke(null,h.level.getServer());
     h.assertTrue(!data.hungerForcedSit&&!TameEntityAdapter.isStayingStill(pet),"owner refilling saturation wakes hunger-forced bodyguards");
     data.hungerForcedSit=true;data.hungerSaturation=0;DuelFollowerPause.begin(h.level.getServer(),Set.of(owner.getUUID()));DuelFollowerPause.tick(pet,data);
     process.invoke(null,h.level.getServer());
     h.assertTrue(DuelFollowerPause.isPaused(pet)&&TameEntityAdapter.isStayingStill(pet),"owner feeding never overrides duel waiting sit");
     DuelFollowerPause.end(h.level.getServer(),Set.of(owner.getUUID()));
     if(!primitive){
      data.hungerSaturation=0;data.hungerInventory.add(new ItemStack(Items.BEEF,3));owner.getFoodData().setSaturation(0);
      h.assertTrue(TameCommands.prepareHungerForUltimine(data,pet,1),"stored food remains fallback when owner's saturation is empty");
      h.assertTrue(data.hungerInventory.isEmpty()||data.hungerInventory.get(0).getCount()<3,"fallback consumes stored food");
     }
     data.rosterBodyguard=false;data.mode=TameMode.DEFAULT.id();data.hungerInventory.clear();data.hungerSaturation=0;owner.getFoodData().setSaturation(2);
     TameFoodManager.tick(pet,data);
     h.assertTrue(data.hungerSaturation==0&&owner.getFoodData().getSaturationLevel()==2,"ordinary pets never drain owner's saturation");
    } finally {DuelFollowerPause.end(h.level.getServer(),Set.of(owner.getUUID()));TameRegistry.remove(data.uuid);pet.discard();}
   }
  } finally {owner.getFoodData().setSaturation(saved);}
  org.apache.logging.log4j.LogManager.getLogger("BodyguardFoodRegression").info("BODYGUARD_FOOD_TESTS_PASS");
 }
}
