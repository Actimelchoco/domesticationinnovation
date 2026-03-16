package com.github.alexthe668.domesticationinnovation.server.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class LanternRequest {
    public static final String MODE_LANTERN = "LANTERN";
    public static final String MODE_PLAYER_TP = "PLAYER_TP";
    public static final String MODE_FIXED_TARGET_TP = "FIXED_TARGET_TP";

    private String entityType;
    private long timestamp;
    private String nametag;
    private String mode;

    private UUID petUUID;
    private UUID tlId;
    private UUID ownerUUID;

    private BlockPos chunkPosition;
    private String targetDimension;
    private double targetX;
    private double targetY;
    private double targetZ;
    private float targetYaw;
    private float targetPitch;

    public LanternRequest(UUID petUUID, String entityType, UUID ownerUUID, BlockPos chunkPosition, long timestamp, String nametag) {
        this(petUUID, null, entityType, ownerUUID, chunkPosition, timestamp, nametag, MODE_LANTERN, "", 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
    }

    public LanternRequest(UUID petUUID, UUID tlId, String entityType, UUID ownerUUID, BlockPos chunkPosition, long timestamp, String nametag) {
        this(petUUID, tlId, entityType, ownerUUID, chunkPosition, timestamp, nametag, MODE_LANTERN, "", 0.0D, 0.0D, 0.0D, 0.0F, 0.0F);
    }

    public LanternRequest(
            UUID petUUID,
            UUID tlId,
            String entityType,
            UUID ownerUUID,
            BlockPos chunkPosition,
            long timestamp,
            String nametag,
            String mode,
            String targetDimension,
            double targetX,
            double targetY,
            double targetZ,
            float targetYaw,
            float targetPitch
    ) {
        this.petUUID = petUUID;
        this.tlId = tlId;
        this.entityType = entityType;
        this.chunkPosition = chunkPosition;
        this.ownerUUID = ownerUUID;
        this.timestamp = timestamp;
        this.nametag = nametag;
        this.mode = mode == null || mode.isBlank() ? MODE_LANTERN : mode;
        this.targetDimension = targetDimension == null ? "" : targetDimension;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
        this.targetYaw = targetYaw;
        this.targetPitch = targetPitch;
    }

    public UUID getPetUUID() {
        return petUUID;
    }

    public UUID getTlId() {
        return tlId;
    }

    public String getEntityTypeLoc() {
        return this.entityType;
    }

    public EntityType getEntityType() {
        return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(this.entityType));
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNametag() {
        return this.nametag;
    }

    public BlockPos getChunkPosition() {
        return chunkPosition;
    }

    public String getMode() {
        return mode == null || mode.isBlank() ? MODE_LANTERN : mode;
    }

    public boolean isLanternMode() {
        return MODE_LANTERN.equalsIgnoreCase(getMode());
    }

    public boolean isPlayerTeleportMode() {
        return MODE_PLAYER_TP.equalsIgnoreCase(getMode());
    }

    public boolean isFixedTargetTeleportMode() {
        return MODE_FIXED_TARGET_TP.equalsIgnoreCase(getMode());
    }

    public String getTargetDimension() {
        return targetDimension == null ? "" : targetDimension;
    }

    public double getTargetX() {
        return targetX;
    }

    public double getTargetY() {
        return targetY;
    }

    public double getTargetZ() {
        return targetZ;
    }

    public float getTargetYaw() {
        return targetYaw;
    }

    public float getTargetPitch() {
        return targetPitch;
    }

    public String toString(){
        if(getNametag() == null || getNametag().isEmpty()){
            return this.entityType;
        }else{
            return getNametag() + "|" + this.entityType;
        }
    }
}
