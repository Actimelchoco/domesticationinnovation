package com.github.alexthe668.domesticationinnovation.server.item;

import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class CollarTagItem extends Item {

    public CollarTagItem() {
        super(new Item.Properties());
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return DIEnchantmentRegistry.isAllowedCollarTagEnchantment(enchantment);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        if (book == null || !book.isEnchanted()) {
            return false;
        }
        return book.getAllEnchantments().keySet().stream().allMatch(DIEnchantmentRegistry::isAllowedCollarTagEnchantment);
    }
}
