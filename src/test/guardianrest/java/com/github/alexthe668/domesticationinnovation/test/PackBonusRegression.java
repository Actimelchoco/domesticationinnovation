package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class PackBonusRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1E-6, message + ": " + actual + " expected " + expected);
    }
    private static TamePackService.Member member(String type, int level) {
        return new TamePackService.Member(UUID.randomUUID(), type, type, level);
    }
    @GameTest(template = "empty")
    public static void mixedPackAndDiminishingReturns(GameTestHelper helper) {
        var self = member("animights:canito", 10);
        var result = TamePackService.calculate(self, List.of(self, member("animights:catino", 11), member("animights:other", 20)));
        close(result.cooldownReduction(), .175, "two diverse packmates");
        close(result.damageBonus(), .2, "inclusive ten-level range");
        close(result.xpBonus(), .2, "lowest level XP");
        close(result.defense(), .1, "same-mod diversity defense");
        close(result.healingSpeed(), .5, "trio size bonus survives different-species penalties");
        check(result.members().size() == 2, "self must not count");
        result = TamePackService.calculate(self, List.of(member("animights:canito", 10), member("minecraft:wolf", 21)));
        close(result.cooldownReduction(), .1, "duplicates do not cancel partial diversity");
        close(result.damageBonus(), 0, "eleven-level spread disables damage");
        close(result.xpBonus(), 0, "eleven-level spread disables lowest-level XP");
        close(result.defense(), 0, "foreign-mod penalty clamped");
        close(TamePackService.cooldownReduction(5), .305078125, "five-member cooldown curve");
        close(TamePackService.cooldownReduction(1000), .4, "natural forty-percent limit");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void sameSpeciesHealingThresholds(GameTestHelper helper) {
        var self = member("minecraft:wolf", 10);
        List<TamePackService.Member> members = new ArrayList<>();
        for (int i=0; i<10; i++) members.add(member("minecraft:wolf", 10));
        var result = TamePackService.calculate(self, members);
        close(result.healingSpeed(), 2, "ten same-type packmates healing speed");
        close(result.healingAmount(), 1, "eleven total members retain full healing");
        members.add(member("minecraft:wolf", 10));
        close(TamePackService.calculate(self, members).healingAmount(), .8, "twelfth same-type member penalty");
        for(int i=0;i<10;i++) members.add(member("minecraft:cat",10));
        close(TamePackService.calculate(self, members).healingSpeed(), 0, "mixed-species healing penalty clamped");
        for(int i=0;i<10;i++) members.add(member("minecraft:wolf",10));
        close(TamePackService.calculate(self, members).healingAmount(), 0, "large packs never heal negatively");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void morningsDeathPersistenceAndFractionalCosts(GameTestHelper helper) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        TameData data = new TameData(wolf); data.bornDayTime = 0;
        check(TamePackService.morning(data, 1, true), "online morning award");
        check(!TamePackService.morning(data, 1, true), "no duplicate morning");
        check(TamePackService.morning(data, 2, false), "offline morning award");
        close(data.dailyPackXpBonus,.15,"online plus offline streak");
        int xp=0, saturation=0;
        for(int i=0;i<20;i++) { xp+=TamePackService.bonusXp(data,1); saturation+=TamePackService.saturationCost(data,1); }
        check(xp==23,"fractional XP must accrue"); check(saturation==25,"daily category plus XP saturation cost");
        for(long day=3;day<40;day++) TamePackService.morning(data,day,true);
        close(data.dailyPackXpBonus,1,"daily XP cap");
        data.level=10;data.hasSavedProgress=true;data.savedLevel=12;
        var reward = new net.minecraft.nbt.CompoundTag();reward.putInt("level",12);data.levelRewardHistory.add(reward);
        TamePackService.recordDeath(data,40);check(data.savedLevel==12,"first death keeps high record");
        TamePackService.recordDeath(data,40);check(data.savedLevel==11,"second death loses one high record level");
        TamePackService.recordDeath(data,40);TamePackService.recordDeath(data,40);
        check(data.savedLevel==10 && data.levelRewardHistory.size()==1,"record floor and reward memory");
        close(data.dailyPackXpBonus,0,"death resets XP streak");
        check(!TamePackService.morning(data,40,true),"no streak on death day");
        var loaded=TameData.fromTag(data.toTag());check(loaded.packDeathsToday==4 && loaded.packDeathDay==40,"death state saved");
        TamePackService.recordDeath(loaded,41);check(loaded.packDeathsToday==1,"death counter resets by day");
        data.dead=true;check(!TamePackService.morning(data,41,true),"dead pet no morning XP");
        data.dead=false;data.stored=true;check(!TamePackService.morning(data,41,true),"stored pet no morning XP");
        helper.succeed();
    }
    @SuppressWarnings("unchecked")
    @GameTest(template = "empty")
    public static void realCombatEligibilityDamageXpAndCommand(GameTestHelper helper) throws Exception {
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        var players=(Map<UUID,ServerPlayer>)field.get(helper.getLevel().getServer().getPlayerList());
        UUID ownerId=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(ownerId,"PackTest"));
        var pos=helper.absolutePos(new BlockPos(2,2,2));owner.setPos(pos.getX(),pos.getY(),pos.getZ());
        Wolf first=EntityType.WOLF.create(helper.getLevel()), second=EntityType.WOLF.create(helper.getLevel());
        Villager enemy=EntityType.VILLAGER.create(helper.getLevel());
        for(Wolf wolf:List.of(first,second)) {
            wolf.setTame(true);wolf.setOwnerUUID(ownerId);wolf.setPos(pos.getX(),pos.getY(),pos.getZ());
            helper.getLevel().addFreshEntity(wolf);var data=new TameData(wolf);data.name=wolf==first?"Pack First":"Pack Second";
            TameRegistry.register(data);LoadedTameIndex.refresh(wolf);
        }
        enemy.setPos(pos.getX()+1,pos.getY(),pos.getZ());helper.getLevel().addFreshEntity(enemy);
        try {
            players.put(ownerId,owner);first.setTarget(enemy);second.setTarget(enemy);
            var data=TameRegistry.get(first.getUUID());
            TamePackService.refresh(helper.getLevel().getServer());
            check(TamePackService.bonuses(data).members().size()==1,"real combat pack eligibility");
            var schedule=TamePackService.class.getDeclaredMethod("refreshIfDue",net.minecraft.server.MinecraftServer.class,long.class);
            schedule.setAccessible(true);
            var next=TamePackService.class.getDeclaredField("nextRefreshTick");next.setAccessible(true);
            long previousNext=next.getLong(null);
            try {
                next.setLong(null,0);
                schedule.invoke(null,helper.getLevel().getServer(),0L);
                var cached=data.packModifiers;
                second.setPos(pos.getX()+33,pos.getY(),pos.getZ());
                schedule.invoke(null,helper.getLevel().getServer(),59L);
                check(data.packModifiers==cached && TamePackService.bonuses(data).members().size()==1,
                        "no recomputation before sixty ticks");
                check(helper.getLevel().getServer().getCommands().getDispatcher().execute("tames pack \"Pack First\"",owner.createCommandSourceStack())==1,
                        "cached pack status command");
                check(data.packModifiers==cached,"status command must not bypass throttle");
                schedule.invoke(null,helper.getLevel().getServer(),60L);
                close(data.packModifiers.damageMultiplier(),1.5,"sixty-tick refresh switches to lonely damage");
                close(data.packModifiers.saturationMultiplier(),1.2,"lonely damage and healing cost two saturation categories");
                check(TamePackService.bonuses(data).members().isEmpty(),"sixty-tick refresh updates membership");
                var lonelyHit=new LivingHurtEvent(enemy,helper.getLevel().damageSources().mobAttack(first),10);
                TamePackService.hurt(lonelyHit);close(lonelyHit.getAmount(),15,"real lonely damage multiplier");
                second.setPos(pos.getX(),pos.getY(),pos.getZ());
                schedule.invoke(null,helper.getLevel().getServer(),120L);
                check(TamePackService.bonuses(data).members().size()==1,"next scheduled refresh restores membership");
            } finally { next.setLong(null,previousNext); }
            var hit=new LivingHurtEvent(enemy,helper.getLevel().damageSources().mobAttack(first),10);
            TamePackService.hurt(hit);close(hit.getAmount(),11,"combat damage multiplier");
            LevelSystem.grantXP(first,data,10);check(LevelSystem.estimateInvestedXp(data)==11,"incoming XP integration across level-up");
            var interval = Class.forName("com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents")
                    .getDeclaredMethod("passiveHealInterval", net.minecraft.world.entity.LivingEntity.class, TameData.class);
            interval.setAccessible(true);
            check((long)interval.invoke(null,first,data)==45,"same-species passive healing interval");
            var action=TameCommands.class.getDeclaredMethod("consumeHungerForAction",TameData.class,net.minecraft.world.entity.LivingEntity.class,int.class);
            action.setAccessible(true);data.hungerSaturation=1000;
            for(int i=0;i<10;i++) check((boolean)action.invoke(null,data,first,1),"pack saturation action succeeds");
            check(data.hungerSaturation==986,"three categories plus ten-percent XP cost is 1.4x");
            data.hungerSaturation=0;data.hungerInventory.clear();
            double remainder=data.packSaturationRemainder;
            check(!(boolean)action.invoke(null,data,first,1),"unfed action must fail");
            close(data.packSaturationRemainder,remainder,"failed actions cannot charge fractional saturation");
            data.hungerSaturation=1000;
            check(helper.getLevel().getServer().getCommands().getDispatcher().execute("tames pack \"Pack First\"",owner.createCommandSourceStack())==1,"pack status command");
            first.setTarget(null);second.setTarget(null);TamePackService.refresh(helper.getLevel().getServer());
            check(TamePackService.bonuses(data).members().size()==1,"combat grace period");
            second.setPos(pos.getX()+33,pos.getY(),pos.getZ());TamePackService.refresh(helper.getLevel().getServer());
            check(TamePackService.bonuses(data).members().isEmpty(),"32-block pack distance");
            second.setPos(pos.getX(),pos.getY(),pos.getZ());owner.setPos(pos.getX()+68,pos.getY(),pos.getZ());
            TamePackService.refresh(helper.getLevel().getServer());check(TamePackService.bonuses(data).members().isEmpty(),"67-block owner distance");
            owner.setPos(pos.getX(),pos.getY(),pos.getZ());first.setTarget(enemy);second.setTarget(enemy);
            players.remove(ownerId);TamePackService.refresh(helper.getLevel().getServer());
            check(TamePackService.bonuses(data).members().isEmpty(),"offline owner gets no combat bonuses");
            close(data.packModifiers.damageMultiplier(),1,"offline owner cannot receive lonely bonus");
        } finally {
            players.remove(ownerId);first.discard();second.discard();enemy.discard();
            TameRegistry.TAMES.remove(first.getUUID());TameRegistry.TAMES.remove(second.getUUID());
            TamePackService.refresh(helper.getLevel().getServer());
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void fixedArisePriceAndSameDayRespawn(GameTestHelper helper) throws Exception {
        Wolf wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
        TameData data=new TameData(wolf);data.level=20;
        Class<?> mode=Class.forName(TameCommands.class.getName()+"$ReviveMode");
        Object respawn=null,arise=null;
        for(Object value:mode.getEnumConstants()) { if(value.toString().equals("RESPAWN"))respawn=value; if(value.toString().equals("ARISE"))arise=value; }
        for(String name:List.of("reviveXpCost","reviveApprovedItemCost")) {
            var method=TameCommands.class.getDeclaredMethod(name,TameData.class,mode);method.setAccessible(true);
            int normal=(int)method.invoke(null,data,respawn), immediate=(int)method.invoke(null,data,arise);
            check(immediate==normal*2,"fixed twice-normal "+name);
        }
        data.hasPetBed=true;data.petBedDimension="minecraft:overworld";
        var items=TameCommands.class.getDeclaredMethod("reviveApprovedItemCost",TameData.class,mode);items.setAccessible(true);
        check((int)items.invoke(null,data,arise)==2,"bed-aware Arise price");
        UUID ownerId=UUID.randomUUID();var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(ownerId,"PackRevive"));
        owner.getInventory().clearContent();
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        owner.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TOTEM_OF_UNDYING));
        var payment=TameCommands.class.getDeclaredMethod("tryConsumePayment",ServerPlayer.class,int.class,int.class,int.class,boolean.class,String.class);
        payment.setAccessible(true);var paid=payment.invoke(null,owner,10000,10000,2,true,"arise");
        var success=paid.getClass().getDeclaredField("success");success.setAccessible(true);
        check(success.getBoolean(paid) && owner.getMainHandItem().isEmpty() && owner.getOffhandItem().isEmpty(),"two separate unstackable totems pay Arise");
        data.ownerUUID=ownerId;data.name="Pack Revive";data.hasPetBed=false;data.dead=true;
        data.deadGameTime=helper.getLevel().getGameTime();wolf.save(data.entitySnapshot);
        TameRegistry.register(data);owner.giveExperiencePoints(10000);
        try {
            check(helper.getLevel().getServer().getCommands().getDispatcher().execute("tames respawn \"Pack Revive\"",owner.createCommandSourceStack())==1,
                    "same-day death must allow immediate normal respawn");
            check(!data.dead,"same-day respawn rebuilt the tame");
        } finally {
            var body=LoadedTameIndex.find(helper.getLevel().getServer(),data.uuid,data.tlId);
            if(body!=null)body.discard();TameRegistry.TAMES.remove(data.uuid);owner.getInventory().clearContent();
        }
        helper.succeed();
    }
}
