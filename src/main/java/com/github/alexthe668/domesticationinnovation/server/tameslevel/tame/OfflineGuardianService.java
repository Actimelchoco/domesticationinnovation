package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent five-per-owner assignments, with bounded ticking tickets for deployed posts. */
public final class OfflineGuardianService {
    public static final int MAX_GUARDIANS = 5;
    private record ChunkTicket(ResourceKey<Level> dimension, int x, int z) { }
    private static final Set<UUID> DEPLOYED = new HashSet<>();
    private static final Map<UUID, Set<ChunkTicket>> TICKETS = new HashMap<>();
    private static final Map<UUID, UUID> REST_WALK_TICKETS = new HashMap<>();
    private static MinecraftServer activeServer;

    private OfflineGuardianService() { }

    public static List<TameData> selected(UUID owner) {
        return TameRegistry.getOwned(owner).stream().filter(data -> data.offlineGuardianOrder > 0)
                .sorted(Comparator.comparingLong((TameData data) -> data.offlineGuardianOrder)
                        .thenComparing(data -> String.valueOf(data.tlId))).toList();
    }

    /** Returns any oldest assignment displaced by a new sixth selection. */
    public static TameData set(TameData data, ServerLevel level, BlockPos post) {
        List<TameData> assigned = selected(data.ownerUUID);
        if (data.offlineGuardianOrder <= 0) {
            data.offlineGuardianOrder = assigned.stream().mapToLong(tame -> tame.offlineGuardianOrder).max().orElse(0L) + 1L;
        }
        data.offlineGuardianDimension = level.dimension().location().toString();
        data.offlineGuardianX = post.getX(); data.offlineGuardianY = post.getY(); data.offlineGuardianZ = post.getZ();
        deactivate(data);
        TameData evicted = null;
        List<TameData> updated = selected(data.ownerUUID);
        for (int i = 0; i < updated.size() - MAX_GUARDIANS; i++) {
            evicted = updated.get(i);
            removeAssignment(evicted);
        }
        TameRegistry.markDirty();
        return evicted;
    }

    public static boolean deploy(MinecraftServer server, TameData data) {
        if (data == null || data.offlineGuardianOrder <= 0 || data.stored || postLevel(server, data) == null
                || TameDuelManager.isTameInDuel(data.uuid)) return false;
        activeServer = server;
        data.offlineGuardianDeployed = true;
        data.offlineGuardianNeedsDeployment = true;
        remember(data);
        update(server, data);
        TameRegistry.markDirty();
        return true;
    }

    public static void deactivate(TameData data) {
        if (data == null) return;
        data.offlineGuardianDeployed = false;
        data.offlineGuardianNeedsDeployment = false;
        forget(data);
        TameRegistry.markDirty();
    }

    public static void removeAssignment(TameData data) {
        deactivate(data);
        data.offlineGuardianOrder = 0;
    }

    public static void remember(TameData data) {
        if (data == null || data.tlId == null) return;
        if (data.offlineGuardianOrder > 0 && data.offlineGuardianDeployed) DEPLOYED.add(data.tlId);
        else forget(data);
    }

    public static void forget(TameData data) {
        if (data == null || data.tlId == null) return;
        releaseRestWalk(data);
        DEPLOYED.remove(data.tlId);
        syncTickets(data.tlId, Set.of());
    }

    public static ServerLevel postLevel(MinecraftServer server, TameData data) {
        ResourceLocation id = ResourceLocation.tryParse(data.offlineGuardianDimension);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    public static BlockPos post(TameData data) {
        return new BlockPos(data.offlineGuardianX, data.offlineGuardianY, data.offlineGuardianZ);
    }

    private static UUID ticketId(UUID identity) {
        return UUID.nameUUIDFromBytes(("domesticationinnovation:offline_guardian:" + identity).getBytes(StandardCharsets.UTF_8));
    }

    private static void around(Set<ChunkTicket> desired, ServerLevel level, BlockPos pos) {
        if (level == null) return;
        ChunkPos chunk = new ChunkPos(pos);
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
            desired.add(new ChunkTicket(level.dimension(), chunk.x + x, chunk.z + z));
        }
    }

    private static void syncTickets(UUID identity, Set<ChunkTicket> desired) {
        Set<ChunkTicket> held = TICKETS.getOrDefault(identity, Set.of());
        if (activeServer != null) {
            for (ChunkTicket chunk : held) if (!desired.contains(chunk)) force(identity, chunk, false);
            for (ChunkTicket chunk : desired) if (!held.contains(chunk)) force(identity, chunk, true);
        }
        if (desired.isEmpty()) TICKETS.remove(identity);
        else TICKETS.put(identity, new HashSet<>(desired));
    }

    private static void force(UUID identity, ChunkTicket chunk, boolean load) {
        ServerLevel level = activeServer.getLevel(chunk.dimension());
        if (level != null) ForgeChunkManager.forceChunk(level, DomesticationMod.MODID, ticketId(identity), chunk.x(), chunk.z(), load, true);
    }

    private static void update(MinecraftServer server, TameData data) {
        // Homeward walkers use separate tickets so they never replace a deployed post's tickets.
        if (data == null || data.offlineGuardianOrder <= 0 || !data.offlineGuardianDeployed) return;
        if (data.stored) { deactivate(data); return; }
        if (TameDuelManager.isTameInDuel(data.uuid)) { syncTickets(data.tlId, Set.of()); return; }
        ServerLevel target = postLevel(server, data);
        if (target == null) { syncTickets(data.tlId, Set.of()); return; }
        Set<ChunkTicket> desired = new HashSet<>();
        around(desired, target, post(data));
        LivingEntity body = LoadedTameIndex.find(server, data.uuid, data.tlId);
        if (body != null && body.isAlive()) {
            around(desired, (ServerLevel) body.level(), body.blockPosition());
        } else if (!data.dead && data.lastKnownDimension != null) {
            ResourceLocation id = ResourceLocation.tryParse(data.lastKnownDimension);
            if (id != null) around(desired, server.getLevel(ResourceKey.create(Registries.DIMENSION, id)),
                    new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ));
        }
        if (data.dead && data.hasPetBed) {
            ResourceLocation id = ResourceLocation.tryParse(data.petBedDimension);
            if (id != null) around(desired, server.getLevel(ResourceKey.create(Registries.DIMENSION, id)),
                    new BlockPos(data.petBedX, data.petBedY, data.petBedZ));
        }
        syncTickets(data.tlId, desired);
        if (body == null || !body.isAlive() || data.dead) return;
        if (data.guardianResting) return;
        if (data.offlineGuardianNeedsDeployment) {
            TameCommands.deployOfflineGuardianToPost(server, data, body);
        } else if (!matchesPost(data)) {
            // Regular movement/guardian commands take precedence over an old deployment.
            deactivate(data);
        } else if (!data.hungerForcedSit && (data.movementOrder != 3
                || body.level() != target || body.distanceToSqr(data.offlineGuardianX + 0.5D,
                        data.offlineGuardianY, data.offlineGuardianZ + 0.5D) > 48.0D * 48.0D)) {
            TameCommands.deployOfflineGuardianToPost(server, data, body);
        }
    }

    public static boolean returnAfterRespawn(MinecraftServer server, TameData data) {
        if (!data.offlineGuardianDeployed || data.offlineGuardianOrder <= 0) return false;
        data.offlineGuardianNeedsDeployment = true;
        remember(data);
        update(server, data);
        return true;
    }

    private static boolean matchesPost(TameData data) {
        return data.hasHome && data.homeDimension.equals(data.offlineGuardianDimension)
                && data.homeX == data.offlineGuardianX && data.homeY == data.offlineGuardianY && data.homeZ == data.offlineGuardianZ;
    }

    /** Holds only a homeward walker's current 3x3 area, never its entire route. */
    public static void holdRestWalk(TameData data, LivingEntity body) {
        activeServer = body.getServer();
        UUID identity = REST_WALK_TICKETS.computeIfAbsent(data.ensureTlId(), id ->
                UUID.nameUUIDFromBytes(("guardian_rest_walk:" + id).getBytes(StandardCharsets.UTF_8)));
        Set<ChunkTicket> desired = new HashSet<>();
        around(desired, (ServerLevel) body.level(), body.blockPosition());
        syncTickets(identity, desired);
    }

    public static void releaseRestWalk(TameData data) {
        if (data == null || data.tlId == null) return;
        UUID identity = REST_WALK_TICKETS.remove(data.tlId);
        if (identity != null) syncTickets(identity, Set.of());
    }

    /** Restores active deployments after the generic Forge startup ticket cleanup. */
    public static void rebuild(MinecraftServer server) {
        for (UUID id : new ArrayList<>(TICKETS.keySet())) syncTickets(id, Set.of());
        DEPLOYED.clear(); TICKETS.clear(); REST_WALK_TICKETS.clear(); activeServer = server;
        for (TameData data : TameRegistry.TAMES.values()) remember(data);
        maintain(server);
    }

    public static void maintain(MinecraftServer server) {
        activeServer = server;
        for (UUID id : new ArrayList<>(REST_WALK_TICKETS.keySet())) {
            TameData data = TameRegistry.getByTlId(id);
            LivingEntity body = data == null ? null : LoadedTameIndex.find(server, data.uuid, data.tlId);
            if (data == null || data.dead || data.stored || !data.guardianResting || body == null || !body.isAlive()
                    || TameDuelManager.isTameInDuel(data.uuid)) {
                UUID identity = REST_WALK_TICKETS.remove(id);
                syncTickets(identity, Set.of());
            }
        }
        for (UUID id : new ArrayList<>(DEPLOYED)) {
            TameData data = TameRegistry.getByTlId(id);
            if (data == null || !data.offlineGuardianDeployed || data.offlineGuardianOrder <= 0) {
                DEPLOYED.remove(id); syncTickets(id, Set.of());
            } else update(server, data);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.getServer().getTickCount() % 20 == 0) maintain(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopping(ServerStoppingEvent event) {
        for (UUID id : new ArrayList<>(TICKETS.keySet())) syncTickets(id, Set.of());
        DEPLOYED.clear(); TICKETS.clear(); REST_WALK_TICKETS.clear(); activeServer = null;
    }
}
