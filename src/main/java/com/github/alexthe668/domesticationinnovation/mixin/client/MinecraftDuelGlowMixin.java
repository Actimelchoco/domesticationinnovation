package com.github.alexthe668.domesticationinnovation.mixin.client;

import com.github.alexthe668.domesticationinnovation.client.ClientDuelOutlines;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftDuelGlowMixin {

    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void domesticationinnovation$renderExtensionDuelGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientDuelOutlines.team(entity) != 0) {
            cir.setReturnValue(true);
        }
    }
}
