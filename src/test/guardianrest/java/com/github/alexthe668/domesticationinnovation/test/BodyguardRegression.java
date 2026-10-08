package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class BodyguardRegression {
    @SuppressWarnings("unchecked")
    @GameTest(template="empty")
    public static void missingOriginalRecoveryNeverCreatesDuplicate(GameTestHelper helper) throws Exception {
        UUID id=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(id,"GuardRecovery"));
        var pos=helper.absolutePos(new BlockPos(2,2,2));owner.setPos(pos.getX(),pos.getY(),pos.getZ());
        players(helper).put(id,owner);
        Wolf body=wolf(helper,owner,"GuardRecovery");TameData data=TameRegistry.get(body.getUUID());
        data.rosterBodyguard=true;
        UUID original=data.uuid;body.discard();
        try {
            check(!data.dead,"missing original still recorded alive");
            TameCommands.maintainRosterBodyguard(owner,data);
            var field=TameCommands.class.getDeclaredField("PENDING_IMMEDIATE_CHUNK_TELEPORTS");field.setAccessible(true);
            var pending=(Map<UUID,Object>)field.get(null);
            check(pending.containsKey(data.tlId),"unloaded recovery queues original chunk lookup");
            Object request=pending.get(data.tlId);
            var next=request.getClass().getDeclaredField("nextAttemptTick");next.setAccessible(true);
            next.setLong(request,helper.getLevel().getGameTime());
            var ready=request.getClass().getDeclaredField("chunksReadyTick");ready.setAccessible(true);
            ready.setLong(request,helper.getLevel().getGameTime()-20);
            var process=TameCommands.class.getDeclaredMethod("processPendingImmediateChunkTeleports",net.minecraft.server.MinecraftServer.class);
            process.setAccessible(true);process.invoke(null,helper.getLevel().getServer());
            check(data.uuid.equals(original)&&TameRegistry.getByTlId(data.tlId)==data,"recovery preserves registry identity");
            check(LoadedTameIndex.find(helper.getLevel().getServer(),data.uuid,data.tlId)==null,
                    "missing original is not rebuilt from snapshot");
        } finally {
            var clear=TameCommands.class.getDeclaredMethod("clearPendingImmediateChunkTeleport",TameData.class);clear.setAccessible(true);clear.invoke(null,data);
            players(helper).remove(id);TameRegistry.remove(data.uuid);
        }
        helper.succeed();
    }
    private static void check(boolean condition,String message) { if(!condition)throw new AssertionError(message); }
    private static void close(double actual,double expected,String message) { check(Math.abs(actual-expected)<1E-5,message+": "+actual); }
    @SuppressWarnings("unchecked")
    private static Map<UUID,ServerPlayer> players(GameTestHelper helper) throws Exception {
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        return (Map<UUID,ServerPlayer>)field.get(helper.getLevel().getServer().getPlayerList());
    }
    private static Wolf wolf(GameTestHelper helper,ServerPlayer owner,String name) {
        Wolf body=EntityType.WOLF.create(helper.getLevel());body.setTame(true);body.setOwnerUUID(owner.getUUID());
        body.setPos(owner.getX(),owner.getY(),owner.getZ());helper.getLevel().addFreshEntity(body);
        TameData data=new TameData(body);data.name=name;TameRegistry.register(data);LoadedTameIndex.refresh(body);return body;
    }
    @GameTest(template="empty")
    public static void rosterCommandsPersistenceAndBonuses(GameTestHelper helper) throws Exception {
        UUID id=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(id,"GuardOwner"));
        var pos=helper.absolutePos(new BlockPos(2,2,2));owner.setPos(pos.getX(),pos.getY(),pos.getZ());
        owner.setHealth(owner.getMaxHealth());players(helper).put(id,owner);
        List<Wolf> bodies=new ArrayList<>();
        var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();var source=owner.createCommandSourceStack();
        try {
            for(int index=1;index<=3;index++) {
                Wolf body=wolf(helper,owner,"Guard"+index);bodies.add(body);
                check(dispatcher.execute("tames bodyguard add Guard"+index,source)==1,"add guard");
                TameData data=TameRegistry.get(body.getUUID());
                close(data.bodyguardHealingBonus,BodyguardService.healingBonus(index),"living count healing");
                close(TamePackService.xpBonus(data),.5,"full owner health XP");
                check(data.mode==TameMode.BODYGUARD.id()&&data.movementOrder==0,"bodyguard follows");
            }
            var suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("tames bodyguard ",source)).join();
            check(!dispatcher.getRoot().getChild("tames").getChild("bodyguard").getChild("add").canUse(source),"add filtered out of player command tree at three");
            check(dispatcher.execute("tames Bodyguard setRange 22",source)==1,"capitalized alias and range");
            try {dispatcher.execute("tames bodyguard setRange 23",source);throw new AssertionError("range 23 accepted");}
            catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            TameData first=TameRegistry.get(bodies.get(0).getUUID());
            check(dispatcher.execute("tames mode Guard1 bodyguard 23",source)==0,"legacy mode range capped");
            check(dispatcher.execute("tames mode Guard1 aggressive",source)==0,"roster mode locked");
            owner.setHealth(4);BodyguardService.refresh(helper.getLevel().getServer());
            check(first.closeMovement,"low health close movement");close(TamePackService.xpBonus(first),0,"hurt owner removes XP bonus");
            owner.setHealth(owner.getMaxHealth());BodyguardService.refresh(helper.getLevel().getServer());
            check(!first.closeMovement,"healthy owner restores previous movement");
            first.dead=true;BodyguardService.refresh(helper.getLevel().getServer());
            close(TameRegistry.get(bodies.get(1).getUUID()).bodyguardHealingBonus,1,"dead guard excluded from alive count");
            first.dead=false;first.mode=TameMode.DEFAULT.id();first.movementOrder=1;bodies.get(0).setOrderedToSit(true);
            BodyguardService.refresh(helper.getLevel().getServer());
            check(first.mode==TameMode.BODYGUARD.id()&&first.movementOrder==0&&!bodies.get(0).isOrderedToSit(),"respawn state resumes bodyguard");
            var roster=BodyguardService.roster(owner);var saved=PlayerDuelStats.fromTag(roster.toTag());
            check(saved.bodyguards.equals(roster.bodyguards)&&saved.bodyguardRange==22,"stable roster saved");
            check(TameData.fromTag(first.toTag()).rosterBodyguard,"tame membership saved");
            check(dispatcher.execute("tames bodyguard remove Guard1",source)==1,"remove guard");
            check(!first.rosterBodyguard&&first.movementOrder==1&&bodies.get(0).isOrderedToSit(),"removed guard goes home and sits");
            suggestions=dispatcher.getCompletionSuggestions(dispatcher.parse("tames bodyguard ",source)).join();
            check(dispatcher.getRoot().getChild("tames").getChild("bodyguard").getChild("add").canUse(source),"add returns to player command tree below three");
        } finally {
            players(helper).remove(id);BodyguardService.roster(owner).bodyguards.clear();
            for(Wolf body:bodies){body.discard();TameRegistry.TAMES.remove(body.getUUID());}
            TameRegistry.removePlayerDuelStats(id);
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void ownerFeedingAndSmallPackHealing(GameTestHelper helper) throws Exception {
        UUID id=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(id,"GuardFood"));
        players(helper).put(id,owner);Wolf body=wolf(helper,owner,"GuardFood");TameData data=TameRegistry.get(body.getUUID());
        try {
            data.rosterBodyguard=true;data.hungerSaturation=0;owner.getFoodData().setSaturation(5);
            check(BodyguardService.feedFromOwner(data,body,250),"owner feeds guard");
            close(owner.getFoodData().getSaturationLevel(),2.5,"one saturation equals one food point");
            check(data.hungerSaturation==250,"borrow credits exact tame units");
            check(!BodyguardService.feedFromOwner(data,body,600),"insufficient owner saturation cannot pay");
            close(owner.getFoodData().getSaturationLevel(),2.5,"failed action does not drain owner");
            data.hungerInventory.add(new ItemStack(Items.COOKED_BEEF));
            check(!BodyguardService.feedFromOwner(data,body,300),"stored food takes priority");data.hungerInventory.clear();
            data.rosterBodyguard=false;check(!BodyguardService.feedFromOwner(data,body,300),"ordinary tame cannot borrow");
            var self=new TamePackService.Member(UUID.randomUUID(),"wolf","minecraft:wolf",10);
            var other=new TamePackService.Member(UUID.randomUUID(),"cat","minecraft:cat",10);
            close(TamePackService.calculate(self,List.of()).healingSpeed(),2,"solo healing +200%");
            close(TamePackService.calculate(self,List.of(other)).healingSpeed(),1,"duo healing +100%");
            var third=new TamePackService.Member(UUID.randomUUID(),"pig","minecraft:pig",10);
            close(TamePackService.calculate(self,List.of(other,third)).healingSpeed(),.5,"trio healing +50%");
            var interval=Class.forName("com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents")
                    .getDeclaredMethod("passiveHealInterval",net.minecraft.world.entity.LivingEntity.class,TameData.class);interval.setAccessible(true);
            data.bodyguardHealingBonus=2;TamePackService.updateModifiers(data);
            check((long)interval.invoke(null,body,data)==33,"bodyguard speeds actual passive healing");
            data.bodyguardHealingBonus=0;TamePackService.updateModifiers(data);
            check((long)interval.invoke(null,body,data)==100,"ordinary base interval preserved");
            data.bodyguardHealingBonus=2;TamePackService.updateModifiers(data);data.hungerSaturation=1000;
            check(TameCommands.consumeHungerForPassiveHeal(data,body,1),"first faster heal is paid");
            check(TameCommands.consumeHungerForPassiveHeal(data,body,1),"second faster heal is paid");
            check(TameCommands.consumeHungerForPassiveHeal(data,body,1),"third faster heal is paid");
            check(data.hungerSaturation==667,"three faster heals consume three payments plus bonus category");
        } finally { players(helper).remove(id);body.discard();TameRegistry.TAMES.remove(body.getUUID()); }
        helper.succeed();
    }
}
