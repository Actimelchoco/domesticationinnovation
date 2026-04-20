package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.SpawnerTriggerSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BaseSpawner.class)
public class BaseSpawnerMixin {

    @Inject(method = "isNearPlayer", at = @At("RETURN"), cancellable = true)
    private void di_isNearPlayer(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || level == null || level.isClientSide || pos == null) {
            return;
        }
        if (SpawnerTriggerSupport.hasSpawnerTriggerTameInRange(level, Vec3.atCenterOf(pos), 16.0D)) {
            cir.setReturnValue(true);
        }
    }
}
