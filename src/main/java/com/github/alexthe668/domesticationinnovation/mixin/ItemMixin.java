package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.GuardianToolEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void di_getUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (GuardianToolEvents.isGuardianToolArrow(stack)) {
            cir.setReturnValue(120);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void di_getUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        if (GuardianToolEvents.isGuardianToolArrow(stack)) {
            cir.setReturnValue(UseAnim.BLOCK);
        }
    }
}
