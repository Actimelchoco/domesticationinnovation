package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.DrumBlockEntity;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.LoadedChestDrums;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class ChestDrumRangeRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void drum(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, DIBlockRegistry.DRUM.get().defaultBlockState());
        ((DrumBlockEntity) level.getBlockEntity(pos)).onLoad();
    }

    private static LoadedChestDrums.Chest chest(ServerLevel level, BlockPos pos) {
        return LoadedChestDrums.chests(level).stream().filter(chest -> chest.pos().equals(pos)).findFirst().orElseThrow();
    }

    @GameTest(template = "empty")
    public static void adjacentDrumsExpandRange(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos base = pos.below();
        List<BlockPos> placed = new ArrayList<>();
        try {
            drum(level, base); placed.add(base);
            level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState()); placed.add(pos);
            check(chest(level, pos).range() == 25 && chest(level, pos).height() == 5, "base range must stay 25/5");
            int count = 0;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos extra = base.relative(direction);
                drum(level, extra); placed.add(extra);
                count++;
                var current = chest(level, pos);
                check(current.extraDrums() == count && current.range() == 25 + count * 10
                        && current.height() == 5 + count * 2, "each adjacent drum must add 10/2");
            }
            for (BlockPos ignored : List.of(base.offset(1, 0, 1), base.offset(2, 0, 0), base.below())) {
                drum(level, ignored); placed.add(ignored);
            }
            var expanded = chest(level, pos);
            check(expanded.range() == 65 && expanded.height() == 13, "diagonals, chains, and vertical drums must not amplify");
            check(expanded.inRange(pos.offset(65, 13, -65)), "expanded corner must be inclusive");
            check(!expanded.inRange(pos.offset(66, 0, 0)) && !expanded.inRange(pos.offset(0, 14, 0)),
                    "points beyond either expanded limit must be excluded");
            level.removeBlock(base.west(), false);
            check(chest(level, pos).range() == 55 && chest(level, pos).height() == 11, "removing an amplifier must shrink the range");
            BlockPos other = pos.offset(50, 10, 0);
            level.getChunk(other);
            drum(level, other.below()); placed.add(other.below());
            level.setBlockAndUpdate(other, Blocks.CHEST.defaultBlockState()); placed.add(other);
            check(LoadedChestDrums.statuses(level).stream().anyMatch(status -> status.chest().pos().equals(other)
                    && pos.equals(status.blocker())), "expanded active range must deactivate a second setup inside it");
            for (Direction direction : Direction.Plane.HORIZONTAL) level.removeBlock(base.relative(direction), false);
            check(LoadedChestDrums.statuses(level).stream().anyMatch(status -> status.chest().pos().equals(other)
                    && status.active()), "shrinking range must reactivate a setup that is now outside it");
        } finally {
            for (BlockPos block : placed) level.removeBlock(block, false);
        }
        helper.succeed();
    }

    private static Wolf tame(ServerLevel level, BlockPos pos) {
        Wolf wolf = new Wolf(EntityType.WOLF, level) {
            @Override public boolean isAlwaysTicking() { return true; }
        };
        wolf.setNoAi(true);
        wolf.setNoGravity(true);
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        check(level.addFreshEntity(wolf), "feeding fixture must spawn");
        TameRegistry.register(new TameData(wolf));
        return wolf;
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void amplifiedDrumFeedsExpandedBoundary(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        BlockPos amplifier = pos.below().east();
        List<Wolf> pets = new ArrayList<>();
        java.util.Set<Long> forced = new java.util.HashSet<>();
        for (BlockPos target : List.of(pos, pos.offset(35, 7, 35), pos.offset(36, 0, 0), pos.offset(0, 8, 0))) {
            var chunk = new net.minecraft.world.level.ChunkPos(target);
            if (!level.getForcedChunks().contains(chunk.toLong()) && forced.add(chunk.toLong())) {
                level.setChunkForced(chunk.x, chunk.z, true);
            }
            level.getChunk(target);
        }
        drum(level, pos.below());
        drum(level, amplifier);
        level.setBlockAndUpdate(pos, Blocks.CHEST.defaultBlockState());
        helper.runAfterDelay(10, () -> {
            try {
                pets.add(tame(level, pos.offset(35, 7, 35)));
                pets.add(tame(level, pos.offset(36, 0, 0)));
                pets.add(tame(level, pos.offset(0, 8, 0)));
                ChestBlockEntity container = (ChestBlockEntity) level.getBlockEntity(pos);
                container.setItem(0, new ItemStack(Items.COOKED_BEEF, 64));
                var feed = TameCommands.class.getDeclaredMethod("feedLoadedChestDrums", net.minecraft.server.MinecraftServer.class);
                feed.setAccessible(true);
                feed.invoke(null, level.getServer());
                check(!TameRegistry.get(pets.get(0).getUUID()).hungerInventory.isEmpty(), "expanded X/Z/Y boundary must receive food");
                check(TameRegistry.get(pets.get(1).getUUID()).hungerInventory.isEmpty()
                        && TameRegistry.get(pets.get(2).getUUID()).hungerInventory.isEmpty(), "outside expanded range must not receive food");
                TameRegistry.get(pets.get(0).getUUID()).hungerInventory.clear();
                level.removeBlock(amplifier, false);
                feed.invoke(null, level.getServer());
                check(TameRegistry.get(pets.get(0).getUUID()).hungerInventory.isEmpty(), "removing amplifier must stop feeding the expanded boundary");
                helper.succeed();
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            } finally {
                for (Wolf pet : pets) { TameRegistry.remove(pet.getUUID()); pet.discard(); }
                level.removeBlock(pos, false);
                level.removeBlock(pos.below(), false);
                level.removeBlock(amplifier, false);
                for (long packed : forced) {
                    var chunk = new net.minecraft.world.level.ChunkPos(packed);
                    level.setChunkForced(chunk.x, chunk.z, false);
                }
            }
        });
    }
}
