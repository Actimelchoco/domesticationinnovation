package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Online-time accounting never loads chunks. Wild ability records live on the entity, not in the tame registry. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class TameLimitService {
    public static final int MAX_PER_TYPE = 10;
    public static final int GRACE_TICKS = 10 * 60 * 20;
    public static final String WILD = "TLReleasedHunter";
    private static final String RECORD = "TLReleasedHunterData";
    private static final Map<Entity, TameData> CACHE = new WeakHashMap<>();
    private static long lastRegistrationOrder;
    private TameLimitService() {}

    public static long nextRegistrationOrder() {
        return lastRegistrationOrder = Math.max(lastRegistrationOrder + 1, System.currentTimeMillis());
    }

    public static boolean isReleased(Entity entity) {
        return entity != null && entity.getPersistentData().getBoolean(WILD);
    }

    public static TameData wildData(Entity entity) {
        if (!isReleased(entity)) return null;
        return CACHE.computeIfAbsent(entity, key -> {
            TameData data = TameData.fromTag(key.getPersistentData().getCompound(RECORD));
            data.ownerUUID = null;
            data.mode = TameMode.MONSTER_HUNTER.id();
            data.dead = false;
            data.stored = false;
            return data;
        });
    }

    public static TameData abilityData(Entity entity) {
        if (entity == null) return null;
        TameData referee = RankedRefereeService.data(entity);
        if (referee != null) return referee;
        if (isReleased(entity)) return wildData(entity);
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat.isTemporary(entity))
            return com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat.dataFor(entity);
        return TameRegistry.get(entity.getUUID());
    }

    public static boolean countsTowardLimit(TameData data) {
        return data != null && data.level >= 10 && (!data.abilities.isEmpty()
                || data.abilityLevels.values().stream().anyMatch(level -> level != null && level > 0)
                || data.attributeLevels.values().stream().anyMatch(level -> level != null && level > 0));
    }

    /** Returns newest qualifying excess entries; older records use stable UUID ordering. */
    public static List<TameData> excess(List<TameData> records) {
        Map<String, List<TameData>> types = new HashMap<>();
        for (TameData data : records) if (countsTowardLimit(data) && data.ownerUUID != null)
            types.computeIfAbsent(data.type, ignored -> new ArrayList<>()).add(data);
        List<TameData> result = new ArrayList<>();
        for (List<TameData> type : types.values()) {
            type.sort(Comparator.comparingLong((TameData data) -> data.limitRegisteredAt)
                    .thenComparing(data -> data.ensureTlId().toString()));
            if (type.size() > MAX_PER_TYPE) result.addAll(type.subList(MAX_PER_TYPE, type.size()));
        }
        return result;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0 || !TameRegistry.isInitialized()) return;
        advanceOnlinePlayers(server);
    }

    public static void advanceOnlinePlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) advanceOnlineOwner(server, player);
        pending(server).materializeStored(server);
    }

    public static void advanceOnlineOwner(MinecraftServer server, ServerPlayer owner) {
        List<TameData> owned = TameRegistry.getOwned(owner.getUUID());
        Set<TameData> extras = new HashSet<>(excess(owned));
        int releases = 0;
        int warnings = 0;
        List<String> examples = new ArrayList<>();
        for (TameData data : owned) {
            if (!extras.contains(data)) {
                if (data.limitOnlineTicks != 0 || data.limitWarned) {
                    data.limitOnlineTicks = 0;
                    data.limitWarned = false;
                    TameRegistry.markEntryDataDirty();
                }
                continue;
            }
            if (!data.limitWarned) {
                warnings++;
                if (examples.size() < 3) examples.add(data.name + " (" + data.type + ")");
                data.limitWarned = true;
            }
            data.limitOnlineTicks = Math.min(GRACE_TICKS, data.limitOnlineTicks + 20);
            TameRegistry.markEntryDataDirty();
            // Bound expensive cleanup and notifications when migrating a very large registry.
            if (data.limitOnlineTicks >= GRACE_TICKS && releases < 4) {
                release(server, data);
                releases++;
                owner.sendSystemMessage(Component.literal(data.name + " exceeded the per-type tame limit and is now wild.")
                        .withStyle(ChatFormatting.RED));
            }
        }
        if (warnings > 0) owner.sendSystemMessage(Component.literal(warnings + " trained creature(s) exceed the limit of 10 per type (level 10+ with an ability or attribute upgrade): "
                + String.join(", ", examples) + (warnings > examples.size() ? ", ..." : "")
                + ". Make room within 10 minutes of online time or they become wild monster hunters.")
                .withStyle(ChatFormatting.GOLD));
    }

    public static void release(MinecraftServer server, TameData data) {
        TameDuelManager.endDuelForEntity(server, data.uuid);
        TameData restored = TameRegistry.get(data.uuid);
        if (restored != null) data = restored;
        LivingEntity live = TameEntityAdapter.findLoaded(server, data.uuid, data.tlId);
        UUID archiveId = LostTameRegistry.get(server).archive(data, live);
        LostTameRegistry.get(server).stampGameTime(archiveId, server.overworld().getGameTime());
        Pending pending = pending(server);
        // Persist the tombstone before dropping ownership; unloaded chunks must never restore an old owner.
        CompoundTag row = data.toTag();
        row.putUUID("lostArchiveId", archiveId);
        row.putLong("lostGameTime", server.overworld().getGameTime());
        row.remove("ownerUUID");
        row.putInt("mode", TameMode.MONSTER_HUNTER.id());
        row.putInt("movementOrder", 2);
        row.putBoolean("guardianResting", false);
        // A transformed wolf exists only as a snapshot plus a temporary hostile body.
        // Queue the original wolf without forcing the temporary body's chunk to load.
        CompoundTag transformation = data.bewereagerState;
        if (transformation.getBoolean("Active") && transformation.contains("Wolf", 10)) {
            row.put("entitySnapshot", transformation.getCompound("Wolf").copy());
            row.putBoolean("stored", true);
            row.putBoolean("dead", false);
            row.putString("lastKnownDimension", transformation.getString("Dimension"));
            row.putInt("lastKnownX", net.minecraft.util.Mth.floor(transformation.getDouble("X")));
            row.putInt("lastKnownY", net.minecraft.util.Mth.floor(transformation.getDouble("Y")));
            row.putInt("lastKnownZ", net.minecraft.util.Mth.floor(transformation.getDouble("Z")));
            row.remove("bewereagerState");
            if (transformation.hasUUID("Body")) for (ServerLevel level : server.getAllLevels()) {
                Entity body = level.getEntity(transformation.getUUID("Body"));
                if (body != null) body.discard();
            }
            live = null;
        }
        pending.put(data.uuid, row);
        pending.setDirty();
        var ownerStats = TameRegistry.getPlayerDuelStats().get(data.ownerUUID);
        if (ownerStats != null) ownerStats.bodyguards.remove(data.tlId);
        TameRegistry.remove(data.uuid);
        var world = com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData.get(server.overworld());
        if (world != null) {
            Set<UUID> identities = new HashSet<>();
            identities.add(data.uuid);
            if (data.tlId != null) identities.add(data.tlId);
            world.removeRespawnRequestsForPets(identities);
            world.removeLanternRequestsForPets(identities);
        }
        if (live != null) applyPending(live);
    }

    /** Also called at registration boundaries to cover mods that assign ownership after joining the level. */
    public static boolean applyPending(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;
        if (!TameEntityAdapter.isSupported(entity) && !isReleased(entity)) return false;
        if (level.getServer().overworld() == null) return false;
        Pending pending = pending(level.getServer());
        CompoundTag row = pending.rows.get(entity.getUUID());
        UUID tlId = TameData.getTlId(entity);
        if (row == null && tlId != null) {
            UUID canonical = pending.byTlId(tlId);
            if (canonical != null) row = pending.rows.get(canonical);
        }
        if (row != null && row.hasUUID("recoveredBody")) {
            if (!row.getUUID("recoveredBody").equals(entity.getUUID())) {
                if (isReleased(entity) || row.hasUUID("retiredBody") && row.getUUID("retiredBody").equals(entity.getUUID())) {
                    entity.discard();
                    return true;
                }
                // Normal respawn/dimension-recovery paths may assign a new UUID.
                // Their owned, non-wild snapshots remain subject to the normal registry identity checks.
                return false;
            }
            entity.getPersistentData().remove(WILD);
            entity.getPersistentData().remove(RECORD);
            CACHE.remove(entity);
            return false;
        }
        if (row != null && !isReleased(entity)) {
            entity.getPersistentData().putBoolean(WILD, true);
            entity.getPersistentData().put(RECORD, row.copy());
            CACHE.remove(entity);
            // Keep the small tombstone until death: another saved copy/respawn must also lose its owner.
            row.putBoolean("materialized", true);
            pending.setDirty();
        }
        if (row != null && !row.getBoolean("materialized")) {
            row.putBoolean("materialized", true);
            pending.setDirty();
        }
        if (!isReleased(entity)) return false;
        wildData(entity).rememberImprintedOwner(entity, null);
        TameRegistry.remove(entity.getUUID());
        TameData stale = tlId == null ? null : TameRegistry.getByTlId(tlId);
        if (stale != null) TameRegistry.remove(stale.uuid);
        clearOwner(entity);
        if (entity instanceof Mob mob) {
            if (mob.getTarget() != null && !hunterTarget(mob.getTarget())) mob.setTarget(null);
            mob.getNavigation().stop();
            if (mob.targetSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof WildHunterGoal))
                mob.targetSelector.addGoal(-1, new WildHunterGoal(mob));
        }
        return true;
    }

    private static void clearOwner(LivingEntity entity) {
        if (!TameEntityAdapter.isTame(entity) && TameEntityAdapter.ownerUuid(entity) == null) return;
        var attributes = entity.getAttributes().save();
        float health = entity.getHealth();
        if (entity instanceof TamableAnimal tame) {
            tame.setOrderedToSit(false);
            tame.setInSittingPose(false);
            tame.setOwnerUUID(null);
            tame.setTame(false);
        } else if (entity instanceof ModifedToBeTameable modified) {
            modified.setTameOwnerUUID(null);
            modified.setTame(false);
        }
        if (entity instanceof com.github.alexthe666.citadel.server.entity.IComandableMob commandable)
            commandable.setCommand(0);
        // Some vanilla/modded setTame implementations reset base health or damage.
        entity.getAttributes().load(attributes);
        entity.setHealth(health);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void join(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && !event.getLevel().isClientSide) {
            applyPending(living);
            if (living.isRemoved()) event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tame(AnimalTameEvent event) {
        if (isReleased(event.getAnimal())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void target(LivingChangeTargetEvent event) {
        if (isReleased(event.getEntity()) && event.getNewTarget() != null && !hunterTarget(event.getNewTarget()))
            event.setNewTarget(null);
    }

    @SubscribeEvent
    public static void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!isReleased(entity) || entity.level().isClientSide || (entity.tickCount + entity.getId()) % 100 != 0) return;
        clearOwner(entity);
        LevelSystem.syncLevelArmor(entity, wildData(entity));
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        if (event.getEntity() instanceof LivingEntity living) LostHunterActivity.forget(living);
        TameData data = CACHE.remove(event.getEntity());
        if (data != null) event.getEntity().getPersistentData().put(RECORD, data.toTag());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (!event.isCanceled() && isReleased(event.getEntity()) && event.getEntity().level() instanceof ServerLevel level) {
            Pending pending = pending(level.getServer());
            pending.remove(event.getEntity().getUUID());
            pending.setDirty();
        }
    }

    private static boolean hunterTarget(LivingEntity target) {
        return target instanceof Enemy && target.isAlive() && !TameEntityAdapter.isTame(target) && !isReleased(target);
    }

    public record LostRecovery(LivingEntity entity, String error) {}

    /** Event-driven, in-place reclaim: no entity searches, chunk loading or periodic timers. */
    public static String retameLost(ServerPlayer player, LivingEntity body) {
        return retameLost(player, body, net.minecraft.world.InteractionHand.MAIN_HAND);
    }
    public static String retameLost(ServerPlayer player, LivingEntity body, net.minecraft.world.InteractionHand hand) {
        if (!isReleased(body) || !body.isAlive()) return "This creature is not a living lost tame.";
        MinecraftServer server = player.getServer();
        Pending pending = pending(server);
        UUID key = pending.byTlId(TameData.getTlId(body));
        if (key == null) key = body.getUUID();
        CompoundTag row = pending.rows.get(key);
        if (row == null) return "This lost tame has no recovery record; ask an admin.";
        long now = server.overworld().getGameTime();
        // Older worlds begin the public cooldown on their first reclaim attempt.
        if (!row.contains("lostGameTime")) { row.putLong("lostGameTime", now);pending.setDirty(); }
        TameData data = TameData.fromTag(wildData(body).toTag());
        data.rememberImprintedOwner(body, null);
        long remaining = 72000L - (now - row.getLong("lostGameTime"));
        if (!player.getUUID().equals(data.imprintedOwnerUUID()) && remaining > 0)
            return "Only the imprinted owner can reclaim this tame yet. Public retaming in " + ((remaining + 1199) / 1200) + " minute(s) at 20 TPS.";
        if (!TameFoodManager.accepts(player.getItemInHand(hand), data, body))
            return "Give this creature a food it accepts to retame it.";
        if (TameRegistry.get(body.getUUID()) != null || TameRegistry.getByTlId(data.tlId) != null)
            return "This tame is already registered; ask an admin.";
        if (TameRegistry.isTameTypeDisabled(body) || TameRegistry.isSummonType(body)) return "This tame type is disabled.";
        data.uuid = body.getUUID();data.ownerUUID = player.getUUID();data.dead = false;data.stored = false;
        data.mode = TameMode.DEFAULT.id();data.movementOrder = 1;data.hasHome = false;data.rosterBodyguard = false;
        data.guardianResting = false;data.guardianReleaseMovementPending = -1;data.hungerForcedSit = false;
        data.limitOnlineTicks = 0;data.limitWarned = false;data.limitRegisteredAt = nextRegistrationOrder();
        body.getPersistentData().remove(WILD);body.getPersistentData().remove(RECORD);CACHE.remove(body);
        var attributes = body.getAttributes().save();float health = body.getHealth();
        TameEntityAdapter.setOwner(body, player.getUUID());body.getAttributes().load(attributes);body.setHealth(health);
        if (body instanceof Mob mob) { mob.setTarget(null);mob.getNavigation().stop(); }
        if (body instanceof TamableAnimal animal) { animal.setOrderedToSit(true);animal.setInSittingPose(true); }
        if (body instanceof com.github.alexthe666.citadel.server.entity.IComandableMob commandable) commandable.setCommand(1);
        data.lastKnownDimension = body.level().dimension().location().toString();
        data.lastKnownX = body.blockPosition().getX();data.lastKnownY = body.blockPosition().getY();data.lastKnownZ = body.blockPosition().getZ();
        CompoundTag snapshot = new CompoundTag();body.save(snapshot);data.entitySnapshot = snapshot;
        row.putUUID("recoveredBody", body.getUUID());row.remove("retiredBody");row.putBoolean("materialized", true);row.putBoolean("stored", false);
        pending.setDirty();
        TameRegistry.register(data);TameRegistry.bindEntityToData(body, data);
        if (body instanceof TamableAnimal animal)
            com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller.installIfMissing(animal);
        var archive = LostTameRegistry.get(server);
        if (row.hasUUID("lostArchiveId")) archive.recovered(row.getUUID("lostArchiveId"), body.getUUID());
        else { // Compatibility with v49 releases which did not link the pending row to its archive.
            for (UUID id : archive.ids(true)) {
                if (data.tlId.equals(TameData.fromTag(archive.entry(id).getCompound("tame")).tlId)) {
                    archive.recovered(id, body.getUUID());break;
                }
            }
        }
        if (!player.getAbilities().instabuild && !TameFoodManager.isEternalSteak(player.getItemInHand(hand))) {
            var food = player.getItemInHand(hand);var container = food.getCraftingRemainingItem();food.shrink(1);
            if (!container.isEmpty() && !player.getInventory().add(container)) player.drop(container, false);
        }
        return null;
    }

    /** Explicit admin recovery may load the destination chunk; limit enforcement never does. */
    public static LostRecovery recoverLost(MinecraftServer server, UUID archiveId, ServerLevel destination,
                                          net.minecraft.world.phys.Vec3 position) {
        LostTameRegistry archive = LostTameRegistry.get(server);
        CompoundTag entry = archive.entry(archiveId);
        if (entry == null) return new LostRecovery(null, "Lost tame entry not found.");
        if (entry.contains("recoveredAt")) return new LostRecovery(null, "That entry was already recovered.");
        TameData data = TameData.fromTag(entry.getCompound("tame"));
        if (data.ownerUUID == null || data.entitySnapshot.isEmpty()) return new LostRecovery(null, "Saved owner or entity snapshot is missing.");
        if (TameRegistry.get(data.uuid) != null || TameRegistry.getByTlId(data.tlId) != null)
            return new LostRecovery(null, "That tame is already registered. Recovery was not duplicated.");
        CompoundTag snapshot = data.entitySnapshot.copy();
        snapshot.remove("Passengers");
        Entity created = EntityType.loadEntityRecursive(snapshot, destination, entity -> entity);
        if (!(created instanceof LivingEntity body) || !TameEntityAdapter.isSupported(body))
            return new LostRecovery(null, "Saved entity type is unavailable or unsupported.");
        if (TameRegistry.isSummonType(body) || TameRegistry.isTameTypeDisabled(body))
            return new LostRecovery(null, "That tame type is disabled in the current server settings.");
        UUID originalId = data.uuid;
        LivingEntity oldBody = TameEntityAdapter.findLoaded(server, data.uuid, data.tlId);
        // Released wild bodies are deliberately excluded from the owned-tame index.
        if (oldBody == null) {
            for (ServerLevel level : server.getAllLevels()) {
                if (level.getEntity(originalId) instanceof LivingEntity living) {
                    oldBody = living;
                    break;
                }
            }
        }
        Pending pending = pending(server);
        UUID previousKey = pending.byTlId(data.tlId);
        if (previousKey == null) previousKey = originalId;
        CompoundTag previous = pending.rows.get(previousKey);
        previous = previous == null ? null : previous.copy();
        UUID newId = UUID.randomUUID();
        body.setUUID(newId);
        data.uuid = newId;
        data.dead = false; data.stored = false;
        data.bewereagerState = new CompoundTag();
        data.rosterBodyguard = false; data.hasHome = false; data.guardianResting = false;
        data.guardianReleaseMovementPending = -1; data.hungerForcedSit = false;
        data.mode = TameMode.DEFAULT.id(); data.movementOrder = 1;
        data.limitOnlineTicks = 0; data.limitWarned = false; data.limitRegisteredAt = nextRegistrationOrder();
        body.getPersistentData().remove(WILD);body.getPersistentData().remove(RECORD);
        TameData.syncTlIdToEntity(body, data.ensureTlId());
        data.rememberImprintedOwner(body, data.ownerUUID);
        var attributes = body.getAttributes().save();
        TameEntityAdapter.setOwner(body, data.ownerUUID);
        body.getAttributes().load(attributes);
        body.setHealth(body.getMaxHealth());body.deathTime = 0;body.hurtTime = 0;
        body.moveTo(position.x, position.y, position.z, 0, 0);
        if (body instanceof TamableAnimal animal) { animal.setOrderedToSit(true);animal.setInSittingPose(true); }
        if (body instanceof com.github.alexthe666.citadel.server.entity.IComandableMob commandable) commandable.setCommand(1);
        CompoundTag directive = previous == null ? entry.getCompound("tame").copy() : previous.copy();
        directive.putUUID("recoveredBody", newId);directive.putUUID("recoveredOwner", data.ownerUUID);
        directive.putUUID("retiredBody", originalId);
        directive.putBoolean("materialized", true);directive.putBoolean("stored", false);
        pending.put(originalId, directive);pending.setDirty();
        boolean committed = false;
        com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents.beginTameReconstruction();
        try {
            destination.getChunk(body.blockPosition());
            if (!destination.addFreshEntity(body)) return new LostRecovery(null, "Entity spawning was rejected; the archive is unchanged.");
            data.lastKnownDimension = destination.dimension().location().toString();
            data.lastKnownX = body.blockPosition().getX();data.lastKnownY = body.blockPosition().getY();data.lastKnownZ = body.blockPosition().getZ();
            data.lastKnownGameTime = destination.getGameTime();
            CompoundTag saved = new CompoundTag();body.save(saved);data.entitySnapshot = saved;
            TameRegistry.register(data);
            if (TameRegistry.get(newId) != data) return new LostRecovery(null, "Registry rejected the saved tame; the archive is unchanged.");
            TameRegistry.bindEntityToData(body, data);
            if (body instanceof TamableAnimal animal)
                com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller.installIfMissing(animal);
            if (oldBody != null && oldBody != body) oldBody.discard();
            var ownerStats = TameRegistry.getPlayerDuelStats().get(data.ownerUUID);
            if (ownerStats != null) ownerStats.bodyguards.remove(data.tlId);
            archive.recovered(archiveId, newId);
            committed = true;
            return new LostRecovery(body, "");
        } finally {
            com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents.endTameReconstruction();
            if (!committed) {
                body.discard();TameRegistry.remove(newId);
                pending.remove(originalId);
                if (previous != null) pending.put(previousKey, previous);
                pending.setDirty();
            }
        }
    }

    private static final class WildHunterGoal extends Goal {
        private final Mob mob;
        WildHunterGoal(Mob mob) { this.mob = mob; setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() { return isReleased(mob); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (!LostHunterActivity.active(mob)) return;
            LivingEntity current = mob.getTarget();
            if (current != null && hunterTarget(current) && !current.isRemoved()) return;
            if (current != null) mob.setTarget(null);
            if (Math.floorMod(mob.level().getGameTime() + mob.getUUID().hashCode(), LostHunterActivity.IDLE_SEARCH_TICKS) != 0) return;
            LivingEntity closest = null;
            double distance = Double.MAX_VALUE;
            for (LivingEntity candidate : mob.level().getEntitiesOfClass(LivingEntity.class,
                    mob.getBoundingBox().inflate(10), TameLimitService::hunterTarget)) {
                double d = mob.distanceToSqr(candidate);
                if (d < distance) { distance = d; closest = candidate; }
            }
            if (closest != null) {
                mob.setTarget(closest);
                mob.setLastHurtByMob(closest);
            }
        }
    }

    private static Pending pending(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Pending::load, Pending::new, "tameslevel_wild_releases");
    }

    public static final class Pending extends SavedData {
        final Map<UUID, CompoundTag> rows = new HashMap<>();
        private final Map<UUID, UUID> stableIds = new HashMap<>();
        private final ArrayDeque<UUID> stored = new ArrayDeque<>();
        UUID byTlId(UUID id) { return stableIds.get(id); }
        void put(UUID id, CompoundTag row) {
            if (row.hasUUID(TameData.TL_ID_TAG)) {
                UUID previous = stableIds.get(row.getUUID(TameData.TL_ID_TAG));
                if (previous != null && !previous.equals(id)) rows.remove(previous);
            }
            rows.put(id, row);
            if (row.hasUUID(TameData.TL_ID_TAG)) stableIds.put(row.getUUID(TameData.TL_ID_TAG), id);
            if (row.getBoolean("stored") && !row.getBoolean("dead") && !row.getBoolean("materialized")) stored.add(id);
        }
        void remove(UUID id) {
            CompoundTag row = rows.remove(id);
            if (row != null && row.hasUUID(TameData.TL_ID_TAG)) stableIds.remove(row.getUUID(TameData.TL_ID_TAG));
        }
        public static Pending load(CompoundTag tag) {
            Pending data = new Pending();
            for (String key : tag.getAllKeys()) try { data.put(UUID.fromString(key), tag.getCompound(key)); }
            catch (IllegalArgumentException ignored) {}
            return data;
        }
        @Override public CompoundTag save(CompoundTag tag) {
            rows.forEach((id, row) -> tag.put(id.toString(), row));
            return tag;
        }
        void materializeStored(MinecraftServer server) {
            int budget = 4;
            int checks = Math.min(32, stored.size());
            while (checks-- > 0) {
                UUID id = stored.removeFirst();
                CompoundTag row = rows.get(id);
                if (row == null || row.getBoolean("materialized")) continue;
                stored.addLast(id);
                TameData data = TameData.fromTag(row);
                for (ServerLevel level : server.getAllLevels()) {
                    if (!level.dimension().location().toString().equals(data.lastKnownDimension)) continue;
                    if (!level.hasChunkAt(new net.minecraft.core.BlockPos(data.lastKnownX, data.lastKnownY, data.lastKnownZ))) continue;
                    Entity entity = EntityType.loadEntityRecursive(data.entitySnapshot.copy(), level, value -> value);
                    if (entity instanceof LivingEntity living) {
                        living.moveTo(data.lastKnownX + 0.5, data.lastKnownY, data.lastKnownZ + 0.5,
                                living.getYRot(), living.getXRot());
                        applyPending(living);
                        if (!level.addFreshEntity(living)) {
                            row.putBoolean("materialized", false);
                            setDirty();
                        }
                        if (--budget == 0) return;
                    }
                }
            }
        }
    }
}
