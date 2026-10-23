package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public final class TameLimitRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static TameData row(UUID owner, String type, int order) {
        TameData data = TameData.fromTag(new CompoundTag());
        data.uuid = UUID.randomUUID(); data.tlId = UUID.randomUUID(); data.ownerUUID = owner;
        data.type = type; data.name = "LimitFixture" + order; data.limitRegisteredAt = order;
        data.level = 10; data.abilities.add("arrow_shot"); data.abilityLevels.put("arrow_shot",1);
        return data;
    }
    private static List<TameData> overLimit(UUID owner) {
        List<TameData> rows = new ArrayList<>();
        for (int i=0;i<101;i++) { TameData data = row(owner, "minecraft:wolf", i+1); TameRegistry.register(data); rows.add(data); }
        return rows;
    }
    private static void cleanup(List<TameData> rows) { rows.forEach(data -> TameRegistry.remove(data.uuid)); }

    @GameTest(template="empty")
    public static void onlyLevelTenWithAnAbilityOrAttributeCounts(GameTestHelper h) {
        TameData data=row(UUID.randomUUID(),"minecraft:wolf",1);
        data.level=1;check(!TameLimitService.countsTowardLimit(data),"level one with an ability is exempt");
        data.level=9;check(!TameLimitService.countsTowardLimit(data),"level nine is exempt");
        data.level=10;check(TameLimitService.countsTowardLimit(data),"level ten plus ability qualifies");
        data.abilities.clear();check(TameLimitService.countsTowardLimit(data),"positive ability level alone qualifies");
        data.abilityLevels.clear();check(!TameLimitService.countsTowardLimit(data),"level ten alone is exempt");
        data.attributeLevels.put("armor",0);check(!TameLimitService.countsTowardLimit(data),"zero-level attribute does not qualify");
        data.attributeLevels.put("armor",1);check(TameLimitService.countsTowardLimit(data),"positive attribute qualifies without abilities");
        h.succeed();
    }

    @GameTest(template="empty")
    public static void untrainedTamesDoNotConsumeSlotsOrKeepReleaseTimers(GameTestHelper h) {
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"LimitTraining"));
        List<TameData> rows=overLimit(player.getUUID());TameData extra=rows.get(100);
        try {
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==20,"qualified extra starts grace");
            extra.level=1;
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==0 && !extra.limitWarned,"exempt tame has no release timer");
            check(TameRegistry.get(extra.uuid)==extra,"exempt tame stays registered and owned");
            extra.level=10;
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==20,"qualification starts a fresh grace period");
            rows.get(0).abilities.clear();rows.get(0).abilityLevels.clear();rows.get(0).attributeLevels.clear();
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==0,"level-ten tame without upgrades does not consume a slot");
        } finally {cleanup(rows);}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void oldestHundredAcrossTypesAreProtected(GameTestHelper h) {
        UUID owner = UUID.randomUUID(); List<TameData> rows = overLimit(owner);
        try {
            rows.get(100).type = "minecraft:cat";
            List<TameData> extras = TameLimitService.excess(TameRegistry.getOwned(owner));
            check(extras.size()==1 && extras.get(0)==rows.get(100), "101st tame is excess across different types");
            check(TameLimitService.excess(rows.subList(0,100)).isEmpty(), "exactly 100 tames are allowed");
            TameData other = row(UUID.randomUUID(),"minecraft:wolf",102);
            List<TameData> mixed = new ArrayList<>(rows); mixed.add(other);
            check(TameLimitService.excess(mixed).equals(extras), "other owners have independent allowances");
        } finally { cleanup(rows); }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void onlineGracePersistsAndExpiresAtTenMinutes(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"LimitTimer"));
        List<TameData> rows = overLimit(player.getUUID()); TameData extra = rows.get(100);
        try {
            for(int i=0;i<300;i++) TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==6000,"five online minutes counted");
            extra = TameData.fromTag(extra.toTag()); rows.set(100,extra); TameRegistry.register(extra);
            for(int i=0;i<299;i++) TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(TameRegistry.get(extra.uuid)!=null && extra.limitOnlineTicks==11980,"no early release after saved-record reload");
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(TameRegistry.get(extra.uuid)==null,"removed exactly at ten online minutes");
            check(TameRegistry.getOwned(player.getUUID()).size()==100,"owner index has 100 entries");
        } finally { cleanup(rows); }
        h.succeed();
    }

    @GameTest(template="empty")
    public static void freeingAPlaceCancelsTheGraceTimer(GameTestHelper h) {
        var player = FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"LimitPlace"));
        List<TameData> rows=overLimit(player.getUUID()); TameData extra=rows.get(100);
        try {
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==20,"extra timer started");
            TameRegistry.remove(rows.get(0).uuid);
            TameLimitService.advanceOnlineOwner(h.getLevel().getServer(),player);
            check(extra.limitOnlineTicks==0 && !extra.limitWarned,"opening a place cancels release");
            check(TameRegistry.get(extra.uuid)==extra,"extra is retained");
        } finally {cleanup(rows);}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void offlineTimeDoesNotAdvanceAndTransferStartsFresh(GameTestHelper h) {
        UUID owner=UUID.randomUUID(); List<TameData> rows=overLimit(owner); TameData extra=rows.get(100);
        try {
            extra.limitOnlineTicks=6000;extra.limitWarned=true;
            for(int i=0;i<600;i++) TameLimitService.advanceOnlinePlayers(h.getLevel().getServer());
            check(extra.limitOnlineTicks==6000,"ten minutes of offline accounting do not consume grace");
            UUID recipient=UUID.randomUUID();
            check(TameRegistry.transferOwnership(extra.uuid,recipient),"ownership transferred");
            check(extra.limitOnlineTicks==0 && !extra.limitWarned,"previous owner's timer is cleared");
            check(TameLimitService.excess(TameRegistry.getOwned(recipient)).isEmpty(),"recipient has an independent allowance");
        } finally {cleanup(rows);}
        h.succeed();
    }

    private static Wolf wolf(GameTestHelper h, UUID owner) {
        Wolf wolf=EntityType.WOLF.create(h.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(owner);
        wolf.moveTo(h.absolutePos(new BlockPos(1,1,1)),0,0);
        check(h.getLevel().addFreshEntity(wolf),"fixture spawned");
        return wolf;
    }

    @GameTest(template="empty")
    public static void releaseKeepsAbilitiesButRemovesControl(GameTestHelper h) {
        Wolf wolf=wolf(h,UUID.randomUUID());
        try {
            TameData data=TameRegistry.get(wolf.getUUID());
            check(data!=null,"tame registered"); data.abilities.add("arrow_shot"); data.abilityLevels.put("arrow_shot",3);
            data.level=20; wolf.setOrderedToSit(true);
            double healthBase=wolf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue();
            TameLimitService.release(h.getLevel().getServer(),data);
            check(TameRegistry.get(wolf.getUUID())==null,"registry entry removed");
            check(!wolf.isTame() && wolf.getOwnerUUID()==null && !wolf.isOrderedToSit(),"owner and sit state cleared");
            TameData wild=TameLimitService.wildData(wolf);
            check(wild.ownerUUID==null && wild.mode==TameMode.MONSTER_HUNTER.id(),"wild permanent hunter record");
            check(wild.abilityLevels.get("arrow_shot")==3 && wild.level==20,"abilities and level preserved");
            check(wolf.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue()==healthBase,"untaming preserves attributes");
            wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
            check(TameSpawnEvents.registerOrRestoreTame(wolf,false)==null && !wolf.isTame(),"cannot bypass release by retaming");
            check(wolf.targetSelector.getAvailableGoals().stream().anyMatch(goal -> goal.getPriority()==-1),"hunter goal installed");
        } finally {TameRegistry.remove(wolf.getUUID());wolf.discard();}
        h.succeed();
    }

    @GameTest(template="empty")
    public static void unloadedEntityCannotRestoreItsPreviousOwner(GameTestHelper h) {
        Wolf wolf=wolf(h,UUID.randomUUID()); TameData data=TameRegistry.get(wolf.getUUID());
        CompoundTag snapshot=new CompoundTag(); wolf.save(snapshot); wolf.discard();
        TameLimitService.release(h.getLevel().getServer(),data);
        LivingEntity restored=(LivingEntity)EntityType.loadEntityRecursive(snapshot,h.getLevel(),entity->entity);
        try {
            check(TameLimitService.applyPending(restored),"pending release applied without loading its chunk");
            check(!TameEntityAdapter.isTame(restored) && TameEntityAdapter.ownerUuid(restored)==null,"stale owner cleared");
            CompoundTag saved=new CompoundTag();restored.save(saved);
            LivingEntity again=(LivingEntity)EntityType.loadEntityRecursive(saved,h.getLevel(),entity->entity);
            try { check(TameLimitService.isReleased(again) && TameLimitService.wildData(again)!=null,"wild state survives entity save/load"); }
            finally {again.discard();}
        } finally {restored.discard();}
        h.succeed();
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void releasedHunterStillCastsItsAbilities(GameTestHelper h) {
        Wolf wolf=wolf(h,UUID.randomUUID()); wolf.setNoAi(true);
        var viewer=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"HunterViewer"));
        viewer.setPos(wolf.getX(),wolf.getY(),wolf.getZ());h.getLevel().players().add(viewer);
        var zombie=EntityType.ZOMBIE.create(h.getLevel()); zombie.setNoAi(true);
        zombie.moveTo(h.absolutePos(new BlockPos(4,1,1)),0,0);
        h.getLevel().addFreshEntity(zombie);
        TameData data=TameRegistry.get(wolf.getUUID());
        data.abilities.clear();data.abilityLevels.clear(); data.cooldowns.clear();
        data.abilities.add("arrow_shot");data.abilityLevels.put("arrow_shot",3);
        TameLimitService.release(h.getLevel().getServer(),data);
        wolf.setTarget(zombie);
        h.runAfterDelay(30,()->{
            try {
                check(TameLimitService.wildData(wolf).cooldowns.containsKey("arrow"),"released creature actually cast arrow_shot");
                var cow=EntityType.COW.create(h.getLevel());
                try {
                    var event=new LivingChangeTargetEvent(wolf,cow,LivingChangeTargetEvent.LivingTargetType.MOB_TARGET);
                    TameLimitService.target(event);
                    check(event.getNewTarget()==null,"hunter rejects passive animals");
                }finally{cow.discard();}
            }finally{h.getLevel().players().remove(viewer);wolf.discard();zombie.discard();}
            h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void storedExcessIsReleasedBackIntoTheWorld(GameTestHelper h) {
        Wolf wolf=wolf(h,UUID.randomUUID());TameData data=TameRegistry.get(wolf.getUUID());
        data.entitySnapshot=new CompoundTag();wolf.save(data.entitySnapshot);
        data.stored=true;wolf.discard();
        TameLimitService.release(h.getLevel().getServer(),data);
        h.runAfterDelay(3,()->{
            TameLimitService.advanceOnlinePlayers(h.getLevel().getServer());
            Entity released=h.getLevel().getEntity(data.uuid);
            try {
                check(released!=null && TameLimitService.isReleased(released),"stored snapshot becomes wild in an already loaded chunk");
                check(!TameEntityAdapter.isTame(released),"stored creature loses its owner");
                check(TameRegistry.get(data.uuid)==null,"stored creature cannot restore its registry entry");
            }finally{if(released!=null)released.discard();}
            h.succeed();
        });
    }

    @GameTest(template="empty", timeoutTicks=100)
    public static void transformedWolfIsReleasedWithoutLeavingAnOrphanBody(GameTestHelper h) {
        Wolf wolf=wolf(h,UUID.randomUUID());TameData data=TameRegistry.get(wolf.getUUID());
        CompoundTag snapshot=new CompoundTag();wolf.save(snapshot);
        var body=EntityType.ZOMBIE.create(h.getLevel());body.setNoAi(true);
        body.moveTo(wolf.position());h.getLevel().addFreshEntity(body);
        data.bewereagerState.putBoolean("Active",true);
        data.bewereagerState.put("Wolf",snapshot);
        data.bewereagerState.putUUID("Body",body.getUUID());
        data.bewereagerState.putString("Dimension",h.getLevel().dimension().location().toString());
        data.bewereagerState.putDouble("X",body.getX());data.bewereagerState.putDouble("Y",body.getY());data.bewereagerState.putDouble("Z",body.getZ());
        wolf.discard();TameLimitService.release(h.getLevel().getServer(),data);
        check(body.isRemoved(),"temporary body removed on release");
        h.runAfterDelay(3,()->{
            TameLimitService.advanceOnlinePlayers(h.getLevel().getServer());
            Entity released=h.getLevel().getEntity(data.uuid);
            try {
                check(released instanceof Wolf && TameLimitService.isReleased(released),"original wolf restored as a wild hunter");
                check(TameRegistry.get(data.uuid)==null,"wolf remains outside registry");
            } finally {if(released!=null)released.discard();}
            h.succeed();
        });
    }
}
