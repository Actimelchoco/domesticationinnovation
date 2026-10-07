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
    public static final int INVENTORY_SIZE = 27;
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
                && data != null && !data.dead && !data.stored && !TameDuelManager.isTameInDuel(tame.getUUID());
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

    /** L2's SealedItem stores the complete original stack under sealedItem. */
    public static int restoreSealedItems(TameData data) {
        int restored = 0;
        for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
            ItemStack sealed = data.animightEquipmentInventory.get(i);
            if (!isSealedItem(sealed) || sealed.getTag() == null || !sealed.getTag().contains("sealedItem", Tag.TAG_COMPOUND)) continue;
            ItemStack original = ItemStack.of(sealed.getTag().getCompound("sealedItem").copy());
            if (original.isEmpty() || isSealedItem(original)) continue;
            data.animightEquipmentInventory.set(i, original);
            restored++;
        }
        if (restored > 0) TameRegistry.markDirty();
        return restored;
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
    }

    private static void maintain(LivingEntity tame, TameData data, int incomingDamage) {
        if (!active(tame, data)) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack worn = tame.getItemBySlot(slot);
            ItemStack removed = ItemStack.EMPTY;
            if (!worn.isEmpty() && (!validArmor(tame, worn, slot) || tooWorn(worn, incomingDamage))) {
                removed = worn;
                tame.setItemSlot(slot, ItemStack.EMPTY);
            }
            {
                int best = -1;
                ItemStack bestStack = tame.getItemBySlot(slot);
                for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
                    ItemStack candidate = data.animightEquipmentInventory.get(i);
                    if (validArmor(tame, candidate, slot) && !tooWorn(candidate, incomingDamage) && compare(candidate, bestStack, slot) > 0) {
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

    private static void maintainWeapon(LivingEntity tame, TameData data) {
        ItemStack held = tame.getMainHandItem();
        // Preserve unrelated hand items such as shields and utility items.
        if (!held.isEmpty() && !validWeapon(tame, held) && !isRecoveryItem(held)) return;
        boolean unusable = !held.isEmpty() && (isRecoveryItem(held) || tooWorn(held, 0));
        ItemStack offhand = tame.getOffhandItem();
        boolean reserveOffhand = !offhand.isEmpty() && (validWeapon(tame, offhand) || isRecoveryItem(offhand));
        int best = -1;
        ItemStack bestStack = unusable ? ItemStack.EMPTY : held;
        double bestDamage = bestStack.isEmpty() ? Double.NEGATIVE_INFINITY : weaponDamage(tame, bestStack);
        // The native ranged goal checks both hands. Keep the selected weapon in the main hand,
        // otherwise a weaker offhand bow could override a stronger selected melee weapon.
        if (reserveOffhand && validWeapon(tame, offhand) && !tooWorn(offhand, 0)) {
            double damage = weaponDamage(tame, offhand);
            if (damage > bestDamage || (damage == bestDamage && !bestStack.isEmpty()
                    && offhand.getMaxDamage() - offhand.getDamageValue() > bestStack.getMaxDamage() - bestStack.getDamageValue())) {
                best = -2; bestStack = offhand; bestDamage = damage;
            }
        }
        for (int i = 0; i < data.animightEquipmentInventory.size(); i++) {
            ItemStack candidate = data.animightEquipmentInventory.get(i);
            if (!validWeapon(tame, candidate) || tooWorn(candidate, 0)) continue;
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onKill(LivingDeathEvent event) {
        if (event.isCanceled() || event.getEntity().level().isClientSide || !(event.getEntity() instanceof Mob)) return;
        LivingEntity killer = TameCombatEvents.resolveTameAttacker(event.getSource());
        if (!isAnimight(killer)) return;
        TameData data = data(killer);
        if (!active(killer, data)) return;
        // Include seals still occupying equipment slots before the next periodic check.
        maintain(killer, data);
        if (restoreSealedItems(data) > 0) maintain(killer, data);
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
        if (!active(killer, data)) return;
        event.getDrops().removeIf(drop -> {
            ItemStack stack = drop.getItem();
            if (!acceptsEquipment(killer, stack)) return false;
            store(data, stack);
            return stack.isEmpty();
        });
        maintain(killer, data);
    }

    public static boolean openInventory(ServerPlayer player, LivingEntity tame, TameData data) {
        if (!active(tame, data) || !player.getUUID().equals(data.ownerUUID)) return false;
        player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new ChestMenu(MenuType.GENERIC_9x3,
                id, inventory, new ReserveContainer(tame, data), 3), Component.literal(data.name + " Equipment")));
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
        public boolean stillValid(Player player) { return active(tame, data) && player.getUUID().equals(data.ownerUUID); }
        public boolean canPlaceItem(int slot, ItemStack stack) { return acceptsEquipment(tame, stack); }
        public void clearContent() { data.animightEquipmentInventory.replaceAll(ignored -> ItemStack.EMPTY); setChanged(); }
        public void stopOpen(Player player) { maintain(tame, data); }
    }
}
