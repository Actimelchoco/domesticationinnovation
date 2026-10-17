package dev.ftb.mods.ftbchunks.claimfire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Called only by Amarok's two terrain-changing abilities, not by ordinary block placement. */
public final class ClaimMobTerrainProtection {
    private ClaimMobTerrainProtection() { }

    public static boolean setBlock(Level level, BlockPos pos, BlockState state, int flags) {
        return !ClaimFireProtection.protectedAt(level, pos) && level.setBlock(pos, state, flags);
    }
}
