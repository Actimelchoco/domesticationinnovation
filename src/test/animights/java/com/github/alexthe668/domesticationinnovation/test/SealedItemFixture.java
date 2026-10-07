package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/** Registry fixture using L2's exact item ID and NBT format, without loading L2. */
@Mod.EventBusSubscriber(modid = DomesticationMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SealedItemFixture {
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ITEMS, helper -> helper.register(
                new ResourceLocation("l2hostility", "sealed_item"), new Item(new Item.Properties().stacksTo(1))));
        event.register(ForgeRegistries.Keys.ITEMS, helper -> helper.register(
                new ResourceLocation(DomesticationMod.MODID, "regression_damage_bow"), new BowItem(new Item.Properties().durability(384)) {
                    @Override public AbstractArrow customArrow(AbstractArrow arrow) {
                        arrow.setBaseDamage(arrow.getBaseDamage() + 10);
                        return arrow;
                    }
                }));
    }
}
