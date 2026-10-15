package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.GameType;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public final class GuardianProximityRegression {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }

    @SuppressWarnings("unchecked")
    private static List<ServerPlayer> online(GameTestHelper helper) throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
        field.setAccessible(true);
        return (List<ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
    }

    private static void invalidate(TameData data) {
        try {
            var field = GuardianActivityService.class.getDeclaredField("PROXIMITY");
            field.setAccessible(true);
            ((Map<?, ?>) field.get(null)).remove(data);
        } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    @GameTest(template = "empty", batch = "guardian-proximity")
    public static void nearbyVisitorWakesGuardianAndLeavingRestsIt(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var server = level.getServer();
        var visitor = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "guard_visitor"));
        var players = online(helper);
        Wolf wolf = EntityType.WOLF.create(level);
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID()); wolf.setNoGravity(true);
        BlockPos post = helper.absolutePos(new BlockPos(2, 2, 2));
        wolf.moveTo(post.getX() + .5, post.getY(), post.getZ() + .5);
        level.addFreshEntity(wolf);
        TameData data = new TameData(wolf);
        data.hasHome = true; data.movementOrder = 3; data.hungerSaturation = 100;
        data.homeDimension = level.dimension().location().toString();
        data.homeX = post.getX(); data.homeY = post.getY(); data.homeZ = post.getZ();
        data.guardianRestHome.putString("dimension", data.homeDimension);
        data.guardianRestHome.putDouble("x", post.getX() + 40.5);
        data.guardianRestHome.putDouble("y", post.getY());
        data.guardianRestHome.putDouble("z", post.getZ() + .5);
        TameRegistry.register(data); LoadedTameIndex.refresh(wolf);
        players.add(visitor);
        GuardianActivityService.rebuild(server); // New test player list needs its own snapshot.
        try {
            var forced = level.getDataStorage().computeIfAbsent(net.minecraft.world.level.ForcedChunksSavedData::load,
                    net.minecraft.world.level.ForcedChunksSavedData::new, "chunks");
            var baseline = new HashSet<>(forced.getEntityForcedChunks().getTickingChunks().keySet());
            visitor.setPos(post.getX() + 20.51, post.getY(), post.getZ() + .5);
            check(!GuardianActivityService.hasNearbyPlayer(server, data), "outside 20-block radius must stay inactive");
            TameCommands.processGuardianRest(wolf, data);
            check(data.guardianResting && wolf.isOrderedToSit(), "offline guardian rests without a nearby player");
            check(wolf.distanceToSqr(post.getX() + 40.5, post.getY(), post.getZ() + .5) < 1, "resting body is far from post");
            visitor.setPos(post.getX() + 20.5, post.getY(), post.getZ() + .5);
            invalidate(data);
            check(GuardianActivityService.hasNearbyPlayer(server, data), "inclusive radius around POST, not body");
            GuardianActivityService.processRestThrottled(wolf, data);
            check(!data.guardianResting && !wolf.isOrderedToSit(), "visitor must wake resting guardian with owner offline");
            check(wolf.distanceToSqr(post.getX() + .5, post.getY(), post.getZ() + .5) < 1, "guardian returns to saved post");
            check(forced.getEntityForcedChunks().getTickingChunks().keySet().equals(baseline), "activity must not force-load guard-post chunks");
            visitor.setGameMode(GameType.SPECTATOR); invalidate(data);
            check(!GuardianActivityService.hasNearbyPlayer(server, data), "spectator must not activate");
            visitor.setGameMode(GameType.SURVIVAL);
            String dimension = data.homeDimension; data.homeDimension = "minecraft:the_nether";
            check(!GuardianActivityService.hasNearbyPlayer(server, data), "other dimensions must not activate");
            data.homeDimension = dimension;
            visitor.setPos(post.getX() + .5, post.getY() + 20.01, post.getZ() + .5); invalidate(data);
            check(!GuardianActivityService.hasNearbyPlayer(server, data), "vertical distance counts too");
            TameCommands.processGuardianRest(wolf, data);
            check(data.guardianResting && data.hasHome && data.movementOrder == 3, "leaving radius returns guardian to rest, retaining post");
            var saved = TameData.fromTag(data.toTag());
            check(saved.guardianResting && saved.homeX == post.getX(), "resting guardian survives save/load");
            check(server.getCommands().getDispatcher().getRoot().getChild("tames").getChild("offlineGuardian") == null,
                    "offlineGuardian command must be removed");
        } finally { players.remove(visitor); visitor.setGameMode(GameType.SURVIVAL); TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "guardian-proximity")
    public static void legacyDeploymentsMigrateWithoutQuotaOrTickets(GameTestHelper helper) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        TameData data = new TameData(wolf);
        data.offlineGuardianOrder = 5; data.offlineGuardianDeployed = true;
        data.offlineGuardianNeedsDeployment = true;
        data.offlineGuardianDimension = helper.getLevel().dimension().location().toString();
        data.offlineGuardianX = 123; data.offlineGuardianY = 45; data.offlineGuardianZ = -67;
        TameData restored = TameData.fromTag(data.toTag());
        GuardianActivityService.remember(restored);
        check(restored.hasHome && restored.movementOrder == 3 && restored.homeX == 123 && restored.homeZ == -67,
                "old deployed posts must become ordinary guardian posts");
        check(restored.offlineGuardianOrder == 0 && !restored.offlineGuardianDeployed && !restored.offlineGuardianNeedsDeployment,
                "legacy deployment and quota flags must clear");
        restored.hasHome = true; restored.homeX = 321; restored.offlineGuardianDeployed = true;
        GuardianActivityService.remember(restored);
        check(restored.homeX == 321, "migration must not overwrite a newer regular post");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "guardian-proximity")
    public static void proximityCostForHundredsOfGuardians(GameTestHelper helper) throws Exception {
        var players = online(helper);
        List<ServerPlayer> fixtures = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "guard_bench_" + i));
            player.setPos(100000 + i, 100, 100000);
            fixtures.add(player);
        }
        List<TameData> guardians = new ArrayList<>();
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        for (int i = 0; i < 500; i++) {
            TameData data = new TameData(wolf);
            data.hasHome = true; data.homeDimension = helper.getLevel().dimension().location().toString();
            data.homeX = i; guardians.add(data);
        }
        players.addAll(fixtures);
        GuardianActivityService.rebuild(helper.getLevel().getServer());
        try {
            // Uncached spatial queries: distant players should be skipped by their cells.
            for (int round = 0; round < 10; round++) for (TameData data : guardians) {
                data.homeY++; GuardianActivityService.hasNearbyPlayer(helper.getLevel().getServer(), data);
            }
            long start = System.nanoTime();
            for (int round = 0; round < 50; round++) for (TameData data : guardians) {
                data.homeY++;
                check(!GuardianActivityService.hasNearbyPlayer(helper.getLevel().getServer(), data), "benchmark fixtures are far away");
            }
            double millisPerPass = (System.nanoTime() - start) / 1_000_000.0D / 50;
            start = System.nanoTime();
            for (int round = 0; round < 50; round++) for (TameData data : guardians)
                GuardianActivityService.hasNearbyPlayer(helper.getLevel().getServer(), data);
            double cachedMillisPerPass = (System.nanoTime() - start) / 1_000_000.0D / 50;
            // Dense worst case: all 40 players share candidate cells but are
            // outside the 20-block sphere, so every candidate must be checked.
            for (int i = 0; i < fixtures.size(); i++) fixtures.get(i).setPos(25 + i * .01, 0, 0);
            for (TameData data : guardians) { data.homeX = 0; data.homeY = 0; }
            GuardianActivityService.rebuild(helper.getLevel().getServer());
            for (int round = 0; round < 10; round++) for (TameData data : guardians) {
                data.homeY++; GuardianActivityService.hasNearbyPlayer(helper.getLevel().getServer(), data);
            }
            start = System.nanoTime();
            for (int round = 0; round < 50; round++) for (TameData data : guardians) {
                data.homeY++;
                check(!GuardianActivityService.hasNearbyPlayer(helper.getLevel().getServer(), data), "dense candidates must remain outside activation sphere");
            }
            double denseMillisPerPass = (System.nanoTime() - start) / 1_000_000.0D / 50;
            LogUtils.getLogger().info("[GuardianProximityBenchmark] 500 guardians x 40 distant players: uncached spatial queries {} ms/pass (two-second cache); cached {} ms/pass. Proximity only, excludes AI, IO and combat.",
                    millisPerPass, cachedMillisPerPass);
            LogUtils.getLogger().info("[GuardianProximityBenchmark] 500 guardians x 40 nearby candidate players, all rejected by distance: {} ms/pass (two-second cache).", denseMillisPerPass);
        } finally { players.removeAll(fixtures); for (TameData data : guardians) invalidate(data); }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "guardian-budget")
    public static void strictSharedBudgetsAndNonblockingChunkRequests(GameTestHelper helper) throws Exception {
        GuardianWorkBudget limiter = new GuardianWorkBudget();
        int records = 0, transitions = 0, requests = 0;
        for (int i = 0; i < 500; i++) {
            if (limiter.tryInspect(100)) records++;
            if (limiter.tryTransition(100)) transitions++;
            if (limiter.tryChunkRequest(100)) requests++;
        }
        check(records == 32 && transitions == 2 && requests == 1, "500 requests must respect all shared tick limits");
        check(!limiter.tryInspect(100) && !limiter.tryTransition(100), "repeated callbacks cannot reset the limits");
        check(limiter.tryInspect(101) && limiter.tryTransition(101), "per-tick budgets must reset next tick");
        check(!limiter.tryChunkRequest(109) && limiter.tryChunkRequest(110), "chunk loads must be spaced ten ticks apart");

        var server = helper.getLevel().getServer();
        GuardianActivityService.rebuild(server);
        var pendingField = GuardianActivityService.class.getDeclaredField("WAKE_TICKETS");
        pendingField.setAccessible(true);
        var pending = (Map<?, ?>) pendingField.get(null);
        List<TameData> walkers = new ArrayList<>();
        long start = System.nanoTime();
        try {
            for (int i = 0; i < 500; i++) {
                check(!GuardianActivityService.prepareDestination(helper.getLevel(),
                        new net.minecraft.world.phys.Vec3(1_000_000 + i * 32, 100, 1_000_000)), "test destinations should be unloaded");
            }
            check(pending.size() == 1, "500 unloaded destinations must schedule only one asynchronous request this tick");
            double chunkRequestMillis = (System.nanoTime() - start) / 1_000_000.0D;
            Wolf walker = EntityType.WOLF.create(helper.getLevel());
            walker.setTame(true); walker.setOwnerUUID(UUID.randomUUID());
            BlockPos position = helper.absolutePos(new BlockPos(2, 2, 2));
            walker.moveTo(position.getX() + .5, position.getY(), position.getZ() + .5);
            var cacheHome = TameCommands.class.getDeclaredMethod("cacheGuardianRestHome", net.minecraft.world.entity.LivingEntity.class, TameData.class);
            cacheHome.setAccessible(true);
            TameData coldBed = new TameData(walker);
            coldBed.hasPetBed = true; coldBed.petBedDimension = helper.getLevel().dimension().location().toString();
            coldBed.petBedX = 2_000_000; coldBed.petBedY = 100; coldBed.petBedZ = 2_000_000;
            check(!(boolean) cacheHome.invoke(null, walker, coldBed), "bed cache must defer an unloaded bed instead of synchronously resolving it");
            check(coldBed.guardianRestHome.isEmpty() && pending.size() == 1, "cold-bed lookup must share the global chunk budget");
            for (int i = 0; i < 500; i++) {
                TameData data = new TameData(walker); data.tlId = UUID.randomUUID(); walkers.add(data);
                GuardianActivityService.holdRestWalk(data, walker);
            }
            var centersField = GuardianActivityService.class.getDeclaredField("REST_WALK_CENTERS");
            centersField.setAccessible(true);
            check(((Map<?, ?>) centersField.get(null)).size() == GuardianWorkBudget.MAX_REST_WALK_GUARDIANS,
                    "500 hungry guardians must not create more than four walking-ticket groups");
            LogUtils.getLogger().info("[GuardianBudgetRegression] 500 missing-chunk requests scheduled one ticket in {} ms without waiting for chunks.",
                    chunkRequestMillis);
        } finally {
            for (TameData data : walkers) GuardianActivityService.releaseRestWalk(data);
            GuardianActivityService.rebuild(server);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "guardian-budget", timeoutTicks = 100)
    public static void boundedQueueMakesProgressForFiveHundredRecords(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        GuardianActivityService.rebuild(server);
        List<TameData> records = new ArrayList<>();
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        for (int i = 0; i < 500; i++) {
            TameData data = new TameData(wolf);
            data.uuid = UUID.randomUUID(); data.tlId = UUID.randomUUID(); data.name = "Queue guardian " + i;
            data.hasHome = true; data.movementOrder = 3;
            TameRegistry.register(data); records.add(data);
        }
        var workField = GuardianActivityService.class.getDeclaredField("WORK");
        workField.setAccessible(true);
        var work = (Map<?, ?>) workField.get(null);
        var dueField = work.values().iterator().next().getClass().getDeclaredField("nextMaintenance");
        dueField.setAccessible(true);
        GuardianActivityService.maintain(server);
        GuardianActivityService.maintain(server);
        int inspected = 0;
        for (TameData data : records) if (dueField.getInt(work.get(data)) != 0) inspected++;
        check(inspected > 0 && inspected <= 32, "two maintenance calls in one tick must inspect at most 32 records");
        helper.runAfterDelay(40, () -> {
            try {
                for (TameData data : records)
                    check(work.get(data) != null && dueField.getInt(work.get(data)) != 0, "bounded queue must eventually visit every record");
                helper.succeed();
            } catch (IllegalAccessException error) { throw new AssertionError(error); }
            finally { for (TameData data : records) TameRegistry.remove(data.uuid); }
        });
    }
}
