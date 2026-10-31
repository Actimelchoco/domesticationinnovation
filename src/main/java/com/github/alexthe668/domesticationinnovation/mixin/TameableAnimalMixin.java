package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TamableAnimal.class)
public abstract class TameableAnimalMixin extends Animal {

    protected TameableAnimalMixin(EntityType<? extends Animal> an, Level lvl) {
        super(an, lvl);
    }

    // Vanilla canAttack rejects isOwnedBy before any targeting conditions run.
    // Change only that owner veto; retain its normal difficulty/target checks.
    @org.spongepowered.asm.mixin.injection.Redirect(method = "canAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/TamableAnimal;isOwnedBy(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean tl$allowDuelOwnerTarget(TamableAnimal tame, LivingEntity target) {
        return !TameDuelManager.areDuelOpponents(tame.getUUID(), target.getUUID()) && tame.isOwnedBy(target);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/TamableAnimal;isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z"},
            remap = true,
            at = @At(
                    value = "HEAD"
            ),
            cancellable = true
    )
    private void di_isAlliedTo(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other != null && TameDuelManager.areDuelOpponents(getUUID(), other.getUUID())) {
            cir.setReturnValue(false);
            return;
        }
        if(TameableUtils.hasSameOwnerAs(this, other)){
            cir.setReturnValue(true);
        }
    }
}
