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
            level().explode(this, getX(), getY(), getZ(), PrimitiveMobsCompat.petBlastRadius(this, explosionRadius * (creeper.isPowered() ? 2.0F : 1.0F)), Level.ExplosionInteraction.NONE);
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
            return;
        }
        // Festive creepers suppress swelling; support creepers only acquire a native
        // fuse at low health. Owned combat pets need a fuse at any health.
        if (!level().isClientSide && PrimitiveMobsCompat.isPrimitivePet(this)
                && ((ModifedToBeTameable) this).isTame()
                && java.util.Set.of("festive_creeper", "support_creeper").contains(
                        net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(getType()).getPath())) {
            ModifedToBeTameable pet = (ModifedToBeTameable) this;
            var target = getTarget();
            double rangeSquared = swell > 0 ? 49.0D : 9.0D;
            boolean attack = !pet.isStayingStill() && target != null && target.isAlive()
                    && distanceToSqr(target) < rangeSquared && pet.isValidAttackTarget(target)
                    && canAttack(target) && getSensing().hasLineOfSight(target);
            ((Creeper) (Object) this).setSwellDir(attack ? 1 : -1);
        }
    }
}
