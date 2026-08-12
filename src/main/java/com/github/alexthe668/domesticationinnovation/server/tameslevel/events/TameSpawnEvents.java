package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TameSpawnEvents {
    private static final Pattern LEVEL_PREFIX =
            Pattern.compile("^\\[lvl\\s*(\\d+)\\]\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Path RANDOM_TAME_NAME_FILE = FMLPaths.CONFIGDIR.get()
            .resolve("domesticationinnovation")
            .resolve("tame_name_pool.txt");
    private static final List<String> DEFAULT_RANDOM_TAME_NAMES = List.of(
            "Bramble",
            "Miso",
            "Thistle",
            "Koda",
            "Juniper",
            "Pico",
            "Sable",
            "Mochi",
            "Rook",
            "Tansy"
    );
    private static final Map<UUID, Long> PENDING_DEFERRED_STAT_REFRESH = new HashMap<>();
    private static final Map<UUID, PendingNewTameNotification> PENDING_NEW_TAME_NOTIFICATIONS = new HashMap<>();
    private static final long DEFERRED_STAT_REFRESH_DELAY_TICKS = 1200L;

    @SubscribeEvent
    public static void onSpawn(EntityJoinLevelEvent event) {

        if (!(event.getEntity() instanceof TamableAnimal tame)) return;

        // only track tamed animals
        if (!tame.isTame()) return;

        if (purgeInvalidPrefixedTame(tame)) return;

        // Always ensure goals are present for loaded tames, even if already registered.
        TameGoalInstaller.installIfMissing(tame);

        UUID entityTlId = TameData.readOrCreateTlId(tame);

        // Normal dimension travel can temporarily expose the same UUID during transfer;
        // if already tracked, bind identity tags and never discard the newly joined entity here.
        TameData existingByUuid = TameRegistry.get(tame.getUUID());
        if (existingByUuid != null) {
            TameRegistry.bindEntityToData(tame, existingByUuid);
            LevelSystem.reapplyTypeBasePlusBonuses(tame, existingByUuid);
            return;
        }

        TameData existingByTlId = TameRegistry.getByTlId(entityTlId);
        if (existingByTlId != null) {
            TamableAnimal loadedByTlId = findOtherLoadedByTlId(tame, entityTlId);
            if (loadedByTlId != null) {
                if (shouldKeepJoiningTame(tame, loadedByTlId, existingByTlId)) {
                    forceRemoveTameEntity(loadedByTlId);
                    TameRegistry.rebindEntityUuid(existingByTlId, tame.getUUID());
                    registerOrRestoreTame(tame, false, true);
                    return;
                } else {
                    forceRemoveTameEntity(tame);
                    return;
                }
            }
        }

        TamableAnimal existing = findOtherLoadedByUuid(tame);
        if (existing != null) {
            // If a copy with the same UUID is already active and this UUID is not yet tracked,
            // treat this join as a duplicate materialization and discard it.
            forceRemoveTameEntity(tame);
            return;
        }

        ParsedName parsed = parseName(tame);
        TamableAnimal clone = findLoadedCloneByIdentity(tame, parsed);
        if (clone != null) {
            if (shouldKeepJoiningIdentityClone(tame, clone, parsed)) {
                TameData cloneData = TameRegistry.get(clone.getUUID());
                forceRemoveTameEntity(clone);
                if (cloneData != null) {
                    TameRegistry.rebindEntityUuid(cloneData, tame.getUUID());
                    registerOrRestoreTame(tame, false, true);
                    return;
                }
            } else {
                forceRemoveTameEntity(tame);
                return;
            }
        }

        registerOrRestoreTame(tame, true, true);
    }

    @SubscribeEvent
    public static void onTamed(AnimalTameEvent event) {
        if (!(event.getAnimal() instanceof TamableAnimal tame)) return;
        if (event.getTamer() != null && tame.getOwnerUUID() == null) {
            // Ensure owner is available before registry dead-entry matching.
            tame.setOwnerUUID(event.getTamer().getUUID());
        }
        if (purgeInvalidPrefixedTame(tame)) return;

        // Allow dead-entry identity sync so reincarnation can consume dead rows
        // instead of creating a second live entry with the same base identity.
        TameData data = registerOrRestoreTame(tame, true, true);
        if (data == null) return;
        boolean changed = false;
        if (event.getTamer() != null) {
            if (!event.getTamer().getUUID().equals(data.ownerUUID)) {
                data.ownerUUID = event.getTamer().getUUID();
                changed = true;
            }
        } else if (tame.getOwnerUUID() != null && !tame.getOwnerUUID().equals(data.ownerUUID)) {
            data.ownerUUID = tame.getOwnerUUID();
            changed = true;
        }
        String current = data.name == null || data.name.isBlank()
                ? (tame.hasCustomName() && tame.getCustomName() != null ? tame.getCustomName().getString() : tame.getName().getString())
                : data.name;
        String unique = uniqueLoadedNameFor(tame, current);
        if (!unique.equals(data.name)) {
            data.name = unique;
            changed = true;
        }
        LevelSystem.updateTameName(tame, data);
        if (changed) {
            TameRegistry.markDirty();
        }
        LevelSystem.ensureClassAssigned(tame, data, true);
        TameGoalInstaller.installIfMissing(tame);
    }

    public static TameData registerOrRestoreTame(TamableAnimal tame, boolean notifyClassIfNew) {
        return registerOrRestoreTame(tame, notifyClassIfNew, true);
    }

    public static TameData registerOrRestoreTame(TamableAnimal tame, boolean notifyClassIfNew, boolean allowDeadIdentitySync) {
        if (tame == null || !tame.isTame()) return null;
        if (purgeInvalidPrefixedTame(tame)) return null;
        if (TameRegistry.isTameTypeDisabled(tame)) {
            return null;
        }
        UUID entityTlId = TameData.readOrCreateTlId(tame);
        TameData existing = TameRegistry.get(tame.getUUID());
        if (existing != null) {
            boolean changed = false;
            boolean released = existing.stored;
            if (existing.dead) {
                existing.dead = false;
                existing.deadGameTime = 0L;
                existing.deadUnixMillis = 0L;
                existing.deathDimension = "";
                existing.deathX = 0;
                existing.deathY = 0;
                existing.deathZ = 0;
                changed = true;
            }
            if (existing.stored) {
                existing.stored = false;
                changed = true;
            }
            if (TameBedRegistrySync.syncFromEntity(tame, existing)) {
                changed = true;
            }
            if (changed) {
                TameRegistry.markDirty();
            }
            if (released) {
                notifyFlutterReleased(tame, existing);
            }
            // Keep entity attributes in sync with registry bonuses whenever a tracked tame loads.
            TameRegistry.bindEntityToData(tame, existing);
            LevelSystem.reapplyTypeBasePlusBonuses(tame, existing);
            queueDeferredStatRefresh(tame, existing, DEFERRED_STAT_REFRESH_DELAY_TICKS);
            return existing;
        }

        TameData existingByTlId = TameRegistry.getByTlId(entityTlId);
        if (existingByTlId != null) {
            TameRegistry.rebindEntityUuid(existingByTlId, tame.getUUID());
            TameRegistry.bindEntityToData(tame, existingByTlId);
            boolean changed = false;
            boolean released = existingByTlId.stored;
            if (existingByTlId.dead) {
                existingByTlId.dead = false;
                existingByTlId.deadGameTime = 0L;
                existingByTlId.deadUnixMillis = 0L;
                existingByTlId.deathDimension = "";
                existingByTlId.deathX = 0;
                existingByTlId.deathY = 0;
                existingByTlId.deathZ = 0;
                changed = true;
            }
            if (existingByTlId.stored) {
                existingByTlId.stored = false;
                changed = true;
            }
            if (TameBedRegistrySync.syncFromEntity(tame, existingByTlId)) {
                changed = true;
            }
            if (changed) {
                TameRegistry.markDirty();
            }
            if (released) {
                notifyFlutterReleased(tame, existingByTlId);
            }
            LevelSystem.reapplyTypeBasePlusBonuses(tame, existingByTlId);
            queueDeferredStatRefresh(tame, existingByTlId, DEFERRED_STAT_REFRESH_DELAY_TICKS);
            return existingByTlId;
        }

        ParsedName parsed = parseName(tame);
        TameData synced = trySyncFromLeveledNameMatch(tame, parsed);
        if (synced != null) {
            return synced;
        }
        if (allowDeadIdentitySync) {
            TameData syncedDead = trySyncFromDeadIdentityMatch(tame, parsed);
            if (syncedDead != null) {
                return syncedDead;
            }
        }
        TameData data = new TameData(tame);
        if (data.ownerUUID == null && tame.getOwnerUUID() != null) {
            data.ownerUUID = tame.getOwnerUUID();
        }
        String randomName = pickRandomUnusedTameName(usedLoadedNamesForOwner(tame));
        data.name = uniqueLoadedNameFor(tame, randomName.isBlank() ? data.name : randomName);
        TameBedRegistrySync.syncFromEntity(tame, data);
        TameRegistry.register(data);
        TameRegistry.bindEntityToData(tame, data);
        notifyNewTameFound(tame, data);
        LevelSystem.ensureClassAssigned(tame, data, true);
        LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
        queueDeferredStatRefresh(tame, data, DEFERRED_STAT_REFRESH_DELAY_TICKS);
        System.out.println("[TamesLevel] Registered tame: " + data.name);
        return data;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || PENDING_NEW_TAME_NOTIFICATIONS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        PENDING_NEW_TAME_NOTIFICATIONS.entrySet().removeIf(entry -> trySendPendingNewTameNotification(server, entry.getKey(), entry.getValue()));
    }

    public static void queueDeferredStatRefresh(TamableAnimal tame, TameData data, long delayTicks) {
        if (tame == null || data == null || tame.level().isClientSide || !LevelSystem.needsDeferredStatRefresh(tame, data)) {
            return;
        }
        UUID key = data.ensureTlId();
        long dueTick = tame.level().getGameTime() + Math.max(1L, delayTicks);
        Long existing = PENDING_DEFERRED_STAT_REFRESH.get(key);
        if (existing == null || dueTick > existing) {
            PENDING_DEFERRED_STAT_REFRESH.put(key, dueTick);
        }
    }

    public static void processDeferredStatRefresh(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || tame.level().isClientSide) {
            return;
        }
        UUID key = data.ensureTlId();
        Long dueTick = PENDING_DEFERRED_STAT_REFRESH.get(key);
        if (dueTick == null || tame.level().getGameTime() < dueTick) {
            return;
        }
        PENDING_DEFERRED_STAT_REFRESH.remove(key);

        float oldHealth = tame.getHealth();
        float oldMaxHealth = Math.max(1.0F, (float) tame.getMaxHealth());
        double healthRatio = Mth.clamp(oldHealth / oldMaxHealth, 0.0F, 1.0F);

        TameRegistry.bindEntityToData(tame, data);
        LevelSystem.ensureClassAssigned(tame, data, false);
        if (!LevelSystem.reapplyTypeBasePlusBonuses(tame, data)) {
            return;
        }
        LevelSystem.updateTameName(tame, data);
        tame.setHealth((float) Mth.clamp(tame.getMaxHealth() * healthRatio, 1.0D, tame.getMaxHealth()));
        refreshRegistrySnapshot(tame, data);
        queueDeferredStatRefresh(tame, data, DEFERRED_STAT_REFRESH_DELAY_TICKS);
    }

    private static void refreshRegistrySnapshot(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return;
        }
        CompoundTag snapshot = new CompoundTag();
        tame.save(snapshot);
        data.entitySnapshot = snapshot;
        data.lastKnownDimension = tame.level().dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = tame.level().getGameTime();
        TameRegistry.markDirty();
    }

    private static void notifyNewTameFound(TamableAnimal tame, TameData data) {
        if (!(tame.getOwner() instanceof ServerPlayer owner) || data == null) {
            return;
        }
        if (tame.getPersistentData().getBoolean(TameCommands.ADMIN_CLONE_SILENT_TAG)) {
            return;
        }
        String name = data == null || data.name == null || data.name.isBlank()
                ? tame.getName().getString()
                : data.name;
        long dueTick = tame.level().getGameTime() + 1L;
        PENDING_NEW_TAME_NOTIFICATIONS.put(data.uuid, new PendingNewTameNotification(owner.getUUID(), data.uuid, data.ensureTlId(), name, dueTick));
    }

    private static boolean trySendPendingNewTameNotification(MinecraftServer server, UUID tameUuid, PendingNewTameNotification pending) {
        if (server == null || pending == null) {
            return true;
        }
        ServerPlayer owner = server.getPlayerList().getPlayer(pending.ownerUuid());
        if (owner == null) {
            return true;
        }
        if (!PlayerDebugSettings.newTameMessages(owner.getUUID())) {
            return true;
        }
        TameData data = TameRegistry.get(tameUuid);
        if (data == null || data.isInactive() || !pending.ownerUuid().equals(data.ownerUUID) || pending.tlId() == null || !pending.tlId().equals(data.tlId)) {
            return true;
        }
        TamableAnimal loaded = findLoadedByUuid(server, tameUuid);
        if (loaded == null || !loaded.isAlive() || loaded.isRemoved()) {
            return true;
        }
        if (!(loaded.level() instanceof ServerLevel serverLevel) || serverLevel.getGameTime() < pending.dueTick()) {
            return false;
        }
        owner.sendSystemMessage(Component.literal("Found new tame: " + pending.name() + ".").withStyle(ChatFormatting.AQUA));
        return true;
    }

    private static TamableAnimal findLoadedByUuid(MinecraftServer server, UUID tameUuid) {
        if (server == null || tameUuid == null) {
            return null;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (!(level.getEntity(tameUuid) instanceof TamableAnimal tame)) {
                continue;
            }
            return tame;
        }
        return null;
    }

    private static TameData trySyncFromLeveledNameMatch(TamableAnimal tame, ParsedName parsed) {
        UUID ownerId = tame.getOwnerUUID();
        if (ownerId == null || parsed == null) return null;
        if (parsed.baseName == null || parsed.baseName.isBlank() || parsed.level <= 0) return null;

        UUID newUuid = tame.getUUID();
        TameData candidate = null;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null || d.name == null) continue;
            if (!ownerId.equals(d.ownerUUID)) continue;
            if (!d.name.equalsIgnoreCase(parsed.baseName)) continue;
            if (d.level != parsed.level) continue;
            if (d.uuid.equals(newUuid)) continue;
            if (isUuidLoaded(tame, d.uuid)) continue;
            candidate = d;
            break;
        }

        if (candidate == null) return null;

        UUID oldUuid = candidate.uuid;
        if (candidate.ownerUUID == null) {
            candidate.ownerUUID = ownerId;
        }
        boolean released = candidate.stored;
        candidate.stored = false;
        // Normalize back to base name if spawn item carried [Lvl X] prefix.
        candidate.name = uniqueLoadedNameFor(tame, parsed.baseName);
        TameRegistry.rebindEntityUuid(candidate, newUuid);
        TameRegistry.bindEntityToData(tame, candidate);
        TameRegistry.markDirty();
        LevelSystem.reapplyTypeBasePlusBonuses(tame, candidate);
        LevelSystem.updateTameName(tame, candidate);
        if (released) {
            notifyFlutterReleased(tame, candidate);
        }
        System.out.println("[TamesLevel] Synced tame entry by [Lvl] name match: " + candidate.name + " (" + oldUuid + " -> " + newUuid + ")");
        return candidate;
    }

    private static TameData trySyncFromDeadIdentityMatch(TamableAnimal tame, ParsedName parsed) {
        UUID ownerId = tame.getOwnerUUID();
        if (ownerId == null) return null;

        String baseName = parsed != null ? parsed.baseName : "";
        if (baseName == null || baseName.isBlank()) {
            baseName = stripLevelPrefixes(tame.hasCustomName() && tame.getCustomName() != null ? tame.getCustomName().getString() : tame.getName().getString());
        }
        if (baseName == null || baseName.isBlank()) {
            return null;
        }

        String tameType = normalizeTypeId(ForgeRegistries.ENTITY_TYPES.getKey(tame.getType()) == null ? "" : ForgeRegistries.ENTITY_TYPES.getKey(tame.getType()).toString());
        UUID newUuid = tame.getUUID();

        TameData candidate = null;
        long bestUnix = Long.MIN_VALUE;
        long bestGame = Long.MIN_VALUE;
        for (TameData d : TameRegistry.TAMES.values()) {
            if (d == null || d.uuid == null || d.name == null) continue;
            if (!ownerId.equals(d.ownerUUID)) continue;
            if (!d.dead) continue;
            if (d.uuid.equals(newUuid)) continue;
            if (!stripLevelPrefixes(d.name).equalsIgnoreCase(baseName)) continue;
            if (!sameType(tameType, d.type)) continue;
            if (isUuidLoaded(tame, d.uuid)) continue;
            if (d.deadUnixMillis > bestUnix || (d.deadUnixMillis == bestUnix && d.deadGameTime > bestGame)) {
                bestUnix = d.deadUnixMillis;
                bestGame = d.deadGameTime;
                candidate = d;
            }
        }
        if (candidate == null) return null;

        UUID oldUuid = candidate.uuid;
        candidate.dead = false;
        candidate.stored = false;
        candidate.deadGameTime = 0L;
        candidate.deadUnixMillis = 0L;
        candidate.deathDimension = "";
        candidate.deathX = 0;
        candidate.deathY = 0;
        candidate.deathZ = 0;
        candidate.name = uniqueLoadedNameFor(tame, stripLevelPrefixes(candidate.name));
        TameBedRegistrySync.syncFromEntity(tame, candidate);

        TameRegistry.rebindEntityUuid(candidate, newUuid);
        TameRegistry.bindEntityToData(tame, candidate);
        TameRegistry.markDirty();
        LevelSystem.reapplyTypeBasePlusBonuses(tame, candidate);
        LevelSystem.updateTameName(tame, candidate);
        System.out.println("[TamesLevel] Synced respawned tame to dead entry: " + candidate.name + " (" + oldUuid + " -> " + newUuid + ")");
        return candidate;
    }

    public static void markFlutterStoredFromPot(TamableAnimal tame) {
        if (tame == null || tame.level().isClientSide || !isAlexsMobsFlutter(tame)) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            UUID tlId = TameData.getTlId(tame);
            if (tlId != null) {
                data = TameRegistry.getByTlId(tlId);
            }
        }
        if (data == null || data.stored) {
            return;
        }
        data.stored = true;
        TameRegistry.markDirty();
        notifyFlutterStored(tame, data);
    }

    private static boolean isAlexsMobsFlutter(TamableAnimal tame) {
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null && "alexsmobs:flutter".equals(key.toString());
    }

    private static void notifyFlutterStored(TamableAnimal tame, TameData data) {
        if (!isAlexsMobsFlutter(tame)) {
            return;
        }
        if (tame.getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.literal("flutter stored").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (data != null && data.ownerUUID != null && tame.level().getServer() != null) {
            ServerPlayer owner = tame.level().getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                owner.displayClientMessage(Component.literal("flutter stored").withStyle(ChatFormatting.YELLOW), true);
            }
        }
    }

    private static void notifyFlutterReleased(TamableAnimal tame, TameData data) {
        if (!isAlexsMobsFlutter(tame)) {
            return;
        }
        if (tame.getOwner() instanceof ServerPlayer owner) {
            owner.displayClientMessage(Component.literal("flutter released").withStyle(ChatFormatting.GREEN), true);
            return;
        }
        if (data != null && data.ownerUUID != null && tame.level().getServer() != null) {
            ServerPlayer owner = tame.level().getServer().getPlayerList().getPlayer(data.ownerUUID);
            if (owner != null) {
                owner.displayClientMessage(Component.literal("flutter released").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    private static boolean sameType(String tameType, String dataTypeRaw) {
        String dataType = normalizeTypeId(dataTypeRaw);
        if (tameType.isBlank() || dataType.isBlank()) {
            return true;
        }
        if (tameType.equals(dataType)) {
            return true;
        }
        int tameSep = tameType.indexOf(':');
        int dataSep = dataType.indexOf(':');
        String tamePath = tameSep >= 0 ? tameType.substring(tameSep + 1) : tameType;
        String dataPath = dataSep >= 0 ? dataType.substring(dataSep + 1) : dataType;
        return tamePath.equals(dataPath);
    }

    private static String normalizeTypeId(String raw) {
        if (raw == null) return "";
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return "";
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
            int firstDot = normalized.indexOf('.');
            if (firstDot > 0 && !normalized.contains(":")) {
                normalized = normalized.substring(0, firstDot) + ":" + normalized.substring(firstDot + 1);
            }
        }
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(normalized);
        return parsed == null ? normalized : parsed.toString();
    }

    private static boolean isUuidLoaded(TamableAnimal context, UUID uuid) {
        if (context == null || uuid == null || context.level() == null || context.level().getServer() == null) return false;
        for (var level : context.level().getServer().getAllLevels()) {
            if (level.getEntity(uuid) != null) return true;
        }
        return false;
    }

    private static TamableAnimal findOtherLoadedByUuid(TamableAnimal context) {
        if (context == null || context.level() == null || context.level().getServer() == null) return null;
        UUID uuid = context.getUUID();
        for (var level : context.level().getServer().getAllLevels()) {
            var found = level.getEntity(uuid);
            if (found instanceof TamableAnimal other && other != context) {
                return other;
            }
        }
        return null;
    }

    private static TamableAnimal findOtherLoadedByTlId(TamableAnimal context, UUID tlId) {
        if (context == null || tlId == null || context.level() == null || context.level().getServer() == null) return null;
        for (var level : context.level().getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal other) || other == context) continue;
                UUID otherTlId = TameData.getTlId(other);
                if (tlId.equals(otherTlId)) {
                    return other;
                }
            }
        }
        return null;
    }

    private static boolean shouldKeepJoiningTame(TamableAnimal joining, TamableAnimal loaded, TameData tlData) {
        int joiningXp = resolveTrackedXp(joining, tlData);
        int loadedXp = resolveTrackedXp(loaded, tlData);
        if (joiningXp != loadedXp) {
            return joiningXp > loadedXp;
        }
        TameData joiningData = TameRegistry.get(joining.getUUID());
        TameData loadedData = TameRegistry.get(loaded.getUUID());
        int joiningLevel = joiningData != null ? joiningData.level : (tlData == null ? 1 : tlData.level);
        int loadedLevel = loadedData != null ? loadedData.level : (tlData == null ? 1 : tlData.level);
        if (joiningLevel != loadedLevel) {
            return joiningLevel > loadedLevel;
        }
        return joining.tickCount >= loaded.tickCount;
    }

    private static boolean shouldKeepJoiningIdentityClone(TamableAnimal joining, TamableAnimal loaded, ParsedName joiningParsed) {
        int joiningLevel = resolveTrackedLevel(joining, joiningParsed);
        int loadedLevel = resolveTrackedLevel(loaded, parseName(loaded));
        if (joiningLevel != loadedLevel) {
            return joiningLevel > loadedLevel;
        }
        return joining.tickCount >= loaded.tickCount;
    }

    private static int resolveTrackedXp(TamableAnimal tame, TameData fallback) {
        if (tame == null) {
            return Integer.MIN_VALUE;
        }
        TameData exact = TameRegistry.get(tame.getUUID());
        if (exact != null) {
            return exact.xp;
        }
        UUID tlId = TameData.getTlId(tame);
        if (tlId != null) {
            TameData byTlId = TameRegistry.getByTlId(tlId);
            if (byTlId != null) {
                return byTlId.xp;
            }
        }
        return fallback == null ? 0 : fallback.xp;
    }

    private static TamableAnimal findLoadedCloneByIdentity(TamableAnimal context, ParsedName parsed) {
        if (context == null || parsed == null || context.level() == null || context.level().getServer() == null) return null;
        String thisBase = parsed.baseName == null || parsed.baseName.isBlank()
                ? stripLevelPrefixes(context.hasCustomName() && context.getCustomName() != null ? context.getCustomName().getString() : context.getName().getString())
                : parsed.baseName;
        if (thisBase.isBlank()) return null;
        UUID owner = context.getOwnerUUID();
        if (owner == null) return null;

        for (var level : context.level().getServer().getAllLevels()) {
            for (var entity : level.getAllEntities()) {
                if (!(entity instanceof TamableAnimal other) || other == context || !other.isTame() || !other.isAlive()) continue;
                if (!owner.equals(other.getOwnerUUID())) continue;
                if (!other.getType().equals(context.getType())) continue;
                ParsedName otherParsed = parseName(other);
                String otherBase = otherParsed.baseName == null || otherParsed.baseName.isBlank()
                        ? stripLevelPrefixes(other.hasCustomName() && other.getCustomName() != null ? other.getCustomName().getString() : other.getName().getString())
                        : otherParsed.baseName;
                if (!namesOverlapByContainment(thisBase, otherBase)) continue;
                return other;
            }
        }
        return null;
    }

    private static int resolveTrackedLevel(TamableAnimal tame, ParsedName parsed) {
        if (tame == null) {
            return Integer.MIN_VALUE;
        }
        TameData exact = TameRegistry.get(tame.getUUID());
        if (exact != null) {
            return exact.level;
        }
        UUID tlId = TameData.getTlId(tame);
        if (tlId != null) {
            TameData byTlId = TameRegistry.getByTlId(tlId);
            if (byTlId != null) {
                return byTlId.level;
            }
        }
        return parsed == null ? 1 : Math.max(1, parsed.level);
    }

    private static boolean namesOverlapByContainment(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        String left = stripLevelPrefixes(a).trim().toLowerCase(Locale.ROOT);
        String right = stripLevelPrefixes(b).trim().toLowerCase(Locale.ROOT);
        if (left.isBlank() || right.isBlank()) {
            return false;
        }
        return left.contains(right) || right.contains(left);
    }

    public static String uniqueLoadedNameFor(TamableAnimal self, String requestedName) {
        String base = stripLevelPrefixes(requestedName);
        java.util.Set<String> used = usedLoadedNamesForOwner(self);

        if (base.isBlank()) {
            String randomBlank = pickRandomUnusedTameName(used);
            if (!randomBlank.isBlank()) {
                return randomBlank;
            }
            base = "Tame";
        }

        String lower = base.toLowerCase(java.util.Locale.ROOT);
        if (!used.contains(lower)) {
            return base;
        }
        String randomReplacement = pickRandomUnusedTameName(used);
        if (!randomReplacement.isBlank()) {
            return randomReplacement;
        }
        int i = 2;
        while (i < 10000) {
            String candidate = base + " " + i;
            if (!used.contains(candidate.toLowerCase(java.util.Locale.ROOT))) {
                return candidate;
            }
            i++;
        }
        return base + " " + self.getUUID().toString().substring(0, 8);
    }

    private static java.util.Set<String> usedLoadedNamesForOwner(TamableAnimal self) {
        java.util.Set<String> used = new java.util.HashSet<>();
        UUID ownerId = self.getOwnerUUID();
        UUID selfTlId = TameData.getTlId(self);
        if (ownerId != null) {
            for (TameData data : TameRegistry.TAMES.values()) {
                if (data == null || data.uuid == null || data.name == null || data.name.isBlank()) continue;
                if (!ownerId.equals(data.ownerUUID)) continue;
                if (data.uuid.equals(self.getUUID())) continue;
                if (data.dead) continue;
                if (selfTlId != null && selfTlId.equals(data.tlId)) continue;
                if (!isUuidLoaded(self, data.uuid)) continue;
                used.add(stripLevelPrefixes(data.name).toLowerCase(java.util.Locale.ROOT));
            }
        }
        if (self.level() != null && self.level().getServer() != null) {
            for (var level : self.level().getServer().getAllLevels()) {
                for (var entity : level.getAllEntities()) {
                    if (!(entity instanceof TamableAnimal other) || !other.isTame()) continue;
                    if (other.getUUID().equals(self.getUUID())) continue;
                    if (ownerId != null && !ownerId.equals(other.getOwnerUUID())) continue;
                    String otherName;
                    TameData reg = TameRegistry.get(other.getUUID());
                    if (reg != null && reg.name != null && !reg.name.isBlank()) {
                        otherName = stripLevelPrefixes(reg.name);
                    } else {
                        String raw = other.hasCustomName() && other.getCustomName() != null
                                ? other.getCustomName().getString()
                                : other.getName().getString();
                        otherName = stripLevelPrefixes(raw);
                    }
                    if (!otherName.isBlank()) {
                        used.add(otherName.toLowerCase(java.util.Locale.ROOT));
                    }
                }
            }
        }
        return used;
    }

    private static String pickRandomUnusedTameName(java.util.Set<String> used) {
        List<String> pool = loadRandomTameNames();
        if (pool.isEmpty()) {
            return "";
        }
        List<String> shuffled = new ArrayList<>(pool);
        java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());
        for (String candidate : shuffled) {
            String cleaned = stripLevelPrefixes(candidate).trim();
            if (cleaned.isBlank()) {
                continue;
            }
            if (!used.contains(cleaned.toLowerCase(Locale.ROOT))) {
                return cleaned;
            }
        }
        return "";
    }

    private static List<String> loadRandomTameNames() {
        LinkedHashSet<String> configNames = new LinkedHashSet<>();
        try {
            for (String raw : DomesticationMod.CONFIG.randomTameNames.get()) {
                if (raw == null) {
                    continue;
                }
                String cleaned = stripLevelPrefixes(raw).trim();
                if (!cleaned.isBlank() && !cleaned.startsWith("#")) {
                    configNames.add(cleaned);
                }
            }
        } catch (RuntimeException ignored) {
        }
        if (!configNames.isEmpty()) {
            return new ArrayList<>(configNames);
        }
        try {
            ensureRandomTameNameFileExists();
            LinkedHashSet<String> names = new LinkedHashSet<>();
            for (String line : Files.readAllLines(RANDOM_TAME_NAME_FILE, StandardCharsets.UTF_8)) {
                if (line == null) {
                    continue;
                }
                String cleaned = line.trim();
                if (cleaned.isBlank() || cleaned.startsWith("#")) {
                    continue;
                }
                names.add(cleaned);
            }
            if (!names.isEmpty()) {
                return new ArrayList<>(names);
            }
        } catch (IOException ignored) {
        }
        return new ArrayList<>(DEFAULT_RANDOM_TAME_NAMES);
    }

    private static void ensureRandomTameNameFileExists() throws IOException {
        if (Files.exists(RANDOM_TAME_NAME_FILE)) {
            return;
        }
        Files.createDirectories(RANDOM_TAME_NAME_FILE.getParent());
        List<String> lines = new ArrayList<>();
        lines.add("# One tame name per line.");
        lines.add("# When a duplicate name would need a suffix, TL picks a random unused name from this file instead.");
        lines.addAll(DEFAULT_RANDOM_TAME_NAMES);
        Files.write(RANDOM_TAME_NAME_FILE, lines, StandardCharsets.UTF_8);
    }

    private static String stripLevelPrefixes(String name) {
        return TameRegistry.stripLevelPrefixes(name);
    }

    private static ParsedName parseName(TamableAnimal tame) {
        String raw = tame.hasCustomName() && tame.getCustomName() != null
                ? tame.getCustomName().getString()
                : tame.getName().getString();
        if (raw == null) {
            return new ParsedName("", -1);
        }
        Matcher matcher = LEVEL_PREFIX.matcher(raw.trim());
        if (matcher.matches()) {
            int level = -1;
            try {
                level = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
            String base = matcher.group(2) == null ? "" : matcher.group(2).trim();
            return new ParsedName(base, level);
        }
        return new ParsedName(raw.trim(), -1);
    }

    private record ParsedName(String baseName, int level) {}

    private static boolean purgeInvalidPrefixedTame(TamableAnimal tame) {
        if (tame == null || tame.level().isClientSide) {
            return false;
        }
        TameData existing = TameRegistry.get(tame.getUUID());
        if (existing != null) {
            if (!TameRegistry.hasLevelPrefixName(existing.name)) {
                return false;
            }
            TameRegistry.remove(existing.uuid);
            TameRegistry.removeDeathsForIdentity(existing.uuid, existing.tlId);
            forceRemoveTameEntity(tame);
            return true;
        }
        UUID tlId = TameData.getTlId(tame);
        TameData byTlId = tlId == null ? null : TameRegistry.getByTlId(tlId);
        if (byTlId == null || !TameRegistry.hasLevelPrefixName(byTlId.name)) {
            return false;
        }
        TameRegistry.remove(byTlId.uuid);
        TameRegistry.removeDeathsForIdentity(byTlId.uuid, byTlId.tlId);
        forceRemoveTameEntity(tame);
        return true;
    }

    private static void forceRemoveTameEntity(TamableAnimal tame) {
        if (tame == null) {
            return;
        }
        boolean silentClone = tame.getPersistentData().getBoolean(TameCommands.ADMIN_CLONE_SILENT_TAG);
        tame.setTarget(null);
        tame.getNavigation().stop();
        if (tame.isAlive()) {
            try {
                tame.kill();
            } catch (Throwable ignored) {
            }
        }
        if (silentClone) {
            return;
        }
        if (!tame.isRemoved()) {
            try {
                tame.remove(Entity.RemovalReason.KILLED);
            } catch (Throwable ignored) {
            }
        }
        if (!tame.isRemoved()) {
            tame.discard();
        }
    }

    private record PendingNewTameNotification(UUID ownerUuid, UUID tameUuid, UUID tlId, String name, long dueTick) {}
}
