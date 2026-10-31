package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import java.util.*;
public final class OwnerDuelRegression {
 @SuppressWarnings("unchecked") static void verify(PrimitiveFuseRegression.Fixture h)throws Exception {
  var server=h.level.getServer();java.lang.reflect.Field f;
  try{f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");}catch(NoSuchFieldException e){f=net.minecraft.server.players.PlayerList.class.getDeclaredField("f_11197_");}
  f.setAccessible(true);var players=(Map<UUID,ServerPlayer>)f.get(server.getPlayerList());
  for(boolean ranked:List.of(false,true)) {
   var player=new ServerPlayer(server,h.level,new GameProfile(UUID.randomUUID(),"WolfOwner"));
   player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(server,new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player);
   player.setGameMode(GameType.SURVIVAL);player.setNoGravity(true);players.put(player.getUUID(),player);h.level.addNewPlayer(player);
   java.lang.reflect.Field spawn;try{spawn=ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");}catch(NoSuchFieldException e){spawn=ServerPlayer.class.getDeclaredField("f_8921_");}spawn.setAccessible(true);spawn.setInt(player,0);
   player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);player.setHealth(1000);
   BodyguardFoodRegression.verify(h,player);
   SupportBodyguardRegression.verify(h,player);
   var wolf=EntityType.WOLF.create(h.level);wolf.setTame(true);wolf.setOwnerUUID(player.getUUID());wolf.setNoAi(true);wolf.setPos(2,-57,2);player.setPos(3,-57,2);h.level.addFreshEntity(wolf);
   var data=new TameData(wolf,player.getUUID(),false);data.hungerSaturation=100000;TameRegistry.register(data);
   var spectator=EntityType.WOLF.create(h.level);spectator.setTame(true);spectator.setOwnerUUID(player.getUUID());spectator.setNoAi(false);spectator.setNoGravity(true);spectator.setPos(10,-57,2);h.level.addFreshEntity(spectator);
   var guardData=new TameData(spectator,player.getUUID(),false);guardData.rosterBodyguard=true;guardData.mode=TameMode.BODYGUARD.id();guardData.movementOrder=0;guardData.bodyguardMovementProfile="skeleton";guardData.skeletonMovement=true;guardData.hungerSaturation=100000;TameRegistry.register(guardData);LoadedTameIndex.refresh(spectator);
   var roster=BodyguardService.roster(player);roster.bodyguards.add(guardData.ensureTlId());roster.bodyguardRange=2;
   try {
    h.assertTrue(wolf.isAlliedTo(player),"owner starts allied");
    TameDuelManager.startTeamDuel(server,player.getUUID(),Set.of(wolf.getUUID()),UUID.randomUUID(),Set.of(player.getUUID()),Set.of(),false,ranked);
    var guardPosition=spectator.position();player.setPos(100,-57,100);
    for(int i=0;i<4;i++)BodyguardService.refresh(server);
    h.assertTrue(spectator.position().equals(guardPosition),"bodyguard maintenance never teleports a waiting guard into owner's duel");
    h.assertTrue(spectator.isNoAi()&&spectator.isOrderedToSit()&&spectator.isInSittingPose(),"bodyguard remains sitting through roster maintenance");
    h.assertTrue(guardData.movementOrder==0&&guardData.mode==TameMode.BODYGUARD.id()&&guardData.skeletonMovement&&!guardData.closeMovement,"temporary wait preserves bodyguard orders and movement profile");
    player.setPos(3,-57,2);
    boolean opposing=TameDuelManager.areDuelOpponents(wolf.getUUID(),player.getUUID());
    boolean allied=wolf.isAlliedTo(player);
    boolean canAttack=wolf.canAttack(player);
    h.assertTrue(wolf.isAlliedTo(spectator),"nonparticipating pet remains allied during duel");
    for(int x=-2;x<=8;x++)for(int z=-2;z<=6;z++)h.level.setBlock(new net.minecraft.core.BlockPos(x,-58,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
    float aiBefore=player.getHealth();wolf.setNoAi(false);
    for(int i=0;i<65;i++){
     ((net.minecraft.world.level.storage.ServerLevelData)h.level.getLevelData()).setGameTime(h.level.getGameTime()+1);
     wolf.setTarget(player);wolf.setPos(2,-57,2);wolf.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.setPos(3,-57,2);player.invulnerableTime=0;wolf.tick();
    }
    h.assertTrue(player.getHealth()<aiBefore,"native wolf attack AI damages owner without forced melee call");
    wolf.setNoAi(true);player.invulnerableTime=0;
    float before=player.getHealth();
    boolean hit=wolf.doHurtTarget(player);
    float damage=before-player.getHealth();
    org.apache.logging.log4j.LogManager.getLogger("OwnerDuelRegression").info("OWNER_DUEL_BASELINE opposing={} allied={} canAttack={} hit={} damage={}",opposing,allied,canAttack,hit,damage);
    h.assertTrue(opposing&&!allied&&canAttack,"duel opponent must pass native owner/alliance targeting checks");
    h.assertTrue(hit&&damage>0,"native wolf melee damages survival owner in duel");
    TameDuelManager.endDuelsForOwner(server,player.getUUID());
    h.assertTrue(!spectator.isNoAi()&&!spectator.isOrderedToSit(),"bodyguard resumes its native state after duel");
    BodyguardService.refresh(server);
    h.assertTrue(spectator.distanceToSqr(player)<16&&!spectator.position().equals(guardPosition)&&guardData.rosterBodyguard,"bodyguard resumes normal follow teleport after duel");
    h.assertTrue(wolf.isAlliedTo(player)&&!wolf.canAttack(player),"owner alliance and native protection resume after duel");
   } finally {
    TameDuelManager.endDuelsForOwner(server,player.getUUID());TameRegistry.remove(wolf.getUUID());wolf.discard();spectator.discard();TameRegistry.remove(spectator.getUUID());
    h.level.removePlayerImmediately(player,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);players.remove(player.getUUID());TameRegistry.removePlayerDuelStats(player.getUUID());
   }
  }
  org.apache.logging.log4j.LogManager.getLogger("OwnerDuelRegression").info("OWNER_DUEL_TESTS_PASS");
 }
}
