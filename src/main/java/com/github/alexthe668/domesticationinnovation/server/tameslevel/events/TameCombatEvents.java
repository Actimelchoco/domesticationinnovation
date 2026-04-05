package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BlessfulledCompat;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
    private record PendingInstantRespawn(UUID tameUuid, long dueTick, int retriesRemaining) {
    }

    private static final Object DEATH_QUEUE_LOCK = new Object();
    private static final Deque<PendingDeath> PENDING_DEATHS = new ArrayDeque<>();
    private static final Set<UUID> ACTIVE_DEATHS = new HashSet<>();
    private static final Map<UUID, PendingDeath> CAPTURED_DEATHS = new HashMap<>();
    private static final Map<UUID, PendingInstantRespawn> PENDING_INSTANT_RESPAWNS = new HashMap<>();
    private static boolean processingDeaths = false;
    private static long serverTick = 0L;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity mob = event.getEntity();
        if (event.getAmount() <= 0.0F) return;

        TamableAnimal tame = resolveTameAttacker(event);
        if (tame != null && tame.isTame()) {
            LevelSystem.trackDamage(mob, tame);
            boolean duelPink = TameDuelManager.isTameInDuel(tame.getUUID()) && !TameDuelManager.isTeamAEntity(tame.getUUID());
            BlessfulledCompat.showTameDealtDamagePopup(tame, mob, event.getAmount(), duelPink);
        } else if (mob instanceof TamableAnimal targetTame && targetTame.isTame()) {
            BlessfulledCompat.showTameReceivedDamagePopup(event.getSource().getEntity(), targetTame, event.getAmount());
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LevelSystem.trackOwnerDamage(mob, player);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        try {
            PendingDeath death = PendingDeath.capture(event);
            if (death == null) {
                return;
            }
            synchronized (DEATH_QUEUE_LOCK) {
                CAPTURED_DEATHS.put(death.deadId(), death);
            }
            enqueueAndDrainDeaths(death);
        } catch (Throwable t) {
            System.err.println("[TamesLevel] onDeath error: " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onTameDrops(LivingDropsEvent event) {
        if (event == null || !(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) {
            return;
        }
        List<ItemStack> allowed = collectRetainedDeathItems(tame);
        event.getDrops().removeIf(drop -> !shouldKeepDrop(drop, allowed));
    }

    @SubscribeEvent
    public static void onTameDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;

        if (!(event.getEntity() instanceof TamableAnimal tame)) return;
        if (!tame.isTame()) return;

        boolean diedInDuel = TameDuelManager.isTameInDuel(tame.getUUID()) || TameDuelManager.consumeRecentDuelElimination(tame.getUUID());
        TameData data = TameRegistry.get(tame.getUUID());
        if (data != null) {
            LevelSystem.storeHighestProgressSnapshot(data);
        }
        LevelSystem.onTameDeath(tame, !diedInDuel);
        if (data != null) {
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
            if (!diedInDuel) {
                String deathMessage = event.getSource().getLocalizedDeathMessage(tame).getString();
                TameDeathRecord deathRecord = TameDeathRecord.fromTame(data, tame, tame.level().getGameTime());
                TameRegistry.archiveDeath(deathRecord);
                notifyOwnerOfDeath(tame, data, deathMessage);
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
            }
            TameBedRegistrySync.syncFromEntity(tame, data);
            TameRegistry.markDirty();
        }
        tame.setTarget(null);
        tame.getNavigation().stop();
        if (!tame.isRemoved()) {
            tame.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        }
        if (!tame.isRemoved()) {
            tame.discard();
        }
        clearCapturedDeath(tame.getUUID());
        if (shouldInstantRebuildNearOwner(tame, data, diedInDuel) && tame.level().getServer() != null) {
            scheduleInstantRespawn(tame.getUUID(), 2, 20);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        serverTick++;
        if (PENDING_INSTANT_RESPAWNS.isEmpty()) {
            return;
        }
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, PendingInstantRespawn> entry : PENDING_INSTANT_RESPAWNS.entrySet()) {
            PendingInstantRespawn pending = entry.getValue();
            if (pending == null || pending.tameUuid() == null) {
                finished.add(entry.getKey());
                continue;
            }
            if (pending.dueTick() > serverTick) {
                continue;
            }
            TameData data = TameRegistry.get(pending.tameUuid());
            if (data == null || !data.dead) {
                finished.add(entry.getKey());
                continue;
            }
            boolean rebuilt = TameCommands.respawnDeadTameNextToOwner(event.getServer(), pending.tameUuid());
            if (rebuilt || pending.retriesRemaining() <= 0) {
                finished.add(entry.getKey());
                continue;
            }
            entry.setValue(new PendingInstantRespawn(pending.tameUuid(), serverTick + 2L, pending.retriesRemaining() - 1));
        }
        for (UUID tameUuid : finished) {
            PENDING_INSTANT_RESPAWNS.remove(tameUuid);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!TameDuelManager.isEntityInDuel(player.getUUID())) return;
        PendingDeath death = PendingDeath.capture(event);
        if (death == null) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, player.getMaxHealth()));
        player.removeAllEffects();
        player.setSecondsOnFire(0);
        player.setDeltaMovement(0.0D, 0.0D, 0.0D);
        player.invulnerableTime = Math.max(player.invulnerableTime, 20);
        player.hurtMarked = true;
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, true, true));
        MinecraftServer server = player.getServer();
        if (server != null) {
            TameDuelManager.recordElimination(
                    server,
                    player.getUUID(),
                    death.contributors(),
                    resolveDuelKillerParticipantUuid(server, death, death.killerTameUuid())
            );
            TameDuelManager.endDuelForEntity(server, player.getUUID());
        }
    }

    private static TamableAnimal resolveTameAttacker(LivingHurtEvent event) {
        return resolveTameAttacker(event.getSource());
    }

    private static UUID resolveKillerTameUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        TamableAnimal tame = resolveTameAttacker(event.getSource());
        return tame != null && tame.isTame() ? tame.getUUID() : null;
    }

    private static UUID resolveKillerParticipantUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        TamableAnimal tame = resolveTameAttacker(event.getSource());
        return tame != null && tame.isTame() ? tame.getUUID() : null;
    }

    private static TamableAnimal resolveTameAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        TamableAnimal tame = resolveTameFromEntity(source.getEntity());
        if (tame != null) {
            return tame;
        }
        return resolveTameFromEntity(source.getDirectEntity());
    }

    private static TamableAnimal resolveTameFromEntity(Entity entity) {
        if (entity instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        if (entity instanceof EvokerFangs fangs && fangs.getOwner() instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        if (entity instanceof Projectile projectile && projectile.getOwner() instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        if (entity instanceof OwnableEntity ownable && ownable.getOwner() instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        return null;
    }

    private static List<ItemStack> collectRetainedDeathItems(TamableAnimal tame) {
        List<ItemStack> allowed = new ArrayList<>();
        if (tame == null) {
            return allowed;
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = tame.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                allowed.add(stack.copy());
            }
        }
        if (tame instanceof Container container) {
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (!stack.isEmpty()) {
                    allowed.add(stack.copy());
                }
            }
        }
        return allowed;
    }

    private static boolean shouldKeepDrop(ItemEntity drop, List<ItemStack> allowed) {
        if (drop == null) {
            return false;
        }
        ItemStack stack = drop.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        for (int i = 0; i < allowed.size(); i++) {
            ItemStack candidate = allowed.get(i);
            if (!ItemStack.isSameItemSameTags(stack, candidate)) {
                continue;
            }
            if (candidate.getCount() < stack.getCount()) {
                continue;
            }
            candidate.shrink(stack.getCount());
            if (candidate.isEmpty()) {
                allowed.remove(i);
            }
            return true;
        }
        return false;
    }

    private static void notifyOwnerOfDeath(TamableAnimal tame, TameData data, String deathMessage) {
        if (!(tame.level() instanceof ServerLevel serverLevel) || data == null || data.ownerUUID == null) {
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUUID);
        if (owner == null) {
            return;
        }
        String tameName = data.name == null || data.name.isBlank() ? "Your tame" : data.name;
        String line = (deathMessage != null && !deathMessage.isBlank()) ? deathMessage : (tameName + " died.");
        owner.sendSystemMessage(
                Component.literal("[Tames] ").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal(line).withStyle(ChatFormatting.RED))
        );
    }

    private static boolean shouldInstantRebuildNearOwner(TamableAnimal tame, TameData data, boolean diedInDuel) {
        if (tame == null || data == null || diedInDuel || data.ownerUUID == null) {
            return false;
        }
        ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        String typeId = key == null ? tame.getType().toString() : key.toString();
        return "mutantmonsters:creeper_minion".equalsIgnoreCase(typeId)
                || "entity.mutantmonsters.creeper_minion".equalsIgnoreCase(typeId);
    }

    private static void scheduleInstantRespawn(UUID tameUuid, int delayTicks, int retries) {
        if (tameUuid == null) {
            return;
        }
        PENDING_INSTANT_RESPAWNS.put(tameUuid, new PendingInstantRespawn(
                tameUuid,
                serverTick + Math.max(1, delayTicks),
                Math.max(0, retries)
        ));
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
                        CAPTURED_DEATHS.remove(current.deadId());
                        current = PENDING_DEATHS.pollFirst();
                    }
                }
            }
        } finally {
            synchronized (DEATH_QUEUE_LOCK) {
                processingDeaths = false;
                PENDING_DEATHS.clear();
                ACTIVE_DEATHS.clear();
                CAPTURED_DEATHS.clear();
            }
        }
    }

    private static PendingDeath getCapturedDeath(UUID deadId) {
        if (deadId == null) {
            return null;
        }
        synchronized (DEATH_QUEUE_LOCK) {
            return CAPTURED_DEATHS.get(deadId);
        }
    }

    private static void clearCapturedDeath(UUID deadId) {
        if (deadId == null) {
            return;
        }
        synchronized (DEATH_QUEUE_LOCK) {
            CAPTURED_DEATHS.remove(deadId);
        }
    }

    private static void processDeath(PendingDeath death) {
        LivingEntity dead = death.dead();
        ServerLevel serverLevel = dead.level() instanceof ServerLevel level ? level : null;
        TamableAnimal effectiveKillerTame = resolveEffectiveKillerTame(serverLevel, death);
        UUID effectiveKillerTameUuid = effectiveKillerTame != null ? effectiveKillerTame.getUUID() : death.killerTameUuid();
        if (dead != null && TameDuelManager.isEntityInDuel(death.deadId())) {
            TameDuelManager.recordElimination(
                    dead.level().getServer(),
                    death.deadId(),
                    death.contributors(),
                    resolveDuelKillerParticipantUuid(dead.level().getServer(), death, effectiveKillerTameUuid)
            );
            TameDuelManager.endDuelForEntity(dead.level().getServer(), death.deadId());
        }
        LivingEntity killerForXp = effectiveKillerTame != null ? effectiveKillerTame : death.killer();
        LevelSystem.distributeXP(dead, killerForXp);
        if (serverLevel == null) {
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
            boolean wasKiller = effectiveKillerTameUuid != null && effectiveKillerTameUuid.equals(tameId);
            TameAbilityEvents.onKillOrAssist(tame, data, dead, wasKiller);
        }
        debugEnemyKilled(serverLevel, dead, dead.getExperienceReward(), death.contributors(), effectiveKillerTameUuid);
    }

    private static UUID resolveDuelKillerParticipantUuid(MinecraftServer server, PendingDeath death, UUID effectiveKillerTameUuid) {
        if (death == null) {
            return null;
        }
        if (death.killer() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return effectiveKillerTameUuid;
    }

    private static TamableAnimal resolveEffectiveKillerTame(ServerLevel level, PendingDeath death) {
        if (level == null || death == null) {
            return null;
        }
        if (death.killer() instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        if (death.killerTameUuid() != null && level.getEntity(death.killerTameUuid()) instanceof TamableAnimal tame && tame.isTame()) {
            return tame;
        }
        if (death.contributors() == null || death.contributors().isEmpty()) {
            return null;
        }
        TamableAnimal best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID tameId : death.contributors()) {
            if (!(level.getEntity(tameId) instanceof TamableAnimal contributor) || !contributor.isTame() || !contributor.isAlive()) {
                continue;
            }
            double dist = contributor.distanceToSqr(death.dead());
            if (dist < bestDist) {
                bestDist = dist;
                best = contributor;
            }
        }
        return best;
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

            TamableAnimal killerTame = resolveTameAttacker(event.getSource());
            if (killerTame != null) {
                killer = killerTame;
                killerTameUuid = killerTame.getUUID();
            } else if (event.getSource().getEntity() instanceof LivingEntity e) {
                killer = e;
            }
            if (killerTameUuid == null
                    && event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                    && ownable.getOwner() instanceof TamableAnimal tame
                    && tame.isTame()) {
                killer = tame;
                killerTameUuid = tame.getUUID();
            }

            Set<UUID> contributors = new HashSet<>(LevelSystem.mobDamageTracker.getOrDefault(dead.getUUID(), Set.of()));
            contributors.addAll(LevelSystem.mobOwnerDamageTracker.getOrDefault(dead.getUUID(), Set.of()));
            return new PendingDeath(
                    dead,
                    dead.getUUID(),
                    killer,
                    killerTameUuid,
                    contributors
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
            line.append("Killed: ")
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
