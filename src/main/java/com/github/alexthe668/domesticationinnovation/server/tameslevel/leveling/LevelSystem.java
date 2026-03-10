package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public class LevelSystem {

    public static final int BASE_XP = 50;
    public static final int XP_PER_LEVEL_STEP = 3;
    public static final double DEATH_XP_LOSS = 0.67D;
    public static final double ABILITY_UPGRADE_EXISTING_CHANCE = 0.625D;
    public static final double ATTRIBUTE_UPGRADE_EXISTING_CHANCE = 0.50D;

    private static final Random RANDOM = new Random();

    // mobUUID -> set of tameUUID
    public static final Map<UUID, Set<UUID>> mobDamageTracker = new HashMap<>();

    private enum BaseStatReward {
        HP("HP", Attributes.MAX_HEALTH, 2.0D),
        DAMAGE("Damage", Attributes.ATTACK_DAMAGE, 1.0D),
        SPEED("Speed", Attributes.MOVEMENT_SPEED, 0.01D),
        ARMOR("Armor", Attributes.ARMOR, 1.0D),
        ARMOR_TOUGHNESS("Armor Toughness", Attributes.ARMOR_TOUGHNESS, 1.0D),
        KNOCKBACK("Attack Knockback", Attributes.ATTACK_KNOCKBACK, 0.5D),
        KNOCKBACK_RESIST("Knockback Resistance", Attributes.KNOCKBACK_RESISTANCE, 0.05D);

        private final String display;
        private final Attribute attribute;
        private final double amount;

        BaseStatReward(String display, Attribute attribute, double amount) {
            this.display = display;
            this.attribute = attribute;
            this.amount = amount;
        }
    }

    private enum AttributeReward {
        SPEED("speed", Integer.MAX_VALUE),
        RESISTANCE("resistance", Integer.MAX_VALUE),
        STRENGTH("strength", Integer.MAX_VALUE),
        FIRE_RESISTANCE("fire_resistance", Integer.MAX_VALUE),
        POISON_RESISTANCE("poison_resistance", Integer.MAX_VALUE),
        ABILITY_POWER("ability_power", Integer.MAX_VALUE),
        LIFESTEAL("lifesteal", Integer.MAX_VALUE),
        REGENERATION("regeneration", Integer.MAX_VALUE),
        REJUVENATION("rejuvenation", Integer.MAX_VALUE),
        FIREFANG("firefang", Integer.MAX_VALUE),
        POISON_FANG("poison_fang", Integer.MAX_VALUE),
        WITHERFANG("witherfang", Integer.MAX_VALUE),
        LIGHTNINGFANG("lightningfang", Integer.MAX_VALUE),
        EMERGENCY_COOLDOWN_REDUCTION("emergency_cooldown_reduction", Integer.MAX_VALUE),
        KILLER("killer", Integer.MAX_VALUE),
        PACIFIST("pacifist", Integer.MAX_VALUE),
        BOSSKILLER("bosskiller", Integer.MAX_VALUE),
        KILLEXPLODER("killexploder", Integer.MAX_VALUE),
        TOTEM("totem", 5),
        JUMP_BOOST("jump_boost", Integer.MAX_VALUE),
        FEATHER_FALLING("feather_falling", Integer.MAX_VALUE),
        EXPLOSION_RESISTANCE("explosion_resistance", Integer.MAX_VALUE),
        SMITE("smite", Integer.MAX_VALUE),
        BANE_OF_ARTHROPODS("bane_of_arthropods", Integer.MAX_VALUE),
        POSITIVE_EFFECT_STEAL("positive_effect_steal", Integer.MAX_VALUE),
        NEGATIVE_EFFECT_TRANSFER("negative_effect_transfer", Integer.MAX_VALUE),
        SWEEPING_EDGE("sweeping_edge", Integer.MAX_VALUE),
        CHAIN_LIGHTNING("chain_lightning", Integer.MAX_VALUE),
        FROST_FANG("frost_fang", Integer.MAX_VALUE),
        MAGNETIC("magnetic", Integer.MAX_VALUE),
        LINKED_INVENTORY("linked_inventory", 1),
        HEALTH_SIPHON("health_siphon", Integer.MAX_VALUE),
        BUBBLING("bubbling", Integer.MAX_VALUE),
        HERDING("herding", Integer.MAX_VALUE),
        AMPHIBIOUS("amphibious", 1),
        VOID_CLOUD("void_cloud", 1),
        CHARISMA("charisma", Integer.MAX_VALUE),
        DISC_JOCKEY("disc_jockey", 1),
        WARPING_BITE("warping_bite", Integer.MAX_VALUE),
        ORE_SCENTING("ore_scenting", 1),
        GLUTTONOUS("gluttonous", 1),
        TETHERED_TELEPORT("tethered_teleport", 1),
        MUFFLED("muffled", 1),
        BLAZING_PROTECTION("blazing_protection", Integer.MAX_VALUE);

        private final String id;
        private final int maxLevel;

        AttributeReward(String id, int maxLevel) {
            this.id = id;
            this.maxLevel = maxLevel;
        }
    }

    private enum AbilityReward {
        CREEPER_EXPLOSION("creeper_explosion", true, Integer.MAX_VALUE),
        ARROW_SHOT("arrow_shot", true, Integer.MAX_VALUE),
        GHAST_FIREBALL("ghast_fireball", true, Integer.MAX_VALUE),
        BATTLE_STRENGTH("battle_strength", true, Integer.MAX_VALUE),
        DEFENSIVE_AURA("defensive_aura", true, Integer.MAX_VALUE),
        SNOWBALL_SHOT("snowball_shot", true, Integer.MAX_VALUE),
        ENDER_PEARL_JUMP("ender_pearl_jump", true, Integer.MAX_VALUE),
        LIGHTNING_STRIKE("lightning_strike", true, Integer.MAX_VALUE),
        WARDEN_SCREAM("warden_scream", true, Integer.MAX_VALUE),
        WITHER_SKULL("wither_skull", true, Integer.MAX_VALUE),
        BLAZE_ATTACK("blaze_attack", true, Integer.MAX_VALUE),
        GUARDIAN_BEAM("guardian_beam", true, Integer.MAX_VALUE),
        ELDER_GUARDIAN_BEAM("elder_guardian_beam", true, Integer.MAX_VALUE),
        BERSERKER("berserker", true, Integer.MAX_VALUE),
        BLOODLUST("bloodlust", true, Integer.MAX_VALUE),
        TRIDENT("trident", true, Integer.MAX_VALUE),
        CROSSBOW("crossbow", true, Integer.MAX_VALUE),
        EVOKER_FANGS("evoker_fangs", true, Integer.MAX_VALUE),
        SHULKER_BULLET("shulker_bullet", true, Integer.MAX_VALUE),
        DRAGON_FIREBALL("dragon_fireball", true, Integer.MAX_VALUE),
        LLAMA_SPIT("llama_spit", true, Integer.MAX_VALUE),
        FISHING("fishing", true, Integer.MAX_VALUE),
        DASH("dash", true, Integer.MAX_VALUE),
        RETALIATION_SLOW("retaliation_slow", true, Integer.MAX_VALUE),
        IMMUNITY_FRAME("immunity_frame", true, Integer.MAX_VALUE),
        DEFLECTION("deflection", false, 1),
        DEFUSAL("defusal", true, Integer.MAX_VALUE),
        SHADOW_HANDS("shadow_hands", true, Integer.MAX_VALUE),
        PSYCHIC_WALL("psychic_wall", true, Integer.MAX_VALUE),
        HEALING_AURA("healing_aura", true, Integer.MAX_VALUE),
        HEALING_BOTTLE("healing_bottle", true, Integer.MAX_VALUE),
        GUARDIAN_REPULSE("guardian_repulse", true, Integer.MAX_VALUE),
        LAST_STAND_FURY("last_stand_fury", true, Integer.MAX_VALUE),
        SHIELD_BLOCK("shield_block", true, Integer.MAX_VALUE),
        SKY_LAUNCH("sky_launch", true, Integer.MAX_VALUE);

        private final String id;
        private final boolean upgradable;
        private final int maxLevel;

        AbilityReward(String id, boolean upgradable, int maxLevel) {
            this.id = id;
            this.upgradable = upgradable;
            this.maxLevel = maxLevel;
        }
    }

    private record WeightedOption<T>(T value, double weight) {}

    private static final Set<String> KNOWN_ABILITIES;
    private static final Set<String> KNOWN_ATTRIBUTES;
    private static final Map<String, String> ABILITY_ALIASES;
    static {
        Set<String> ids = new LinkedHashSet<>();
        for (AbilityReward value : AbilityReward.values()) {
            ids.add(value.id);
        }
        Set<String> attributes = new LinkedHashSet<>();
        for (AttributeReward value : AttributeReward.values()) {
            attributes.add(value.id);
        }
        Map<String, String> aliases = new HashMap<>();

        KNOWN_ABILITIES = Collections.unmodifiableSet(new LinkedHashSet<>() {{
            addAll(ids);
            addAll(aliases.keySet());
        }});
        KNOWN_ATTRIBUTES = Collections.unmodifiableSet(attributes);
        ABILITY_ALIASES = Collections.unmodifiableMap(aliases);
    }

    // ===============================
    // DAMAGE TRACKING
    // ===============================

    public static void trackDamage(LivingEntity mob, TamableAnimal tame) {
        mobDamageTracker.computeIfAbsent(mob.getUUID(), k -> new HashSet<>()).add(tame.getUUID());
    }

    // ===============================
    // XP DISTRIBUTION
    // ===============================

    public static void distributeXP(LivingEntity dead, LivingEntity killer) {
        UUID mobId = dead.getUUID();
        Set<UUID> tameIds = mobDamageTracker.get(mobId);
        if (tameIds == null || tameIds.isEmpty()) {
            return;
        }

        int xpAmount = dead.getExperienceReward();
        for (UUID tameId : tameIds) {
            TameData data = TameRegistry.get(tameId);
            if (data == null) {
                continue;
            }

            double gainedXP;
            if (killer != null && killer.getUUID().equals(tameId)) {
                data.kills++;
                gainedXP = xpAmount;
            } else {
                data.assists++;
                gainedXP = xpAmount * 0.25D;
            }

            data.xp += (int) Math.round(gainedXP);

            Entity entity = dead.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(tameId) : null;
            if (entity instanceof TamableAnimal tame) {
                checkLevelUp(tame, data);
            }
        }

        TameRegistry.markDirty();
        mobDamageTracker.remove(mobId);
    }

    // ===============================
    // XP LOSS ON DEATH
    // ===============================

    public static void onTameDeath(TamableAnimal tame) {
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            return;
        }

        data.xp = (int) Math.floor(data.xp * (1.0D - DEATH_XP_LOSS));
        data.deaths++;
        TameRegistry.markDirty();
    }

    public static Set<String> knownAbilityIds() {
        return KNOWN_ABILITIES;
    }

    public static Set<String> knownAttributeIds() {
        return KNOWN_ATTRIBUTES;
    }

    public static int getAttributeLevel(TameData data, String attributeId) {
        if (attributeId == null) {
            return 0;
        }
        return data.attributeLevels.getOrDefault(attributeId.trim().toLowerCase(java.util.Locale.ROOT), 0);
    }

    public static boolean addAttribute(TameData data, String attributeId, int amount) {
        if (attributeId == null) {
            return false;
        }
        String id = attributeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!KNOWN_ATTRIBUTES.contains(id)) {
            return false;
        }
        AttributeReward reward = byAttributeId(id);
        if (reward == null) {
            return false;
        }
        int current = data.attributeLevels.getOrDefault(id, 0);
        int next = Math.min(reward.maxLevel, current + Math.max(1, amount));
        if (next == current) {
            return false;
        }
        data.attributeLevels.put(id, next);
        TameRegistry.markDirty();
        return true;
    }

    public static boolean removeAttribute(TameData data, String attributeId, int amount) {
        if (attributeId == null) {
            return false;
        }
        String id = attributeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (!KNOWN_ATTRIBUTES.contains(id)) {
            return false;
        }
        int current = data.attributeLevels.getOrDefault(id, 0);
        if (current <= 0) {
            return false;
        }
        int next = current - Math.max(1, amount);
        if (next > 0) {
            data.attributeLevels.put(id, next);
        } else {
            data.attributeLevels.remove(id);
        }
        TameRegistry.markDirty();
        return true;
    }

    public static boolean hasAbility(TameData data, String abilityId) {
        String canonical = canonicalAbilityId(abilityId);
        return resolveAbilityLevel(data, canonical) > 0
                || data.abilities.contains(canonical)
                || hasLegacyAlias(data, canonical);
    }

    public static int getAbilityLevel(TameData data, String abilityId) {
        return resolveAbilityLevel(data, canonicalAbilityId(abilityId));
    }

    public static boolean addAbility(TameData data, String abilityId, int amount) {
        abilityId = canonicalAbilityId(abilityId);
        if (!KNOWN_ABILITIES.contains(abilityId)) {
            return false;
        }
        AbilityReward reward = byAbilityId(abilityId);
        if (reward == null) {
            return false;
        }
        int current = resolveAbilityLevel(data, abilityId);
        int next = Math.min(reward.maxLevel, current + Math.max(1, amount));
        if (next == current) {
            return false;
        }
        normalizeAliasesForAbility(data, abilityId);
        data.abilityLevels.put(abilityId, next);
        data.abilities.add(abilityId);
        TameRegistry.markDirty();
        return true;
    }

    public static boolean removeAbility(TameData data, String abilityId, int amount) {
        abilityId = canonicalAbilityId(abilityId);
        if (!KNOWN_ABILITIES.contains(abilityId)) {
            return false;
        }
        int current = resolveAbilityLevel(data, abilityId);
        if (current <= 0) {
            return false;
        }
        normalizeAliasesForAbility(data, abilityId);
        int next = current - Math.max(1, amount);
        if (next > 0) {
            data.abilityLevels.put(abilityId, next);
        } else {
            data.abilityLevels.remove(abilityId);
            data.abilities.remove(abilityId);
        }
        TameRegistry.markDirty();
        return true;
    }

    public static void grantXP(TamableAnimal tame, TameData data, int amount) {
        if (amount == 0) {
            return;
        }
        data.xp = Math.max(0, data.xp + amount);
        checkLevelUp(tame, data);
        TameRegistry.markDirty();
    }

    // ===============================
    // CLASS ROLLING
    // ===============================

    public static TameClass rollClass() {
        TameClass[] values = TameClass.values();
        return values[RANDOM.nextInt(values.length)];
    }

    public static void ensureClassAssigned(TamableAnimal tame, TameData data, boolean notifyOwner) {
        if (data.tameClass != null) {
            return;
        }

        data.tameClass = rollClass();
        TameRegistry.markDirty();
        if (!notifyOwner) {
            return;
        }

        if (tame.getOwner() instanceof Player owner) {
            owner.sendSystemMessage(Component.literal(
                    "§b" + data.name + " class assigned: §e" + data.tameClass.name()
            ));
        }
    }

    // ===============================
    // LEVEL UP CHECK
    // ===============================

    public static void checkLevelUp(TamableAnimal tame, TameData data) {
        ensureClassAssigned(tame, data, false);
        data.xpToNext = xpRequiredForLevel(data.level);
        boolean leveled = false;

        while (data.xp >= data.xpToNext) {
            leveled = true;
            data.xp -= data.xpToNext;
            data.level++;
            data.xpToNext = xpRequiredForLevel(data.level);

            String rewardSummary = applyLevelReward(tame, data);
            updateTameName(tame, data);

            if (tame.getOwner() instanceof Player owner && PlayerDebugSettings.levelUp(owner.getUUID())) {
                owner.sendSystemMessage(Component.literal(
                        "§6Your pet §e" + data.name + " §6leveled up to §eLevel " + data.level + "§6."
                ));
                if (!rewardSummary.isEmpty()) {
                    owner.sendSystemMessage(Component.literal("§7Reward: §f" + rewardSummary));
                }
            }
        }
        if (leveled) {
            TameRegistry.markDirty();
        }
    }

    // ===============================
    // REWARD ROLLING
    // ===============================

    public static String applyLevelReward(TamableAnimal tame, TameData data) {
        RewardCategory category = rollCategory(data);
        String primary = switch (category) {
            case BASE_STAT -> applyBaseStatReward(tame, data);
            case ATTRIBUTE -> applyAttributeReward(tame, data, true);
            case ABILITY -> applyAbilityReward(tame, data, true);
        };
        List<String> guaranteed = grantGuaranteedAttributesForLevel(data);
        if (guaranteed.isEmpty()) {
            return primary;
        }
        if (primary == null || primary.isBlank()) {
            return String.join(" + ", guaranteed);
        }
        return primary + " + " + String.join(" + ", guaranteed);
    }

    private static RewardCategory rollCategory(TameData data) {
        if (data.level % 30 == 0) {
            return RewardCategory.ABILITY;
        }
        if (data.level % 20 == 0) {
            return RewardCategory.ATTRIBUTE;
        }

        double baseChance = 0.93D;
        double attributeChance = 0.05D;
        double abilityChance = 0.02D;

        double baseMult = 1.0D;
        double attributeMult = 1.0D;
        double abilityMult = 1.0D;

        if (data.tameClass != null) {
            switch (data.tameClass) {
                case TANKER -> {
                    baseMult = 1.40D;
                    attributeMult = 0.65D;
                    abilityMult = 0.50D;
                }
                case DPS -> {
                    baseMult = 1.30D;
                    attributeMult = 0.70D;
                    abilityMult = 0.80D;
                }
                case ASSASSIN -> {
                    baseMult = 1.25D;
                    attributeMult = 0.75D;
                    abilityMult = 1.15D;
                }
                case PROTECTOR -> {
                    baseMult = 1.20D;
                    attributeMult = 1.20D;
                    abilityMult = 0.75D;
                }
                case MAGE -> {
                    baseMult = 0.60D;
                    attributeMult = 1.20D;
                    abilityMult = 2.80D;
                }
                case SHOOTER -> {
                    baseMult = 0.90D;
                    attributeMult = 0.80D;
                    abilityMult = 1.80D;
                }
                case MANIAC -> {
                    baseMult = 0.45D;
                    attributeMult = 0.90D;
                    abilityMult = 3.20D;
                }
                case ATTRIBUTER -> {
                    baseMult = 0.35D;
                    attributeMult = 3.50D;
                    abilityMult = 0.55D;
                }
            }
        }

        baseChance *= baseMult;
        attributeChance *= attributeMult;
        abilityChance *= abilityMult;

        double total = baseChance + attributeChance + abilityChance;
        if (total <= 0.0D) {
            return RewardCategory.BASE_STAT;
        }

        double roll = RANDOM.nextDouble() * total;
        if (roll < baseChance) {
            return RewardCategory.BASE_STAT;
        }
        if (roll < baseChance + attributeChance) {
            return RewardCategory.ATTRIBUTE;
        }
        return RewardCategory.ABILITY;
    }

    private static String applyBaseStatReward(TamableAnimal tame, TameData data) {
        List<WeightedOption<BaseStatReward>> options = new ArrayList<>();
        for (BaseStatReward reward : BaseStatReward.values()) {
            options.add(new WeightedOption<>(reward, modifiedBaseStatWeight(data.tameClass, reward)));
        }

        BaseStatReward reward = pickWeighted(options);
        addToAttribute(tame, reward.attribute, reward.amount);
        trackBonus(data, reward);
        if (reward == BaseStatReward.HP) {
            tame.setHealth(tame.getMaxHealth());
        }
        TameRegistry.markDirty();
        return reward.display + " +" + formatDouble(reward.amount);
    }

    private static String applyAttributeReward(TamableAnimal tame, TameData data, boolean allowAbilityFallback) {
        AttributeReward upgraded = tryUpgradeExistingAttribute(data);
        if (upgraded != null) {
            int newLevel = data.attributeLevels.get(upgraded.id);
            return upgraded.id + " upgraded to " + roman(newLevel);
        }

        List<WeightedOption<AttributeReward>> options = new ArrayList<>();
        for (AttributeReward reward : AttributeReward.values()) {
            if (isGuaranteedOnlyAttribute(reward.id)) {
                continue;
            }
            int current = data.attributeLevels.getOrDefault(reward.id, 0);
            if (current >= reward.maxLevel) {
                continue;
            }
            double weight = modifiedAttributeWeight(data.tameClass, reward) * ownedAttributeRollMultiplier(current);
            options.add(new WeightedOption<>(reward, weight));
        }
        if (options.isEmpty()) {
            if (allowAbilityFallback) {
                return applyAbilityReward(tame, data, false);
            }
            return applyBaseStatReward(tame, data);
        }

        AttributeReward rolled = pickWeighted(options);
        int current = data.attributeLevels.getOrDefault(rolled.id, 0);
        data.attributeLevels.put(rolled.id, current + 1);
        TameRegistry.markDirty();
        return rolled.id + " " + roman(current + 1);
    }

    private static String applyAbilityReward(TamableAnimal tame, TameData data, boolean allowAttributeFallback) {
        AbilityReward upgraded = tryUpgradeExistingAbility(data);
        if (upgraded != null) {
            int newLevel = data.abilityLevels.get(upgraded.id);
            return upgraded.id + " upgraded to " + roman(newLevel);
        }

        List<WeightedOption<AbilityReward>> options = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            int current = resolveAbilityLevel(data, reward.id);
            if (current >= reward.maxLevel) {
                continue;
            }
            double weight = modifiedAbilityWeight(data.tameClass, reward) * ownedAbilityRollMultiplier(current);
            options.add(new WeightedOption<>(reward, weight));
        }
        if (options.isEmpty()) {
            if (allowAttributeFallback) {
                return applyAttributeReward(tame, data, false);
            }
            return applyBaseStatReward(tame, data);
        }

        AbilityReward rolled = pickWeighted(options);
        int current = resolveAbilityLevel(data, rolled.id);
        if (current == 0) {
            normalizeAliasesForAbility(data, rolled.id);
            data.abilities.add(rolled.id);
            data.abilityLevels.put(rolled.id, 1);
            TameRegistry.markDirty();
            return "Unlocked " + rolled.id + " I";
        }

        if (rolled.upgradable && current < rolled.maxLevel) {
            normalizeAliasesForAbility(data, rolled.id);
            data.abilityLevels.put(rolled.id, current + 1);
            data.abilities.add(rolled.id);
            TameRegistry.markDirty();
            return rolled.id + " upgraded to " + roman(current + 1);
        }

        return applyBaseStatReward(tame, data);
    }

    private static AbilityReward tryUpgradeExistingAbility(TameData data) {
        AbilityReward existing = pickRandomOwnedUpgradeableAbility(data);
        if (existing == null || RANDOM.nextDouble() > abilityUpgradeChance(data)) {
            return null;
        }
        int level = data.abilityLevels.getOrDefault(existing.id, 0);
        data.abilityLevels.put(existing.id, level + 1);
        TameRegistry.markDirty();
        return existing;
    }

    private static AbilityReward pickRandomOwnedUpgradeableAbility(TameData data) {
        List<AbilityReward> pool = new ArrayList<>();
        List<WeightedOption<AbilityReward>> weighted = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            int current = data.abilityLevels.getOrDefault(reward.id, 0);
            if (current > 0 && reward.upgradable && current < reward.maxLevel) {
                pool.add(reward);
                weighted.add(new WeightedOption<>(reward, ownedAbilityRollMultiplier(current)));
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pickWeighted(weighted);
    }

    private static AttributeReward tryUpgradeExistingAttribute(TameData data) {
        AttributeReward existing = pickRandomOwnedUpgradeableAttribute(data);
        if (existing == null || RANDOM.nextDouble() > ATTRIBUTE_UPGRADE_EXISTING_CHANCE) {
            return null;
        }
        int level = data.attributeLevels.getOrDefault(existing.id, 0);
        data.attributeLevels.put(existing.id, level + 1);
        TameRegistry.markDirty();
        return existing;
    }

    private static AttributeReward pickRandomOwnedUpgradeableAttribute(TameData data) {
        List<AttributeReward> pool = new ArrayList<>();
        List<WeightedOption<AttributeReward>> weighted = new ArrayList<>();
        for (AttributeReward reward : AttributeReward.values()) {
            if (isGuaranteedOnlyAttribute(reward.id)) {
                continue;
            }
            int current = data.attributeLevels.getOrDefault(reward.id, 0);
            if (current > 0 && current < reward.maxLevel) {
                pool.add(reward);
                weighted.add(new WeightedOption<>(reward, ownedAttributeRollMultiplier(current)));
            }
        }
        if (pool.isEmpty()) {
            return null;
        }
        return pickWeighted(weighted);
    }

    private static double ownedAbilityRollMultiplier(int currentLevel) {
        if (currentLevel <= 0) {
            return 1.0D;
        }
        if (currentLevel < 5) {
            return 5.0D;
        }
        return 1.5D;
    }

    private static double ownedAttributeRollMultiplier(int currentLevel) {
        if (currentLevel <= 0) {
            return 1.0D;
        }
        if (currentLevel < 5) {
            return 3.0D;
        }
        return 1.5D;
    }

    private static double abilityUpgradeChance(TameData data) {
        return ABILITY_UPGRADE_EXISTING_CHANCE;
    }

    private static int ownedAbilityCount(TameData data) {
        int count = 0;
        for (AbilityReward reward : AbilityReward.values()) {
            if (resolveAbilityLevel(data, reward.id) > 0) {
                count++;
            }
        }
        return count;
    }

    private static double modifiedBaseStatWeight(TameClass tameClass, BaseStatReward reward) {
        double base = switch (reward) {
            case HP -> 59.7D;
            case DAMAGE -> 20.0D;
            case SPEED -> 3.0D;
            case ARMOR -> 5.0D;
            case ARMOR_TOUGHNESS -> 2.0D;
            case KNOCKBACK -> 5.0D;
            case KNOCKBACK_RESIST -> 5.0D;
        };

        if (tameClass == null) {
            return base;
        }

        return switch (tameClass) {
            case TANKER -> switch (reward) {
                case HP -> base * 3.0D;
                case ARMOR, ARMOR_TOUGHNESS -> base * 3.0D;
                default -> base;
            };
            case DPS -> switch (reward) {
                case DAMAGE -> base * 3.0D;
                default -> base;
            };
            case ASSASSIN -> switch (reward) {
                case SPEED, DAMAGE -> base * 2.8D;
                default -> base;
            };
            case PROTECTOR -> switch (reward) {
                case SPEED, KNOCKBACK, KNOCKBACK_RESIST -> base * 2.8D;
                default -> base;
            };
            case MAGE -> switch (reward) {
                case SPEED -> base * 0.8D;
                default -> base * 0.7D;
            };
            case SHOOTER -> switch (reward) {
                case DAMAGE, SPEED -> base * 1.8D;
                default -> base;
            };
            case MANIAC -> base * 0.50D;
            case ATTRIBUTER -> base * 0.40D;
        };
    }

    private static double modifiedAttributeWeight(TameClass tameClass, AttributeReward reward) {
        double base = 1.0D;
        if (tameClass == null) {
            return base;
        }

        return switch (tameClass) {
            case ATTRIBUTER -> base * 3.5D;
            case MANIAC -> base * 1.8D;
            case TANKER -> switch (reward) {
                case RESISTANCE -> base * 4.6D;
                case REGENERATION -> base * 4.2D;
                case FIRE_RESISTANCE -> base * 3.8D;
                case POISON_RESISTANCE -> base * 3.8D;
                case TOTEM -> base * 3.6D;
                case PACIFIST -> base * 2.8D;
                case HEALTH_SIPHON -> base * 2.5D;
                case BLAZING_PROTECTION -> base * 4.0D;
                default -> base;
            };
            case ASSASSIN -> switch (reward) {
                case SPEED -> base * 3.2D;
                case STRENGTH -> base * 3.4D;
                case KILLER -> base * 4.0D;
                case LIFESTEAL -> base * 3.0D;
                case REJUVENATION -> base * 4.0D;
                case FIREFANG -> base * 2.6D;
                case WITHERFANG -> base * 3.0D;
                case LIGHTNINGFANG -> base * 1.6D;
                case CHAIN_LIGHTNING -> base * 4.0D;
                case FROST_FANG -> base * 2.6D;
                case MAGNETIC -> base * 4.0D;
                case BUBBLING -> base * 2.5D;
                case AMPHIBIOUS -> base * 4.0D;
                case VOID_CLOUD -> base * 2.5D;
                case WARPING_BITE -> base * 4.0D;
                case MUFFLED -> base * 1.5D;
                default -> base;
            };
            case DPS -> switch (reward) {
                case STRENGTH -> base * 4.0D;
                case ABILITY_POWER -> base * 3.2D;
                case KILLER -> base * 3.4D;
                case LIFESTEAL -> base * 2.8D;
                case FIREFANG -> base * 3.0D;
                case LIGHTNINGFANG -> base * 1.6D;
                case SWEEPING_EDGE -> base * 4.0D;
                case CHAIN_LIGHTNING -> base * 2.5D;
                default -> base;
            };
            case PROTECTOR -> switch (reward) {
                case RESISTANCE -> base * 4.2D;
                case REGENERATION -> base * 4.0D;
                case FIRE_RESISTANCE -> base * 3.6D;
                case POISON_RESISTANCE -> base * 3.6D;
                case TOTEM -> base * 4.2D;
                case PACIFIST -> base * 3.2D;
                case POSITIVE_EFFECT_STEAL -> base * 2.8D;
                case LINKED_INVENTORY -> base * 2.5D;
                case HERDING -> base * 2.5D;
                case CHARISMA -> base * 2.5D;
                case ORE_SCENTING -> base * 2.5D;
                default -> base;
            };
            case MAGE -> switch (reward) {
                case ABILITY_POWER -> base * 4.2D;
                case REJUVENATION -> base * 2.5D;
                case LIGHTNINGFANG -> base * 3.2D;
                case WITHERFANG -> base * 3.2D;
                case FIREFANG -> base * 3.0D;
                case NEGATIVE_EFFECT_TRANSFER -> base * 3.2D;
                case POSITIVE_EFFECT_STEAL -> base * 2.6D;
                case CHAIN_LIGHTNING -> base * 2.5D;
                default -> base;
            };
            case SHOOTER -> switch (reward) {
                case SPEED -> base * 3.4D;
                case STRENGTH -> base * 3.0D;
                case KILLER -> base * 3.2D;
                case LIGHTNINGFANG -> base * 2.6D;
                case FIREFANG -> base * 2.6D;
                default -> base;
            };
        };
    }

    private static double modifiedAbilityWeight(TameClass tameClass, AbilityReward reward) {
        double base = 1.0D;
        if (tameClass == null) {
            return base;
        }

        return switch (tameClass) {
            case MANIAC -> base * 3.0D;
            case TANKER -> switch (reward) {
                case DEFENSIVE_AURA -> base * 4.8D;
                case BERSERKER -> base * 3.7D;
                case IMMUNITY_FRAME -> base * 7.0D;
                case DEFLECTION -> base * 4.0D;
                case DEFUSAL -> base * 7.0D;
                case GUARDIAN_REPULSE -> base * 4.8D;
                case LAST_STAND_FURY -> base * 2.8D;
                case SHIELD_BLOCK -> base * 4.6D;
                case SKY_LAUNCH -> base * 4.4D;
                case WARDEN_SCREAM -> base * 1.0D;
                case EVOKER_FANGS -> base * 1.8D;
                default -> base;
            };
            case ASSASSIN -> switch (reward) {
                case SHULKER_BULLET -> base * 2.0D;
                case WITHER_SKULL -> base * 2.4D;
                case BLOODLUST -> base * 3.6D;
                case BERSERKER -> base * 3.2D;
                case ENDER_PEARL_JUMP -> base * 4.2D;
                case WARDEN_SCREAM -> base * 2.8D;
                default -> base;
            };
            case PROTECTOR -> switch (reward) {
                case DEFENSIVE_AURA -> base * 4.6D;
                case BATTLE_STRENGTH -> base * 3.2D;
                case DEFUSAL -> base * 4.0D;
                case PSYCHIC_WALL -> base * 4.0D;
                case HEALING_AURA -> base * 10.0D;
                case HEALING_BOTTLE -> base * 8.0D;
                case GUARDIAN_REPULSE -> base * 4.8D;
                case LAST_STAND_FURY -> base * 4.8D;
                case SHIELD_BLOCK -> base * 4.6D;
                case SKY_LAUNCH -> base * 4.4D;
                case ELDER_GUARDIAN_BEAM -> base * 1.8D;
                case WARDEN_SCREAM -> base * 1.8D;
                default -> base;
            };
            case DPS -> switch (reward) {
                case BLOODLUST -> base * 4.2D;
                case BERSERKER -> base * 3.9D;
                case ARROW_SHOT -> base * 1.5D;
                case LIGHTNING_STRIKE -> base * 2.5D;
                case WARDEN_SCREAM -> base * 1.8D;
                default -> base;
            };
            case MAGE -> switch (reward) {
                case DRAGON_FIREBALL -> base * 4.6D;
                case GUARDIAN_BEAM -> base * 4.4D;
                case ELDER_GUARDIAN_BEAM -> base * 4.2D;
                case SHADOW_HANDS -> base * 4.0D;
                case WITHER_SKULL -> base * 4.0D;
                case EVOKER_FANGS -> base * 3.6D;
                case HEALING_BOTTLE -> base * 2.2D;
                case LIGHTNING_STRIKE -> base * 3.6D;
                case GHAST_FIREBALL -> base * 3.2D;
                case SHULKER_BULLET -> base * 4.0D;
                case WARDEN_SCREAM -> base * 3.8D;
                default -> base;
            };
            case SHOOTER -> switch (reward) {
                case CROSSBOW -> base * 5.5D;
                case TRIDENT -> base * 5.2D;
                case ARROW_SHOT -> base * 5.0D;
                case BLAZE_ATTACK -> base * 3.6D;
                case LLAMA_SPIT -> base * 4.2D;
                case SNOWBALL_SHOT -> base * 5.8D;
                case SHULKER_BULLET -> base * 2.0D;
                case WITHER_SKULL -> base * 2.4D;
                case GHAST_FIREBALL -> base * 2.2D;
                case EVOKER_FANGS -> base * 1.8D;
                case WARDEN_SCREAM -> base * 1.8D;
                default -> base;
            };
            case ATTRIBUTER -> base * 0.6D;
        };
    }

    private static AbilityReward byAbilityId(String id) {
        id = canonicalAbilityId(id);
        for (AbilityReward reward : AbilityReward.values()) {
            if (reward.id.equals(id)) {
                return reward;
            }
        }
        return null;
    }

    private static AttributeReward byAttributeId(String id) {
        for (AttributeReward reward : AttributeReward.values()) {
            if (reward.id.equals(id)) {
                return reward;
            }
        }
        return null;
    }

    private static String canonicalAbilityId(String id) {
        if (id == null) {
            return "";
        }
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        return ABILITY_ALIASES.getOrDefault(normalized, normalized);
    }

    private static boolean isGuaranteedOnlyAttribute(String attributeId) {
        if (attributeId == null) {
            return false;
        }
        return "gluttonous".equals(attributeId) || "tethered_teleport".equals(attributeId);
    }

    private static List<String> grantGuaranteedAttributesForLevel(TameData data) {
        List<String> granted = new ArrayList<>();
        if (data == null) {
            return granted;
        }
        if (data.level >= 10 && getAttributeLevel(data, "tethered_teleport") <= 0 && addAttribute(data, "tethered_teleport", 1)) {
            granted.add("Unlocked tethered_teleport I");
        }
        if (data.level >= 30 && getAttributeLevel(data, "gluttonous") <= 0 && addAttribute(data, "gluttonous", 1)) {
            granted.add("Unlocked gluttonous I");
        }
        return granted;
    }

    private static int resolveAbilityLevel(TameData data, String canonicalId) {
        int level = data.abilityLevels.getOrDefault(canonicalId, 0);
        if (level > 0) {
            return level;
        }
        for (Map.Entry<String, String> entry : ABILITY_ALIASES.entrySet()) {
            if (!entry.getValue().equals(canonicalId)) {
                continue;
            }
            int aliasLevel = data.abilityLevels.getOrDefault(entry.getKey(), 0);
            if (aliasLevel > level) {
                level = aliasLevel;
            }
        }
        return level;
    }

    private static boolean hasLegacyAlias(TameData data, String canonicalId) {
        for (Map.Entry<String, String> entry : ABILITY_ALIASES.entrySet()) {
            if (entry.getValue().equals(canonicalId) && data.abilities.contains(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static void normalizeAliasesForAbility(TameData data, String canonicalId) {
        for (Map.Entry<String, String> entry : ABILITY_ALIASES.entrySet()) {
            if (!entry.getValue().equals(canonicalId)) {
                continue;
            }
            String alias = entry.getKey();
            int aliasLevel = data.abilityLevels.getOrDefault(alias, 0);
            if (aliasLevel > 0) {
                int current = data.abilityLevels.getOrDefault(canonicalId, 0);
                if (aliasLevel > current) {
                    data.abilityLevels.put(canonicalId, aliasLevel);
                }
                data.abilityLevels.remove(alias);
            }
            data.abilities.remove(alias);
        }
    }

    private static <T> T pickWeighted(List<WeightedOption<T>> options) {
        double total = 0.0D;
        for (WeightedOption<T> option : options) {
            total += Math.max(0.0D, option.weight);
        }
        if (total <= 0.0D) {
            return options.get(0).value;
        }

        double roll = RANDOM.nextDouble() * total;
        double cumulative = 0.0D;
        for (WeightedOption<T> option : options) {
            cumulative += Math.max(0.0D, option.weight);
            if (roll <= cumulative) {
                return option.value;
            }
        }
        return options.get(options.size() - 1).value;
    }

    // ===============================
    // UPDATE NAME
    // ===============================

    public static void updateTameName(LivingEntity entity, TameData data) {
        String name = "[Lvl " + data.level + "] " + data.name;
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
    }

    private static void addToAttribute(LivingEntity entity, Attribute attribute, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.setBaseValue(instance.getBaseValue() + amount);
    }

    public static boolean reapplyTypeBasePlusBonuses(TamableAnimal tame, TameData data) {
        if (tame == null || data == null || !(tame.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        Entity spawned = tame.getType().create(serverLevel);
        if (!(spawned instanceof TamableAnimal template)) {
            return false;
        }
        template.setTame(true);
        if (tame.getOwnerUUID() != null) {
            template.setOwnerUUID(tame.getOwnerUUID());
        } else if (data.ownerUUID != null) {
            template.setOwnerUUID(data.ownerUUID);
        }

        setAttributeBaseValue(tame, Attributes.MAX_HEALTH, readBaseOrDefault(template, Attributes.MAX_HEALTH) + data.bonusHealth);
        setAttributeBaseValue(tame, Attributes.ATTACK_DAMAGE, readBaseOrDefault(template, Attributes.ATTACK_DAMAGE) + data.bonusDamage);
        setAttributeBaseValue(tame, Attributes.MOVEMENT_SPEED, readBaseOrDefault(template, Attributes.MOVEMENT_SPEED) + data.bonusSpeed);
        setAttributeBaseValue(tame, Attributes.ARMOR, readBaseOrDefault(template, Attributes.ARMOR) + data.bonusArmor);
        setAttributeBaseValue(tame, Attributes.ARMOR_TOUGHNESS, readBaseOrDefault(template, Attributes.ARMOR_TOUGHNESS) + data.bonusArmorToughness);
        setAttributeBaseValue(tame, Attributes.ATTACK_KNOCKBACK, readBaseOrDefault(template, Attributes.ATTACK_KNOCKBACK) + data.bonusKnockback);
        setAttributeBaseValue(tame, Attributes.KNOCKBACK_RESISTANCE, readBaseOrDefault(template, Attributes.KNOCKBACK_RESISTANCE) + data.bonusKnockbackResist);

        updateTameName(tame, data);
        tame.setHealth(tame.getMaxHealth());
        return true;
    }

    private static double readBaseOrDefault(TamableAnimal tame, Attribute attribute) {
        if (tame == null || attribute == null) return 0.0D;
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return attribute.getDefaultValue();
        return instance.getBaseValue();
    }

    private static void setAttributeBaseValue(TamableAnimal tame, Attribute attribute, double value) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(value);
    }

    private static void trackBonus(TameData data, BaseStatReward reward) {
        switch (reward) {
            case HP -> data.bonusHealth += reward.amount;
            case DAMAGE -> data.bonusDamage += reward.amount;
            case SPEED -> data.bonusSpeed += reward.amount;
            case ARMOR -> data.bonusArmor += reward.amount;
            case ARMOR_TOUGHNESS -> data.bonusArmorToughness += reward.amount;
            case KNOCKBACK -> data.bonusKnockback += reward.amount;
            case KNOCKBACK_RESIST -> data.bonusKnockbackResist += reward.amount;
        }
    }

    public static int estimateInvestedXp(TameData data) {
        int total = 0;
        for (int lvl = 1; lvl < data.level; lvl++) {
            total += xpRequiredForLevel(lvl);
        }
        total += Math.max(0, data.xp);
        return total;
    }

    public static int xpRequiredForLevel(int level) {
        return BASE_XP + (Math.max(1, level) - 1) * XP_PER_LEVEL_STEP;
    }

    public static void storeProgressSnapshot(TameData data) {
        data.hasSavedProgress = true;
        data.savedProgressCost = estimateInvestedXp(data);
        data.savedLevel = data.level;
        data.savedXp = data.xp;
        data.savedXpToNext = data.xpToNext;
        data.savedKills = data.kills;
        data.savedAssists = data.assists;
        data.savedBonusHealth = data.bonusHealth;
        data.savedBonusDamage = data.bonusDamage;
        data.savedBonusSpeed = data.bonusSpeed;
        data.savedBonusArmor = data.bonusArmor;
        data.savedBonusArmorToughness = data.bonusArmorToughness;
        data.savedBonusKnockback = data.bonusKnockback;
        data.savedBonusKnockbackResist = data.bonusKnockbackResist;

        data.savedAbilities.clear();
        data.savedAbilities.addAll(data.abilities);
        data.savedAbilityLevels.clear();
        data.savedAbilityLevels.putAll(data.abilityLevels);
        data.savedAttributeLevels.clear();
        data.savedAttributeLevels.putAll(data.attributeLevels);
    }

    public static void resetProgress(TamableAnimal tame, TameData data) {
        storeProgressSnapshot(data);
        applyBonusDelta(tame, -data.bonusHealth, -data.bonusDamage, -data.bonusSpeed, -data.bonusArmor,
                -data.bonusArmorToughness, -data.bonusKnockback, -data.bonusKnockbackResist);

        data.level = 1;
        data.xp = 0;
        data.xpToNext = xpRequiredForLevel(data.level);
        data.kills = 0;
        data.assists = 0;
        data.abilities.clear();
        data.abilityLevels.clear();
        data.attributeLevels.clear();
        data.cooldowns.clear();
        data.bonusHealth = 0;
        data.bonusDamage = 0;
        data.bonusSpeed = 0;
        data.bonusArmor = 0;
        data.bonusArmorToughness = 0;
        data.bonusKnockback = 0;
        data.bonusKnockbackResist = 0;

        updateTameName(tame, data);
        tame.setHealth(tame.getMaxHealth());
        TameRegistry.markDirty();
    }

    public static boolean restoreProgress(TamableAnimal tame, TameData data) {
        if (!data.hasSavedProgress) {
            return false;
        }
        if (data.xp < data.savedProgressCost) {
            return false;
        }

        data.xp -= data.savedProgressCost;
        data.level = data.savedLevel;
        data.xp += data.savedXp;
        data.xpToNext = data.savedXpToNext;
        data.kills = data.savedKills;
        data.assists = data.savedAssists;
        data.abilities.clear();
        data.abilities.addAll(data.savedAbilities);
        data.abilityLevels.clear();
        data.abilityLevels.putAll(data.savedAbilityLevels);
        data.attributeLevels.clear();
        data.attributeLevels.putAll(data.savedAttributeLevels);

        applyBonusDelta(
                tame,
                data.savedBonusHealth - data.bonusHealth,
                data.savedBonusDamage - data.bonusDamage,
                data.savedBonusSpeed - data.bonusSpeed,
                data.savedBonusArmor - data.bonusArmor,
                data.savedBonusArmorToughness - data.bonusArmorToughness,
                data.savedBonusKnockback - data.bonusKnockback,
                data.savedBonusKnockbackResist - data.bonusKnockbackResist
        );

        data.bonusHealth = data.savedBonusHealth;
        data.bonusDamage = data.savedBonusDamage;
        data.bonusSpeed = data.savedBonusSpeed;
        data.bonusArmor = data.savedBonusArmor;
        data.bonusArmorToughness = data.savedBonusArmorToughness;
        data.bonusKnockback = data.savedBonusKnockback;
        data.bonusKnockbackResist = data.savedBonusKnockbackResist;
        data.cooldowns.clear();
        data.hasSavedProgress = false;

        updateTameName(tame, data);
        tame.setHealth(tame.getMaxHealth());
        TameRegistry.markDirty();
        return true;
    }

    private static void applyBonusDelta(
            TamableAnimal tame,
            double hp,
            double dmg,
            double speed,
            double armor,
            double toughness,
            double kb,
            double kbResist
    ) {
        addToAttribute(tame, Attributes.MAX_HEALTH, hp);
        addToAttribute(tame, Attributes.ATTACK_DAMAGE, dmg);
        addToAttribute(tame, Attributes.MOVEMENT_SPEED, speed);
        addToAttribute(tame, Attributes.ARMOR, armor);
        addToAttribute(tame, Attributes.ARMOR_TOUGHNESS, toughness);
        addToAttribute(tame, Attributes.ATTACK_KNOCKBACK, kb);
        addToAttribute(tame, Attributes.KNOCKBACK_RESISTANCE, kbResist);
    }

    private static String formatDouble(double value) {
        if (Math.floor(value) == value) {
            return Integer.toString((int) value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static String roman(int value) {
        switch (value) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            case 5:
                return "V";
            default:
                return Integer.toString(value);
        }
    }
}

