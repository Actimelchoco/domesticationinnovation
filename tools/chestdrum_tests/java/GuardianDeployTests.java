package test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
import java.util.List;
import java.util.UUID;

final class GuardianDeployTests {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    static void run(ServerLevel level) throws Exception {
        var player = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "DeployTest"));
        Wolf wolf = new Wolf(EntityType.WOLF, level);
        wolf.setTame(true); wolf.setOwnerUUID(player.getUUID()); wolf.setNoAi(true); wolf.setNoGravity(true);
        wolf.moveTo(0.5, 80, 0.5); wolf.setCustomName(Component.literal("Buddy"));
        level.addFreshEntity(wolf);
        TameData data = new TameData(wolf, player.getUUID(), false);
        data.name = "Buddy";
        TameRegistry.register(data);
        CompoundTag anchor = new CompoundTag();
        anchor.putString("dimension", "minecraft:overworld");
        anchor.putInt("x", 8); anchor.putInt("y", 80); anchor.putInt("z", 8);
        data.guardianSetAnchors.put("home", anchor);
        var dispatcher = level.getServer().getCommands().getDispatcher();
        var source = player.createCommandSourceStack().withPermission(2);
        check(dispatcher.execute("tames guardian deployGroup home Buddy", source) == 1, "named deployment succeeds");
        check(data.movementOrder == 3 && data.hasHome && data.homeX == 8 && data.homeZ == 8, "guardian anchor applied");
        // Transfers choose a collision-free position within two blocks horizontally and one vertically.
        check(wolf.distanceToSqr(8.5, 80, 8.5) <= 9, "named tame moves near saved location");
        data.name = "Deploy Buddy";
        wolf.setCustomName(Component.literal(data.name));
        wolf.moveTo(0.5, 80, 0.5);
        check(dispatcher.execute("tames guardian deployGroup home \"Deploy Buddy\"", source) == 1,
                "quoted specific tame name succeeds");
        check(wolf.distanceToSqr(8.5, 80, 8.5) <= 9, "quoted tame reaches saved location");
        check(dispatcher.execute("tames guardian deployGroup home Missing", source) == 0, "missing tame fails gracefully");
        check(dispatcher.execute("tames guardian deployGroup other \"Deploy Buddy\"", source) == 0,
                "nonmember is filtered without exception");
        check(dispatcher.execute("tames guardian deployGroup home all", source) == 1, "all selector still works");
        var resolve = TameCommands.class.getDeclaredMethod("resolveHungerSelection", net.minecraft.server.MinecraftServer.class, UUID.class, String.class);
        resolve.setAccessible(true);
        data.stored = true;
        check(((List<?>) resolve.invoke(null, level.getServer(), player.getUUID(), "Deploy Buddy")).isEmpty(), "stored named tame filtered");
        data.stored = false; data.dead = true;
        check(((List<?>) resolve.invoke(null, level.getServer(), player.getUUID(), "Deploy Buddy")).isEmpty(), "dead named tame filtered");
        data.dead = false;
        check(((List<?>) resolve.invoke(null, level.getServer(), player.getUUID(), "state invalid")).isEmpty(), "invalid state returns empty selection");
        wolf.discard();
        System.out.println("GUARDIAN_DEPLOY_TESTS_PASS checks=" + checks);
    }
}
