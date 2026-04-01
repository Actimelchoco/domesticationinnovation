package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.ArmageddonCompatUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.armageddonmod.procedures.TheFamineOnEntityTickUpdateProcedure")
public abstract class ArmageddonTheFamineProcedureMixin {

    @Inject(method = "lambda$execute$13", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageTamesInAttackBurst(LevelAccessor level, Entity entity, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearbyBossTames(level, entity, 5.0D, 98.0F, 0.8D);
    }

    @Inject(method = "lambda$execute$10", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageTamesInSweep(LevelAccessor level, double x, double y, double z, Entity entity, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearbyBossTames(level, entity, 6.0D, 98.0F, 1.0D);
    }

    @Inject(method = "lambda$execute$2", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageNearestTameInFinisher(LevelAccessor level, Entity entity, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearestBossTame(level, entity, 8.0D, 95.0F, 0.0D);
    }
}
