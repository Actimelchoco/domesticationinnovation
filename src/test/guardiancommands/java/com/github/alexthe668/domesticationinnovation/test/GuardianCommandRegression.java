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
public class GuardianCommandRegression {
    private static Set<String> suggestions(GameTestHelper helper,ServerPlayer owner,String command) {
        var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
        Set<String> result=new HashSet<>();
        dispatcher.getCompletionSuggestions(dispatcher.parse("tames "+command,owner.createCommandSourceStack())).join().getList().forEach(s->result.add(s.getText()));
        return result;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked")
    private static Map<UUID,ServerPlayer> players(GameTestHelper helper)throws Exception{
        var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);
        return (Map<UUID,ServerPlayer>)f.get(helper.getLevel().getServer().getPlayerList());
    }
    private static ServerPlayer owner(GameTestHelper helper)throws Exception{
        UUID id=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(id,"GuardianCmd"));
        var p=helper.absolutePos(new BlockPos(2,2,2));owner.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);
        players(helper).put(id,owner);return owner;
    }
    private static Wolf wolf(GameTestHelper helper,ServerPlayer owner,String name,boolean loaded){
        Wolf wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(owner.getUUID());wolf.setNoGravity(true);
        wolf.setPos(owner.getX()+8,owner.getY(),owner.getZ());
        if(loaded)helper.getLevel().addFreshEntity(wolf);
        TameData data=new TameData(wolf);data.name=name;data.group="watch";data.hungerSaturation=500;
        data.hasHome=true;data.movementOrder=3;data.homeDimension=helper.getLevel().dimension().location().toString();
        data.homeX=wolf.blockPosition().getX();data.homeY=wolf.blockPosition().getY();data.homeZ=wolf.blockPosition().getZ();
        TameRegistry.register(data);if(loaded)LoadedTameIndex.refresh(wolf);return wolf;
    }
    private static int run(GameTestHelper helper,ServerPlayer owner,String command)throws Exception{
        return helper.getLevel().getServer().getCommands().getDispatcher().execute("tames "+command,owner.createCommandSourceStack());
    }
    private static void clean(GameTestHelper helper,ServerPlayer owner,Wolf... wolves)throws Exception{
        players(helper).remove(owner.getUUID());TameRegistry.removePlayerDuelStats(owner.getUUID());
        for(Wolf wolf:wolves){TameRegistry.remove(wolf.getUUID());wolf.discard();}
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void singleBulkAndDrumCommandsPreserveAssignedGuardians(GameTestHelper helper)throws Exception{
        var owner=owner(helper);Wolf guard=wolf(helper,owner,"Locked",true),normal=wolf(helper,owner,"Ordinary",true);
        var data=TameRegistry.get(guard.getUUID());var other=TameRegistry.get(normal.getUUID());other.hasHome=false;other.movementOrder=0;
        var position=guard.position();int mode=data.mode;
        try{
            for(String cmd:List.of("sit Locked","follow Locked","wander Locked","tp Locked","tphome Locked","guardian set Locked","movement Locked close","mode Locked aggressive","bodyguard add Locked","guardian clearDeploys Locked")){
                check(run(helper,owner,cmd)==0,"reject locked command "+cmd);
                check(data.hasHome&&data.movementOrder==3&&data.mode==mode&&guard.position().equals(position),"preserve duty "+cmd);
            }
            run(helper,owner,"sit all");check(data.hasHome&&data.movementOrder==3&&!guard.isOrderedToSit(),"bulk sit skips guardian");
            check(other.movementOrder==1&&normal.isOrderedToSit(),"ordinary tame receives bulk sit");
            run(helper,owner,"mode all aggressive");check(data.mode==mode&&other.mode==TameMode.AGGRESSIVE.id(),"bulk mode skips guardian");
            run(helper,owner,"tp all");check(data.hasHome&&guard.position().equals(position),"bulk tp skips guardian");
            run(helper,owner,"tphome all");check(data.hasHome&&guard.position().equals(position),"bulk home skips guardian");
            var drum=TameCommands.class.getDeclaredMethod("drumApplyMovementCommand",ServerPlayer.class,List.class,int.class);drum.setAccessible(true);
            drum.invoke(null,owner,List.of(guard),1);TameCommands.drumFinalizePassiveSit(guard);
            TameCommands.drumCyclePlacedMode(owner,"all");
            check(data.hasHome&&data.movementOrder==3&&data.mode==mode&&!guard.isOrderedToSit(),"drums preserve guardian");
            guard.setOrderedToSit(true);TameCommands.enforceGuardianCommandLock(guard,data);
            check(!guard.isOrderedToSit()&&data.hasHome,"native sit is corrected without clearing anchor");
            var commandable=(com.github.alexthe666.citadel.server.entity.IComandableMob)guard;
            commandable.setCommand(2);TameCommands.enforceGuardianCommandLock(guard,data);
            check(commandable.getCommand()==0&&data.hasHome,"native follow is corrected without clearing anchor");
            data.guardianResting=true;guard.setOrderedToSit(true);TameCommands.enforceGuardianCommandLock(guard,data);
            check(guard.isOrderedToSit()&&data.hasHome,"automatic rest remains allowed");data.guardianResting=false;
            check(run(helper,owner,"inventory autopickup true Locked")==1&&data.hungerAutopickup,"inventory remains editable");
        }finally{clean(helper,owner,guard,normal);}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void releaseActionsAreExplicitAndFree(GameTestHelper helper)throws Exception{
        var owner=owner(helper);List<Wolf> wolves=new ArrayList<>();
        try{
            for(String action:List.of("follow","sit","tp","tphome","guardian")){
                Wolf wolf=wolf(helper,owner,"Release "+action,true);wolves.add(wolf);var data=TameRegistry.get(wolf.getUUID());
                var before=wolf.position();int food=data.hungerSaturation;int xp=owner.totalExperience;
                String cmd=action.equals("guardian")?"guardian release \"Release guardian\"":"release "+action+" \"Release "+action+"\"";
                check(run(helper,owner,cmd)==1,"release action "+action);
                boolean sit=action.equals("sit")||action.equals("tphome")||action.equals("guardian");
                check(!data.hasHome&&data.movementOrder==(sit?1:0)&&wolf.isOrderedToSit()==sit,"released movement "+action);
                if(action.equals("sit")||action.equals("follow"))check(wolf.position().equals(before),"in-place release "+action);
                else check(wolf.distanceToSqr(owner)<.01,"teleport release fallback home/owner "+action);
                check(data.hungerSaturation==food&&owner.totalExperience==xp,"release has no teleport charge");
                check(data.guardianMorningReturnDay==Long.MIN_VALUE,"release cancels pending morning");
            }
        }finally{clean(helper,owner,wolves.toArray(Wolf[]::new));}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void selectorsRespectOwnershipAndBodyguardRoster(GameTestHelper helper)throws Exception{
        var owner=owner(helper);var stranger=owner(helper);
        Wolf one=wolf(helper,owner,"One",true),two=wolf(helper,owner,"Two",true),bodyguard=wolf(helper,owner,"Roster",true),foreign=wolf(helper,stranger,"Foreign",true);
        var roster=TameRegistry.get(bodyguard.getUUID());roster.rosterBodyguard=true;roster.hasHome=false;roster.movementOrder=0;
        try{
            var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
            var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("tames release follow ",owner.createCommandSourceStack())).join().getList();
            check(suggestions.stream().anyMatch(s->s.getText().equals("One")),"suggest own guardian");
            check(suggestions.stream().noneMatch(s->Set.of("Foreign","Roster").contains(s.getText())),"suggest no foreign/roster tames");
            check(run(helper,owner,"release follow Foreign")==0&&TameRegistry.get(foreign.getUUID()).hasHome,"cannot release foreign guardian");
            check(run(helper,owner,"release sit group watch")==2,"group releases only own guardians");
            check(roster.rosterBodyguard&&roster.movementOrder==0,"guardian release preserves bodyguard roster");
            check(!TameRegistry.get(one.getUUID()).hasHome&&!TameRegistry.get(two.getUUID()).hasHome,"selected guardians released");
        }finally{clean(helper,owner,one,two,bodyguard);clean(helper,stranger,foreign);}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void unloadedReleasePersistsUntilNaturalLoad(GameTestHelper helper)throws Exception{
        var owner=owner(helper);Wolf wolf=wolf(helper,owner,"Cold",false);var data=TameRegistry.get(wolf.getUUID());
        try{
            check(run(helper,owner,"release sit Cold")==1,"unloaded guardian releases without source lookup");
            check(!data.hasHome&&data.guardianReleaseMovementPending==1,"unloaded sit saved");
            check(TameData.fromTag(data.toTag()).guardianReleaseMovementPending==1,"pending release survives save");
            var old=data.toTag();old.remove("guardianReleaseMovementPending");
            check(TameData.fromTag(old).guardianReleaseMovementPending==-1,"old saves have no pending release");
            helper.getLevel().addFreshEntity(wolf);LoadedTameIndex.refresh(wolf);TameCommands.applyPendingGuardianRelease(wolf,data);
            check(wolf.isOrderedToSit()&&data.movementOrder==1&&data.guardianReleaseMovementPending==-1,"natural load applies pending sit once");
            check(run(helper,owner,"follow Cold")==1&&!wolf.isOrderedToSit(),"ordinary commands work after release");
        }finally{clean(helper,owner,wolf);}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void roleColorsAndBodyguardSuggestionsMatchEligibility(GameTestHelper helper)throws Exception{
        var owner=owner(helper);
        Wolf guardian=wolf(helper,owner,"Post",true),bodyguard=wolf(helper,owner,"Escort",true),normal=wolf(helper,owner,"Available",true),stored=wolf(helper,owner,"Stored",true);
        var g=TameRegistry.get(guardian.getUUID());var b=TameRegistry.get(bodyguard.getUUID());var n=TameRegistry.get(normal.getUUID());var s=TameRegistry.get(stored.getUUID());
        b.hasHome=false;b.rosterBodyguard=true;b.movementOrder=0;n.hasHome=false;s.hasHome=false;s.stored=true;
        BodyguardService.roster(owner).bodyguards.add(b.ensureTlId());
        try{
            check(suggestions(helper,owner,"bodyguard add ").equals(Set.of("Available","all","name","type","group")),"add suggests only eligible unassigned tames and selection helpers");
            check(suggestions(helper,owner,"bodyguard remove ").equals(Set.of("Escort","all","name","type","group")),"remove suggests only roster members and selection helpers");
            var color=TameCommands.class.getDeclaredMethod("statusColor",TameData.class,boolean.class);color.setAccessible(true);
            var suffix=TameCommands.class.getDeclaredMethod("inactiveSuffix",TameData.class);suffix.setAccessible(true);
            check(color.invoke(null,g,true)==net.minecraft.ChatFormatting.AQUA&&color.invoke(null,g,false)==net.minecraft.ChatFormatting.AQUA,"guardian color independent of load");
            check(color.invoke(null,b,true)==net.minecraft.ChatFormatting.BLUE,"bodyguard has unique blue color");
            check(suffix.invoke(null,g).equals(" [GUARDIAN]")&&suffix.invoke(null,b).equals(" [BODYGUARD]"),"role labels distinguish duty");
            b.dead=true;check(color.invoke(null,b,true)==net.minecraft.ChatFormatting.GRAY&&suffix.invoke(null,b).toString().contains("BODYGUARD"),"dead status retains priority and role label");
            check(suggestions(helper,owner,"bodyguard remove ").contains("Escort"),"dead roster slot can still be removed");
            b.dead=false;
            check(run(helper,owner,"guardian")==1,"bare guardian lists posts");
            check(run(helper,owner,"bodyguard add Available")==1,"eligible suggested tame can be added");
            check(!suggestions(helper,owner,"bodyguard add ").contains("Available"),"new member disappears from add");
            check(suggestions(helper,owner,"bodyguard remove ").contains("Available"),"new member appears in remove");
        }finally{BodyguardService.roster(owner).bodyguards.clear();clean(helper,owner,guardian,bodyguard,normal,stored);}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void releaseThenDeployRestoresSavedPostAcrossSave(GameTestHelper helper)throws Exception{
        var owner=owner(helper);owner.giveExperiencePoints(1000);Wolf wolf=wolf(helper,owner,"Returning",true);var data=TameRegistry.get(wolf.getUUID());
        int x=data.homeX,y=data.homeY,z=data.homeZ;
        try{
            check(run(helper,owner,"release tp Returning")==1&&!data.hasHome,"release clears active assignment");
            check(data.lastGuardianPost.getInt("x")==x,"release remembers last guarding location");
            data.lastGuardianPost=TameData.fromTag(data.toTag()).lastGuardianPost;
            check(run(helper,owner,"guardian deploy Returning")==1,"deploy released guardian using saved post");
            check(data.hasHome&&data.homeX==x&&data.homeY==y&&data.homeZ==z&&data.movementOrder==3,"deploy restores assignment and guarding");
            check(wolf.distanceToSqr(x+.5,y,z+.5)<.01,"deploy teleports to last post");
            check(run(helper,owner,"guardian deploy Returning")==0,"already assigned guardian cannot be moved by repeated deploy");
        }finally{clean(helper,owner,wolf);}helper.succeed();
    }

    @GameTest(template="empty",batch="guardian-command")
    public static void drumCombatOrdersSkipBothGuardRoles(GameTestHelper helper)throws Exception{
        var owner=owner(helper);Wolf guard=wolf(helper,owner,"Post",true),escort=wolf(helper,owner,"Escort",true),normal=wolf(helper,owner,"Normal",true);
        var data=TameRegistry.get(escort.getUUID());data.hasHome=false;data.rosterBodyguard=true;data.movementOrder=0;
        TameRegistry.get(normal.getUUID()).hasHome=false;
        var enemy=EntityType.ZOMBIE.create(helper.getLevel());
        try{
            check(TameCommands.drumSetTargets(owner,net.minecraft.world.item.ItemStack.EMPTY,enemy)==1,"only ordinary tame receives attack order");
            check(guard.getTarget()==null&&escort.getTarget()==null&&normal.getTarget()==enemy,"combat target respects both roles");
            guard.setTarget(enemy);escort.setTarget(enemy);
            check(TameCommands.drumClearTargets(owner,net.minecraft.world.item.ItemStack.EMPTY)==1,"only ordinary tame receives clear-target order");
            check(guard.getTarget()==enemy&&escort.getTarget()==enemy&&normal.getTarget()==null,"guard fights remain intact");
        }finally{enemy.discard();clean(helper,owner,guard,escort,normal);}helper.succeed();
    }

    @SuppressWarnings("unchecked")
    @GameTest(template="empty",batch="guardian-command")
    public static void unloadedDeployRetainsDutyAfterOriginalEntityLookup(GameTestHelper helper)throws Exception{
        var owner=owner(helper);owner.giveExperiencePoints(1000);Wolf wolf=wolf(helper,owner,"ColdPost",false);var data=TameRegistry.get(wolf.getUUID());
        int x=data.homeX,y=data.homeY,z=data.homeZ;
        try{
            check(run(helper,owner,"release follow ColdPost")==1,"release cold guardian");
            data.lastKnownDimension=helper.getLevel().dimension().location().toString();data.lastKnownX=x;data.lastKnownY=y;data.lastKnownZ=z;
            check(run(helper,owner,"guardian deploy ColdPost")==1&&data.hasHome,"unloaded deployment queued and duty assigned");
            var field=TameCommands.class.getDeclaredField("PENDING_IMMEDIATE_CHUNK_TELEPORTS");field.setAccessible(true);
            Object pending=((Map<UUID,Object>)field.get(null)).get(data.tlId);
            check(pending!=null,"deployment looks up original entity");
            for(String name:List.of("nextAttemptTick","chunksReadyTick")){
                var f=pending.getClass().getDeclaredField(name);f.setAccessible(true);f.setLong(pending,0L);
            }
            helper.getLevel().addFreshEntity(wolf);LoadedTameIndex.refresh(wolf);
            var process=TameCommands.class.getDeclaredMethod("processPendingImmediateChunkTeleports",net.minecraft.server.MinecraftServer.class);process.setAccessible(true);process.invoke(null,helper.getLevel().getServer());
            check(data.hasHome&&data.movementOrder==3&&wolf.distanceToSqr(x+.5,y,z+.5)<.01,"queued transfer preserves guarding post and order");
            check(!TameCommands.hasPendingImmediateChunkTeleport(data),"completed deployment releases lookup");
        }finally{TameCommands.cancelPendingImmediateChunkTeleport(helper.getLevel().getServer(),data);clean(helper,owner,wolf);}helper.succeed();
    }
}
