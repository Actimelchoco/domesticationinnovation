package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAutoFollowEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.mojang.datafixers.util.Pair;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WaystonesTeleportCompat {
    private static final Map<UUID, PendingWaystoneTeleport> PENDING_WAYSTONE_TELEPORTS = new HashMap<>();
    private static final Map<UUID, PendingOwnerResync> PENDING_OWNER_RESYNC = new HashMap<>();
    private static long serverTick = 0L;

    private record PendingWaystoneTeleport(
            UUID ownerUuid,
            String targetDimension,
            BlockPos targetBlock,
            float yRot,
            float xRot,
            long dueTick
    ) {
    }

    private record PendingOwnerResync(UUID ownerUuid, UUID tameUuid, String dimensionId, long dueTick) {
    }

    private WaystonesTeleportCompat() {
    }

    public static void init() {
        Balm.getEvents().onEvent(WaystoneTeleportEvent.Pre.class, WaystonesTeleportCompat::onWaystoneTeleportPre);
    }

    private static void onWaystoneTeleportPre(WaystoneTeleportEvent.Pre event) {
        if (event == null || event.getContext() == null) {
            return;
        }
        Entity entity = event.getContext().getEntity();
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel)) {
            return;
        }
        TeleportDestination destination = event.getContext().getDestination();
        if (destination == null || destination.getLevel() == null || destination.getLocation() == null) {
            return;
        }
        Direction direction = destination.getDirection();
        float yRot = direction == null ? player.getYRot() : direction.toYRot();
        float xRot = player.getXRot();
        BlockPos targetBlock = BlockPos.containing(destination.getLocation());
        TameAutoFollowEvents.suppressOwnerTeleportFollow(player.getUUID(), 40);
        PENDING_WAYSTONE_TELEPORTS.put(player.getUUID(), new PendingWaystoneTeleport(
                player.getUUID(),
                destination.getLevel().dimension().location().toString(),
                targetBlock,
                yRot,
                xRot,
                serverTick + 1L
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer() == null) {
            return;
        }
        serverTick++;
        if (!PENDING_WAYSTONE_TELEPORTS.isEmpty()) {
            PENDING_WAYSTONE_TELEPORTS.entrySet().removeIf(entry -> processWaystoneTeleport(event, entry.getValue()));
        }
        if (!PENDING_OWNER_RESYNC.isEmpty()) {
            PENDING_OWNER_RESYNC.entrySet().removeIf(entry -> processOwnerResync(event, entry.getValue()));
        }
    }

    private static boolean processWaystoneTeleport(TickEvent.ServerTickEvent event, PendingWaystoneTeleport pending) {
        if (pending == null || pending.dueTick() > serverTick) {
            return false;
        }
        ServerPlayer owner = event.getServer().getPlayerList().getPlayer(pending.ownerUuid());
        if (owner == null) {
            return true;
        }
        ServerLevel targetLevel = resolveTargetLevel(event, pending.targetDimension());
        if (targetLevel == null) {
            return true;
        }

        UUID ownerId = owner.getUUID();
        Vec3 targetPos = centeredTargetPos(pending.targetBlock());
        for (TameData data : TameRegistry.getOwned(ownerId)) {
            LivingEntity loaded = findLoadedOwnedTame(owner, data.uuid);
            if (loaded != null && loaded.isAlive()) {
                if (loaded instanceof net.minecraft.world.entity.TamableAnimal tamable) TameCommands.syncLiveMovementStateFor(tamable);
                if (!isWaystoneTeleportEligibleLoaded(loaded, data, ownerId)) {
                    continue;
                }
                LivingEntity moved = teleportWaystoneStyle(owner, loaded, data, targetLevel, pending.targetBlock(), pending.yRot(), pending.xRot());
                if (moved != null) {
                    TameCommands.queueClientReloadForTame(moved);
                }
                continue;
            }
            if (!isWaystoneTeleportEligibleFromData(data, ownerId)) continue;
            if (!TameCommands.hasPendingImmediateChunkTeleport(data)) {
                TameCommands.autoFollowTeleportViaTpPath(owner, data, targetLevel, targetPos, pending.yRot(), pending.xRot());
            }
        }
        return true;
    }

    private static LivingEntity teleportWaystoneStyle(ServerPlayer owner, LivingEntity tame, TameData data, ServerLevel targetLevel, BlockPos targetBlock, float yRot, float xRot) {
        if (owner == null || tame == null || data == null || targetLevel == null || targetBlock == null) {
            return null;
        }
        Vec3 basePos = centeredTargetPos(targetBlock);
        if (tame.level().dimension().equals(targetLevel.dimension())) {
            double offsetX = (owner.getRandom().nextDouble() - 0.5D) * 2.0D;
            double offsetZ = (owner.getRandom().nextDouble() - 0.5D) * 2.0D;
            tame.teleportTo(basePos.x + offsetX, basePos.y, basePos.z + offsetZ);
            tame.setYRot(yRot);
            tame.setXRot(xRot);
            refreshData(data, tame, targetLevel);
            TameCommands.queueClientReloadForTame(tame);
            return tame;
        }

        Vec3 targetPos = new Vec3(
                basePos.x + (owner.getRandom().nextDouble() - 0.5D) * 2.0D,
                basePos.y,
                basePos.z + (owner.getRandom().nextDouble() - 0.5D) * 2.0D
        );
        TameAutoFollowEvents.scheduleDimensionTpFollow(owner, targetLevel, targetPos, yRot, xRot, 1);
        return null;
    }

    private static boolean processOwnerResync(TickEvent.ServerTickEvent event, PendingOwnerResync pending) {
        if (pending == null || pending.dueTick() > serverTick) {
            return false;
        }
        ServerPlayer owner = event.getServer().getPlayerList().getPlayer(pending.ownerUuid());
        if (owner == null) {
            return true;
        }
        ServerLevel level = resolveTargetLevel(event, pending.dimensionId());
        if (level == null || owner.level() != level) {
            return true;
        }
        Entity entity = level.getEntity(pending.tameUuid());
        if (entity instanceof LivingEntity tame) {
            resendEntityToOwner(owner, tame);
        }
        return true;
    }

    private static void queueOwnerResync(ServerPlayer owner, LivingEntity tame, ServerLevel level) {
        if (owner == null || tame == null || level == null) {
            return;
        }
        PENDING_OWNER_RESYNC.put(tame.getUUID(), new PendingOwnerResync(
                owner.getUUID(),
                tame.getUUID(),
                level.dimension().location().toString(),
                serverTick + 1L
        ));
    }

    private static void resendEntityToOwner(ServerPlayer owner, LivingEntity tame) {
        if (owner == null || tame == null || owner.connection == null) {
            return;
        }
        if (tame.level() instanceof ServerLevel level) {
            List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values = tame.getEntityData().getNonDefaultValues();
            for (ServerPlayer viewer : level.players()) {
                if (viewer == null || viewer.connection == null || viewer.isRemoved()
                        || viewer.distanceToSqr(tame) > 192.0D * 192.0D) continue;
                sendClientPacket(viewer, new net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket(tame.getId()));
                sendClientPacket(viewer, tame.getAddEntityPacket());
                if (values != null && !values.isEmpty()) {
                    sendClientPacket(viewer, new ClientboundSetEntityDataPacket(tame.getId(), values));
                }
                sendClientPacket(viewer, new ClientboundTeleportEntityPacket(tame));
            }
        }
    }

    private static void sendClientPacket(ServerPlayer player, Packet<?> packet) {
        if (player == null || packet == null || player.connection == null) {
            return;
        }
        player.connection.send(packet);
    }

    private static boolean isWaystoneTeleportEligibleFromData(TameData data, UUID ownerId) {
        if (data == null || data.isInactive() || data.uuid == null) {
            return false;
        }
        if (!ownerId.equals(data.ownerUUID) || data.hasHome || data.wanderLock) {
            return false;
        }
        if (TameDuelManager.isTameInDuel(data.uuid)) {
            return false;
        }
        if (LevelSystem.getAttributeLevel(data, "tethered_teleport") <= 0) {
            return false;
        }
        return TameAutoFollowEvents.isFollowing(data);
    }

    private static boolean isWaystoneTeleportEligibleLoaded(LivingEntity tame, TameData data, UUID ownerId) {
        if (tame == null || data == null || data.isInactive() || data.uuid == null) {
            return false;
        }
        if (!ownerId.equals(data.ownerUUID) || data.hasHome || data.wanderLock) {
            return false;
        }
        if (TameDuelManager.isTameInDuel(data.uuid)) {
            return false;
        }
        if (LevelSystem.getAttributeLevel(data, "tethered_teleport") <= 0) {
            return false;
        }
        // Modded tames do not all expose TL's movement mode through their live tame API.
        // Require the registry state as well so "sit", "wander", and "guardian" cannot
        // be mistaken for follow merely because an entity reports that it is not sitting.
        return TameAutoFollowEvents.isFollowing(data) && TameEntityAdapter.isFollowingOwner(tame);
    }

    private static LivingEntity findLoadedOwnedTame(ServerPlayer owner, UUID tameUuid) {
        if (owner == null || owner.server == null || tameUuid == null) {
            return null;
        }
        for (ServerLevel level : owner.server.getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof LivingEntity tame) || !TameEntityAdapter.isTame(tame)) continue;
            if (!owner.getUUID().equals(TameEntityAdapter.ownerUuid(tame))) continue;
            return tame;
        }
        return null;
    }

    private static void refreshData(TameData data, LivingEntity tame, ServerLevel level) {
        if (data == null || tame == null || level == null) {
            return;
        }
        TameRegistry.bindEntityToData(tame, data);
        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = level.getGameTime();
        net.minecraft.nbt.CompoundTag refreshedSnapshot = new net.minecraft.nbt.CompoundTag();
        tame.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
        TameRegistry.markDirty();
    }

    private static ServerLevel resolveTargetLevel(TickEvent.ServerTickEvent event, String dimensionId) {
        if (event == null || event.getServer() == null || dimensionId == null || dimensionId.isBlank()) {
            return null;
        }
        for (ServerLevel candidate : event.getServer().getAllLevels()) {
            if (candidate.dimension().location().toString().equals(dimensionId)) {
                return candidate;
            }
        }
        return null;
    }

    private static Vec3 centeredTargetPos(BlockPos targetBlock) {
        return new Vec3(targetBlock.getX() + 0.5D, targetBlock.getY(), targetBlock.getZ() + 0.5D);
    }
}
