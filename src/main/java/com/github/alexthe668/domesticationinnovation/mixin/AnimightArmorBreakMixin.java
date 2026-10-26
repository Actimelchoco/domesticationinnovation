package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class AnimightArmorBreakMixin {
    // After real durability/Unbreaking calculation, before break callbacks and consumption.
    // This also precedes DNL's own conversion at ItemStack.shrink.
    @Inject(method = "hurtAndBreak", at = @At(value = "INVOKE",
            target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"), cancellable = true)
    private <T extends LivingEntity> void di_preserveAnimightArmor(int amount, T wearer,
            Consumer<T> onBreak, CallbackInfo ci) {
        if (AnimightEquipmentEvents.preserveBreakingArmor(wearer, (ItemStack) (Object) this)) ci.cancel();
    }
}
