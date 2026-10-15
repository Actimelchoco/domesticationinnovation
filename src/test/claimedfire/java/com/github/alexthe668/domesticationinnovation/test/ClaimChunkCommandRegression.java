package com.github.alexthe668.domesticationinnovation.test;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public final class ClaimChunkCommandRegression {
    @GameTest(template = "empty")
    public static void chunkPruneCommandRegistration(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var dispatcher = server.getCommands().getDispatcher();
        var root = dispatcher.getRoot().getChild("chunks");
        if (root == null || (root.getRedirect() == null ? root : root.getRedirect()).getChild("deleteNoClaimedChunks") == null)
            throw new AssertionError("prune command is not registered");
        var source = server.createCommandSourceStack().withPermission(4);
        for (String command : new String[]{"chunks deleteNoClaimedChunks", "chunks deleteNoClaimedChunks -1"}) {
            try {
                dispatcher.execute(command, source);
                throw new AssertionError("invalid distance accepted: " + command);
            } catch (CommandSyntaxException expected) { }
        }
        if (dispatcher.execute("chunks deleteNoClaimedChunks 25", source) != 0)
            throw new AssertionError("non-dedicated GameTest server should be rejected");
        try {
            dispatcher.execute("chunks deleteNoClaimedChunks 25", source.withPermission(2));
            throw new AssertionError("permission level 2 accepted");
        } catch (CommandSyntaxException expected) { }
        if (!server.isRunning()) throw new AssertionError("rejected command stopped server");
        helper.succeed();
    }
}
