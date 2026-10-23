package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class PostRankedDeathRegression {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        final Wolf first,second;final List<UUID> ids=new ArrayList<>();
        Fixture(GameTestHelper h,boolean ranked){
            this.h=h;first=pet(a,1);second=pet(b,4);
            TameDuelManager.startTeamDuel(h.getLevel().getServer(),a,Set.of(first.getUUID()),b,Set.of(second.getUUID()),Set.of(),false,ranked);
            check(TameDuelManager.isEntityInDuel(first.getUUID()),"fixture battle started");
        }
        Wolf pet(UUID owner,int x){
            var pet=EntityType.WOLF.create(h.getLevel());pet.setTame(true);pet.setOwnerUUID(owner);pet.setNoAi(true);
            pet.moveTo(h.absolutePos(new BlockPos(x,2,2)),0,0);h.getLevel().addFreshEntity(pet);
            var data=new TameData(pet);data.level=20;data.name="PostRankedDeath";TameRegistry.register(data);LoadedTameIndex.refresh(pet);ids.add(pet.getUUID());return pet;
        }
        LivingEntity body(UUID id){var data=TameRegistry.get(id);return TameEntityAdapter.findLoaded(h.getLevel().getServer(),id,data.tlId);}
        void ordinaryDeath(UUID id){
            var body=body(id);check(body!=null&&body.isAlive(),"returned tame exists before ordinary death");
            check(!TameDuelManager.isTameInDuel(id),"returned tame is outside battle");
            body.hurt(h.getLevel().damageSources().genericKill(),10000);
            var data=TameRegistry.get(id);
            check(data!=null&&data.dead,"post-ranked death must be recorded dead, not left unloaded and alive");
            check(data.commandRespawnAfterDayTime>=0,"normal morning respawn delay recorded");
            check(!data.deathHistory.isEmpty(),"ordinary death archived in history");
        }
        public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),a);
            for(var id:ids){var data=TameRegistry.get(id);var body=data==null?null:TameEntityAdapter.findLoaded(h.getLevel().getServer(),id,data.tlId);TameRegistry.remove(id);if(body!=null)body.discard();}
        }
    }
    @GameTest(template="empty") public static void withdrawnRankedTameLaterDiesNormally(GameTestHelper h){
        try(var f=new Fixture(h,true)){
            TameDuelManager.endDuelForEntity(h.getLevel().getServer(),f.first.getUUID());
            f.ordinaryDeath(f.first.getUUID());
        }h.succeed();
    }
    @GameTest(template="empty") public static void actualRankedDeathRestoresThenLaterDeathIsOrdinary(GameTestHelper h){
        try(var f=new Fixture(h,true)){
            f.first.hurt(h.getLevel().damageSources().genericKill(),10000);
            check(!TameDuelManager.isEntityInDuel(f.first.getUUID()),"ranked death finishes round");
            check(!TameRegistry.get(f.first.getUUID()).dead,"ranked death restored alive");
            f.ordinaryDeath(f.first.getUUID());
        }h.succeed();
    }
    @GameTest(template="empty") public static void withdrawnDuelTameLaterDiesNormally(GameTestHelper h){
        try(var f=new Fixture(h,false)){
            TameDuelManager.endDuelForEntity(h.getLevel().getServer(),f.first.getUUID());
            f.ordinaryDeath(f.first.getUUID());
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianRecallThenLaterDeathIsOrdinary(GameTestHelper h){
        try(var f=new Fixture(h,true)){
            check(TameDuelManager.recallRankedGuardian(h.getLevel().getServer(),f.first.getUUID()),"guardian recalled ranked fighter");
            f.ordinaryDeath(f.first.getUUID());
        }h.succeed();
    }
}
