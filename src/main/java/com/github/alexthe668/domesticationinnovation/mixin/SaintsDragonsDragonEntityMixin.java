package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.server.entity.base.DragonEntity")
public abstract class SaintsDragonsDragonEntityMixin extends TamableAnimal {

    protected SaintsDragonsDragonEntityMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "isAlly", at = @At("HEAD"), cancellable = true, remap = false)
    private void domesticationinnovation$allowDuelOpponentDamage(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (target != null && TameDuelManager.areDuelOpponents(this.getUUID(), target.getUUID())) {
            cir.setReturnValue(false);
        }
    }
}
