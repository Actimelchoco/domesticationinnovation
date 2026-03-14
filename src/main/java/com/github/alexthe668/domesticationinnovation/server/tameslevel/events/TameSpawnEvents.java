package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TameSpawnEvents {
    private static final Pattern LEVEL_PREFIX =
            Pattern.compile("^\\[lvl\\s*(\\d+)\\]\\s*(.*)$", Pattern.CASE_INSENSITIVE);

    @SubscribeEvent
    public static void onSpawn(EntityJoinLevelEvent event) {

        if (!(event.getEntity() instanceof TamableAnimal tame)) return;

        // only track tamed animals
        if (!tame.isTame()) return;

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
                    loadedByTlId.discard();
                    TameRegistry.rebindEntityUuid(existingByTlId, tame.getUUID());
                    TameRegistry.bindEntityToData(tame, existingByTlId);
                    LevelSystem.reapplyTypeBasePlusBonuses(tame, existingByTlId);
                } else {
                    tame.discard();
                    return;
                }
            }
        }

        TamableAnimal existing = findOtherLoadedByUuid(tame);
        if (existing != null) {
            // If a copy with the same UUID is already active and this UUID is not yet tracked,
            // treat this join as a duplicate materialization and discard it.
            tame.discard();
            return;
        }

        ParsedName parsed = parseName(tame);
        TamableAnimal clone = findLoadedCloneByIdentity(tame, parsed);
        if (clone != null) {
            // Prevent external/duplicate respawn systems from materializing clones
            // when an equivalent [Lvl X] tame with the same owner and name is already loaded.
            tame.discard();
            return;
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
        UUID entityTlId = TameData.readOrCreateTlId(tame);
        TameData existing = TameRegistry.get(tame.getUUID());
        if (existing != null) {
            boolean changed = false;
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
            if (TameBedRegistrySync.syncFromEntity(tame, existing)) {
                changed = true;
            }
            if (changed) {
                TameRegistry.markDirty();
            }
            // Keep entity attributes in sync with registry bonuses whenever a tracked tame loads.
            TameRegistry.bindEntityToData(tame, existing);
            LevelSystem.reapplyTypeBasePlusBonuses(tame, existing);
            return existing;
        }

        TameData existingByTlId = TameRegistry.getByTlId(entityTlId);
        if (existingByTlId != null) {
            TameRegistry.rebindEntityUuid(existingByTlId, tame.getUUID());
            TameRegistry.bindEntityToData(tame, existingByTlId);
            boolean changed = false;
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
            if (TameBedRegistrySync.syncFromEntity(tame, existingByTlId)) {
                changed = true;
            }
            if (changed) {
                TameRegistry.markDirty();
            }
            LevelSystem.reapplyTypeBasePlusBonuses(tame, existingByTlId);
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
        data.name = uniqueLoadedNameFor(tame, data.name);
        TameBedRegistrySync.syncFromEntity(tame, data);
        TameRegistry.register(data);
        TameRegistry.bindEntityToData(tame, data);
        LevelSystem.ensureClassAssigned(tame, data, notifyClassIfNew);
        LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
        System.out.println("[TamesLevel] Registered tame: " + data.name);
        return data;
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
        // Normalize back to base name if spawn item carried [Lvl X] prefix.
        candidate.name = uniqueLoadedNameFor(tame, parsed.baseName);
        TameRegistry.rebindEntityUuid(candidate, newUuid);
        TameRegistry.bindEntityToData(tame, candidate);
        TameRegistry.markDirty();
        LevelSystem.reapplyTypeBasePlusBonuses(tame, candidate);
        LevelSystem.updateTameName(tame, candidate);
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
                if (!otherBase.equalsIgnoreCase(thisBase)) continue;
                return other;
            }
        }
        return null;
    }

    public static String uniqueLoadedNameFor(TamableAnimal self, String requestedName) {
        String base = stripLevelPrefixes(requestedName);
        if (base.isBlank()) {
            base = "Tame";
        }

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

        String lower = base.toLowerCase(java.util.Locale.ROOT);
        if (!used.contains(lower)) {
            return base;
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

    private static String stripLevelPrefixes(String name) {
        if (name == null) return "";
        String cleaned = name;
        while (true) {
            String next = LEVEL_PREFIX.matcher(cleaned).replaceFirst("");
            if (next.equals(cleaned)) break;
            cleaned = next;
        }
        return cleaned.trim();
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
}
