package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.LegendaryMonstersDuelCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.FHauntedGuardEntity",
        "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.FLivingArmorEntity",
        "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.Pets.MossyGolemEntity"
})
public abstract class LegendaryMonstersPetDuelMixin {

    @Inject(method = "m_7307_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tl$allowDuelOpponentDamage(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (LegendaryMonstersDuelCompat.shouldTreatAsNotAlliedInDuel((Entity) (Object) this, other)) {
            cir.setReturnValue(false);
        }
    }
}

