package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Transient player-duel waiting state; no saved tame orders or assignments are changed. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class DuelFollowerPause {
    private static final String KEY = "tl_duel_follower_wait";
    private static final Set<UUID> PLAYERS = new HashSet<>();
    private DuelFollowerPause() { }

    private static UUID waitingPlayer(TameData data) {
        if (PLAYERS.isEmpty() || data == null || data.dead || data.stored
                || TameDuelManager.isTameInDuel(data.uuid)) return null;
        UUID protectedPlayer = GuardianChestBodyguards.protectedPlayerId(data);
        if (protectedPlayer != null) return PLAYERS.contains(protectedPlayer) ? protectedPlayer : null;
        if (data.hasHome && !data.rosterBodyguard) return null;
        if (data.movementOrder != 0 && !data.rosterBodyguard && TameMode.byId(data.mode) != TameMode.BODYGUARD) return null;
        return PLAYERS.contains(data.ownerUUID) ? data.ownerUUID : null;
    }

    public static boolean isPaused(Entity entity) {
        return entity != null && !entity.level().isClientSide && entity.getPersistentData().contains(KEY);
    }

    public static void discardCopiedPause(Entity entity) { entity.getPersistentData().remove(KEY); }

    public static boolean shouldWait(TameData data) { return waitingPlayer(data) != null; }

    public static void begin(MinecraftServer server, Set<UUID> roster) {
        boolean changed = false;
        for (UUID id : roster) if (server.getPlayerList().getPlayer(id) != null) changed |= PLAYERS.add(id);
        if (changed) refreshLoaded(server);
    }

    public static void end(MinecraftServer server, Set<UUID> roster) {
        if (PLAYERS.removeAll(roster)) refreshLoaded(server);
    }

    private static void refreshLoaded(MinecraftServer server) {
        for (var level : server.getAllLevels()) for (LivingEntity entity : LoadedTameIndex.snapshotForLevel(level)) {
            TameData data = TameRegistry.get(entity.getUUID());
            if (data != null) tick(entity, data);
        }
    }

    /** Runs in the existing tame callback; idle cost is a set/marker check. */
    public static boolean tick(LivingEntity entity, TameData data) {
        if (!(entity instanceof Mob mob)) return false;
        UUID player = waitingPlayer(data);
        if (player == null) {
            restore(mob);
            return false;
        }
        if (!mob.getPersistentData().contains(KEY)) {
            CompoundTag saved = new CompoundTag();
            saved.putUUID("player", player);
            saved.putBoolean("noAi", mob.isNoAi());
            if (mob instanceof TamableAnimal pet) {
                saved.putBoolean("orderedSit", pet.isOrderedToSit());
                saved.putBoolean("sittingPose", pet.isInSittingPose());
            }
            Integer command = command(mob);
            if (command != null) saved.putInt("command", command);
            mob.getPersistentData().put(KEY, saved);
            if (command != null) setCommand(mob, 1);
            if (mob instanceof TamableAnimal pet) {
                pet.setOrderedToSit(true);
                pet.setInSittingPose(true);
            }
            stop(mob);
            mob.setNoAi(true);
        } else if ((mob.tickCount + mob.getId()) % 10 == 0) {
            stop(mob);
            mob.setNoAi(true);
            if (mob instanceof TamableAnimal pet) {
                if (!pet.isOrderedToSit()) pet.setOrderedToSit(true);
                if (!pet.isInSittingPose()) pet.setInSittingPose(true);
            }
        }
        return true;
    }

    private static void stop(Mob mob) {
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.getNavigation().stop();
        mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
    }

    private static Integer command(Mob mob) {
        if (mob instanceof IComandableMob commanded) return commanded.getCommand();
        try { return ((Number) mob.getClass().getMethod("getCommand").invoke(mob)).intValue(); }
        catch (ReflectiveOperationException | ClassCastException ignored) { return null; }
    }

    private static void setCommand(Mob mob, int command) {
        if (mob instanceof IComandableMob commanded) { commanded.setCommand(command); return; }
        try { mob.getClass().getMethod("setCommand", int.class).invoke(mob, command); }
        catch (ReflectiveOperationException ignored) { }
    }

    private static void restore(Mob mob) {
        if (!mob.getPersistentData().contains(KEY)) return;
        CompoundTag saved = mob.getPersistentData().getCompound(KEY);
        mob.getPersistentData().remove(KEY);
        if (saved.contains("command")) setCommand(mob, saved.getInt("command"));
        if (mob instanceof TamableAnimal pet) {
            pet.setOrderedToSit(saved.getBoolean("orderedSit"));
            pet.setInSittingPose(saved.getBoolean("sittingPose"));
        }
        mob.setNoAi(saved.getBoolean("noAi"));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void joined(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof Mob mob)
                || !mob.getPersistentData().contains(KEY)) return;
        CompoundTag saved = mob.getPersistentData().getCompound(KEY);
        if (!saved.hasUUID("player") || !PLAYERS.contains(saved.getUUID("player"))
                || TameDuelManager.isTameInDuel(mob.getUUID())) restore(mob);
    }

    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        PLAYERS.clear();
        refreshLoaded(event.getServer());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PLAYERS.clear(); }
}
