package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Opt-in integration suite; excluded from production builds. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class BewereagerRegression {
    private static int readyTick = -1;
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    @SubscribeEvent
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tl.bewereagerRegression")) return;
        ServerLevel level = event.getServer().overworld();
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            level.setChunkForced(x, z, true);
            level.getChunk(x, z);
        }
        for (int x = -24; x <= 32; x++) for (int z = -24; z <= 24; z++) {
            level.setBlockAndUpdate(new BlockPos(x, 70, z), Blocks.STONE.defaultBlockState());
        }
        // Fresh chunks must become available to navigation before exercising physical movement.
        readyTick = event.getServer().getTickCount() + 40;
    }
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || readyTick < 0 || event.getServer().getTickCount() < readyTick) return;
        readyTick = -1;
        runSuite(event.getServer());
    }
    private static void runSuite(MinecraftServer server) {
        try {
            ServerLevel level = server.overworld();
            FakePlayer owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CurseRegression"));
            level.setDayTime(18000);
            TameData data = createWolf(level, owner);
            UUID wolfId = data.uuid;
            Wolf wolf = (Wolf) level.getEntity(wolfId);
            TameableUtils.setHasCollar(wolf, true);
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            wolf.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(12);
            wolf.setHealth(100);
            BewereagerCompat.transform(wolf);
            Mob body = body(level, data);
            check(BewereagerCompat.isLocked(data) && data.isInactive(), "temporary form must lock commands");
            check(!TameEntityAdapter.isTame(body), "temporary form is hostile");
            check(body.targetSelector.getAvailableGoals().size() >= 4, "temporary form retains hostile targeting");
            check(TameableUtils.hasCollar(body), "collar survives transformation");
            check(body.getMaxHealth() == 100, "wolf max health survives transformation");
            check(body.getAttributeValue(Attributes.ATTACK_DAMAGE) == 12, "wolf damage survives transformation");
            check(BewereagerCompat.dataFor(body) == data && data.attributeLevels.get("quicky") == 2
                    && data.abilityLevels.get("llama_spit") == 3, "hostile form uses wolf attributes and abilities");
            check(data.uuid.equals(wolfId) && data.entitySnapshot.getString("id").equals("minecraft:wolf"), "canonical wolf snapshot");
            TameData persisted = TameData.fromTag(data.toTag());
            check(BewereagerCompat.isLocked(persisted) && persisted.bewereagerState.getCompound("Wolf").equals(data.bewereagerState.getCompound("Wolf")), "restart retains protected snapshot");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DIItemRegistry.SINISTER_CARROT.get(), 2));
            body.setHealth(60);
            BewereagerCompat.feed(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, body));
            check(BewereagerCompat.isLocked(data) && owner.getMainHandItem().getCount() == 2, "60 HP must reject carrot");
            body.setHealth(59);
            BewereagerCompat.feed(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, body));
            check(!TameEntityAdapter.isTame(body) && BewereagerCompat.isLocked(data), "carrot cannot tame below 60 HP");
            check(owner.getMainHandItem().getCount() == 2, "rejected carrot is not consumed");
            level.setDayTime(24000);
            BewereagerCompat.tickServer(server);
            check(level.getEntity(wolfId) instanceof Wolf && data.level == 17, "dawn restores wolf without bonus levels");

            level.setDayTime(210000);
            TameData dawnData = createWolf(level, owner);
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            Mob nightBody = body(level, dawnData);
            CompoundTag stale = new CompoundTag(); nightBody.save(stale);
            // Exercise the absent-body recovery branch and delayed chunk entity load.
            nightBody.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            level.setDayTime(216000);
            BewereagerCompat.tickServer(server);
            check(level.getEntity(dawnData.uuid) instanceof Wolf && !BewereagerCompat.isLocked(dawnData), "dawn restores absent body");
            Entity oldBody = EntityType.loadEntityRecursive(stale, level, entity -> entity);
            check(oldBody != null && !level.addFreshEntity(oldBody), "stale chunk body cannot duplicate restored wolf");

            level.setDayTime(402000);
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            Mob doomed = body(level, dawnData);
            int xpBeforeDeath = dawnData.xp;
            int carrotsBeforeDeath = owner.getMainHandItem().getCount();
            doomed.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            BewereagerCompat.tickServer(server);
            check(level.getEntity(dawnData.uuid) instanceof Wolf && !BewereagerCompat.isLocked(dawnData), "death respawns wolf");
            check(dawnData.level == 27 && dawnData.kills == 9, "killed curse grants exactly ten levels and keeps kills");
            check(dawnData.abilityLevels.get("llama_spit") >= 3, "wolf abilities retained");
            check(dawnData.xp == xpBeforeDeath && owner.getMainHandItem().getCount() == carrotsBeforeDeath,
                    "bonus levels preserve partial XP and consume no items");
            BewereagerCompat.tickServer(server);
            check(dawnData.level == 27, "reward is applied only once");
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            check(!BewereagerCompat.isLocked(dawnData), "death immunity prevents immediate retransformation");
            check(dawnData.bewereagerState.getString("Notification").contains("respawned as a wolf"), "offline death notification retained");
            System.out.println("BEWEREAGER_REGRESSION_PASS checks=" + checks);
        } catch (Throwable failure) {
            System.err.println("BEWEREAGER_REGRESSION_FAIL");
            failure.printStackTrace();
        } finally { server.halt(false); }
    }
    private static TameData createWolf(ServerLevel level, FakePlayer owner) {
        Wolf wolf = EntityType.WOLF.create(level);
        wolf.setTame(true); wolf.setOwnerUUID(owner.getUUID()); wolf.moveTo(0, 71, 0);
        TameSpawnEvents.beginTameReconstruction();
        try { check(level.addFreshEntity(wolf), "spawn test wolf"); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        TameData data = new TameData(wolf);
        data.level = 17; data.xp = 7; data.attributeLevels.put("quicky", 2); data.kills = 9; data.abilityLevels.put("llama_spit", 3);
        TameRegistry.register(data);
        TameRegistry.bindEntityToData(wolf, data);
        return data;
    }
    private static Mob body(ServerLevel level, TameData data) {
        return (Mob) level.getEntity(data.bewereagerState.getUUID("Body"));
    }
}
