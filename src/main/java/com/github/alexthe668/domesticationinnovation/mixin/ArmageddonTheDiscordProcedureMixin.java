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
@Mixin(targets = "net.mcreator.armageddonmod.procedures.TheDiscordOnEntityTickUpdateProcedure")
public abstract class ArmageddonTheDiscordProcedureMixin {

    @Inject(method = "lambda$execute$48", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageTamesInAttackThree(LevelAccessor level, Entity entity, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearbyBossTames(level, entity, 7.0D, 100.0F, 0.0D);
    }

    @Inject(method = "lambda$execute$42", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageTamesInAttackFour(LevelAccessor level, Entity entity, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearbyBossTames(level, entity, 4.0D, 95.0F, 0.0D);
    }

    @Inject(method = "lambda$execute$33", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$damageTamesInLaunch(LevelAccessor level, Entity entity, double y, CallbackInfo ci) {
        ArmageddonCompatUtil.damageNearbyBossTames(level, entity, 7.0D, 90.0F, 1.2D);
    }

    @Inject(method = "lambda$execute$28", at = @At("HEAD"), remap = false, require = 0)
    private static void tl$aimSonicAttackAtTame(LevelAccessor level, Entity entity, double x, double y, double z, CallbackInfo ci) {
        ArmageddonCompatUtil.lookAtNearestBossTameIfNoPlayers(level, entity, x, y, z, 12.0D);
    }
}
