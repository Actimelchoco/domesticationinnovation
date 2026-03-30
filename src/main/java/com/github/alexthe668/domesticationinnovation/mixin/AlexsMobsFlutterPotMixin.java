package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.item.ItemFlutterPot")
public abstract class AlexsMobsFlutterPotMixin {

    @Inject(method = "placeFish", at = @At("RETURN"), remap = false, require = 0)
    private void tl$registerReleasedFlutter(ServerLevel worldIn, ItemStack stack, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (worldIn == null || pos == null) {
            return;
        }
        for (TamableAnimal tame : worldIn.getEntitiesOfClass(TamableAnimal.class, new net.minecraft.world.phys.AABB(pos).inflate(2.0D))) {
            if (!tame.isTame()) {
                continue;
            }
            if (!"alexsmobs:flutter".equals(String.valueOf(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(tame.getType())))) {
                continue;
            }
            TameSpawnEvents.registerOrRestoreTame(tame, false, true);
            break;
        }
    }
}
