package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAutoFollowEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class DuelFollowerRegression {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") static Map<UUID,ServerPlayer> online(GameTestHelper h){
        try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);return (Map<UUID,ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());}
        catch(Exception e){throw new RuntimeException(e);}
    }
    @SuppressWarnings("unchecked") static List<ServerPlayer> players(GameTestHelper h){
        try{var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("players");field.setAccessible(true);return (List<ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());}
        catch(Exception e){throw new RuntimeException(e);}
    }
    static class Fixture implements AutoCloseable{
        final GameTestHelper h;final FakePlayer a,b;final List<LivingEntity> pets=new ArrayList<>();final Object source=new Object();
        Fixture(GameTestHelper h){this.h=h;a=player("WaitA");b=player("WaitB");}
        FakePlayer player(String name){
            var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),name));p.moveTo(h.absolutePos(new BlockPos(2,2,2)),0,0);
            p.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p);
            online(h).put(p.getUUID(),p);players(h).add(p);h.getLevel().players().add(p);return p;
        }
        Wolf wolf(UUID owner){var pet=EntityType.WOLF.create(h.getLevel());pet.setTame(true);pet.setOwnerUUID(owner);register(pet,owner);return pet;}
        void register(LivingEntity pet,UUID owner){pet.moveTo(h.absolutePos(new BlockPos(3,2,2)),0,0);h.getLevel().addFreshEntity(pet);TameData data=new TameData(pet,owner,false);data.level=20;TameRegistry.register(data);LoadedTameIndex.refresh(pet);pets.add(pet);}
        void start(Set<UUID> first,Set<UUID> second){TameDuelManager.startTeamDuel(h.getLevel().getServer(),a.getUUID(),first,b.getUUID(),second);check(TameDuelManager.isEntityInDuel(first.iterator().next()),"battle started");}
        void end(){TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),a.getUUID());}
        public void close(){end();GuardianChestBodyguards.remove(source);GuardianChestBodyguards.clear();for(var pet:pets){var data=TameRegistry.get(pet.getUUID());var body=data==null?null:TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);TameRegistry.remove(pet.getUUID());if(body!=null)body.discard();pet.discard();}for(var p:List.of(a,b)){online(h).remove(p.getUUID());players(h).remove(p);h.getLevel().players().remove(p);}}
    }

    @GameTest(template="empty") public static void playerFightersPauseFollowersAndBothKindsOfBodyguard(GameTestHelper h){
        try(var f=new Fixture(h)){
            var follower=f.wolf(f.a.getUUID());var bodyguard=f.wolf(f.a.getUUID());var data=TameRegistry.get(bodyguard.getUUID());data.rosterBodyguard=true;data.mode=TameMode.BODYGUARD.id();
            var post=f.wolf(f.a.getUUID());var postData=TameRegistry.get(post.getUUID());postData.hasHome=true;postData.movementOrder=3;
            var chestGuard=f.wolf(UUID.randomUUID());var cg=TameRegistry.get(chestGuard.getUUID());cg.hasHome=true;cg.movementOrder=3;cg.homeDimension=h.getLevel().dimension().location().toString();
            CompoundTag anchor=new CompoundTag();anchor.putString("dimension",cg.homeDimension);anchor.putInt("x",cg.homeX);anchor.putInt("y",cg.homeY);anchor.putInt("z",cg.homeZ);cg.guardianSetAnchors.put("wait",anchor);
            GuardianChestBodyguards.clear();GuardianChestBodyguards.replace(f.source,Map.of(cg.tlId,new GuardianChestBodyguards.Rule(cg.ownerUUID,"wait",f.a.getGameProfile().getName())));GuardianChestBodyguards.refresh(h.getLevel().getServer());
            check(GuardianChestBodyguards.protectedPlayerId(cg).equals(f.a.getUUID()),"cross-owner guardian bodyguard selected");
            var fox=EntityType.FOX.create(h.getLevel());var modified=(ModifedToBeTameable)fox;modified.setTame(true);modified.setTameOwnerUUID(f.a.getUUID());((IComandableMob)fox).setCommand(2);f.register(fox,f.a.getUUID());
            var outsider=f.wolf(UUID.randomUUID());var fighter=f.wolf(f.a.getUUID());fighter.setNoAi(true);
            f.start(Set.of(f.a.getUUID(),fighter.getUUID()),Set.of(f.b.getUUID()));
            for(var pet:List.of(follower,bodyguard,chestGuard))check(pet.isOrderedToSit()&&pet.isNoAi()&&pet.getTarget()==null,"following pets must wait sitting with AI paused");
            check(fox.isNoAi()&&((IComandableMob)fox).getCommand()==1,"interface follower must wait too");
            check(!post.isNoAi()&&!post.isOrderedToSit()&&!outsider.isNoAi(),"static guardians and unrelated pets are unaffected");
            check(!fighter.isOrderedToSit(),"selected tame fighter is not paused");
            com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.syncLiveMovementStateFor(follower);
            com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.enforceGuardianCommandLock(chestGuard,cg);
            check(TameRegistry.get(follower.getUUID()).movementOrder==0&&chestGuard.isOrderedToSit(),"native state synchronization must not overwrite orders or unseat paused guardians");
            check(data.rosterBodyguard&&data.mode==TameMode.BODYGUARD.id()&&data.movementOrder==0&&cg.hasHome&&cg.movementOrder==3,"saved assignments and orders remain intact");
            check(!TameAutoFollowEvents.isFollowing(TameRegistry.get(follower.getUUID())),"automatic owner teleport follow is blocked");
            f.end();
            for(var pet:List.of(follower,bodyguard,chestGuard))check(!pet.isOrderedToSit()&&!pet.isNoAi(),"followers resume after duel ends");
            check(!fox.isNoAi()&&((IComandableMob)fox).getCommand()==2,"interface command restored");
            check(cg.movementOrder==3&&data.rosterBodyguard,"guardian/bodyguard assignments survive restoration");
        }h.succeed();
    }

    @GameTest(template="empty") public static void tameOnlyBattleDoesNotPauseOwnerFollowers(GameTestHelper h){
        try(var f=new Fixture(h)){var follower=f.wolf(f.a.getUUID());var first=f.wolf(f.a.getUUID());var second=f.wolf(f.b.getUUID());first.setNoAi(true);second.setNoAi(true);f.start(Set.of(first.getUUID()),Set.of(second.getUUID()));check(!follower.isNoAi()&&!follower.isOrderedToSit(),"owner watching a tame-only duel keeps followers active");}h.succeed();
    }

    @GameTest(template="empty") public static void lateFollowerAndUnloadedSavedPauseRestore(GameTestHelper h){
        try(var f=new Fixture(h)){
            f.start(Set.of(f.a.getUUID()),Set.of(f.b.getUUID()));var follower=f.wolf(f.a.getUUID());var data=TameRegistry.get(follower.getUUID());
            check(DuelFollowerPause.tick(follower,data)&&follower.isNoAi(),"newly loaded follower pauses during ongoing duel");
            CompoundTag snapshot=new CompoundTag();follower.save(snapshot);follower.discard();f.end();
            var restored=EntityType.WOLF.create(h.getLevel());restored.load(snapshot);h.getLevel().addFreshEntity(restored);f.pets.add(restored);
            check(!restored.isNoAi()&&!restored.isOrderedToSit(),"saved temporary pause clears when body loads after duel ended");
        }h.succeed();
    }

    @GameTest(template="empty") public static void existingSitAndNoAiSurvivePause(GameTestHelper h){
        try(var f=new Fixture(h)){var pet=f.wolf(f.a.getUUID());pet.setNoAi(true);pet.setOrderedToSit(true);pet.setInSittingPose(true);f.start(Set.of(f.a.getUUID()),Set.of(f.b.getUUID()));f.end();check(pet.isNoAi()&&pet.isOrderedToSit()&&pet.isInSittingPose(),"original sitting and disabled AI must be restored exactly");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=140) public static void pauseSurvivesMaintenanceAndPlayerElimination(GameTestHelper h){
        var f=new Fixture(h);var pet=f.wolf(f.a.getUUID());pet.setNoGravity(true);var data=TameRegistry.get(pet.getUUID());data.rosterBodyguard=true;data.mode=TameMode.BODYGUARD.id();
        var fighter=f.wolf(f.a.getUUID());fighter.setNoAi(true);fighter.setNoGravity(true);
        f.start(Set.of(f.a.getUUID(),fighter.getUUID()),Set.of(f.b.getUUID()));
        TameDuelManager.endDuelForEntity(h.getLevel().getServer(),f.a.getUUID());
        h.runAfterDelay(80,()->{
            try{
                check(pet.isNoAi()&&pet.isOrderedToSit()&&pet.getTarget()==null,"bodyguard stays paused through multiple maintenance cycles until the whole battle ends");
                check(data.rosterBodyguard&&data.movementOrder==0&&data.mode==TameMode.BODYGUARD.id(),"native sitting must not overwrite saved follow/bodyguard settings");
                check(TameEntityAdapter.isStayingStill(pet)&&!TameAutoFollowEvents.isFollowing(data),"ability and teleport helpers respect the pause");
                f.end();check(!pet.isNoAi()&&!pet.isOrderedToSit(),"bodyguard resumes when remaining battle ends");
            }finally{f.close();}h.succeed();
        });
    }

}
