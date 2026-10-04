package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class TameDuplicateRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    @SuppressWarnings("unchecked")
    private static PersistentEntitySectionManager<Entity> manager(ServerLevel level) throws Exception {
        Field field = ServerLevel.class.getDeclaredField("entityManager");
        field.setAccessible(true);
        return (PersistentEntitySectionManager<Entity>) field.get(level);
    }

    private static Wolf fixture(ServerLevel level, BlockPos pos, UUID owner) throws Exception {
        manager(level).updateChunkStatus(new ChunkPos(pos), Visibility.TICKING);
        Wolf wolf = EntityType.WOLF.create(level);
        wolf.setTame(true); wolf.setOwnerUUID(owner); wolf.setNoAi(true); wolf.setNoGravity(true);
        wolf.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        TameSpawnEvents.beginTameReconstruction();
        try { check(level.addFreshEntity(wolf), "fixture must be accepted"); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        TameRegistry.register(new TameData(wolf));
        check(level.getEntity(wolf.getUUID()) == wolf, "fixture must initially be visible");
        return wolf;
    }

    private static void hide(ServerLevel level, Wolf wolf) throws Exception {
        manager(level).updateChunkStatus(new ChunkPos(wolf.blockPosition()), Visibility.HIDDEN);
        check(level.getEntity(wolf.getUUID()) == null && !wolf.isRemoved() && wolf.isAlive(),
                "real tracking-end transition must hide a still-existing body");
    }

    @GameTest(template = "empty")
    public static void hiddenSectionRetainsIdentity(GameTestHelper helper) throws Exception {
        ServerLevel source = helper.getLevel().getServer().getLevel(Level.NETHER);
        Wolf wolf = fixture(source, new BlockPos(128, 70, 128), UUID.randomUUID());
        TameData data = TameRegistry.get(wolf.getUUID());
        try {
            hide(source, wolf);
            check(LoadedTameIndex.find(source.getServer(), data.uuid, null) == wolf, "hidden live body must resolve by UUID");
            check(LoadedTameIndex.find(source.getServer(), null, data.tlId) == wolf, "hidden live body must resolve by stable identity");
            LoadedTameIndex.started(new net.minecraftforge.event.server.ServerStartedEvent(source.getServer()));
            check(LoadedTameIndex.find(source.getServer(), data.uuid, data.tlId) == wolf,
                    "startup backfill must not erase a body that joined before becoming hidden");
            UUID before = data.tlId, changed = UUID.randomUUID();
            TameData.syncTlIdToEntity(wolf, changed);
            check(LoadedTameIndex.find(source.getServer(), null, before) == null
                    && LoadedTameIndex.find(source.getServer(), null, changed) == wolf,
                    "hidden body identity refresh must invalidate the old key");
            check(LoadedTameIndex.snapshotForLevel(source).contains(wolf), "persistence must retain a hidden live body");
            wolf.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            check(LoadedTameIndex.find(source.getServer(), data.uuid, changed) == null,
                    "actual unload from a hidden section must invalidate lookup even without another leave event");
        } finally { TameRegistry.remove(data.uuid); wolf.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rejectedUuidJoinKeepsOriginal(GameTestHelper helper) throws Exception {
        ServerLevel source = helper.getLevel().getServer().getLevel(Level.NETHER);
        Wolf original = fixture(source, new BlockPos(256, 70, 128), UUID.randomUUID());
        TameData data = TameRegistry.get(original.getUUID());
        Wolf rejected = EntityType.WOLF.create(source);
        try {
            hide(source, original);
            rejected.setUUID(original.getUUID()); rejected.setTame(true); rejected.setOwnerUUID(data.ownerUUID);
            TameData.syncTlIdToEntity(rejected, data.tlId);
            rejected.moveTo(original.position());
            TameSpawnEvents.beginTameReconstruction();
            try { check(!source.addFreshEntity(rejected), "duplicate UUID addition must be rejected after the join event"); }
            finally { TameSpawnEvents.endTameReconstruction(); }
            check(LoadedTameIndex.find(source.getServer(), data.uuid, data.tlId) == original,
                    "rejected join must not replace the hidden original in the index");
            original.discard();
            check(LoadedTameIndex.find(source.getServer(), data.uuid, data.tlId) == null,
                    "rejected candidate must not become a phantom body after original removal");
        } finally { TameRegistry.remove(data.uuid); original.discard(); rejected.discard(); }
        helper.succeed();
    }

    private static boolean success(Object result) throws Exception {
        String member = result.getClass().getSimpleName().equals("RecoverResult") ? "entity" : "success";
        Field field = result.getClass().getDeclaredField(member);
        field.setAccessible(true);
        return member.equals("entity") ? field.get(result) != null : field.getBoolean(result);
    }

    @GameTest(template = "empty")
    public static void crossDimensionRebuildRefusesHiddenBody(GameTestHelper helper) throws Exception {
        ServerLevel target = helper.getLevel(), source = target.getServer().getLevel(Level.NETHER);
        UUID owner = UUID.randomUUID();
        Wolf original = fixture(source, new BlockPos(384, 70, 128), owner);
        TameData data = TameRegistry.get(original.getUUID());
        UUID originalUuid = data.uuid;
        var player = FakePlayerFactory.get(target, new GameProfile(owner, "duplicate_test"));
        BlockPos dest = helper.absolutePos(new BlockPos(2, 2, 2));
        try {
            original.save(data.entitySnapshot);
            hide(source, original);
            Method recover = TameCommands.class.getDeclaredMethod("recoverPetEntity",
                    net.minecraft.commands.CommandSourceStack.class, ServerPlayer.class, TameData.class);
            recover.setAccessible(true);
            check(!success(recover.invoke(null, player.createCommandSourceStack(), player, data)),
                    "recover must refuse to create another body in a different dimension");
            Class<?> location = Class.forName(TameCommands.class.getName() + "$SpawnTarget");
            var constructor = location.getDeclaredConstructor(ServerLevel.class, Vec3.class, float.class, float.class);
            constructor.setAccessible(true);
            Object destination = constructor.newInstance(target, Vec3.atBottomCenterOf(dest), 0F, 0F);
            Method recoverAt = TameCommands.class.getDeclaredMethod("recoverPetEntityAtLocation", ServerPlayer.class, location, TameData.class);
            recoverAt.setAccessible(true);
            check(!success(recoverAt.invoke(null, player, destination, data)), "location recovery must also refuse a hidden live body");
            data.dead = true; // A stale death flag must not override physical presence.
            check(!TameCommands.respawnDeadTameAtBed(target, dest, Direction.NORTH, data),
                    "automatic bed respawn must not duplicate a live body behind a stale death flag");
            Method respawn = TameCommands.class.getDeclaredMethod("respawnDeadTameAt",
                    net.minecraft.commands.CommandSourceStack.class, TameData.class, ServerLevel.class, Vec3.class, float.class, float.class);
            respawn.setAccessible(true);
            check(!success(respawn.invoke(null, player.createCommandSourceStack(), data, target, Vec3.atBottomCenterOf(dest), 0F, 0F)),
                    "command respawn must not duplicate a hidden live body");
            check(data.uuid.equals(originalUuid) && target.getEntity(originalUuid) == null && !original.isRemoved(),
                    "refused rebuilds must preserve original body and canonical UUID");
            original.discard();
            check(TameCommands.respawnDeadTameAtBed(target, dest, Direction.NORTH, data),
                    "legitimate respawn must work after actual removal");
            check(LoadedTameIndex.find(target.getServer(), originalUuid, data.tlId) != null && !data.dead,
                    "replacement must retain canonical identity and clear death state");
        } finally {
            var body = LoadedTameIndex.find(target.getServer(), originalUuid, data.tlId);
            TameRegistry.remove(data.uuid);
            if (body != null) body.discard();
            original.discard();
        }
        helper.succeed();
    }
}
