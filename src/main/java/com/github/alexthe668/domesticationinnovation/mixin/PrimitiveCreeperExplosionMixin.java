package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class PrimitiveCreeperExplosionMixin extends Monster {
    @Shadow private int swell;
    @Shadow private int oldSwell;
    @Shadow private int explosionRadius;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;
    @Unique private int tl$blastCooldown;
    protected PrimitiveCreeperExplosionMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = "explodeCreeper", at = @At("HEAD"), cancellable = true)
    private void tl$surviveBlast(CallbackInfo ci) {
        if (!PrimitiveMobsCompat.isPrimitivePet(this) || !((ModifedToBeTameable) this).isTame()) return;
        ci.cancel();
        Creeper creeper = (Creeper) (Object) this;
        swell = 0;
        oldSwell = 0;
        creeper.setSwellDir(-1);
        entityData.set(DATA_IS_IGNITED, false);
        if (!level().isClientSide && tl$blastCooldown == 0) {
            tl$blastCooldown = 60;
            level().explode(this, getX(), getY(), getZ(), explosionRadius * (creeper.isPowered() ? 2.0F : 1.0F), Level.ExplosionInteraction.MOB);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void tl$cooldown(CallbackInfo ci) {
        if (tl$blastCooldown > 0) {
            tl$blastCooldown--;
            swell = 0;
            oldSwell = 0;
            ((Creeper) (Object) this).setSwellDir(-1);
            entityData.set(DATA_IS_IGNITED, false);
        }
    }
}
