package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameStoredArmorEvents;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;
import net.minecraftforge.registries.ForgeRegistries;

public class LevelSystem {
    private static final UUID COLLAR_ARMOR_UUID = UUID.fromString("e6e52fdd-8e14-4c0d-9ac1-8fbc60f3dd01");
    private static final UUID COLLAR_ARMOR_TOUGHNESS_UUID = UUID.fromString("f2f6c7ab-8a73-4d1c-95e4-07f171ddca8f");
    private static final UUID LEGENDARY_MONSTERS_HEALTH_BONUS_UUID = UUID.fromString("5d39f5cd-0d9d-4308-96d5-76aef6c72601");
    private static final UUID LEGENDARY_MONSTERS_DAMAGE_BONUS_UUID = UUID.fromString("f8d8b4d9-7d04-4e0c-90d9-76b8f5485cc7");

    public enum AbilityType {
        ATTACK,
        HEAL,
        SUPPORT
    }

    public static final double DEATH_XP_LOSS = 0.10D;
    public static final double RECOVERY_XP_MULTIPLIER = 2.0D;
    public static final double ATTRIBUTE_UPGRADE_EXISTING_CHANCE = 0.50D;
    private static final double DPS_DAMAGE_REWARD_AMOUNT = 1.0D;
    private static final double STRIKER_DAMAGE_REWARD_AMOUNT = 1.0D;
    private static volatile ClassWeightConfig CLASS_WEIGHT_CONFIG = ClassWeightConfig.loadOrThrow();

    private static final Random RANDOM = new Random();

    public record LevelRewardRepairResult(int repairedRows, int baseStats, int attributes, int abilities) {
        public boolean changed() {
            return repairedRows > 0 || baseStats > 0 || attributes > 0 || abilities > 0;
        }
    }

    // mobUUID -> set of tameUUID
    public static final Map<UUID, Set<UUID>> mobDamageTracker = new HashMap<>();
    // mobUUID -> set of playerUUID
    public static final Map<UUID, Set<UUID>> mobOwnerDamageTracker = new HashMap<>();
    // mobUUID -> tameUUID that most recently damaged it
    private static final Map<UUID, UUID> lastTameDamagerByMob = new HashMap<>();

    private enum BaseStatReward {
        HP("hp", "HP", Attributes.MAX_HEALTH, 1.0D),
        DAMAGE("damage", "Damage", Attributes.ATTACK_DAMAGE, 1.0D),
        SPEED("speed", "Speed", Attributes.MOVEMENT_SPEED, 0.01D),
        ARMOR("armor", "Armor", Attributes.ARMOR, 1.0D),
        ARMOR_TOUGHNESS("armor_toughness", "Armor Toughness", Attributes.ARMOR_TOUGHNESS, 1.0D),
        KNOCKBACK("knockback", "Attack Knockback", Attributes.ATTACK_KNOCKBACK, 0.5D),
        KNOCKBACK_RESIST("knockback_resist", "Knockback Resistance", Attributes.KNOCKBACK_RESISTANCE, 0.05D);

        private final String id;
        private final String display;
        private final Attribute attribute;
        private final double amount;

        BaseStatReward(String id, String display, Attribute attribute, double amount) {
            this.id = id;
            this.display = display;
            this.attribute = attribute;
            this.amount = amount;
        }
    }

    private enum AttributeReward {
        SPEED("speed", 5),
        RESISTANCE("resistance", 30),
        STRENGTH("strength", 5),
        FIRE_RESISTANCE("fire_resistance", 1),
        POISON_RESISTANCE("poison_resistance", 1),
        ABILITY_POWER("ability_power", Integer.MAX_VALUE),
        LIFESTEAL("lifesteal", Integer.MAX_VALUE),
        REGENERATION("regeneration", Integer.MAX_VALUE),
        REJUVENATION("rejuvenation", Integer.MAX_VALUE),
        COMFORT("comfort", Integer.MAX_VALUE),
        FIREFANG("firefang", Integer.MAX_VALUE),
        POISON_FANG("poison_fang", Integer.MAX_VALUE),
        WITHERFANG("witherfang", Integer.MAX_VALUE),
        LIGHTNINGFANG("lightningfang", Integer.MAX_VALUE),
        EMERGENCY_COOLDOWN_REDUCTION("emergency_cooldown_reduction", Integer.MAX_VALUE),
        KILLER("killer", Integer.MAX_VALUE),
        PACIFIST("pacifist", Integer.MAX_VALUE),
        BOSSKILLER("bosskiller", 7),
        KILLEXPLODER("killexploder", Integer.MAX_VALUE),
        TOTEM("totem", 5),
        JUMP_BOOST("jump_boost", Integer.MAX_VALUE),
        FEATHER_FALLING("feather_falling", 5),
        EXPLOSION_RESISTANCE("explosion_resistance", 5),
        SMITE("smite", Integer.MAX_VALUE),
        BANE_OF_ARTHROPODS("bane_of_arthropods", Integer.MAX_VALUE),
        POSITIVE_EFFECT_STEAL("positive_effect_steal", 5),
        NEGATIVE_EFFECT_TRANSFER("negative_effect_transfer", Integer.MAX_VALUE),
        SWEEPING_EDGE("sweeping_edge", Integer.MAX_VALUE),
        CHAIN_LIGHTNING("chain_lightning", Integer.MAX_VALUE),
        FROST_FANG("frost_fang", Integer.MAX_VALUE),
        MAGNETIC("magnetic", 8),
        LINKED_INVENTORY("linked_inventory", 1),
        HEALTH_SIPHON("health_siphon", 7),
        VICTIM_SIPHON("victim_siphon", 7),
        PIERCE("pierce", 5),
        BUBBLING("bubbling", Integer.MAX_VALUE),
        QUICKY("quicky", Integer.MAX_VALUE),
        HERDING("herding", Integer.MAX_VALUE),
        AMPHIBIOUS("amphibious", 1),
        WALL_CLIMBER("wall_climber", 5),
        VOID_CLOUD("void_cloud", 1),
        CHARISMA("charisma", 5),
        DISC_JOCKEY("disc_jockey", 1),
        WARPING_BITE("warping_bite", 13),
        ORE_SCENTING("ore_scenting", 1),
        GLUTTONOUS("gluttonous", 1),
        TETHERED_TELEPORT("tethered_teleport", 1),
        MUFFLED("muffled", 1),
        SPAWNER_TRIGGER("spawner_trigger", 1),
        BLAZING_PROTECTION("blazing_protection", Integer.MAX_VALUE);

        private final String id;
        private final int maxLevel;

        AttributeReward(String id, int maxLevel) {
            this.id = id;
            this.maxLevel = maxLevel;
        }
    }

    private enum AbilityReward {
        CREEPER_EXPLOSION("creeper_explosion", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        ARROW_SHOT("arrow_shot", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        GHAST_FIREBALL("ghast_fireball", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        BATTLE_STRENGTH("battle_strength", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        DEFENSIVE_AURA("defensive_aura", AbilityType.SUPPORT, true, 30),
        SNOWBALL_SHOT("snowball_shot", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        ENDER_PEARL_JUMP("ender_pearl_jump", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        LIGHTNING_STRIKE("lightning_strike", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        WARDEN_SCREAM("warden_scream", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        WITHER_SKULL("wither_skull", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        BLAZE_ATTACK("blaze_attack", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        GUARDIAN_BEAM("guardian_beam", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        ELDER_GUARDIAN_BEAM("elder_guardian_beam", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        BERSERKER("berserker", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        BLOODLUST("bloodlust", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        TRIDENT("trident", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        CROSSBOW("crossbow", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        EVOKER_FANGS("evoker_fangs", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        SHULKER_BULLET("shulker_bullet", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        DRAGON_FIREBALL("dragon_fireball", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        LLAMA_SPIT("llama_spit", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        FISHING("fishing", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        DASH("dash", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        FLASH("flash", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        RETALIATION_SLOW("retaliation_slow", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        IMMUNITY_FRAME("immunity_frame", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        DEFLECTION("deflection", AbilityType.SUPPORT, false, 1),
        DEFUSAL("defusal", AbilityType.SUPPORT, true, 5),
        SHADOW_HANDS("shadow_hands", AbilityType.ATTACK, true, Integer.MAX_VALUE),
        PSYCHIC_WALL("psychic_wall", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        HEALING_AURA("healing_aura", AbilityType.HEAL, true, Integer.MAX_VALUE),
        HEALING_BOTTLE("healing_bottle", AbilityType.HEAL, true, 4),
        GUARDIAN_REPULSE("guardian_repulse", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        LAST_STAND_FURY("last_stand_fury", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        SHIELD_BLOCK("shield_block", AbilityType.SUPPORT, true, 15),
        SKY_LAUNCH("sky_launch", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        GUARDIAN_INTERCEPT("guardian_intercept", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        EMERGENCY_SHIELD("emergency_shield", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        BODY_BLOCK("body_block", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        BATTLEFIELD_MEDIC("battlefield_medic", AbilityType.HEAL, true, 8),
        TRIAGE_PULSE("triage_pulse", AbilityType.HEAL, true, Integer.MAX_VALUE),
        REVITALIZING_PRESENCE("revitalizing_presence", AbilityType.HEAL, true, Integer.MAX_VALUE),
        CLEANSE_TOUCH("cleanse_touch", AbilityType.SUPPORT, true, 9),
        PACK_GUARD("pack_guard", AbilityType.SUPPORT, true, Integer.MAX_VALUE),
        LIFE_GIFT("life_gift", AbilityType.HEAL, true, 40);

        private final String id;
        private final AbilityType type;
        private final boolean upgradable;
        private final int maxLevel;

        AbilityReward(String id, AbilityType type, boolean upgradable, int maxLevel) {
            this.id = id;
            this.type = type;
            this.upgradable = upgradable;
            this.maxLevel = maxLevel;
        }
    }

    private enum AbilityRollChoice {
        NEW_UNLOCK,
        UPGRADE_EXISTING
    }

    private record WeightedOption<T>(T value, double weight) {}

    public record ClassCategoryView(double base, double attribute, double ability) {
    }

    private record LevelRewardResult(RewardCategory category, String rewardId, double amount, String summary) {
        private static LevelRewardResult fromHistoryRow(CompoundTag row) {
            if (row == null
                    || !row.contains("rewardCategory", Tag.TAG_STRING)
                    || !row.contains("rewardId", Tag.TAG_STRING)) {
                return null;
            }
            try {
                RewardCategory category = RewardCategory.valueOf(row.getString("rewardCategory"));
                String rewardId = row.getString("rewardId");
                double amount = row.contains("rewardAmount", Tag.TAG_DOUBLE) ? row.getDouble("rewardAmount") : 1.0D;
                String summary = row.contains("reward", Tag.TAG_STRING) ? row.getString("reward") : "";
                return new LevelRewardResult(category, rewardId, amount, summary);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
    }

    private static final Map<String, AbilityReward> ABILITY_REWARDS_BY_ID;
    private static final Map<String, AttributeReward> ATTRIBUTE_REWARDS_BY_ID;
    private static final Set<String> KNOWN_ABILITIES;
    private static final Set<String> KNOWN_ATTRIBUTES;
    private static final Map<String, String> ABILITY_ALIASES;
    static {
        Map<String, AbilityReward> abilityRewards = new HashMap<>();
        Map<String, AttributeReward> attributeRewards = new HashMap<>();
        Set<String> ids = new LinkedHashSet<>();
        for (AbilityReward value : AbilityReward.values()) {
            ids.add(value.id);
            abilityRewards.put(value.id, value);
        }
        Set<String> attributes = new LinkedHashSet<>();
        for (AttributeReward value : AttributeReward.values()) {
            attributes.add(value.id);
            attributeRewards.put(value.id, value);
        }
        Map<String, String> aliases = new HashMap<>();

        KNOWN_ABILITIES = Collections.unmodifiableSet(new LinkedHashSet<>() {{
            addAll(ids);
            addAll(aliases.keySet());
        }});
        KNOWN_ATTRIBUTES = Collections.unmodifiableSet(attributes);
        ABILITY_ALIASES = Collections.unmodifiableMap(aliases);
        ABILITY_REWARDS_BY_ID = Collections.unmodifiableMap(abilityRewards);
        ATTRIBUTE_REWARDS_BY_ID = Collections.unmodifiableMap(attributeRewards);
    }

    // ===============================
    // DAMAGE TRACKING
    // ===============================

    public static void trackDamage(LivingEntity mob, LivingEntity tame) {
        if (mob == null || tame == null) {
            return;
        }
        mobDamageTracker.computeIfAbsent(mob.getUUID(), k -> new HashSet<>()).add(tame.getUUID());
        lastTameDamagerByMob.put(mob.getUUID(), tame.getUUID());
    }

    public static UUID lastTameDamager(UUID mobId) {
        return mobId == null ? null : lastTameDamagerByMob.get(mobId);
    }

    public static void trackOwnerDamage(LivingEntity mob, Player player) {
        if (mob == null || player == null) {
            return;
        }
        mobOwnerDamageTracker.computeIfAbsent(mob.getUUID(), k -> new HashSet<>()).add(player.getUUID());
    }

    /**
     * Releases combat-contribution bookkeeping when a target leaves the world
     * without passing through the normal death/XP distribution path.  Mobs can
     * despawn or unload after being hit, so relying on distributeXP alone leaves
     * their UUIDs retained for the lifetime of the server.
     */
    public static void clearDamageTracking(UUID mobId) {
        if (mobId == null) {
            return;
        }
        mobDamageTracker.remove(mobId);
        mobOwnerDamageTracker.remove(mobId);
        lastTameDamagerByMob.remove(mobId);
    }

    // ===============================
    // XP DISTRIBUTION
    // ===============================

    public static void distributeXP(LivingEntity dead, LivingEntity killer) {
        UUID mobId = dead.getUUID();
        Set<UUID> tameIds = mobDamageTracker.get(mobId);
        if (tameIds == null || tameIds.isEmpty()) {
            clearDamageTracking(mobId);
            return;
        }

        int xpAmount = dead.getExperienceReward();
        UUID killerTameId = null;
        if (TameEntityAdapter.isTame(killer)) {
            killerTameId = killer.getUUID();
        }

        Set<UUID> participants = new LinkedHashSet<>(tameIds);
        if (killerTameId != null) {
            participants.add(killerTameId);
        }

        List<UUID> assisters = new ArrayList<>();
        for (UUID tameId : participants) {
            if (tameId == null || tameId.equals(killerTameId)) {
                continue;
            }
            assisters.add(tameId);
        }

        Set<UUID> ownerAssisters = new LinkedHashSet<>();
        Set<UUID> damagedByOwners = mobOwnerDamageTracker.getOrDefault(mobId, Set.of());
        if (!damagedByOwners.isEmpty()) {
            for (UUID ownerId : damagedByOwners) {
                if (ownerId == null) {
                    continue;
                }
                boolean hasContributingTame = false;
                for (UUID tameId : participants) {
                    TameData data = TameRegistry.get(tameId);
                    if (data != null && ownerId.equals(data.ownerUUID)) {
                        hasContributingTame = true;
                        break;
                    }
                }
                if (hasContributingTame) {
                    ownerAssisters.add(ownerId);
                }
            }
        }

        int assisterSlots = assisters.size() + ownerAssisters.size();
        double killerBaseXp = 0.0D;
        double assisterPoolXp = 0.0D;
        if (killerTameId != null) {
            if (assisterSlots <= 0) {
                killerBaseXp = xpAmount;
            } else if (assisterSlots == 1) {
                killerBaseXp = xpAmount * 0.75D;
                assisterPoolXp = xpAmount * 0.25D;
            } else {
                killerBaseXp = xpAmount * 0.50D;
                assisterPoolXp = xpAmount * 0.50D;
            }
        } else if (assisterSlots > 0) {
            assisterPoolXp = xpAmount * 0.50D;
        }
        double assisterShareXp = assisterSlots <= 0 ? 0.0D : assisterPoolXp / assisterSlots;

        for (UUID tameId : participants) {
            TameData data = TameRegistry.get(tameId);
            if (data == null) {
                continue;
            }

            double gainedXP;
            if (killerTameId != null && killerTameId.equals(tameId)) {
                data.kills++;
                gainedXP = killerBaseXp;
            } else {
                data.assists++;
                gainedXP = assisterShareXp;
            }

            Entity entity = dead.level() instanceof ServerLevel serverLevel ? serverLevel.getEntity(tameId) : null;
            if (gainedXP > 0.0D) {
                int adjustedXp = scaleRecoveryXpGain(data, gainedXP);
                data.xp += TameStoredArmorEvents.repairWithMending(
                        entity instanceof LivingEntity living ? living : null, data, adjustedXp);
            }
            if (entity instanceof LivingEntity tame) {
                checkLevelUp(tame, data);
            }
        }

        TameRegistry.markDirty();
        clearDamageTracking(mobId);
    }

    // ===============================
    // XP LOSS ON DEATH
    // ===============================

    public static void onTameDeath(LivingEntity tame, boolean applyPenaltyAndCountDeath) {
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            UUID tlId = TameData.getTlId(tame);
            if (tlId != null) {
                data = TameRegistry.getByTlId(tlId);
            }
        }
        if (data == null) {
            return;
        }

        int totalXp = estimateInvestedXp(data);
        int xpLoss = (int) Math.floor(totalXp * DEATH_XP_LOSS);
        if (applyPenaltyAndCountDeath && xpLoss > 0) {
            int previousLevel = data.level;
            int remainingXp = Math.max(0, totalXp - xpLoss);
            int resultingLevel = levelForInvestedXp(remainingXp);
            rollbackLostLevelRewards(tame, data, previousLevel, resultingLevel);
            applyInvestedXp(data, remainingXp);
        }
        data.activeSurvivalDays = 0;
        data.adminSurvivalStart = Long.MIN_VALUE;
        data.lastActiveSurvivalDay = Long.MIN_VALUE;
        updateTameName(tame, data);
        if (applyPenaltyAndCountDeath) {
            data.deaths++;
        }
        TameRegistry.markDirty();
    }

    public static Set<String> knownAbilityIds() {
        return KNOWN_ABILITIES;
    }

    public static AbilityType getAbilityType(String abilityId) {
        AbilityReward reward = byAbilityId(abilityId);
        return reward == null ? null : reward.type;
    }

    public static boolean isAttackAbility(String abilityId) {
        return getAbilityType(abilityId) == AbilityType.ATTACK;
    }

    public static Set<String> knownAttributeIds() {
        return KNOWN_ATTRIBUTES;
    }

    public static int getAttributeLevel(TameData data, String attributeId) {
        if (data == null || attributeId == null) {
            return 0;
        }
        String id = ATTRIBUTE_REWARDS_BY_ID.containsKey(attributeId)
                ? attributeId : attributeId.trim().toLowerCase(java.util.Locale.ROOT);
        return clampAttributeLevel(id, data.attributeLevels.getOrDefault(id, 0));
    }

    public static double quickyCooldownMultiplier(TameData data) {
        int level = Math.max(0, getAttributeLevel(data, "quicky"));
        if (level <= 0) {
            return 1.0D;
        }
        return Math.pow(0.96D, level);
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
        if (data == null) {
            return 0;
        }
        String canonicalId = canonicalAbilityId(abilityId);
        return clampAbilityLevel(canonicalId, resolveAbilityLevel(data, canonicalId));
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

    public static void grantXP(LivingEntity tame, TameData data, int amount) {
        if (amount == 0) {
            return;
        }
        int adjusted = amount > 0 ? scaleRecoveryXpGain(data, amount) : amount;
        if (adjusted > 0) adjusted = TameStoredArmorEvents.repairWithMending(tame, data, adjusted);
        data.xp = Math.max(0, data.xp + adjusted);
        checkLevelUp(tame, data);
        TameRegistry.markDirty();
    }

    public static LevelRewardRepairResult repairMissingRecordedLevelRewards(TamableAnimal tame, TameData data) {
        if (data == null || data.levelRewardHistory == null || data.levelRewardHistory.isEmpty()) {
            return new LevelRewardRepairResult(0, 0, 0, 0);
        }

        List<CompoundTag> recordedRows = new ArrayList<>();
        for (CompoundTag row : data.levelRewardHistory) {
            if (LevelRewardResult.fromHistoryRow(row) == null) {
                continue;
            }
            boolean active = !row.contains("active") || row.getBoolean("active");
            if (!active) {
                continue;
            }
            recordedRows.add(row);
        }
        if (recordedRows.isEmpty()) {
            return new LevelRewardRepairResult(0, 0, 0, 0);
        }

        int expectedRewardCount = Math.min(recordedRows.size(), Math.max(0, data.level - 1));
        if (expectedRewardCount <= 0) {
            return new LevelRewardRepairResult(0, 0, 0, 0);
        }

        Map<String, Double> availableBase = new HashMap<>();
        availableBase.put(BaseStatReward.HP.id, data.bonusHealth);
        availableBase.put(BaseStatReward.DAMAGE.id, data.bonusDamage);
        availableBase.put(BaseStatReward.SPEED.id, data.bonusSpeed);
        availableBase.put(BaseStatReward.ARMOR.id, data.bonusArmor);
        availableBase.put(BaseStatReward.ARMOR_TOUGHNESS.id, data.bonusArmorToughness);
        availableBase.put(BaseStatReward.KNOCKBACK.id, data.bonusKnockback);
        availableBase.put(BaseStatReward.KNOCKBACK_RESIST.id, data.bonusKnockbackResist);

        Map<String, Integer> availableAttributes = new HashMap<>(data.attributeLevels);
        Map<String, Integer> availableAbilities = new HashMap<>(data.abilityLevels);
        int appliedRewardCount = countAppliedRewardRows(
                recordedRows.subList(0, expectedRewardCount),
                availableBase,
                availableAttributes,
                availableAbilities
        );

        int repairedRows = Math.max(0, expectedRewardCount - appliedRewardCount);
        int baseRepairs = 0;
        int attributeRepairs = 0;
        int abilityRepairs = 0;
        for (int i = appliedRewardCount; i < expectedRewardCount; i++) {
            LevelRewardResult reward = LevelRewardResult.fromHistoryRow(recordedRows.get(i));
            if (reward == null) {
                continue;
            }
            applyStoredLevelReward(tame, data, reward);
            switch (reward.category()) {
                case BASE_STAT -> baseRepairs++;
                case ATTRIBUTE -> attributeRepairs++;
                case ABILITY -> abilityRepairs++;
            }
        }

        String milestoneSummary = grantMissingMilestoneAttributes(data);
        if (!milestoneSummary.isEmpty()) {
            attributeRepairs++;
        }

        if ((baseRepairs > 0 || attributeRepairs > 0 || abilityRepairs > 0) && tame != null) {
            if (!reapplyTypeBasePlusBonuses(tame, data)) {
                updateTameName(tame, data);
                tame.setHealth(tame.getMaxHealth());
            }
        }
        if (baseRepairs > 0 || attributeRepairs > 0 || abilityRepairs > 0) {
            TameRegistry.markDirty();
        }
        return new LevelRewardRepairResult(repairedRows, baseRepairs, attributeRepairs, abilityRepairs);
    }

    private static int countAppliedRewardRows(
            List<CompoundTag> rows,
            Map<String, Double> availableBase,
            Map<String, Integer> availableAttributes,
            Map<String, Integer> availableAbilities
    ) {
        Map<String, List<Double>> baseById = new HashMap<>();
        Map<String, List<Integer>> attributeById = new HashMap<>();
        Map<String, List<Integer>> abilityById = new HashMap<>();

        for (CompoundTag row : rows) {
            LevelRewardResult reward = LevelRewardResult.fromHistoryRow(row);
            if (reward == null) {
                continue;
            }
            switch (reward.category()) {
                case BASE_STAT -> baseById.computeIfAbsent(reward.rewardId(), key -> new ArrayList<>()).add(reward.amount());
                case ATTRIBUTE -> attributeById.computeIfAbsent(reward.rewardId(), key -> new ArrayList<>()).add(Math.max(1, (int) Math.round(reward.amount())));
                case ABILITY -> abilityById.computeIfAbsent(reward.rewardId(), key -> new ArrayList<>()).add(Math.max(1, (int) Math.round(reward.amount())));
            }
        }

        int matched = 0;
        for (Map.Entry<String, List<Double>> entry : baseById.entrySet()) {
            List<Double> amounts = entry.getValue();
            amounts.sort(Double::compare);
            double available = availableBase.getOrDefault(entry.getKey(), 0.0D);
            for (double amount : amounts) {
                if (available + 1.0E-6D < amount) {
                    break;
                }
                available -= amount;
                matched++;
            }
        }
        for (Map.Entry<String, List<Integer>> entry : attributeById.entrySet()) {
            List<Integer> amounts = entry.getValue();
            amounts.sort(Integer::compareTo);
            int available = availableAttributes.getOrDefault(entry.getKey(), 0);
            for (int amount : amounts) {
                if (available < amount) {
                    break;
                }
                available -= amount;
                matched++;
            }
        }
        for (Map.Entry<String, List<Integer>> entry : abilityById.entrySet()) {
            List<Integer> amounts = entry.getValue();
            amounts.sort(Integer::compareTo);
            int available = availableAbilities.getOrDefault(entry.getKey(), 0);
            for (int amount : amounts) {
                if (available < amount) {
                    break;
                }
                available -= amount;
                matched++;
            }
        }
        return matched;
    }

    // ===============================
    // CLASS ROLLING
    // ===============================

    public static TameClass rollClass() {
        return TameClassRoller.roll(CLASS_WEIGHT_CONFIG);
    }

    public static void ensureClassAssigned(LivingEntity tame, TameData data, boolean notifyOwner) {
        if (tame == null || data == null || TameRegistry.isSummonType(tame)) {
            return;
        }
        if (data.tameClass != null) {
            return;
        }

        data.tameClass = rollClass();
        TameRegistry.markDirty();
        if (!notifyOwner) {
            return;
        }
        notifyAssignedClass(tame, data);
    }

    public static void notifyAssignedClass(LivingEntity tame, TameData data) {
        if (tame == null || data == null || data.tameClass == null) {
            return;
        }
        if (tame.getPersistentData().getBoolean(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.ADMIN_CLONE_SILENT_TAG)) {
            return;
        }
        if (TameableUtils.getOwnerOf(tame) instanceof Player owner) {
            owner.sendSystemMessage(Component.literal(data.name).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal(" class assigned: ").withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(data.tameClass.id()).withStyle(ChatFormatting.YELLOW)));
        }
    }

    // ===============================
    // LEVEL UP CHECK
    // ===============================

    public static void checkLevelUp(LivingEntity tame, TameData data) {
        ensureClassAssigned(tame, data, false);
        data.xpToNext = xpRequiredForLevel(data.level);
        boolean leveled = false;

        while (data.xp >= data.xpToNext) {
            leveled = true;
            boolean regainingLevels = isRegainingLevels(data);
            data.xp -= data.xpToNext;
            data.level++;
            data.xpToNext = xpRequiredForLevel(data.level);

            LevelRewardResult reward = restoreStoredLevelReward(tame, data, data.level);
            if (reward == null) {
                reward = applyLevelReward(tame, data);
                recordLevelReward(data, data.level, reward, tame.level().getGameTime());
            }
            String rewardSummary = reward.summary();
            String milestoneSummary = grantMissingMilestoneAttributes(data);
            if (!milestoneSummary.isEmpty()) {
                rewardSummary = rewardSummary.isEmpty() ? milestoneSummary : rewardSummary + " + " + milestoneSummary;
            }
            updateTameName(tame, data);

            if (!regainingLevels && TameableUtils.getOwnerOf(tame) instanceof Player owner && PlayerDebugSettings.levelUp(owner.getUUID())) {
                owner.sendSystemMessage(Component.literal("Your pet ").withStyle(ChatFormatting.GOLD)
                        .append(Component.literal(data.name).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(" leveled up to ").withStyle(ChatFormatting.GOLD))
                        .append(Component.literal("Level " + data.level).withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal(".").withStyle(ChatFormatting.GOLD)));
                if (!rewardSummary.isEmpty()) {
                    owner.sendSystemMessage(Component.literal("Reward: ").withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(rewardSummary).withStyle(ChatFormatting.WHITE)));
                }
            }
        }
        if (leveled) {
            TameRegistry.markDirty();
        }
    }

    public static String grantMissingMilestoneAttributes(TameData data) {
        if (data == null) {
            return "";
        }
        List<String> granted = new ArrayList<>();
        if (data.level >= 10 && data.attributeLevels.getOrDefault("tethered_teleport", 0) <= 0) {
            data.attributeLevels.put("tethered_teleport", 1);
            granted.add("tethered_teleport I");
        }
        if (data.level >= 30 && data.attributeLevels.getOrDefault("gluttonous", 0) <= 0) {
            data.attributeLevels.put("gluttonous", 1);
            granted.add("gluttonous I");
        }
        if (data.level >= 50 && data.tameClass == TameClass.DISCO
                && data.attributeLevels.getOrDefault("spawner_trigger", 0) <= 0) {
            data.attributeLevels.put("spawner_trigger", 1);
            granted.add("spawner_trigger I");
        }
        if (granted.isEmpty()) {
            return "";
        }
        TameRegistry.markDirty();
        return String.join(", ", granted);
    }

    private static boolean isRegainingLevels(TameData data) {
        return data != null && data.hasSavedProgress && data.level < Math.max(1, data.savedLevel);
    }

    private static int scaleRecoveryXpGain(TameData data, double baseAmount) {
        if (baseAmount <= 0.0D) {
            return 0;
        }
        double scaled = isRegainingLevels(data) ? baseAmount * RECOVERY_XP_MULTIPLIER : baseAmount;
        return Math.max(1, (int) Math.round(scaled));
    }

    private static void recordLevelReward(TameData data, int level, LevelRewardResult reward, long gameTime) {
        if (data == null) {
            return;
        }
        CompoundTag row = new CompoundTag();
        row.putInt("level", Math.max(1, level));
        row.putString("reward", reward == null ? "" : reward.summary());
        row.putLong("gameTime", Math.max(0L, gameTime));
        if (reward != null) {
            row.putString("rewardCategory", reward.category().name());
            row.putString("rewardId", reward.rewardId());
            row.putDouble("rewardAmount", reward.amount());
            if (reward.category() == RewardCategory.BASE_STAT && "DAMAGE".equals(reward.rewardId())) row.putBoolean("damageRewardV2", true);
        }
        row.putBoolean("active", true);
        data.levelRewardHistory.add(row);
        while (data.levelRewardHistory.size() > 256) {
            data.levelRewardHistory.remove(0);
        }
    }

    // ===============================
    // REWARD ROLLING
    // ===============================

    public static LevelRewardResult applyLevelReward(LivingEntity tame, TameData data) {
        RewardCategory category = rollCategory(data);
        return switch (category) {
            case BASE_STAT -> applyBaseStatReward(tame, data);
            case ATTRIBUTE -> applyAttributeReward(tame, data, true);
            case ABILITY -> applyAbilityReward(tame, data, true);
        };
    }

    private static RewardCategory rollCategory(TameData data) {
        ClassWeightConfig.CategoryWeights weights = CLASS_WEIGHT_CONFIG.categoryWeights(data.tameClass);
        double baseChance = weights.base();
        double attributeChance = weights.attribute();
        double abilityChance = weights.ability();

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

    private static LevelRewardResult applyBaseStatReward(LivingEntity tame, TameData data) {
        List<WeightedOption<BaseStatReward>> options = new ArrayList<>();
        for (BaseStatReward reward : BaseStatReward.values()) {
            options.add(new WeightedOption<>(reward, modifiedBaseStatWeight(data.tameClass, reward)));
        }

        BaseStatReward reward = pickWeighted(options);
        return applyBaseStatReward(tame, data, reward, effectiveBaseStatAmount(data, reward));
    }

    private static LevelRewardResult applyAttributeReward(LivingEntity tame, TameData data, boolean allowAbilityFallback) {
        AttributeReward upgraded = tryUpgradeExistingAttribute(data);
        if (upgraded != null) {
            int newLevel = data.attributeLevels.get(upgraded.id);
            return new LevelRewardResult(RewardCategory.ATTRIBUTE, upgraded.id, 1.0D, upgraded.id + " upgraded to " + roman(newLevel));
        }

        List<WeightedOption<AttributeReward>> options = new ArrayList<>();
        for (AttributeReward reward : AttributeReward.values()) {
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
        return new LevelRewardResult(RewardCategory.ATTRIBUTE, rolled.id, 1.0D, rolled.id + " " + roman(current + 1));
    }

    private static LevelRewardResult applyAbilityReward(LivingEntity tame, TameData data, boolean allowAttributeFallback) {
        AbilityReward unlocked = null;
        AbilityReward upgraded = null;

        AbilityRollChoice choice = rollAbilityChoice(data);
        if (choice == AbilityRollChoice.NEW_UNLOCK) {
            unlocked = tryUnlockNewAbility(data);
        } else if (choice == AbilityRollChoice.UPGRADE_EXISTING) {
            upgraded = tryUpgradeExistingAbility(data);
        }

        if (unlocked != null) {
            unlockAbility(data, unlocked);
            return new LevelRewardResult(RewardCategory.ABILITY, unlocked.id, 1.0D, "Unlocked " + unlocked.id + " I");
        }
        if (upgraded != null) {
            int newLevel = data.abilityLevels.get(upgraded.id);
            return new LevelRewardResult(RewardCategory.ABILITY, upgraded.id, 1.0D, upgraded.id + " upgraded to " + roman(newLevel));
        }

        List<WeightedOption<AbilityReward>> options = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            if (resolveAbilityLevel(data, reward.id) > 0) {
                continue;
            }
            double weight = modifiedAbilityWeight(data.tameClass, reward);
            options.add(new WeightedOption<>(reward, weight));
        }
        if (options.isEmpty()) {
            AbilityReward fallbackUpgrade = tryUpgradeExistingAbility(data);
            if (fallbackUpgrade != null) {
                int newLevel = data.abilityLevels.get(fallbackUpgrade.id);
                return new LevelRewardResult(RewardCategory.ABILITY, fallbackUpgrade.id, 1.0D, fallbackUpgrade.id + " upgraded to " + roman(newLevel));
            }
            if (allowAttributeFallback) {
                return applyAttributeReward(tame, data, false);
            }
            return applyBaseStatReward(tame, data);
        }

        AbilityReward rolled = pickWeighted(options);
        unlockAbility(data, rolled);
        return new LevelRewardResult(RewardCategory.ABILITY, rolled.id, 1.0D, "Unlocked " + rolled.id + " I");
    }

    private static LevelRewardResult applyBaseStatReward(LivingEntity tame, TameData data, BaseStatReward reward, double amount) {
        if (data != null && reward == BaseStatReward.HP && usesFixedHealthClass(data.tameClass)) {
            convertFixedHealthBonus(data, amount, false);
            boolean reapplied = reapplyTypeBasePlusBonuses(tame, data);
            if (reapplied && tame != null) {
                tame.setHealth(Math.min(tame.getHealth(), tame.getMaxHealth()));
            }
            TameRegistry.markDirty();
            return new LevelRewardResult(
                    RewardCategory.BASE_STAT,
                    reward.name(),
                    amount,
                    "Armor +" + formatDouble(fixedHealthArmorAmount(amount))
                            + ", Knockback Resistance +" + formatDouble(fixedHealthKnockbackResistAmount(amount))
            );
        }
        double previousTrackedBonus = trackedBonus(data, reward);
        applyTrackedBaseStatReward(tame, data, reward, amount, previousTrackedBonus);
        if (reward == BaseStatReward.HP) {
            if (amount >= 0.0D) {
                tame.setHealth(tame.getMaxHealth());
            } else {
                tame.setHealth(Math.min(tame.getHealth(), tame.getMaxHealth()));
            }
        }
        TameRegistry.markDirty();
        return new LevelRewardResult(RewardCategory.BASE_STAT, reward.name(), amount, reward.display + " +" + formatDouble(amount));
    }

    private static AbilityReward tryUpgradeExistingAbility(TameData data) {
        AbilityReward existing = pickRandomOwnedUpgradeableAbility(data);
        if (existing == null) {
            return null;
        }
        int level = data.abilityLevels.getOrDefault(existing.id, 0);
        data.abilityLevels.put(existing.id, level + 1);
        TameRegistry.markDirty();
        return existing;
    }

    private static AbilityReward pickRandomOwnedUpgradeableAbility(TameData data) {
        List<WeightedOption<AbilityReward>> weighted = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            int current = data.abilityLevels.getOrDefault(reward.id, 0);
            if (current > 0 && reward.upgradable && current < reward.maxLevel) {
                weighted.add(new WeightedOption<>(reward, ownedAbilityRollMultiplier(current)));
            }
        }
        if (weighted.isEmpty()) {
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
            return 9.0D;
        }
        return 4.0D;
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
        double base = CLASS_WEIGHT_CONFIG.defaultBaseStatWeight(reward.id, switch (reward) {
            case HP -> 70.0D;
            case DAMAGE -> 3.0D;
            case SPEED -> 3.0D;
            case ARMOR -> 3.0D;
            case ARMOR_TOUGHNESS -> 3.0D;
            case KNOCKBACK -> 3.0D;
            case KNOCKBACK_RESIST -> 3.0D;
        });
        if (usesFixedHealthClass(tameClass)) {
            double hpWeight = CLASS_WEIGHT_CONFIG.defaultBaseStatWeight(BaseStatReward.HP.id, 70.0D)
                    * CLASS_WEIGHT_CONFIG.baseStatMultiplier(tameClass, BaseStatReward.HP.id);
            if (reward == BaseStatReward.HP) {
                return 0.0D;
            }
            if (reward == BaseStatReward.ARMOR || reward == BaseStatReward.KNOCKBACK_RESIST) {
                base += hpWeight * 0.5D;
            }
        }
        return base * CLASS_WEIGHT_CONFIG.baseStatMultiplier(tameClass, reward.id);
    }

    private static double effectiveBaseStatAmount(TameData data, BaseStatReward reward) {
        if (data == null || reward == null) {
            return reward == null ? 0.0D : reward.amount;
        }
        if (data.tameClass == TameClass.DPS && reward == BaseStatReward.DAMAGE) {
            return DPS_DAMAGE_REWARD_AMOUNT;
        }
        if (data.tameClass == TameClass.STRIKER && reward == BaseStatReward.DAMAGE) {
            return STRIKER_DAMAGE_REWARD_AMOUNT;
        }
        return reward.amount;
    }

    private static double modifiedAttributeWeight(TameClass tameClass, AttributeReward reward) {
        double weight = CLASS_WEIGHT_CONFIG.attributeWeight(tameClass, reward.id);
        return amplifyPreferredAttributeWeight(tameClass, weight);
    }

    private static double modifiedAbilityWeight(TameClass tameClass, AbilityReward reward) {
        double weight = CLASS_WEIGHT_CONFIG.abilityWeight(tameClass, reward.id);
        return amplifyPreferredAbilityWeight(tameClass, weight);
    }

    private static double amplifyPreferredAttributeWeight(TameClass tameClass, double weight) {
        if (weight <= 1.0D) {
            return weight;
        }
        if (CLASS_WEIGHT_CONFIG.autoPreferredWeightBalance(tameClass)) {
            return weight * automaticPreferredAttributeMultiplier(tameClass);
        }
        return weight * CLASS_WEIGHT_CONFIG.preferredAttributeWeightMultiplier(tameClass);
    }

    private static double amplifyPreferredAbilityWeight(TameClass tameClass, double weight) {
        if (weight <= 1.0D) {
            return weight;
        }
        if (CLASS_WEIGHT_CONFIG.autoPreferredWeightBalance(tameClass)) {
            return weight * automaticPreferredAbilityMultiplier(tameClass);
        }
        return weight * CLASS_WEIGHT_CONFIG.preferredAbilityWeightMultiplier(tameClass);
    }

    private static double automaticPreferredAttributeMultiplier(TameClass tameClass) {
        return automaticPreferredMultiplier(
                tameClass,
                CLASS_WEIGHT_CONFIG.categoryWeights(tameClass).attribute(),
                attributePreferredWeightSums(tameClass)
        );
    }

    private static double automaticPreferredAbilityMultiplier(TameClass tameClass) {
        return automaticPreferredMultiplier(
                tameClass,
                CLASS_WEIGHT_CONFIG.categoryWeights(tameClass).ability(),
                abilityPreferredWeightSums(tameClass)
        );
    }

    private static double automaticPreferredMultiplier(TameClass tameClass, double categoryWeight, double[] preferredAndNonPreferred) {
        if (tameClass == null) {
            return 1.0D;
        }
        double preferred = preferredAndNonPreferred[0];
        double nonPreferred = preferredAndNonPreferred[1];
        if (preferred <= 0.0D || nonPreferred <= 0.0D || categoryWeight <= 0.0D) {
            return 1.0D;
        }
        double totalCategory = CLASS_WEIGHT_CONFIG.categoryWeights(tameClass).base()
                + CLASS_WEIGHT_CONFIG.categoryWeights(tameClass).attribute()
                + CLASS_WEIGHT_CONFIG.categoryWeights(tameClass).ability();
        if (totalCategory <= 0.0D) {
            return 1.0D;
        }
        double expectedRollsByLevel100 = 99.0D * (categoryWeight / totalCategory);
        if (expectedRollsByLevel100 <= 0.0D) {
            return 1.0D;
        }
        double targetNonPreferredPerRoll = 1.0D - Math.pow(0.90D, 1.0D / expectedRollsByLevel100);
        if (targetNonPreferredPerRoll <= 0.0D || targetNonPreferredPerRoll >= 1.0D) {
            return 1.0D;
        }
        double multiplier = (nonPreferred * (1.0D - targetNonPreferredPerRoll)) / (targetNonPreferredPerRoll * preferred);
        if (Double.isNaN(multiplier) || Double.isInfinite(multiplier) || multiplier <= 0.0D) {
            return 1.0D;
        }
        return multiplier;
    }

    private static double[] attributePreferredWeightSums(TameClass tameClass) {
        double preferred = 0.0D;
        double nonPreferred = 0.0D;
        for (AttributeReward reward : AttributeReward.values()) {
            double weight = CLASS_WEIGHT_CONFIG.attributeWeight(tameClass, reward.id);
            if (weight > 1.0D) {
                preferred += weight;
            } else {
                nonPreferred += Math.max(0.0D, weight);
            }
        }
        return new double[]{preferred, nonPreferred};
    }

    private static double[] abilityPreferredWeightSums(TameClass tameClass) {
        double preferred = 0.0D;
        double nonPreferred = 0.0D;
        for (AbilityReward reward : AbilityReward.values()) {
            double weight = CLASS_WEIGHT_CONFIG.abilityWeight(tameClass, reward.id);
            if (weight > 1.0D) {
                preferred += weight;
            } else {
                nonPreferred += Math.max(0.0D, weight);
            }
        }
        return new double[]{preferred, nonPreferred};
    }

    public static void reloadClassWeightConfig() {
        CLASS_WEIGHT_CONFIG = ClassWeightConfig.loadOrThrow();
    }

    public static String classWeightConfigResourcePath() {
        return ClassWeightConfig.resourcePath();
    }

    public static ClassCategoryView classCategoryWeights(TameClass tameClass) {
        ClassWeightConfig.CategoryWeights weights = CLASS_WEIGHT_CONFIG.categoryWeights(tameClass);
        return new ClassCategoryView(weights.base(), weights.attribute(), weights.ability());
    }

    public static Map<String, Double> classBaseStatWeights(TameClass tameClass) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>();
        for (BaseStatReward reward : BaseStatReward.values()) {
            double multiplier = CLASS_WEIGHT_CONFIG.baseStatMultiplier(tameClass, reward.id);
            if (multiplier > 1.0D) {
                entries.add(Map.entry(reward.id, multiplier));
            }
        }
        return toOrderedWeightMap(entries);
    }

    public static Map<String, Double> classAttributeWeights(TameClass tameClass) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>();
        for (AttributeReward reward : AttributeReward.values()) {
            double multiplier = CLASS_WEIGHT_CONFIG.attributeWeight(tameClass, reward.id);
            if (multiplier > 1.0D) {
                entries.add(Map.entry(reward.id, multiplier));
            }
        }
        return toOrderedWeightMap(entries);
    }

    public static Map<String, Double> classAbilityWeights(TameClass tameClass) {
        List<Map.Entry<String, Double>> entries = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            double multiplier = CLASS_WEIGHT_CONFIG.abilityWeight(tameClass, reward.id);
            if (multiplier > 1.0D) {
                entries.add(Map.entry(reward.id, multiplier));
            }
        }
        return toOrderedWeightMap(entries);
    }

    public static double preferredAttributeWeightMultiplier() {
        return CLASS_WEIGHT_CONFIG.preferredAttributeWeightMultiplier();
    }

    public static double preferredAbilityWeightMultiplier() {
        return CLASS_WEIGHT_CONFIG.preferredAbilityWeightMultiplier();
    }

    private static Map<String, Double> toOrderedWeightMap(List<Map.Entry<String, Double>> entries) {
        entries.sort(Comparator
                .comparingDouble((Map.Entry<String, Double> entry) -> -entry.getValue())
                .thenComparing(Map.Entry::getKey));
        Map<String, Double> ordered = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries) {
            ordered.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(ordered);
    }

    private static double protectorAbilityWeight(AbilityReward reward) {
        if (reward == null) {
            return 1.0D;
        }
        if (isHealingSupportAbility(reward)) {
            return 3.5D;
        }
        if (isProtectiveSupportAbility(reward)) {
            return 1.8D;
        }
        return 1.0D;
    }

    private static double tankerAbilityWeight(AbilityReward reward) {
        if (reward == null) {
            return 1.0D;
        }
        if (isProtectiveSupportAbility(reward)) {
            return 3.5D;
        }
        if (isHealingSupportAbility(reward)) {
            return 1.8D;
        }
        return 1.0D;
    }

    private static boolean isHealingSupportAbility(AbilityReward reward) {
        return switch (reward) {
            case HEALING_AURA, HEALING_BOTTLE, BATTLEFIELD_MEDIC, TRIAGE_PULSE, REVITALIZING_PRESENCE, LIFE_GIFT -> true;
            default -> false;
        };
    }

    private static boolean isProtectiveSupportAbility(AbilityReward reward) {
        return switch (reward) {
            case DEFENSIVE_AURA, DEFLECTION, DEFUSAL, PSYCHIC_WALL, GUARDIAN_REPULSE, SHIELD_BLOCK,
                    GUARDIAN_INTERCEPT, EMERGENCY_SHIELD, BODY_BLOCK, CLEANSE_TOUCH, PACK_GUARD -> true;
            default -> false;
        };
    }

    private static AbilityReward byAbilityId(String id) {
        return ABILITY_REWARDS_BY_ID.get(canonicalAbilityId(id));
    }

    private static AttributeReward byAttributeId(String id) {
        return ATTRIBUTE_REWARDS_BY_ID.get(id);
    }

    private static BaseStatReward byBaseStatId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return BaseStatReward.valueOf(id);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String canonicalAbilityId(String id) {
        if (id == null) {
            return "";
        }
        // Internal callers already use canonical IDs. Normalize external input only.
        if (ABILITY_REWARDS_BY_ID.containsKey(id)) return id;
        String normalized = id.trim().toLowerCase(java.util.Locale.ROOT);
        return ABILITY_ALIASES.getOrDefault(normalized, normalized);
    }

    private static AbilityReward tryUnlockNewAbility(TameData data) {
        List<WeightedOption<AbilityReward>> options = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            if (resolveAbilityLevel(data, reward.id) > 0) {
                continue;
            }
            options.add(new WeightedOption<>(reward, modifiedAbilityWeight(data.tameClass, reward)));
        }
        if (options.isEmpty()) {
            return null;
        }
        return pickWeighted(options);
    }

    private static AbilityRollChoice rollAbilityChoice(TameData data) {
        List<WeightedOption<AbilityRollChoice>> options = new ArrayList<>();

        double newAbilityWeight = unownedAbilityCount(data) > 0 ? 1.0D : 0.0D;
        if (newAbilityWeight > 0.0D) {
            options.add(new WeightedOption<>(AbilityRollChoice.NEW_UNLOCK, newAbilityWeight));
        }

        double upgradeWeight = 0.0D;
        for (AbilityReward reward : AbilityReward.values()) {
            int current = data.abilityLevels.getOrDefault(reward.id, 0);
            if (current > 0 && reward.upgradable && current < reward.maxLevel) {
                upgradeWeight += ownedAbilityRollMultiplier(current);
            }
        }
        if (upgradeWeight > 0.0D) {
            options.add(new WeightedOption<>(AbilityRollChoice.UPGRADE_EXISTING, upgradeWeight));
        }

        if (options.isEmpty()) {
            return AbilityRollChoice.NEW_UNLOCK;
        }
        return pickWeighted(options);
    }

    private static void unlockAbility(TameData data, AbilityReward reward) {
        if (data == null || reward == null) {
            return;
        }
        normalizeAliasesForAbility(data, reward.id);
        data.abilities.add(reward.id);
        data.abilityLevels.put(reward.id, 1);
        TameRegistry.markDirty();
    }

    private static int unownedAbilityCount(TameData data) {
        int count = 0;
        for (AbilityReward reward : AbilityReward.values()) {
            if (resolveAbilityLevel(data, reward.id) <= 0) {
                count++;
            }
        }
        return count;
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
        return clampAbilityLevel(canonicalId, level);
    }

    private static int clampAttributeLevel(String attributeId, int level) {
        AttributeReward reward = byAttributeId(attributeId);
        if (reward == null) {
            return Math.max(0, level);
        }
        return Mth.clamp(level, 0, reward.maxLevel);
    }

    private static int clampAbilityLevel(String abilityId, int level) {
        AbilityReward reward = byAbilityId(abilityId);
        if (reward == null) {
            return Math.max(0, level);
        }
        return Mth.clamp(level, 0, reward.maxLevel);
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
        if (entity == null || data == null || TameRegistry.isSummonType(entity)) {
            return;
        }
        String name = PlayerDebugSettings.hideLevelInName(data.ownerUUID)
                ? data.name
                : "[Lvl " + data.level + "] " + data.name;
        entity.setCustomName(Component.literal(name));
        entity.setCustomNameVisible(true);
    }

    private static void applyTrackedBaseStatReward(LivingEntity tame, TameData data, BaseStatReward reward,
                                                   double amount, double previousTrackedBonus) {
        AttributeInstance instance = tame == null ? null : tame.getAttribute(reward.attribute);
        boolean managedLegendaryModifier = reward == BaseStatReward.HP
                || (data != null && isLegendaryMonstersType(data.type) && reward == BaseStatReward.DAMAGE);
        double unmodifiedBase = instance == null
                ? reward.attribute.getDefaultValue()
                : instance.getBaseValue() - (managedLegendaryModifier ? 0.0D : previousTrackedBonus);

        trackBonus(data, reward, amount);
        double totalTrackedBonus = trackedBonus(data, reward);
        if (instance == null) {
            return;
        }
        instance.setBaseValue(clampAttributeBaseValue(reward.attribute,
                unmodifiedBase + (managedLegendaryModifier ? 0.0D : totalTrackedBonus)));
        if (managedLegendaryModifier) {
            UUID modifierId = reward == BaseStatReward.HP
                    ? LEGENDARY_MONSTERS_HEALTH_BONUS_UUID
                    : LEGENDARY_MONSTERS_DAMAGE_BONUS_UUID;
            applyManagedAdditionModifier(tame, reward.attribute, modifierId, totalTrackedBonus,
                    reward == BaseStatReward.HP ? "tl_legendary_bonus_health" : "tl_legendary_bonus_damage");
        }
    }

    private static void addToAttribute(LivingEntity entity, Attribute attribute, double amount) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(clampAttributeBaseValue(attribute, instance.getBaseValue() + amount));
        }
    }

    public static boolean reapplyTypeBasePlusBonuses(LivingEntity tame, TameData data) {
        if (tame == null || data == null || !(tame.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (TameRegistry.isSummonType(tame)) {
            return false;
        }
        normalizeFixedHealthBonuses(data);
        normalizeLiveTypeId(tame, data);
        Entity spawned = tame.getType().create(serverLevel);
        LivingEntity template;
        if (spawned instanceof LivingEntity createdTemplate) {
            template = createdTemplate;
        } else {
            // Some tames do not expose a separate "tamed template"; their default entity is already the tamed form.
            template = tame;
        }

        scrubLegacyManagedModifiers(tame);
        Double forcedMaxHealth = resolveForcedTypeBaseValue(data, Attributes.MAX_HEALTH);
        boolean legendaryMonsters = isLegendaryMonstersType(data.type);
        double maxHealthBase = forcedMaxHealth != null
                ? forcedMaxHealth
                : resolveBaseValue(data, template, Attributes.MAX_HEALTH, data.bonusHealth);
        if (legendaryMonsters) {
            setAttributeBaseValue(tame, Attributes.MAX_HEALTH, maxHealthBase);
            setAttributeBaseValue(tame, Attributes.ATTACK_DAMAGE, resolveBaseValue(data, template, Attributes.ATTACK_DAMAGE, data.bonusDamage));
            applyManagedAdditionModifier(tame, Attributes.ATTACK_DAMAGE, LEGENDARY_MONSTERS_DAMAGE_BONUS_UUID, data.bonusDamage, "tl_legendary_bonus_damage");
        } else {
            setAttributeBaseValue(tame, Attributes.MAX_HEALTH, maxHealthBase);
            setAttributeBaseValue(tame, Attributes.ATTACK_DAMAGE, resolveBaseValue(data, template, Attributes.ATTACK_DAMAGE, data.bonusDamage) + data.bonusDamage);
        }
        applyManagedAdditionModifier(tame, Attributes.MAX_HEALTH, LEGENDARY_MONSTERS_HEALTH_BONUS_UUID, data.bonusHealth, "tl_bonus_health");
        setAttributeBaseValue(tame, Attributes.MOVEMENT_SPEED, resolveBaseValue(data, template, Attributes.MOVEMENT_SPEED, data.bonusSpeed) + data.bonusSpeed);
        setAttributeBaseValue(tame, Attributes.ARMOR, resolveBaseValue(data, template, Attributes.ARMOR, data.bonusArmor) + data.bonusArmor);
        setAttributeBaseValue(tame, Attributes.ARMOR_TOUGHNESS, resolveBaseValue(data, template, Attributes.ARMOR_TOUGHNESS, data.bonusArmorToughness) + data.bonusArmorToughness);
        setAttributeBaseValue(tame, Attributes.ATTACK_KNOCKBACK, clampAttributeBaseValue(Attributes.ATTACK_KNOCKBACK, resolveBaseValue(data, template, Attributes.ATTACK_KNOCKBACK, data.bonusKnockback) + data.bonusKnockback));
        setAttributeBaseValue(tame, Attributes.KNOCKBACK_RESISTANCE, clampAttributeBaseValue(Attributes.KNOCKBACK_RESISTANCE, resolveBaseValue(data, template, Attributes.KNOCKBACK_RESISTANCE, data.bonusKnockbackResist) + data.bonusKnockbackResist));
        TameableUtils.refreshCollarAttributes(tame);

        updateTameName(tame, data);
        tame.setHealth((float) Mth.clamp(tame.getHealth(), 1.0D, tame.getMaxHealth()));
        return true;
    }

    public static boolean needsDeferredStatRefresh(LivingEntity tame, TameData data) {
        ResourceLocation liveType = tame == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (liveType != null && "crittersandcompanions".equals(liveType.getNamespace()) && "dragonfly".equals(liveType.getPath())) {
            return true;
        }
        return isDragonflyType(data == null ? null : data.type);
    }

    private static double readBaseOrDefault(LivingEntity tame, Attribute attribute) {
        if (tame == null || attribute == null) return 0.0D;
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return attribute.getDefaultValue();
        return instance.getBaseValue();
    }

    /** Retain the live base and external modifiers while replacing, not stacking, TL's HP reward. */
    public static void refreshTrackedHealthBonus(LivingEntity tame, TameData data) {
        applyManagedAdditionModifier(tame, Attributes.MAX_HEALTH, LEGENDARY_MONSTERS_HEALTH_BONUS_UUID,
                data.bonusHealth, "tl_bonus_health");
    }

    private static double resolveBaseValue(TameData data, LivingEntity template, Attribute attribute, double trackedBonus) {
        Double forcedBase = resolveForcedTypeBaseValue(data, attribute);
        if (forcedBase != null) {
            return forcedBase;
        }
        UUID managedBonusId = attribute == Attributes.MAX_HEALTH
                ? LEGENDARY_MONSTERS_HEALTH_BONUS_UUID
                : data != null && isLegendaryMonstersType(data.type) && attribute == Attributes.ATTACK_DAMAGE
                ? LEGENDARY_MONSTERS_DAMAGE_BONUS_UUID
                : null;
        Double snapshotBase = readBaseFromSnapshot(data == null ? null : data.entitySnapshot, attribute, trackedBonus, managedBonusId);
        if (snapshotBase != null) {
            double templateBase = readBaseOrDefault(template, attribute);
            // Repair snapshots produced while the reward existed both in the live base and in
            // the tracked total. In that broken state, subtracting the tracked total once still
            // leaves exactly one additional copy above the entity type's real base.
            if (Math.abs(trackedBonus) > 1.0E-6D
                    && !hasExternalBaseScaling(data, attribute)
                    && Math.abs(snapshotBase - templateBase - trackedBonus) <= 1.0E-6D) {
                return templateBase;
            }
            return snapshotBase;
        }
        return readBaseOrDefault(template, attribute);
    }

    private static boolean hasExternalBaseScaling(TameData data, Attribute attribute) {
        if (data == null || data.entitySnapshot == null) return false;
        CompoundTag persistent = data.entitySnapshot.getCompound("ForgeData");
        // Armageddon scales the base once and retains these flags across saves.
        // A scaled base can coincide with a TL reward; it is not a duplicate reward.
        return (attribute == Attributes.MAX_HEALTH && persistent.getBoolean("health_doubled"))
                || (attribute == Attributes.ATTACK_DAMAGE && persistent.getBoolean("damage_doubled"));
    }

    private static Double resolveForcedTypeBaseValue(TameData data, Attribute attribute) {
        Double primitiveBase = com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat.baseValue(data, attribute);
        if (primitiveBase != null) return primitiveBase;
        if (data == null || data.type == null) {
            return resolveForcedClassBaseValue(data, attribute);
        }
        if (attribute == Attributes.MAX_HEALTH && isDragonflyType(data.type)) {
            return 4.0D;
        }
        // Rabbit.Variant.EVIL applies these bases from setVariant. A freshly created
        // rabbit template is the normal variant, so generic stat restoration would
        // otherwise collapse an evil tame back to ordinary rabbit stats after respawn.
        if (isEvilRabbit(data)) {
            if (attribute == Attributes.MAX_HEALTH) return 30.0D;
            if (attribute == Attributes.ARMOR) return 8.0D;
        }
        return resolveForcedClassBaseValue(data, attribute);
    }

    private static boolean isEvilRabbit(TameData data) {
        if (data == null || data.type == null || data.entitySnapshot == null) {
            return false;
        }
        String normalized = data.type.trim().toLowerCase(java.util.Locale.ROOT);
        boolean rabbit = "minecraft:rabbit".equals(normalized)
                || "entity.minecraft.rabbit".equals(normalized);
        return rabbit && data.entitySnapshot.contains("RabbitType", net.minecraft.nbt.Tag.TAG_INT)
                && data.entitySnapshot.getInt("RabbitType") == 99;
    }

    private static boolean isDragonflyType(String typeId) {
        return "crittersandcompanions:dragonfly".equals(typeId)
                || "entity.crittersandcompanions.dragonfly".equals(typeId);
    }

    private static boolean isLegendaryMonstersType(String typeId) {
        if (typeId == null || typeId.isBlank()) {
            return false;
        }
        String normalized = typeId.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            normalized = normalized.substring("entity.".length());
        }
        return normalized.startsWith("legendary_monsters:")
                || normalized.startsWith("legendary_monsters.");
    }

    private static void normalizeLiveTypeId(LivingEntity tame, TameData data) {
        if (tame == null || data == null) {
            return;
        }
        ResourceLocation liveType = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (liveType == null) {
            return;
        }
        String normalized = liveType.toString();
        if (!normalized.equals(data.type)) {
            data.type = normalized;
        }
    }

    private static Double resolveForcedClassBaseValue(TameData data, Attribute attribute) {
        return null;
    }

    private static boolean usesFixedHealthClass(TameClass tameClass) {
        return false;
    }

    private static void normalizeFixedHealthBonuses(TameData data) {
        if (data == null || !usesFixedHealthClass(data.tameClass)) {
            return;
        }
        double currentHealthBonus = data.bonusHealth;
        double savedHealthBonus = data.savedBonusHealth;
        if (Math.abs(currentHealthBonus) > 1.0E-6D) {
            convertFixedHealthBonus(data, currentHealthBonus, false);
            data.bonusHealth = 0.0D;
        }
        if (Math.abs(savedHealthBonus) > 1.0E-6D) {
            convertFixedHealthBonus(data, savedHealthBonus, true);
            data.savedBonusHealth = 0.0D;
        }
    }

    private static void convertFixedHealthBonus(TameData data, double healthAmount, boolean saved) {
        if (data == null || Math.abs(healthAmount) <= 1.0E-6D) {
            return;
        }
        double armorAmount = fixedHealthArmorAmount(healthAmount);
        double knockbackResistAmount = fixedHealthKnockbackResistAmount(healthAmount);
        if (saved) {
            data.savedBonusArmor += armorAmount;
            data.savedBonusKnockbackResist += knockbackResistAmount;
        } else {
            data.bonusArmor += armorAmount;
            data.bonusKnockbackResist += knockbackResistAmount;
        }
    }

    private static double fixedHealthArmorAmount(double healthAmount) {
        return healthAmount * 0.5D;
    }

    private static double fixedHealthKnockbackResistAmount(double healthAmount) {
        return healthAmount * 0.5D * BaseStatReward.KNOCKBACK_RESIST.amount;
    }

    private static Double readBaseFromSnapshot(CompoundTag snapshot, Attribute attribute, double trackedBonus, UUID managedBonusId) {
        if (snapshot == null || snapshot.isEmpty() || attribute == null) {
            return null;
        }
        if (!snapshot.contains("Attributes", Tag.TAG_LIST)) {
            return null;
        }
        net.minecraft.resources.ResourceLocation key = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (key == null) {
            return null;
        }
        ListTag attributes = snapshot.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag entry = attributes.getCompound(i);
            if (!entry.contains("Name", Tag.TAG_STRING) || !entry.contains("Base", Tag.TAG_DOUBLE)) {
                continue;
            }
            if (!key.toString().equals(entry.getString("Name"))) {
                continue;
            }
            double base = entry.getDouble("Base");
            double normalized = snapshotAttributeHasModifier(entry, managedBonusId) ? base : base - trackedBonus;
            double defaultValue = attribute.getDefaultValue();
            // Some old saves store the raw entity base in the snapshot instead of "base + tracked bonus".
            // In that case subtracting the tracked bonus produces nonsense and would erase all TL bonuses.
            if (normalized <= 0.0D || normalized < defaultValue * 0.25D) {
                return null;
            }
            return normalized;
        }
        return null;
    }

    private static boolean snapshotAttributeHasModifier(CompoundTag attributeEntry, UUID modifierId) {
        if (attributeEntry == null || modifierId == null || !attributeEntry.contains("Modifiers", Tag.TAG_LIST)) {
            return false;
        }
        ListTag modifiers = attributeEntry.getList("Modifiers", Tag.TAG_COMPOUND);
        for (int i = 0; i < modifiers.size(); i++) {
            CompoundTag modifier = modifiers.getCompound(i);
            if (modifier.hasUUID("UUID") && modifierId.equals(modifier.getUUID("UUID"))) {
                return true;
            }
        }
        return false;
    }

    private static void setAttributeBaseValue(LivingEntity tame, Attribute attribute, double value) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) return;
        instance.setBaseValue(clampAttributeBaseValue(attribute, value));
    }

    private static void scrubLegacyManagedModifiers(LivingEntity tame) {
        scrubUnknownModifiers(tame, Attributes.MAX_HEALTH, LEGENDARY_MONSTERS_HEALTH_BONUS_UUID);
        scrubUnknownModifiers(tame, Attributes.ATTACK_DAMAGE, LEGENDARY_MONSTERS_DAMAGE_BONUS_UUID);
        scrubUnknownModifiers(tame, Attributes.MOVEMENT_SPEED);
        scrubUnknownModifiers(tame, Attributes.ARMOR, COLLAR_ARMOR_UUID);
        scrubUnknownModifiers(tame, Attributes.ARMOR_TOUGHNESS, COLLAR_ARMOR_TOUGHNESS_UUID);
        scrubUnknownModifiers(tame, Attributes.ATTACK_KNOCKBACK);
        scrubUnknownModifiers(tame, Attributes.KNOCKBACK_RESISTANCE);
    }

    private static void scrubUnknownModifiers(LivingEntity tame, Attribute attribute, UUID... preservedModifierIds) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        Set<UUID> preserved = preservedModifierIds.length == 0
                ? new HashSet<>()
                : new HashSet<>(List.of(preservedModifierIds));
        preserveEquippedItemModifiers(tame, attribute, preserved);
        com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat.preserveIndividualModifiers(tame, attribute, preserved);
        for (AttributeModifier modifier : new ArrayList<>(instance.getModifiers())) {
            if (!preserved.contains(modifier.getId())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private static double clampAttributeBaseValue(Attribute attribute, double value) {
        if (attribute == Attributes.ATTACK_KNOCKBACK) {
            return Mth.clamp(value, 0.0D, 2.0D);
        }
        return value;
    }

    private static void applyManagedAdditionModifier(LivingEntity tame, Attribute attribute, UUID id, double amount, String name) {
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null) {
            instance.removeModifier(existing);
        }
        if (Math.abs(amount) <= 1.0E-6D) {
            return;
        }
        instance.addPermanentModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void trackBonus(TameData data, BaseStatReward reward) {
        trackBonus(data, reward, reward.amount);
    }

    private static void trackBonus(TameData data, BaseStatReward reward, double amount) {
        switch (reward) {
            case HP -> data.bonusHealth += amount;
            case DAMAGE -> data.bonusDamage += amount;
            case SPEED -> data.bonusSpeed += amount;
            case ARMOR -> data.bonusArmor += amount;
            case ARMOR_TOUGHNESS -> data.bonusArmorToughness += amount;
            case KNOCKBACK -> data.bonusKnockback += amount;
            case KNOCKBACK_RESIST -> data.bonusKnockbackResist += amount;
        }
    }

    private static void preserveEquippedItemModifiers(LivingEntity tame, Attribute attribute, Set<UUID> preserved) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack equipped = tame.getItemBySlot(slot);
            if (equipped.isEmpty()) continue;
            for (AttributeModifier modifier : equipped.getAttributeModifiers(slot).get(attribute)) {
                preserved.add(modifier.getId());
            }
        }
    }

    private static double trackedBonus(TameData data, BaseStatReward reward) {
        if (data == null) {
            return 0.0D;
        }
        return switch (reward) {
            case HP -> data.bonusHealth;
            case DAMAGE -> data.bonusDamage;
            case SPEED -> data.bonusSpeed;
            case ARMOR -> data.bonusArmor;
            case ARMOR_TOUGHNESS -> data.bonusArmorToughness;
            case KNOCKBACK -> data.bonusKnockback;
            case KNOCKBACK_RESIST -> data.bonusKnockbackResist;
        };
    }

    public static int estimateInvestedXp(TameData data) {
        int total = 0;
        for (int lvl = 1; lvl < data.level; lvl++) {
            total += xpRequiredForLevel(lvl);
        }
        total += Math.max(0, data.xp);
        return total;
    }

    private static int totalXpRequiredForLevel(int level) {
        int total = 0;
        for (int lvl = 1; lvl < Math.max(1, level); lvl++) {
            total += xpRequiredForLevel(lvl);
        }
        return total;
    }

    private static int levelForInvestedXp(int totalXp) {
        int remaining = Math.max(0, totalXp);
        int level = 1;
        int xpToNext = xpRequiredForLevel(level);

        while (remaining >= xpToNext) {
            remaining -= xpToNext;
            level++;
            xpToNext = xpRequiredForLevel(level);
        }
        return level;
    }

    private static void applyInvestedXp(TameData data, int totalXp) {
        int remaining = Math.max(0, totalXp);
        int level = 1;
        int xpToNext = xpRequiredForLevel(level);

        while (remaining >= xpToNext) {
            remaining -= xpToNext;
            level++;
            xpToNext = xpRequiredForLevel(level);
        }

        data.level = level;
        data.xp = remaining;
        data.xpToNext = xpToNext;
    }

    private static void rollbackLostLevelRewards(LivingEntity tame, TameData data, int previousLevel, int resultingLevel) {
        for (int level = previousLevel; level > resultingLevel; level--) {
            CompoundTag row = findLatestLevelRewardRow(data, level, true);
            LevelRewardResult reward = LevelRewardResult.fromHistoryRow(row);
            if (reward == null) {
                continue;
            }
            removeStoredLevelReward(tame, data, reward);
            row.putBoolean("active", false);
        }
    }

    private static LevelRewardResult restoreStoredLevelReward(LivingEntity tame, TameData data, int level) {
        CompoundTag row = findLatestLevelRewardRow(data, level, false);
        LevelRewardResult reward = LevelRewardResult.fromHistoryRow(row);
        if (reward == null) {
            return null;
        }
        applyStoredLevelReward(tame, data, reward);
        row.putBoolean("active", true);
        return reward;
    }

    private static CompoundTag findLatestLevelRewardRow(TameData data, int level, boolean active) {
        for (int i = data.levelRewardHistory.size() - 1; i >= 0; i--) {
            CompoundTag row = data.levelRewardHistory.get(i);
            if (row.getInt("level") != level) {
                continue;
            }
            boolean rowActive = !row.contains("active") || row.getBoolean("active");
            if (rowActive == active) {
                return row;
            }
        }
        return null;
    }

    private static void applyStoredLevelReward(LivingEntity tame, TameData data, LevelRewardResult reward) {
        switch (reward.category()) {
            case BASE_STAT -> {
                BaseStatReward baseReward = byBaseStatId(reward.rewardId());
                if (baseReward != null) {
                    applyBaseStatReward(tame, data, baseReward, reward.amount());
                }
            }
            case ATTRIBUTE -> addAttribute(data, reward.rewardId(), Math.max(1, (int) Math.round(reward.amount())));
            case ABILITY -> addAbility(data, reward.rewardId(), Math.max(1, (int) Math.round(reward.amount())));
        }
    }

    private static void removeStoredLevelReward(LivingEntity tame, TameData data, LevelRewardResult reward) {
        switch (reward.category()) {
            case BASE_STAT -> {
                BaseStatReward baseReward = byBaseStatId(reward.rewardId());
                if (baseReward != null) {
                    applyBaseStatReward(tame, data, baseReward, -reward.amount());
                }
            }
            case ATTRIBUTE -> removeAttribute(data, reward.rewardId(), Math.max(1, (int) Math.round(reward.amount())));
            case ABILITY -> removeAbility(data, reward.rewardId(), Math.max(1, (int) Math.round(reward.amount())));
        }
    }

    public static int xpRequiredForLevel(int level) {
        int currentLevel = Math.max(1, level);
        if (currentLevel <= 16) {
            return 2 * currentLevel + 7;
        }
        if (currentLevel <= 31) {
            return 5 * currentLevel - 38;
        }
        return 9 * currentLevel - 158;
    }

    public static boolean normalizeXpForCurrentLevel(TameData data) {
        if (data == null) {
            return false;
        }
        boolean changed = false;

        int normalizedLevel = Math.max(1, data.level);
        if (data.level != normalizedLevel) {
            data.level = normalizedLevel;
            changed = true;
        }
        int normalizedXpToNext = xpRequiredForLevel(data.level);
        int remappedXp = remapProgressXp(data.xp, data.xpToNext, normalizedXpToNext);
        if (data.xp != remappedXp) {
            data.xp = remappedXp;
            changed = true;
        }
        if (data.xpToNext != normalizedXpToNext) {
            data.xpToNext = normalizedXpToNext;
            changed = true;
        }

        if (data.hasSavedProgress) {
            int normalizedSavedLevel = Math.max(1, data.savedLevel);
            if (data.savedLevel != normalizedSavedLevel) {
                data.savedLevel = normalizedSavedLevel;
                changed = true;
            }
            int normalizedSavedXpToNext = xpRequiredForLevel(data.savedLevel);
            int remappedSavedXp = remapProgressXp(data.savedXp, data.savedXpToNext, normalizedSavedXpToNext);
            if (data.savedXp != remappedSavedXp) {
                data.savedXp = remappedSavedXp;
                changed = true;
            }
            if (data.savedXpToNext != normalizedSavedXpToNext) {
                data.savedXpToNext = normalizedSavedXpToNext;
                changed = true;
            }
        }
        return changed;
    }

    private static int remapProgressXp(int xp, int oldXpToNext, int newXpToNext) {
        int safeTarget = Math.max(1, newXpToNext);
        int safeOld = Math.max(1, oldXpToNext);
        int clampedXp = Mth.clamp(xp, 0, Math.max(0, safeOld - 1));
        double progress = (double) clampedXp / (double) safeOld;
        return Mth.clamp((int) Math.floor(progress * safeTarget), 0, Math.max(0, safeTarget - 1));
    }

    public static void storeProgressSnapshot(TameData data) {
        normalizeFixedHealthBonuses(data);
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

    public static void storeHighestProgressSnapshot(TameData data) {
        if (data == null) {
            return;
        }
        int currentTotal = estimateInvestedXp(data);
        if (!data.hasSavedProgress || currentTotal >= estimateSavedProgress(data)) {
            storeProgressSnapshot(data);
        }
    }

    public static int estimateSavedProgress(TameData data) {
        if (data == null || !data.hasSavedProgress) {
            return 0;
        }
        int total = 0;
        for (int lvl = 1; lvl < Math.max(1, data.savedLevel); lvl++) {
            total += xpRequiredForLevel(lvl);
        }
        total += Math.max(0, data.savedXp);
        return total;
    }

    public static int reincarnationXpCost(TameData data) {
        if (data == null || !data.hasSavedProgress) {
            return 0;
        }
        if (data.level >= data.savedLevel) {
            return 0;
        }
        return Math.max(0, estimateSavedProgress(data) - estimateInvestedXp(data));
    }

    public static int rerollHalfDamageBonus(TameData data) {
        if (data == null) {
            return 0;
        }
        int rerolled = 0;
        rerolled += rerollHalfDamageValueIntoProgress(data, false);
        rerolled += rerollHalfDamageValueIntoProgress(data, true);
        return rerolled;
    }

    public static int restoreHalfDamageBonusFromHealth(TameData data) {
        if (data == null) {
            return 0;
        }
        int moved = 0;
        moved += restoreHalfDamageBonusFromHealth(data, false);
        moved += restoreHalfDamageBonusFromHealth(data, true);
        return moved;
    }

    private static int restoreHalfDamageBonusFromHealth(TameData data, boolean saved) {
        double currentDamage = saved ? data.savedBonusDamage : data.bonusDamage;
        double currentHealth = saved ? data.savedBonusHealth : data.bonusHealth;
        if (currentDamage <= 0.0D || currentHealth <= 0.0D) {
            return 0;
        }
        int pointsToMove = (int) Math.min(Math.floor(currentDamage), Math.floor(currentHealth));
        if (pointsToMove <= 0) {
            return 0;
        }
        if (saved) {
            data.savedBonusHealth = Math.max(0.0D, data.savedBonusHealth - pointsToMove);
            data.savedBonusDamage += pointsToMove;
        } else {
            data.bonusHealth = Math.max(0.0D, data.bonusHealth - pointsToMove);
            data.bonusDamage += pointsToMove;
        }
        return pointsToMove;
    }

    private static int rerollHalfDamageValueIntoProgress(TameData data, boolean saved) {
        double current = saved ? data.savedBonusDamage : data.bonusDamage;
        if (current <= 0.0D) {
            return 0;
        }
        int rollsToReroll = (int) Math.floor(current * 0.5D);
        if (rollsToReroll <= 0) {
            return 0;
        }
        if (saved) {
            data.savedBonusDamage = Math.max(0.0D, data.savedBonusDamage - rollsToReroll);
        } else {
            data.bonusDamage = Math.max(0.0D, data.bonusDamage - rollsToReroll);
        }
        for (int i = 0; i < rollsToReroll; i++) {
            double roll = RANDOM.nextDouble();
            if (roll < 0.70D) {
                BaseStatReward reward = pickWeightedNonDamageBaseReward();
                if (reward == null) {
                    continue;
                }
                if (saved) {
                    applySavedBaseStatReward(data, reward);
                } else {
                    trackBonus(data, reward);
                }
                continue;
            }
            if (roll < 0.90D) {
                applyMigratedAttributeReward(data, saved);
                continue;
            }
            applyMigratedAbilityReward(data, saved);
        }
        return rollsToReroll;
    }

    private static void applyMigratedAttributeReward(TameData data, boolean saved) {
        AttributeReward reward = pickMigratedAttributeReward(data, saved);
        if (reward == null) {
            BaseStatReward fallback = pickWeightedNonDamageBaseReward();
            if (fallback != null) {
                if (saved) {
                    applySavedBaseStatReward(data, fallback);
                } else {
                    trackBonus(data, fallback);
                }
            }
            return;
        }
        if (saved) {
            int current = data.savedAttributeLevels.getOrDefault(reward.id, 0);
            data.savedAttributeLevels.put(reward.id, current + 1);
        } else {
            int current = data.attributeLevels.getOrDefault(reward.id, 0);
            data.attributeLevels.put(reward.id, current + 1);
            TameRegistry.markDirty();
        }
    }

    private static AttributeReward pickMigratedAttributeReward(TameData data, boolean saved) {
        List<WeightedOption<AttributeReward>> options = new ArrayList<>();
        for (AttributeReward reward : AttributeReward.values()) {
            int current = saved
                    ? data.savedAttributeLevels.getOrDefault(reward.id, 0)
                    : data.attributeLevels.getOrDefault(reward.id, 0);
            if (current >= reward.maxLevel) {
                continue;
            }
            double weight = modifiedAttributeWeight(data.tameClass, reward) * ownedAttributeRollMultiplier(current);
            options.add(new WeightedOption<>(reward, weight));
        }
        return options.isEmpty() ? null : pickWeighted(options);
    }

    private static void applyMigratedAbilityReward(TameData data, boolean saved) {
        AbilityReward reward = pickMigratedAbilityReward(data, saved);
        if (reward == null) {
            applyMigratedAttributeReward(data, saved);
            return;
        }
        if (saved) {
            data.savedAbilities.add(reward.id);
            int current = data.savedAbilityLevels.getOrDefault(reward.id, 0);
            data.savedAbilityLevels.put(reward.id, current + 1);
        } else {
            normalizeAliasesForAbility(data, reward.id);
            data.abilities.add(reward.id);
            int current = data.abilityLevels.getOrDefault(reward.id, 0);
            data.abilityLevels.put(reward.id, current + 1);
            TameRegistry.markDirty();
        }
    }

    private static AbilityReward pickMigratedAbilityReward(TameData data, boolean saved) {
        List<WeightedOption<AbilityReward>> options = new ArrayList<>();
        for (AbilityReward reward : AbilityReward.values()) {
            int current = saved
                    ? data.savedAbilityLevels.getOrDefault(reward.id, 0)
                    : resolveAbilityLevel(data, reward.id);
            if (current >= reward.maxLevel) {
                continue;
            }
            double weight = modifiedAbilityWeight(data.tameClass, reward);
            if (current > 0) {
                weight *= ownedAbilityRollMultiplier(current);
            }
            options.add(new WeightedOption<>(reward, weight));
        }
        return options.isEmpty() ? null : pickWeighted(options);
    }

    private static BaseStatReward pickWeightedNonDamageBaseReward() {
        List<WeightedOption<BaseStatReward>> options = new ArrayList<>();
        for (BaseStatReward reward : BaseStatReward.values()) {
            if (reward == BaseStatReward.DAMAGE) {
                continue;
            }
            double weight = switch (reward) {
                case HP -> 70.7D;
                case SPEED -> 3.0D;
                case ARMOR -> 5.0D;
                case ARMOR_TOUGHNESS -> 2.0D;
                case KNOCKBACK -> 5.0D;
                case KNOCKBACK_RESIST -> 5.0D;
                default -> 0.0D;
            };
            if (weight > 0.0D) {
                options.add(new WeightedOption<>(reward, weight));
            }
        }
        return options.isEmpty() ? null : pickWeighted(options);
    }

    private static void applySavedBaseStatReward(TameData data, BaseStatReward reward) {
        switch (reward) {
            case HP -> data.savedBonusHealth += reward.amount;
            case DAMAGE -> data.savedBonusDamage += reward.amount;
            case SPEED -> data.savedBonusSpeed += reward.amount;
            case ARMOR -> data.savedBonusArmor += reward.amount;
            case ARMOR_TOUGHNESS -> data.savedBonusArmorToughness += reward.amount;
            case KNOCKBACK -> data.savedBonusKnockback += reward.amount;
            case KNOCKBACK_RESIST -> data.savedBonusKnockbackResist += reward.amount;
        }
    }

    public static void resetProgress(LivingEntity tame, TameData data) {
        normalizeFixedHealthBonuses(data);
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

    public static boolean restoreProgress(LivingEntity tame, TameData data) {
        normalizeFixedHealthBonuses(data);
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

    public static boolean restoreHighestProgressWithoutXpCost(LivingEntity tame, TameData data) {
        normalizeFixedHealthBonuses(data);
        if (tame == null || data == null || !data.hasSavedProgress) {
            return false;
        }
        if (data.level >= data.savedLevel) {
            return false;
        }

        data.level = data.savedLevel;
        data.xp = data.savedXp;
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

        data.hasSavedProgress = false;
        data.savedProgressCost = 0;
        data.savedLevel = 1;
        data.savedXp = 0;
        data.savedXpToNext = xpRequiredForLevel(1);
        data.savedKills = 0;
        data.savedAssists = 0;
        data.savedBonusHealth = 0;
        data.savedBonusDamage = 0;
        data.savedBonusSpeed = 0;
        data.savedBonusArmor = 0;
        data.savedBonusArmorToughness = 0;
        data.savedBonusKnockback = 0;
        data.savedBonusKnockbackResist = 0;
        data.savedAbilities.clear();
        data.savedAbilityLevels.clear();
        data.savedAttributeLevels.clear();

        updateTameName(tame, data);
        tame.setHealth(tame.getMaxHealth());
        TameRegistry.markDirty();
        return true;
    }

    private static void applyBonusDelta(
            LivingEntity tame,
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
