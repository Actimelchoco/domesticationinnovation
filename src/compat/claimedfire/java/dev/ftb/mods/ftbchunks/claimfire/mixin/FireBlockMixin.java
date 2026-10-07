package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimFireProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    // Forge-added method retains its name in production; it has no vanilla SRG mapping.
    @Inject(method = "tryCatchFire", at = @At("HEAD"), cancellable = true, remap = false)
    private void ftbc_noClaimedBurn(Level level, BlockPos pos, int chance, RandomSource random,
            int age, Direction face, CallbackInfo ci) {
        if (ClaimFireProtection.protectedAt(level, pos)) ci.cancel();
    }

    @Inject(method = "getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I",
            at = @At("HEAD"), cancellable = true)
    private void ftbc_noClaimedSpread(LevelReader level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (ClaimFireProtection.protectedAt(level, pos)) cir.setReturnValue(0);
    }
}
