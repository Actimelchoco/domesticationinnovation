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
}
