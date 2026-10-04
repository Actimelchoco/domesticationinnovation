package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class OfflineGuardianRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static Wolf wolf(ServerLevel level, UUID owner, String name, BlockPos pos, boolean spawn) {
        Wolf wolf = new Wolf(EntityType.WOLF, level) {
            @Override public boolean isAlwaysTicking() { return true; }
        };
        wolf.setNoAi(true); wolf.setNoGravity(true);
        wolf.setOwnerUUID(owner); wolf.setTame(true);
        wolf.setCustomName(Component.literal(name));
        wolf.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        if (spawn) check(level.addFreshEntity(wolf), "fixture must spawn");
        TameRegistry.register(new TameData(wolf));
        return wolf;
    }

    @GameTest(template = "empty")
    public static void selectionLimitOrderAndPersistence(GameTestHelper helper) {
        UUID owner = UUID.randomUUID(), other = UUID.randomUUID();
        List<Wolf> fixtures = new ArrayList<>();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        try {
            for (int i = 0; i < 6; i++) {
                Wolf wolf = wolf(helper.getLevel(), owner, "Offline " + i, pos, false); fixtures.add(wolf);
                TameData data = TameRegistry.get(wolf.getUUID());
                TameData evicted = OfflineGuardianService.set(data, helper.getLevel(), pos);
                check(OfflineGuardianService.selected(owner).size() == Math.min(i + 1, 5), "per-owner limit must be five");
                if (i == 5) check(evicted == TameRegistry.get(fixtures.get(0).getUUID()) && evicted.offlineGuardianOrder == 0,
                        "sixth selection must evict first assignment and keep tame");
            }
            Wolf separate = wolf(helper.getLevel(), other, "Other owner", pos, false); fixtures.add(separate);
            OfflineGuardianService.set(TameRegistry.get(separate.getUUID()), helper.getLevel(), pos);
            TameData first = TameRegistry.get(fixtures.get(1).getUUID());
            long originalOrder = first.offlineGuardianOrder;
            OfflineGuardianService.set(first, helper.getLevel(), pos.east());
            check(first.offlineGuardianOrder == originalOrder && OfflineGuardianService.selected(owner).get(0) == first,
                    "updating existing assignment must preserve FIFO position");
            check(OfflineGuardianService.selected(other).size() == 1, "owner rosters must be independent");
            first.offlineGuardianDeployed = true;
            TameData restored = TameData.fromTag(first.toTag());
            check(restored.offlineGuardianDeployed && restored.offlineGuardianOrder == originalOrder
                    && OfflineGuardianService.post(restored).equals(pos.east()), "selection, deployment and post must persist");
            check(TameRegistry.transferOwnership(first.uuid, other) && first.offlineGuardianOrder == 0
                    && !first.offlineGuardianDeployed, "ownership transfer must clear assignment to preserve new owner's quota");
        } finally {
            for (Wolf wolf : fixtures) { TameRegistry.remove(wolf.getUUID()); wolf.discard(); }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandsDeployOfflineAndReleaseTickets(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.randomUUID();
        BlockPos source = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos post = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(post.below(), Blocks.STONE.defaultBlockState());
        Wolf wolf = wolf(level, owner, "Offline Keeper", source, true);
        TameData data = TameRegistry.get(wolf.getUUID());
        var player = FakePlayerFactory.get(level, new GameProfile(owner, "guardian_test"));
        player.moveTo(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D);
        var dispatcher = level.getServer().getCommands().getDispatcher();
        ForcedChunksSavedData forced = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData::load, ForcedChunksSavedData::new, "chunks");
        var baseline = new HashSet<>(forced.getEntityForcedChunks().getTickingChunks().keySet());
        try {
            check(level.getServer().getPlayerList().getPlayer(owner) == null, "owner must be offline");
            check(dispatcher.execute("tames offlineGuardian", player.createCommandSourceStack()) == 1, "rules command must work");
            check(dispatcher.execute("tames offlineGuardian set Offline Keeper", player.createCommandSourceStack()) == 1, "set command must accept a spaced name");
            check(!data.offlineGuardianDeployed, "set must save without deploying");
            check(dispatcher.execute("tames offlineGuardian deploy", player.createCommandSourceStack()) == 1, "nameless deploy must activate selected guardians");
            OfflineGuardianService.maintain(level.getServer());
            check(data.movementOrder == 3 && data.hasHome && data.homeX == post.getX()
                    && wolf.distanceToSqr(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D) <= 9.0D, "offline deployment: order=" + data.movementOrder + " home=" + data.hasHome + " homeX=" + data.homeX + " post=" + post + " body=" + wolf.position() + " pending=" + data.offlineGuardianNeedsDeployment + " visible=" + (level.getEntity(wolf.getUUID()) == wolf));
            var tickets = forced.getEntityForcedChunks().getTickingChunks();
            var ticket = tickets.keySet().stream().filter(key -> !baseline.contains(key)).findFirst().orElseThrow();
            check(tickets.get(ticket).size() >= 9 && tickets.get(ticket).contains(new ChunkPos(post).toLong()), "guard post must have real entity-ticking tickets");
            OfflineGuardianService.rebuild(level.getServer());
            check(tickets.containsKey(ticket) && data.offlineGuardianDeployed, "restart rebuild must restore tickets from persistent assignment");
            check(dispatcher.execute("tames offlineGuardian deploy Offline Keeper", player.createCommandSourceStack()) == 1, "named deploy must work");
            TameCommands.applyMovementOrderCode(wolf, 0);
            OfflineGuardianService.maintain(level.getServer());
            check(!data.offlineGuardianDeployed && data.offlineGuardianOrder > 0 && !tickets.containsKey(ticket), "regular movement must release tickets without losing saved selection");
        } finally { TameRegistry.remove(wolf.getUUID()); wolf.discard(); level.removeBlock(post.below(), false); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void morningRespawnReturnsToPostWithOfflineOwner(GameTestHelper helper) throws Exception {
        ServerLevel level = helper.getLevel();
        UUID owner = UUID.randomUUID();
        BlockPos bed = helper.absolutePos(new BlockPos(1, 2, 1));
        BlockPos post = helper.absolutePos(new BlockPos(3, 2, 3));
        level.setBlockAndUpdate(bed.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(post.below(), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(bed, DIBlockRegistry.ORANGE_PET_BED.get().defaultBlockState());
        Wolf original = wolf(level, owner, "Offline Respawn", post, true);
        TameData data = TameRegistry.get(original.getUUID());
        try {
            data.hasPetBed = true; data.petBedDimension = level.dimension().location().toString();
            data.petBedX = bed.getX(); data.petBedY = bed.getY(); data.petBedZ = bed.getZ();
            OfflineGuardianService.set(data, level, post);
            check(OfflineGuardianService.deploy(level.getServer(), data), "guardian must deploy");
            original.save(data.entitySnapshot);
            original.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            data.dead = true; data.deadGameTime = 0;
            OfflineGuardianService.maintain(level.getServer());
            Method morning = TameCommands.class.getDeclaredMethod("processMorningPetBedRespawns", net.minecraft.server.MinecraftServer.class);
            morning.setAccessible(true);
            morning.invoke(null, level.getServer());
            var rebuilt = LoadedTameIndex.find(level.getServer(), data.uuid, data.tlId);
            check(rebuilt != null && rebuilt.isAlive() && !data.dead, "normal bed respawn must work with offline owner");
            check(data.offlineGuardianDeployed && data.movementOrder == 3 && data.hasHome
                    && rebuilt.distanceToSqr(post.getX() + 0.5D, post.getY(), post.getZ() + 0.5D) <= 9.0D,
                    "respawned offline guardian must return to saved post");
        } finally {
            var rebuilt = LoadedTameIndex.find(level.getServer(), data.uuid, data.tlId);
            TameRegistry.remove(data.uuid);
            if (rebuilt != null) rebuilt.discard();
            original.discard();
            level.removeBlock(bed, false); level.removeBlock(bed.below(), false); level.removeBlock(post.below(), false);
        }
        helper.succeed();
    }
}
