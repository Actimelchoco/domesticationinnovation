package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class BodyguardCompositionRegression {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    private static Map<UUID,ServerPlayer> players(GameTestHelper h)throws Exception{
        var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);
        return (Map<UUID,ServerPlayer>)f.get(h.getLevel().getServer().getPlayerList());
    }
    private static ServerPlayer owner(GameTestHelper h)throws Exception{
        var owner=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"GuardMix"));
        var p=h.absolutePos(new BlockPos(2,2,2));owner.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);players(h).put(owner.getUUID(),owner);return owner;
    }
    private static LivingEntity pet(GameTestHelper h,ServerPlayer owner,EntityType<? extends LivingEntity> type,String name,String group){
        LivingEntity pet=type.create(h.getLevel());TameEntityAdapter.setOwner(pet,owner.getUUID());
        pet.setPos(owner.getX()+4,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(pet);
        var data=new TameData(pet,owner.getUUID(),!(pet instanceof TamableAnimal));data.name=name;data.group=group;
        data.hasHome=false;data.hungerSaturation=500;data.hasPetBed=true;data.petBedDimension=h.getLevel().dimension().location().toString();
        data.petBedX=owner.blockPosition().getX();data.petBedY=owner.blockPosition().getY();data.petBedZ=owner.blockPosition().getZ();
        TameRegistry.register(data);LoadedTameIndex.refresh(pet);return pet;
    }
    private static int run(GameTestHelper h,ServerPlayer owner,String command)throws Exception{
        return h.getLevel().getServer().getCommands().getDispatcher().execute("tames bodyguard "+command,owner.createCommandSourceStack());
    }
    private static Set<String> suggest(GameTestHelper h,ServerPlayer owner,String command){
        var d=h.getLevel().getServer().getCommands().getDispatcher();Set<String> result=new HashSet<>();
        d.getCompletionSuggestions(d.parse("tames bodyguard "+command,owner.createCommandSourceStack())).join().getList().forEach(s->result.add(s.getText()));return result;
    }
    private static void clean(GameTestHelper h,ServerPlayer owner,List<LivingEntity> pets)throws Exception{
        BodyguardService.roster(owner).bodyguards.clear();players(h).remove(owner.getUUID());TameRegistry.removePlayerDuelStats(owner.getUUID());
        for(var pet:pets){var live=h.getLevel().getEntity(pet.getUUID());if(live!=null)live.discard();TameRegistry.remove(pet.getUUID());pet.discard();}
    }
    @GameTest(template="empty",batch="bodyguard-composition")
    public static void addingGuardiansOverridesDutyOnlyAfterCompositionAcceptsThem(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();
        try{
            pets.add(pet(h,owner,EntityType.WOLF,"AlphaGuard","posts"));
            pets.add(pet(h,owner,EntityType.FOX,"BravoGuard","posts"));
            pets.add(pet(h,owner,EntityType.WOLF,"ExtraGuard","posts"));
            for(var body:pets){var data=TameRegistry.get(body.getUUID());data.hasHome=true;data.movementOrder=3;data.guardianResting=true;
                data.homeDimension=h.getLevel().dimension().location().toString();data.homeX=body.blockPosition().getX();data.homeY=body.blockPosition().getY();data.homeZ=body.blockPosition().getZ();
                data.guardianSetAnchors.put("saved",new net.minecraft.nbt.CompoundTag());}
            check(suggest(h,owner,"add ").contains("AlphaGuard"),"guardian is offered for explicit bodyguard promotion");
            check(run(h,owner,"add AlphaGuard")==1,"single guardian promotion succeeds");
            check(run(h,owner,"add group posts")==1,"bulk promotion adds compatible interface guardian and skips duplicate type");
            for(int i=0;i<2;i++){var data=TameRegistry.get(pets.get(i).getUUID());
                check(data.rosterBodyguard&&!data.hasHome&&!data.guardianResting&&data.movementOrder==0&&data.mode==TameMode.BODYGUARD.id(),"accepted guardian becomes a following normal bodyguard");
                check(data.guardianSetAnchors.containsKey("saved"),"named deployment groups remain saved");}
            var rejected=TameRegistry.get(pets.get(2).getUUID());
            check(rejected.hasHome&&rejected.guardianResting&&!rejected.rosterBodyguard&&rejected.movementOrder==3,"composition rejection preserves guardian assignment");
            check(run(h,owner,"remove BravoGuard")==1,"remove fox so the remaining wolf composition permits another wolf");
            rejected.bewereagerState.putBoolean("Active",true);
            check(!suggest(h,owner,"add ").contains("ExtraGuard")&&run(h,owner,"add ExtraGuard")==0,"hostile transformation lock is not bypassed by guardian override");
            check(rejected.hasHome&&!rejected.rosterBodyguard,"locked guardian keeps its assignment");
            rejected.bewereagerState.remove("Active");
        }finally{clean(h,owner,pets);}h.succeed();
    }
    @GameTest(template="empty",batch="bodyguard-composition")
    public static void sameTypeHasNoSizeCapAndAllRemovesDeadMembers(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();
        try{
            for(int i=0;i<8;i++)pets.add(pet(h,owner,EntityType.WOLF,"Wolf"+i,"wolves"));
            pets.add(pet(h,owner,EntityType.FOX,"OtherFox","others"));
            check(run(h,owner,"add group wolves")==8,"more than three same-type bodyguards added in one command");
            var roster=BodyguardService.roster(owner);check(roster.bodyguards.size()==8,"roster is unlimited");
            check(PlayerDuelStats.fromTag(roster.toTag()).bodyguards.equals(roster.bodyguards),"save/load retains all eight assignments");
            check(run(h,owner,"add OtherFox")==0,"mixed type cannot join repeated same-type roster");
            check(!suggest(h,owner,"add ").contains("OtherFox"),"incompatible type is not suggested");
            var dead=TameRegistry.get(pets.get(0).getUUID());dead.dead=true;
            check(run(h,owner,"remove all")==8&&roster.bodyguards.isEmpty(),"remove all includes dead roster members");
            for(int i=0;i<8;i++)check(!TameRegistry.get(pets.get(i).getUUID()).rosterBodyguard,"removed member flag cleared");
        }finally{clean(h,owner,pets);}h.succeed();
    }
    @GameTest(template="empty",batch="bodyguard-composition")
    public static void uniqueRosterRejectsDuplicatesAndSuggestionsFollowRemovals(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();
        try{
            pets.add(pet(h,owner,EntityType.WOLF,"Alpha","main"));pets.add(pet(h,owner,EntityType.FOX,"Bravo","main"));
            pets.add(pet(h,owner,EntityType.CAT,"Charlie","main"));pets.add(pet(h,owner,EntityType.FROG,"Delta","main"));
            pets.add(pet(h,owner,EntityType.WOLF,"Extra Wolf","extras"));pets.add(pet(h,owner,EntityType.FOX,"ExtraFox","extras"));
            check(run(h,owner,"add group main")==4,"four unique types, including interface pets, can bodyguard");
            check(run(h,owner,"add \"Extra Wolf\"")==0&&run(h,owner,"add ExtraFox")==0,"duplicates cannot join unique roster");
            check(!suggest(h,owner,"add ").contains("ExtraFox"),"duplicate is hidden from suggestions");
            check(run(h,owner,"remove type minecraft:fox")==1,"remove supports type selections");
            check(suggest(h,owner,"add ").contains("ExtraFox"),"newly available type is suggested");
            check(run(h,owner,"add ExtraFox")==1,"replacement type can join");
            check(run(h,owner,"remove all")==4,"all clears unique roster");
            check(run(h,owner,"add \"Extra Wolf\"")==1,"quoted names still work");
        }finally{clean(h,owner,pets);}h.succeed();
    }
    @GameTest(template="empty",batch="bodyguard-composition")
    public static void bulkSkipsConflictsAndLegacyMixedRosterCanBeRepaired(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();
        try{
            pets.add(pet(h,owner,EntityType.WOLF,"One","main"));pets.add(pet(h,owner,EntityType.WOLF,"Two","main"));
            pets.add(pet(h,owner,EntityType.FOX,"Fox","main"));pets.add(pet(h,owner,EntityType.CAT,"Cat","main"));
            check(run(h,owner,"add One, Fox, Two, Cat")==3,"ordered bulk selection skips duplicate and keeps legal unique types");
            var roster=BodyguardService.roster(owner);var two=TameRegistry.get(pets.get(1).getUUID());
            roster.bodyguards.add(two.ensureTlId());two.rosterBodyguard=true;
            check(run(h,owner,"add Two")==0,"existing assignments cannot be duplicated");
            check(run(h,owner,"remove Two")==1,"legacy invalid composition can be repaired without dropping others");
            check(run(h,owner,"remove all")==3,"all clears repaired roster");
            roster.bodyguards.add(UUID.randomUUID());
            check(run(h,owner,"remove all")==1&&roster.bodyguards.isEmpty(),"all also removes stale assignments");
        }finally{clean(h,owner,pets);}h.succeed();
    }

    @SuppressWarnings("unchecked") private static Set<UUID> rankedPool()throws Exception{
        var field=com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.class.getDeclaredField("RANKED_POOL");field.setAccessible(true);return (Set<UUID>)field.get(null);
    }
    private static void rankedPromotion(GameTestHelper h,boolean hidden)throws Exception{
        var owner=owner(h);var enemyOwner=owner(h);List<LivingEntity> pets=new ArrayList<>();var pool=rankedPool();var saved=new LinkedHashSet<>(pool);var savedPersisted=new LinkedHashSet<>(TameRegistry.getRankedParticipants());
        var commands=com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.class;
        var sessionField=commands.getDeclaredField("RANKED_DUEL_SESSION");sessionField.setAccessible(true);var previousSession=sessionField.get(null);Object testSession=null;
        try{
            var guard=pet(h,owner,hidden?EntityType.FOX:EntityType.WOLF,"RankedGuard","ranked");pets.add(guard);
            var enemy=pet(h,enemyOwner,EntityType.WOLF,"RankedEnemy","ranked");pets.add(enemy);
            var data=TameRegistry.get(guard.getUUID());data.level=25;UUID stable=data.ensureTlId();
            pool.add(data.uuid);pool.add(stable);TameRegistry.setRankedParticipants(new LinkedHashSet<>(pool));
            if(hidden){var method=com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.class.getDeclaredMethod("unloadRankedParticipantTame",net.minecraft.server.MinecraftServer.class,UUID.class);method.setAccessible(true);method.invoke(null,h.getLevel().getServer(),data.uuid);check(guard.isRemoved(),"waiting ranked tame really hidden");}
            else {
                TameDuelManager.startTeamDuel(h.getLevel().getServer(),owner.getUUID(),Set.of(guard.getUUID()),enemyOwner.getUUID(),Set.of(enemy.getUUID()),Set.of(),false,true);
                var type=Arrays.stream(commands.getDeclaredClasses()).filter(c->c.getSimpleName().equals("ActiveDuelSession")).findFirst().orElseThrow();var ctor=type.getDeclaredConstructors()[0];ctor.setAccessible(true);
                testSession=ctor.newInstance(UUID.randomUUID(),owner.getUUID(),enemyOwner.getUUID(),Set.of(),Set.of(guard.getUUID(),stable,enemy.getUUID()),Set.of(),null,null,null,null,"",true,true,true);
                var first=type.getDeclaredField("currentRoundA");first.setAccessible(true);first.set(testSession,Set.of(guard.getUUID()));
                var second=type.getDeclaredField("currentRoundB");second.setAccessible(true);second.set(testSession,Set.of(enemy.getUUID()));
                var queued=type.getDeclaredField("queuedPullAfterRound");queued.setAccessible(true);((Set<UUID>)queued.get(testSession)).addAll(Set.of(guard.getUUID(),stable));
                sessionField.set(null,testSession);
            }
            check(suggest(h,owner,"add ").contains("RankedGuard"),"ranked candidate appears in bodyguard suggestions");
            check(run(h,owner,"add RankedGuard")==1,"ranked tame can be promoted immediately");
            var current=TameRegistry.getByTlId(stable);var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),current.uuid,stable);
            check(current.rosterBodyguard&&!current.hasHome&&current.mode==TameMode.BODYGUARD.id()&&current.movementOrder==0,"canonical restored registry row is a following bodyguard");
            check(live!=null&&live.isAlive()&&!TameEntityAdapter.isStayingStill(live),"ranked bodyguard is loaded alive and following");
            check(!pool.contains(guard.getUUID())&&!pool.contains(stable)&&!pool.contains(current.uuid),"all ranked pool identity aliases removed immediately");
            check(!TameRegistry.getRankedParticipants().contains(stable)&&!TameRegistry.getRankedParticipants().contains(current.uuid),"ranked withdrawal persisted");
            check(!TameDuelManager.isEntityInDuel(current.uuid)&&!current.dead&&current.level==25,"withdrawal restores tame without death penalty");
            check(BodyguardService.roster(owner).bodyguards.contains(stable),"roster uses stable tame identity");
            if(testSession!=null){
                for(String name:List.of("poolA","poolB","currentRoundA","currentRoundB","queuedPullAfterRound")){
                    var field=testSession.getClass().getDeclaredField(name);field.setAccessible(true);var ids=(Set<?>)field.get(testSession);
                    check(!ids.contains(guard.getUUID())&&!ids.contains(stable),"ranked session bookkeeping no longer contains promoted tame: "+name);
                }
                var next=testSession.getClass().getDeclaredField("nextRoundAtTick");next.setAccessible(true);check(next.getLong(testSession)>=h.getLevel().getGameTime(),"ranked session schedules its next match after bodyguard withdrawal");
            }
        }finally{sessionField.set(null,previousSession);TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());pool.clear();pool.addAll(saved);TameRegistry.setRankedParticipants(savedPersisted);clean(h,owner,pets);players(h).remove(enemyOwner.getUUID());TameRegistry.removePlayerDuelStats(enemyOwner.getUUID());}h.succeed();
    }
    @GameTest(template="empty",batch="bodyguard-ranked") public static void activeRankedTameBecomesBodyguardImmediately(GameTestHelper h)throws Exception{rankedPromotion(h,false);}
    @GameTest(template="empty",batch="bodyguard-hidden-ranked") public static void hiddenRankedInterfaceTameBecomesLoadedBodyguard(GameTestHelper h)throws Exception{rankedPromotion(h,true);}
    @GameTest(template="empty",batch="bodyguard-duel-lock") public static void regularDuelRemainsLockedEvenWithStaleRankedPoolEntry(GameTestHelper h)throws Exception{
        var owner=owner(h);var enemyOwner=owner(h);List<LivingEntity> pets=new ArrayList<>();var pool=rankedPool();var saved=new LinkedHashSet<>(pool);
        try{
            var guard=pet(h,owner,EntityType.WOLF,"DuelGuard","duel");var enemy=pet(h,enemyOwner,EntityType.WOLF,"Enemy","duel");pets.add(guard);pets.add(enemy);
            pool.add(guard.getUUID());TameDuelManager.startTeamDuel(h.getLevel().getServer(),owner.getUUID(),Set.of(guard.getUUID()),enemyOwner.getUUID(),Set.of(enemy.getUUID()));
            check(!suggest(h,owner,"add ").contains("DuelGuard")&&run(h,owner,"add DuelGuard")==0,"ordinary duel remains locked");
            check(TameDuelManager.isEntityInDuel(guard.getUUID())&&pool.contains(guard.getUUID())&&!TameRegistry.get(guard.getUUID()).rosterBodyguard,"rejection cannot withdraw tame or alter duty");
        }finally{TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());pool.clear();pool.addAll(saved);clean(h,owner,pets);players(h).remove(enemyOwner.getUUID());TameRegistry.removePlayerDuelStats(enemyOwner.getUUID());}h.succeed();
    }

    @GameTest(template="empty",batch="bodyguard-ranked-bulk") public static void bulkPromotionUsesRowsRestoredByFirstRankedWithdrawal(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();var pool=rankedPool();var saved=new LinkedHashSet<>(pool);var persisted=new LinkedHashSet<>(TameRegistry.getRankedParticipants());
        try{
            var first=pet(h,owner,EntityType.WOLF,"AlphaRanked","promote");var second=pet(h,owner,EntityType.FOX,"BravoRanked","promote");pets.add(first);pets.add(second);
            for(var pet:pets){var data=TameRegistry.get(pet.getUUID());data.level=25;pool.add(data.uuid);}
            TameDuelManager.startTeamDuel(h.getLevel().getServer(),owner.getUUID(),Set.of(first.getUUID()),owner.getUUID(),Set.of(second.getUUID()),Set.of(),false,true);
            check(run(h,owner,"add group promote")==2,"bulk command promotes both ranked opponents");
            for(var pet:pets){var data=TameRegistry.get(pet.getUUID());check(data.rosterBodyguard&&data.mode==TameMode.BODYGUARD.id()&&!pool.contains(data.uuid),"all restored canonical rows become bodyguards");}
            BodyguardService.refresh(h.getLevel().getServer());
            check(BodyguardService.roster(owner).bodyguards.size()==2,"bulk roster remains stable after normal maintenance");
        }finally{TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());pool.clear();pool.addAll(saved);TameRegistry.setRankedParticipants(persisted);clean(h,owner,pets);}h.succeed();
    }
    @GameTest(template="empty",batch="bodyguard-ranked-rejection") public static void rejectedCompositionDoesNotWithdrawRankedTame(GameTestHelper h)throws Exception{
        var owner=owner(h);List<LivingEntity> pets=new ArrayList<>();var pool=rankedPool();var saved=new LinkedHashSet<>(pool);
        try{
            pets.add(pet(h,owner,EntityType.WOLF,"Alpha","mix"));pets.add(pet(h,owner,EntityType.FOX,"Bravo","mix"));
            var candidate=pet(h,owner,EntityType.WOLF,"RejectedRanked","other");pets.add(candidate);
            check(run(h,owner,"add Alpha, Bravo")==2,"fixture has a legal unique-type roster");pool.add(candidate.getUUID());
            check(run(h,owner,"add RejectedRanked")==0,"duplicate type is rejected before withdrawing");
            check(pool.contains(candidate.getUUID())&&!TameRegistry.get(candidate.getUUID()).rosterBodyguard,"rejected tame remains ranked");
        }finally{pool.clear();pool.addAll(saved);clean(h,owner,pets);}h.succeed();
    }
}
