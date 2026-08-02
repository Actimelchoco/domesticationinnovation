package com.github.alexthe668.domesticationinnovation.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.github.alexthe666.alexsmobs.entity.EntityWarpedToad")
public abstract class AlexsMobsWarpedToadMixin {

    @Redirect(
            method = "m_8119_",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;m_20256_(Lnet/minecraft/world/phys/Vec3;)V",
                    remap = false
            ),
            remap = false,
            require = 0
    )
    private void tl$skipTonguePullAgainstStrongerKnockbackResistance(LivingEntity target, Vec3 movement) {
        LivingEntity toad = (LivingEntity) (Object) this;
        double targetResistance = target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double toadResistance = toad.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (targetResistance > toadResistance) {
            return;
        }
        target.setDeltaMovement(movement);
    }
}
