package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class HeldItemWeaponDurabilityMixin {
    @Inject(method="hurtAndBreak", at=@At("HEAD"), cancellable=true)
    private void tl$keepHeldWeapon(int amount, LivingEntity holder, Consumer<?> onBreak, CallbackInfo ci) {
        if (HeldItemTameWeapons.isArmed(holder) && holder.getMainHandItem() == (Object)this) ci.cancel();
    }
}
