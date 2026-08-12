package com.github.alexthe668.domesticationinnovation.server.block;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData;
import com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class PetBedBlockEntity extends BlockEntity {

    public PetBedBlockEntity(BlockPos pos, BlockState state) {
        super(DITileEntityRegistry.PET_BED.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PetBedBlockEntity blockEntity) {
        long time = level.dayTime() % 24000L;
        if(time == 1){
            DIWorldData data = DIWorldData.get(level);
            if(data != null){
               List<RespawnRequest> requestList = data.getRespawnRequestsFor(level, pos);
               for(RespawnRequest request : requestList){
                    TameData tameData = findRegistryDataForRequest(request);
                    if (tameData == null || !tameData.dead) {
                        data.removeRespawnRequest(request);
                        continue;
                    }
                    if(addAndRemoveEntity(level, pos, state.getValue(PetBedBlock.FACING), request, tameData)){
                        data.removeRespawnRequest(request);
                    }
               }
            }
        }
    }

    public void removeAllRequestsFor(@Nullable Player message){
        DIWorldData data = DIWorldData.get(level);
        if(data != null){
            List<RespawnRequest> requestList = data.getRespawnRequestsFor(level, this.getBlockPos());
            for(RespawnRequest request : requestList){
                data.removeRespawnRequest(request);
                if(message != null){
                    message.displayClientMessage(Component.translatable("message.domesticationinnovation.goodbye", request.getNametag()), false);
                }
            }
        }
    }

    private static boolean addAndRemoveEntity(Level level, BlockPos pos, Direction dir, RespawnRequest request, TameData tameData) {
        UUID requestUuid = readRequestUuid(request);
        UUID requestTlId = readRequestTlId(request);
        if (requestUuid != null && isEntityAliveAnywhere(level, requestUuid)) {
            return true;
        }
        if (requestTlId != null && isTlEntityAliveAnywhere(level, requestTlId)) {
            return true;
        }
        if (!DomesticationMod.CONFIG.petBedRespawns.get() || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!TameCommands.respawnDeadTameAtBed(serverLevel, pos, dir, tameData)) {
            return false;
        }
        Entity spawned = tameData != null && tameData.uuid != null ? serverLevel.getEntity(tameData.uuid) : null;
        Entity owner = spawned == null ? null : TameableUtils.getOwnerOf(spawned);
        if (owner instanceof Player player && spawned != null && PlayerDebugSettings.autoRespawnMessages(player.getUUID())) {
            player.displayClientMessage(Component.translatable("message.domesticationinnovation.respawn", spawned.getName()), false);
        }
        return true;
    }

    @Nullable
    private static TameData findRegistryDataForRequest(RespawnRequest request) {
        if (request == null) {
            return null;
        }
        UUID tlId = readRequestTlId(request);
        if (tlId != null) {
            TameData byTlId = TameRegistry.getByTlId(tlId);
            if (byTlId != null) {
                return byTlId;
            }
        }
        UUID uuid = readRequestUuid(request);
        return uuid == null ? null : TameRegistry.get(uuid);
    }

    @Nullable
    private static UUID readRequestUuid(RespawnRequest request) {
        if (request == null) {
            return null;
        }
        CompoundTag entityData = request.getEntityData();
        if (entityData == null) {
            return null;
        }
        if (entityData.hasUUID("TLRegistryUUID")) {
            return entityData.getUUID("TLRegistryUUID");
        }
        if (entityData.contains("UUID", Tag.TAG_INT_ARRAY) && entityData.hasUUID("UUID")) {
            return entityData.getUUID("UUID");
        }
        return null;
    }

    @Nullable
    private static UUID readRequestTlId(RespawnRequest request) {
        if (request == null) {
            return null;
        }
        CompoundTag entityData = request.getEntityData();
        if (entityData == null) {
            return null;
        }
        return entityData.hasUUID("TLID") ? entityData.getUUID("TLID") : null;
    }

    private static boolean isEntityAliveAnywhere(Level level, UUID uuid) {
        if (level == null || level.getServer() == null || uuid == null) {
            return false;
        }
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            Entity loaded = serverLevel.getEntity(uuid);
            if (loaded instanceof LivingEntity living && living.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTlEntityAliveAnywhere(Level level, UUID tlId) {
        if (level == null || level.getServer() == null || tlId == null) {
            return false;
        }
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Entity loaded : serverLevel.getAllEntities()) {
                if (!(loaded instanceof LivingEntity living) || !living.isAlive()) {
                    continue;
                }
                if (loaded instanceof TamableAnimal tame && tlId.equals(TameData.getTlId(tame))) {
                    return true;
                }
            }
        }
        return false;
    }

    public void resetBedsForNearbyPets() {
        Predicate<Entity> pet = (animal) -> TameableUtils.isTamed(animal) && TameableUtils.getPetBedPos((LivingEntity)animal) != null && TameableUtils.getPetBedPos((LivingEntity)animal).equals(this.getBlockPos());
        List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(this.getBlockPos().offset(-10, -5, -10), this.getBlockPos().offset(10, 5, 10)), EntitySelector.NO_SPECTATORS.and(pet));
        for (LivingEntity entity : list){
            Entity owner = TameableUtils.getOwnerOf(entity);
            if(owner instanceof Player){
                ((Player)owner).displayClientMessage(Component.translatable("message.domesticationinnovation.remove_respawn", entity.getName()), false);
                TameableUtils.removePetBedPos(entity);
            }
        }
    }
}
