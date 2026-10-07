package com.github.alexthe668.domesticationinnovation.test;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public final class ClaimFireRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    @GameTest(template = "empty")
    public static void claimedBoundaryProtection(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var source = level.getServer().createCommandSourceStack().withPermission(4);
        var team = FTBTeamsAPI.api().getManager().createServerTeam(source, "Fire regression", "", Color4I.WHITE);
        var data = FTBChunksAPI.api().getManager().getOrCreateData(team);
        BlockPos origin = helper.absolutePos(new BlockPos(64, 2, 64));
        BlockPos claimed = new BlockPos((origin.getX() >> 4) * 16, origin.getY(), origin.getZ());
        BlockPos unclaimed = claimed.west();
        var claimPos = new ChunkDimPos(level, claimed);
        check(data.claim(source, claimPos, false).isSuccess(), "test chunk must be claimed");
        var ignite = FireBlock.class.getDeclaredMethod("getIgniteOdds", LevelReader.class, BlockPos.class);
        var burn = FireBlock.class.getDeclaredMethod("tryCatchFire", Level.class, BlockPos.class, int.class, RandomSource.class, int.class, Direction.class);
        ignite.setAccessible(true); burn.setAccessible(true);
        try {
            level.setBlockAndUpdate(claimed, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(unclaimed, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(claimed.above(), Blocks.OAK_PLANKS.defaultBlockState());
            level.setBlockAndUpdate(unclaimed.above(), Blocks.OAK_PLANKS.defaultBlockState());
            check((int) ignite.invoke(Blocks.FIRE, level, claimed) == 0, "air inside claim must not ignite from adjacent fuel");
            check((int) ignite.invoke(Blocks.FIRE, level, unclaimed) > 0, "air outside claim must keep normal ignition odds");
            for (BlockPos pos : new BlockPos[]{claimed, unclaimed}) {
                level.setBlockAndUpdate(pos, Blocks.OAK_PLANKS.defaultBlockState());
                burn.invoke(Blocks.FIRE, level, pos, 1, RandomSource.create(42), 0, Direction.UP);
            }
            check(level.getBlockState(claimed).is(Blocks.OAK_PLANKS), "claimed fuel must survive fire");
            check(!level.getBlockState(unclaimed).is(Blocks.OAK_PLANKS), "unclaimed fuel must burn normally");
            for (BlockPos pos : new BlockPos[]{claimed, unclaimed}) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                var event = new BlockEvent.FluidPlaceBlockEvent(level, pos, unclaimed, Blocks.FIRE.defaultBlockState());
                MinecraftForge.EVENT_BUS.post(event);
                check(event.getNewState().is(pos.equals(claimed) ? Blocks.AIR : Blocks.FIRE), "lava ignition must respect destination claim");
                var stone = new BlockEvent.FluidPlaceBlockEvent(level, pos, unclaimed, Blocks.STONE.defaultBlockState());
                MinecraftForge.EVENT_BUS.post(stone);
                check(stone.getNewState().is(Blocks.STONE), "non-fire fluid placement must remain unchanged");
            }
        } finally {
            data.unclaim(source, claimPos, false);
        }
        helper.succeed();
    }
}
