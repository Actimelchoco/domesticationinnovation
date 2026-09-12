package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BlessfulledCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameBedRegistrySync;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDeathRecord;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.LivingEntity;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;

public class TameCombatEvents {
    public static final String DEATH_REMOVAL_TAG = "TamesLevelDeathRemoval";

    private record PendingInstantRespawn(UUID tameUuid, long dueTick, int retriesRemaining) {
    }

    private static final Object DEATH_QUEUE_LOCK = new Object();
    private static final Deque<PendingDeath> PENDING_DEATHS = new ArrayDeque<>();
    private static final Set<UUID> ACTIVE_DEATHS = new HashSet<>();
    private static final Map<UUID, PendingDeath> CAPTURED_DEATHS = new HashMap<>();
    private static final Map<UUID, PendingInstantRespawn> PENDING_INSTANT_RESPAWNS = new HashMap<>();
    private static final Map<UUID, Integer> DUEL_VALLUMRAPTOR_INVISIBILITY_COUNTS = new HashMap<>();
    private static final Set<UUID> DUEL_VALLUMRAPTORS_CURRENTLY_INVISIBLE = new HashSet<>();
    private static boolean processingDeaths = false;
    private static long serverTick = 0L;

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity mob = event.getEntity();
        if (event.getAmount() <= 0.0F) return;
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport.setAssasinTarget(player, mob);
        }
        UUID victimParticipantId = resolveDuelParticipantUuid(mob);
        UUID attackerParticipantId = resolveDuelParticipantUuid(event.getSource());
        if (attackerParticipantId != null && victimParticipantId != null) {
            if (TameDuelManager.isSameDuelTeam(attackerParticipantId, victimParticipantId)) {
                event.setCanceled(true);
                return;
            }
            boolean attackerInDuel = TameDuelManager.isEntityInDuel(attackerParticipantId);
            boolean victimInDuel = TameDuelManager.isEntityInDuel(victimParticipantId);
            if ((attackerInDuel || victimInDuel) && !TameDuelManager.areDuelOpponents(attackerParticipantId, victimParticipantId)) {
                event.setCanceled(true);
                return;
            }
        }

        LivingEntity tame = resolveTameAttacker(event);
        if (tame != null && TameEntityAdapter.isTame(tame)) {
            LevelSystem.trackDamage(mob, tame);
            boolean duelPink = TameDuelManager.isTameInDuel(tame.getUUID()) && !TameDuelManager.isTeamAEntity(tame.getUUID());
            BlessfulledCompat.showTameDealtDamagePopup(tame, mob, event.getAmount(), duelPink);
        } else if (TameEntityAdapter.isTame(mob)) {
            BlessfulledCompat.showTameReceivedDamagePopup(event.getSource().getEntity(), mob, event.getAmount());
        }
        if (tame == null && event.getSource().getEntity() instanceof LivingEntity modifiedAttacker
                && modifiedAttacker instanceof ModifedToBeTameable modified
                && TameEntityAdapter.isTame(modifiedAttacker)
                && (!(modifiedAttacker instanceof AbstractHorse horse)
                    || horse.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player)
                && TameRegistry.get(modifiedAttacker.getUUID()) != null) {
            LevelSystem.trackDamage(mob, modifiedAttacker);
        }

        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            LevelSystem.trackOwnerDamage(mob, player);
        }
    }

    @SubscribeEvent
    public static void onAssasinClearTarget(PlayerInteractEvent.RightClickItem event) {
        clearAssasinTarget(event);
    }

    @SubscribeEvent
    public static void onAssasinClearTargetBlock(PlayerInteractEvent.RightClickBlock event) {
        clearAssasinTarget(event);
    }

    private static void clearAssasinTarget(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()) return;
        if (!(event.getItemStack().getItem() instanceof SwordItem)) return;
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport.clearAssasinTarget(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Assasin target cleared.").withStyle(ChatFormatting.GRAY));
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
        if (event == null || !TameEntityAdapter.isTame(event.getEntity())) {
            return;
        }
        LivingEntity tame = event.getEntity();
        removeAnimightEquipmentDrops(tame, event.getDrops());
        List<ItemStack> allowed = collectRetainedDeathItems(tame);
        event.getDrops().removeIf(drop -> !shouldKeepDrop(drop, allowed));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTameFoodAutopickupDrops(LivingDropsEvent event) {
        if (event == null || event.isCanceled() || event.getDrops().isEmpty()) {
            return;
        }
        LivingEntity killerTame = resolveTameAttacker(event.getSource());
        if (killerTame == null || !TameEntityAdapter.isTame(killerTame) || !killerTame.isAlive()) {
            return;
        }
        TameData data = TameRegistry.get(killerTame.getUUID());
        if (data == null) {
            data = TameRegistry.getByTlId(TameData.getTlId(killerTame));
        }
        if (data == null || data.dead || data.stored || !data.hungerAutopickup) {
            return;
        }
        TameData finalData = data;
        event.getDrops().removeIf(drop -> {
            if (drop == null) {
                return false;
            }
            ItemStack stack = drop.getItem();
            int moved = TameCommands.storeHungerFoodFromDrop(killerTame, finalData, stack);
            if (moved <= 0) {
                return false;
            }
            if (!stack.isEmpty()) {
                drop.setItem(stack);
                return false;
            }
            return true;
        });
    }

    // Clean up the original dead entity before the normal-priority elimination listener can
    // synchronously finish the duel and rebuild it. Running this at LOWEST meant the cleanup
    // acted on the registry after restoration, invalidating the replacement's ranked identity.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onTameDeath(LivingDeathEvent event) {
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat.isTemporary(event.getEntity())) return;
        if (event.isCanceled()) return;

        LivingEntity tame = event.getEntity();
        UUID tlId = TameData.getTlId(tame);
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null && tlId != null) {
            data = TameRegistry.getByTlId(tlId);
        }
        // Some interface-based tames clear or temporarily stop reporting their tame
        // state while entering death. Their persistent TL identity is authoritative.
        if (!TameEntityAdapter.isTame(tame)
                && (!TameEntityAdapter.isSupported(tame) || data == null)) {
            return;
        }

        boolean diedInDuel = TameDuelManager.isTameInDuel(tame.getUUID())
                || (tlId != null && TameDuelManager.isTameInDuel(tlId))
                || TameDuelManager.consumeRecentDuelElimination(tame.getUUID())
                || (tlId != null && TameDuelManager.consumeRecentDuelElimination(tlId));
        if (!diedInDuel && tame instanceof Rabbit && data != null && data.activeSurvivalDays >= 18) {
            tame.spawnAtLocation(new ItemStack(DIItemRegistry.SINISTER_CARROT.get()));
        }
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
            if (diedInDuel) {
                data.dead = false;
                data.deadGameTime = 0L;
                data.deadUnixMillis = 0L;
                data.deathDimension = "";
                data.deathX = 0;
                data.deathY = 0;
                data.deathZ = 0;
            } else {
                data.dead = true;
                data.deadGameTime = tame.level().getGameTime();
                data.deadUnixMillis = System.currentTimeMillis();
                data.deathDimension = tame.level().dimension().location().toString();
                data.deathX = tame.blockPosition().getX();
                data.deathY = tame.blockPosition().getY();
                data.deathZ = tame.blockPosition().getZ();
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
                data.adminSurvivalStart = Long.MIN_VALUE;
                while (data.deathHistory.size() > 64) {
                    data.deathHistory.remove(0);
                }
            }
            if (tame instanceof net.minecraft.world.entity.TamableAnimal tamable) TameBedRegistrySync.syncFromEntity(tamable, data);
            TameRegistry.markDirty();
        }
        if (tame instanceof net.minecraft.world.entity.Mob mob) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        tame.getPersistentData().putBoolean(DEATH_REMOVAL_TAG, true);
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

    /** Some modded attacks remove their victim without firing LivingDeathEvent. */
    @SubscribeEvent
    public static void onRemovedTame(net.minecraftforge.event.entity.EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof LivingEntity tame) || tame.level().isClientSide) return;
        Entity.RemovalReason reason = tame.getRemovalReason();
        if (reason != Entity.RemovalReason.KILLED
                && !(reason == Entity.RemovalReason.DISCARDED && tame.getHealth() <= 0)) return;
        if (tame.getPersistentData().getBoolean(DEATH_REMOVAL_TAG)
                || tame.getPersistentData().getBoolean(TameCommands.ADMIN_CLONE_SILENT_TAG)) return;
        TameData data = TameRegistry.get(tame.getUUID());
        UUID tlId = TameData.getTlId(tame);
        if (data == null && tlId != null) data = TameRegistry.getByTlId(tlId);
        // Never let removal of an obsolete entity invalidate its replacement.
        if (data == null || data.dead || !tame.getUUID().equals(data.uuid)) return;
        // Duels own elimination/restoration, including removals during cleanup.
        if (TameDuelManager.isTameInDuel(tame.getUUID())
                || (tlId != null && TameDuelManager.isTameInDuel(tlId))) return;
        DamageSource source = tame.getLastDamageSource();
        if (source == null) source = tame.damageSources().generic();
        // Call just our registry handler; reposting a death event would rerun other mods' drops/rewards.
        onTameDeath(new LivingDeathEvent(tame, source));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        serverTick++;
        processDuelVallumraptorInvisibility(event.getServer());
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

    private static void processDuelVallumraptorInvisibility(MinecraftServer server) {
        if (server == null) {
            return;
        }
        Set<UUID> seenDuelRaptors = new HashSet<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (!(entity instanceof LivingEntity tame)) {
                    continue;
                }
                if (!isAlexsCavesVallumraptor(tame) || !tame.isAlive() || !TameDuelManager.isTameInDuel(tame.getUUID())) {
                    continue;
                }
                UUID tameId = tame.getUUID();
                seenDuelRaptors.add(tameId);
                boolean invisible = isVallumraptorHiding(tame);
                boolean wasInvisible = DUEL_VALLUMRAPTORS_CURRENTLY_INVISIBLE.contains(tameId);
                if (invisible && !wasInvisible) {
                    int count = DUEL_VALLUMRAPTOR_INVISIBILITY_COUNTS.getOrDefault(tameId, 0) + 1;
                    DUEL_VALLUMRAPTOR_INVISIBILITY_COUNTS.put(tameId, count);
                    DUEL_VALLUMRAPTORS_CURRENTLY_INVISIBLE.add(tameId);
                    if (count % 3 == 0) {
                        killDuelVallumraptorForInvisibility(tame);
                    }
                } else if (!invisible && wasInvisible) {
                    DUEL_VALLUMRAPTORS_CURRENTLY_INVISIBLE.remove(tameId);
                }
            }
        }
        DUEL_VALLUMRAPTOR_INVISIBILITY_COUNTS.keySet().removeIf(id -> !seenDuelRaptors.contains(id) && !TameDuelManager.isTameInDuel(id));
        DUEL_VALLUMRAPTORS_CURRENTLY_INVISIBLE.removeIf(id -> !seenDuelRaptors.contains(id) && !TameDuelManager.isTameInDuel(id));
    }

    private static void killDuelVallumraptorForInvisibility(LivingEntity tame) {
        if (tame == null || tame.level().isClientSide || !tame.isAlive()) {
            return;
        }
        tame.hurt(tame.damageSources().magic(), Math.max(1000.0F, tame.getMaxHealth() * 10.0F));
    }

    private static boolean isAlexsCavesVallumraptor(LivingEntity tame) {
        if (tame == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return id != null && "alexscaves".equals(id.getNamespace()) && "vallumraptor".equals(id.getPath());
    }

    private static boolean isVallumraptorHiding(LivingEntity tame) {
        if (tame == null) {
            return false;
        }
        try {
            Method method = tame.getClass().getMethod("getHideFor");
            Object value = method.invoke(tame);
            if (value instanceof Number number) {
                return number.intValue() > 0;
            }
        } catch (Throwable ignored) {
        }
        return tame.isInvisible();
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

    private static LivingEntity resolveTameAttacker(LivingHurtEvent event) {
        return resolveTameAttacker(event.getSource());
    }

    private static UUID resolveKillerTameUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        LivingEntity tame = resolveTameAttacker(event.getSource());
        return tame != null && TameEntityAdapter.isTame(tame) ? tame.getUUID() : null;
    }

    private static UUID resolveKillerParticipantUuid(LivingDeathEvent event) {
        if (event == null || event.getSource() == null) {
            return null;
        }
        ServerPlayer player = resolvePlayerAttacker(event.getSource());
        if (player != null) {
            return player.getUUID();
        }
        LivingEntity tame = resolveTameAttacker(event.getSource());
        return tame != null && TameEntityAdapter.isTame(tame) ? tame.getUUID() : null;
    }

    private static LivingEntity resolveTameAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        LivingEntity tame = resolveTameFromEntity(source.getEntity());
        if (tame != null) {
            return tame;
        }
        return resolveTameFromEntity(source.getDirectEntity());
    }

    private static LivingEntity resolveTameFromEntity(Entity entity) {
        if (entity instanceof LivingEntity living && TameEntityAdapter.isTame(living)) {
            return living;
        }
        if (entity instanceof EvokerFangs fangs && TameEntityAdapter.isTame(TameEntityAdapter.owner(fangs))) {
            return TameEntityAdapter.owner(fangs);
        }
        if (entity instanceof Projectile projectile && TameEntityAdapter.isTame(TameEntityAdapter.owner(projectile))) {
            return TameEntityAdapter.owner(projectile);
        }
        if (entity instanceof OwnableEntity ownable && TameEntityAdapter.isTame(ownable.getOwner())) {
            return ownable.getOwner();
        }
        return null;
    }

    private static UUID resolveDuelParticipantUuid(DamageSource source) {
        if (source == null) {
            return null;
        }
        ServerPlayer player = resolvePlayerAttacker(source);
        if (player != null) {
            return player.getUUID();
        }
        LivingEntity tame = resolveTameAttacker(source);
        return resolveDuelParticipantUuid(tame);
    }

    private static ServerPlayer resolvePlayerAttacker(DamageSource source) {
        if (source == null) {
            return null;
        }
        if (source.getEntity() instanceof ServerPlayer player) {
            return player;
        }
        if (source.getDirectEntity() instanceof Projectile projectile && TameEntityAdapter.owner(projectile) instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static UUID resolveDuelParticipantUuid(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            return player.getUUID();
        }
        if (TameEntityAdapter.isTame(entity)) {
            UUID tlId = TameData.getTlId(entity);
            if (tlId != null && TameDuelManager.isEntityInDuel(tlId)) {
                return tlId;
            }
            return entity.getUUID();
        }
        return null;
    }

    private static List<ItemStack> collectRetainedDeathItems(LivingEntity tame) {
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

    /**
     * Animights normally ejects and clears every equipped item while a companion dies.
     * Tames Level restores a dead tame from the pre-drop entity snapshot, so letting any
     * equipped item also enter the world would duplicate it. Retain all hand and armor
     * equipment for every Animight in that snapshot instead.
     */
    private static void removeAnimightEquipmentDrops(LivingEntity tame, java.util.Collection<ItemEntity> drops) {
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (typeId == null || !"animights".equals(typeId.getNamespace())) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            UUID tlId = TameData.getTlId(tame);
            data = tlId == null ? null : TameRegistry.getByTlId(tlId);
        }
        CompoundTag snapshot = data == null ? null : data.entitySnapshot;
        if (snapshot == null) {
            return;
        }
        removeSnapshotEquipmentDrops(snapshot, "HandItems", drops);
        removeSnapshotEquipmentDrops(snapshot, "ArmorItems", drops);
    }

    private static void removeSnapshotEquipmentDrops(CompoundTag snapshot, String nbtKey,
                                                     java.util.Collection<ItemEntity> drops) {
        if (!snapshot.contains(nbtKey, Tag.TAG_LIST)) {
            return;
        }
        ListTag equipment = snapshot.getList(nbtKey, Tag.TAG_COMPOUND);
        for (int slot = 0; slot < equipment.size(); slot++) {
            removeMatchingDrop(ItemStack.of(equipment.getCompound(slot)), drops);
        }
    }

    private static void removeMatchingDrop(ItemStack retained, java.util.Collection<ItemEntity> drops) {
        if (retained.isEmpty()) return;
        int remaining = retained.getCount();
        var iterator = drops.iterator();
        while (iterator.hasNext() && remaining > 0) {
            ItemEntity drop = iterator.next();
            ItemStack dropped = drop.getItem();
            if (!ItemStack.isSameItemSameTags(retained, dropped)) {
                continue;
            }
            if (dropped.getCount() <= remaining) {
                remaining -= dropped.getCount();
                iterator.remove();
            } else {
                dropped.shrink(remaining);
                remaining = 0;
            }
        }
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

    private static void notifyOwnerOfDeath(LivingEntity tame, TameData data, String deathMessage) {
        if (!(tame.level() instanceof ServerLevel serverLevel) || data == null || data.ownerUUID == null) {
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(data.ownerUUID);
        if (owner == null) {
            return;
        }
        if (!PlayerDebugSettings.combatDeath(owner.getUUID())) {
            return;
        }
        String tameName = data.name == null || data.name.isBlank() ? "Your tame" : data.name;
        String line = (deathMessage != null && !deathMessage.isBlank()) ? deathMessage : (tameName + " died.");
        owner.sendSystemMessage(
                Component.literal("[Tames] ").withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal(line).withStyle(ChatFormatting.RED))
        );
    }

    private static boolean shouldInstantRebuildNearOwner(LivingEntity tame, TameData data, boolean diedInDuel) {
        return false;
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
        LivingEntity effectiveKillerTame = resolveEffectiveKillerTame(serverLevel, death);
        UUID effectiveKillerTameUuid = effectiveKillerTame != null ? effectiveKillerTame.getUUID() : death.killerTameUuid();
        UUID deadParticipantId = death.duelParticipantId();
        if (dead != null && deadParticipantId != null && TameDuelManager.isEntityInDuel(deadParticipantId)) {
            TameDuelManager.recordElimination(
                    dead.level().getServer(),
                    deadParticipantId,
                    death.contributors(),
                    resolveDuelKillerParticipantUuid(dead.level().getServer(), death, effectiveKillerTameUuid)
            );
            TameDuelManager.endDuelForEntity(dead.level().getServer(), deadParticipantId);
        }
        LivingEntity killerForXp = effectiveKillerTame != null ? effectiveKillerTame : death.killer();
        LevelSystem.distributeXP(dead, killerForXp);
        if (serverLevel == null) {
            return;
        }
        for (UUID tameId : death.contributors()) {
            if (!(serverLevel.getEntity(tameId) instanceof LivingEntity tame) || !TameEntityAdapter.isTame(tame)) {
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
        if (death.lastTameDamagerUuid() != null) {
            return death.lastTameDamagerUuid();
        }
        if (death.killer() instanceof ServerPlayer player) {
            return player.getUUID();
        }
        return effectiveKillerTameUuid;
    }

    private static LivingEntity resolveEffectiveKillerTame(ServerLevel level, PendingDeath death) {
        if (level == null || death == null) {
            return null;
        }
        if (TameEntityAdapter.isTame(death.killer())) {
            return death.killer();
        }
        if (death.killerTameUuid() != null && level.getEntity(death.killerTameUuid()) instanceof LivingEntity tame && TameEntityAdapter.isTame(tame)) {
            return tame;
        }
        if (death.contributors() == null || death.contributors().isEmpty()) {
            return null;
        }
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID tameId : death.contributors()) {
            if (!(level.getEntity(tameId) instanceof LivingEntity contributor) || !TameEntityAdapter.isTame(contributor) || !contributor.isAlive()) {
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

    private record PendingDeath(LivingEntity dead, UUID deadId, UUID duelParticipantId, LivingEntity killer, UUID killerTameUuid,
                                UUID lastTameDamagerUuid, Set<UUID> contributors) {
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

            LivingEntity killerTame = resolveTameAttacker(event.getSource());
            if (killerTame != null) {
                killer = killerTame;
                killerTameUuid = killerTame.getUUID();
            } else if (event.getSource().getEntity() instanceof LivingEntity e) {
                killer = e;
            }
            if (killerTameUuid == null
                    && event.getSource().getDirectEntity() instanceof OwnableEntity ownable
                    && TameEntityAdapter.isTame(ownable.getOwner())) {
                killer = ownable.getOwner();
                killerTameUuid = killer.getUUID();
            }

            Set<UUID> contributors = new HashSet<>(LevelSystem.mobDamageTracker.getOrDefault(dead.getUUID(), Set.of()));
            contributors.addAll(LevelSystem.mobOwnerDamageTracker.getOrDefault(dead.getUUID(), Set.of()));
            return new PendingDeath(
                    dead,
                    dead.getUUID(),
                    resolveDuelParticipantUuid(dead),
                    killer,
                    killerTameUuid,
                    LevelSystem.lastTameDamager(dead.getUUID()),
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
            boolean showKills = PlayerDebugSettings.combatKills(ownerId);
            boolean showAssists = PlayerDebugSettings.combatAssists(ownerId);
            if (!showKills && !showAssists) continue;
            ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
            if (owner == null) continue;

            String killerName = killerByOwner.getOrDefault(ownerId, "-");
            List<String> assisters = assistsByOwner.getOrDefault(ownerId, List.of());
            StringBuilder line = new StringBuilder();
            line.append("Killed: ")
                    .append(mobType)
                    .append("[")
                    .append(xpReward)
                    .append("]: ");
            boolean wrotePart = false;
            if (showKills) {
                line.append("K: ").append(killerName);
                wrotePart = true;
            }
            if (showAssists) {
                for (String assister : assisters) {
                    if (wrotePart) {
                        line.append(", ");
                    }
                    line.append("A: ").append(assister);
                    wrotePart = true;
                }
            }
            if (!wrotePart) {
                continue;
            }
            owner.sendSystemMessage(Component.literal(line.toString()).withStyle(ChatFormatting.GRAY));
        }
    }
}
