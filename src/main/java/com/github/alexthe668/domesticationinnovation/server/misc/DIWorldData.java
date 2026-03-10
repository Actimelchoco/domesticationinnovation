package com.github.alexthe668.domesticationinnovation.server.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.*;

public class DIWorldData extends SavedData {

    private static final String IDENTIFIER = "domesticationinnovation_world_data";
    private final List<RespawnRequest> respawnRequestList = new ArrayList<>();
    private final List<LanternRequest> lanternRequestList = new ArrayList<>();

    private DIWorldData() {
        super();
    }

    public static DIWorldData get(Level world) {
        if (world instanceof ServerLevel) {
            ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
            DimensionDataStorage storage = overworld.getDataStorage();
            DIWorldData data = storage.computeIfAbsent(DIWorldData::load, DIWorldData::new, IDENTIFIER);
            if (data != null) {
                data.setDirty();
            }
            return data;
        }
        return null;
    }

    public static DIWorldData load(CompoundTag nbt) {
        DIWorldData data = new DIWorldData();
        if (nbt.contains("RespawnList")) {
            ListTag listtag = nbt.getList("RespawnList", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = listtag.getCompound(i);
                data.respawnRequestList.add(new RespawnRequest(innerTag.getString("EntityType"), innerTag.getString("DimensionIn"), innerTag.getCompound("EntityData"),
                        new BlockPos(innerTag.getInt("X"), innerTag.getInt("Y"), innerTag.getInt("Z")), innerTag.getLong("Timestamp"), innerTag.getString("EntityNametag")));
            }
        }
        if (nbt.contains("LanternList")) {
            ListTag listtag = nbt.getList("LanternList", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag innerTag = listtag.getCompound(i);
                String mode = innerTag.contains("RequestMode", Tag.TAG_STRING)
                        ? innerTag.getString("RequestMode")
                        : LanternRequest.MODE_LANTERN;
                String targetDimension = innerTag.contains("TargetDimension", Tag.TAG_STRING) ? innerTag.getString("TargetDimension") : "";
                double targetX = innerTag.contains("TargetX", Tag.TAG_DOUBLE) ? innerTag.getDouble("TargetX") : 0.0D;
                double targetY = innerTag.contains("TargetY", Tag.TAG_DOUBLE) ? innerTag.getDouble("TargetY") : 0.0D;
                double targetZ = innerTag.contains("TargetZ", Tag.TAG_DOUBLE) ? innerTag.getDouble("TargetZ") : 0.0D;
                float targetYaw = innerTag.contains("TargetYaw", Tag.TAG_FLOAT) ? innerTag.getFloat("TargetYaw") : 0.0F;
                float targetPitch = innerTag.contains("TargetPitch", Tag.TAG_FLOAT) ? innerTag.getFloat("TargetPitch") : 0.0F;
                data.lanternRequestList.add(new LanternRequest(
                        innerTag.getUUID("PetUUID"),
                        innerTag.getString("EntityType"),
                        innerTag.getUUID("OwnerUUID"),
                        new BlockPos(innerTag.getInt("X"), innerTag.getInt("Y"), innerTag.getInt("Z")),
                        innerTag.getLong("Timestamp"),
                        innerTag.getString("EntityNametag"),
                        mode,
                        targetDimension,
                        targetX,
                        targetY,
                        targetZ,
                        targetYaw,
                        targetPitch
                ));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag compound) {
        if (!this.respawnRequestList.isEmpty()) {
            ListTag listTag = new ListTag();
            for(RespawnRequest request : respawnRequestList){
                CompoundTag tag = new CompoundTag();
                tag.putString("EntityType", request.getEntityTypeLoc());
                tag.putString("DimensionIn", request.getDimension());
                tag.put("EntityData", request.getEntityData());
                tag.putInt("X", request.getBedPosition().getX());
                tag.putInt("Y", request.getBedPosition().getY());
                tag.putInt("Z", request.getBedPosition().getZ());
                tag.putLong("Timestamp", request.getTimestamp());
                tag.putString("EntityNametag", request.getNametag());
                listTag.add(tag);
            }
            compound.put("RespawnList", listTag);
        }
        if (!this.lanternRequestList.isEmpty()) {
            ListTag listTag = new ListTag();
            for(LanternRequest request : lanternRequestList){
                CompoundTag tag = new CompoundTag();
                tag.putUUID("PetUUID", request.getPetUUID());
                tag.putString("EntityType", request.getEntityTypeLoc());
                tag.putUUID("OwnerUUID", request.getOwnerUUID());
                tag.putLong("Timestamp", request.getTimestamp());
                tag.putString("EntityNametag", request.getNametag());
                tag.putInt("X", request.getChunkPosition().getX());
                tag.putInt("Y", request.getChunkPosition().getY());
                tag.putInt("Z", request.getChunkPosition().getZ());
                tag.putString("RequestMode", request.getMode());
                if (!request.getTargetDimension().isBlank()) {
                    tag.putString("TargetDimension", request.getTargetDimension());
                }
                tag.putDouble("TargetX", request.getTargetX());
                tag.putDouble("TargetY", request.getTargetY());
                tag.putDouble("TargetZ", request.getTargetZ());
                tag.putFloat("TargetYaw", request.getTargetYaw());
                tag.putFloat("TargetPitch", request.getTargetPitch());
                listTag.add(tag);
            }
            compound.put("LanternList", listTag);
        }
        return compound;
    }

    public void addRespawnRequest(RespawnRequest request){
        this.respawnRequestList.add(request);
        this.setDirty();
    }

    public void removeRespawnRequest(RespawnRequest request){
        this.respawnRequestList.remove(request);
        this.setDirty();
    }
    public List<RespawnRequest> getRespawnRequestsFor(Level level, BlockPos pos){
        List<RespawnRequest> list = new ArrayList<>();
        String dimension = level.dimension().toString();
        for(RespawnRequest request : this.respawnRequestList){
            if(dimension.equals(request.getDimension()) && pos.equals(request.getBedPosition())){
                list.add(request);
            }
        }
        return list;
    }

    public List<RespawnRequest> getRespawnRequestsSnapshot() {
        return new ArrayList<>(this.respawnRequestList);
    }

    public int clearAllRespawnRequests() {
        int removed = this.respawnRequestList.size();
        if (removed > 0) {
            this.respawnRequestList.clear();
            this.setDirty();
        }
        return removed;
    }

    public int removeRespawnRequestsForPet(UUID petUuid) {
        if (petUuid == null) {
            return 0;
        }
        int before = this.respawnRequestList.size();
        this.respawnRequestList.removeIf(request -> request != null && requestMatchesPetUuid(request, petUuid));
        int removed = before - this.respawnRequestList.size();
        if (removed > 0) {
            this.setDirty();
        }
        return removed;
    }

    private static boolean requestMatchesPetUuid(RespawnRequest request, UUID petUuid) {
        CompoundTag entityData = request.getEntityData();
        if (entityData == null) {
            return false;
        }
        if (entityData.hasUUID("TLRegistryUUID") && petUuid.equals(entityData.getUUID("TLRegistryUUID"))) {
            return true;
        }
        return entityData.contains("UUID", Tag.TAG_INT_ARRAY)
                && entityData.hasUUID("UUID")
                && petUuid.equals(entityData.getUUID("UUID"));
    }

    public void addLanternRequest(LanternRequest request){
        this.lanternRequestList.add(request);
        this.setDirty();
    }

    public void removeLanternRequest(LanternRequest request){
        this.lanternRequestList.remove(request);
        this.setDirty();
    }

    public void removeMatchingLanternRequests(UUID reloaded){
        this.lanternRequestList.removeIf(request -> request.getPetUUID().equals(reloaded));
        this.setDirty();
    }

    public List<LanternRequest> getLanternRequestsFor(UUID uuid){
        List<LanternRequest> list = new ArrayList<>();
        for(LanternRequest request : this.lanternRequestList){
            if(uuid.equals(request.getOwnerUUID())){
                list.add(request);
            }
        }
        return list;
    }

    public List<LanternRequest> getLanternRequestsSnapshot() {
        return new ArrayList<>(this.lanternRequestList);
    }
}
