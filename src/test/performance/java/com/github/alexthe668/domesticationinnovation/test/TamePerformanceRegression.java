package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
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
