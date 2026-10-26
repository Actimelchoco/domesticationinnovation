package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class SkeloraptorSmoothChaseRegression {
    static class CountingNavigation extends net.minecraft.world.entity.ai.navigation.GroundPathNavigation {
        int targetRequests, pathRequests;
        CountingNavigation(Mob mob){super(mob,mob.level());}
        @Override public boolean moveTo(Entity target,double speed){targetRequests++;return super.moveTo(target,speed);}
        @Override public boolean moveTo(net.minecraft.world.level.pathfinder.Path path,double speed){pathRequests++;return super.moveTo(path,speed);}
    }
    static void continuous(GameTestHelper h,boolean duel,boolean interrupt){
        var f=new SkeloraptorRegression.Fixture(h,false,duel);
        for(int x=0;x<=60;x++)for(int z=0;z<=6;z++)h.getLevel().setBlockAndUpdate(h.absolutePos(new BlockPos(x,1,z)),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        LivingEntity target;
        if(duel)target=f.wolf;
        else{var hostile=EntityType.HUSK.create(h.getLevel());hostile.setNoAi(true);hostile.setPersistenceRequired();hostile.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);hostile.setHealth(1000);h.getLevel().addFreshEntity(hostile);target=hostile;}
        var pos=h.absolutePos(new BlockPos(10,2,2));target.moveTo(pos,0,0);
        var data=TameRegistry.get(f.raptor.getUUID());data.mode=TameMode.MONSTER_HUNTER.id();data.movementOrder=2;data.hungerSaturation=10000;
        TameGoalInstaller.installIfMissing(f.raptor);
        try{f.raptor.getClass().getMethod("setCommand",int.class).invoke(f.raptor,0);}catch(Exception e){throw new RuntimeException(e);}
        var navigation=new CountingNavigation(f.raptor);
        try{var field=Mob.class.getDeclaredField("navigation");field.setAccessible(true);field.set(f.raptor,navigation);}catch(Exception e){throw new RuntimeException(e);}
        f.raptor.setOrderedToSit(false);f.raptor.setNoGravity(false);f.raptor.setNoAi(false);f.raptor.setOnGround(true);f.raptor.setTarget(target);f.raptor.getRandom().setSeed(7123);
        int[] stats={0,0,0,0,0};double[] last={f.raptor.getX(),f.raptor.getZ()};var trace=new StringBuilder();
        for(int tick=1;tick<=100;tick++){final int sample=tick;h.runAfterDelay(tick,()->{
            try{
                target.setPos(target.getX()+0.30,target.getY(),target.getZ());
                if(interrupt&&sample%20==4) f.raptor.setOrderedToSit(false);
                int state=((Number)f.raptor.getClass().getMethod("getAttackState").invoke(f.raptor)).intValue();
                if(sample%20==0)trace.append(" [sample=").append(sample).append(" state=").append(state).append(" sit=").append(f.raptor.isOrderedToSit()).append(" target=").append(f.raptor.getTarget()).append(" mode=").append(TameRegistry.get(f.raptor.getUUID()).mode).append(" pathDone=").append(f.raptor.getNavigation().isDone()).append("]");
                double moved=Math.hypot(f.raptor.getX()-last[0],f.raptor.getZ()-last[1]);last[0]=f.raptor.getX();last[1]=f.raptor.getZ();
                if(sample>=10&&state==0&&f.raptor.distanceTo(target)>5&&f.raptor.getTarget()==target
                        &&f.raptor.goalSelector.getRunningGoals().anyMatch(g->g.getGoal() instanceof SkeloraptorDuelChaseGoal)){
                    stats[0]++;if(moved<0.015){stats[1]++;stats[2]++;stats[3]=Math.max(stats[3],stats[2]);}else stats[2]=0;
                    if(f.raptor.getNavigation().isDone())stats[4]++;
                    if(sample%5==0)trace.append(" t=").append(sample).append(" moved=").append(String.format(java.util.Locale.ROOT,"%.3f",moved)).append(" pos=").append(f.raptor.position()).append(" targetPos=").append(target.position()).append(" pathDone=").append(f.raptor.getNavigation().isDone()).append(" goals=").append(f.raptor.goalSelector.getRunningGoals().map(g->g.getGoal().getClass().getSimpleName()).toList());
                }
            }catch(Exception e){throw new RuntimeException(e);}
        });}
        h.runAfterDelay(101,()->{
            try{
                System.out.println("[SkeloraptorSmooth] duel="+duel+" interrupted="+interrupt+" eligible="+stats[0]+" still="+stats[1]+" longestPause="+stats[3]+" missingPath="+stats[4]+" targetRequests="+navigation.targetRequests+" pathRequests="+navigation.pathRequests+trace);
                h.assertTrue(navigation.targetRequests<=6,"resuming paths must retain the once-per-20-ticks target path request budget: "+navigation.targetRequests);
                if(interrupt) h.assertTrue(navigation.pathRequests>navigation.targetRequests,"interrupted chase actually reused a calculated path");
                h.assertTrue(stats[0]>=40,"enough open-ground non-attack chase ticks sampled");
                h.assertTrue(stats[1]<=10&&stats[3]<=4,"chase must remain continuous outside attack animations: stationary="+stats[1]+", longestPause="+stats[3]+", missingPath="+stats[4]+trace);
            }finally{if(!duel)target.discard();f.close();}h.succeed();
        });
    }
    @GameTest(template="skeloraptor_lane",batch="smoothDuel",timeoutTicks=140)public static void distantMovingDuelTargetIsChasedContinuously(GameTestHelper h){continuous(h,true,false);}
    @GameTest(template="skeloraptor_lane",batch="smoothNormal",timeoutTicks=140)public static void distantMovingHostileIsChasedContinuously(GameTestHelper h){continuous(h,false,false);}
    @GameTest(template="skeloraptor_lane",batch="resumeDuel",timeoutTicks=140)public static void interruptedDuelPathResumesWithoutWaitingForRepath(GameTestHelper h){continuous(h,true,true);}
    @GameTest(template="skeloraptor_lane",batch="resumeNormal",timeoutTicks=140)public static void interruptedNormalPathResumesWithoutWaitingForRepath(GameTestHelper h){continuous(h,false,true);}
}

