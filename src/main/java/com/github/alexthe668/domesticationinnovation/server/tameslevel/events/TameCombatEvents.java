package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDeathRecord;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TameCombatEvents {
    private static final Object DEATH_QUEUE_LOCK = new Object();
    private static final Deque<PendingDeath> PENDING_DEATHS = new ArrayDeque<>();
    private static final Set<UUID> ACTIVE_DEATHS = new HashSet<>();
    private static boolean processingDeaths = false;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {

        TamableAnimal tame = resolveTameAttacker(event);
        if (tame == null) return;
        if (!tame.isTame()) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        LivingEntity mob = event.getEntity();
        LevelSystem.trackDamage(mob, tame);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        try {
            PendingDeath death = PendingDeath.capture(event);
            if (death == null) {
                return;
            }
            enqueueAndDrainDeaths(death);
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onDeath error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onTameDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;

        if (!(event.getEntity() instanceof TamableAnimal tame)) return;
        if (!tame.isTame()) return;

        boolean diedInDuel = TameDuelManager.isTameInDuel(tame.getUUID());
        if (diedInDuel) {
            UUID killerTameUuid = resolveKillerTameUuid(event);
            Set<UUID> contributors = new HashSet<>(LevelSystem.mobDamageTracker.getOrDefault(tame.getUUID(), Set.of()));
            TameDuelManager.recordElimination(tame.level().getServer(), tame.getUUID(), contributors, killerTameUuid);
        }
        TameDuelManager.endDuelForTame(tame.level().getServer(), tame.getUUID());
        TameData data = TameRegistry.get(tame.getUUID());
        if (data != null) {
            LevelSystem.storeHighestProgressSnapshot(data);
        }
        LevelSystem.onTameDeath(tame);
        if (data != null) {
            String deathMessage = event.getSource().getLocalizedDeathMessage(tame).getString();
            TameDeathRecord deathRecord = TameDeathRecord.fromTame(data, tame, tame.level().getGameTime());
            TameRegistry.archiveDeath(deathRecord);
            // Reincarnation is disabled; keep the live registry row so DI respawn
            // can continue with the same tame progress and bonuses.
            if (data.entitySnapshot != null) {
                tame.save(data.entitySnapshot);
            }
            data.dead = true;
            data.deadGameTime = tame.level().getGameTime();
            data.deadUnixMillis = System.currentTimeMillis();
            data.deathDimension = tame.level().dimension().location().toString();
            data.deathX = tame.blockPosition().getX();
            data.deathY = tame.blockPosition().getY();
            data.deathZ = tame.blockPosition().getZ();
            CompoundTag deathRow = new CompoundTag();
            deathRow.putLong("gameTime", data.deadGameTime);
            deathRow.putLong("unixMillis", data.deadUnixMillis);
            deathRow.putInt("level", data.level);
            deathRow.putInt("kills", data.kills);
            deathRow.putInt("assists", data.assists);
            deathRow.putInt("deaths", data.deaths);
            deathRow.putString("message", deathMessage == null ? "" : deathMessage);
            deathRow.putString("dimension", data.deathDimension == null ? "" : data.deathDimension);
            deathRow.putInt("x", data.deathX);
            deathRow.putInt("y", data.deathY);
            deathRow.putInt("z", data.deathZ);
            data.deathHistory.add(deathRow);
            while (data.deathHistory.size() > 64) {
                data.deathHistory.remove(0);
            }
            if (diedInDuel) {
                data.escapeActive = false;
            }
            TameBedRegistrySync.syncFromEntity(tame, data);
            TameRegistry.markDirty();
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TameDuelManager.isEntityInDuel(player.getUUID())) return;

        UUID killerParticipantUuid = resolveKillerParticipantUuid(event);
        TameDuelManager.recordElimination(player.level().getServer(), player.getUUID(), Set.of(), killerParticipantUuid);
        TameDuelManager.endDuelForEntity(player.level().getServer(), player.getUUID());
    }

    private static TamableAnimal resolveTameAttacker(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof TamableAnimal tame) {
            return tame;
        }

        if (event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() instanceof TamableAnimal tame) {
            return tame;
        }

        return null;
    }

    private static UUID resolveKillerTameUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        if (event.getSource().getEntity() instanceof TamableAnimal tame && tame.isTame()) {
            return tame.getUUID();
        }
        if (event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() instanceof TamableAnimal tame
                && tame.isTame()) {
            return tame.getUUID();
        }
        return null;
    }

    private static UUID resolveKillerParticipantUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        if (event.getSource().getEntity() instanceof TamableAnimal tame && tame.isTame()) {
            return tame.getUUID();
        }
        if (event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                && ownable.getOwner() instanceof TamableAnimal tame
                && tame.isTame()) {
            return tame.getUUID();
        }
        return null;
    }

    private static void enqueueAndDrainDeaths(PendingDeath death) {
        synchronized (DEATH_QUEUE_LOCK) {
            if (!ACTIVE_DEATHS.add(death.deadId())) {
                return;
            }
            if (processingDeaths) {
                PENDING_DEATHS.addLast(death);
                return;
            }
            processingDeaths = true;
        }

        try {
            PendingDeath current = death;
            while (current != null) {
                try {
                    processDeath(current);
                } catch (Throwable t) {
                    System.err.println("[TamesLevel] queued onDeath error: " + t.getClass().getName() + ": " + t.getMessage());
                    t.printStackTrace();
                } finally {
                    synchronized (DEATH_QUEUE_LOCK) {
                        ACTIVE_DEATHS.remove(current.deadId());
                        current = PENDING_DEATHS.pollFirst();
                    }
                }
            }
        } finally {
            synchronized (DEATH_QUEUE_LOCK) {
                processingDeaths = false;
                PENDING_DEATHS.clear();
                ACTIVE_DEATHS.clear();
            }
        }
    }

    private static void processDeath(PendingDeath death) {
        LivingEntity dead = death.dead();
        LevelSystem.distributeXP(dead, death.killer());
        if (!(dead.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID tameId : death.contributors()) {
            if (!(serverLevel.getEntity(tameId) instanceof TamableAnimal tame) || !tame.isTame()) {
                continue;
            }
            TameData data = TameRegistry.get(tameId);
            if (data == null) {
                continue;
            }
            boolean wasKiller = death.killerTameUuid() != null && death.killerTameUuid().equals(tameId);
            TameAbilityEvents.onKillOrAssist(tame, data, dead, wasKiller);
        }
        debugEnemyKilled(serverLevel, dead, dead.getExperienceReward(), death.contributors(), death.killerTameUuid());
    }

    private record PendingDeath(LivingEntity dead, UUID deadId, LivingEntity killer, UUID killerTameUuid, Set<UUID> contributors) {
        private static PendingDeath capture(LivingDeathEvent event) {
            if (event == null || event.isCanceled()) {
                return null;
            }
            LivingEntity dead = event.getEntity();
            if (dead == null) {
                return null;
            }
            LivingEntity killer = null;
            UUID killerTameUuid = null;

            if (event.getSource().getEntity() instanceof LivingEntity e) {
                killer = e;
                if (e instanceof TamableAnimal tame && tame.isTame()) {
                    killerTameUuid = tame.getUUID();
                }
            }
            if (killerTameUuid == null
                    && event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                    && ownable.getOwner() instanceof TamableAnimal tame
                    && tame.isTame()) {
                killer = tame;
                killerTameUuid = tame.getUUID();
            }

            return new PendingDeath(
                    dead,
                    dead.getUUID(),
                    killer,
                    killerTameUuid,
                    new HashSet<>(LevelSystem.mobDamageTracker.getOrDefault(dead.getUUID(), Set.of()))
            );
        }
    }

    private static void debugEnemyKilled(ServerLevel level, LivingEntity dead, int xpReward, Set<UUID> contributors, UUID killerTameUuid) {
        if (contributors == null || contributors.isEmpty()) return;

        Map<UUID, String> killerByOwner = new HashMap<>();
        Map<UUID, List<String>> assistsByOwner = new HashMap<>();
        for (UUID tameId : contributors) {
            TameData data = TameRegistry.get(tameId);
            if (data == null || data.ownerUUID == null) continue;
            if (killerTameUuid != null && killerTameUuid.equals(tameId)) {
                killerByOwner.put(data.ownerUUID, data.name);
            } else {
                assistsByOwner.computeIfAbsent(data.ownerUUID, k -> new ArrayList<>()).add(data.name);
            }
        }

        Set<UUID> owners = new HashSet<>();
        owners.addAll(killerByOwner.keySet());
        owners.addAll(assistsByOwner.keySet());

        String mobType = dead.getType().toShortString();
        for (UUID ownerId : owners) {
            if (!PlayerDebugSettings.enemyKilled(ownerId)) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null) continue;

            String killerName = killerByOwner.getOrDefault(ownerId, "-");
            List<String> assisters = assistsByOwner.getOrDefault(ownerId, List.of());
            StringBuilder line = new StringBuilder();
            line.append("enemyKilled true: Killed: ")
                    .append(mobType)
                    .append("[")
                    .append(xpReward)
                    .append("]: K: ")
                    .append(killerName);
            for (String assister : assisters) {
                line.append(", A: ").append(assister);
            }
            owner.sendSystemMessage(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
