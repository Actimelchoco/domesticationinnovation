package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.gametest.*;
import java.lang.reflect.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardDuelRegression {
    static Wolf pet(GuardianChestRegression.Fixture f,UUID owner,String name){
        var pet=EntityType.WOLF.create(f.h.getLevel());TameEntityAdapter.setOwner(pet,owner);pet.setNoAi(true);
        pet.setPos(f.home.getX(),f.home.getY(),f.home.getZ());f.h.getLevel().addFreshEntity(pet);
        var data=new TameData(pet);data.name=name;TameRegistry.register(data);LoadedTameIndex.refresh(pet);return pet;
    }
    static Object call(String name,Class<?>[] types,Object... args)throws Exception{
        var method=TameCommands.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(null,args);
    }
    static Object field(Object result,String name)throws Exception{var field=result.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(result);}
    @GameTest(template="empty") public static void bodyguardsAndGuardiansIgnoreManagerDuelAndRankedCalls(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            UUID opponent=UUID.randomUUID();var regular=pet(f,f.owner.getUUID(),"Regular");var enemy=pet(f,opponent,"Enemy");
            var original=f.wolf.position();
            try{
                for(boolean guardian:new boolean[]{false,true})for(boolean ranked:new boolean[]{false,true}){
                    f.data.rosterBodyguard=!guardian;f.data.hasHome=guardian;f.data.movementOrder=guardian?3:0;
                    TameDuelManager.startTeamDuel(h.getLevel().getServer(),f.owner.getUUID(),Set.of(f.wolf.getUUID(),regular.getUUID()),opponent,Set.of(enemy.getUUID()),Set.of(),false,ranked);
                    h.assertTrue(!TameDuelManager.isEntityInDuel(f.wolf.getUUID())&&TameDuelManager.isEntityInDuel(regular.getUUID()),"guard duty is excluded while ordinary selected tames still duel");
                    h.assertTrue(f.wolf.position().equals(original)&&TameDuelManager.ignoresDuelCalls(f.data.tlId),"guard stays in place and stable IDs also respect the lock");
                    TameDuelManager.endDuelForEntity(h.getLevel().getServer(),regular.getUUID());
                }
            }finally{TameDuelManager.endDuelForEntity(h.getLevel().getServer(),regular.getUUID());TameRegistry.remove(regular.getUUID());TameRegistry.remove(enemy.getUUID());regular.discard();enemy.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void duelSelectionSkipsGuardDutyAndExplainsGuardOnlySelection(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            var regular=pet(f,f.owner.getUUID(),"Regular");f.data.rosterBodyguard=true;
            try{
                Class<?> selection=Class.forName(TameCommands.class.getName()+"$DuelSelection");var all=selection.getDeclaredMethod("all");all.setAccessible(true);
                Object result=call("resolveLoadedDuelSelection",new Class<?>[]{net.minecraft.commands.CommandSourceStack.class,UUID.class,selection},f.owner.createCommandSourceStack(),f.owner.getUUID(),all.invoke(null));
                h.assertTrue(((List<?>)field(result,"tames")).equals(List.of(regular)),"all selection omits bodyguards");
                TameRegistry.get(regular.getUUID()).hasHome=true;
                result=call("resolveLoadedDuelSelection",new Class<?>[]{net.minecraft.commands.CommandSourceStack.class,UUID.class,selection},f.owner.createCommandSourceStack(),f.owner.getUUID(),all.invoke(null));
                h.assertTrue(((List<?>)field(result,"tames")).isEmpty()&&((String)field(result,"error")).contains("Release"),"guard-only selections explain that guard duty must be released");
            }finally{TameRegistry.remove(regular.getUUID());regular.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void staleRankedPoolCannotHideOrRecallBodyguard(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            f.data.rosterBodyguard=true;f.data.level=20;var position=f.wolf.position();
            var poolField=TameCommands.class.getDeclaredField("RANKED_POOL");poolField.setAccessible(true);
            @SuppressWarnings("unchecked") Set<UUID> pool=(Set<UUID>)poolField.get(null);var old=new LinkedHashSet<>(pool);
            try{
                pool.add(f.wolf.getUUID());TameRegistry.setRankedParticipants(new LinkedHashSet<>(pool));
                Class<?>[] args={net.minecraft.server.MinecraftServer.class,UUID.class};
                call("unloadRankedParticipantTame",args,h.getLevel().getServer(),f.wolf.getUUID());
                call("sendPulledRankedParticipantHome",args,h.getLevel().getServer(),f.wolf.getUUID());
                h.assertTrue(!f.wolf.isRemoved()&&f.wolf.position().equals(position)&&f.data.rosterBodyguard,"stale pool housekeeping cannot move or hide a bodyguard");
                call("scrubInvalidRankedParticipants",new Class<?>[]{net.minecraft.server.MinecraftServer.class},h.getLevel().getServer());
                h.assertTrue(!pool.contains(f.wolf.getUUID())&&!TameRegistry.getRankedParticipants().contains(f.wolf.getUUID()),"stale guard entries are removed from ranked");
            }finally{pool.clear();pool.addAll(old);}
        }h.succeed();
    }
    @GameTest(template="empty") public static void sessionTeleportSkipsGuardDutyAndReleaseRestoresEligibility(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            f.data.rosterBodyguard=true;var position=f.wolf.position();
            Class<?> target=Class.forName(TameCommands.class.getName()+"$SpawnTarget");var constructor=target.getDeclaredConstructor(net.minecraft.server.level.ServerLevel.class,net.minecraft.world.phys.Vec3.class,float.class,float.class);constructor.setAccessible(true);
            Object destination=constructor.newInstance(h.getLevel(),net.minecraft.world.phys.Vec3.atCenterOf(f.post),0F,0F);
            call("teleportDuelSessionParticipants",new Class<?>[]{net.minecraft.server.MinecraftServer.class,Set.class,target},h.getLevel().getServer(),Set.of(f.wolf.getUUID()),destination);
            h.assertTrue(f.wolf.position().equals(position),"session call cannot teleport a bodyguard");
            f.data.rosterBodyguard=false;f.data.hasHome=true;
            h.assertTrue(TameDuelManager.ignoresDuelCalls(f.data.uuid),"guardian assignment also retains priority");
            f.data.hasHome=false;
            h.assertTrue(!TameDuelManager.ignoresDuelCalls(f.data.uuid),"releasing guard duty restores ordinary duel eligibility");
        }h.succeed();
    }
}
