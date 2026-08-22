package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus")
public abstract class SaintsDragonsVarasuchusMixin extends TamableAnimal {

    protected SaintsDragonsVarasuchusMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Inject(method = "isTargetValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void domesticationinnovation$allowDuelOpponentTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (target != null && target.isAlive()
                && TameDuelManager.areDuelOpponents(this.getUUID(), target.getUUID())) {
            cir.setReturnValue(true);
        }
    }
}
