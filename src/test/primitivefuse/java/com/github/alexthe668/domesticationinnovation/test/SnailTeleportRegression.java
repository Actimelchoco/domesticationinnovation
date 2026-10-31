package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
public final class SnailTeleportRegression {
 @SuppressWarnings("unchecked") static void verify(PrimitiveFuseRegression.Fixture h,ServerPlayer owner)throws Exception {
  var typeId=new ResourceLocation("crittersandcompanions","snail");if(!ForgeRegistries.ENTITY_TYPES.containsKey(typeId))return;
  var poolField=TameCommands.class.getDeclaredField("RANKED_POOL");poolField.setAccessible(true);var pool=(Set<UUID>)poolField.get(null);
  var pendingField=TameCommands.class.getDeclaredField("PENDING_IMMEDIATE_CHUNK_TELEPORTS");pendingField.setAccessible(true);var pending=(Map<UUID,Object>)pendingField.get(null);
  var targetType=Arrays.stream(TameCommands.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("SpawnTarget")).findFirst().orElseThrow();
  var targetCtor=targetType.getDeclaredConstructors()[0];targetCtor.setAccessible(true);
  var queue=TameCommands.class.getDeclaredMethod("queueImmediateChunkTeleport",ServerLevel.class,BlockPos.class,TameData.class,targetType,boolean.class,boolean.class);queue.setAccessible(true);
  var process=TameCommands.class.getDeclaredMethod("processPendingImmediateChunkTeleports",net.minecraft.server.MinecraftServer.class);process.setAccessible(true);
  var original=owner.position();
  try {for(int state=0;state<3;state++) {
   owner.setPos(30,-57,2);
   for(int x=24;x<=36;x++)for(int z=-4;z<=8;z++){
    h.level.setBlock(new BlockPos(x,-58,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
    h.level.setBlock(new BlockPos(x,-57,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
    h.level.setBlock(new BlockPos(x,-56,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
   }
   var pet=(TamableAnimal)ForgeRegistries.ENTITY_TYPES.getValue(typeId).create(h.level);pet.setTame(true);pet.setOwnerUUID(owner.getUUID());pet.setOrderedToSit(false);pet.setNoAi(true);pet.setPos(2,-57,2);h.level.addFreshEntity(pet);
   var data=new TameData(pet,owner.getUUID(),false);data.hungerSaturation=100000;TameRegistry.register(data);TameRegistry.bindEntityToData(pet,data);
   var follow=(FollowOwnerGoal)pet.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof FollowOwnerGoal).findFirst().orElseThrow();
   h.assertTrue(follow.canUse(),"native snail follow starts before lock");follow.start();
   // Queue the same native chunk-transfer job used by automatic owner following.
   queue.invoke(null,h.level,pet.blockPosition(),data,targetCtor.newInstance(h.level,owner.position(),owner.getYRot(),owner.getXRot()),true,true);
   var job=pending.get(data.tlId);h.assertTrue(job!=null,"follow teleport queued before duel/ranked lock");
   var auto=job.getClass().getDeclaredField("autoFollow");auto.setAccessible(true);auto.setBoolean(job,true);
   var enemy=EntityType.WOLF.create(h.level);enemy.setTame(true);enemy.setOwnerUUID(UUID.randomUUID());enemy.setNoAi(true);enemy.setPos(5,-57,2);h.level.addFreshEntity(enemy);var ed=new TameData(enemy,enemy.getOwnerUUID(),false);ed.hungerSaturation=100000;TameRegistry.register(ed);
   try {
    if(state==0)pool.add(pet.getUUID());else TameDuelManager.startTeamDuel(h.level.getServer(),owner.getUUID(),Set.of(pet.getUUID()),ed.ownerUUID,Set.of(enemy.getUUID()),Set.of(),false,state==2);
    var before=pet.position();for(int i=0;i<30;i++)follow.tick();
    h.assertTrue(pet.position().distanceToSqr(before)<.01,"native snail teleport remains blocked while waiting/in regular or ranked duel");
    boolean entryBlocked=!TameCommands.autoFollowTeleportViaTpPath(owner,data,h.level,owner.position(),0,0);
    pet.setPos(before); // Test queued execution independently of the immediate entry.
    ((net.minecraft.world.level.storage.ServerLevelData)h.level.getLevelData()).setGameTime(h.level.getGameTime()+200);
    process.invoke(null,h.level.getServer());
    boolean queuedBlocked=pet.position().distanceToSqr(before)<.01;
    org.apache.logging.log4j.LogManager.getLogger("SnailTeleportRegression").info("SNAIL_TELEPORT_BASELINE state={} entryBlocked={} queuedBlocked={}",state,entryBlocked,queuedBlocked);
    h.assertTrue(entryBlocked,"automatic teleport entry respects ranked waiting and duel locks");
    h.assertTrue(queuedBlocked,"a queued owner teleport cannot pull a snail out of ranked or a duel");
    h.assertTrue(!TameCommands.hasPendingImmediateChunkTeleport(data),"blocked pending teleport is cancelled");
    if(state==0){pool.remove(pet.getUUID());follow.stop();h.assertTrue(follow.canUse(),"normal following is available after ranked clears");follow.start();for(int i=0;i<50&&pet.distanceToSqr(owner)>64;i++)follow.tick();h.assertTrue(pet.distanceToSqr(owner)<=64,"ordinary snail owner teleport resumes outside locks");}
   }finally{
    pool.remove(pet.getUUID());TameDuelManager.endDuelsForOwner(h.level.getServer(),owner.getUUID());TameCommands.cancelPendingImmediateChunkTeleport(h.level.getServer(),data);
    for(var d:List.of(data,ed)){var live=h.level.getEntity(d.uuid);if(live!=null)live.discard();TameRegistry.remove(d.uuid);}
    pet.discard();enemy.discard();
   }
  }}finally{owner.setPos(original);}
  org.apache.logging.log4j.LogManager.getLogger("SnailTeleportRegression").info("SNAIL_TELEPORT_TESTS_PASS");
 }
}
