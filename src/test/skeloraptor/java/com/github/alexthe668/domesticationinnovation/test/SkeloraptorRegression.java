package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class SkeloraptorRegression {
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final FakePlayer owner;final TamableAnimal raptor,wolf;final UUID opponent=UUID.randomUUID();
        Fixture(GameTestHelper h,boolean ranked){this(h,ranked,true);}
        Fixture(GameTestHelper h,boolean ranked,boolean duel){
            this.h=h;owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"RaptorFixture"));
            owner.setPos(h.absolutePos(new BlockPos(1,2,1)).getX(),h.absolutePos(new BlockPos(1,2,1)).getY(),h.absolutePos(new BlockPos(1,2,1)).getZ());
            h.getLevel().players().add(owner);
            if(owner.connection==null) owner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),owner);
            var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("legendary_monsters:skeloraptor"));
            if(type==null)throw new AssertionError("real Skeloraptor type missing");
            raptor=(TamableAnimal)type.create(h.getLevel());wolf=EntityType.WOLF.create(h.getLevel());
            int x=2;for(var pet:List.of(raptor,wolf)){
                pet.setTame(true);pet.setOwnerUUID(owner.getUUID());pet.setNoAi(true);pet.setNoGravity(true);
                pet.moveTo(h.absolutePos(new BlockPos(x++,2,2)),0,0);
                h.getLevel().addFreshEntity(pet);var data=new TameData(pet);data.level=20;data.name="RaptorFixture"+x;TameRegistry.register(data);LoadedTameIndex.refresh(pet);
            }
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);wolf.setHealth(1000);
            if(duel){TameDuelManager.startTeamDuel(h.getLevel().getServer(),owner.getUUID(),Set.of(raptor.getUUID()),opponent,Set.of(wolf.getUUID()),Set.of(),false,ranked);
            h.assertTrue(TameDuelManager.areDuelOpponents(raptor.getUUID(),wolf.getUUID()),"real opposing participants registered");}
        }
        void swing()throws Exception{
            var method=raptor.getClass().getDeclaredMethod("AreaAttack",float.class,float.class,float.class,float.class,int.class,boolean.class,float.class,boolean.class,float.class,boolean.class);
            method.setAccessible(true);method.invoke(raptor,6F,5F,360F,10F,0,false,0F,false,0F,false);
        }
        public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());
            for(var pet:List.of(raptor,wolf)){var data=TameRegistry.get(pet.getUUID());var body=data==null?null:TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);TameRegistry.remove(pet.getUUID());if(body!=null)body.discard();pet.discard();}
            h.getLevel().players().remove(owner);
        }
    }
    @GameTest(template="empty") public static void nativeSkeloraptorSwingDamagesDuelOpponent(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,false)){
            float before=f.wolf.getHealth();f.swing();
            h.assertTrue(f.wolf.getHealth()<before,"native swing must damage duel opponent: allied="+f.raptor.isAlliedTo(f.wolf)+", command="+f.raptor.getClass().getMethod("getCommand").invoke(f.raptor)+", health="+f.wolf.getHealth());
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeSkeloraptorSwingDamagesRankedOpponent(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,true)){float before=f.wolf.getHealth();f.swing();h.assertTrue(f.wolf.getHealth()<before,"ranked opponent receives native attack damage");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160) public static void nativeAttackGoalsActuallyDamageOpponent(GameTestHelper h){
        var f=new Fixture(h,false);float before=f.wolf.getHealth();
        floor(h);f.raptor.setNoGravity(false);f.wolf.setNoGravity(false);f.raptor.setNoAi(false);
        h.runAfterDelay(110,()->{
            try{h.assertTrue(f.wolf.getHealth()<before,"native attack AI must hit opponent on server ticks: health="+f.wolf.getHealth()+", target="+f.raptor.getTarget()+", sit="+f.raptor.isOrderedToSit());}
            finally{f.close();}h.succeed();
        });
    }
    @GameTest(template="empty") public static void nonparticipantsRemainProtected(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,false)){
            var friend=EntityType.WOLF.create(h.getLevel());friend.setTame(true);friend.setOwnerUUID(f.owner.getUUID());friend.setNoAi(true);friend.setPos(f.wolf.getX(),f.wolf.getY(),f.wolf.getZ());h.getLevel().addFreshEntity(friend);
            try{float before=friend.getHealth();f.swing();h.assertTrue(friend.getHealth()==before&&f.raptor.isAlliedTo(friend),"same-owner nonparticipant keeps native ally protection");}
            finally{TameRegistry.remove(friend.getUUID());friend.discard();}
        }h.succeed();
    }

    static void floor(GameTestHelper h){for(int x=0;x<=22;x++)for(int z=0;z<=6;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());}
    static void chase(GameTestHelper h,boolean ranked){
        var f=new Fixture(h,ranked);
        floor(h);
        var destination=h.absolutePos(new BlockPos(15,2,2));f.wolf.setPos(destination.getX(),destination.getY(),destination.getZ());
        f.raptor.setNoGravity(false);f.wolf.setNoGravity(false);f.raptor.setNoAi(false);f.raptor.getRandom().setSeed(7123);
        float health=f.wolf.getHealth();double initial=f.raptor.distanceTo(f.wolf);
        StringBuilder trace=new StringBuilder();
        for(int step=20;step<=100;step+=20){final int sample=step;h.runAfterDelay(step,()->{
            try{trace.append(" [t=").append(sample).append(" pos=").append(f.raptor.position()).append(" state=").append(f.raptor.getClass().getMethod("getAttackState").invoke(f.raptor)).append(" travel=").append(f.raptor.getClass().getMethod("canTravel").invoke(f.raptor)).append(" speed=").append(f.raptor.getAttributeValue(Attributes.MOVEMENT_SPEED)).append(" actualSpeed=").append(f.raptor.getSpeed()).append(" path=").append(f.raptor.getNavigation().getPath()==null?"null":f.raptor.getNavigation().getPath().getNextNodeIndex()+"/"+f.raptor.getNavigation().getPath().getNodeCount()).append(" running=").append(f.raptor.goalSelector.getRunningGoals().map(g->g.getGoal().getClass().getSimpleName()).toList()).append("]");}
            catch(Exception error){throw new RuntimeException(error);}
        });}
        h.runAfterDelay(800,()->{
            try{h.assertTrue(f.wolf.getHealth()<health,"Skeloraptor must chase and hit a distant opponent: initialDistance="+initial+", finalDistance="+f.raptor.distanceTo(f.wolf)+", health="+f.wolf.getHealth()+", navigationDone="+f.raptor.getNavigation().isDone()+", target="+f.raptor.getTarget()+trace);}
            finally{f.close();}h.succeed();
        });
    }
    @GameTest(template="empty",batch="chaseDuel",timeoutTicks=860) public static void chasesDistantDuelOpponent(GameTestHelper h){chase(h,false);}
    @GameTest(template="empty",batch="chaseRanked",timeoutTicks=860) public static void chasesDistantRankedOpponent(GameTestHelper h){chase(h,true);}

    @GameTest(template="empty") public static void chaseGoalIsRemovedWhenDuelEnds(GameTestHelper h){
        try(var f=new Fixture(h,false)){
            h.assertTrue(f.raptor.goalSelector.getAvailableGoals().stream().anyMatch(g->g.getGoal().getClass()==com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.SkeloraptorDuelChaseGoal.class),"chase installed only for participating raptor");
            TameDuelManager.endDuelForEntity(h.getLevel().getServer(),f.raptor.getUUID());
            h.assertTrue(f.raptor.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal().getClass()==com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.SkeloraptorDuelChaseGoal.class),"old body has no remaining duel chase goal");
            var data=TameRegistry.get(f.raptor.getUUID());var restored=(net.minecraft.world.entity.Mob)TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);
            h.assertTrue(restored!=null&&restored.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal().getClass()==com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.SkeloraptorDuelChaseGoal.class),"restored body returns to native AI");
        }h.succeed();
    }

    @GameTest(template="empty") public static void repeatedDuelMovementRefreshPreservesNativePath(GameTestHelper h){
        try(var f=new Fixture(h,false)){
            floor(h);var pos=h.absolutePos(new BlockPos(15,2,2));f.wolf.setPos(pos.getX(),pos.getY(),pos.getZ());
            f.raptor.setOnGround(true);
            h.assertTrue(f.raptor.getNavigation().moveTo(f.wolf,1.0),"native navigation finds path");
            for(int i=0;i<5;i++)com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.applyDuelMovementOrder(f.raptor,true);
            h.assertTrue(!f.raptor.getNavigation().isDone(),"unchanged duel command must not stop native navigation");
        }h.succeed();
    }

    @GameTest(template="empty",batch="normalChase",timeoutTicks=860)
    public static void monsterHunterApproachesAndAttacksNormally(GameTestHelper h){
        var f=new Fixture(h,false,false);floor(h);
        var enemy=EntityType.HUSK.create(h.getLevel());var pos=h.absolutePos(new BlockPos(11,2,2));
        enemy.moveTo(pos,0,0);enemy.setNoAi(true);enemy.setPersistenceRequired();
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);enemy.setHealth(1000);h.getLevel().addFreshEntity(enemy);
        var data=TameRegistry.get(f.raptor.getUUID());data.mode=TameMode.MONSTER_HUNTER.id();data.movementOrder=2;
        com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller.installIfMissing(f.raptor);
        try{f.raptor.getClass().getMethod("setCommand",int.class).invoke(f.raptor,2);}catch(Exception e){throw new RuntimeException(e);}
        f.raptor.setOrderedToSit(false);f.raptor.setNoGravity(false);f.raptor.setNoAi(false);f.raptor.getRandom().setSeed(7123);
        h.runAfterDelay(800,()->{
            try{h.assertTrue(enemy.getHealth()<1000,"normal Monster Hunter must find, chase and hit hostile: health="+enemy.getHealth()+", distance="+f.raptor.distanceTo(enemy)+", target="+f.raptor.getTarget());}
            finally{enemy.discard();f.close();}h.succeed();
        });
    }

    @GameTest(template="empty")
    public static void normalChaseRespectsRestFriendshipAndBoundaries(GameTestHelper h){
        try(var f=new Fixture(h,false,false)){
            var enemy=EntityType.HUSK.create(h.getLevel());var pos=h.absolutePos(new BlockPos(10,2,2));enemy.moveTo(pos,0,0);enemy.setNoAi(true);h.getLevel().addFreshEntity(enemy);
            try{
                var data=TameRegistry.get(f.raptor.getUUID());data.mode=TameMode.MONSTER_HUNTER.id();data.movementOrder=2;
                for(int i=0;i<3;i++)com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller.installIfMissing(f.raptor);
                var goals=f.raptor.goalSelector.getAvailableGoals().stream().map(g->g.getGoal()).filter(g->g instanceof com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.SkeloraptorNormalChaseGoal).toList();
                h.assertTrue(goals.size()==1,"normal chase installs once without a global entity index");var goal=goals.get(0);
                f.raptor.setNoAi(false);f.raptor.setOrderedToSit(false);f.raptor.setTarget(enemy);
                h.assertTrue(goal.canUse(),"normal hostile target can be chased");
                data.guardianResting=true;h.assertTrue(!goal.canUse(),"offline/resting guardian stays inactive");data.guardianResting=false;
                data.hungerForcedSit=true;h.assertTrue(!goal.canUse(),"hungry pet stays inactive");data.hungerForcedSit=false;
                data.mode=TameMode.PASSIVE.id();h.assertTrue(!goal.canUse(),"passive mode does not chase");data.mode=TameMode.MONSTER_HUNTER.id();
                f.raptor.setOrderedToSit(true);h.assertTrue(!goal.canUse(),"sitting pet does not chase");f.raptor.setOrderedToSit(false);
                f.raptor.setTarget(f.wolf);h.assertTrue(!goal.canUse(),"same-owner tame stays protected");f.raptor.setTarget(enemy);
                data.hasProtectionZone=true;data.protectionDimension=h.getLevel().dimension().location().toString();data.protectionX=f.raptor.blockPosition().getX();data.protectionY=f.raptor.blockPosition().getY();data.protectionZ=f.raptor.blockPosition().getZ();data.protectionRadius=4;
                h.assertTrue(!goal.canUse(),"target outside protection zone is not chased");data.hasProtectionZone=false;
                data.mode=TameMode.BODYGUARD.id();data.bodyguardRange=1;h.assertTrue(!goal.canUse(),"bodyguard leash wins over chase");
            }finally{enemy.discard();}
        }h.succeed();
    }

}
