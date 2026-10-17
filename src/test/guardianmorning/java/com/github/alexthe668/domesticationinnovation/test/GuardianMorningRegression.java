package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianMorningRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    private static Map<UUID,ServerPlayer> online(GameTestHelper helper)throws Exception{
        var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);
        return (Map<UUID,ServerPlayer>)f.get(helper.getLevel().getServer().getPlayerList());
    }
    private static Wolf guardian(GameTestHelper helper,UUID owner,long day){
        Wolf wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(owner);wolf.setNoGravity(true);
        var post=helper.absolutePos(new BlockPos(2,2,2));wolf.setPos(post.getX()+10.5,post.getY(),post.getZ()+.5);
        helper.getLevel().addFreshEntity(wolf);
        TameData data=new TameData(wolf);data.name="Morning Guardian";data.movementOrder=3;data.hasHome=true;data.hungerSaturation=500;
        data.homeDimension=helper.getLevel().dimension().location().toString();data.homeX=post.getX();data.homeY=post.getY();data.homeZ=post.getZ();
        data.guardianMorningReturnDay=day;
        data.guardianRestHome.putString("dimension",data.homeDimension);data.guardianRestHome.putDouble("x",post.getX()+.5);
        data.guardianRestHome.putDouble("y",post.getY());data.guardianRestHome.putDouble("z",post.getZ()+.5);
        TameRegistry.register(data);LoadedTameIndex.refresh(wolf);return wolf;
    }
    private static boolean atPost(Wolf wolf,TameData data){return wolf.distanceToSqr(data.homeX+.5,data.homeY,data.homeZ+.5)<.01;}

    @GameTest(template="empty",batch="guardian-morning")
    public static void dawnWaitsForCombatAndReturnsOnceWithoutFoodCharge(GameTestHelper helper)throws Exception{
        var world=helper.getLevel().getServer().overworld();long oldTime=world.getDayTime();long day=Math.floorDiv(oldTime,24000L);
        UUID ownerId=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(ownerId,"DawnOwner"));
        online(helper).put(ownerId,owner);Wolf wolf=guardian(helper,ownerId,day);TameData data=TameRegistry.get(wolf.getUUID());
        var enemy=EntityType.ZOMBIE.create(helper.getLevel());
        GuardianActivityService.rebuild(world.getServer());
        try{
            world.setDayTime(day*24000L+23999);
            check(!TameCommands.processGuardianMorningReturn(wolf,data)&&!atPost(wolf,data),"no early return before dawn");
            world.setDayTime((day+1)*24000L);wolf.setTarget(enemy);
            check(!TameCommands.processGuardianMorningReturn(wolf,data)&&!atPost(wolf,data),"live enemy must defer morning return");
            var saved=data.toTag();check(TameData.fromTag(saved).guardianMorningReturnDay==day,"pending morning survives serialization");
            saved.remove("guardianMorningReturnDay");check(TameData.fromTag(saved).guardianMorningReturnDay==Long.MIN_VALUE,"old saves migrate without fabricated pending day");
            wolf.setTarget(null);wolf.hurtTime=2;
            check(!TameCommands.processGuardianMorningReturn(wolf,data),"incoming-hit animation must defer return");wolf.hurtTime=0;
            check(TameCommands.processGuardianMorningReturn(wolf,data)&&atPost(wolf,data),"first idle check after combat must return to post");
            check(data.guardianMorningReturnDay==day+1&&data.hungerSaturation==500,"return marks morning complete and charges no food");
            wolf.setPos(data.homeX+10.5,data.homeY,data.homeZ+.5);
            check(!TameCommands.processGuardianMorningReturn(wolf,data)&&!atPost(wolf,data),"only one return per morning");
            world.setDayTime(0);check(!TameCommands.processGuardianMorningReturn(wolf,data)&&data.guardianMorningReturnDay==0,"backward time resets baseline");
            data.guardianMorningReturnDay=Long.MIN_VALUE;check(!TameCommands.processGuardianMorningReturn(wolf,data),"old guardian initializes without immediate teleport");
            world.setDayTime(24000);data.hungerSaturation=0;
            check(!TameCommands.processGuardianMorningReturn(wolf,data),"morning must not override hunger rest");
        }finally{world.setDayTime(oldTime);online(helper).remove(ownerId);TameRegistry.remove(data.uuid);wolf.discard();enemy.discard();}
        helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-morning")
    public static void sharedReturnBudgetAndUnloadedPendingGuardians(GameTestHelper helper)throws Exception{
        var server=helper.getLevel().getServer();var world=server.overworld();long oldTime=world.getDayTime();
        long day=Math.floorDiv(oldTime,24000L);world.setDayTime((day+1)*24000L);
        UUID ownerId=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(ownerId,"DawnBudget"));online(helper).put(ownerId,owner);
        List<Wolf> wolves=new ArrayList<>();
        TameData unloaded=null;
        try{
            for(int i=0;i<10;i++)wolves.add(guardian(helper,ownerId,day));
            Wolf cold=EntityType.WOLF.create(helper.getLevel());cold.setTame(true);cold.setOwnerUUID(ownerId);
            unloaded=new TameData(cold);unloaded.hasHome=true;unloaded.movementOrder=3;unloaded.homeDimension=helper.getLevel().dimension().location().toString();
            unloaded.homeX=500000;unloaded.homeZ=500000;unloaded.homeY=70;unloaded.guardianMorningReturnDay=day;TameRegistry.register(unloaded);
            GuardianActivityService.rebuild(server);
            int returned=0;for(Wolf wolf:wolves)if(TameCommands.processGuardianMorningReturn(wolf,TameRegistry.get(wolf.getUUID())))returned++;
            check(returned==GuardianWorkBudget.MAX_TRANSITIONS_PER_TICK,"morning returns must share global transition limit");
            GuardianActivityService.maintain(server);
            check(unloaded.guardianMorningReturnDay==day,"unloaded guardian must retain pending morning");
            var wake=GuardianActivityService.class.getDeclaredField("WAKE_TICKETS");wake.setAccessible(true);
            check(((Map<?,?>)wake.get(null)).isEmpty(),"pending morning must not request source chunks for unloaded guardians");
        }finally{world.setDayTime(oldTime);online(helper).remove(ownerId);for(Wolf wolf:wolves){TameRegistry.remove(wolf.getUUID());wolf.discard();}if(unloaded!=null)TameRegistry.remove(unloaded.uuid);}
        helper.succeed();
    }
}
