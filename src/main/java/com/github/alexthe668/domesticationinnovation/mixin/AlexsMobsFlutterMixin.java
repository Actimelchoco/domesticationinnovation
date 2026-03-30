package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityFlutter")
public abstract class AlexsMobsFlutterMixin {

    @Inject(method = {"mobInteract", "interactMob"}, at = @At("RETURN"), remap = false, require = 0)
    private void tl$markStoredOnPotStore(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!((Object) this instanceof TamableAnimal tame)) {
            return;
        }
        if (!cir.getReturnValue().consumesAction()) {
            return;
        }
        if (!player.isShiftKeyDown()) {
            return;
        }
        if (!tame.isRemoved() || tame.getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.DISCARDED) {
            return;
        }
        TameSpawnEvents.markFlutterStoredFromPot(tame);
    }

    @Inject(method = "m_6071_", at = @At("RETURN"), remap = false, require = 0)
    private void tl$markStoredOnPotStoreObf(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        tl$markStoredOnPotStore(player, hand, cir);
    }
}
