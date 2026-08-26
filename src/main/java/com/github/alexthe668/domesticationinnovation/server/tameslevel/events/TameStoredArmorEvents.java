package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.UUID;

public final class TameStoredArmorEvents {
    private static final UUID ARMOR_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a1");
    private static final UUID TOUGHNESS_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a2");
    private static final UUID KNOCKBACK_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a3");
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private TameStoredArmorEvents() { }

    public static void sync(LivingEntity tame, TameData data) {
        if (tame == null || data == null || tame.level().isClientSide) return;
        apply(tame, Attributes.ARMOR, ARMOR_ID, total(data, Attributes.ARMOR));
        apply(tame, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, total(data, Attributes.ARMOR_TOUGHNESS));
        apply(tame, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, total(data, Attributes.KNOCKBACK_RESISTANCE));
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity tame = event.getEntity();
        if (!TameEntityAdapter.isTame(tame) || tame.level().isClientSide) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || data.armorInventory.isEmpty()) return;
        List<ItemStack> armor = data.armorInventory.stream().filter(stack -> stack != null && !stack.isEmpty()).toList();
        if (armor.isEmpty()) return;
        int protection = Math.min(20, EnchantmentHelper.getDamageProtection(armor, event.getSource()));
        if (protection > 0) event.setAmount(event.getAmount() * (1.0F - protection / 25.0F));
        int durabilityDamage = Math.max(1, (int) (event.getAmount() / 4.0F));
        for (ItemStack stack : armor) stack.hurtAndBreak(durabilityDamage, tame, ignored -> { });
        data.armorInventory.removeIf(stack -> stack == null);
        while (data.armorInventory.size() < 4) data.armorInventory.add(ItemStack.EMPTY);
        sync(tame, data);
        TameRegistry.markDirty();
    }

    private static double total(TameData data, Attribute attribute) {
        double result = 0.0D;
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack stack = data.armorInventory.get(i);
            if (stack == null || stack.isEmpty()) continue;
            for (AttributeModifier modifier : stack.getAttributeModifiers(SLOTS[i]).get(attribute)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) result += modifier.getAmount();
            }
        }
        return result;
    }

    private static void apply(LivingEntity tame, Attribute attribute, UUID id, double amount) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Double.compare(existing.getAmount(), amount) == 0) return;
        if (existing != null) instance.removeModifier(id);
        if (amount != 0.0D) {
            instance.addTransientModifier(new AttributeModifier(id, "TamesLevel stored armor", amount, AttributeModifier.Operation.ADDITION));
        }
    }
}
