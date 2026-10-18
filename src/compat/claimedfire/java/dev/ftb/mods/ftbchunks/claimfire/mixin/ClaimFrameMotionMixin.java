package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimDisplayProtection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HangingEntity.class)
public abstract class ClaimFrameMotionMixin {
    @Inject(method = "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V", at = @At("HEAD"), cancellable = true)
    private void ftbc$stopFrameMovementBreak(MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object)this instanceof ItemFrame && ClaimDisplayProtection.protectedDisplay((Entity)(Object)this)) ci.cancel();
    }
    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true)
    private void ftbc$stopFramePushBreak(double x, double y, double z, CallbackInfo ci) {
        if ((Object)this instanceof ItemFrame && ClaimDisplayProtection.protectedDisplay((Entity)(Object)this)) ci.cancel();
    }
}
