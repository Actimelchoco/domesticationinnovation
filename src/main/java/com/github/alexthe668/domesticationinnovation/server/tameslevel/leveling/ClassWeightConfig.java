package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class ClassWeightConfig {
    private static final String RESOURCE_PATH = "data/domesticationinnovation/tameslevel/class_weights.json";

    record CategoryWeights(double base, double attribute, double ability) {
    }

    private static final class ClassWeights {
        private final CategoryWeights category;
        private final Map<String, Double> baseStats;
        private final Map<String, Double> attributes;
        private final Map<String, Double> abilities;
        private final Double preferredAttributeWeightMultiplier;
        private final Double preferredAbilityWeightMultiplier;
        private final Boolean autoPreferredWeightBalance;

        private ClassWeights(CategoryWeights category,
                             Map<String, Double> baseStats,
                             Map<String, Double> attributes,
                             Map<String, Double> abilities,
                             Double preferredAttributeWeightMultiplier,
                             Double preferredAbilityWeightMultiplier,
                             Boolean autoPreferredWeightBalance) {
            this.category = category;
            this.baseStats = baseStats;
            this.attributes = attributes;
            this.abilities = abilities;
            this.preferredAttributeWeightMultiplier = preferredAttributeWeightMultiplier;
            this.preferredAbilityWeightMultiplier = preferredAbilityWeightMultiplier;
            this.autoPreferredWeightBalance = autoPreferredWeightBalance;
        }
    }

    private final double preferredAttributeWeightMultiplier;
    private final double preferredAbilityWeightMultiplier;
    private final CategoryWeights defaultCategoryWeights;
    private final Map<String, Double> defaultBaseStatWeights;
    private final Map<TameClass, Double> classRollWeights;
    private final Map<TameClass, ClassWeights> classes;

    private ClassWeightConfig(double preferredAttributeWeightMultiplier,
                              double preferredAbilityWeightMultiplier,
                              CategoryWeights defaultCategoryWeights,
                              Map<String, Double> defaultBaseStatWeights,
                              Map<TameClass, Double> classRollWeights,
                              Map<TameClass, ClassWeights> classes) {
        this.preferredAttributeWeightMultiplier = preferredAttributeWeightMultiplier;
        this.preferredAbilityWeightMultiplier = preferredAbilityWeightMultiplier;
        this.defaultCategoryWeights = defaultCategoryWeights;
        this.defaultBaseStatWeights = defaultBaseStatWeights;
        this.classRollWeights = classRollWeights;
        this.classes = classes;
    }

    static ClassWeightConfig loadOrThrow() {
        try (InputStream stream = ClassWeightConfig.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing class weight config resource: " + RESOURCE_PATH);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                ClassWeightConfig config = fromJson(root);
                DomesticationMod.LOGGER.info("Loaded tame class weight config from {}", RESOURCE_PATH);
                return config;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read class weight config: " + RESOURCE_PATH, e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Invalid class weight config: " + RESOURCE_PATH, e);
        }
    }

    static String resourcePath() {
        return RESOURCE_PATH;
    }

    private static ClassWeightConfig fromJson(JsonObject root) {
        double preferredAttributeWeightMultiplier = getRequiredDouble(root, "preferredAttributeWeightMultiplier");
        double preferredAbilityWeightMultiplier = getRequiredDouble(root, "preferredAbilityWeightMultiplier");
        CategoryWeights defaultCategoryWeights = parseCategoryWeights(getRequiredObject(root, "defaultCategoryWeights"));
        Map<String, Double> defaultBaseStatWeights = parseWeightMap(getRequiredObject(root, "defaultBaseStatWeights"));

        Map<TameClass, ClassWeights> classes = new HashMap<>();
        JsonObject classesObject = getRequiredObject(root, "classes");
        for (Map.Entry<String, JsonElement> entry : classesObject.entrySet()) {
            TameClass tameClass = TameClass.ensureRegistered(entry.getKey());
            if (tameClass == null) {
                DomesticationMod.LOGGER.warn("Skipping unknown tame class '{}' in {}", entry.getKey(), RESOURCE_PATH);
                continue;
            }
            JsonObject classObject = entry.getValue().getAsJsonObject();
            CategoryWeights category = classObject.has("category")
                    ? parseCategoryWeights(classObject.getAsJsonObject("category"))
                    : null;
            Map<String, Double> baseStats = classObject.has("baseStats")
                    ? parseWeightMap(classObject.getAsJsonObject("baseStats"))
                    : Collections.emptyMap();
            Map<String, Double> attributes = classObject.has("attributes")
                    ? parseWeightMap(classObject.getAsJsonObject("attributes"))
                    : Collections.emptyMap();
            Map<String, Double> abilities = classObject.has("abilities")
                    ? parseWeightMap(classObject.getAsJsonObject("abilities"))
                    : Collections.emptyMap();
            Double classPreferredAttributeWeightMultiplier = classObject.has("preferredAttributeWeightMultiplier")
                    ? classObject.get("preferredAttributeWeightMultiplier").getAsDouble()
                    : null;
            Double classPreferredAbilityWeightMultiplier = classObject.has("preferredAbilityWeightMultiplier")
                    ? classObject.get("preferredAbilityWeightMultiplier").getAsDouble()
                    : null;
            Boolean autoPreferredWeightBalance = classObject.has("autoPreferredWeightBalance")
                    ? classObject.get("autoPreferredWeightBalance").getAsBoolean()
                    : null;
            classes.put(tameClass, new ClassWeights(
                    category,
                    baseStats,
                    attributes,
                    abilities,
                    classPreferredAttributeWeightMultiplier,
                    classPreferredAbilityWeightMultiplier,
                    autoPreferredWeightBalance
            ));
        }
        Map<TameClass, Double> classRollWeights = parseClassRollWeights(root);

        return new ClassWeightConfig(
                preferredAttributeWeightMultiplier,
                preferredAbilityWeightMultiplier,
                defaultCategoryWeights,
                defaultBaseStatWeights,
                classRollWeights,
                Collections.unmodifiableMap(classes)
        );
    }

    private static Map<TameClass, Double> parseClassRollWeights(JsonObject root) {
        if (!root.has("classRarity") || !root.get("classRarity").isJsonObject()) {
            return legacyClassRollWeights();
        }
        Map<TameClass, Double> weights = new LinkedHashMap<>();
        JsonObject classRarity = root.getAsJsonObject("classRarity");
        for (TameClass.Rarity rarity : TameClass.Rarity.values()) {
            String key = rarity.name().toLowerCase(Locale.ROOT);
            if (!classRarity.has(key) || !classRarity.get(key).isJsonArray()) {
                continue;
            }
            java.util.List<TameClass> bucket = new java.util.ArrayList<>();
            for (JsonElement element : classRarity.getAsJsonArray(key)) {
                TameClass tameClass = TameClass.ensureRegistered(element.getAsString());
                if (tameClass != null) {
                    bucket.add(tameClass);
                }
            }
            if (bucket.isEmpty()) {
                continue;
            }
            double perClassWeight = rarity.weight() / bucket.size();
            for (TameClass tameClass : bucket) {
                weights.put(tameClass, perClassWeight);
            }
        }
        return Collections.unmodifiableMap(weights);
    }

    private static Map<TameClass, Double> legacyClassRollWeights() {
        Map<TameClass, Double> weights = new LinkedHashMap<>();
        for (TameClass tameClass : TameClass.values()) {
            weights.put(tameClass, tameClass.rarity().weight());
        }
        return Collections.unmodifiableMap(weights);
    }

    private static CategoryWeights parseCategoryWeights(JsonObject object) {
        return new CategoryWeights(
                getRequiredDouble(object, "base"),
                getRequiredDouble(object, "attribute"),
                getRequiredDouble(object, "ability")
        );
    }

    private static Map<String, Double> parseWeightMap(JsonObject object) {
        Map<String, Double> weights = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            weights.put(normalize(entry.getKey()), entry.getValue().getAsDouble());
        }
        return Collections.unmodifiableMap(weights);
    }

    private static JsonObject getRequiredObject(JsonObject object, String memberName) {
        if (!object.has(memberName) || !object.get(memberName).isJsonObject()) {
            throw new IllegalStateException("Missing object '" + memberName + "'");
        }
        return object.getAsJsonObject(memberName);
    }

    private static double getRequiredDouble(JsonObject object, String memberName) {
        if (!object.has(memberName) || !object.get(memberName).isJsonPrimitive()) {
            throw new IllegalStateException("Missing number '" + memberName + "'");
        }
        return object.get(memberName).getAsDouble();
    }

    private static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    CategoryWeights categoryWeights(TameClass tameClass) {
        if (tameClass == null) {
            return defaultCategoryWeights;
        }
        ClassWeights weights = classes.get(tameClass);
        return weights != null && weights.category != null ? weights.category : defaultCategoryWeights;
    }

    double defaultBaseStatWeight(String rewardId, double fallback) {
        return defaultBaseStatWeights.getOrDefault(normalize(rewardId), fallback);
    }

    Map<TameClass, Double> classRollWeights() {
        return classRollWeights;
    }

    double baseStatMultiplier(TameClass tameClass, String rewardId) {
        return classWeight(tameClass, rewardId, weights -> weights.baseStats);
    }

    double attributeWeight(TameClass tameClass, String rewardId) {
        return classWeight(tameClass, rewardId, weights -> weights.attributes);
    }

    double abilityWeight(TameClass tameClass, String rewardId) {
        return classWeight(tameClass, rewardId, weights -> weights.abilities);
    }

    double preferredAttributeWeightMultiplier(TameClass tameClass) {
        if (tameClass == null) {
            return preferredAttributeWeightMultiplier;
        }
        ClassWeights weights = classes.get(tameClass);
        if (weights == null || weights.preferredAttributeWeightMultiplier == null) {
            return preferredAttributeWeightMultiplier;
        }
        return weights.preferredAttributeWeightMultiplier;
    }

    double preferredAttributeWeightMultiplier() {
        return preferredAttributeWeightMultiplier;
    }

    double preferredAbilityWeightMultiplier(TameClass tameClass) {
        if (tameClass == null) {
            return preferredAbilityWeightMultiplier;
        }
        ClassWeights weights = classes.get(tameClass);
        if (weights == null || weights.preferredAbilityWeightMultiplier == null) {
            return preferredAbilityWeightMultiplier;
        }
        return weights.preferredAbilityWeightMultiplier;
    }

    double preferredAbilityWeightMultiplier() {
        return preferredAbilityWeightMultiplier;
    }

    boolean autoPreferredWeightBalance(TameClass tameClass) {
        if (tameClass == null) {
            return false;
        }
        ClassWeights weights = classes.get(tameClass);
        return weights != null && Boolean.TRUE.equals(weights.autoPreferredWeightBalance);
    }

    private double classWeight(TameClass tameClass, String rewardId, java.util.function.Function<ClassWeights, Map<String, Double>> mapGetter) {
        if (tameClass == null) {
            return 1.0D;
        }
        ClassWeights weights = classes.get(tameClass);
        if (weights == null) {
            return 1.0D;
        }
        return mapGetter.apply(weights).getOrDefault(normalize(rewardId), 1.0D);
    }
}
