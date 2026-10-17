package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.stream.Collectors;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class PackInfoRegression {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @GameTest(template="empty")
    public static void navigationCompletionAndOwnership(GameTestHelper helper) throws Exception {
        var owner=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"PackInfoOwner"));
        var source=owner.createCommandSourceStack().withPermission(0);
        var dispatcher=helper.getLevel().getServer().getCommands().getDispatcher();
        check(dispatcher.execute("tames pack system",source)==1,"system must show help");
        check(dispatcher.execute("tames pack system info",source)==1,"system info must show help");
        var bonuses=dispatcher.getCompletionSuggestions(dispatcher.parse("tames pack system info ",source)).join().getList().stream().map(s->s.getText()).collect(Collectors.toSet());
        check(bonuses.equals(Set.of("lonely","smallPack","sameSpecies","diversity","levelDamage","lowestLevelXp","sameModDefense","dailyXp","bodyguard","saturation","deathPenalty")),"bonus completion must cover existing mechanics");
        for(String bonus:bonuses) check(dispatcher.execute("tames pack system info "+bonus,source)==1,"every suggested bonus must execute");
        check(dispatcher.execute("tames pack system info unknown",source)==0,"unknown bonus must report failure");
        var wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(owner.getUUID());
        TameData data=new TameData(wolf);data.name="Pack Test Pet";TameRegistry.register(data);
        try {
            check(dispatcher.execute("tames pack tameInfo \"Pack Test Pet\"",source)==1,"quoted owned tame name works");
            check(dispatcher.execute("tames pack \"Pack Test Pet\"",source)==1,"old detailed command remains available");
            var stranger=FakePlayerFactory.get(helper.getLevel(),new GameProfile(UUID.randomUUID(),"PackInfoOther"));
            check(dispatcher.execute("tames pack tameInfo \"Pack Test Pet\"",stranger.createCommandSourceStack())==0,"another owner's tame must be rejected");
        } finally {TameRegistry.remove(data.uuid);wolf.discard();}
        helper.succeed();
    }
    @GameTest(template="empty")
    @SuppressWarnings("unchecked")
    public static void briefSummaryUsesCachedEffectiveBonuses(GameTestHelper helper) throws Exception {
        var wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
        TameData data=new TameData(wolf);
        var summary=TameCommands.class.getDeclaredMethod("packTameBonusSummary",TameData.class);summary.setAccessible(true);
        var bonusesField=TamePackService.class.getDeclaredField("BONUSES");bonusesField.setAccessible(true);
        Map<UUID,TamePackService.Bonuses> cache=(Map<UUID,TamePackService.Bonuses>)bonusesField.get(null);
        try {
            TamePackService.updateModifiers(data);
            check(summary.invoke(null,data).equals("No active pack bonuses."),"inactive tame must not report boosts");
            data.dailyPackXpBonus=.5;data.bodyguardHealingBonus=1;TamePackService.updateModifiers(data);
            String text=(String)summary.invoke(null,data);
            check(text.contains("Healing speed +100.0%")&&text.contains("XP +50.0%")&&text.contains("Saturation use 1.70x"),"daily and bodyguard boosts must use effective values");
            cache.put(data.uuid,TamePackService.Bonuses.LONELY);TamePackService.updateModifiers(data);
            text=(String)summary.invoke(null,data);
            check(text.contains("Healing speed +300.0%")&&text.contains("Damage +50.0%")&&text.contains("Saturation use 1.90x"),"stacked lonely and bodyguard boost must be accurate");
            check(!text.contains("packmates")&&!text.contains("within")&&!text.contains("streak"),"brief summary must omit rosters and system explanations");
        } finally {cache.remove(data.uuid);wolf.discard();}
        helper.succeed();
    }
}
