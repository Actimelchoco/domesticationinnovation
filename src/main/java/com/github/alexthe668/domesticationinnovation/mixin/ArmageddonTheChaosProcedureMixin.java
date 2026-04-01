package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.ArmageddonCompatUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.armageddonmod.procedures.TheChaosOnEntityTickUpdateProcedure")
public abstract class ArmageddonTheChaosProcedureMixin {

    private static final ResourceLocation CHAOS_SIGIL_CIRCLE_ID = new ResourceLocation("armageddon_mod", "chaos_sigil_circle");

    @Inject(method = "lambda$execute$11", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$spawnSigilsUnderTames(Entity entity, LevelAccessor level, CallbackInfo ci) {
        ArmageddonCompatUtil.spawnEntityAtNearbyBossTames(level, entity, 12.0D, CHAOS_SIGIL_CIRCLE_ID);
    }

    @Inject(method = "lambda$execute$7", at = @At("TAIL"), remap = false, require = 0)
    private static void tl$strikeNearbyTames(Entity entity, LevelAccessor level, CallbackInfo ci) {
        ArmageddonCompatUtil.repeatLightningDamageNearbyBossTames(level, entity, 12.0D, 5, 20, 70.0F);
    }
}
