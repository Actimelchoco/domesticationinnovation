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
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** Opt-in integration suite; excluded from production builds. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class BewereagerRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    @SubscribeEvent
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("tl.bewereagerRegression")) return;
        try {
            ServerLevel level = event.getServer().overworld();
            FakePlayer owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "CurseRegression"));
            level.setDayTime(18000);
            TameData data = createWolf(level, owner);
            UUID wolfId = data.uuid;
            Wolf wolf = (Wolf) level.getEntity(wolfId);
            TameableUtils.setHasCollar(wolf, true);
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
            wolf.setHealth(100);
            BewereagerCompat.transform(wolf);
            Mob body = body(level, data);
            check(BewereagerCompat.isLocked(data) && data.isInactive(), "temporary form must lock commands");
            check(!TameEntityAdapter.isTame(body), "temporary form is hostile");
            check(TameableUtils.hasCollar(body), "collar survives transformation");
            check(body.getMaxHealth() == 100, "wolf max health survives transformation");
            check(data.uuid.equals(wolfId) && data.entitySnapshot.getString("id").equals("minecraft:wolf"), "canonical wolf snapshot");
            TameData persisted = TameData.fromTag(data.toTag());
            check(BewereagerCompat.isLocked(persisted) && persisted.bewereagerState.getCompound("Wolf").equals(data.bewereagerState.getCompound("Wolf")), "restart retains protected snapshot");
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DIItemRegistry.SINISTER_CARROT.get(), 2));
            body.setHealth(60);
            BewereagerCompat.feed(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, body));
            check(BewereagerCompat.isLocked(data) && owner.getMainHandItem().getCount() == 2, "60 HP must reject carrot");
            body.setHealth(59);
            BewereagerCompat.feed(new PlayerInteractEvent.EntityInteract(owner, InteractionHand.MAIN_HAND, body));
            check(TameEntityAdapter.isTame(body) && !BewereagerCompat.isLocked(data), "carrot permanently tames below 60 HP");
            check(data.level == 17 && data.kills == 9 && data.abilityLevels.get("llama_spit") == 3, "progress retained");
            check(owner.getMainHandItem().getCount() == 1 && TameableUtils.hasCollar(body), "one carrot consumed and collar retained");
            level.setDayTime(24000);
            BewereagerCompat.tickServer(event.getServer());
            check(body.isAlive() && TameEntityAdapter.isTame(body), "permanent form survives dawn");
            body.discard();

            level.setDayTime(210000);
            TameData dawnData = createWolf(level, owner);
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            Mob nightBody = body(level, dawnData);
            CompoundTag stale = new CompoundTag(); nightBody.save(stale);
            // Exercise the absent-body recovery branch and delayed chunk entity load.
            nightBody.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            level.setDayTime(216000);
            BewereagerCompat.tickServer(event.getServer());
            check(level.getEntity(dawnData.uuid) instanceof Wolf && !BewereagerCompat.isLocked(dawnData), "dawn restores absent body");
            Entity oldBody = EntityType.loadEntityRecursive(stale, level, entity -> entity);
            check(oldBody != null && !level.addFreshEntity(oldBody), "stale chunk body cannot duplicate restored wolf");

            level.setDayTime(402000);
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            Mob doomed = body(level, dawnData);
            doomed.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            BewereagerCompat.tickServer(event.getServer());
            check(level.getEntity(dawnData.uuid) instanceof Wolf && !BewereagerCompat.isLocked(dawnData), "death respawns wolf");
            BewereagerCompat.transform((Wolf) level.getEntity(dawnData.uuid));
            check(!BewereagerCompat.isLocked(dawnData), "death immunity prevents immediate retransformation");
            check(dawnData.bewereagerState.getString("Notification").contains("respawned as a wolf"), "offline death notification retained");
            System.out.println("BEWEREAGER_REGRESSION_PASS");
        } catch (Throwable failure) {
            System.err.println("BEWEREAGER_REGRESSION_FAIL");
            failure.printStackTrace();
        } finally { event.getServer().halt(false); }
    }
    private static TameData createWolf(ServerLevel level, FakePlayer owner) {
        Wolf wolf = EntityType.WOLF.create(level);
        wolf.setTame(true); wolf.setOwnerUUID(owner.getUUID()); wolf.moveTo(0, 71, 0);
        TameSpawnEvents.beginTameReconstruction();
        try { check(level.addFreshEntity(wolf), "spawn test wolf"); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        TameData data = new TameData(wolf);
        data.level = 17; data.kills = 9; data.abilityLevels.put("llama_spit", 3);
        TameRegistry.register(data);
        TameRegistry.bindEntityToData(wolf, data);
        return data;
    }
    private static Mob body(ServerLevel level, TameData data) {
        return (Mob) level.getEntity(data.bewereagerState.getUUID("Body"));
    }
}
