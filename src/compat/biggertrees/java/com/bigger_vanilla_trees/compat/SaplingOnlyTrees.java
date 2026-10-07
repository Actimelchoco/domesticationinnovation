package com.bigger_vanilla_trees.compat;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AzaleaBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The replacement JAR keeps its larger features private to sapling growth. */
@Mod.EventBusSubscriber(modid = "bigger_vanilla_trees")
public final class SaplingOnlyTrees {
    private SaplingOnlyTrees() { }

    @SubscribeEvent
    public static void grow(SaplingGrowTreeEvent event) {
        if (event.getResult() == Event.Result.DENY || event.getFeature() == null
                || !(event.getLevel() instanceof ServerLevel level)) return;
        var block = level.getBlockState(event.getPos()).getBlock();
        if (!(block instanceof SaplingBlock) && !(block instanceof AzaleaBlock)) return;
        var original = event.getFeature().unwrapKey().orElse(null);
        if (original == null || !original.location().getNamespace().equals("minecraft")) return;
        var replacement = ResourceKey.create(Registries.CONFIGURED_FEATURE,
                new ResourceLocation("bigger_vanilla_trees", original.location().getPath()));
        level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolder(replacement).ifPresent(event::setFeature);
    }
}
