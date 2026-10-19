package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class LostHunterRegression {
    @GameTest(template="empty")
    public static void reactiveSplashScansAreRateLimitedOnlyForLostTames(GameTestHelper h)throws Exception{
        var body=lost(h,UUID.randomUUID());
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"SplashViewer"));
        player.setPos(body.getX()+1,body.getY(),body.getZ());h.getLevel().players().add(player);
        var method=com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents.class
                .getDeclaredMethod("claimLostAreaScan",LivingEntity.class,TameData.class,String.class);method.setAccessible(true);
        try{
            var data=TameLimitService.wildData(body);
            check((boolean)method.invoke(null,body,data,"chain_lightning"),"first reactive splash permitted");
            check(!(boolean)method.invoke(null,body,data,"chain_lightning"),"same effect cannot rescan on repeated hits");
            check(data.cooldowns.get("lost_scan_chain_lightning")==h.getLevel().getGameTime()+20,"one second reactive cooldown saved");
            check((boolean)method.invoke(null,body,data,"sweeping_edge"),"different effects have independent budgets");
            body.getPersistentData().remove(TameLimitService.WILD);
            check((boolean)method.invoke(null,body,data,"chain_lightning")&&(boolean)method.invoke(null,body,data,"chain_lightning"),"owned and ordinary creatures keep original splash behavior");
        }finally{h.getLevel().players().remove(player);body.discard();LostHunterActivity.forget(body);}h.succeed();
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static final class CountingWolf extends Wolf {
        int aiCalls;
        CountingWolf(ServerLevel level){super(EntityType.WOLF,level);}
        @Override protected void customServerAiStep(){aiCalls++;super.customServerAiStep();}
    }
    private static CountingWolf lost(GameTestHelper h,UUID owner){
        var body=new CountingWolf(h.getLevel());body.setPos(12000,100,12000);TameEntityAdapter.setOwner(body,owner);
        var data=new TameData(body,owner,false);data.name="ThrottleFixture";data.level=15;
        data.abilities.add("arrow_shot");data.abilityLevels.put("arrow_shot",2);
        body.save(data.entitySnapshot);TameRegistry.register(data);
        TameLimitService.release(h.getLevel().getServer(),data);TameLimitService.applyPending(body);return body;
    }
    @GameTest(template="empty")
    public static void nativeAiSleepsWithoutPlayerAndResumesAfterReclaim(GameTestHelper h){
        UUID owner=UUID.randomUUID();var body=lost(h,owner);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(owner,"SleepViewer"));
        player.setPos(0,100,0);h.getLevel().players().add(player);
        try{
            check(!LostHunterActivity.active(body),"far lost tame is inactive");
            body.aiStep();check(body.aiCalls==0,"actual native AI mixin skips custom AI when unobserved");
            check(!body.isNoAi(),"sleep never changes persistent NoAI flag");
            player.setPos(body.getX()+1,body.getY(),body.getZ());LostHunterActivity.forget(body);
            body.aiStep();check(body.aiCalls>0,"native AI resumes near player");
            int before=body.aiCalls;player.setPos(0,100,0);LostHunterActivity.forget(body);
            body.aiStep();check(body.aiCalls==before,"native AI sleeps again after player leaves");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF));
            check(TameLimitService.retameLost(player,body)==null,"imprinted owner reclaims fixture");
            body.aiStep();check(body.aiCalls>before,"reclaimed tame is no longer throttled even away from players");
        }finally{h.getLevel().players().remove(player);TameRegistry.remove(body.getUUID());body.discard();LostHunterActivity.forget(body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void abilityAndPathBudgetsAreBoundedAndCooldownFree(GameTestHelper h)throws Exception{
        var body=lost(h,UUID.randomUUID());var target=EntityType.ZOMBIE.create(h.getLevel());target.setPos(body.getX()+3,body.getY(),body.getZ());
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"BudgetViewer"));player.setPos(body.getX()+1,body.getY(),body.getZ());h.getLevel().players().add(player);
        try{
            int idle=0;for(long tick=0;tick<100;tick+=5)if(LostHunterActivity.abilityPass(body,tick))idle++;
            check(idle==1,"idle abilities evaluated only once per five seconds");
            check(LostHunterActivity.claimPath(body)&&!LostHunterActivity.claimPath(body),"idle path requests limited");
            body.setTarget(target);
            check(LostHunterActivity.claimPath(body)&&!LostHunterActivity.claimPath(body),"combat transition immediately allows one path then throttles repeats");
            int combat=0;for(long tick=0;tick<100;tick+=5)if(LostHunterActivity.abilityPass(body,tick))combat++;
            check(combat==5,"combat ability evaluations once per second");
            for(int i=0;i<8;i++)check(!LostHunterActivity.claimPath(body),"unreachable routes cannot hammer pathfinding in one tick");
            long now=h.getLevel().getGameTime();
            body.getNavigation().createPath(body.blockPosition().offset(3,0,0),1);
            check(body.getNavigation().getPath()==null,"blocked path request does not create a route");
            var route=new net.minecraft.world.level.pathfinder.Path(List.of(new net.minecraft.world.level.pathfinder.Node(12001,100,12000)),body.blockPosition().offset(1,0,0),true);
            var pathField=net.minecraft.world.entity.ai.navigation.PathNavigation.class.getDeclaredField("path");pathField.setAccessible(true);pathField.set(body.getNavigation(),route);
            check(body.getNavigation().createPath(body.blockPosition().offset(4,0,0),1)==route,"moving targets retain a usable old route until the next refresh budget");
            check(h.getLevel().getGameTime()==now,"budget checks do not advance world time");
        }finally{h.getLevel().players().remove(player);target.discard();body.discard();LostHunterActivity.forget(body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void farHuntersClearTargetsAndCannotRequestPathsOrAbilities(GameTestHelper h){
        var body=lost(h,UUID.randomUUID());var target=EntityType.ZOMBIE.create(h.getLevel());target.setPos(12003,100,12000);body.setTarget(target);
        try{
            check(!LostHunterActivity.active(body)&&body.getTarget()==null,"distant hunter drops stale battle target");
            check(!LostHunterActivity.claimPath(body),"distant hunter has no path budget");
            check(body.getNavigation().createPath(body.blockPosition().offset(3,0,0),1)==null,"actual navigation mixin rejects distant path computation");
            for(int i=0;i<100;i+=5)check(!LostHunterActivity.abilityPass(body,i),"distant hunters do not run ability passes");
            var normal=new CountingWolf(h.getLevel());
            try{check(LostHunterActivity.active(normal)&&LostHunterActivity.claimPath(normal)&&LostHunterActivity.claimPath(normal),"ordinary creatures retain unrestricted AI/path policy");}
            finally{normal.discard();}
        }finally{target.discard();body.discard();LostHunterActivity.forget(body);}h.succeed();
    }
}
