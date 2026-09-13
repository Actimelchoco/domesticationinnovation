package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.misanthropy.primitive_mobs.entity.projectile.PrimitiveTnt", remap = false)
public abstract class PrimitiveTntMixin extends Entity {
    protected PrimitiveTntMixin(EntityType<?> type, Level level) { super(type, level); }
    @Shadow public abstract LivingEntity getOwner();
    @Shadow public abstract float getStrength();

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void tl$attributePetBlast(CallbackInfo ci) {
        LivingEntity owner = getOwner();
        if (!TameEntityAdapter.isTame(owner)) return;
        ci.cancel();
        // The native TNT is a plain Entity, so vanilla cannot find its living owner.
        // Using the pet as the explosion source also excludes it from its own blast.
        if (!level().isClientSide) level().explode(owner, getX(), getY() + 0.0625D, getZ(),
                com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat.petBlastRadius(owner, getStrength()), Level.ExplosionInteraction.NONE);
    }
}
