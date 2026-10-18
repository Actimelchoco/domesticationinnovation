package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimDisplayProtection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ClaimFrameSupportMixin {
    @Inject(method = "survives()Z", at = @At("HEAD"), cancellable = true)
    private void ftbc$keepClaimedFrameAttached(CallbackInfoReturnable<Boolean> cir) {
        if (ClaimDisplayProtection.protectedDisplay((Entity)(Object)this)) cir.setReturnValue(true);
    }
}
