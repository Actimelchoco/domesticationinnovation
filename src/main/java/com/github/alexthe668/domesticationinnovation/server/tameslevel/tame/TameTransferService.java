package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalInstaller;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe666.citadel.server.entity.IComandableMob;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TameTransferService {

    public record TransferResult(TamableAnimal entity, boolean crossDimension, String error) {
        public boolean success() {
            return entity != null && error.isBlank();
        }
    }

    private TameTransferService() {}

    public static TransferResult transferToPlayer(TamableAnimal tame, ServerPlayer player, TameData data) {
        if (tame == null || player == null) {
            return new TransferResult(null, false, "invalid context");
        }
        if (!tame.isAlive()) {
            return new TransferResult(null, false, "tame is not alive");
        }

        boolean crossDimension = !tame.level().dimension().equals(player.level().dimension());
        if (!crossDimension) {
            tame.teleportTo(player.getX(), player.getY(), player.getZ());
            normalizeTransferredTame(tame, data);
            refreshLastKnown(data, tame, player.serverLevel());
            return new TransferResult(tame, false, "");
        }

        CompoundTag snapshot = new CompoundTag();
        tame.save(snapshot);
        UUID ownerId = tame.getOwnerUUID();
        if (data != null && data.ownerUUID != null) {
            ownerId = data.ownerUUID;
        }
        DyeColor collar = tame instanceof Wolf wolf ? wolf.getCollarColor() : null;

        ServerLevel targetLevel = player.serverLevel();
        List<double[]> attempts = transferAttempts(player);
        for (double[] pos : attempts) {
            Entity created = tame.getType().create(targetLevel);
            if (!(created instanceof TamableAnimal moved)) {
                return new TransferResult(null, true, "failed to create tame type");
            }
            try {
                if (!snapshot.isEmpty()) {
                    moved.load(snapshot.copy());
                }
                moved.setUUID(tame.getUUID());
                moved.moveTo(pos[0], pos[1], pos[2], player.getYRot(), player.getXRot());
                enforceTamedOwnerPreserveCollar(moved, ownerId, collar);
                normalizeTransferredTame(moved, data);

                if (!targetLevel.addFreshEntity(moved)) {
                    continue;
                }
                tame.getPersistentData().putBoolean(CommonProxy.SKIP_LANTERN_UNLOAD_ONCE_TAG, true);
                tame.discard();
                moved.getNavigation().moveTo(player, 1.0D);
                refreshLastKnown(data, moved, targetLevel);
                return new TransferResult(moved, true, "");
            } catch (Throwable ignored) {
                // retry at another nearby position
            }
        }
        return new TransferResult(null, true, "spawn failed in target dimension");
    }

    public static TransferResult transferToLocation(
            TamableAnimal tame,
            ServerLevel targetLevel,
            double x,
            double y,
            double z,
            float yRot,
            float xRot,
            TameData data
    ) {
        if (tame == null || targetLevel == null) {
            return new TransferResult(null, false, "invalid context");
        }
        if (!tame.isAlive()) {
            return new TransferResult(null, false, "tame is not alive");
        }

        boolean crossDimension = !tame.level().dimension().equals(targetLevel.dimension());
        if (!crossDimension) {
            tame.teleportTo(x, y, z);
            tame.setYRot(yRot);
            tame.setXRot(xRot);
            normalizeTransferredTame(tame, data);
            refreshLastKnown(data, tame, targetLevel);
            return new TransferResult(tame, false, "");
        }

        CompoundTag snapshot = new CompoundTag();
        tame.save(snapshot);
        UUID ownerId = tame.getOwnerUUID();
        if (data != null && data.ownerUUID != null) {
            ownerId = data.ownerUUID;
        }
        DyeColor collar = tame instanceof Wolf wolf ? wolf.getCollarColor() : null;

        List<double[]> attempts = new ArrayList<>();
        attempts.add(new double[]{x, y, z});
        attempts.add(new double[]{x + 1.5D, y, z});
        attempts.add(new double[]{x - 1.5D, y, z});
        attempts.add(new double[]{x, y, z + 1.5D});
        attempts.add(new double[]{x, y, z - 1.5D});

        for (double[] pos : attempts) {
            Entity created = tame.getType().create(targetLevel);
            if (!(created instanceof TamableAnimal moved)) {
                return new TransferResult(null, true, "failed to create tame type");
            }
            try {
                if (!snapshot.isEmpty()) {
                    moved.load(snapshot.copy());
                }
                moved.setUUID(tame.getUUID());
                moved.moveTo(pos[0], pos[1], pos[2], yRot, xRot);
                enforceTamedOwnerPreserveCollar(moved, ownerId, collar);
                normalizeTransferredTame(moved, data);

                if (!targetLevel.addFreshEntity(moved)) {
                    continue;
                }
                tame.getPersistentData().putBoolean(CommonProxy.SKIP_LANTERN_UNLOAD_ONCE_TAG, true);
                tame.discard();
                if (targetLevel.getServer() != null) {
                    ServerPlayer nearest = targetLevel.getServer().getPlayerList().getPlayer(ownerId);
                    if (nearest != null) {
                        moved.getNavigation().moveTo(nearest, 1.0D);
                    }
                }
                refreshLastKnown(data, moved, targetLevel);
                return new TransferResult(moved, true, "");
            } catch (Throwable ignored) {
            }
        }
        return new TransferResult(null, true, "spawn failed in target dimension");
    }

    private static List<double[]> transferAttempts(ServerPlayer player) {
        List<double[]> positions = new ArrayList<>();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        positions.add(new double[]{x, y, z});
        positions.add(new double[]{x + 1.5D, y, z});
        positions.add(new double[]{x - 1.5D, y, z});
        positions.add(new double[]{x, y, z + 1.5D});
        positions.add(new double[]{x, y, z - 1.5D});
        return positions;
    }

    private static void normalizeTransferredTame(TamableAnimal tame, TameData data) {
        if (tame == null) {
            return;
        }
        tame.stopRiding();
        if (tame.isVehicle()) {
            tame.ejectPassengers();
        }
        tame.fallDistance = 0.0F;
        tame.hurtTime = 0;
        tame.deathTime = 0;
        tame.invulnerableTime = 0;
        tame.setSecondsOnFire(0);
        tame.setRemainingFireTicks(0);
        tame.setDeltaMovement(0.0D, 0.0D, 0.0D);
        tame.setTarget(null);
        tame.getNavigation().stop();
        tame.setNoGravity(false);
        tame.setNoAi(false);
        tame.setOrderedToSit(false);
        tame.setInSittingPose(false);
        TameGoalInstaller.installIfMissing(tame);
        if (data != null) {
            TameRegistry.bindEntityToData(tame, data);
            LevelSystem.reapplyTypeBasePlusBonuses(tame, data);
        }
    }

    private static void refreshLastKnown(TameData data, TamableAnimal tame, ServerLevel level) {
        if (data == null || tame == null || level == null) {
            return;
        }
        data.lastKnownDimension = level.dimension().location().toString();
        data.lastKnownX = tame.blockPosition().getX();
        data.lastKnownY = tame.blockPosition().getY();
        data.lastKnownZ = tame.blockPosition().getZ();
        data.lastKnownGameTime = level.getGameTime();
        CompoundTag refreshedSnapshot = new CompoundTag();
        TameRegistry.bindEntityToData(tame, data);
        tame.save(refreshedSnapshot);
        data.entitySnapshot = refreshedSnapshot;
        TameRegistry.markDirty();
    }

    private static void enforceTamedOwnerPreserveCollar(TamableAnimal tame, UUID ownerId, DyeColor collar) {
        if (tame == null) return;
        if (ownerId != null) {
            tame.setTame(true);
            if (!ownerId.equals(tame.getOwnerUUID())) {
                tame.setOwnerUUID(ownerId);
            }
        } else if (!tame.isTame()) {
            tame.setTame(true);
        }
        if (collar != null && tame instanceof Wolf wolf) {
            wolf.setCollarColor(collar);
        }
    }
}
