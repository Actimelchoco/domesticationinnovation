package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class RankedRefereeRegression {
    @GameTest(template="empty",batch="refereeCombat",timeoutTicks=100)
    public static void refereeActuallyKillsNearbyAttackerOnServerTicks(GameTestHelper h){
        var f=new Fixture(h,true);

        var monster=f.hostile();monster.setHealth(1);monster.setPos(f.point.x()+1,f.point.y(),f.point.z());
        monster.setTarget(f.first);
        h.runAfterDelay(8,()->{
            try{var body=RankedRefereeService.body(f.battle); h.assertTrue(!monster.isAlive(),"automatic referee kill: threatHealth="+monster.getHealth()+", body="+body+", target="+(body==null?null:body.getTarget())+", position="+(body==null?null:body.position())+", threatPosition="+monster.position()+", attack="+(body==null?null:body.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))+", time="+h.getLevel().getGameTime());
                h.assertTrue(RankedRefereeService.body(f.battle)!=null,"referee waits after clearing the threat");}
            finally{monster.discard();f.close();}h.succeed();
        });
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static final class Fixture implements AutoCloseable {
        final GameTestHelper h;final String arena,oldArena;final Set<UUID> oldPool;
        final UUID ownerA=UUID.randomUUID(),ownerB=UUID.randomUUID();final List<LivingEntity> pets=new ArrayList<>();
        final Wolf first,second,top,lower;final TameArenaRegistry.ArenaPoint point;UUID battle;
        Fixture(GameTestHelper helper,boolean ranked){
            h=helper;var server=h.getLevel().getServer();TameArenaRegistry.init(server);
            oldArena=TameRegistry.getRankedArenaName();oldPool=TameRegistry.getRankedParticipants();
            arena="referee_"+UUID.randomUUID().toString().substring(0,8);TameArenaRegistry.createArena(arena);
            var pos=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));
            point=new TameArenaRegistry.ArenaPoint(h.getLevel().dimension().location().toString(),pos.getX()+0.5,pos.getY(),pos.getZ()+0.5,0,0);
            TameArenaRegistry.setRefereeSpot(arena,point);TameRegistry.setRankedArenaName(arena);
            first=pet(ownerA,"FighterA",9000);second=pet(ownerB,"FighterB",8000);top=pet(ownerA,"TopBench",4000);lower=pet(ownerA,"LowerBench",2000);
            top.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
            TameRegistry.setRankedParticipants(Set.of(first.getUUID(),second.getUUID(),top.getUUID(),lower.getUUID()));
            TameDuelManager.startTeamDuel(server,ownerA,Set.of(first.getUUID()),ownerB,Set.of(second.getUUID()),Set.of(),false,ranked);
            battle=TameDuelManager.rankedBattleId(first.getUUID());
        }
        Wolf pet(UUID owner,String name,int mmr){
            var pet=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(pet,owner);pet.setNoAi(true);
            pet.setPos(point.x(),point.y(),point.z());h.getLevel().addFreshEntity(pet);
            var data=new TameData(pet);data.name=name;data.duelMmr=mmr;TameRegistry.register(data);LoadedTameIndex.refresh(pet);pets.add(pet);return pet;
        }
        Mob hostile(){var mob=EntityType.ZOMBIE.create(h.getLevel());mob.setNoAi(true);mob.setPos(point.x()+3,point.y(),point.z());h.getLevel().addFreshEntity(mob);return mob;}
        void tick(){RankedRefereeService.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END,()->true,h.getLevel().getServer()));}
        @Override public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),ownerA);if(battle!=null)RankedRefereeService.end(battle);
            TameRegistry.setRankedArenaName(oldArena);TameRegistry.setRankedParticipants(oldPool);TameArenaRegistry.deleteArena(arena);
            for(var pet:pets){TameRegistry.remove(pet.getUUID());pet.discard();}
        }
    }
    @GameTest(template="empty")
    public static void hostileTargetSpawnsHighestMmrBenchCopyAndMatchEndRemovesIt(GameTestHelper h){
        try(var f=new Fixture(h,true)){
            check(f.battle!=null&&RankedRefereeService.body(f.battle)==null,"referee waits for external hostility");
            Vec3 original=f.top.position();var originalData=TameRegistry.get(f.top.getUUID());int originalMmr=originalData.duelMmr;
            var monster=f.hostile();
            try{
                monster.setTarget(f.first);var referee=RankedRefereeService.body(f.battle);
                check(referee!=null,"actual target event spawns referee");
                check(RankedRefereeService.data(referee).duelMmr==4000&&referee.getName().getString().contains("TopBench"),"highest MMR nonparticipant is selected");
                check(monster.getTarget()==referee,"hostile redirects away from ranked fighter");
                check(!referee.getUUID().equals(f.top.getUUID())&&TameRegistry.get(referee.getUUID())==null,"temporary copy is never registered");
                check(!TameDuelManager.isEntityInDuel(referee.getUUID()),"referee is not a scoring participant");
                check(!referee.shouldBeSaved(),"referee cannot persist into world save");
                check(f.top.position().equals(original)&&originalData.duelMmr==originalMmr&&f.top.getMainHandItem().is(Items.DIAMOND_SWORD),"original tame, MMR and equipment unchanged");
                check(!referee.hurt(h.getLevel().damageSources().mobAttack(monster),10),"referee is protected from death");
                check(!f.second.hurt(h.getLevel().damageSources().mobAttack(referee),10),"referee cannot harm ranked participants");
                check(RankedRefereeService.blocks(f.first,h.getLevel().damageSources().mobAttack(referee)),"explicit damage whitelist excludes fighters");
                float health=monster.getHealth();check(referee.doHurtTarget(monster)&&monster.getHealth()<health,"referee can damage the external threat");
                var blast=new net.minecraft.world.level.Explosion(h.getLevel(),referee,referee.getX(),referee.getY(),referee.getZ(),2,false,net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
                blast.getToBlow().add(referee.blockPosition());
                var affected=new ArrayList<Entity>(List.of(f.first,monster));
                RankedRefereeService.explosion(new net.minecraftforge.event.level.ExplosionEvent.Detonate(h.getLevel(),blast,affected));
                check(blast.getToBlow().isEmpty()&&affected.equals(List.of(monster)),"referee explosions cannot damage arena blocks or fighters");
                var arrow=new net.minecraft.world.entity.projectile.Arrow(h.getLevel(),referee);h.getLevel().addFreshEntity(arrow);
                check(RankedRefereeService.temporary(arrow)&&!arrow.shouldBeSaved(),"referee projectiles are transient");
                var drops=new LivingDropsEvent(referee,h.getLevel().damageSources().generic(),new ArrayList<>(),0,false);RankedRefereeService.drops(drops);
                check(drops.isCanceled(),"copied inventory cannot be dropped for loot");
                TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),f.ownerA);
                check(referee.isRemoved()&&RankedRefereeService.body(f.battle)==null,"duel end immediately removes referee");
                check(arrow.isRemoved(),"duel end also removes outstanding referee projectiles");
            }finally{monster.discard();}
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void refereeReturnsSitsAndWakesForNextHostile(GameTestHelper h){
        long time=h.getLevel().getGameTime();
        try(var f=new Fixture(h,true)){
            var monster=f.hostile();var second=f.hostile();
            try{
                check(RankedRefereeService.alert(monster,f.first),"threat recorded");var referee=RankedRefereeService.body(f.battle);check(referee!=null,"referee spawned");
                f.tick();check(referee.getTarget()==monster,"referee targets threat");
                monster.discard();referee.setPos(f.point.x()+5,f.point.y(),f.point.z());
                h.getLevel().getServer().getWorldData().overworldData().setGameTime(time+10);f.tick();
                check(referee.getTarget()==null&&RankedRefereeService.data(referee).movementOrder==2,"cleared battle switches to returning");
                h.getLevel().getServer().getWorldData().overworldData().setGameTime(time+220);f.tick();
                check(referee.position().distanceToSqr(new Vec3(f.point.x(),f.point.y(),f.point.z()))<0.01
                        &&((Wolf)referee).isOrderedToSit(),"stuck return falls back to referee post and sits");
                second.setTarget(f.second);h.getLevel().getServer().getWorldData().overworldData().setGameTime(time+230);f.tick();
                check(RankedRefereeService.body(f.battle)==referee&&referee.getTarget()==second&&!((Wolf)referee).isOrderedToSit(),"same referee wakes for next outside monster");
            }finally{monster.discard();second.discard();}
        }finally{h.getLevel().getServer().getWorldData().overworldData().setGameTime(time);}h.succeed();
    }
    @GameTest(template="empty")
    public static void refereeSpotPersistsAndOrphanRefereesAreRejected(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,true)){
            var field=TameCommands.class.getDeclaredField("RANKED_ARENA_NAME");field.setAccessible(true);Object old=field.get(null);field.set(null,f.arena);
            try{
                var admin=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"RefAdmin"));admin.setPos(f.point.x(),f.point.y(),f.point.z());
                var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();
                boolean denied=false;try{dispatcher.execute("tames admin ranked setRefereeSpot",admin.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
                check(denied,"setting referee post requires admin");
                check(dispatcher.execute("tames admin ranked setRefereeSpot",admin.createCommandSourceStack().withPermission(2))==1,"admin command sets post");
                var arena=TameArenaRegistry.getArena(f.arena);var loaded=TameArenaRegistry.TameArena.fromTag(arena.toTag());
                check(loaded.refereeSpot()!=null&&loaded.refereeSpot().x()==admin.getX(),"post survives serialized arena data");
                check(arena.withHideTames(true).copy().refereeSpot().equals(arena.refereeSpot()),"other arena edits preserve post");
                check(TameDuelManager.isRankedBattleActive(f.battle),"setting post does not cancel current match");
            }finally{field.set(null,old);}
            var orphan=EntityType.WOLF.create(h.getLevel());orphan.getPersistentData().putUUID(RankedRefereeService.MARKER,UUID.randomUUID());
            var join=new EntityJoinLevelEvent(orphan,h.getLevel());RankedRefereeService.join(join);
            check(join.isCanceled()&&orphan.isRemoved(),"orphaned referee cannot reappear after restart");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void casualDuelsDoNotSpawnReferees(GameTestHelper h){
        try(var f=new Fixture(h,false)){
            var monster=f.hostile();try{check(!RankedRefereeService.alert(monster,f.first),"casual duel ignores ranked referee trigger");}
            finally{monster.discard();}
        }h.succeed();
    }
}



