package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAutoFollowEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.waystones.api.TeleportDestination;
import net.blay09.mods.waystones.api.WaystoneTeleportEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
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
    private static final Map<UUID, PendingOwnerResync> PENDING_OWNER_RESYNC = new HashMap<>();
    private static long serverTick = 0L;

    private static final class PendingOwnerResync {
        private final UUID ownerUuid;
        private final UUID tameUuid;
        private final ResourceKeyWrapper dimension;
        private final long dueTick;

        private PendingOwnerResync(UUID ownerUuid, UUID tameUuid, ResourceKeyWrapper dimension, long dueTick) {
            this.ownerUuid = ownerUuid;
            this.tameUuid = tameUuid;
            this.dimension = dimension;
            this.dueTick = dueTick;
        }
    }

    private record ResourceKeyWrapper(String id) {
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
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel sourceLevel)) {
            return;
        }
        TeleportDestination destination = event.getContext().getDestination();
        if (destination == null || destination.getLevel() == null || destination.getLocation() == null) {
            return;
        }
        ServerLevel targetLevel = destination.getLevel();
        Vec3 targetPos = destination.getLocation();
        Direction direction = destination.getDirection();
        float yRot = direction == null ? player.getYRot() : direction.toYRot();
        float xRot = player.getXRot();
        BlockPos targetBlock = BlockPos.containing(targetPos);
        TameAutoFollowEvents.suppressOwnerTeleportFollow(player.getUUID(), 40);
        TameAutoFollowEvents.scheduleDimensionTpFollow(
                player,
                targetLevel,
                centeredTargetPos(targetBlock),
                yRot,
                xRot,
                100
        );
    }

    private static void teleportWaystoneStyle(ServerPlayer player, TamableAnimal tame, TameData data, ServerLevel targetLevel, BlockPos targetBlock, float yRot, float xRot) {
        if (player == null || tame == null || data == null || targetLevel == null || targetBlock == null) {
            return;
        }
        TamableAnimal moved = tame;
        if (!tame.level().dimension().equals(targetLevel.dimension())) {
            Entity changed = tame.changeDimension(targetLevel);
            if (!(changed instanceof TamableAnimal changedTame)) {
                return;
            }
            moved = changedTame;
        }
        double offsetX = (player.getRandom().nextDouble() - 0.5D) * 2.0D;
        double offsetZ = (player.getRandom().nextDouble() - 0.5D) * 2.0D;
        double finalX = targetBlock.getX() + 0.5D + offsetX;
        double finalY = targetBlock.getY();
        double finalZ = targetBlock.getZ() + 0.5D + offsetZ;
        moved.teleportTo(finalX, finalY, finalZ);
        moved.setYRot(yRot);
        moved.setXRot(xRot);
        TameRegistry.bindEntityToData(moved, data);
        data.lastKnownDimension = targetLevel.dimension().location().toString();
        data.lastKnownX = moved.blockPosition().getX();
        data.lastKnownY = moved.blockPosition().getY();
        data.lastKnownZ = moved.blockPosition().getZ();
        data.lastKnownGameTime = targetLevel.getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        moved.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
        TameRegistry.markDirty();
        queueOwnerResync(player, moved, targetLevel);
    }

    private static void queueOwnerResync(ServerPlayer owner, TamableAnimal tame, ServerLevel level) {
        if (owner == null || tame == null || level == null) {
            return;
        }
        PENDING_OWNER_RESYNC.put(tame.getUUID(), new PendingOwnerResync(
                owner.getUUID(),
                tame.getUUID(),
                new ResourceKeyWrapper(level.dimension().location().toString()),
                serverTick + 1L
        ));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        serverTick++;
        if (PENDING_OWNER_RESYNC.isEmpty() || event.getServer() == null) {
            return;
        }
        List<UUID> finished = new ArrayList<>();
        for (Map.Entry<UUID, PendingOwnerResync> entry : PENDING_OWNER_RESYNC.entrySet()) {
            PendingOwnerResync pending = entry.getValue();
            if (pending == null || pending.dueTick > serverTick) {
                continue;
            }
            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(pending.ownerUuid);
            if (owner == null) {
                finished.add(entry.getKey());
                continue;
            }
            ServerLevel level = null;
            for (ServerLevel candidate : event.getServer().getAllLevels()) {
                if (candidate.dimension().location().toString().equals(pending.dimension.id())) {
                    level = candidate;
                    break;
                }
            }
            if (level == null) {
                finished.add(entry.getKey());
                continue;
            }
            Entity entity = level.getEntity(pending.tameUuid);
            if (entity instanceof TamableAnimal tame && owner.level() == level) {
                resendEntityToOwner(owner, tame);
            }
            finished.add(entry.getKey());
        }
        for (UUID uuid : finished) {
            PENDING_OWNER_RESYNC.remove(uuid);
        }
    }

    private static void resendEntityToOwner(ServerPlayer owner, TamableAnimal tame) {
        if (owner == null || tame == null || owner.connection == null) {
            return;
        }
        owner.connection.send(tame.getAddEntityPacket());
        owner.connection.send(new ClientboundTeleportEntityPacket(tame));
        List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values = tame.getEntityData().getNonDefaultValues();
        if (values != null && !values.isEmpty()) {
            owner.connection.send(new ClientboundSetEntityDataPacket(tame.getId(), values));
        }
        Collection<AttributeInstance> attributes = tame.getAttributes().getSyncableAttributes();
        if (!attributes.isEmpty()) {
            owner.connection.send(new ClientboundUpdateAttributesPacket(tame.getId(), attributes));
        }
        List<Pair<EquipmentSlot, ItemStack>> equipment = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = tame.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                equipment.add(Pair.of(slot, stack.copy()));
            }
        }
        if (!equipment.isEmpty()) {
            owner.connection.send(new ClientboundSetEquipmentPacket(tame.getId(), equipment));
        }
    }

    private static Vec3 centeredTargetPos(BlockPos targetBlock) {
        return new Vec3(targetBlock.getX() + 0.5D, targetBlock.getY(), targetBlock.getZ() + 0.5D);
    }

    private static boolean isWaystoneTeleportEligibleFromData(TameData data) {
        if (data == null || data.isInactive()) {
            return false;
        }
        if (data.hasHome || data.wanderLock) {
            return false;
        }
        if (LevelSystem.getAttributeLevel(data, "tethered_teleport") <= 0) {
            return false;
        }
        return TameAutoFollowEvents.isFollowing(data);
    }

    private static boolean isWaystoneTeleportEligible(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return false;
        }
        if (!tame.isAlive() || !tame.isTame()) {
            return false;
        }
        if (data.hasHome || data.wanderLock) {
            return false;
        }
        if (LevelSystem.getAttributeLevel(data, "tethered_teleport") <= 0) {
            return false;
        }
        return TameAutoFollowEvents.isFollowingForTeleportCompat(tame);
    }
}
