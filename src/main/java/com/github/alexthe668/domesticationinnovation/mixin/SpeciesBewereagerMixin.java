package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.ninni.species.server.entity.mob.update_3.Bewereager")
public abstract class SpeciesBewereagerMixin extends Monster {
    protected SpeciesBewereagerMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }

    @Inject(method = {"transform", "split"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tl$keepForm(CallbackInfo ci) {
        if (BewereagerCompat.isTemporary(this)) ci.cancel();
    }
}
