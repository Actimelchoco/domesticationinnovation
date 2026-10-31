package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import java.util.*;
public final class DamageScalingAudit {
 static class Hits {
  final Mob pet;float amount;
  Hits(Mob pet){this.pet=pet;}
  @SubscribeEvent(priority=EventPriority.LOWEST)public void hit(LivingHurtEvent e){if(e.getSource().getEntity()==pet)amount=e.getAmount();}
 }
 static void verify(PrimitiveFuseRegression.Fixture h)throws Exception {
  for(String species:List.of("minecraft:wolf","minecraft:fox","primitive_mobs:baby_spider","primitive_mobs:support_creeper")) {
   var pet=(Mob)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(species)).create(h.level);
   UUID owner=UUID.randomUUID();
   if(pet instanceof TamableAnimal animal){animal.setTame(true);animal.setOwnerUUID(owner);}else{var modified=(ModifedToBeTameable)pet;modified.setTame(true);modified.setTameOwnerUUID(owner);}
   pet.setNoAi(true);pet.setPos(2,-57,2);h.level.addFreshEntity(pet);var data=new TameData(pet,owner,false);data.hungerSaturation=100000;TameRegistry.register(data);TameRegistry.bindEntityToData(pet,data);
   var target=EntityType.HUSK.create(h.level);target.setNoAi(true);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);target.setPos(3,-57,2);h.level.addFreshEntity(target);
   var hits=new Hits(pet);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(hits);
   float baseline=0,boosted=0;
   try {
    for(int bonus:List.of(0,10)) {
     data.bonusDamage=bonus;LevelSystem.reapplyTypeBasePlusBonuses(pet,data);
     target.invulnerableTime=0;hits.amount=0;
     if(species.endsWith("creeper")) target.hurt(h.level.damageSources().explosion(pet,pet),10);else pet.doHurtTarget(target);
     if(bonus==0)baseline=hits.amount;else boosted=hits.amount;
    }
    org.apache.logging.log4j.LogManager.getLogger("DamageScalingAudit").info("DAMAGE_SCALING_AUDIT species={} bonus=10 baseline={} boosted={} gain={}",species,baseline,boosted,boosted-baseline);
    h.assertTrue(Math.abs(boosted-baseline-10)<.001,"earned damage is added once for "+species);
   }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(hits);TameRegistry.remove(data.uuid);pet.discard();target.discard();}
  }
  org.apache.logging.log4j.LogManager.getLogger("DamageScalingAudit").info("DAMAGE_SCALING_AUDIT_PASS");
 }
}
