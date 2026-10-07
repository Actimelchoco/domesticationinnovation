package com.github.alexthe668.domesticationinnovation.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder("bigger_vanilla_trees")
@PrefixGameTestTemplate(false)
public final class BiggerTreesRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    @GameTest(template = "empty")
    public static void worldgenUsesVanillaDefinitions(GameTestHelper helper) {
        var features = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        TreeConfiguration vanilla = (TreeConfiguration) features.get(new ResourceLocation("minecraft", "oak")).config();
        TreeConfiguration bigger = (TreeConfiguration) features.get(new ResourceLocation("bigger_vanilla_trees", "oak")).config();
        check(vanilla.trunkPlacer.getTreeHeight(RandomSource.create(42)) < bigger.trunkPlacer.getTreeHeight(RandomSource.create(42)),
                "vanilla worldgen must retain its original smaller oak configuration");
        check(features.keySet().stream().filter(id -> id.getNamespace().equals("bigger_vanilla_trees")).count() == 31,
                "all 31 larger configured features must be independently registered");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void growthSelectionAndFallbacks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var features = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(pos, Blocks.OAK_SAPLING.defaultBlockState());
        for (ResourceLocation larger : features.keySet()) {
            if (!larger.getNamespace().equals("bigger_vanilla_trees")) continue;
            var vanilla = features.getHolder(net.minecraft.resources.ResourceKey.create(Registries.CONFIGURED_FEATURE,
                    new ResourceLocation("minecraft", larger.getPath()))).orElse(null);
            if (vanilla == null) continue; // The original mod includes unused/misspelled variants too.
            var event = new SaplingGrowTreeEvent(level, RandomSource.create(42), pos, vanilla);
            MinecraftForge.EVENT_BUS.post(event);
            check(event.getFeature().unwrapKey().orElseThrow().location().equals(larger), "sapling variant must map to its larger definition: " + larger);
        }
        var oak = features.getHolder(net.minecraft.resources.ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", "oak"))).orElseThrow();
        var denied = new SaplingGrowTreeEvent(level, RandomSource.create(42), pos, oak);
        denied.setResult(Event.Result.DENY); MinecraftForge.EVENT_BUS.post(denied);
        check(denied.getFeature() == oak && denied.getResult() == Event.Result.DENY, "denied growth must stay denied");
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var nonSapling = new SaplingGrowTreeEvent(level, RandomSource.create(42), pos, oak);
        MinecraftForge.EVENT_BUS.post(nonSapling);
        check(nonSapling.getFeature() == oak, "non-sapling placements must not change");
        level.setBlockAndUpdate(pos, Blocks.MANGROVE_PROPAGULE.defaultBlockState());
        var mangrove = features.getHolder(net.minecraft.resources.ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation("minecraft", "mangrove"))).orElseThrow();
        var propagule = new SaplingGrowTreeEvent(level, RandomSource.create(42), pos, mangrove);
        MinecraftForge.EVENT_BUS.post(propagule);
        check(propagule.getFeature().unwrapKey().orElseThrow().location().getNamespace().equals("bigger_vanilla_trees"), "mangrove propagules must use larger trees");
        var missing = new SaplingGrowTreeEvent(level, RandomSource.create(42), pos, null);
        MinecraftForge.EVENT_BUS.post(missing);
        check(missing.getFeature() == null, "missing features must remain safely missing");
        level.removeBlock(pos, false);
        helper.succeed();
    }

    public static final class Observer {
        final List<ResourceLocation> selected = new ArrayList<>();
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void grow(SaplingGrowTreeEvent event) {
            if (event.getFeature() != null) selected.add(event.getFeature().unwrapKey().orElseThrow().location());
        }
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void naturalAndBoneMealGrowth(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos natural = helper.absolutePos(new BlockPos(32, 2, 32)), boneMeal = natural.offset(64, 0, 0);
        Observer observer = new Observer();
        MinecraftForge.EVENT_BUS.register(observer);
        try {
            for (BlockPos pos : List.of(natural, boneMeal)) {
                var chunk = new net.minecraft.world.level.ChunkPos(pos);
                for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) level.getChunk(chunk.x + x, chunk.z + z);
                for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-8, 0, -8), pos.offset(8, 40, 8))) {
                    level.setBlock(clear, Blocks.AIR.defaultBlockState(), 2);
                }
                level.setBlockAndUpdate(pos.below(), Blocks.DIRT.defaultBlockState());
                level.setBlockAndUpdate(pos, Blocks.OAK_SAPLING.defaultBlockState().setValue(SaplingBlock.STAGE, 1));
            }
            SaplingBlock sapling = (SaplingBlock) Blocks.OAK_SAPLING;
            sapling.advanceTree(level, natural, level.getBlockState(natural), RandomSource.create(42));
            sapling.performBonemeal(level, RandomSource.create(42), boneMeal, level.getBlockState(boneMeal));
            check(observer.selected.size() == 2 && observer.selected.stream().allMatch(id -> id.getNamespace().equals("bigger_vanilla_trees")),
                    "both natural growth and bone meal must place the larger feature");
            check(level.getBlockState(natural).is(Blocks.OAK_LOG) && level.getBlockState(boneMeal).is(Blocks.OAK_LOG),
                    "both growth paths must actually produce trees: natural=" + level.getBlockState(natural)
                            + " boneMeal=" + level.getBlockState(boneMeal));
        } finally { MinecraftForge.EVENT_BUS.unregister(observer); }
        helper.succeed();
    }
}
