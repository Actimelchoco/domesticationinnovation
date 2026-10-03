package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** The registry remains a wolf while its temporary hostile body exists. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class BewereagerCompat {
    public static final String TEMPORARY = "TLTemporaryBewereager";
    private static final ResourceLocation TYPE = new ResourceLocation("species", "bewereager");
    private BewereagerCompat() {}
    public static boolean isTemporary(Entity entity) { return entity != null && entity.getPersistentData().getBoolean(TEMPORARY); }
    public static boolean isLocked(TameData data) { return data != null && data.bewereagerState.getBoolean("Active"); }

    public static void transform(Wolf wolf) {
        if (!(wolf.level() instanceof ServerLevel level) || !wolf.isAlive()) return;
        if (wolf.getPersistentData().getLong("TLBewereagerImmuneUntil") > level.getDayTime()) return;
        TameData data = TameRegistry.get(wolf.getUUID());
        if (data == null || isLocked(data) || TameDuelManager.isTameInDuel(wolf.getUUID())) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(TYPE);
        Entity created = type == null ? null : type.create(level);
        if (!(created instanceof Mob beast)) return;
        CompoundTag wolfSnapshot = new CompoundTag();
        if (!wolf.save(wolfSnapshot)) return;
        // Copy the actual wolf attributes, including external bonuses, into the
        // other body without loading wolf-specific state into a different species.
        CompoundTag beastSnapshot = new CompoundTag();
        beast.save(beastSnapshot);
        beastSnapshot.put("Attributes", wolfSnapshot.getList("Attributes", 10).copy());
        beastSnapshot.putBoolean("FromWolf", true);
        beastSnapshot.putUUID("Owner", data.ownerUUID);
        beastSnapshot.putInt("CollarColor", wolf.getCollarColor().getId());
        beast.load(beastSnapshot);
        beast.getPersistentData().putBoolean(TEMPORARY, true);
        beast.setCustomName(wolf.getCustomName());
        beast.moveTo(wolf.getX(), wolf.getY(), wolf.getZ(), wolf.getYRot(), wolf.getXRot());
        TameData.syncTlIdToEntity(beast, data.ensureTlId());
        TameableUtils.copyCollar(wolf, beast);
        beast.setHealth(beast.getMaxHealth());
        CompoundTag previousState = data.bewereagerState.copy();
        data.bewereagerState.putBoolean("Active", true);
        data.bewereagerState.put("Wolf", wolfSnapshot.copy());
        data.bewereagerState.putUUID("Body", beast.getUUID());
        data.bewereagerState.putLong("Dawn", (Math.floorDiv(level.getDayTime(), 24000L) + 1L) * 24000L);
        if (!level.addFreshEntity(beast)) { data.bewereagerState = previousState; return; }
        data.entitySnapshot = wolfSnapshot;
        updateLocation(data, beast);
        // Keep the wolf UUID canonical. Temporary bodies are never registry tames.
        wolf.discard();
        notify(data, level.getServer(), data.name + " turned into a hostile Bewereager and cannot be controlled.");
        TameRegistry.markDirty();
    }

    public static TameData dataFor(Entity body) {
        UUID id = body instanceof LivingEntity living ? TameData.getTlId(living) : null;
        TameData data = id == null ? null : TameRegistry.getByTlId(id);
        return isLocked(data) && data.bewereagerState.hasUUID("Body")
                && body.getUUID().equals(data.bewereagerState.getUUID("Body")) ? data : null;
    }

    private static void updateLocation(TameData data, Entity body) {
        CompoundTag state = data.bewereagerState;
        state.putString("Dimension", body.level().dimension().location().toString());
        state.putDouble("X", body.getX()); state.putDouble("Y", body.getY()); state.putDouble("Z", body.getZ());
        TameRegistry.markDirty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void death(LivingDeathEvent event) {
        if (!isTemporary(event.getEntity()) || event.isCanceled()) return;
        TameData data = dataFor(event.getEntity());
        if (data == null) return;
        updateLocation(data, event.getEntity());
        data.bewereagerState.putBoolean("Killed", true);
        // Suppress drops and the original monster death path; the wolf is restored
        // on the next server tick, outside the damage callback.
        event.setCanceled(true);
        event.getEntity().discard();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!isTemporary(event.getEntity()) || event.getLevel().isClientSide) return;
        if (dataFor(event.getEntity()) == null) event.setCanceled(true); // stale body after dawn/restart
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide || !isTemporary(event.getEntity())) return;
        TameData data = dataFor(event.getEntity());
        if (data != null) updateLocation(data, event.getEntity());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0) return;
        tickServer(server);
    }

    public static void tickServer(MinecraftServer server) {
        for (TameData data : new ArrayList<>(TameRegistry.TAMES.values())) {
            deliverNotification(data, server);
            if (!isLocked(data)) continue;
            CompoundTag state = data.bewereagerState;
            ResourceLocation dimension = ResourceLocation.tryParse(state.getString("Dimension"));
            ServerLevel level = dimension == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null) continue;
            Entity body = level.getEntity(state.getUUID("Body"));
            if (body != null) updateLocation(data, body);
            if (!state.getBoolean("Killed") && level.getDayTime() < state.getLong("Dawn")) continue;
            // This also loads entities saved in an otherwise unloaded chunk. Keep
            // the snapshot fallback for bodies removed by another mod.
            BlockPos pos = BlockPos.containing(state.getDouble("X"), state.getDouble("Y"), state.getDouble("Z"));
            level.getChunk(pos);
            body = level.getEntity(state.getUUID("Body"));
            restoreWolf(data, level, body);
        }
    }

    private static void restoreWolf(TameData data, ServerLevel level, Entity body) {
        CompoundTag state = data.bewereagerState;
        if (body != null) updateLocation(data, body);
        if (level.getEntity(data.uuid) != null) return;
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) return;
        wolf.load(state.getCompound("Wolf").copy());
        wolf.setUUID(data.uuid);
        wolf.moveTo(state.getDouble("X"), state.getDouble("Y"), state.getDouble("Z"), wolf.getYRot(), wolf.getXRot());
        wolf.deathTime = 0; wolf.hurtTime = 0;
        wolf.getPersistentData().putLong("TLBewereagerImmuneUntil", state.getLong("Dawn"));
        wolf.setHealth(wolf.getMaxHealth());
        TameSpawnEvents.beginTameReconstruction();
        boolean added;
        try { added = level.addFreshEntity(wolf); }
        finally { TameSpawnEvents.endTameReconstruction(); }
        if (!added) return;
        if (body != null) body.discard();
        boolean killed = state.getBoolean("Killed");
        String pendingNotification = state.getString("Notification");
        data.bewereagerState = new CompoundTag();
        data.bewereagerState.putString("Notification", pendingNotification);
        TameRegistry.bindEntityToData(wolf, data);
        if (killed) {
            LevelSystem.restoreHighestProgressWithoutXpCost(wolf, data);
            // Run normal level rewards while preserving the wolf's partial XP.
            for (int i = 0; i < 10; i++) {
                int partialXp = data.xp;
                data.xp = LevelSystem.xpRequiredForLevel(data.level);
                LevelSystem.checkLevelUp(wolf, data);
                data.xp = partialXp;
            }
            wolf.setHealth(wolf.getMaxHealth());
        }
        data.entitySnapshot = new CompoundTag(); wolf.save(data.entitySnapshot);
        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = wolf.blockPosition().getX(); data.lastKnownY = wolf.blockPosition().getY(); data.lastKnownZ = wolf.blockPosition().getZ();
        notify(data, level.getServer(), data.name + (killed ? " was killed as a Bewereager and respawned as a wolf, reincarnated for free with 10 extra levels." : " returned to wolf form at dawn."));
        TameRegistry.markDirty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void feed(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getTarget() instanceof Mob beast)
                || !isTemporary(beast) || !event.getItemStack().is(DIItemRegistry.SINISTER_CARROT.get())) return;
        event.setCanceled(true); event.setCancellationResult(InteractionResult.CONSUME);
        // The curse cannot be converted into a permanent pet, even at low health.
    }

    private static void notify(TameData data, MinecraftServer server, String message) {
        String pending = data.bewereagerState.getString("Notification");
        data.bewereagerState.putString("Notification", pending.isEmpty() ? message : pending + "\n" + message);
        deliverNotification(data, server);
    }
    private static void deliverNotification(TameData data, MinecraftServer server) {
        if (data == null || data.ownerUUID == null) return;
        String message = data.bewereagerState.getString("Notification");
        ServerPlayer owner = server.getPlayerList().getPlayer(data.ownerUUID);
        if (owner != null && !message.isEmpty()) {
            owner.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.YELLOW));
            data.bewereagerState.remove("Notification"); TameRegistry.markDirty();
        }
    }

}
