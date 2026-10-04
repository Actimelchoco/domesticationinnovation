package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TamePersistenceEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

/** Only included by the opt-in Gradle init script; never included in release builds. */
@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class TamePerformanceRegression {
    private static Wolf wolf(GameTestHelper helper) {
        return helper.spawn(EntityType.WOLF, new BlockPos(1, 1, 1));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Wolf scheduledWolf(GameTestHelper helper, int snapshotSlot) {
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        UUID uuid;
        do { uuid = UUID.randomUUID(); } while (Math.floorMod(uuid.hashCode(), 30) != snapshotSlot);
        wolf.setUUID(uuid);
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.setCustomName(Component.literal("Persistence Test"));
        wolf.moveTo(helper.absolutePos(new BlockPos(1, 1, 1)), 0, 0);
        TameSpawnEvents.beginTameReconstruction();
        try { check(helper.getLevel().addFreshEntity(wolf), "scheduled fixture must spawn"); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        TameRegistry.register(new TameData(wolf));
        return wolf;
    }

    private static void persistenceTick(ServerLevel level, long time) {
        level.getServer().getWorldData().overworldData().setGameTime(time);
        TamePersistenceEvents.onLevelTick(new TickEvent.LevelTickEvent(
                LogicalSide.SERVER, TickEvent.Phase.END, level, () -> true));
    }

    @GameTest(template = "empty")
    public static void persistenceSnapshotsAreStaggered(GameTestHelper helper) {
        var level = helper.getLevel();
        long originalTime = level.getGameTime();
        Wolf first = scheduledWolf(helper, 1);
        Wolf second = scheduledWolf(helper, 2);
        TameData firstData = TameRegistry.get(first.getUUID());
        TameData secondData = TameRegistry.get(second.getUUID());
        try {
            int firstSnapshots = 0, secondSnapshots = 0;
            for (int time = 0; time < 1200; time++) {
                first.getPersistentData().putInt("PersistenceProbe", time);
                second.getPersistentData().putInt("PersistenceProbe", time);
                CompoundTag beforeFirst = firstData.entitySnapshot;
                CompoundTag beforeSecond = secondData.entitySnapshot;
                persistenceTick(level, time);
                if (firstData.entitySnapshot != beforeFirst) {
                    firstSnapshots++;
                    check(time == 40, "first fixture must snapshot in slot one");
                }
                if (secondData.entitySnapshot != beforeSecond) {
                    secondSnapshots++;
                    check(time == 80, "second fixture must snapshot in slot two");
                }
            }
            check(firstSnapshots == 1 && secondSnapshots == 1, "each tame must snapshot once per 1200 ticks");
            check(firstData.entitySnapshot.getCompound("ForgeData").getInt("PersistenceProbe") == 40,
                    "snapshot must contain the actual entity state from its scheduled tick");
            check(secondData.entitySnapshot.getCompound("ForgeData").getInt("PersistenceProbe") == 80,
                    "second snapshot must contain its own scheduled state");
            persistenceTick(level, 1240);
            check(firstData.entitySnapshot.getCompound("ForgeData").getInt("PersistenceProbe") == 1199,
                    "snapshot must repeat in the next minute");
        } finally {
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
            TameRegistry.remove(first.getUUID());
            TameRegistry.remove(second.getUUID());
            first.discard();
            second.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void persistenceLocationAndRecovery(GameTestHelper helper) {
        var level = helper.getLevel();
        long originalTime = level.getGameTime();
        Wolf wolf = scheduledWolf(helper, 2);
        Wolf missing = scheduledWolf(helper, 3);
        TameData data = TameRegistry.get(wolf.getUUID());
        try {
            int initialX = data.lastKnownX;
            data.entitySnapshot = new CompoundTag();
            wolf.setPos(wolf.getX() + 1, wolf.getY(), wolf.getZ());
            persistenceTick(level, 1);
            check(data.lastKnownX == initialX && data.entitySnapshot.isEmpty(), "ordinary ticks must skip persistence work");
            persistenceTick(level, 40);
            check(data.lastKnownX == wolf.blockPosition().getX(), "maintenance must update location");
            check(!data.entitySnapshot.isEmpty(), "maintenance must recover a missing snapshot without waiting a minute");
            persistenceTick(level, 200);
            check(data.lastKnownGameTime == 200, "location changes between save ticks must still be persisted");

            TameRegistry.remove(missing.getUUID());
            persistenceTick(level, 560);
            check(TameRegistry.get(missing.getUUID()) == null, "backfill must respect its own interval");
            persistenceTick(level, 600);
            check(TameRegistry.get(missing.getUUID()) != null, "indexed backfill must restore an unregistered tame");

            wolf.setPos(wolf.getX() + 1, wolf.getY(), wolf.getZ());
            int finalX = wolf.blockPosition().getX();
            level.getServer().getWorldData().overworldData().setGameTime(601);
            wolf.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            check(data.lastKnownX == finalX && data.lastKnownGameTime == 601, "unload must retain the final location");
            check(!LoadedTameIndex.snapshotForLevel(level).contains(wolf), "unloaded tames must leave the level index");
        } finally {
            level.getServer().getWorldData().overworldData().setGameTime(originalTime);
            TameRegistry.remove(wolf.getUUID());
            TameRegistry.remove(missing.getUUID());
            wolf.discard();
            missing.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void identityLifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        Wolf original = wolf(helper);
        UUID identity = UUID.randomUUID();
        // Join happens before taming. The index must still support later taming.
        TameData.syncTlIdToEntity(original, identity);
        check(TameEntityAdapter.findLoaded(server, original.getUUID(), identity) == null, "untamed body must be rejected");
        original.setTame(true);
        check(TameEntityAdapter.findLoaded(server, null, identity) == original, "later taming must need no world scan");
        UUID newIdentity = UUID.randomUUID();
        TameData.syncTlIdToEntity(original, newIdentity);
        check(TameEntityAdapter.findLoaded(server, null, identity) == null, "old identity must be invalidated");
        check(TameEntityAdapter.findLoaded(server, null, newIdentity) == original, "identity change must become visible");

        Wolf replacement = wolf(helper);
        replacement.setTame(true);
        TameData.syncTlIdToEntity(replacement, newIdentity);
        original.discard();
        // Repeated/delayed old-body removal must not evict the replacement.
        LoadedTameIndex.leave(new EntityLeaveLevelEvent(original, level));
        check(TameEntityAdapter.findLoaded(server, original.getUUID(), newIdentity) == replacement, "respawn must resolve new body by stable identity");
        for (int i = 0; i < 10000; i++) {
            check(TameEntityAdapter.findLoaded(server, null, newIdentity) == replacement, "indexed lookup must remain stable");
            check(TameEntityAdapter.findLoaded(server, null, identity) == null, "missing lookup must remain empty");
        }
        replacement.setHealth(0);
        check(TameEntityAdapter.findLoaded(server, replacement.getUUID(), newIdentity) == null, "dead entity must be rejected");
        replacement.setHealth(replacement.getMaxHealth());
        replacement.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(TameEntityAdapter.findLoaded(server, null, newIdentity) == null, "unloaded entity must be forgotten");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void exactUuidWins(GameTestHelper helper) {
        Wolf exact = wolf(helper);
        Wolf other = wolf(helper);
        exact.setTame(true);
        other.setTame(true);
        UUID identity = UUID.randomUUID();
        TameData.syncTlIdToEntity(other, identity);
        check(TameEntityAdapter.findLoaded(helper.getLevel().getServer(), exact.getUUID(), identity) == exact,
                "exact UUID must win over another body's cached stable identity");
        exact.discard();
        other.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void otherDimensionAndModifiedTame(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var nether = server.getLevel(Level.NETHER);
        check(nether != null, "test server must provide the Nether");
        // GameTest has no players in the Nether. An always-ticking fixture makes
        // the body accessible without relying on player-driven chunk visibility.
        Wolf wolf = new Wolf(EntityType.WOLF, nether) {
            @Override public boolean isAlwaysTicking() { return true; }
        };
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.moveTo(1, 70, 1);
        UUID identity = UUID.randomUUID();
        TameData.syncTlIdToEntity(wolf, identity);
        TameSpawnEvents.beginTameReconstruction();
        try { check(nether.addFreshEntity(wolf), "cross-dimension body must load"); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        try {
            check(nether.getEntity(wolf.getUUID()) == wolf, "cross-dimension fixture must be visible");
            check(TameEntityAdapter.findLoaded(server, wolf.getUUID(), identity) == wolf, "UUID lookup must work across dimensions");
            check(TameEntityAdapter.findLoaded(server, UUID.randomUUID(), identity) == wolf, "stable identity must work across dimensions");
        } finally { wolf.discard(); }
        check(TameEntityAdapter.findLoaded(server, null, identity) == null, "removed cross-dimension body must be forgotten");
        var rabbit = helper.spawn(EntityType.RABBIT, new BlockPos(1, 1, 1));
        TameEntityAdapter.setOwner(rabbit, UUID.randomUUID());
        TameData.syncTlIdToEntity(rabbit, identity);
        check(TameEntityAdapter.findLoaded(server, null, identity) == rabbit, "modified tameable species must be indexed");
        rabbit.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rewardLookupAndLiveEffects(GameTestHelper helper) {
        Wolf wolf = wolf(helper);
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        TameData data = new TameData(wolf);
        TameRegistry.register(data);
        try {
            for (String id : LevelSystem.knownAbilityIds()) {
                data.abilityLevels.put(id, 1);
                check(LevelSystem.getAbilityLevel(data, id) == 1, "known ability must resolve: " + id);
                check(LevelSystem.getAbilityLevel(data, " " + id.toUpperCase(java.util.Locale.ROOT) + " ") == 1,
                        "external ability ID normalization must be preserved: " + id);
                check(LevelSystem.getAbilityType(id) != null, "reward map must contain ability: " + id);
            }
            for (String id : LevelSystem.knownAttributeIds()) {
                data.attributeLevels.put(id, 1);
                check(LevelSystem.getAttributeLevel(data, id) == 1, "known attribute must resolve: " + id);
                check(LevelSystem.getAttributeLevel(data, " " + id.toUpperCase(java.util.Locale.ROOT) + " ") == 1,
                        "external attribute ID normalization must be preserved: " + id);
            }
            data.abilityLevels.put("amphibious", 1);
            data.attributeLevels.remove("amphibious");
            check(CommonProxy.getAbilityOrEnchantLevelForCompat(wolf, "amphibious") == 1, "live effect initially enabled");
            data.abilityLevels.remove("amphibious");
            check(CommonProxy.getAbilityOrEnchantLevelForCompat(wolf, "amphibious") == 0, "ability removal must take effect immediately");
            data.attributeLevels.put("amphibious", 1);
            check(CommonProxy.getAbilityOrEnchantLevelForCompat(wolf, "amphibious") == 1, "attribute edit must take effect immediately");
            data.attributeLevels.remove("amphibious");
            data.abilityLevels.put("unknown_custom_ability", 7);
            check(LevelSystem.getAbilityLevel(data, "unknown_custom_ability") == 7, "unknown IDs retain existing fallback behavior");
            data.abilityLevels.put("deflection", Integer.MAX_VALUE);
            check(LevelSystem.getAbilityLevel(data, "deflection") == 1, "known ability must still clamp to its maximum");
            data.attributeLevels.put("amphibious", -5);
            check(LevelSystem.getAttributeLevel(data, "amphibious") == 0, "negative attribute levels must still clamp to zero");
            check(LevelSystem.getAbilityLevel(null, "amphibious") == 0, "null progression must remain supported");
        } finally {
            TameRegistry.remove(data.uuid);
            wolf.discard();
        }
        helper.succeed();
    }
}
