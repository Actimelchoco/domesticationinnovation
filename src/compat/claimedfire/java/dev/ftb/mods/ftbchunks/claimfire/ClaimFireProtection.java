package dev.ftb.mods.ftbchunks.claimfire;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelReader;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Claim presence is authoritative: no owner, ally or admin bypass for fire spread. */
@Mod.EventBusSubscriber(modid = "ftbchunks")
public final class ClaimFireProtection {
    private ClaimFireProtection() { }

    public static boolean protectedAt(LevelReader level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        var api = FTBChunksAPI.api();
        return api.isManagerLoaded() && api.getManager().getChunk(new ChunkDimPos(serverLevel, pos)) != null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fluidIgnition(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getNewState().is(BlockTags.FIRE) && event.getLevel() instanceof ServerLevel level
                && protectedAt(level, event.getPos())) {
            event.setNewState(event.getOriginalState());
        }
    }
}
