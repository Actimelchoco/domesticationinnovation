package dev.ftb.mods.ftbchunks.claimfire.mixin;

import dev.ftb.mods.ftbchunks.claimfire.ClaimMobTerrainProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.ovinter.mythsandlegends.entity.projectile.FrostSpikeEntity", remap = false)
public abstract class FrostSpikeClaimMixin {
    @Redirect(method = {"onHitBlock", "m_8060_"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", remap = true), require = 1)
    private boolean ftbc_noClaimedFrostSpike(Level level, BlockPos pos, BlockState state, int flags) {
        return ClaimMobTerrainProtection.setBlock(level, pos, state, flags);
    }
}
