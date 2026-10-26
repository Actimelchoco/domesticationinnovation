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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
            for (int i = 0; i < AnimightEquipmentEvents.INVENTORY_SIZE; i++) data.animightEquipmentInventory.add(new ItemStack(Items.DIAMOND_HELMET));
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
            check(upgraded.is(Items.NETHERITE_HELMET) && upgraded.getDamageValue() == 0
                    && upgraded.isEnchanted() && upgraded.getTag().getString("RegressionMarker").equals("original-armor"),
                    "unsealed upgrade must preserve the original armor data");
            check(reserves(data, Items.DIAMOND_HELMET) == 1 && tame.getMainHandItem().is(Items.DIAMOND_SWORD)
                    && reserves(data, Items.COD) == 32, "kill must restore every valid seal and equip the restored weapon");
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isSealedItem).count() == 1
                    && reserves(data, scrapItem) == 0 && tame.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS), "malformed seal remains intact; valid DNL scrap is restored");
            check(AnimightEquipmentEvents.restoreRecoveryItems(tame, data) == 0, "repeated restoration must not duplicate originals");
            data.animightEquipmentInventory.clear();
            for (int i = 0; i < AnimightEquipmentEvents.INVENTORY_SIZE; i++) data.animightEquipmentInventory.add(new ItemStack(Items.IRON_HELMET));
            data.animightEquipmentInventory.set(26, sealed(new ItemStack(Items.NETHERITE_CHESTPLATE)));
            tame.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
            AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim, helper.getLevel().damageSources().mobAttack(tame)));
            check(tame.getItemBySlot(EquipmentSlot.CHEST).is(Items.NETHERITE_CHESTPLATE)
                    && reserves(data, Items.DIAMOND_CHESTPLATE) == 1, "full reserve must unseal in place and store displaced armor in the freed slot");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weaponsUsePerHitDamageAndPreserveReplacements(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            tame.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
            tame.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            var menu = new AnimightEquipmentEvents.ReserveContainer(tame, data);
            check(menu.canPlaceItem(0, new ItemStack(Items.DIAMOND_SWORD))
                    && menu.canPlaceItem(0, new ItemStack(Items.BOW)) && !menu.canPlaceItem(0, new ItemStack(Items.APPLE)),
                    "equipment inventory must accept weapons and bows but reject unrelated items");
            ItemStack enchanted = new ItemStack(Items.IRON_SWORD);
            enchanted.enchant(Enchantments.SHARPNESS, 5);
            menu.setItem(0, enchanted);
            menu.setItem(1, new ItemStack(Items.DIAMOND_SWORD));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.IRON_SWORD) && tame.getMainHandItem().isEnchanted(),
                    "Sharpness must let lower-tier weapon beat plain diamond by hit damage");
            menu.setItem(2, new ItemStack(Items.DIAMOND_AXE));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.DIAMOND_AXE), "slower axe must beat swords when it hits harder");
            check(reserves(data, Items.IRON_SWORD) == 2 && reserves(data, Items.DIAMOND_SWORD) == 1,
                    "each displaced weapon must be retained once");
            // Apply vanilla held attributes to verify that comparison doesn't double count the current weapon.
            tame.getAttributes().addTransientAttributeModifiers(tame.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND));
            double diamondDamage = AnimightEquipmentEvents.weaponDamage(tame, new ItemStack(Items.DIAMOND_SWORD));
            check(Math.abs(diamondDamage - 8.0D) < 0.001D, "candidate must replace current weapon modifiers, not add to them");
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.DIAMOND_AXE) && reserves(data, Items.IRON_SWORD) == 2,
                    "unchanged reserve must not repeatedly swap or duplicate weapons");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void bowPowerAndCrossTypeRanking(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            tame.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
            tame.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            tame.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.SHIELD));
            ItemStack weaker = new ItemStack(Items.BOW);
            weaker.enchant(Enchantments.POWER_ARROWS, 1);
            ItemStack stronger = new ItemStack(Items.BOW);
            stronger.enchant(Enchantments.POWER_ARROWS, 5);
            AnimightEquipmentEvents.store(data, weaker);
            AnimightEquipmentEvents.store(data, stronger);
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.BOW)
                    && net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, tame.getMainHandItem()) == 5,
                    "best Power bow must beat weaker bow and weaker melee weapon");
            check(tame.getOffhandItem().is(Items.SHIELD), "automatic weapon selection must preserve the shield");
            AnimightEquipmentEvents.store(data, new ItemStack(Items.NETHERITE_AXE));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.NETHERITE_AXE) && reserves(data, Items.BOW) == 2,
                    "harder-hitting melee weapon must replace bow and retain both bows");
            tame.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.BOW));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.NETHERITE_AXE) && tame.getOffhandItem().isEmpty() && reserves(data, Items.BOW) == 3,
                    "weaker offhand bow must move to reserve instead of overriding selected melee attack");
            var customBow = ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesticationinnovation", "regression_damage_bow"));
            tame.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(customBow));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(customBow) && tame.getOffhandItem().isEmpty(),
                    "strongest offhand bow must move to mainhand and customArrow damage must participate in ranking");
            var victim = EntityType.ZOMBIE.create(helper.getLevel());
            victim.moveTo(tame.getX() + 5, tame.getY(), tame.getZ());
            ((net.minecraft.world.entity.monster.RangedAttackMob) tame).performRangedAttack(victim, 1.0F);
            var arrows = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.projectile.AbstractArrow.class,
                    tame.getBoundingBox().inflate(2), arrow -> arrow.getOwner() == tame);
            check(arrows.size() == 1 && arrows.get(0).getBaseDamage() >= 11,
                    "native ranged attack must actually use the selected bow's customArrow damage");
            arrows.forEach(net.minecraft.world.entity.Entity::discard);
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void weaponLootWearSealsAndFullReserve(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);
        TameData data = TameRegistry.get(tame.getUUID());
        try {
            tame.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2);
            data.hungerAutopickup = false;
            tame.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
            var victim = EntityType.ZOMBIE.create(helper.getLevel());
            List<ItemEntity> drops = new ArrayList<>();
            for (var item : List.of(Items.DIAMOND_SWORD, Items.IRON_AXE, Items.BOW, Items.APPLE)) {
                drops.add(new ItemEntity(helper.getLevel(), 0, 0, 0, new ItemStack(item)));
            }
            MinecraftForge.EVENT_BUS.post(new LivingDropsEvent(victim, helper.getLevel().damageSources().mobAttack(tame), drops, 0, true));
            check(tame.getMainHandItem().is(Items.IRON_AXE) && drops.size() == 1 && drops.get(0).getItem().is(Items.APPLE),
                    "kill must collect weapons/bows and choose the hardest hitter independently of food pickup");
            tame.getMainHandItem().setDamageValue(tame.getMainHandItem().getMaxDamage() - 1);
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.DIAMOND_SWORD) && reserves(data, Items.IRON_AXE) == 1,
                    "nearly broken weapon must be retained and replaced with a usable spare");
            ItemStack original = new ItemStack(Items.NETHERITE_AXE);
            original.enchant(Enchantments.SHARPNESS, 4);
            original.getOrCreateTag().putString("RegressionMarker", "saved-weapon");
            tame.setItemSlot(EquipmentSlot.MAINHAND, sealed(original));
            MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(victim, helper.getLevel().damageSources().mobAttack(tame)));
            check(tame.getMainHandItem().is(Items.NETHERITE_AXE) && tame.getMainHandItem().isEnchanted()
                    && tame.getMainHandItem().getTag().getString("RegressionMarker").equals("saved-weapon"),
                    "kill must store/unseal held weapon and equip its restored original with NBT intact");
            data.animightEquipmentInventory.clear();
            for (int i = 0; i < AnimightEquipmentEvents.INVENTORY_SIZE; i++) data.animightEquipmentInventory.add(new ItemStack(Items.IRON_SWORD));
            data.animightEquipmentInventory.set(26, new ItemStack(Items.NETHERITE_AXE));
            tame.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WOODEN_SWORD));
            AnimightEquipmentEvents.maintain(tame, data);
            check(tame.getMainHandItem().is(Items.NETHERITE_AXE) && reserves(data, Items.WOODEN_SWORD) == 1,
                    "full reserve must free replacement slot before retaining the old weapon");
            ItemStack breaking = worn(new ItemStack(Items.DIAMOND_SWORD));
            data.animightEquipmentInventory.set(25, ItemStack.EMPTY);
            breaking.enchant(DNLEnchantments.BREAK_PROTECTION.get(), 1);
            breaking.hurtAndBreak(10000, tame, ignored -> {});
            check(breaking.isEmpty() && data.animightEquipmentInventory.stream().anyMatch(AnimightEquipmentEvents::isRecoveryItem),
                    "DNL weapon scrap must be captured just like armor scrap");
        } finally { TameRegistry.remove(tame.getUUID()); tame.discard(); }
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void largerReserveAndOptInWornArmorDisposal(GameTestHelper helper) {
        var tame=companion(helper);helper.getLevel().addFreshEntity(tame);var data=TameRegistry.get(tame.getUUID());
        try {
            var inventory=new AnimightEquipmentEvents.ReserveContainer(tame,data);
            check(inventory.getContainerSize()==54,"equipment reserve doubles to 54 slots");
            inventory.setItem(53,new ItemStack(Items.IRON_SWORD));
            check(TameData.fromTag(data.toTag()).animightEquipmentInventory.get(53).is(Items.IRON_SWORD),"last new slot survives saves");
            data.animightEquipmentInventory.clear();
            ItemStack weak=worn(new ItemStack(Items.LEATHER_BOOTS)),strong=worn(new ItemStack(Items.DIAMOND_CHESTPLATE));
            data.animightEquipmentInventory.add(weak);data.animightEquipmentInventory.add(strong);
            check(!com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.discardWornArmor(data.ownerUUID),"disposal defaults false");
            AnimightEquipmentEvents.maintain(tame,data);check(reserves(data,Items.LEATHER_BOOTS)==1,"default setting retains worn gear");
            com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.setDiscardWornArmor(data.ownerUUID,true);
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.saveAll().get(data.ownerUUID).getBoolean("discardWornArmor"),"setting persists per owner");
            AnimightEquipmentEvents.maintain(tame,data);
            check(reserves(data,Items.LEATHER_BOOTS)==0&&reserves(data,Items.DIAMOND_CHESTPLATE)==1,"only weakest worn piece discarded");
            var drops=helper.getLevel().getEntitiesOfClass(ItemEntity.class,tame.getBoundingBox().inflate(2),e->e.getItem().is(Items.LEATHER_BOOTS));
            check(drops.stream().anyMatch(e->e.getItem().getTag()!=null&&e.getItem().getTag().getString("RegressionMarker").equals("keep-this-data")),"discarded armor drops intact with NBT");
            AnimightEquipmentEvents.maintain(tame,data);check(reserves(data,Items.DIAMOND_CHESTPLATE)==1,"repeated checks respect discard throttle");
        } finally {
            com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.setDiscardWornArmor(data.ownerUUID,false);
            TameRegistry.remove(tame.getUUID());tame.discard();
        }
        helper.succeed();
    }
    @GameTest(template="empty")
    public static void healthyReserveArmorPreventsDisposal(GameTestHelper helper) {
        var tame=companion(helper);var data=TameRegistry.get(tame.getUUID());
        try {
            com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.setDiscardWornArmor(data.ownerUUID,true);
            data.animightEquipmentInventory.add(worn(new ItemStack(Items.LEATHER_BOOTS)));
            data.animightEquipmentInventory.add(new ItemStack(Items.IRON_CHESTPLATE));
            check(!AnimightEquipmentEvents.discardWornReserveArmor(tame,data)&&reserves(data,Items.LEATHER_BOOTS)==1,"a healthy stored armor piece prevents disposal");
        } finally {
            com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.setDiscardWornArmor(data.ownerUUID,false);
            TameRegistry.remove(tame.getUUID());tame.discard();
        }
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void xpRepairsEveryItemWithoutSpendingXp(GameTestHelper helper) {
        TamableAnimal tame = companion(helper);TameData data = TameRegistry.get(tame.getUUID());
        try {
            data.level=20;data.xp=0;
            ItemStack helmet=new ItemStack(Items.IRON_HELMET);helmet.setDamageValue(100);
            ItemStack sword=new ItemStack(Items.DIAMOND_SWORD);sword.setDamageValue(100);sword.enchant(Enchantments.SHARPNESS,3);
            sword.getOrCreateTag().putString("RegressionMarker","xp-weapon");
            tame.setItemSlot(EquipmentSlot.HEAD,helmet);tame.setItemSlot(EquipmentSlot.MAINHAND,sword);
            ItemStack boots=new ItemStack(Items.IRON_BOOTS);boots.setDamageValue(100);
            ItemStack bow=new ItemStack(Items.BOW);bow.setDamageValue(100);
            ItemStack almost=new ItemStack(Items.IRON_CHESTPLATE);almost.setDamageValue(3);
            for(var stack:List.of(boots,bow,almost))AnimightEquipmentEvents.store(data,stack);
            var reserveBoots=data.animightEquipmentInventory.stream().filter(st->st.is(Items.IRON_BOOTS)).findFirst().orElseThrow();
            var reserveBow=data.animightEquipmentInventory.stream().filter(st->st.is(Items.BOW)).findFirst().orElseThrow();
            com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem.grantXP(tame,data,5);
            check(data.xp==5,"all awarded XP must still go to the tame");
            check(helmet.getDamageValue()==90 && sword.getDamageValue()==90 && reserveBoots.getDamageValue()==90 && reserveBow.getDamageValue()==90,
                    "every item gets full allowance: type="+data.type+", helmet="+helmet.getDamageValue()+", sword="+sword.getDamageValue()+", boots="+reserveBoots.getDamageValue()+", bow="+reserveBow.getDamageValue());
            check(data.animightEquipmentInventory.stream().filter(st->st.is(Items.IRON_CHESTPLATE)).findFirst().orElseThrow().getDamageValue()==0,"repair must stop at full durability");
            check(sword.isEnchanted()&&sword.getTag().getString("RegressionMarker").equals("xp-weapon"),"repair preserves NBT");
            com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem.grantXP(tame,data,-1);
            check(sword.getDamageValue()==90&&data.xp==4,"XP loss does not repair gear");
            AnimightEquipmentEvents.repairOnXp(tame,data,Integer.MAX_VALUE);
            check(sword.getDamageValue()==0&&reserveBow.getDamageValue()==0,"large XP awards must not overflow");
        }finally{TameRegistry.remove(tame.getUUID());tame.discard();}helper.succeed();
    }

    @GameTest(template="empty")
    public static void xpRestoresScrapsWithoutKill(GameTestHelper helper) {
        TamableAnimal tame=companion(helper);TameData data=TameRegistry.get(tame.getUUID());
        try {
            ItemStack original=worn(new ItemStack(Items.IRON_HELMET));
            original.enchant(Enchantments.ALL_DAMAGE_PROTECTION,3);
            tame.setItemSlot(EquipmentSlot.HEAD,ScrapItem.ofOriginal(original));
            AnimightEquipmentEvents.store(data,ScrapItem.ofOriginal(worn(new ItemStack(Items.DIAMOND_SWORD))));
            ItemStack malformed=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("dungeonnowloading:item_scraps")));
            AnimightEquipmentEvents.store(data,malformed);
            AnimightEquipmentEvents.repairOnXp(tame,data,0);
            check(AnimightEquipmentEvents.isRecoveryItem(tame.getItemBySlot(EquipmentSlot.HEAD)),"zero XP must not restore scraps");
            AnimightEquipmentEvents.repairOnXp(tame,data,1);
            ItemStack helmet=tame.getItemBySlot(EquipmentSlot.HEAD);
            check(helmet.is(Items.IRON_HELMET)&&!helmet.isDamaged()&&helmet.isEnchanted()
                    &&helmet.getTag().getString("RegressionMarker").equals("keep-this-data"),"XP restores equipped scrap with NBT intact");
            check(data.animightEquipmentInventory.stream().anyMatch(st->st.is(Items.DIAMOND_SWORD)&&!st.isDamaged()),"XP restores reserve scraps without a kill");
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==1,"malformed scrap remains intact");
            var snapshot=new CompoundTag();var armor=new net.minecraft.nbt.ListTag();
            for(int i=0;i<4;i++)armor.add((i==3?ScrapItem.ofOriginal(original):ItemStack.EMPTY).save(new CompoundTag()));
            snapshot.put("ArmorItems",armor);data.entitySnapshot=snapshot;
            AnimightEquipmentEvents.repairOnXp(null,data,1);
            check(ItemStack.of(snapshot.getList("ArmorItems",10).getCompound(3)).is(Items.IRON_HELMET)
                    &&ItemStack.of(snapshot.getCompound("head")).is(Items.IRON_HELMET),"unloaded XP restores scraps and synchronizes native armor tags");
        } finally {TameRegistry.remove(tame.getUUID());tame.discard();}
        helper.succeed();
    }

    @GameTest(template="empty")
    public static void killRestoresEquippedScrapsAndPreservesMalformedTokens(GameTestHelper helper){
        TamableAnimal tame=companion(helper);TameData data=TameRegistry.get(tame.getUUID());
        try{
            ItemStack original=worn(new ItemStack(Items.NETHERITE_SWORD));original.enchant(Enchantments.SHARPNESS,4);
            tame.setItemSlot(EquipmentSlot.MAINHAND,ScrapItem.ofOriginal(original));
            ItemStack malformed=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("dungeonnowloading:item_scraps")));
            AnimightEquipmentEvents.store(data,malformed);
            var victim=EntityType.ZOMBIE.create(helper.getLevel());
            AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,helper.getLevel().damageSources().mobAttack(tame)));
            check(tame.getMainHandItem().is(Items.NETHERITE_SWORD)&&tame.getMainHandItem().getDamageValue()==0&&tame.getMainHandItem().isEnchanted(),"kill alone restores equipped DNL weapon scrap at full durability");
            check(tame.getMainHandItem().getTag().getString("RegressionMarker").equals("keep-this-data"),"DNL restoration preserves custom NBT");
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==1,"malformed scrap is retained");
            check(AnimightEquipmentEvents.restoreRecoveryItems(tame,data)==0,"repeat recovery never duplicates items");
            var snapshot=new net.minecraft.nbt.CompoundTag();var hands=new net.minecraft.nbt.ListTag();
            ItemStack saved=new ItemStack(Items.BOW);saved.setDamageValue(50);hands.add(saved.save(new CompoundTag()));snapshot.put("HandItems",hands);data.entitySnapshot=snapshot;
            AnimightEquipmentEvents.repairOnXp(null,data,4);
            check(ItemStack.of(data.entitySnapshot.getList("HandItems",10).getCompound(0)).getDamageValue()==42,"unloaded ranked reward repairs saved native equipment");
        }finally{TameRegistry.remove(tame.getUUID());tame.discard();}helper.succeed();
    }

    @GameTest(template="empty")
    public static void wornArmorAlwaysBecomesScrapWithoutEnchantment(GameTestHelper helper){
        TamableAnimal tame=companion(helper);TameData data=TameRegistry.get(tame.getUUID());
        try{
            var slots=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
            var items=List.of(Items.IRON_HELMET,Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_BOOTS);
            for(int i=0;i<slots.size();i++){
                ItemStack armor=worn(new ItemStack(items.get(i)));armor.enchant(Enchantments.ALL_DAMAGE_PROTECTION,2);
                tame.setItemSlot(slots.get(i),armor);
                armor.hurtAndBreak(10000,tame,ignored->{});
                check(armor.isEmpty()&&tame.getItemBySlot(slots.get(i)).isEmpty(),"broken worn armor must leave equipment slot");
            }
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==4,"each worn piece becomes exactly one DNL scrap without Break Protection");
            for(var token:data.animightEquipmentInventory){
                if(token.isEmpty())continue;var original=ScrapItem.getOriginal(token);
                check(original.isEnchanted()&&original.getTag().getString("RegressionMarker").equals("keep-this-data"),"scraps preserve original enchantments and custom NBT");
            }
            var victim=EntityType.ZOMBIE.create(helper.getLevel());
            AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,helper.getLevel().damageSources().mobAttack(tame)));
            for(int i=0;i<slots.size();i++)check(tame.getItemBySlot(slots.get(i)).is(items.get(i))&&!tame.getItemBySlot(slots.get(i)).isDamaged(),"next kill restores each scrap to fully repaired armor");
            var wolf=EntityType.WOLF.create(helper.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
            ItemStack ordinary=worn(new ItemStack(Items.IRON_CHESTPLATE));wolf.setItemSlot(EquipmentSlot.CHEST,ordinary);ordinary.hurtAndBreak(10000,wolf,ignored->{});
            check(ordinary.isEmpty(),"non-Animights keep normal armor durability behavior");wolf.discard();
        }finally{TameRegistry.remove(tame.getUUID());tame.discard();}helper.succeed();
    }

    @GameTest(template="empty")
    public static void fullReserveRetainsPendingScrapWithoutDropping(GameTestHelper helper){
        TamableAnimal tame=companion(helper);TameData data=TameRegistry.get(tame.getUUID());
        try{
            tame.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.NETHERITE_HELMET));
            for(int i=0;i<AnimightEquipmentEvents.INVENTORY_SIZE;i++)AnimightEquipmentEvents.store(data,new ItemStack(Items.DIAMOND_HELMET));
            ItemStack armor=worn(new ItemStack(Items.IRON_BOOTS));armor.enchant(DNLEnchantments.BREAK_PROTECTION.get(),1);tame.setItemSlot(EquipmentSlot.FEET,armor);
            armor.hurtAndBreak(10000,tame,ignored->{});
            check(armor.isEmpty()&&AnimightEquipmentEvents.isRecoveryItem(tame.getItemBySlot(EquipmentSlot.FEET)),"full inventory retains one pending scrap in equipment slot");
            AnimightEquipmentEvents.maintain(tame,data);
            check(AnimightEquipmentEvents.isRecoveryItem(tame.getItemBySlot(EquipmentSlot.FEET))&&data.animightEquipmentInventory.stream().noneMatch(AnimightEquipmentEvents::isRecoveryItem),"periodic equipment management must not drop pending scrap or duplicate DNL conversion");
            data.animightEquipmentInventory.set(53,ItemStack.EMPTY);AnimightEquipmentEvents.maintain(tame,data);
            check(tame.getItemBySlot(EquipmentSlot.FEET).isEmpty()&&data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==1,"pending scrap moves into reserve once space opens");
            var victim=EntityType.ZOMBIE.create(helper.getLevel());AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,helper.getLevel().damageSources().mobAttack(tame)));
            check(tame.getItemBySlot(EquipmentSlot.FEET).is(Items.IRON_BOOTS)&&!tame.getItemBySlot(EquipmentSlot.FEET).isDamaged(),"retained scrap repairs on a later kill");
        }finally{TameRegistry.remove(tame.getUUID());tame.discard();}helper.succeed();
    }

    static void startEquipmentDuel(GameTestHelper h,TamableAnimal a,TamableAnimal b){
        for(var pet:List.of(a,b)){pet.setNoAi(true);pet.setNoGravity(true);h.getLevel().addFreshEntity(pet);com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.LoadedTameIndex.refresh(pet);}
        com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager.startTeamDuel(h.getLevel().getServer(),a.getOwnerUUID(),java.util.Set.of(a.getUUID()),b.getOwnerUUID(),java.util.Set.of(b.getUUID()));
        check(com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager.isTameInDuel(a.getUUID()),"actual equipment duel started");
    }
    static LivingEntity equipmentBody(GameTestHelper h,TamableAnimal old){var data=TameRegistry.get(old.getUUID());return com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);}
    static void closeEquipmentDuel(GameTestHelper h,TamableAnimal a,TamableAnimal b){
        com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),a.getOwnerUUID());
        for(var old:List.of(a,b)){var body=equipmentBody(h,old);TameRegistry.remove(old.getUUID());if(body!=null)body.discard();old.discard();}
    }
    static void equipmentRound(GameTestHelper h,boolean lethal){
        var a=companion(h);var b=companion(h);
        var helmet=worn(new ItemStack(Items.IRON_HELMET));var sword=worn(new ItemStack(Items.DIAMOND_SWORD));a.setItemSlot(EquipmentSlot.HEAD,helmet);a.setItemSlot(EquipmentSlot.MAINHAND,sword);
        try{
            startEquipmentDuel(h,a,b);
            a.hurt(h.getLevel().damageSources().mobAttack(b),8);
            check(a.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"real duel damage must wear armor into a scrap instead of rescuing it at ten percent");
            check(a.doHurtTarget(b),"real native melee hit succeeds");
            check(a.getMainHandItem().isEmpty(),"real native duel melee consumes weapon durability into a scrap");
            check(TameRegistry.get(a.getUUID()).animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==2,"exactly two broken pieces retained");
            if(lethal) a.hurt(h.getLevel().damageSources().genericKill(),10000);
            else com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),a.getOwnerUUID());
            var restored=equipmentBody(h,a);var data=TameRegistry.get(a.getUUID());
            check(restored!=null&&restored.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&restored.getMainHandItem().isEmpty(),"duel restoration must not duplicate originals: restored="+restored+", head="+(restored==null?"null":restored.getItemBySlot(EquipmentSlot.HEAD))+", hand="+(restored==null?"null":restored.getMainHandItem())+", reserve="+data.animightEquipmentInventory+", dead="+data.dead+", inDuel="+com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager.isTameInDuel(a.getUUID()));
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==2,"duel end preserves both scraps");
            var victim=EntityType.ZOMBIE.create(h.getLevel());AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,h.getLevel().damageSources().mobAttack(restored)));
            check(restored.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)&&restored.getMainHandItem().is(Items.DIAMOND_SWORD),"later kill restores both pieces exactly once");
        }finally{closeEquipmentDuel(h,a,b);}h.succeed();
    }
    @GameTest(template="empty") public static void duelHitsWearArmorAndWeaponsWithoutRestorationDuplication(GameTestHelper h){equipmentRound(h,false);}
    @GameTest(template="empty") public static void duelDeathRetainsScrapsWithoutRestoringDuplicateOriginals(GameTestHelper h){equipmentRound(h,true);}
    @GameTest(template="empty") public static void duelBowShotsBreakAndKillsRepairEquipment(GameTestHelper h){
        var a=companion(h);var b=companion(h);a.setItemSlot(EquipmentSlot.MAINHAND,worn(new ItemStack(Items.BOW)));
        try{
            startEquipmentDuel(h,a,b);((net.minecraft.world.entity.monster.RangedAttackMob)a).performRangedAttack(b,1F);
            check(a.getMainHandItem().isEmpty()&&TameRegistry.get(a.getUUID()).animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==1,"actual native bow shot must consume durability into one scrap");
            var victim=EntityType.ZOMBIE.create(h.getLevel());AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,h.getLevel().damageSources().mobAttack(a)));
            check(a.getMainHandItem().is(Items.BOW)&&!a.getMainHandItem().isDamaged(),"kills during duels restore equipment immediately");
        }finally{closeEquipmentDuel(h,a,b);}h.succeed();
    }

    @GameTest(template="empty") public static void duelOffhandBowScrapSurvivesFullInventory(GameTestHelper h){
        var a=companion(h);var b=companion(h);a.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.NETHERITE_HELMET));a.setItemSlot(EquipmentSlot.OFFHAND,worn(new ItemStack(Items.BOW)));var data=TameRegistry.get(a.getUUID());
        for(int i=0;i<AnimightEquipmentEvents.INVENTORY_SIZE;i++)AnimightEquipmentEvents.store(data,new ItemStack(Items.DIAMOND_HELMET));
        try{
            startEquipmentDuel(h,a,b);((net.minecraft.world.entity.monster.RangedAttackMob)a).performRangedAttack(b,1F);
            check(AnimightEquipmentEvents.isRecoveryItem(a.getOffhandItem()),"full reserve retains broken offhand bow as pending scrap");
            AnimightEquipmentEvents.maintain(a,data);
            check(AnimightEquipmentEvents.isRecoveryItem(a.getOffhandItem()),"equipment maintenance must not drop a pending offhand scrap from a full inventory");
            var victim=EntityType.ZOMBIE.create(h.getLevel());AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,h.getLevel().damageSources().mobAttack(a)));
            check(a.getMainHandItem().is(Items.BOW)&&!a.getMainHandItem().isDamaged()&&a.getOffhandItem().isEmpty(),"kill restores the pending bow without extra copies");
        }finally{closeEquipmentDuel(h,a,b);}h.succeed();
    }


    @GameTest(template="empty") public static void ordinaryDeathClearsStaleArmorAndRespawnsScrapOnce(GameTestHelper h){
        var a=companion(h);var data=TameRegistry.get(a.getUUID());
        try{
            a.setNoAi(true);h.getLevel().addFreshEntity(a);
            var helmet=worn(new ItemStack(Items.IRON_HELMET));a.setItemSlot(EquipmentSlot.HEAD,helmet);
            data.entitySnapshot=new CompoundTag();a.save(data.entitySnapshot);
            check(ItemStack.of(data.entitySnapshot.getCompound("head")).is(Items.IRON_HELMET),"fixture has native redundant armor save");
            helmet.hurtAndBreak(10000,a,ignored->{});
            a.hurt(h.getLevel().damageSources().genericKill(),10000);
            check(data.dead,"ordinary combat death is recorded outside any duel");
            check(ItemStack.of(data.entitySnapshot.getCompound("head")).isEmpty(),"ordinary death clears stale native armor alias before archive");
            a.discard();
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.respawnDeadTameForDuel(h.getLevel().getServer(),data.uuid),"shared command respawn succeeds");
            var live=equipmentBody(h,a);
            check(live!=null&&live.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"normal respawn cannot resurrect broken armor");
            check(data.animightEquipmentInventory.stream().filter(AnimightEquipmentEvents::isRecoveryItem).count()==1,"normal death preserves exactly one scrap");
            var victim=EntityType.ZOMBIE.create(h.getLevel());AnimightEquipmentEvents.onKill(new LivingDeathEvent(victim,h.getLevel().damageSources().mobAttack(live)));
            check(live.getItemBySlot(EquipmentSlot.HEAD).is(Items.IRON_HELMET)&&data.animightEquipmentInventory.stream().noneMatch(AnimightEquipmentEvents::isRecoveryItem),"later kill restores exactly one helmet");
        }finally{var live=equipmentBody(h,a);TameRegistry.remove(a.getUUID());if(live!=null)live.discard();a.discard();}h.succeed();
    }
    @GameTest(template="empty") public static void legacyArmorAliasesAreNormalizedWithoutMutatingArchive(GameTestHelper h){
        var a=companion(h);var data=TameRegistry.get(a.getUUID());
        try{
            a.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));var snapshot=new CompoundTag();a.save(snapshot);
            a.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);a.save(snapshot);
            check(!ItemStack.of(snapshot.getCompound("head")).isEmpty(),"fixture reproduces stale native alias");
            var loader=com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.class.getDeclaredMethod("loadRespawnSnapshot",LivingEntity.class,CompoundTag.class);loader.setAccessible(true);
            loader.invoke(null,a,snapshot);
            check(a.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"shared respawn and recovery loader repairs legacy snapshots");
            check(ItemStack.of(snapshot.getCompound("head")).is(Items.IRON_HELMET),"loading preserves original archive for recovery");
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}finally{TameRegistry.remove(a.getUUID());a.discard();}h.succeed();
    }
}
