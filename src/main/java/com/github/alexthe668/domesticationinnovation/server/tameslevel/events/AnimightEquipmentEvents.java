package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Reserve storage for native Animights equipment, independent of the retired collar armor. */
public final class AnimightEquipmentEvents {
    public static final int INVENTORY_SIZE = 54;
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private AnimightEquipmentEvents() { }

    public static boolean isAnimight(LivingEntity tame) {
        ResourceLocation type = tame == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return type != null && type.getNamespace().equals("animights") && TameEntityAdapter.isTame(tame);
    }

    private static TameData data(LivingEntity tame) {
        TameData data = TameRegistry.get(tame.getUUID());
        return data != null ? data : TameRegistry.getByTlId(TameData.getTlId(tame));
    }

    private static boolean active(LivingEntity tame, TameData data) {
        return isAnimight(tame) && !tame.level().isClientSide && tame.isAlive()
                && data != null && !data.dead && !data.stored;
    }

    public static boolean isRecoveryItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && (id.toString().equals("dungeonnowloading:item_scraps")
                || isSealedItem(stack));
    }

    public static boolean isSealedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && id.getNamespace().equals("l2hostility") && id.getPath().contains("seal");
    }

    private static ItemStack restoredRecoveryItem(ItemStack wrapper) {
        if (!isRecoveryItem(wrapper) || wrapper.getTag() == null) return ItemStack.EMPTY;
        String key = isSealedItem(wrapper) ? "sealedItem" : "Original";
        if (!wrapper.getTag().contains(key, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        ItemStack original = ItemStack.of(wrapper.getTag().getCompound(key).copy());
        // Never unwrap another recovery token or consume malformed originals.
        if (original.isEmpty() || isRecoveryItem(original)) return ItemStack.EMPTY;
        if (original.isDamageableItem()) original.setDamageValue(0);
        return original;
    }

    /** Restore valid L2 seals and DNL scraps in place, including equipped tokens. */
    public static int restoreRecoveryItems(LivingEntity tame, TameData data) {
        int restored = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack original = restoredRecoveryItem(tame.getItemBySlot(slot));
            if (original.isEmpty()) continue;
            tame.setItemSlot(slot, original);
            restored++;
        }
        for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
            ItemStack original = restoredRecoveryItem(data.animightEquipmentInventory.get(i));
            if (original.isEmpty()) continue;
            data.animightEquipmentInventory.set(i, original);
            restored++;
        }
        if (restored > 0) TameRegistry.markDirty();
        return restored;
    }

    public static boolean hasAnimightEquipment(TameData data) {
        return data != null && data.type != null
                && (data.type.startsWith("animights:") || data.type.startsWith("entity.animights."));
    }

    private static boolean repairStack(ItemStack stack, long allowance) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged() || isRecoveryItem(stack)) return false;
        boolean equipment = stack.getItem() instanceof ArmorItem || stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem || stack.getItem() instanceof ProjectileWeaponItem
                || !stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
        if (!equipment) return false;
        stack.setDamageValue((int) Math.max(0L, (long) stack.getDamageValue() - allowance));
        return true;
    }

    /** Each item gets its own two-durability-per-XP allowance; XP is never spent. */
    public static void repairOnXp(LivingEntity tame, TameData data, int xp) {
        if (xp <= 0 || !hasAnimightEquipment(data)) return;
        long allowance = (long) xp * 2L;
        boolean changed = false;
        if (tame != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) changed |= repairStack(tame.getItemBySlot(slot), allowance);
        } else if (data.entitySnapshot != null) {
            // Unloaded ranked participants keep native equipment in their saved entity.
            for (String key : new String[]{"ArmorItems", "HandItems"}) {
                var items = data.entitySnapshot.getList(key, Tag.TAG_COMPOUND);
                for (int i = 0; i < items.size(); i++) {
                    ItemStack stack = ItemStack.of(items.getCompound(i));
                    if (!repairStack(stack, allowance)) continue;
                    items.set(i, stack.save(new net.minecraft.nbt.CompoundTag()));
                    changed = true;
                }
            }
        }
        for (ItemStack stack : data.animightEquipmentInventory) changed |= repairStack(stack, allowance);
        if (changed) TameRegistry.markDirty();
    }

    /** Break-protection mods may spawn the transformed item instead of replacing the slot. */
    public static boolean captureRecoveryDrop(LivingEntity tame, ItemStack stack) {
        if (!isAnimight(tame) || !isRecoveryItem(stack)) return false;
        TameData data = data(tame);
        if (!active(tame, data)) return false;
        store(data, stack);
        return stack.isEmpty();
    }

    private static void pad(TameData data) {
        while (data.animightEquipmentInventory.size() < INVENTORY_SIZE) data.animightEquipmentInventory.add(ItemStack.EMPTY);
    }

    /** Moves only what fits, leaving overflow with the caller. */
    public static int store(TameData data, ItemStack incoming) {
        if (data == null || incoming == null || incoming.isEmpty()) return 0;
        pad(data);
        int before = incoming.getCount();
        for (ItemStack existing : data.animightEquipmentInventory) {
            if (existing.isEmpty() || !ItemStack.isSameItemSameTags(existing, incoming)) continue;
            int count = Math.min(incoming.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (count > 0) { existing.grow(count); incoming.shrink(count); }
            if (incoming.isEmpty()) break;
        }
        for (int i = 0; i < INVENTORY_SIZE && !incoming.isEmpty(); i++) {
            if (!data.animightEquipmentInventory.get(i).isEmpty()) continue;
            data.animightEquipmentInventory.set(i, incoming.split(Math.min(incoming.getCount(), incoming.getMaxStackSize())));
        }
        int moved = before - incoming.getCount();
        if (moved > 0) TameRegistry.markDirty();
        return moved;
    }

    private static boolean fitsRecoveryToken(TameData data, ItemStack token) {
        if (data.animightEquipmentInventory.size() < INVENTORY_SIZE) return true;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack existing = data.animightEquipmentInventory.get(i);
            if (existing.isEmpty() || ItemStack.isSameItemSameTags(existing, token)
                    && existing.getCount() < existing.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Called when worn armor or a dueling weapon would break through durability. */
    public static boolean preserveBreakingArmor(LivingEntity wearer, ItemStack armor) {
        if (wearer.level().isClientSide || !isAnimight(wearer)) return false;
        boolean wornArmor = armor.getItem() instanceof ArmorItem;
        if (!wornArmor && (!TameDuelManager.isTameInDuel(wearer.getUUID()) || !validWeapon(wearer, armor))) return false;
        TameData data = data(wearer);
        if (data == null || data.dead || data.stored) return false;
        EquipmentSlot equipped = null;
        for (EquipmentSlot slot : EquipmentSlot.values()) if ((wornArmor ? slot.getType() == EquipmentSlot.Type.ARMOR : slot.getType() == EquipmentSlot.Type.HAND) && wearer.getItemBySlot(slot) == armor) { equipped = slot; break; }
        if (equipped == null) return false;
        var scrapId = new ResourceLocation("dungeonnowloading", "item_scraps");
        if (!ForgeRegistries.ITEMS.containsKey(scrapId)) {
            // Optional DNL absent: preserve the original rather than destroying it.
            armor.setDamageValue(Math.max(0, armor.getMaxDamage() - 1));
            return true;
        }
        ItemStack token = new ItemStack(ForgeRegistries.ITEMS.getValue(scrapId));
        token.getOrCreateTag().put("Original", armor.copyWithCount(1).save(new net.minecraft.nbt.CompoundTag()));
        store(data, token);
        // A full reserve keeps its pending scrap safely in this equipment slot.
        // It provides no armor and moves into reserve on the next check with space.
        wearer.setItemSlot(equipped, token.isEmpty() ? ItemStack.EMPTY : token);
        armor.setCount(0);
        TameRegistry.markDirty();
        return true;
    }

    private static boolean tooWorn(ItemStack stack, int incomingDamage) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue()
                <= Math.max(1, (int) Math.ceil(stack.getMaxDamage() * 0.10D)) + incomingDamage;
    }

    public static boolean validArmor(LivingEntity tame, ItemStack stack, EquipmentSlot slot) {
        return stack != null && !stack.isEmpty() && stack.getItem() instanceof ArmorItem armor
                && armor.getEquipmentSlot() == slot && stack.canEquip(slot, tame) && !isRecoveryItem(stack);
    }

    /** Both native Animights companions share the same melee/bow attack implementation. */
    private static boolean usesWeapons(LivingEntity tame) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return id != null && id.getNamespace().equals("animights")
                && (id.getPath().equals("canito") || id.getPath().equals("catwain"));
    }

    public static boolean validWeapon(LivingEntity tame, ItemStack stack) {
        if (!usesWeapons(tame) || stack == null || stack.isEmpty() || isRecoveryItem(stack)
                || !stack.canEquip(EquipmentSlot.MAINHAND, tame)) return false;
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem) return true;
        // Include modded melee weapons exposing attack modifiers, but not armor or unsupported ranged items.
        return !(stack.getItem() instanceof ArmorItem) && !(stack.getItem() instanceof ProjectileWeaponItem)
                && stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)
                        .stream().anyMatch(modifier -> modifier.getAmount() > 0);
    }

    public static boolean acceptsEquipment(LivingEntity tame, ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.getItem() instanceof ArmorItem || isRecoveryItem(stack) || validWeapon(tame, stack));
    }

    /** Expected direct damage against a neutral target; attack speed and conditional abilities do not rank gear. */
    public static double weaponDamage(LivingEntity tame, ItemStack stack) {
        if (stack.getItem() instanceof BowItem bow) {
            int power = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.POWER_ARROWS, stack);
            // Native companions use a full-strength mob arrow and launch it at speed 1.6.
            double base = 2.0D + tame.level().getDifficulty().getId() * 0.11D;
            if (power > 0) base += power * 0.5D + 0.5D;
            // Vanilla and Too Many Bows inherit this no-op hook: avoid allocating a probe for them.
            if (CUSTOM_ARROWS.get(bow.getClass())) {
                Arrow probe = new Arrow(tame.level(), tame);
                probe.setBaseDamage(base);
                AbstractArrow custom = bow.customArrow(probe);
                if (custom != null) base = custom.getBaseDamage();
            }
            return Math.max(0, base * 1.6D);
        }
        var instance = tame.getAttribute(Attributes.ATTACK_DAMAGE);
        if (instance == null) return 0;
        var held = tame.getMainHandItem().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
        java.util.Set<java.util.UUID> replaced = new java.util.HashSet<>();
        held.forEach(modifier -> replaced.add(modifier.getId()));
        var candidate = stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE);
        candidate.forEach(modifier -> replaced.add(modifier.getId()));
        java.util.List<AttributeModifier> modifiers = new java.util.ArrayList<>();
        instance.getModifiers().stream().filter(modifier -> !replaced.contains(modifier.getId())).forEach(modifiers::add);
        modifiers.addAll(candidate);
        double value = instance.getBaseValue();
        for (AttributeModifier modifier : modifiers) if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) value += modifier.getAmount();
        double base = value;
        for (AttributeModifier modifier : modifiers) if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_BASE) value += base * modifier.getAmount();
        for (AttributeModifier modifier : modifiers) if (modifier.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL) value *= 1.0D + modifier.getAmount();
        return Math.max(0, Attributes.ATTACK_DAMAGE.sanitizeValue(value)
                + EnchantmentHelper.getDamageBonus(stack, MobType.UNDEFINED));
    }

    private static final ClassValue<Boolean> CUSTOM_ARROWS = new ClassValue<>() {
        protected Boolean computeValue(Class<?> type) {
            try { return type.getMethod("customArrow", AbstractArrow.class).getDeclaringClass() != BowItem.class; }
            catch (NoSuchMethodException ex) { return false; }
        }
    };

    private static double attribute(ItemStack stack, EquipmentSlot slot, Attribute attribute) {
        double additions = 0, multiplyBase = 0, multiplyTotal = 1;
        for (AttributeModifier modifier : stack.getAttributeModifiers(slot).get(attribute)) {
            switch (modifier.getOperation()) {
                case ADDITION -> additions += modifier.getAmount();
                case MULTIPLY_BASE -> multiplyBase += modifier.getAmount();
                case MULTIPLY_TOTAL -> multiplyTotal *= 1.0D + modifier.getAmount();
            }
        }
        return additions * (1.0D + multiplyBase) * multiplyTotal;
    }

    private static int compare(ItemStack candidate, ItemStack best, EquipmentSlot slot) {
        if (best.isEmpty()) return 1;
        int result = Double.compare(attribute(candidate, slot, Attributes.ARMOR), attribute(best, slot, Attributes.ARMOR));
        if (result == 0) result = Double.compare(attribute(candidate, slot, Attributes.ARMOR_TOUGHNESS), attribute(best, slot, Attributes.ARMOR_TOUGHNESS));
        if (result == 0) result = Integer.compare(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, candidate),
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, best));
        if (result == 0) result = Integer.compare(candidate.getMaxDamage() - candidate.getDamageValue(), best.getMaxDamage() - best.getDamageValue());
        return result;
    }

    public static void maintain(LivingEntity tame, TameData data) {
        maintain(tame, data, 0);
        discardWornReserveArmor(tame, data);
    }

    private static void maintain(LivingEntity tame, TameData data, int incomingDamage) {
        if (!active(tame, data)) return;
        boolean duel = TameDuelManager.isTameInDuel(tame.getUUID());
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = tame.getItemBySlot(slot);
            if (isRecoveryItem(worn) && !fitsRecoveryToken(data, worn)) continue;
            ItemStack removed = ItemStack.EMPTY;
            if (!worn.isEmpty() && (!validArmor(tame, worn, slot) || !duel && tooWorn(worn, incomingDamage))) {
                removed = worn;
                tame.setItemSlot(slot, ItemStack.EMPTY);
            }
            {
                int best = -1;
                ItemStack bestStack = tame.getItemBySlot(slot);
                for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
                    ItemStack candidate = data.animightEquipmentInventory.get(i);
                    if (validArmor(tame, candidate, slot) && (duel || !tooWorn(candidate, incomingDamage)) && compare(candidate, bestStack, slot) > 0) {
                        best = i; bestStack = candidate;
                    }
                }
                if (best >= 0) {
                    if (removed.isEmpty()) removed = tame.getItemBySlot(slot);
                    tame.setItemSlot(slot, bestStack.split(1));
                    if (bestStack.isEmpty()) data.animightEquipmentInventory.set(best, ItemStack.EMPTY);
                    TameRegistry.markDirty();
                }
            }
            if (!removed.isEmpty()) {
                store(data, removed);
                if (!removed.isEmpty()) tame.spawnAtLocation(removed);
                TameRegistry.markDirty();
            }
        }
        if (usesWeapons(tame)) maintainWeapon(tame, data);
    }

    /** Opt-in disposal: only one weak piece per five seconds, after normal equipment swaps. */
    public static boolean discardWornReserveArmor(LivingEntity tame, TameData data) {
        if (!active(tame, data) || TameDuelManager.isTameInDuel(tame.getUUID())
                || !com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings.discardWornArmor(data.ownerUUID)) return false;
        long now = tame.level().getGameTime();
        if (now < data.nextAnimightWornArmorDiscardTick) return false;
        data.nextAnimightWornArmorDiscardTick = now + 100L;
        int weakest = -1;
        for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
            ItemStack stack = data.animightEquipmentInventory.get(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) continue;
            // Non-damageable armor must never count as nearly broken.
            if (!stack.isDamageableItem() || !tooWorn(stack, 0)) return false;
            if (weakest < 0 || compareDiscardArmor(stack, data.animightEquipmentInventory.get(weakest)) < 0) weakest = i;
        }
        if (weakest < 0) return false;
        ItemStack stack = data.animightEquipmentInventory.get(weakest);
        ItemStack dropped = stack.copyWithCount(1);
        var entity = tame.spawnAtLocation(dropped);
        if (entity == null) return false;
        entity.setPickUpDelay(40);
        stack.shrink(1);
        if (stack.isEmpty()) data.animightEquipmentInventory.set(weakest, ItemStack.EMPTY);
        TameRegistry.markDirty();
        return true;
    }

    private static int compareDiscardArmor(ItemStack a, ItemStack b) {
        EquipmentSlot aSlot = ((ArmorItem)a.getItem()).getEquipmentSlot(), bSlot = ((ArmorItem)b.getItem()).getEquipmentSlot();
        int result = Double.compare(attribute(a, aSlot, Attributes.ARMOR), attribute(b, bSlot, Attributes.ARMOR));
        if (result == 0) result = Double.compare(attribute(a, aSlot, Attributes.ARMOR_TOUGHNESS), attribute(b, bSlot, Attributes.ARMOR_TOUGHNESS));
        if (result == 0) result = Integer.compare(EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, a),
                EnchantmentHelper.getItemEnchantmentLevel(Enchantments.ALL_DAMAGE_PROTECTION, b));
        if (result == 0) result = Integer.compare(a.getMaxDamage() - a.getDamageValue(), b.getMaxDamage() - b.getDamageValue());
        return result;
    }

    private static void maintainWeapon(LivingEntity tame, TameData data) {
        boolean duel = TameDuelManager.isTameInDuel(tame.getUUID());
        ItemStack held = tame.getMainHandItem();
        if (isRecoveryItem(held) && !fitsRecoveryToken(data, held)) return;
        // Preserve unrelated hand items such as shields and utility items.
        if (!held.isEmpty() && !validWeapon(tame, held) && !isRecoveryItem(held)) return;
        boolean unusable = !held.isEmpty() && (isRecoveryItem(held) || (!duel && tooWorn(held, 0)));
        ItemStack offhand = tame.getOffhandItem();
        if (isRecoveryItem(offhand) && !fitsRecoveryToken(data, offhand)) return;
        boolean reserveOffhand = !offhand.isEmpty() && (validWeapon(tame, offhand) || isRecoveryItem(offhand));
        int best = -1;
        ItemStack bestStack = unusable ? ItemStack.EMPTY : held;
        double bestDamage = bestStack.isEmpty() ? Double.NEGATIVE_INFINITY : weaponDamage(tame, bestStack);
        // The native ranged goal checks both hands. Keep the selected weapon in the main hand,
        // otherwise a weaker offhand bow could override a stronger selected melee weapon.
        if (reserveOffhand && validWeapon(tame, offhand) && (duel || !tooWorn(offhand, 0))) {
            double damage = weaponDamage(tame, offhand);
            if (damage > bestDamage || (damage == bestDamage && !bestStack.isEmpty()
                    && offhand.getMaxDamage() - offhand.getDamageValue() > bestStack.getMaxDamage() - bestStack.getDamageValue())) {
                best = -2; bestStack = offhand; bestDamage = damage;
            }
        }
        for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
            ItemStack candidate = data.animightEquipmentInventory.get(i);
            if (!validWeapon(tame, candidate) || (!duel && tooWorn(candidate, 0))) continue;
            double damage = weaponDamage(tame, candidate);
            if (damage > bestDamage || (damage == bestDamage && !bestStack.isEmpty()
                    && candidate.getMaxDamage() - candidate.getDamageValue() > bestStack.getMaxDamage() - bestStack.getDamageValue())) {
                best = i; bestStack = candidate; bestDamage = damage;
            }
        }
        if (best == -1 && !unusable && !reserveOffhand) return;
        // Free the candidate's slot before storing the displaced weapon, including a full reserve.
        boolean replaceMainhand = best != -1 || unusable;
        ItemStack replacement = best == -1 ? ItemStack.EMPTY : bestStack.split(1);
        if (best >= 0 && bestStack.isEmpty()) data.animightEquipmentInventory.set(best, ItemStack.EMPTY);
        if (tame.isUsingItem() && (replaceMainhand || reserveOffhand)) tame.stopUsingItem();
        if (reserveOffhand) tame.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (replaceMainhand) tame.setItemSlot(EquipmentSlot.MAINHAND, replacement);
        if (replaceMainhand && !held.isEmpty()) {
            store(data, held);
            if (!held.isEmpty()) tame.spawnAtLocation(held);
        }
        if (reserveOffhand && !offhand.isEmpty()) {
            store(data, offhand);
            if (!offhand.isEmpty()) tame.spawnAtLocation(offhand);
        }
        TameRegistry.markDirty();
    }

    public static void damageDuelArmor(LivingEntity tame, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (tame.level().isClientSide || !isAnimight(tame) || !TameDuelManager.isTameInDuel(tame.getUUID()) || damage <= 0 || source.is(DamageTypeTags.BYPASSES_ARMOR)) return;
        int wear = Math.max(1, (int) (damage / 4F));
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = tame.getItemBySlot(slot);
            if (!validArmor(tame, stack, slot) || source.is(DamageTypeTags.IS_FIRE) && stack.getItem().isFireResistant()) continue;
            stack.hurtAndBreak(wear, tame, ignored -> { });
        }
    }

    public static void damageDuelWeapon(LivingEntity tame, boolean ranged) {
        if (tame.level().isClientSide || !isAnimight(tame) || !TameDuelManager.isTameInDuel(tame.getUUID())) return;
        ItemStack stack = tame.getMainHandItem();
        if (ranged && !(stack.getItem() instanceof BowItem)) stack = tame.getOffhandItem();
        if (!validWeapon(tame, stack) || ranged != (stack.getItem() instanceof BowItem)) return;
        stack.hurtAndBreak(ranged || stack.getItem() instanceof SwordItem ? 1 : 2, tame, ignored -> { });
    }

    /** Copy just equipment, never position/health/AI, into a duel restoration snapshot. */
    public static void copyLiveEquipment(LivingEntity tame, net.minecraft.nbt.CompoundTag snapshot) {
        for (EquipmentSlot.Type type : EquipmentSlot.Type.values()) {
            var items = new net.minecraft.nbt.ListTag();
            for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.getType() == type)
                items.add(tame.getItemBySlot(slot).save(new net.minecraft.nbt.CompoundTag()));
            snapshot.put(type == EquipmentSlot.Type.ARMOR ? "ArmorItems" : "HandItems", items);
        }
        syncArmorSlotTags(snapshot);
    }

    public static void syncArmorSlotTags(net.minecraft.nbt.CompoundTag snapshot) {
        if (!snapshot.contains("ArmorItems", Tag.TAG_LIST)) return;
        var items = snapshot.getList("ArmorItems", Tag.TAG_COMPOUND);
        // Animights also reads head/chest/legs/feet after vanilla ArmorItems.
        // Empty slots must overwrite old custom tags, including reused death NBT.
        for (EquipmentSlot slot : ARMOR_SLOTS) snapshot.put(slot.getName(), items.getCompound(slot.getIndex()).copy());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        LivingEntity killer = TameCombatEvents.resolveTameAttacker(event.getSource());
        if (!isAnimight(killer)) return;
        TameData data = data(killer);
        if (!active(killer, data)) return;
        restoreRecoveryItems(killer, data);
        maintain(killer, data);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity tame = event.getEntity();
        if (event.isCanceled() || event.getAmount() <= 0 || !isAnimight(tame)) return;
        int predictedWear = event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) ? 0 : Math.max(1, (int) (event.getAmount() / 4.0F));
        maintain(tame, data(tame), predictedWear);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide) return;
        LivingEntity killer = TameCombatEvents.resolveTameAttacker(event.getSource());
        if (!isAnimight(killer) || TameEntityAdapter.isTame(event.getEntity())) return;
        TameData data = data(killer);
        if (!active(killer, data) || TameDuelManager.isTameInDuel(killer.getUUID())) return;
        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!acceptsEquipment(killer, stack)) return false;
            store(data, stack);
            return stack.isEmpty();
        });
        restoreRecoveryItems(killer, data);
        maintain(killer, data);
    }

    public static boolean openInventory(ServerPlayer player, LivingEntity tame, TameData data) {
        if (!active(tame, data) || TameDuelManager.isTameInDuel(tame.getUUID()) || !player.getUUID().equals(data.ownerUUID)) return false;
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new ChestMenu(MenuType.GENERIC_9x6,
                id, inventory, new ReserveContainer(tame, data), 6), Component.literal(data.name + " Equipment")));
        return true;
    }

    /** Live storage keeps automatic equipment changes and an open menu in sync. */
    public static final class ReserveContainer implements Container {
        private final LivingEntity tame;
        private final TameData data;
        public ReserveContainer(LivingEntity tame, TameData data) { this.tame = tame; this.data = data; pad(data); }
        public int getContainerSize() { return INVENTORY_SIZE; }
        public boolean isEmpty() { return data.animightEquipmentInventory.stream().allMatch(ItemStack::isEmpty); }
        public ItemStack getItem(int slot) { return data.animightEquipmentInventory.get(slot); }
        public ItemStack removeItem(int slot, int amount) {
            ItemStack result = ContainerHelper.removeItem(data.animightEquipmentInventory, slot, amount);
            if (!result.isEmpty()) setChanged();
            return result;
        }
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack result = ContainerHelper.takeItem(data.animightEquipmentInventory, slot);
            if (!result.isEmpty()) setChanged();
            return result;
        }
        public void setItem(int slot, ItemStack stack) { data.animightEquipmentInventory.set(slot, stack); setChanged(); }
        public void setChanged() { TameRegistry.markDirty(); }
        public boolean stillValid(Player player) { return active(tame, data) && !TameDuelManager.isTameInDuel(tame.getUUID()) && player.getUUID().equals(data.ownerUUID); }
        public boolean canPlaceItem(int slot, ItemStack stack) { return acceptsEquipment(tame, stack); }
        public void clearContent() { data.animightEquipmentInventory.replaceAll(ignored -> ItemStack.EMPTY); setChanged(); }
        public void stopOpen(Player player) { maintain(tame, data); }
    }
}
