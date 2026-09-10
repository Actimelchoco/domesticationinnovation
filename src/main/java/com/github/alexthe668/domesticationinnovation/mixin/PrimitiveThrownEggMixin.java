package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo
@Mixin(targets = "com.misanthropy.primitive_mobs.entity.projectile.ThrownPrimitiveEgg", remap = false)
public abstract class PrimitiveThrownEggMixin {
    // Retain the egg's native Player-owner check, replacing only its fixed-20-HP taming call.
    @Redirect(method = "spawnHatchling", at = @At(value = "INVOKE", target = "Lcom/misanthropy/primitive_mobs/entity/PrimitiveTameable;tame(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/player/Player;)V"))
    private void tl$preserveIndividual(Mob mob, Player player) {
        PrimitiveMobsCompat.tame(mob, player);
    }
}
