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
import java.util.LinkedHashMap;
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
    private static final Map<String, Integer> SUPERFOODS = new HashMap<>();
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
        for (String rule : SUPERFOODS.keySet()) {
            if (matchesRule(stack, rule)) return true;
        }
        return false;
    }

    public static boolean addSuperfood(MinecraftServer server, String food, int foodPoints) {
        init(server);
        String rule = normalizeRule(food);
        if (rule == null || foodPoints <= 0) return false;
        Integer previous = SUPERFOODS.put(rule, foodPoints);
        boolean changed = previous == null || previous != foodPoints;
        if (changed) save();
        return changed;
    }

    public static boolean removeSuperfood(MinecraftServer server, String food) {
        init(server);
        String rule = normalizeRule(food);
        boolean changed = rule != null && SUPERFOODS.remove(rule) != null;
        if (changed) save();
        return changed;
    }

    public static int defaultFoodPoints(String food) {
        String rule = normalizeRule(food);
        if (rule == null || rule.startsWith("#")) return 1;
        ResourceLocation id = ResourceLocation.tryParse(rule);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null || item.getFoodProperties() == null
                ? 1 : Math.max(1, item.getFoodProperties().getNutrition());
    }

    public static List<String> superfoodsForDisplay(MinecraftServer server) {
        init(server);
        List<String> result = new ArrayList<>();
        SUPERFOODS.forEach((rule, points) -> {
            String name = rule.startsWith("#")
                    ? "FoodType: " + titleCase(rule.substring(1)) : displayItemName(rule);
            result.add(name + " (" + Math.max(1, points) + " point" + (points == 1 ? "" : "s") + ")");
        });
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
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

    public static List<String> foodsForDisplay(MinecraftServer server, String type) {
        init(server);
        String normalized = normalizeType(type);
        Map<String, String> foodNames = new LinkedHashMap<>();
        Map<String, String> categories = new LinkedHashMap<>();
        for (String itemId : LEARNED.getOrDefault(normalized, Set.of())) {
            String displayName = displayItemName(itemId);
            if (!displayName.isBlank()) foodNames.put(displayName.toLowerCase(Locale.ROOT), displayName);
        }
        for (String rule : CUSTOM.getOrDefault(normalized, Set.of())) {
            if (rule.startsWith("#")) {
                int percentage = Math.max(1, CUSTOM_POINTS.getOrDefault(normalized, Map.of()).getOrDefault(rule, 100));
                String name = "FoodType: " + titleCase(rule.substring(1));
                categories.put(name.toLowerCase(Locale.ROOT), name + (percentage == 100 ? "" : " (" + percentage + "%)"));
            } else {
                String displayName = displayItemName(rule);
                int points = Math.max(1, CUSTOM_POINTS.getOrDefault(normalized, Map.of()).getOrDefault(rule, 1));
                if (!displayName.isBlank()) foodNames.put(displayName.toLowerCase(Locale.ROOT), configuredItemDisplay(rule, displayName, points));
            }
        }
        for (Map.Entry<String, Integer> entry : SUPERFOODS.entrySet()) {
            String rule = entry.getKey();
            int points = Math.max(1, entry.getValue());
            if (rule.startsWith("#")) {
                String name = "FoodType: " + titleCase(rule.substring(1));
                categories.put(name.toLowerCase(Locale.ROOT), name + " (" + points + " food point" + (points == 1 ? "" : "s") + ")");
            }
            else {
                String displayName = displayItemName(rule);
                if (!displayName.isBlank()) foodNames.put(displayName.toLowerCase(Locale.ROOT), configuredItemDisplay(rule, displayName, points));
            }
        }
        List<String> result = foodNames.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        result.addAll(categories.values().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList());
        return result;
    }

    private static String configuredItemDisplay(String rule, String displayName, int points) {
        ResourceLocation id = ResourceLocation.tryParse(rule);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        int nativePoints = item == null || item.getFoodProperties() == null
                ? 0 : Math.max(0, item.getFoodProperties().getNutrition());
        if (nativePoints > 0 && nativePoints == points) return displayName;
        return displayName + " (" + points + " food point" + (points == 1 ? "" : "s") + ")";
    }

    private static String displayItemName(String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? null : ForgeRegistries.ITEMS.getValue(id);
        return item == null ? titleCase(id == null ? itemId : id.getPath()) : item.getDescription().getString();
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "";
        String[] words = value.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return result.toString();
    }

    public static int customFoodPoints(ItemStack stack, TameData data, LivingEntity tame) {
        if (stack == null || stack.isEmpty() || data == null) return 0;
        String type = typeId(tame, data);
        for (String rule : CUSTOM.getOrDefault(type, Set.of())) {
            if (matchesRule(stack, rule)) {
                int configured = Math.max(1, CUSTOM_POINTS.getOrDefault(type, Map.of()).getOrDefault(rule, 1));
                if (rule.startsWith("#")) {
                    int nutrition = stack.getItem().getFoodProperties() == null
                            ? 0 : Math.max(0, stack.getItem().getFoodProperties().getNutrition());
                    if (nutrition <= 0) return 0;
                    return Math.max(1, (int) Math.round(nutrition * configured / 100.0D));
                }
                return configured;
            }
        }
        for (Map.Entry<String, Integer> entry : SUPERFOODS.entrySet()) {
            if (matchesRule(stack, entry.getKey())) return Math.max(1, entry.getValue());
        }
        return 0;
    }

    public static boolean isLearningComplete(MinecraftServer server, String type) {
        init(server);
        return CURSORS.getOrDefault(normalizeType(type), 0) >= candidates.size();
    }

    public static int nativeFoodCount(MinecraftServer server, String type) {
        init(server);
        return LEARNED.getOrDefault(normalizeType(type), Set.of()).size();
    }

    public static void resetLearning(MinecraftServer server, Collection<String> types) {
        init(server);
        if (types == null) return;
        for (String type : types) {
            String normalized = normalizeType(type);
            if (normalized.isBlank()) continue;
            LEARNED.remove(normalized);
            CURSORS.put(normalized, 0);
        }
        save();
    }

    public static int rescanInstant(MinecraftServer server, String type, Animal animal) {
        init(server);
        String normalized = normalizeType(type);
        if (normalized.isBlank() || animal == null) return -1;
        Set<String> learned = new LinkedHashSet<>();
        for (Item item : candidates) {
            try {
                if (animal.isFood(new ItemStack(item))) {
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    if (id != null) learned.add(id.toString());
                }
            } catch (Throwable ignored) { }
        }
        if (learned.isEmpty()) LEARNED.remove(normalized);
        else LEARNED.put(normalized, learned);
        CURSORS.put(normalized, candidates.size());
        save();
        return learned.size();
    }

    public static int customFoodPercentage(ItemStack stack, TameData data, LivingEntity tame) {
        if (stack == null || stack.isEmpty() || data == null) return 0;
        String type = typeId(tame, data);
        for (String rule : CUSTOM.getOrDefault(type, Set.of())) {
            if (rule.startsWith("#") && matchesRule(stack, rule)) {
                return Math.max(1, CUSTOM_POINTS.getOrDefault(type, Map.of()).getOrDefault(rule, 100));
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
        LEARNED.clear(); CUSTOM.clear(); CUSTOM_POINTS.clear(); SUPERFOODS.clear(); CURSORS.clear();
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
            if (config != null && config.superfoods != null) SUPERFOODS.putAll(config.superfoods);
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
            config.superfoods.putAll(SUPERFOODS);
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
        if (id != null && "minecraft".equals(id.getNamespace())
                && Set.of("meat", "fish", "fruit", "vegetable").contains(id.getPath())) {
            return "#" + id.getPath();
        }
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
        Map<String, Integer> superfoods = new HashMap<>();
        Set<String> completedTypes = new LinkedHashSet<>();
    }
}
