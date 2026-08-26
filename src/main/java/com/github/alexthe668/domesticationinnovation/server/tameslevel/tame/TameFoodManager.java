package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Learns Animal.isFood diets gradually and stores administrator additions per entity type. */
public final class TameFoodManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type CONFIG_TYPE = new TypeToken<FoodConfig>() { }.getType();
    private static final Map<String, Set<String>> LEARNED = new HashMap<>();
    private static final Map<String, Set<String>> CUSTOM = new HashMap<>();
    private static final Map<String, Map<String, Integer>> CUSTOM_POINTS = new HashMap<>();
    private static final Map<String, Integer> CURSORS = new HashMap<>();
    private static List<Item> candidates = List.of();
    private static Path configPath;
    private static long lastProbeTick = Long.MIN_VALUE;

    private TameFoodManager() { }

    public static void tick(LivingEntity tame, TameData data) {
        if (!(tame instanceof Animal animal) || data == null || tame.getServer() == null) return;
        init(tame.getServer());
        String type = typeId(tame, data);
        if (type.isBlank() || CURSORS.getOrDefault(type, 0) >= candidates.size()) return;
        long tick = tame.level().getGameTime();
        if (lastProbeTick == tick) return;
        lastProbeTick = tick;
        int cursor = CURSORS.getOrDefault(type, 0);
        Item item = candidates.get(cursor);
        CURSORS.put(type, cursor + 1);
        try {
            ItemStack probe = new ItemStack(item);
            if (animal.isFood(probe)) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null) LEARNED.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(id.toString());
            }
        } catch (Throwable ignored) {
            // A broken modded food predicate must not interrupt the server-wide scan.
        }
        if (cursor + 1 >= candidates.size()) save();
    }

    public static boolean accepts(ItemStack stack, TameData data, LivingEntity tame) {
        if (stack == null || stack.isEmpty() || data == null) return false;
        MinecraftServer server = tame == null ? null : tame.getServer();
        if (server != null) init(server);
        String type = typeId(tame, data);
        if (tame instanceof Animal animal) {
            try {
                if (animal.isFood(stack)) return true;
            } catch (Throwable ignored) { }
        }
        String item = itemId(stack);
        if (LEARNED.getOrDefault(type, Set.of()).contains(item)) return true;
        for (String rule : CUSTOM.getOrDefault(type, Set.of())) {
            if (matchesRule(stack, rule)) return true;
        }
        return false;
    }

    public static boolean addCustom(MinecraftServer server, String type, String food, int foodPoints) {
        init(server);
        String normalizedType = normalizeType(type);
        String rule = normalizeRule(food);
        if (normalizedType.isBlank() || rule == null || foodPoints <= 0) return false;
        boolean added = CUSTOM.computeIfAbsent(normalizedType, ignored -> new LinkedHashSet<>()).add(rule);
        Integer previous = CUSTOM_POINTS.computeIfAbsent(normalizedType, ignored -> new HashMap<>()).put(rule, foodPoints);
        boolean changed = added || previous == null || previous != foodPoints;
        if (changed) save();
        return changed;
    }

    public static boolean removeCustom(MinecraftServer server, String type, String food) {
        init(server);
        Set<String> rules = CUSTOM.get(normalizeType(type));
        String rule = normalizeRule(food);
        boolean changed = rules != null && rule != null && rules.remove(rule);
        Map<String, Integer> points = CUSTOM_POINTS.get(normalizeType(type));
        if (points != null && rule != null) points.remove(rule);
        if (rules != null && rules.isEmpty()) CUSTOM.remove(normalizeType(type));
        if (points != null && points.isEmpty()) CUSTOM_POINTS.remove(normalizeType(type));
        if (changed) save();
        return changed;
    }

    public static int clearCustom(MinecraftServer server, String type) {
        init(server);
        Set<String> removed = CUSTOM.remove(normalizeType(type));
        CUSTOM_POINTS.remove(normalizeType(type));
        if (removed != null) save();
        return removed == null ? 0 : removed.size();
    }

    public static List<String> foodsFor(MinecraftServer server, String type) {
        init(server);
        String normalized = normalizeType(type);
        Set<String> result = new LinkedHashSet<>(LEARNED.getOrDefault(normalized, Set.of()));
        for (String rule : CUSTOM.getOrDefault(normalized, Set.of())) {
            result.add(rule + " (" + CUSTOM_POINTS.getOrDefault(normalized, Map.of()).getOrDefault(rule, 1) + " points)");
        }
        return result.stream().sorted().toList();
    }

    public static int customFoodPoints(ItemStack stack, TameData data, LivingEntity tame) {
        if (stack == null || stack.isEmpty() || data == null) return 0;
        String type = typeId(tame, data);
        for (String rule : CUSTOM.getOrDefault(type, Set.of())) {
            if (matchesRule(stack, rule)) {
                return Math.max(1, CUSTOM_POINTS.getOrDefault(type, Map.of()).getOrDefault(rule, 1));
            }
        }
        return 0;
    }

    public static Collection<String> foodRuleSuggestions() {
        List<String> result = new ArrayList<>(List.of("meat", "fish", "fruit", "vegetable"));
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id != null) result.add(id.toString());
        }
        return result;
    }

    public static String typeId(LivingEntity tame, TameData data) {
        if (tame != null) {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
            if (id != null) return id.toString();
        }
        return normalizeType(data == null ? "" : data.type);
    }

    private static synchronized void init(MinecraftServer server) {
        if (server == null) return;
        Path path = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve("tameslevel-foods.json");
        if (path.equals(configPath)) return;
        configPath = path;
        LEARNED.clear(); CUSTOM.clear(); CUSTOM_POINTS.clear(); CURSORS.clear();
        candidates = ForgeRegistries.ITEMS.getValues().stream()
                .filter(Item::isEdible)
                .sorted(Comparator.comparing(item -> String.valueOf(ForgeRegistries.ITEMS.getKey(item))))
                .toList();
        if (!Files.isRegularFile(path)) return;
        try (Reader reader = Files.newBufferedReader(path)) {
            FoodConfig config = GSON.fromJson(reader, CONFIG_TYPE);
            copy(config == null ? null : config.learned, LEARNED);
            copy(config == null ? null : config.custom, CUSTOM);
            if (config != null && config.customPoints != null) {
                config.customPoints.forEach((type, values) -> CUSTOM_POINTS.put(normalizeType(type), new HashMap<>(values)));
            }
            if (config != null && config.completedTypes != null) {
                for (String type : config.completedTypes) CURSORS.put(normalizeType(type), candidates.size());
            }
        } catch (Exception ignored) { }
    }

    private static void copy(Map<String, Set<String>> source, Map<String, Set<String>> target) {
        if (source == null) return;
        source.forEach((type, values) -> target.put(normalizeType(type), new LinkedHashSet<>(values == null ? Set.of() : values)));
    }

    private static synchronized void save() {
        if (configPath == null) return;
        try {
            Files.createDirectories(configPath.getParent());
            FoodConfig config = new FoodConfig();
            config.learned.putAll(LEARNED);
            config.custom.putAll(CUSTOM);
            config.customPoints.putAll(CUSTOM_POINTS);
            CURSORS.forEach((type, cursor) -> { if (cursor >= candidates.size()) config.completedTypes.add(type); });
            try (Writer writer = Files.newBufferedWriter(configPath)) { GSON.toJson(config, CONFIG_TYPE, writer); }
        } catch (Exception ignored) { }
    }

    private static String normalizeType(String value) {
        if (value == null) return "";
        String result = value.trim().toLowerCase(Locale.ROOT);
        if (result.startsWith("entity.")) {
            result = result.substring(7);
            int dot = result.indexOf('.');
            if (dot > 0) result = result.substring(0, dot) + ":" + result.substring(dot + 1);
        }
        if (!result.isBlank() && !result.contains(":")) result = "minecraft:" + result;
        return result;
    }

    private static String normalizeRule(String value) {
        if (value == null || value.isBlank()) return null;
        String rule = value.trim().toLowerCase(Locale.ROOT);
        if (Set.of("meat", "fish", "fruit", "vegetable").contains(rule)) return "#" + rule;
        ResourceLocation id = ResourceLocation.tryParse(rule.contains(":") ? rule : "minecraft:" + rule);
        return id != null && ForgeRegistries.ITEMS.containsKey(id) ? id.toString() : null;
    }

    private static boolean matchesRule(ItemStack stack, String rule) {
        if (rule == null) return false;
        if (!rule.startsWith("#")) return rule.equals(itemId(stack));
        String id = itemId(stack).toLowerCase(Locale.ROOT);
        return switch (rule) {
            case "#meat" -> stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
            case "#fish" -> id.contains("fish") || id.contains("cod") || id.contains("salmon");
            case "#fruit" -> id.contains("apple") || id.contains("berry") || id.contains("melon") || id.contains("fruit");
            case "#vegetable" -> id.contains("carrot") || id.contains("potato") || id.contains("beetroot") || id.contains("vegetable");
            default -> false;
        };
    }

    private static String itemId(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }

    private static final class FoodConfig {
        Map<String, Set<String>> learned = new HashMap<>();
        Map<String, Set<String>> custom = new HashMap<>();
        Map<String, Map<String, Integer>> customPoints = new HashMap<>();
        Set<String> completedTypes = new LinkedHashSet<>();
    }
}
