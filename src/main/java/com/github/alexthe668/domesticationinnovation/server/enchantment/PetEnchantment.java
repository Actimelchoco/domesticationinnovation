package com.github.alexthe668.domesticationinnovation.server.enchantment;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

public class PetEnchantment extends Enchantment {

    private int levels;
    private int minXP;
    private String registryName;

    protected PetEnchantment(String name, Rarity r, int levels, int minXP) {
        super(r, DIEnchantmentRegistry.CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND});
        this.levels = levels;
        this.minXP = minXP;
        this.registryName = name;
    }

    public int getMinCost(int i) {
        return minXP + (i - 1) * minXP;
    }

    public int getMaxCost(int i) {
        return super.getMinCost(i) + 5;
    }

    public int getMaxLevel() {
        return levels;
    }


    protected boolean checkCompatibility(Enchantment enchantment) {
        return this != enchantment && DIEnchantmentRegistry.areCompatible(this, enchantment);
    }

    public boolean isTradeable() {
        return false;
    }

    public boolean isDiscoverable() {
        return false;
    }

    public boolean isAllowedOnBooks() {
        return false;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        if (stack.is(DIItemRegistry.COLLAR_TAG.get())) {
            return false;
        }
        return super.canEnchant(stack);
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        if (stack.is(DIItemRegistry.COLLAR_TAG.get())) {
            return false;
        }
        return super.canApplyAtEnchantingTable(stack) && DomesticationMod.CONFIG.isEnchantEnabled(this);
    }

    public String getName(){
        return registryName;
    }
}
