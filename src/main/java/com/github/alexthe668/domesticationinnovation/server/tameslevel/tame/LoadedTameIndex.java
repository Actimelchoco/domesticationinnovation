package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Server-thread-only index. Supported untamed entities are retained so later taming needs no scan. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class LoadedTameIndex {
    private static MinecraftServer indexedServer;
    private static final Map<UUID, Set<LivingEntity>> BY_UUID = new HashMap<>();
    private static final Map<UUID, Set<LivingEntity>> BY_TL_ID = new HashMap<>();
    private static final Map<LivingEntity, Ids> IDS = new IdentityHashMap<>();
    private static final Set<LivingEntity> ACCEPTED = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Set<LivingEntity> PENDING_JOINS = Collections.newSetFromMap(new IdentityHashMap<>());

    private static final Map<ServerLevel, Set<LivingEntity>> BY_LEVEL = new IdentityHashMap<>();

    private record Ids(UUID uuid, UUID tlId, ServerLevel level) { }

    private LoadedTameIndex() { }

    private static void useServer(MinecraftServer server) {
        if (indexedServer == server) return;
        clear();
        indexedServer = server;
    }

    private static void clear() {
        BY_UUID.clear();
        BY_TL_ID.clear();
        IDS.clear();
        ACCEPTED.clear();
        PENDING_JOINS.clear();
        BY_LEVEL.clear();
        indexedServer = null;
    }

    public static void refresh(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)
                || entity.isRemoved()
                || (!IDS.containsKey(entity) && !entity.isAddedToWorld()
                    && level.getEntity(entity.getUUID()) != entity)) return;
        track(entity, level.getServer());
    }

    private static void track(LivingEntity entity, MinecraftServer server) {
        if (!TameEntityAdapter.isSupported(entity) || !(entity.level() instanceof ServerLevel level)) return;
        useServer(server);
        // Forge posts join before UUID validation. A rejected addition must never
        // hide the real body or become eligible for lookup on its own.
        boolean accepted = ACCEPTED.contains(entity) || entity.isAddedToWorld()
                || level.getEntity(entity.getUUID()) == entity;
        UUID uuid = entity.getUUID();
        UUID tlId = TameData.getTlId(entity);
        Ids old = IDS.get(entity);
        if (old != null && old.uuid().equals(uuid) && java.util.Objects.equals(old.tlId(), tlId) && old.level() == level) return;
        forget(entity);
        if (accepted) ACCEPTED.add(entity);
        else PENDING_JOINS.add(entity);
        IDS.put(entity, new Ids(uuid, tlId, level));
        BY_LEVEL.computeIfAbsent(level, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
        BY_UUID.computeIfAbsent(uuid, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
        if (tlId != null) BY_TL_ID.computeIfAbsent(tlId, ignored -> Collections.newSetFromMap(new IdentityHashMap<>())).add(entity);
    }

    private static void forget(LivingEntity entity) {
        Ids old = IDS.remove(entity);
        ACCEPTED.remove(entity);
        PENDING_JOINS.remove(entity);
        if (old == null) return;
        // A departing old body must not remove a respawned body's entry.
        Set<LivingEntity> levelEntities = BY_LEVEL.get(old.level());
        if (levelEntities != null) {
            levelEntities.remove(entity);
            if (levelEntities.isEmpty()) BY_LEVEL.remove(old.level());
        }
        remove(BY_UUID, old.uuid(), entity);
        if (old.tlId() != null) remove(BY_TL_ID, old.tlId(), entity);
    }

    private static void remove(Map<UUID, Set<LivingEntity>> index, UUID id, LivingEntity entity) {
        Set<LivingEntity> bodies = index.get(id);
        if (bodies != null && bodies.remove(entity) && bodies.isEmpty()) index.remove(id);
    }

    private static boolean usable(LivingEntity entity, MinecraftServer server) {
        if (entity == null || !(entity.level() instanceof ServerLevel level)
                || level.getServer() != server || entity.isRemoved()
                || !entity.isAlive() || !TameEntityAdapter.isTame(entity)) return false;
        if (entity.isAddedToWorld() || level.getEntity(entity.getUUID()) == entity) ACCEPTED.add(entity);
        // Hidden sections retain live bodies even though getEntity() returns null
        // and Forge has cleared isAddedToWorld during onTrackingEnd.
        return ACCEPTED.contains(entity);
    }

    private static LivingEntity candidate(Set<LivingEntity> bodies, MinecraftServer server, UUID id, boolean stable) {
        if (bodies == null) return null;
        LivingEntity hidden = null;
        for (LivingEntity body : bodies) {
            if (body.isRemoved()) continue;
            if (!usable(body, server) || !id.equals(stable ? TameData.getTlId(body) : body.getUUID())) continue;
            if (((ServerLevel) body.level()).getEntity(body.getUUID()) == body) return body;
            hidden = body;
        }
        return hidden;
    }

    /** Copy before processing: persistence can remove or rebind entities during the pass. */
    public static List<LivingEntity> snapshotForLevel(ServerLevel level) {
        useServer(level.getServer());
        Set<LivingEntity> entities = BY_LEVEL.get(level);
        if (entities == null || entities.isEmpty()) return List.of();
        List<LivingEntity> result = new ArrayList<>(entities.size());
        for (LivingEntity entity : new ArrayList<>(entities)) {
            if (entity.isRemoved()) { forget(entity); continue; }
            if (entity.level() == level && usable(entity, level.getServer())) result.add(entity);
        }
        return result;
    }

    public static LivingEntity find(MinecraftServer server, UUID uuid, UUID tlId) {
        if (server == null) return null;
        useServer(server);
        LivingEntity exact = uuid == null ? null : candidate(BY_UUID.get(uuid), server, uuid, false);
        if (exact != null) return exact;
        // Check UUID in every dimension before falling back to the stable identity.
        // This also handles lookups during another mod's entity-join callback.
        if (uuid != null) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(uuid);
                if (entity instanceof LivingEntity living && usable(living, server)) {
                    track(living, server);
                    return living;
                }
            }
        }
        return tlId == null ? null : candidate(BY_TL_ID.get(tlId), server, tlId, true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level
                && event.getEntity() instanceof LivingEntity living) track(living, level.getServer());
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof LivingEntity living) {
            if (living.isRemoved()) forget(living);
            else {
                // This callback can mean tracking ended, not that the entity unloaded.
                // It also proves the earlier join was accepted by the entity manager.
                track(living, ((ServerLevel) event.getLevel()).getServer());
                if (IDS.containsKey(living)) ACCEPTED.add(living);
            }
        }
    }

    @SubscribeEvent
    public static void finishJoins(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || indexedServer != event.getServer() || PENDING_JOINS.isEmpty()) return;
        for (LivingEntity body : new ArrayList<>(PENDING_JOINS)) {
            if (!body.isRemoved() && body.level() instanceof ServerLevel level
                    && (ACCEPTED.contains(body) || body.isAddedToWorld() || level.getEntity(body.getUUID()) == body)) {
                ACCEPTED.add(body);
                PENDING_JOINS.remove(body);
            } else forget(body);
        }
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        // Join/leave events can already have indexed hidden bodies during startup.
        // A visible-entity backfill cannot replace those entries.
        useServer(event.getServer());
        // One startup backfill; steady-state lookups never enumerate world entities.
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living) track(living, event.getServer());
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (indexedServer == event.getServer()) clear();
    }
}
