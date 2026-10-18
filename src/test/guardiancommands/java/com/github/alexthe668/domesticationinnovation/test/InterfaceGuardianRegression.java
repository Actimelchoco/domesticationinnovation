package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameBehaviorEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class InterfaceGuardianRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    private static Map<UUID,ServerPlayer> players(GameTestHelper h)throws Exception{
        var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);
        return (Map<UUID,ServerPlayer>)f.get(h.getLevel().getServer().getPlayerList());
    }
    private static ServerPlayer owner(GameTestHelper h)throws Exception{
        UUID id=UUID.randomUUID();var owner=FakePlayerFactory.get(h.getLevel(),new GameProfile(id,"InterfaceGuard"));
        var post=h.absolutePos(new BlockPos(2,2,2));owner.setPos(post.getX()+.5,post.getY(),post.getZ()+.5);players(h).put(id,owner);return owner;
    }
    private static PathfinderMob pet(GameTestHelper h,ServerPlayer owner,EntityType<? extends PathfinderMob> type,String name){
        PathfinderMob pet=type.create(h.getLevel());var modified=(ModifedToBeTameable)pet;modified.setTame(true);modified.setTameOwnerUUID(owner.getUUID());
        pet.setPos(owner.getX()+8,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(pet);
        TameData data=new TameData(pet,owner.getUUID(),true);data.name=name;data.group="interface";data.hungerSaturation=500;TameRegistry.register(data);LoadedTameIndex.refresh(pet);return pet;
    }
    private static int run(GameTestHelper h,ServerPlayer owner,String command)throws Exception{
        return h.getLevel().getServer().getCommands().getDispatcher().execute("tames "+command,owner.createCommandSourceStack());
    }
    private static void clean(GameTestHelper h,ServerPlayer owner,PathfinderMob... pets)throws Exception{
        players(h).remove(owner.getUUID());for(var pet:pets){TameRegistry.remove(pet.getUUID());pet.discard();}
    }
    @GameTest(template="empty",batch="interface-guardian")
    public static void fixedInterfaceCommandsIgnoreExternalInversion(GameTestHelper h)throws Exception{
        var owner=owner(h);var frog=pet(h,owner,EntityType.FROG,"FrogPost");var data=TameRegistry.get(frog.getUUID());
        boolean wasInverted=TameRegistry.isCallOrderInvertedType("minecraft:frog");
        if(!wasInverted)TameRegistry.toggleCallOrderInvertedType("minecraft:frog");
        try{
            check(run(h,owner,"guardian set FrogPost")==1,"assign interface guardian");
            check(data.hasHome&&data.movementOrder==3&&((IComandableMob)frog).getCommand()==0,"interface guardian must roam at post, never follow owner under inverted mapping");
            ((IComandableMob)frog).setCommand(2);TameCommands.enforceGuardianCommandLock(frog,data);
            check(((IComandableMob)frog).getCommand()==0,"native follow correction uses interface contract");
            check(run(h,owner,"release sit FrogPost")==1&&((IComandableMob)frog).getCommand()==1,"interface release sit");
            check(run(h,owner,"follow FrogPost")==1&&((IComandableMob)frog).getCommand()==2,"interface follow uses fixed contract");
        }finally{if(!wasInverted)TameRegistry.toggleCallOrderInvertedType("minecraft:frog");clean(h,owner,frog);}h.succeed();
    }
    @GameTest(template="empty",batch="interface-guardian")
    public static void returnPostReplacesStaleBrainWalkTarget(GameTestHelper h)throws Exception{
        var owner=owner(h);var frog=pet(h,owner,EntityType.FROG,"BrainPost");var data=TameRegistry.get(frog.getUUID());
        try{
            check(run(h,owner,"guardian set BrainPost")==1,"assign frog");
            frog.getBrain().setMemory(MemoryModuleType.WALK_TARGET,new WalkTarget(frog.blockPosition().offset(15,0,0),1F,1));
            var method=TameBehaviorEvents.class.getDeclaredMethod("handleGuardianReturn",PathfinderMob.class,TameData.class);method.setAccessible(true);method.invoke(null,frog,data);
            var target=frog.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElseThrow();
            check(target.getTarget().currentBlockPosition().equals(new BlockPos(data.homeX,data.homeY,data.homeZ)),"guardian return must own brain walk target, not only navigation");
        }finally{clean(h,owner,frog);}h.succeed();
    }
    @GameTest(template="empty",batch="interface-guardian")
    public static void bulkGuardianAssignmentIncludesInterfaceTames(GameTestHelper h)throws Exception{
        var owner=owner(h);var frog=pet(h,owner,EntityType.FROG,"BulkFrog");var fox=pet(h,owner,EntityType.FOX,"BulkFox");
        try{
            for(String selector:List.of("all","group interface","type frog","state follow")){
                check(run(h,owner,"guardian set "+selector)==1,"bulk assignment command "+selector);
                check(TameRegistry.get(frog.getUUID()).hasHome,"interface frog included in "+selector);
                if(!selector.equals("type frog"))check(TameRegistry.get(fox.getUUID()).hasHome,"interface fox included in "+selector);
                run(h,owner,"release follow all");
            }
        }finally{clean(h,owner,frog,fox);}h.succeed();
    }
}
