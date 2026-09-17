package com.github.alexthe668.domesticationinnovation.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class DrumBlockEntity extends BlockEntity {

    private UUID placerUUID;
    private String selectorName = "Drum";

    public DrumBlockEntity(BlockPos pos, BlockState state) {
        super(DITileEntityRegistry.DRUM.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        com.github.alexthe668.domesticationinnovation.server.tameslevel.LoadedChestDrums.add(this);
    }

    @Override
    public void setRemoved() {
        com.github.alexthe668.domesticationinnovation.server.tameslevel.LoadedChestDrums.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        com.github.alexthe668.domesticationinnovation.server.tameslevel.LoadedChestDrums.remove(this);
        super.onChunkUnloaded();
    }

    public UUID getPlacerUUID() {
        return placerUUID;
    }

    public void setPlacerUUID(UUID placerUUID) {
        this.placerUUID = placerUUID;
    }

    public String getSelectorName() {
        return selectorName == null || selectorName.isBlank() ? "Drum" : selectorName;
    }

    public void setSelectorName(String selectorName) {
        this.selectorName = selectorName == null || selectorName.isBlank() ? "Drum" : selectorName;
        setChanged();
    }

    @Override
    public void load(CompoundTag compound) {
        super.load(compound);
        if (compound.contains("PlacerUUID")) {
            this.placerUUID = compound.getUUID("PlacerUUID");
        }
        if (compound.contains("SelectorName")) {
            this.selectorName = compound.getString("SelectorName");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compound) {
        super.saveAdditional(compound);
        if (this.placerUUID != null) {
            compound.putUUID("PlacerUUID", placerUUID);
        }
        compound.putString("SelectorName", getSelectorName());
    }
}
