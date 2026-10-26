package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class RankedRoundRecoveryRegression {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Object field(Object object,String name){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new RuntimeException(e);}}
    static void field(Object object,String name,Object value){try{var f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.set(object,value);}catch(Exception e){throw new RuntimeException(e);}}
    static Object call(String name,Class<?>[] types,Object...args){try{var m=TameCommands.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}catch(Exception e){throw new RuntimeException(e);}}
    @GameTest(template="empty") public static void repeatedDeathOfOldBodyCannotKillRestoredRankedTame(GameTestHelper h){
        try(var f=new PostRankedDeathRegression.Fixture(h,true)){
            f.first.hurt(h.getLevel().damageSources().genericKill(),10000);
            var replacement=f.body(f.first.getUUID());
            check(replacement!=null&&replacement!=f.first&&replacement.isAlive(),"actual ranked death restored a new live body");
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(f.first,h.getLevel().damageSources().mobAttack(f.second)));
            check(!TameRegistry.get(f.first.getUUID()).dead,"repeat notification from obsolete dead body must not mark live replacement dead");
            check(TameRegistry.get(f.first.getUUID()).deathHistory.isEmpty(),"obsolete duel body must not create ordinary death history");
            f.ordinaryDeath(f.first.getUUID());
        }h.succeed();
    }
    @GameTest(template="empty") public static void deathWithTemporarilyClearedTameFlagStillEndsRankedRound(GameTestHelper h){
        try(var f=new PostRankedDeathRegression.Fixture(h,true)){
            f.first.setTame(false);
            f.first.hurt(h.getLevel().damageSources().genericKill(),10000);
            check(!TameDuelManager.isEntityInDuel(f.first.getUUID()),"active participant identity must survive native tame flag clearing at death");
            check(f.body(f.first.getUUID())!=null&&f.body(f.first.getUUID()).isAlive(),"ranked participant restored alive");
        }h.succeed();
    }
    @GameTest(template="empty") public static void removingLastFighterSchedulesNextRankedRoundAutomatically(GameTestHelper h){
        try(var f=new DuelFollowerRegression.Fixture(h)){
            var first=f.wolf(f.a.getUUID());var second=f.wolf(f.b.getUUID());first.setNoAi(true);second.setNoAi(true);
            var third=f.wolf(f.b.getUUID());third.setNoAi(true);
            for(var pet:List.of(second,third)) TameRegistry.get(pet.getUUID()).hungerInventory.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COOKED_BEEF,64));
            var ids=new LinkedHashSet<>(Set.of(first.getUUID(),second.getUUID(),third.getUUID()));
            Object session;java.lang.reflect.Field active,pool;Object previous;Set<UUID> saved;
            try{
                var type=Arrays.stream(TameCommands.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("ActiveDuelSession")).findFirst().orElseThrow();
                var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
                var spawnType=Arrays.stream(TameCommands.class.getDeclaredClasses()).filter(c->c.getSimpleName().equals("SpawnTarget")).findFirst().orElseThrow();
                var spawnCtor=spawnType.getDeclaredConstructors()[0];spawnCtor.setAccessible(true);
                var spawn=spawnCtor.newInstance(h.getLevel(),first.position(),0F,0F);
                session=ctor.newInstance(UUID.randomUUID(),f.a.getUUID(),f.b.getUUID(),Set.of(),ids,Set.of(),spawn,spawn,spawn,spawn,"",true,true,false);
                active=TameCommands.class.getDeclaredField("RANKED_DUEL_SESSION");active.setAccessible(true);previous=active.get(null);
                pool=TameCommands.class.getDeclaredField("RANKED_POOL");pool.setAccessible(true);
                @SuppressWarnings("unchecked") var actual=(Set<UUID>)pool.get(null);saved=new LinkedHashSet<>(actual);actual.clear();actual.addAll(ids);active.set(null,session);
            }catch(Exception e){throw new RuntimeException(e);}
            try{
                field(session,"currentRoundA",Set.of(first.getUUID()));field(session,"currentRoundB",Set.of(second.getUUID()));field(session,"roundStartedAtTick",h.getLevel().getGameTime());
                TameDuelManager.startTeamDuel(h.getLevel().getServer(),f.a.getUUID(),Set.of(first.getUUID()),f.b.getUUID(),Set.of(second.getUUID()),Set.of(),false,true);
                TameDuelManager.eliminateParticipantForNoFood(h.getLevel().getServer(),first.getUUID());
                call("removeHungryParticipantFromDuelPools",new Class<?>[]{net.minecraft.server.MinecraftServer.class,UUID.class},h.getLevel().getServer(),first.getUUID());
                call("processDuelSessions",new Class<?>[]{net.minecraft.server.MinecraftServer.class},h.getLevel().getServer());
                check((long)field(session,"nextRoundAtTick")>=h.getLevel().getGameTime(),"empty ranked side must arm next round instead of waiting for manual pool edit");
                check(((Set<?>)field(session,"currentRoundA")).isEmpty()&&((Set<?>)field(session,"currentRoundB")).isEmpty(),"both sides of interrupted round cleaned");
                check(!TameDuelManager.isEntityInDuel(second.getUUID()),"opponent cannot remain trapped in old battle");
                long next=(long)field(session,"nextRoundAtTick");
                call("processDuelSessions",new Class<?>[]{net.minecraft.server.MinecraftServer.class},h.getLevel().getServer());
                check((long)field(session,"nextRoundAtTick")==next,"idle ticks preserve the scheduled deadline");
                field(session,"nextRoundAtTick",h.getLevel().getGameTime());
                call("processDuelSessions",new Class<?>[]{net.minecraft.server.MinecraftServer.class},h.getLevel().getServer());
                check(TameDuelManager.isEntityInDuel(second.getUUID())&&TameDuelManager.isEntityInDuel(third.getUUID()),"remaining tames actually start next ranked round without a pool edit");
                check(!TameDuelManager.isEntityInDuel(first.getUUID()),"pulled tame stays out of the next round");
            }finally{try{active.set(null,previous);@SuppressWarnings("unchecked")var actual=(Set<UUID>)pool.get(null);actual.clear();actual.addAll(saved);}catch(Exception e){throw new RuntimeException(e);}}
        }h.succeed();
    }
}
