package com.github.alexthe668.domesticationinnovation.server.tameslevel.events;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.Reader;
import java.io.Writer;
import java.util.concurrent.ThreadLocalRandom;

public final class TameStoredArmorEvents {
    private static final UUID ARMOR_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a1");
    private static final UUID TOUGHNESS_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a2");
    private static final UUID KNOCKBACK_ID = UUID.fromString("62258fb8-c4d2-43ae-8b45-c21c6f7891a3");
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> FORBIDDEN_ENCHANTMENTS = new LinkedHashSet<>();
    private static Path configPath;

    private TameStoredArmorEvents() { }

    public static void sync(LivingEntity tame, TameData data) {
        if (tame == null || data == null || tame.level().isClientSide) return;
        init(tame.getServer());
        apply(tame, Attributes.ARMOR, ARMOR_ID, total(data, tame, Attributes.ARMOR));
        apply(tame, Attributes.ARMOR_TOUGHNESS, TOUGHNESS_ID, total(data, tame, Attributes.ARMOR_TOUGHNESS));
        apply(tame, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK_ID, total(data, tame, Attributes.KNOCKBACK_RESISTANCE));
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity tame = event.getEntity();
        if (!TameEntityAdapter.isTame(tame) || tame.level().isClientSide) return;
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null || data.armorInventory.isEmpty()) return;
        init(tame.getServer());
        List<ItemStack> armor = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack stack = data.armorInventory.get(i);
            if (isSlotUnlocked(data, tame, i) && stack != null && !stack.isEmpty()) armor.add(withoutForbiddenEnchantments(stack));
        }
        if (armor.isEmpty()) return;
        int protection = Math.min(20, EnchantmentHelper.getDamageProtection(armor, event.getSource()));
        if (protection > 0) event.setAmount(event.getAmount() * (1.0F - protection / 25.0F));
        int durabilityDamage = Math.max(1, (int) (event.getAmount() / 4.0F));
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack original = data.armorInventory.get(i);
            if (!isSlotUnlocked(data, tame, i) || original == null || original.isEmpty()) continue;
            ItemStack filtered = withoutForbiddenEnchantments(original);
            filtered.hurtAndBreak(durabilityDamage, tame, ignored -> { });
            if (filtered.isEmpty()) original.setCount(0);
            else original.setDamageValue(filtered.getDamageValue());
        }
        data.armorInventory.removeIf(stack -> stack == null);
        while (data.armorInventory.size() < 4) data.armorInventory.add(ItemStack.EMPTY);
        sync(tame, data);
        TameRegistry.markDirty();
    }

    private static double total(TameData data, LivingEntity tame, Attribute attribute) {
        double result = 0.0D;
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack stack = data.armorInventory.get(i);
            if (!isSlotUnlocked(data, tame, i) || stack == null || stack.isEmpty()) continue;
            ItemStack filtered = withoutForbiddenEnchantments(stack);
            for (AttributeModifier modifier : filtered.getAttributeModifiers(SLOTS[i]).get(attribute)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) result += modifier.getAmount();
            }
        }
        return result;
    }

    public static boolean isSlotUnlocked(TameData data, LivingEntity tame, int slot) {
        int tier = tame != null && TameableUtils.hasCollar(tame)
                ? Math.max(0, TameableUtils.getEnchantLevel(tame, Enchantments.ALL_DAMAGE_PROTECTION))
                : data == null || !data.hasCollarTag ? 0 : Math.max(0, data.collarTagTier);
        int required = switch (slot) {
            case 3 -> 1; // boots
            case 0 -> 2; // helmet
            case 2 -> 3; // leggings
            case 1 -> 4; // chestplate
            default -> Integer.MAX_VALUE;
        };
        return tier >= required;
    }

    public static int wornPieceCount(TameData data) {
        if (data == null) return 0;
        int count = 0;
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack stack = data.armorInventory.get(i);
            if (isSlotUnlocked(data, null, i) && stack != null && !stack.isEmpty()) count++;
        }
        return count;
    }

    /** Returns XP left after vanilla-rate Mending (two durability per XP). */
    public static int repairWithMending(LivingEntity tame, TameData data, int xp) {
        return repairWithMending(tame == null ? null : tame.getServer(), tame, data, xp);
    }

    public static int repairWithMending(MinecraftServer server, LivingEntity tame, TameData data, int xp) {
        if (data == null || xp <= 0) return Math.max(0, xp);
        if (!PlayerDebugSettings.enableMending(data.ownerUUID)) return xp;
        init(server);
        List<ItemStack> candidates = new java.util.ArrayList<>();
        for (int i = 0; i < Math.min(4, data.armorInventory.size()); i++) {
            ItemStack stack = data.armorInventory.get(i);
            if (!isSlotUnlocked(data, tame, i) || stack == null || stack.isEmpty() || !stack.isDamaged()) continue;
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, withoutForbiddenEnchantments(stack)) > 0) {
                candidates.add(stack);
            }
        }
        if (candidates.isEmpty()) return xp;
        ItemStack selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        int repair = Math.min(selected.getDamageValue(), xp * 2);
        selected.setDamageValue(selected.getDamageValue() - repair);
        int consumedXp = (repair + 1) / 2;
        if (repair > 0) TameRegistry.markDirty();
        return Math.max(0, xp - consumedXp);
    }

    public static boolean forbidEnchantment(MinecraftServer server, String enchantmentId) {
        init(server);
        ResourceLocation id = ResourceLocation.tryParse(enchantmentId == null ? "" : enchantmentId.trim().toLowerCase(java.util.Locale.ROOT));
        if (id == null || !ForgeRegistries.ENCHANTMENTS.containsKey(id)) return false;
        boolean changed = FORBIDDEN_ENCHANTMENTS.add(id.toString());
        if (changed) save();
        return changed;
    }

    public static Set<String> forbiddenEnchantments(MinecraftServer server) {
        init(server);
        return Set.copyOf(FORBIDDEN_ENCHANTMENTS);
    }

    private static ItemStack withoutForbiddenEnchantments(ItemStack original) {
        if (FORBIDDEN_ENCHANTMENTS.isEmpty()) return original;
        ItemStack copy = original.copy();
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(copy);
        enchantments.entrySet().removeIf(entry -> {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            return id != null && FORBIDDEN_ENCHANTMENTS.contains(id.toString());
        });
        EnchantmentHelper.setEnchantments(enchantments, copy);
        return copy;
    }

    private static synchronized void init(MinecraftServer server) {
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("tameslevel-armor.json");
        if (path.equals(configPath)) return;
        configPath = path;
        FORBIDDEN_ENCHANTMENTS.clear();
        if (!Files.isRegularFile(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            ArmorConfig config = GSON.fromJson(reader, ArmorConfig.class);
            if (config != null && config.forbiddenEnchantments != null) FORBIDDEN_ENCHANTMENTS.addAll(config.forbiddenEnchantments);
        } catch (Exception ignored) { }
    }

    private static synchronized void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            ArmorConfig config = new ArmorConfig();
            config.forbiddenEnchantments.addAll(FORBIDDEN_ENCHANTMENTS);
            try (Writer writer = Files.newBufferedWriter(configPath)) { GSON.toJson(config, writer); }
        } catch (Exception ignored) { }
    }

    private static final class ArmorConfig {
        Set<String> forbiddenEnchantments = new LinkedHashSet<>();
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
