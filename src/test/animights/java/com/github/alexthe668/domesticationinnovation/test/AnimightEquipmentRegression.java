package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelSnapshots;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import dev.hexnowloading.dungeonnowloading.item.ScrapItem;
import dev.hexnowloading.dungeonnowloading.registry.DNLEnchantments;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class AnimightEquipmentRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static TamableAnimal companion(GameTestHelper helper) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("animights", "canito"));
        TamableAnimal tame = (TamableAnimal) type.create(helper.getLevel());
        tame.setOwnerUUID(UUID.randomUUID()); tame.setTame(true);
        var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        tame.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        TameRegistry.register(new TameData(tame));
        return tame;
    }

    private static long reserves(TameData data, net.minecraft.world.item.Item item) {
        return data.animightEquipmentInventory.stream().filter(stack -> stack.is(item)).mapToLong(ItemStack::getCount).sum();
    }

    private static ItemStack worn(ItemStack stack) {
        stack.setDamageValue(stack.getMaxDamage() - 1);
        stack.getOrCreateTag().putString("RegressionMarker", "keep-this-data");
        return stack;
    }

    @GameTest(template = "empty")
    public static void strongestSpareAndPreservedArmor(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            ItemStack damaged = worn(new ItemStack(Items.IRON_HELMET));
            tame.setItemSlot(EquipmentSlot.HEAD, damaged);
            ItemStack scrap = ScrapItem.ofOriginal(new ItemStack(Items.IRON_CHESTPLATE));
            var scrapItem = scrap.getItem();
            tame.setItemSlot(EquipmentSlot.CHEST, scrap);
            ItemStack plain = new ItemStack(Items.DIAMOND_HELMET);
            ItemStack enchanted = new ItemStack(Items.DIAMOND_HELMET);
            enchanted.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            for (ItemStack candidate : List.of(new ItemStack(Items.LEATHER_HELMET), plain, enchanted,
                    new ItemStack(Items.DIAMOND_CHESTPLATE), worn(new ItemStack(Items.NETHERITE_BOOTS)))) {
                AnimightEquipmentEvents.store(data, candidate);
            }
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET)
                    && tame.getItemBySlot(EquipmentSlot.HEAD).isEnchanted(), "strongest valid helmet must use Protection tie-break");
            check(tame.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "scrap must be stored and chest replaced");
            check(tame.getItemBySlot(EquipmentSlot.FEET).isEmpty(), "nearly broken spare must never be equipped");
            ItemStack savedDamage = data.animightEquipmentInventory.stream().filter(stack -> stack.is(Items.IRON_HELMET)).findFirst().orElseThrow();
            check(savedDamage.getDamageValue() == Items.IRON_HELMET.getMaxDamage() - 1
                    && savedDamage.getTag().getString("RegressionMarker").equals("keep-this-data"), "recovered armor must preserve durability and NBT");
            check(reserves(data, scrapItem) == 1, "transformed armor must be retained exactly once");
            AnimightEquipmentEvents.store(data, new ItemStack(Items.NETHERITE_HELMET));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getItemBySlot(EquipmentSlot.HEAD).is(Items.NETHERITE_HELMET)
                    && reserves(data, Items.DIAMOND_HELMET) == 2, "stronger spare must replace and retain healthy worn armor");
            TameData restored = TameData.fromTag(data.toTag());
            check(reserves(restored, scrapItem) == 1 && reserves(restored, Items.IRON_HELMET) == 1, "reserve must survive serialization");
            TameData snapshot = new TameData(tame);
            TameDuelSnapshots.copyPersistentStats(data, snapshot);
            savedDamage.setDamageValue(0);
            check(snapshot.animightEquipmentInventory.stream().filter(stack -> stack.is(Items.IRON_HELMET)).findFirst().orElseThrow().isDamaged(),
                    "duel reserve copy must not alias live stacks");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void killDropsAndLiveInventory(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            data.hungerAutopickup = false;
            tame.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
            var victim = EntityType.ZOMBIE.create(helper.getLevel());
            List<ItemEntity> drops = new ArrayList<>();
            for (var item : List.of(Items.LEATHER_BOOTS, Items.DIAMOND_BOOTS, Items.DIAMOND_CHESTPLATE, Items.APPLE)) {
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(item)));
            }
            var event = new LivingDropsEvent(victim, helper.getLevel().damageSources().mobAttack(tame), drops, 0, true);
            MinecraftForge.EVENT_BUS.post(event);
            check(tame.getItemBySlot(EquipmentSlot.FEET).is(Items.DIAMOND_BOOTS)
                    && tame.getItemBySlot(EquipmentSlot.CHEST).is(Items.DIAMOND_CHESTPLATE), "kill loot must equip strongest armor independently of food autopickup");
            check(reserves(data, Items.LEATHER_BOOTS) == 1 && reserves(data, Items.IRON_BOOTS) == 1
                    && drops.size() == 1 && drops.get(0).getItem().is(Items.APPLE),
                    "armor must be collected once while nonarmor remains dropped");
            var menu = new AnimightEquipmentEvents.ReserveContainer(tame, data);
            tame.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
            ItemStack added = new ItemStack(Items.DIAMOND_LEGGINGS);
            menu.setItem(26, added);
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getItemBySlot(EquipmentSlot.LEGS).is(Items.DIAMOND_LEGGINGS)
                    && reserves(data, Items.IRON_LEGGINGS) == 1, "player-added upgrade must swap and preserve old armor while menu is open");
            int original = menu.getContainerSize();
            menu.stopOpen(null);
            check(menu.getContainerSize() == original && reserves(data, Items.DIAMOND_LEGGINGS) == 0
                    && reserves(data, Items.IRON_LEGGINGS) == 1, "closing must not duplicate worn gear");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void breakProtectionAndFullInventory(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            ItemStack armor = worn(new ItemStack(Items.IRON_CHESTPLATE));
            armor.enchant(DNLEnchantments.BREAK_PROTECTION.get(), 1);
            tame.setItemSlot(EquipmentSlot.CHEST, armor);
            armor.hurtAndBreak(10000, tame, ignored -> { });
            check(armor.isEmpty() && data.animightEquipmentInventory.stream().anyMatch(AnimightEquipmentEvents::isRecoveryItem),
                    "real DNL break-protection spawn must be captured in reserve");
            ItemStack recovery = data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).findFirst().orElseThrow();
            check(ScrapItem.getOriginal(recovery).is(Items.IRON_CHESTPLATE), "captured scrap must retain original armor NBT");
            data.animightEquipmentInventory.clear();
            for (int i = 0; i < 27; i++) data.animightEquipmentInventory.add(new ItemStack(Items.DIAMOND_HELMET));
            ItemStack overflow = new ItemStack(Items.IRON_BOOTS);
            check(AnimightEquipmentEvents.store(data, overflow) == 0 && overflow.getCount() == 1, "full inventory must leave overflow untouched");
            ItemStack nearBreak = worn(new ItemStack(Items.IRON_HELMET));
            tame.setItemSlot(EquipmentSlot.HEAD, nearBreak);
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET) && reserves(data, Items.IRON_HELMET) == 1,
                    "replacement must free reserve capacity before storing worn gear");
            ItemStack threatened = new ItemStack(Items.IRON_BOOTS);
            threatened.setDamageValue(threatened.getMaxDamage() - 20);
            tame.setItemSlot(EquipmentSlot.FEET, threatened);
            data.animightEquipmentInventory.set(26, ItemStack.EMPTY);
            AnimightEquipmentEvents.onHurt(new LivingHurtEvent(tame, helper.getLevel().damageSources().mobAttack(tame), 80));
            check(tame.getItemBySlot(EquipmentSlot.FEET).isEmpty() && reserves(data, Items.IRON_BOOTS) == 1,
                    "incoming hit must rescue armor that would cross the safety threshold");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    private static ItemStack sealed(ItemStack original) {
        ItemStack seal = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("l2hostility", "sealed_item")));
        seal.getOrCreateTag().put("sealedItem", original.save(new CompoundTag()));
        seal.getOrCreateTag().putInt("sealTime", 200);
        return seal;
    }

    @GameTest(template = "empty")
    public static void killUnsealsAllItemsAndUpgradesArmor(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            tame.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            ItemStack original = new ItemStack(Items.NETHERITE_HELMET);
            original.setDamageValue(3);
            original.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 4);
            original.getOrCreateTag().putString("RegressionMarker", "original-armor");
            ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
            sword.enchant(Enchantments.SHARPNESS, 3);
            ItemStack scrap = ScrapItem.ofOriginal(new ItemStack(Items.IRON_BOOTS));
            var scrapItem = scrap.getItem();
            ItemStack malformed = sealed(ItemStack.EMPTY);
            for (ItemStack stack : List.of(sealed(original), sealed(sword), sealed(new ItemStack(Items.COD, 32)), malformed, scrap)) {
                AnimightEquipmentEvents.store(data, stack);
            }
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getItemBySlot(EquipmentSlot.HEAD).is(Items.DIAMOND_HELMET), "sealed armor must stay unusable before a kill");
            var victim = EntityType.ZOMBIE.create(helper.getLevel());
            // No drops event: a kill alone must restore the complete reserve.
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(victim, helper.getLevel().damageSources().mobAttack(tame)));
            ItemStack upgraded = tame.getItemBySlot(EquipmentSlot.HEAD);
            check(upgraded.is(Items.NETHERITE_HELMET) && upgraded.getDamageValue() == 3
                    && upgraded.isEnchanted() && upgraded.getTag().getString("RegressionMarker").equals("original-armor"),
                    "unsealed upgrade must preserve the original armor data");
            check(reserves(data, Items.DIAMOND_HELMET) == 1 && reserves(data, Items.DIAMOND_SWORD) == 1
                    && reserves(data, Items.COD) == 32, "kill must restore every valid seal, including nonarmor stacks");
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isSealedItem).count() == 1
                    && reserves(data, scrapItem) == 1, "malformed seal and DNL scrap must remain intact");
            check(AnimightEquipmentEvents.restoreSealedItems(data) == 0, "repeated restoration must not duplicate originals");
            data.animightEquipmentInventory.clear();
            for (int i = 0; i < 27; i++) data.animightEquipmentInventory.add(new ItemStack(Items.IRON_HELMET));
            data.animightEquipmentInventory.set(26, sealed(new ItemStack(Items.NETHERITE_CHESTPLATE)));
            tame.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim, helper.getLevel().damageSources().mobAttack(tame)));
            check(tame.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                    && reserves(data, Items.DIAMOND_CHESTPLATE) == 1, "full reserve must unseal in place and store displaced armor in the freed slot");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }
}
