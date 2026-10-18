package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public final class SettingsCategoriesRegression {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    @GameTest(template = "empty")
    public static void categoryNavigationAndArgumentCompletion(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "settings_menu"));
        var source = player.createCommandSourceStack().withPermission(0);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("tames settings ", source)).join();
        var names = suggestions.getList().stream().map(s -> s.getText()).collect(Collectors.toSet());
        check(names.equals(Set.of("combat", "movement", "abilities", "display", "beds", "selection", "equipment")),
                "settings completion must show only navigable categories");
        check(dispatcher.execute("tames settings", source) == 1, "base command must show category help");
        for (String category : names) check(dispatcher.execute("tames settings " + category, source) == 1, "category must show its own help");
        check(dispatcher.execute("tames info settings", source) == 1, "info settings must use categorized help");
        suggestions = dispatcher.getCompletionSuggestions(dispatcher.parse("tames settings combat friendlyFire ", source)).join();
        names = suggestions.getList().stream().map(s -> s.getText()).collect(Collectors.toSet());
        check(names.equals(Set.of("true", "false")), "nested setting must retain boolean argument completion");
        var combat = dispatcher.getRoot().getChild("tames").getChild("settings").getChild("combat");
        check(combat.getChild("doNotAttack").getChild("remove").getChild("mobtype") != null,
                "nested mob exclusion arguments must remain available");
        var selection = dispatcher.getRoot().getChild("tames").getChild("settings").getChild("selection");
        check(selection.getChild("excludeFromAll").getChild("add").getChild("group").getChild("name") != null,
                "group exclusion arguments must remain available");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void categorizedSettingsKeepTheirEffectsAndPermissions(GameTestHelper helper) throws Exception {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "settings_values"));
        var source = player.createCommandSourceStack().withPermission(0);
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        UUID id = player.getUUID();
        boolean friendly = PlayerDebugSettings.friendlyFire(id), noBed = PlayerDebugSettings.noAutoSetBed(id), discard = PlayerDebugSettings.discardWornArmor(id);
        try {
            check(dispatcher.execute("tames settings combat friendlyFire true", source) == 1
                    && PlayerDebugSettings.friendlyFire(id), "categorized combat command must update the player's setting");
            check(dispatcher.execute("tames settings beds noAutoSetBed true", source) == 1
                    && PlayerDebugSettings.noAutoSetBed(id), "categorized bed command must update the player's setting");
            check(!discard, "worn armor disposal defaults false");
            check(dispatcher.execute("tames settings equipment discardWornArmor true", source) == 1
                    && PlayerDebugSettings.discardWornArmor(id), "equipment command enables disposal for this owner");
            check(dispatcher.execute("tames settings equipment discardWornArmor false", source) == 1
                    && !PlayerDebugSettings.discardWornArmor(id), "equipment command disables disposal");
            for (String command : new String[]{"tames settings abilities herding false", "tames settings movement sitOnChairs false"}) {
                try { dispatcher.execute(command, source); throw new AssertionError("admin setting was executable without permission"); }
                catch (CommandSyntaxException expected) { }
            }
            var settings = dispatcher.getRoot().getChild("tames").getChild("settings");
            check(settings.getChild("abilities").getChild("herding").canUse(source.withPermission(2))
                    && settings.getChild("movement").getChild("sitOnChairs").canUse(source.withPermission(2)),
                    "existing permission level 2 must still grant admin settings");
        } finally { PlayerDebugSettings.setFriendlyFire(id, friendly); PlayerDebugSettings.setNoAutoSetBed(id, noBed); PlayerDebugSettings.setDiscardWornArmor(id, discard); }
        helper.succeed();
    }
}
