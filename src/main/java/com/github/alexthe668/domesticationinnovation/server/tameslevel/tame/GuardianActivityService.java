package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
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
import java.util.*;

/** Nearby players activate ordinary guardians; guard posts never hold chunk tickets. */
public final class GuardianActivityService {
    public static final double ACTIVATION_RADIUS = 2.0D * TameGoalSupport.MONSTER_HUNTER_DETECTION_RADIUS;
    public static final double TEAM_ACTIVATION_RADIUS = 100.0D;
    private record ChunkTicket(ResourceKey<Level> dimension, int x, int z) { }
    private record Proximity(int nextTick, String dimension, int x, int y, int z, boolean nearby) { }
    private record PlayerGrid(ResourceKey<Level> dimension, Long2ObjectMap<List<ServerPlayer>> cells) { }
    private static final int CHECK_INTERVAL = 40;
    private static final int LOAD_RETRY_INTERVAL = 200;
    private static final int WAKE_TICKET_LIFETIME = 100;
    private static final long MAINTENANCE_BUDGET_NANOS = 500_000L;
    private static final TicketType<ChunkPos> WAKE_TICKET = TicketType.create("di_guardian_wake",
            Comparator.comparingLong(ChunkPos::toLong), WAKE_TICKET_LIFETIME);
    private static final Map<TameData, Proximity> PROXIMITY = new WeakHashMap<>();
    private static final Map<String, PlayerGrid> PLAYERS = new HashMap<>();
    private static final Map<TameData, Work> WORK = new IdentityHashMap<>();
    private static final Map<UUID, Set<ChunkTicket>> REST_WALK_TICKETS = new HashMap<>();
    private static final Map<UUID, ChunkTicket> REST_WALK_CENTERS = new HashMap<>();
    private static final Map<ChunkTicket, Integer> WAKE_TICKETS = new HashMap<>();
    private static GuardianWorkBudget budget = new GuardianWorkBudget();
    private static Work cursor;
    private static int nextPlayerSnapshotTick = Integer.MIN_VALUE;
    private static MinecraftServer activeServer;

    // Intrusive ring: registry updates/removals are O(1), without copying the
    // registry each second or accumulating stale entries in a lazy queue.
    private static final class Work {
        final TameData data;
        Work previous, next;
        int nextMaintenance, nextRestCheck, nextLoadAttempt, grantedTick = Integer.MIN_VALUE;
        boolean alternateLoad;
        Work(TameData data) { this.data = data; }
    }

    private GuardianActivityService() { }

    public static void enforceHunterMode(TameData data) {
        if (data != null && data.hasHome && !data.rosterBodyguard && data.mode != TameMode.MONSTER_HUNTER.id()) {
            data.mode = TameMode.MONSTER_HUNTER.id();
            TameRegistry.markDirty();
        }
    }

    private static void useServer(MinecraftServer server) {
        if (activeServer == server) return;
        PROXIMITY.clear(); PLAYERS.clear(); GuardianTeamPresence.clear(); nextPlayerSnapshotTick = Integer.MIN_VALUE;
        budget = new GuardianWorkBudget(); activeServer = server;
    }

    private static Work track(TameData data) {
        Work existing = WORK.get(data);
        if (existing != null) return existing;
        Work work = new Work(data);
        if (cursor == null) { work.next = work.previous = work; cursor = work; }
        else {
            work.previous = cursor.previous; work.next = cursor;
            cursor.previous.next = work; cursor.previous = work;
        }
        WORK.put(data, work);
        return work;
    }

    private static void snapshotPlayers(MinecraftServer server) {
        int now = server.getTickCount();
        if (now < nextPlayerSnapshotTick) return;
        nextPlayerSnapshotTick = now + 20;
        PLAYERS.clear();
        GuardianTeamPresence.snapshot(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            String dimension = player.level().dimension().location().toString();
            int x = Math.floorDiv(player.blockPosition().getX(), 32);
            int z = Math.floorDiv(player.blockPosition().getZ(), 32);
            PLAYERS.computeIfAbsent(dimension, ignored -> new PlayerGrid(player.level().dimension(), new Long2ObjectOpenHashMap<>())).cells
                    .computeIfAbsent(ChunkPos.asLong(x, z), ignored -> new ArrayList<>()).add(player);
        }
    }

    /** Cached spatial query; the larger team radius also expands the cell bounds. */
    public static boolean hasNearbyPlayer(MinecraftServer server, TameData data) {
        if (!data.hasHome) return false;
        useServer(server);
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.GuardianChestAutomation.keepsActive(data)) return true;
        int now = server.getTickCount();
        Proximity old = PROXIMITY.get(data);
        if (old != null && now < old.nextTick && Objects.equals(old.dimension, data.homeDimension)
                && old.x == data.homeX && old.y == data.homeY && old.z == data.homeZ) return old.nearby;
        snapshotPlayers(server);
        boolean nearby = false;
        double radius = GuardianTeamPresence.hasOnlineTeammate(data.ownerUUID) ? TEAM_ACTIVATION_RADIUS : ACTIVATION_RADIUS;
        PlayerGrid grid = PLAYERS.get(data.homeDimension);
        Long2ObjectMap<List<ServerPlayer>> cells = grid == null ? null : grid.cells;
        int centerX = Math.floorDiv(data.homeX, 32), centerZ = Math.floorDiv(data.homeZ, 32);
        int reach = (int)Math.ceil(radius / 32.0D);
        outer: for (int x = centerX - reach; cells != null && x <= centerX + reach; x++) for (int z = centerZ - reach; z <= centerZ + reach; z++) {
            List<ServerPlayer> players = cells.get(ChunkPos.asLong(x, z));
            if (players == null) continue;
            for (ServerPlayer player : players) {
                if (player.isAlive() && !player.isRemoved() && !player.isSpectator()
                        && player.level().dimension().equals(grid.dimension)
                        && player.distanceToSqr(data.homeX + 0.5D, data.homeY, data.homeZ + 0.5D)
                        <= radius * radius) { nearby = true; break outer; }
            }
        }
        PROXIMITY.put(data, new Proximity(now + CHECK_INTERVAL, data.homeDimension, data.homeX, data.homeY, data.homeZ, nearby));
        return nearby;
    }

    /** Loaded entity callbacks and the queue share the same per-guardian gate. */
    public static boolean processRestThrottled(LivingEntity body, TameData data) {
        if (!data.hasHome || (data.movementOrder != 3 && !data.guardianResting && !data.hungerForcedSit)) return false;
        MinecraftServer server = body.getServer();
        if (server == null) return false;
        useServer(server);
        Work work = track(data);
        int now = server.getTickCount();
        if (now < work.nextRestCheck) return data.guardianResting;
        enforceHunterMode(data);
        work.nextRestCheck = now + CHECK_INTERVAL + Math.floorMod(data.uuid.hashCode(), 10);
        if (TameCommands.processGuardianMorningReturn(body, data)) return true;
        boolean resting = TameCommands.processGuardianRest(body, data);
        if (!resting) TameCommands.enforceGuardianCommandLock(body, data);
        return resting;
    }

    /** At most two different guardians may start a rest/teleport transition per tick. */
    public static boolean tryTransition(MinecraftServer server, TameData data) {
        useServer(server);
        Work work = track(data);
        int now = server.getTickCount();
        if (work.grantedTick == now) return true;
        if (!budget.tryTransition(now)) return false;
        work.grantedTick = now;
        return true;
    }

    private static boolean requestChunk(ServerLevel level, BlockPos pos) {
        useServer(level.getServer());
        ChunkPos chunk = new ChunkPos(pos);
        if (level.getChunkSource().getChunkNow(chunk.x, chunk.z) != null) return true;
        ChunkTicket key = new ChunkTicket(level.dimension(), chunk.x, chunk.z);
        if (WAKE_TICKETS.containsKey(key)) return false;
        if (WAKE_TICKETS.size() >= GuardianWorkBudget.MAX_PENDING_CHUNK_REQUESTS) return false;
        int now = level.getServer().getTickCount();
        if (!budget.tryChunkRequest(now)) return false;
        // Region tickets schedule loading asynchronously. Unlike getChunkAt or
        // getChunkFuture on the server thread, this does not wait for disk IO.
        level.getChunkSource().addRegionTicket(WAKE_TICKET, chunk, 0, chunk);
        WAKE_TICKETS.put(key, now + WAKE_TICKET_LIFETIME);
        return false;
    }

    public static boolean prepareDestination(ServerLevel level, Vec3 pos) {
        return level != null && pos != null && requestChunk(level, BlockPos.containing(pos));
    }

    private static void requestBody(MinecraftServer server, Work work) {
        int now = server.getTickCount();
        if (now < work.nextLoadAttempt) return;
        TameData data = work.data;
        boolean cached = work.alternateLoad || data.lastKnownDimension == null || data.lastKnownDimension.isEmpty();
        ResourceLocation id = ResourceLocation.tryParse(cached ? data.guardianRestHome.getString("dimension") : data.lastKnownDimension);
        ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) return;
        BlockPos pos = cached ? BlockPos.containing(data.guardianRestHome.getDouble("x"), data.guardianRestHome.getDouble("y"), data.guardianRestHome.getDouble("z"))
                : new BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ);
        ChunkTicket key = new ChunkTicket(level.dimension(), pos.getX() >> 4, pos.getZ() >> 4);
        boolean pending = WAKE_TICKETS.containsKey(key);
        boolean loaded = requestChunk(level, pos);
        if (loaded || pending || WAKE_TICKETS.containsKey(key)) {
            work.nextLoadAttempt = now + LOAD_RETRY_INTERVAL;
            work.alternateLoad = !work.alternateLoad;
        }
    }

    /** Owner login queues work; it no longer synchronously loads every sleeping tame. */
    public static void requestOwnerWake(ServerPlayer owner) {
        useServer(owner.getServer());
        for (TameData data : TameRegistry.getOwned(owner.getUUID())) if (data.guardianResting && !data.dead && !data.stored) {
            Work work = track(data);
            work.nextMaintenance = work.nextRestCheck = work.nextLoadAttempt = 0;
        }
    }

    /** Retire old deployment flags; already deployed posts become regular guardian posts. */
    public static void remember(TameData data) {
        if (data == null) return;
        if (data.offlineGuardianOrder > 0 || data.offlineGuardianDeployed || data.offlineGuardianNeedsDeployment) {
            if (data.offlineGuardianDeployed && !data.hasHome) {
                data.hasHome = true;
                data.homeDimension = data.offlineGuardianDimension;
                data.homeX = data.offlineGuardianX; data.homeY = data.offlineGuardianY; data.homeZ = data.offlineGuardianZ;
                data.movementOrder = 3;
            }
            data.offlineGuardianOrder = 0;
            data.offlineGuardianDeployed = false;
            data.offlineGuardianNeedsDeployment = false;
            TameRegistry.markDirty();
        }
        if (data.hasHome) track(data);
        else forget(data);
    }

    public static void forget(TameData data) {
        if (data == null) return;
        PROXIMITY.remove(data);
        Work work = WORK.remove(data);
        if (work != null) {
            if (work.next == work) cursor = null;
            else {
                work.previous.next = work.next; work.next.previous = work.previous;
                if (cursor == work) cursor = work.next;
            }
        }
        releaseRestWalk(data);
    }

    public static void removeAssignment(TameData data) {
        if (data == null) return;
        data.offlineGuardianOrder = 0;
        data.offlineGuardianDeployed = false;
        data.offlineGuardianNeedsDeployment = false;
        forget(data);
        if (data.hasHome) track(data);
    }

    private static UUID ticketId(UUID identity) {
        return UUID.nameUUIDFromBytes(("guardian_rest_walk:" + identity).getBytes(StandardCharsets.UTF_8));
    }

    private static void syncTickets(UUID identity, Set<ChunkTicket> desired) {
        Set<ChunkTicket> held = REST_WALK_TICKETS.getOrDefault(identity, Set.of());
        if (activeServer != null) {
            for (ChunkTicket chunk : held) if (!desired.contains(chunk)) force(identity, chunk, false);
            for (ChunkTicket chunk : desired) if (!held.contains(chunk)) force(identity, chunk, true);
        }
        if (desired.isEmpty()) REST_WALK_TICKETS.remove(identity);
        else REST_WALK_TICKETS.put(identity, desired);
    }

    private static void force(UUID identity, ChunkTicket chunk, boolean load) {
        ServerLevel level = activeServer.getLevel(chunk.dimension);
        if (level != null) ForgeChunkManager.forceChunk(level, DomesticationMod.MODID, ticketId(identity), chunk.x, chunk.z, load, true);
    }

    /** Keep the existing bounded tickets only while a hungry guardian walks home. */
    public static void holdRestWalk(TameData data, LivingEntity body) {
        activeServer = body.getServer();
        ChunkPos chunk = body.chunkPosition();
        ChunkTicket center = REST_WALK_CENTERS.get(data.ensureTlId());
        if (center != null && center.dimension.equals(body.level().dimension()) && center.x == chunk.x && center.z == chunk.z) return;
        // Excess hungry guardians can walk in normally ticking chunks; the
        // bounded maintenance queue still handles their eventual home fallback.
        if (center == null && REST_WALK_CENTERS.size() >= GuardianWorkBudget.MAX_REST_WALK_GUARDIANS) return;
        Set<ChunkTicket> desired = new HashSet<>();
        REST_WALK_CENTERS.put(data.tlId, new ChunkTicket(body.level().dimension(), chunk.x, chunk.z));
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            desired.add(new ChunkTicket(body.level().dimension(), chunk.x + x, chunk.z + z));
        syncTickets(data.ensureTlId(), desired);
    }

    public static void releaseRestWalk(TameData data) {
        if (data != null && data.tlId != null) {
            REST_WALK_CENTERS.remove(data.tlId);
            syncTickets(data.tlId, Set.of());
        }
    }

    private static void expireWakeTickets(MinecraftServer server, boolean all) {
        int now = server.getTickCount();
        var iterator = WAKE_TICKETS.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!all && now < entry.getValue()) continue;
            ChunkTicket key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension);
            if (level != null) {
                ChunkPos chunk = new ChunkPos(key.x, key.z);
                level.getChunkSource().removeRegionTicket(WAKE_TICKET, chunk, 0, chunk);
            }
            iterator.remove();
        }
    }

    public static void rebuild(MinecraftServer server) {
        for (UUID id : new ArrayList<>(REST_WALK_TICKETS.keySet())) syncTickets(id, Set.of());
        expireWakeTickets(server, true);
        REST_WALK_TICKETS.clear(); REST_WALK_CENTERS.clear(); PROXIMITY.clear(); PLAYERS.clear(); WORK.clear(); cursor = null;
        GuardianTeamPresence.clear();
        activeServer = server; nextPlayerSnapshotTick = Integer.MIN_VALUE; budget = new GuardianWorkBudget();
        for (TameData data : TameRegistry.TAMES.values()) remember(data);
    }

    public static void maintain(MinecraftServer server) {
        useServer(server);
        expireWakeTickets(server, false);
        int now = server.getTickCount();
        long day = Math.floorDiv(server.overworld().getDayTime(), 24000L);
        long deadline = System.nanoTime() + MAINTENANCE_BUDGET_NANOS;
        int count = Math.min(WORK.size(), GuardianWorkBudget.MAX_RECORDS_PER_TICK);
        for (int i = 0; i < count && cursor != null && budget.tryInspect(now); i++) {
            Work work = cursor;
            cursor = work.next;
            TameData data = work.data;
            if (TameRegistry.get(data.uuid) != data || !data.hasHome) { forget(data); continue; }
            if (now < work.nextMaintenance) continue;
            work.nextMaintenance = now + CHECK_INTERVAL;
            enforceHunterMode(data);
            boolean guardian = !data.rosterBodyguard && (data.movementOrder == 3 || data.guardianResting);
            if (guardian && (data.guardianMorningReturnDay == Long.MIN_VALUE || day < data.guardianMorningReturnDay)) {
                data.guardianMorningReturnDay = day;
                TameRegistry.markDirty();
            }
            if (guardian && !data.guardianResting && data.guardianMorningReturnDay < day
                    && !data.dead && !data.stored && !TameDuelManager.isTameInDuel(data.uuid)) {
                // An unloaded guardian retains its pending day; never load its source chunk for a morning return.
                LivingEntity body = LoadedTameIndex.find(server, data.uuid, data.tlId);
                if (body != null) processRestThrottled(body, data);
            }
            if (data.tlId != null && REST_WALK_TICKETS.containsKey(data.tlId)) {
                LivingEntity walker = LoadedTameIndex.find(server, data.uuid, data.tlId);
                if (data.dead || data.stored || !data.guardianResting || walker == null || !walker.isAlive()
                        || TameDuelManager.isTameInDuel(data.uuid)) releaseRestWalk(data);
            }
            if (data.guardianResting && data.hasHome && !data.dead && !data.stored && !TameDuelManager.isTameInDuel(data.uuid)
                    && ((data.ownerUUID != null && server.getPlayerList().getPlayer(data.ownerUUID) != null)
                    || (!server.getPlayerList().getPlayers().isEmpty() && hasNearbyPlayer(server, data)))) {
                LivingEntity body = LoadedTameIndex.find(server, data.uuid, data.tlId);
                if (body == null) requestBody(server, work);
                else processRestThrottled(body, data);
            }
            if (System.nanoTime() >= deadline) break;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void started(ServerStartedEvent event) { rebuild(event.getServer()); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) maintain(event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void stopping(ServerStoppingEvent event) {
        for (UUID id : new ArrayList<>(REST_WALK_TICKETS.keySet())) syncTickets(id, Set.of());
        expireWakeTickets(event.getServer(), true);
        REST_WALK_TICKETS.clear(); REST_WALK_CENTERS.clear(); PROXIMITY.clear(); PLAYERS.clear(); WORK.clear(); cursor = null;
        GuardianTeamPresence.clear();
        activeServer = null; nextPlayerSnapshotTick = Integer.MIN_VALUE; budget = new GuardianWorkBudget();
    }
}
