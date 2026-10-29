package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class CrittersFollowRegression {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static Set<UUID> rankedPool() throws Exception {
        var field = TameCommands.class.getDeclaredField("RANKED_POOL"); field.setAccessible(true);
        return (Set<UUID>) field.get(null);
    }
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final FakePlayer owner;
        final TamableAnimal pet;
        final TameData data;
        final FollowOwnerGoal follow;
        final Map<UUID, ServerPlayer> online;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper h, String species) throws Exception {
            this.h = h;
            owner = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "CritterOwner"));
            var base = h.absolutePos(new BlockPos(2, 3, 2));
            owner.setPos(base.getX() + 24.5, base.getY(), base.getZ() + .5);
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) {
                var floor = owner.blockPosition().offset(x, -1, z);
                h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                h.getLevel().setBlockAndUpdate(floor.above(), Blocks.AIR.defaultBlockState());
                h.getLevel().setBlockAndUpdate(floor.above(2), Blocks.AIR.defaultBlockState());
            }
            var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
            field.setAccessible(true); online = (Map<UUID, ServerPlayer>) field.get(h.getLevel().getServer().getPlayerList());
            online.put(owner.getUUID(), owner); h.getLevel().players().add(owner);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("crittersandcompanions", species));
            pet = (TamableAnimal) type.create(h.getLevel());
            pet.setTame(true); pet.setOwnerUUID(owner.getUUID()); pet.setOrderedToSit(false);
            pet.setPos(base.getX() + .5, base.getY(), base.getZ() + .5); pet.setNoAi(true);
            h.getLevel().addFreshEntity(pet);
            data = new TameData(pet, owner.getUUID(), false); TameRegistry.register(data);
            follow = pet.goalSelector.getAvailableGoals().stream().map(w -> w.getGoal())
                    .filter(g -> g instanceof FollowOwnerGoal).map(g -> (FollowOwnerGoal) g).findFirst().orElseThrow();
        }
        void startFollow() { check(follow.canUse(), "ordinary native follow can start"); follow.start(); }
        void assertNormalTeleportRestored() {
            follow.stop(); startFollow(); pet.getRandom().setSeed(0L);
            for (int i = 0; i < 30 && pet.distanceToSqr(owner) > 64; i++) follow.tick();
            check(pet.distanceToSqr(owner) <= 64, "ordinary native owner teleport resumes after assignment clears");
        }
        @Override public void close() {
            try { rankedPool().remove(pet.getUUID()); } catch (Exception e) { throw new RuntimeException(e); }
            follow.stop(); TameRegistry.remove(pet.getUUID());
            var current = h.getLevel().getEntity(pet.getUUID()); if (current != null && current != pet) current.discard();
            pet.discard();
            online.remove(owner.getUUID()); h.getLevel().players().remove(owner);
        }
    }
    static void guardian(GameTestHelper h, String species) throws Exception {
        try (var f = new Fixture(h, species)) {
            f.startFollow();
            f.data.hasHome = true; f.data.movementOrder = 3;
            f.data.homeDimension = h.getLevel().dimension().location().toString();
            f.data.homeX = f.pet.blockPosition().getX(); f.data.homeY = f.pet.blockPosition().getY(); f.data.homeZ = f.pet.blockPosition().getZ();
            var before = f.pet.position();
            f.follow.tick();
            check(f.pet.position().distanceToSqr(before) < 0.01, species + " guardian must not teleport to owner during a running native follow tick");
            check(!f.follow.canContinueToUse() && !f.follow.canUse(), "guardian cannot start or continue native owner follow");
            f.data.hasHome = false; f.data.movementOrder = 0;
            check(f.follow.canUse(), "clearing guardian restores following");
            f.assertNormalTeleportRestored();
        }
        h.succeed();
    }
    static void ranked(GameTestHelper h, String species) throws Exception {
        try (var f = new Fixture(h, species)) {
            f.startFollow(); rankedPool().add(f.pet.getUUID());
            check(TameCommands.isDuelSessionLocked(f.pet.getUUID()), "ranked pool lock established before an active round");
            var before = f.pet.position();
            f.follow.tick();
            check(f.pet.position().distanceToSqr(before) < 0.01, species + " ranked waiting participant must not teleport to owner");
            check(!f.follow.canUse() && !f.follow.canContinueToUse(), "ranked lock blocks native follow lifecycle");
            rankedPool().remove(f.pet.getUUID());
            check(f.follow.canUse(), "leaving ranked restores normal following");
            f.assertNormalTeleportRestored();
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void rolypolyGuardian(GameTestHelper h) throws Exception { guardian(h, "roly_poly"); }
    @GameTest(template="empty") public static void snailGuardian(GameTestHelper h) throws Exception { guardian(h, "snail"); }
    @GameTest(template="empty") public static void rolypolyRanked(GameTestHelper h) throws Exception { ranked(h, "roly_poly"); }
    @GameTest(template="empty") public static void snailRanked(GameTestHelper h) throws Exception { ranked(h, "snail"); }
    static void activeRanked(GameTestHelper h, String species) throws Exception {
        try (var f = new Fixture(h, species)) {
            f.startFollow();
            f.owner.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), f.owner);
            var enemy = EntityType.WOLF.create(h.getLevel());
            UUID enemyOwner = UUID.randomUUID();
            enemy.setTame(true); enemy.setOwnerUUID(enemyOwner); enemy.setNoAi(true);
            enemy.setPos(f.pet.getX() + 3, f.pet.getY(), f.pet.getZ());
            h.getLevel().addFreshEntity(enemy); TameRegistry.register(new TameData(enemy, enemyOwner, false));
            try {
                TameDuelManager.startTeamDuel(h.getLevel().getServer(), f.owner.getUUID(), Set.of(f.pet.getUUID()),
                        enemyOwner, Set.of(enemy.getUUID()), Set.of(), false, true);
                check(TameDuelManager.isEntityInDuel(f.pet.getUUID()), "ranked round is active");
                var before = f.pet.position();
                f.follow.tick();
                check(f.pet.position().distanceToSqr(before) < .01, species + " cannot teleport to owner during an active ranked round");
                check(!f.follow.canUse(), "active ranked participant cannot restart native follow");
            } finally {
                TameDuelManager.endDuelsForOwner(h.getLevel().getServer(), f.owner.getUUID());
                TameRegistry.remove(enemy.getUUID());
                var current = h.getLevel().getEntity(enemy.getUUID()); if (current != null && current != enemy) current.discard();
                enemy.discard();
            }
        }
        h.succeed();
    }
    @GameTest(template="empty") public static void rolypolyActiveRanked(GameTestHelper h) throws Exception { activeRanked(h, "roly_poly"); }
    @GameTest(template="empty") public static void snailActiveRanked(GameTestHelper h) throws Exception { activeRanked(h, "snail"); }
}
