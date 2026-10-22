package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimFireProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Check the destination, including when the boss stands outside a claim. */
@Pseudo
@Mixin(targets = {
        "net.mcreator.borninchaosv.procedures.LordPumpkinheadHeadPriObnovlieniiTikaSushchnostiProcedure",
        "net.mcreator.borninchaosv.procedures.LordPumpkinheadPriObnovlieniiTikaSushchnostiProcedure",
        "net.mcreator.borninchaosv.procedures.LordPumpkinheadTicProcedure",
        "net.mcreator.borninchaosv.procedures.FelsteedParProcedure",
        "net.mcreator.borninchaosv.procedures.WithoutHorsePProcedure"
}, remap = false)
public abstract class PumpkinTerrainClaimMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            remap = true), require = 1)
    private static boolean ftbc_protectPumpkinTerrain(LevelAccessor level, BlockPos pos, BlockState state, int flags) {
        return !ClaimFireProtection.protectedAt(level, pos) && level.setBlock(pos, state, flags);
    }
}
