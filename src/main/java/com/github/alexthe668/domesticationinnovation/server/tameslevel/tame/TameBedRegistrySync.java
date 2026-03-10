package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.TamableAnimal;

import java.util.Objects;

public final class TameBedRegistrySync {

    private TameBedRegistrySync() {
    }

    public static boolean syncFromEntity(TamableAnimal tame, TameData data) {
        if (tame == null || data == null) {
            return false;
        }
        BlockPos bedPos = TameableUtils.getPetBedPos(tame);
        String bedDimension = TameableUtils.getPetBedDimension(tame);
        if (bedPos == null || bedDimension == null || bedDimension.isBlank()) {
            boolean changed = data.hasPetBed || !Objects.equals(data.petBedDimension, "")
                    || data.petBedX != 0 || data.petBedY != 0 || data.petBedZ != 0;
            data.hasPetBed = false;
            data.petBedDimension = "";
            data.petBedX = 0;
            data.petBedY = 0;
            data.petBedZ = 0;
            return changed;
        }

        boolean changed = !data.hasPetBed
                || !Objects.equals(data.petBedDimension, bedDimension)
                || data.petBedX != bedPos.getX()
                || data.petBedY != bedPos.getY()
                || data.petBedZ != bedPos.getZ();

        data.hasPetBed = true;
        data.petBedDimension = bedDimension;
        data.petBedX = bedPos.getX();
        data.petBedY = bedPos.getY();
        data.petBedZ = bedPos.getZ();
        return changed;
    }
}
