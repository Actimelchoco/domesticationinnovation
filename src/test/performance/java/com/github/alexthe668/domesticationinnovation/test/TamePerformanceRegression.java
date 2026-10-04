package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.entity.TameTagSync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TamePersistenceEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameCombatEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameDailyCareEvents;
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

    private static void dailyTick(GameTestHelper helper, long dayTime) {
        var server = helper.getLevel().getServer();
        server.getWorldData().overworldData().setDayTime(dayTime);
        TameDailyCareEvents.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END, () -> true, server));
    }

    @GameTest(template = "empty")
    public static void dailyCareEligibility(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        long originalTime = server.overworld().getDayTime();
        var fixtures = new java.util.ArrayList<Wolf>();
        try {
            server.getWorldData().overworldData().setDayTime(23999);
            TameDailyCareEvents.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
            for (int i = 0; i < 8; i++) {
                Wolf tame = scheduledWolf(helper, 1);
                fixtures.add(tame);
                tame.setHealth(tame.getMaxHealth());
                TameData data = TameRegistry.get(tame.getUUID());
                data.bornDayTime = 0;
                data.hungerInventory.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF));
            }
            fixtures.get(1).setHealth(fixtures.get(1).getMaxHealth() - 1);
            TameRegistry.get(fixtures.get(2).getUUID()).hungerInventory.clear();
            TameRegistry.get(fixtures.get(3).getUUID()).hungerInventory.set(0,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIRT));
            TameRegistry.get(fixtures.get(4).getUUID()).bornDayTime = 24000;
            TameRegistry.get(fixtures.get(5).getUUID()).dead = true;
            TameRegistry.get(fixtures.get(6).getUUID()).stored = true;
            fixtures.get(7).remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            dailyTick(helper, 23999);
            check(TameRegistry.get(fixtures.get(0).getUUID()).xp == 0, "reward must wait for dawn");
            dailyTick(helper, 24000);
            for (int i = 0; i < fixtures.size(); i++) {
                TameData data = TameRegistry.get(fixtures.get(i).getUUID());
                check(data.xp == (i == 0 ? 1 : 0), "only a loaded, living, unstored, healthy, fed tame from yesterday qualifies");
            }
            check(TameRegistry.get(fixtures.get(0).getUUID()).hungerInventory.get(0).getCount() == 1,
                    "daily care must not consume food");
            fixtures.get(1).setHealth(fixtures.get(1).getMaxHealth());
            dailyTick(helper, 24001);
            check(TameRegistry.get(fixtures.get(1).getUUID()).xp == 0, "healing after dawn must not retroactively qualify");
        } finally {
            for (Wolf tame : fixtures) {
                TameRegistry.remove(tame.getUUID());
                tame.discard();
            }
            server.getWorldData().overworldData().setDayTime(originalTime);
            TameDailyCareEvents.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dailyCarePersistenceAndLevelUp(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        long originalTime = server.overworld().getDayTime();
        Wolf tame = scheduledWolf(helper, 1);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            data.bornDayTime = 0;
            data.hasSavedProgress = true;
            data.savedLevel = 10;
            data.hungerInventory.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF));
            tame.setHealth(tame.getMaxHealth());
            server.getWorldData().overworldData().setDayTime(23999);
            TameDailyCareEvents.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
            dailyTick(helper, 24000);
            check(data.xp == 1, "the fixed daily reward must not be doubled by recovery XP");
            data = TameData.fromTag(data.toTag());
            TameRegistry.register(data);
            check(data.lastDailyCareDay == 1, "daily evaluation marker must survive saving and loading");
            TameDailyCareEvents.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
            dailyTick(helper, 24001);
            check(data.xp == 1, "restarting in the same day must not grant XP");
            dailyTick(helper, 0);
            dailyTick(helper, 24000);
            check(data.xp == 1, "rewinding time must not duplicate a reward");
            dailyTick(helper, 120000);
            check(data.xp == 2, "skipped days must grant only one reward at the observed boundary");
            data.xp = LevelSystem.xpRequiredForLevel(data.level) - 1;
            dailyTick(helper, 144000);
            check(data.level == 2 && data.xp == 0, "daily XP must trigger normal level-up rewards");
        } finally {
            TameRegistry.remove(tame.getUUID());
            tame.discard();
            server.getWorldData().overworldData().setDayTime(originalTime);
            TameDailyCareEvents.started(new net.minecraftforge.event.server.ServerStartedEvent(server));
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void batchedTagUpdates(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        Wolf first = wolf(helper);
        Wolf removed = wolf(helper);
        var flush = TameTagSync.class.getDeclaredMethod("flush", net.minecraft.server.MinecraftServer.class,
                java.util.function.BiConsumer.class);
        flush.setAccessible(true);
        java.util.Map<net.minecraft.world.entity.LivingEntity, CompoundTag> sent = new java.util.IdentityHashMap<>();
        java.util.function.BiConsumer<net.minecraft.world.entity.LivingEntity, CompoundTag> sink = (entity, tag) -> {
            check(sent.put(entity, tag) == null, "one entity must produce at most one payload per flush");
        };
        // Flush previous tests' work before examining this fixture's payloads.
        flush.invoke(null, server, (java.util.function.BiConsumer<net.minecraft.world.entity.LivingEntity, CompoundTag>) (entity, tag) -> { });
        try {
            TameableUtils.setImmuneTime(first, 30);
            TameableUtils.setImmuneTime(first, 29);
            TameableUtils.setBlazingProtectionBars(first, 4);
            TameableUtils.setHealingAuraTime(first, 200);
            check(TameableUtils.getImmuneTime(first) == 29 && TameableUtils.getBlazingProtectionBars(first) == 4,
                    "gameplay must observe updates before packets flush");
            TameableUtils.setImmuneTime(removed, 10);
            removed.discard();
            flush.invoke(null, server, sink);
            check(sent.size() == 1 && sent.containsKey(first), "removed entities must not send stale payloads");
            CompoundTag payload = sent.get(first);
            check(payload.getInt("PetImmunityTimer") == 29 && payload.getInt("PetBlazingProtectionBars") == 4
                    && payload.getInt("PetHealingAuraTime") == 200, "one payload must contain the final combined state");
            sent.clear();
            TameableUtils.setImmuneTime(first, 29);
            TameableUtils.setBlazingProtectionBars(first, 4);
            TameableUtils.setHealingAuraTime(first, 200);
            flush.invoke(null, server, sink);
            check(sent.isEmpty(), "unchanged setters must not produce another packet");
            TameableUtils.setImmuneTime(first, 28);
            check(payload.getInt("PetImmunityTimer") == 29, "queued future updates must not mutate a sent payload");
            flush.invoke(null, server, sink);
            check(sent.get(first).getInt("PetImmunityTimer") == 28, "the next update must still send");
        } finally {
            first.discard();
            removed.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unchangedProgressPreview(GameTestHelper helper) throws Exception {
        Wolf tame = scheduledWolf(helper, 1);
        TameData data = TameRegistry.get(tame.getUUID());
        var flush = TameTagSync.class.getDeclaredMethod("flush", net.minecraft.server.MinecraftServer.class,
                java.util.function.BiConsumer.class);
        flush.setAccessible(true);
        java.util.List<CompoundTag> sent = new java.util.ArrayList<>();
        java.util.function.BiConsumer<net.minecraft.world.entity.LivingEntity, CompoundTag> sink = (entity, tag) -> {
            if (entity == tame) sent.add(tag);
        };
        try {
            data.attributeLevels.put("strength", 3);
            data.abilityLevels.put("snowball", 2);
            TameableUtils.syncAbilityAttributeProgressPreview(tame, data);
            flush.invoke(null, helper.getLevel().getServer(), sink);
            check(sent.size() == 1 && sent.get(0).getCompound("TLAttributeLevelsSync").getInt("strength") == 3,
                    "changed progression must reach the client payload");
            sent.clear();
            data.attributeLevels.put("ignored_zero", 0);
            TameableUtils.syncAbilityAttributeProgressPreview(tame, data);
            flush.invoke(null, helper.getLevel().getServer(), sink);
            check(sent.isEmpty(), "unchanged normalized progression must not send");
            data.attributeLevels.remove("strength");
            data.abilityLevels.clear();
            TameableUtils.syncAbilityAttributeProgressPreview(tame, data);
            flush.invoke(null, helper.getLevel().getServer(), sink);
            check(sent.size() == 1 && sent.get(0).getCompound("TLAbilityLevelsSync").isEmpty()
                    && sent.get(0).getCompound("TLAttributeLevelsSync").isEmpty(), "removing progression must clear the client preview");
        } finally {
            TameRegistry.remove(tame.getUUID());
            tame.discard();
        }
        helper.succeed();
    }

    public static final class HidingWolf extends Wolf {
        int hideFor;
        boolean broken;

        public HidingWolf(ServerLevel level) { super(EntityType.WOLF, level); }

        public int getHideFor() {
            if (broken) throw new IllegalStateException("optional compatibility method failed");
            return hideFor;
        }
    }

    @GameTest(template = "empty")
    public static void cachedHidingCompatibility(GameTestHelper helper) throws Exception {
        var method = TameCombatEvents.class.getDeclaredMethod("isVallumraptorHiding", net.minecraft.world.entity.LivingEntity.class);
        method.setAccessible(true);
        Wolf ordinary = EntityType.WOLF.create(helper.getLevel());
        HidingWolf hiding = new HidingWolf(helper.getLevel());
        check(!(boolean) method.invoke(null, ordinary), "missing optional method must use vanilla visibility");
        ordinary.setInvisible(true);
        check((boolean) method.invoke(null, ordinary), "cached missing method must still read current visibility");
        hiding.hideFor = 5;
        check((boolean) method.invoke(null, hiding), "optional hiding timer must detect hiding");
        hiding.hideFor = 0;
        hiding.setInvisible(true);
        check(!(boolean) method.invoke(null, hiding), "cached method must read changing timer and take precedence");
        hiding.broken = true;
        check((boolean) method.invoke(null, hiding), "failed optional invocation must fall back to vanilla visibility");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void duelParticipantSnapshots(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        Wolf first = scheduledWolf(helper, 1);
        Wolf second = scheduledWolf(helper, 2);
        Wolf unrelated = scheduledWolf(helper, 3);
        TameData firstData = TameRegistry.get(first.getUUID());
        try {
            check(TameDuelManager.loadedDuelParticipants(server).isEmpty(), "no active duel must return no participants");
            TameDuelManager.startTeamDuel(server, first.getOwnerUUID(),
                    java.util.Set.of(firstData.tlId), second.getOwnerUUID(), java.util.Set.of(second.getUUID()));
            var snapshot = TameDuelManager.loadedDuelParticipants(server);
            check(snapshot.size() == 2 && snapshot.contains(first) && snapshot.contains(second),
                    "duel snapshot must resolve both stable identities and entity UUIDs");
            check(!snapshot.contains(unrelated), "unrelated loaded tames must be excluded");
            TameDuelManager.endDuelForTame(server, second.getUUID());
            check(snapshot.size() == 2, "elimination must not mutate a previously returned snapshot");
            check(TameDuelManager.loadedDuelParticipants(server).isEmpty(), "finished duel must return no participants");
        } finally {
            TameDuelManager.endDuelsForOwner(server, first.getOwnerUUID());
            TameDuelManager.endDuelsForOwner(server, second.getOwnerUUID());
            for (Wolf fixture : java.util.List.of(first, second, unrelated)) {
                TameRegistry.remove(fixture.getUUID());
                fixture.discard();
            }
        }
        helper.succeed();
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
