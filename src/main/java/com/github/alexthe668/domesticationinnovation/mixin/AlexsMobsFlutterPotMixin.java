package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.item.ItemFlutterPot")
public abstract class AlexsMobsFlutterPotMixin {

    @Inject(method = "placeFish", at = @At("RETURN"), remap = false)
    private void tl$registerReleasedFlutter(UseOnContext context, LevelAccessor level, CallbackInfoReturnable<Entity> cir) {
        Entity entity = cir.getReturnValue();
        if (entity instanceof TamableAnimal tame && tame.isTame()) {
            TameSpawnEvents.registerOrRestoreTame(tame, false, true);
        }
    }
}
