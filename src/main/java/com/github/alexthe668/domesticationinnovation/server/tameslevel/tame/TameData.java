package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.TameClass;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TameData {
    public static final String TL_ID_TAG = "tl_id";

    public UUID uuid;
    public UUID tlId;
    public UUID ownerUUID;

    public String name;
    public String type;

    public int level = 1;
    public int xp = 0;
    public int xpToNext = 50;
    public long bornDayTime = -1L;
    public int activeSurvivalDays = 0;
    public long lastActiveSurvivalDay = Long.MIN_VALUE;

    public int kills = 0;
    public int assists = 0;
    public int deaths = 0;
    public int duelMmr = PlayerDuelStats.DEFAULT_MMR;
    public int duelKills = 0;
    public int duelAssists = 0;
    public int duelDeaths = 0;
    public int duelWins = 0;
    public int duelLosses = 0;
    public int duelCount = 0;
    public double duelPoints = 0.0D;
    public boolean dead = false;
    public boolean stored = false;
    public long deadGameTime = 0L;
    public long deadUnixMillis = 0L;
    public String deathDimension = "";
    public int deathX = 0;
    public int deathY = 0;
    public int deathZ = 0;
    public final List<CompoundTag> deathHistory = new ArrayList<>();
    public final List<CompoundTag> levelRewardHistory = new ArrayList<>();
    public String lastKnownDimension = "";
    public int lastKnownX = 0;
    public int lastKnownY = 0;
    public int lastKnownZ = 0;
    public long lastKnownGameTime = 0L;
    public boolean hasHome = false;
    public String homeDimension = "";
    public int homeX = 0;
    public int homeY = 0;
    public int homeZ = 0;
    public boolean hasPreviousHome = false;
    public String previousHomeDimension = "";
    public int previousHomeX = 0;
    public int previousHomeY = 0;
    public int previousHomeZ = 0;
    public int guardianReturnTicks = 0;
    public boolean guardianRelaxing = false;
    public long guardianNextPhaseTick = 0L;
    public UUID guardianTargetUuid = null;
    public int guardianTargetStuckTicks = 0;
    public double guardianTargetBestDistanceSq = 0.0D;
    public int bodyguardRange = 12;
    public String oreScentingOreId = "";
    public boolean hasPetBed = false;
    public String petBedDimension = "";
    public int petBedX = 0;
    public int petBedY = 0;
    public int petBedZ = 0;
    public boolean hasCollarTag = false;
    public int collarTagTier = 0;

    public double baseSpeed;
    public double bonusHealth;
    public double bonusDamage;
    public double bonusSpeed;
    public double bonusArmor;
    public double bonusArmorToughness;
    public double bonusKnockback;
    public double bonusKnockbackResist;

    public TameClass tameClass;
    public int classRerollsUsed = 0;
    public int mode = 0;
    public int movementOrder = 0;
    public String group = "";
    public boolean defendAllies = false;
    public boolean escapeMode = true;
    public boolean escapeActive = false;
    public boolean wanderLock = false;
    public boolean skeletonMovement = false;
    public boolean closeMovement = false;
    public boolean hasProtectionZone = false;
    public String protectionDimension = "";
    public int protectionX = 0;
    public int protectionY = 0;
    public int protectionZ = 0;
    public int protectionRadius = 16;
    public final Map<String, CompoundTag> guardianSetAnchors = new LinkedHashMap<>();

    public final Set<String> abilities = new LinkedHashSet<>();
    public final Map<String, Integer> abilityLevels = new LinkedHashMap<>();
    public final Map<String, Integer> attributeLevels = new LinkedHashMap<>();
    public final Map<String, Long> cooldowns = new LinkedHashMap<>();
    public CompoundTag entitySnapshot = new CompoundTag();
    public int hungerSaturation = 1000;
    public final List<ItemStack> hungerInventory = new ArrayList<>();
    public final List<ItemStack> armorInventory = new ArrayList<>();
    public boolean hungerEmptyNotified = false;
    public boolean hungerLowNotified = false;
    public boolean hungerLastFoodNotified = false;
    public boolean hungerAutopickup = false;

    public boolean hasSavedProgress = false;
    public int savedProgressCost = 0;
    public int savedLevel = 1;
    public int savedXp = 0;
    public int savedXpToNext = 50;
    public int savedKills = 0;
    public int savedAssists = 0;
    public double savedBonusHealth = 0;
    public double savedBonusDamage = 0;
    public double savedBonusSpeed = 0;
    public double savedBonusArmor = 0;
    public double savedBonusArmorToughness = 0;
    public double savedBonusKnockback = 0;
    public double savedBonusKnockbackResist = 0;
    public final Set<String> savedAbilities = new LinkedHashSet<>();
    public final Map<String, Integer> savedAbilityLevels = new LinkedHashMap<>();
    public final Map<String, Integer> savedAttributeLevels = new LinkedHashMap<>();
    public boolean liveOnly = false;
    public boolean horseType = false;
    public double riddenDistanceProgress = 0.0D;

    public TameData(TamableAnimal tame) {

        this((LivingEntity) tame, tame.getOwnerUUID(), false);
    }

    public TameData(LivingEntity tame, UUID ownerUUID, boolean liveOnly) {

        this.uuid = tame.getUUID();
        this.tlId = readOrCreateTlId(tame);
        this.ownerUUID = ownerUUID;
        this.liveOnly = liveOnly;
        this.horseType = tame instanceof net.minecraft.world.entity.animal.horse.AbstractHorse;

        this.type = tame.getType().toString();
        this.name = TameRegistry.stripLevelPrefixes(tame.hasCustomName() ? tame.getCustomName().getString() : tame.getName().getString());
        this.bornDayTime = tame.level().getDayTime();
        this.lastKnownDimension = tame.level().dimension().location().toString();
        this.lastKnownX = tame.blockPosition().getX();
        this.lastKnownY = tame.blockPosition().getY();
        this.lastKnownZ = tame.blockPosition().getZ();
        this.lastKnownGameTime = tame.level().getGameTime();
        this.hasHome = false;
        this.homeDimension = tame.level().dimension().location().toString();
        this.homeX = tame.blockPosition().getX();
        this.homeY = tame.blockPosition().getY();
        this.homeZ = tame.blockPosition().getZ();
        this.hasPetBed = false;
        this.petBedDimension = "";
        this.hasCollarTag = TameableUtils.hasCollar(tame);
        this.collarTagTier = this.hasCollarTag ? Math.max(0, TameableUtils.getEnchantLevel(tame, Enchantments.ALL_DAMAGE_PROTECTION)) : 0;
        syncTlIdToEntity(tame, this.tlId);
        tame.save(entitySnapshot);

        if (tame.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.baseSpeed = tame.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
        }
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("uuid", uuid);
        tag.putBoolean("liveOnly", liveOnly);
        tag.putBoolean("horseType", horseType);
        tag.putDouble("riddenDistanceProgress", riddenDistanceProgress);
        if (tlId != null) {
            tag.putUUID(TL_ID_TAG, tlId);
        }
        if (ownerUUID != null) {
            tag.putUUID("ownerUUID", ownerUUID);
        }
        tag.putString("name", name == null ? "" : name);
        tag.putString("type", type == null ? "" : type);
        tag.putInt("level", level);
        tag.putInt("xp", xp);
        tag.putInt("xpToNext", xpToNext);
        tag.putLong("bornDayTime", bornDayTime);
        tag.putInt("activeSurvivalDays", activeSurvivalDays);
        tag.putLong("lastActiveSurvivalDay", lastActiveSurvivalDay);
        tag.putInt("kills", kills);
        tag.putInt("assists", assists);
        tag.putInt("deaths", deaths);
        tag.putInt("duelMmr", duelMmr);
        tag.putInt("duelKills", Math.max(0, duelKills));
        tag.putInt("duelAssists", Math.max(0, duelAssists));
        tag.putInt("duelDeaths", Math.max(0, duelDeaths));
        tag.putInt("duelWins", Math.max(0, duelWins));
        tag.putInt("duelLosses", Math.max(0, duelLosses));
        tag.putInt("duelCount", Math.max(0, duelCount));
        tag.putDouble("duelPoints", Math.max(0.0D, duelPoints));
        tag.putBoolean("dead", dead);
        tag.putBoolean("stored", stored);
        tag.putLong("deadGameTime", deadGameTime);
        tag.putLong("deadUnixMillis", deadUnixMillis);
        tag.putString("deathDimension", deathDimension == null ? "" : deathDimension);
        tag.putInt("deathX", deathX);
        tag.putInt("deathY", deathY);
        tag.putInt("deathZ", deathZ);
        ListTag deathHistoryTag = new ListTag();
        for (CompoundTag entry : deathHistory) {
            if (entry != null && !entry.isEmpty()) {
                deathHistoryTag.add(entry.copy());
            }
        }
        tag.put("deathHistory", deathHistoryTag);
        ListTag levelRewardHistoryTag = new ListTag();
        for (CompoundTag entry : levelRewardHistory) {
            if (entry != null && !entry.isEmpty()) {
                levelRewardHistoryTag.add(entry.copy());
            }
        }
        tag.put("levelRewardHistory", levelRewardHistoryTag);
        tag.putString("lastKnownDimension", lastKnownDimension == null ? "" : lastKnownDimension);
        tag.putInt("lastKnownX", lastKnownX);
        tag.putInt("lastKnownY", lastKnownY);
        tag.putInt("lastKnownZ", lastKnownZ);
        tag.putLong("lastKnownGameTime", lastKnownGameTime);
        tag.putBoolean("hasHome", hasHome);
        tag.putString("homeDimension", homeDimension == null ? "" : homeDimension);
        tag.putInt("homeX", homeX);
        tag.putInt("homeY", homeY);
        tag.putInt("homeZ", homeZ);
        tag.putBoolean("hasPreviousHome", hasPreviousHome);
        tag.putString("previousHomeDimension", previousHomeDimension == null ? "" : previousHomeDimension);
        tag.putInt("previousHomeX", previousHomeX);
        tag.putInt("previousHomeY", previousHomeY);
        tag.putInt("previousHomeZ", previousHomeZ);
        tag.putInt("guardianReturnTicks", guardianReturnTicks);
        tag.putBoolean("guardianRelaxing", guardianRelaxing);
        tag.putLong("guardianNextPhaseTick", guardianNextPhaseTick);
        if (guardianTargetUuid != null) {
            tag.putUUID("guardianTargetUuid", guardianTargetUuid);
        }
        tag.putInt("guardianTargetStuckTicks", guardianTargetStuckTicks);
        tag.putDouble("guardianTargetBestDistanceSq", guardianTargetBestDistanceSq);
        tag.putInt("bodyguardRange", bodyguardRange);
        tag.putString("oreScentingOreId", oreScentingOreId == null ? "" : oreScentingOreId);
        tag.putBoolean("hasPetBed", hasPetBed);
        tag.putString("petBedDimension", petBedDimension == null ? "" : petBedDimension);
        tag.putInt("petBedX", petBedX);
        tag.putInt("petBedY", petBedY);
        tag.putInt("petBedZ", petBedZ);
        tag.putBoolean("hasCollarTag", hasCollarTag);
        tag.putInt("collarTagTier", Math.max(0, collarTagTier));
        tag.putDouble("baseSpeed", baseSpeed);
        tag.putDouble("bonusHealth", bonusHealth);
        tag.putDouble("bonusDamage", bonusDamage);
        tag.putDouble("bonusSpeed", bonusSpeed);
        tag.putDouble("bonusArmor", bonusArmor);
        tag.putDouble("bonusArmorToughness", bonusArmorToughness);
        tag.putDouble("bonusKnockback", bonusKnockback);
        tag.putDouble("bonusKnockbackResist", bonusKnockbackResist);
        if (tameClass != null) {
            tag.putString("tameClass", tameClass.name());
        }
        tag.putInt("classRerollsUsed", Math.max(0, classRerollsUsed));
        tag.putInt("mode", mode);
        tag.putInt("movementOrder", movementOrder);
        tag.putString("group", group == null ? "" : group);
        tag.putBoolean("defendAllies", defendAllies);
        tag.putBoolean("escapeMode", escapeMode);
        tag.putBoolean("escapeActive", escapeActive);
        tag.putBoolean("wanderLock", wanderLock);
        tag.putBoolean("skeletonMovement", skeletonMovement);
        tag.putBoolean("closeMovement", closeMovement);
        tag.putBoolean("hasProtectionZone", hasProtectionZone);
        tag.putString("protectionDimension", protectionDimension == null ? "" : protectionDimension);
        tag.putInt("protectionX", protectionX);
        tag.putInt("protectionY", protectionY);
        tag.putInt("protectionZ", protectionZ);
        tag.putInt("protectionRadius", protectionRadius);
        CompoundTag guardianSetsTag = new CompoundTag();
        guardianSetAnchors.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null && !value.isEmpty()) {
                guardianSetsTag.put(key, value.copy());
            }
        });
        tag.put("guardianSetAnchors", guardianSetsTag);

        ListTag abilityList = new ListTag();
        for (String ability : abilities) {
            abilityList.add(StringTag.valueOf(ability));
        }
        tag.put("abilities", abilityList);

        CompoundTag abilityLevelsTag = new CompoundTag();
        abilityLevels.forEach(abilityLevelsTag::putInt);
        tag.put("abilityLevels", abilityLevelsTag);

        CompoundTag attributeLevelsTag = new CompoundTag();
        attributeLevels.forEach(attributeLevelsTag::putInt);
        tag.put("attributeLevels", attributeLevelsTag);

        CompoundTag cooldownsTag = new CompoundTag();
        cooldowns.forEach(cooldownsTag::putLong);
        tag.put("cooldowns", cooldownsTag);
        tag.put("entitySnapshot", entitySnapshot == null ? new CompoundTag() : entitySnapshot.copy());
        tag.putInt("hungerSaturation", Math.max(0, hungerSaturation));
        tag.putBoolean("hungerEmptyNotified", hungerEmptyNotified);
        tag.putBoolean("hungerLowNotified", hungerLowNotified);
        tag.putBoolean("hungerLastFoodNotified", hungerLastFoodNotified);
        tag.putBoolean("hungerAutopickup", hungerAutopickup);
        ListTag hungerInventoryTag = new ListTag();
        for (ItemStack stack : hungerInventory) {
            if (stack != null && !stack.isEmpty()) {
                hungerInventoryTag.add(stack.save(new CompoundTag()));
            }
        }
        tag.put("hungerInventory", hungerInventoryTag);
        ListTag armorInventoryTag = new ListTag();
        for (int i = 0; i < 4; i++) {
            ItemStack stack = i < armorInventory.size() ? armorInventory.get(i) : ItemStack.EMPTY;
            armorInventoryTag.add(stack.save(new CompoundTag()));
        }
        tag.put("armorInventory", armorInventoryTag);

        tag.putBoolean("hasSavedProgress", hasSavedProgress);
        tag.putInt("savedProgressCost", savedProgressCost);
        tag.putInt("savedLevel", savedLevel);
        tag.putInt("savedXp", savedXp);
        tag.putInt("savedXpToNext", savedXpToNext);
        tag.putInt("savedKills", savedKills);
        tag.putInt("savedAssists", savedAssists);
        tag.putDouble("savedBonusHealth", savedBonusHealth);
        tag.putDouble("savedBonusDamage", savedBonusDamage);
        tag.putDouble("savedBonusSpeed", savedBonusSpeed);
        tag.putDouble("savedBonusArmor", savedBonusArmor);
        tag.putDouble("savedBonusArmorToughness", savedBonusArmorToughness);
        tag.putDouble("savedBonusKnockback", savedBonusKnockback);
        tag.putDouble("savedBonusKnockbackResist", savedBonusKnockbackResist);

        ListTag savedAbilityList = new ListTag();
        for (String ability : savedAbilities) {
            savedAbilityList.add(StringTag.valueOf(ability));
        }
        tag.put("savedAbilities", savedAbilityList);

        CompoundTag savedAbilityLevelsTag = new CompoundTag();
        savedAbilityLevels.forEach(savedAbilityLevelsTag::putInt);
        tag.put("savedAbilityLevels", savedAbilityLevelsTag);

        CompoundTag savedAttributeLevelsTag = new CompoundTag();
        savedAttributeLevels.forEach(savedAttributeLevelsTag::putInt);
        tag.put("savedAttributeLevels", savedAttributeLevelsTag);

        return tag;
    }

    public static TameData fromTag(CompoundTag tag) {
        TameData data = new TameData();
        data.liveOnly = tag.getBoolean("liveOnly");
        data.horseType = tag.getBoolean("horseType");
        data.riddenDistanceProgress = Math.max(0.0D, tag.getDouble("riddenDistanceProgress"));
        data.uuid = tag.hasUUID("uuid") ? tag.getUUID("uuid") : UUID.randomUUID();
        data.tlId = tag.hasUUID(TL_ID_TAG) ? tag.getUUID(TL_ID_TAG) : UUID.randomUUID();
        if (tag.hasUUID("ownerUUID")) {
            data.ownerUUID = tag.getUUID("ownerUUID");
        }
        data.name = tag.getString("name");
        data.type = tag.getString("type");
        data.level = Math.max(1, tag.getInt("level"));
        data.xp = Math.max(0, tag.getInt("xp"));
        data.xpToNext = Math.max(1, tag.getInt("xpToNext"));
        data.bornDayTime = tag.contains("bornDayTime") ? tag.getLong("bornDayTime") : -1L;
        data.activeSurvivalDays = Math.max(0, tag.getInt("activeSurvivalDays"));
        data.lastActiveSurvivalDay = tag.contains("lastActiveSurvivalDay", Tag.TAG_LONG) ? tag.getLong("lastActiveSurvivalDay") : Long.MIN_VALUE;
        data.kills = Math.max(0, tag.getInt("kills"));
        data.assists = Math.max(0, tag.getInt("assists"));
        data.deaths = Math.max(0, tag.getInt("deaths"));
        if (tag.contains("duelMmr", Tag.TAG_INT)) {
            data.duelMmr = tag.getInt("duelMmr");
        } else {
            data.duelMmr = PlayerDuelStats.DEFAULT_MMR;
        }
        data.duelKills = Math.max(0, tag.getInt("duelKills"));
        data.duelAssists = Math.max(0, tag.getInt("duelAssists"));
        data.duelDeaths = Math.max(0, tag.getInt("duelDeaths"));
        data.duelWins = Math.max(0, tag.getInt("duelWins"));
        data.duelLosses = Math.max(0, tag.getInt("duelLosses"));
        data.duelCount = Math.max(0, tag.getInt("duelCount"));
        data.duelPoints = Math.max(0.0D, tag.getDouble("duelPoints"));
        data.dead = tag.getBoolean("dead");
        data.stored = tag.getBoolean("stored");
        data.deadGameTime = tag.contains("deadGameTime", Tag.TAG_LONG) ? tag.getLong("deadGameTime") : 0L;
        data.deadUnixMillis = tag.contains("deadUnixMillis", Tag.TAG_LONG) ? tag.getLong("deadUnixMillis") : 0L;
        data.deathDimension = tag.contains("deathDimension", Tag.TAG_STRING) ? tag.getString("deathDimension") : "";
        data.deathX = tag.getInt("deathX");
        data.deathY = tag.getInt("deathY");
        data.deathZ = tag.getInt("deathZ");
        data.deathHistory.clear();
        if (tag.contains("deathHistory", Tag.TAG_LIST)) {
            ListTag list = tag.getList("deathHistory", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag row = list.getCompound(i);
                if (!row.isEmpty()) {
                    data.deathHistory.add(row.copy());
                }
            }
        }
        data.levelRewardHistory.clear();
        if (tag.contains("levelRewardHistory", Tag.TAG_LIST)) {
            ListTag list = tag.getList("levelRewardHistory", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag row = list.getCompound(i);
                if (!row.isEmpty()) {
                    data.levelRewardHistory.add(row.copy());
                }
            }
        }
        data.lastKnownDimension = tag.contains("lastKnownDimension", Tag.TAG_STRING) ? tag.getString("lastKnownDimension") : "";
        data.lastKnownX = tag.getInt("lastKnownX");
        data.lastKnownY = tag.getInt("lastKnownY");
        data.lastKnownZ = tag.getInt("lastKnownZ");
        data.lastKnownGameTime = tag.contains("lastKnownGameTime", Tag.TAG_LONG) ? tag.getLong("lastKnownGameTime") : 0L;
        data.hasHome = tag.getBoolean("hasHome");
        data.homeDimension = tag.contains("homeDimension", Tag.TAG_STRING) ? tag.getString("homeDimension") : "";
        data.homeX = tag.getInt("homeX");
        data.homeY = tag.getInt("homeY");
        data.homeZ = tag.getInt("homeZ");
        data.hasPreviousHome = tag.getBoolean("hasPreviousHome");
        data.previousHomeDimension = tag.contains("previousHomeDimension", Tag.TAG_STRING) ? tag.getString("previousHomeDimension") : "";
        data.previousHomeX = tag.getInt("previousHomeX");
        data.previousHomeY = tag.getInt("previousHomeY");
        data.previousHomeZ = tag.getInt("previousHomeZ");
        data.guardianReturnTicks = tag.getInt("guardianReturnTicks");
        data.guardianRelaxing = tag.getBoolean("guardianRelaxing");
        data.guardianNextPhaseTick = tag.contains("guardianNextPhaseTick", Tag.TAG_LONG) ? tag.getLong("guardianNextPhaseTick") : 0L;
        data.guardianTargetUuid = tag.hasUUID("guardianTargetUuid") ? tag.getUUID("guardianTargetUuid") : null;
        data.guardianTargetStuckTicks = Math.max(0, tag.getInt("guardianTargetStuckTicks"));
        data.guardianTargetBestDistanceSq = tag.contains("guardianTargetBestDistanceSq", Tag.TAG_DOUBLE) ? Math.max(0.0D, tag.getDouble("guardianTargetBestDistanceSq")) : 0.0D;
        data.bodyguardRange = tag.contains("bodyguardRange", Tag.TAG_INT) ? Math.max(1, tag.getInt("bodyguardRange")) : 12;
        data.oreScentingOreId = tag.contains("oreScentingOreId", Tag.TAG_STRING) ? tag.getString("oreScentingOreId") : "";
        data.hasPetBed = tag.getBoolean("hasPetBed");
        data.petBedDimension = tag.contains("petBedDimension", Tag.TAG_STRING) ? tag.getString("petBedDimension") : "";
        data.petBedX = tag.getInt("petBedX");
        data.petBedY = tag.getInt("petBedY");
        data.petBedZ = tag.getInt("petBedZ");
        data.hasCollarTag = tag.contains("hasCollarTag", Tag.TAG_BYTE) && tag.getBoolean("hasCollarTag");
        data.collarTagTier = tag.contains("collarTagTier", Tag.TAG_INT) ? Math.max(0, tag.getInt("collarTagTier")) : 0;
        data.baseSpeed = tag.getDouble("baseSpeed");
        data.bonusHealth = tag.getDouble("bonusHealth");
        data.bonusDamage = tag.getDouble("bonusDamage");
        data.bonusSpeed = tag.getDouble("bonusSpeed");
        data.bonusArmor = tag.getDouble("bonusArmor");
        data.bonusArmorToughness = tag.getDouble("bonusArmorToughness");
        data.bonusKnockback = tag.getDouble("bonusKnockback");
        data.bonusKnockbackResist = tag.getDouble("bonusKnockbackResist");
        data.mode = tag.getInt("mode");
        data.movementOrder = tag.contains("movementOrder", Tag.TAG_INT) ? tag.getInt("movementOrder") : 0;
        data.group = tag.getString("group");
        data.defendAllies = tag.getBoolean("defendAllies");
        data.escapeMode = !tag.contains("escapeMode") || tag.getBoolean("escapeMode");
        data.escapeActive = tag.getBoolean("escapeActive");
        data.wanderLock = tag.getBoolean("wanderLock");
        data.skeletonMovement = tag.getBoolean("skeletonMovement");
        data.closeMovement = tag.getBoolean("closeMovement");
        data.hasProtectionZone = tag.getBoolean("hasProtectionZone");
        data.protectionDimension = tag.getString("protectionDimension");
        data.protectionX = tag.getInt("protectionX");
        data.protectionY = tag.getInt("protectionY");
        data.protectionZ = tag.getInt("protectionZ");
        data.protectionRadius = tag.contains("protectionRadius") ? Math.max(4, tag.getInt("protectionRadius")) : 16;
        data.guardianSetAnchors.clear();
        if (tag.contains("guardianSetAnchors", Tag.TAG_COMPOUND)) {
            CompoundTag guardianSetsTag = tag.getCompound("guardianSetAnchors");
            for (String key : guardianSetsTag.getAllKeys()) {
                CompoundTag anchorTag = guardianSetsTag.getCompound(key);
                if (!anchorTag.isEmpty()) {
                    data.guardianSetAnchors.put(key, anchorTag.copy());
                }
            }
        }

        if (tag.contains("tameClass", Tag.TAG_STRING)) {
            data.tameClass = TameClass.ensureRegistered(tag.getString("tameClass"));
        }
        data.classRerollsUsed = Math.max(0, tag.getInt("classRerollsUsed"));

        if (tag.contains("abilities", Tag.TAG_LIST)) {
            ListTag abilitiesTag = tag.getList("abilities", Tag.TAG_STRING);
            for (Tag abilityTag : abilitiesTag) {
                data.abilities.add(abilityTag.getAsString());
            }
        }

        if (tag.contains("abilityLevels", Tag.TAG_COMPOUND)) {
            CompoundTag abilityLevelsTag = tag.getCompound("abilityLevels");
            for (String key : abilityLevelsTag.getAllKeys()) {
                data.abilityLevels.put(key, abilityLevelsTag.getInt(key));
                data.abilities.add(key);
            }
        }

        if (tag.contains("attributeLevels", Tag.TAG_COMPOUND)) {
            CompoundTag attributeLevelsTag = tag.getCompound("attributeLevels");
            for (String key : attributeLevelsTag.getAllKeys()) {
                data.attributeLevels.put(key, attributeLevelsTag.getInt(key));
            }
        }

        if (tag.contains("cooldowns", Tag.TAG_COMPOUND)) {
            CompoundTag cooldownsTag = tag.getCompound("cooldowns");
            for (String key : cooldownsTag.getAllKeys()) {
                data.cooldowns.put(key, cooldownsTag.getLong(key));
            }
        }
        if (tag.contains("entitySnapshot", Tag.TAG_COMPOUND)) {
            data.entitySnapshot = tag.getCompound("entitySnapshot").copy();
        } else {
            data.entitySnapshot = new CompoundTag();
        }
        data.hungerSaturation = tag.contains("hungerSaturation", Tag.TAG_INT) ? Math.max(0, tag.getInt("hungerSaturation")) : 1000;
        data.hungerEmptyNotified = tag.getBoolean("hungerEmptyNotified");
        data.hungerLowNotified = tag.getBoolean("hungerLowNotified");
        data.hungerLastFoodNotified = tag.getBoolean("hungerLastFoodNotified");
        data.hungerAutopickup = tag.getBoolean("hungerAutopickup");
        data.hungerInventory.clear();
        if (tag.contains("hungerInventory", Tag.TAG_LIST)) {
            ListTag hungerInventoryTag = tag.getList("hungerInventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < hungerInventoryTag.size() && data.hungerInventory.size() < 10; i++) {
                ItemStack stack = ItemStack.of(hungerInventoryTag.getCompound(i));
                if (!stack.isEmpty()) {
                    data.hungerInventory.add(stack);
                }
            }
        }
        data.armorInventory.clear();
        ListTag armorInventoryTag = tag.contains("armorInventory", Tag.TAG_LIST)
                ? tag.getList("armorInventory", Tag.TAG_COMPOUND) : new ListTag();
        for (int i = 0; i < 4; i++) {
            data.armorInventory.add(i < armorInventoryTag.size()
                    ? ItemStack.of(armorInventoryTag.getCompound(i)) : ItemStack.EMPTY);
        }

        data.hasSavedProgress = tag.getBoolean("hasSavedProgress");
        data.savedProgressCost = Math.max(0, tag.getInt("savedProgressCost"));
        data.savedLevel = Math.max(1, tag.getInt("savedLevel"));
        data.savedXp = Math.max(0, tag.getInt("savedXp"));
        data.savedXpToNext = Math.max(1, tag.getInt("savedXpToNext"));
        data.savedKills = Math.max(0, tag.getInt("savedKills"));
        data.savedAssists = Math.max(0, tag.getInt("savedAssists"));
        data.savedBonusHealth = tag.getDouble("savedBonusHealth");
        data.savedBonusDamage = tag.getDouble("savedBonusDamage");
        data.savedBonusSpeed = tag.getDouble("savedBonusSpeed");
        data.savedBonusArmor = tag.getDouble("savedBonusArmor");
        data.savedBonusArmorToughness = tag.getDouble("savedBonusArmorToughness");
        data.savedBonusKnockback = tag.getDouble("savedBonusKnockback");
        data.savedBonusKnockbackResist = tag.getDouble("savedBonusKnockbackResist");

        if (tag.contains("savedAbilities", Tag.TAG_LIST)) {
            ListTag savedAbilitiesTag = tag.getList("savedAbilities", Tag.TAG_STRING);
            for (Tag abilityTag : savedAbilitiesTag) {
                data.savedAbilities.add(abilityTag.getAsString());
            }
        }

        if (tag.contains("savedAbilityLevels", Tag.TAG_COMPOUND)) {
            CompoundTag savedAbilityLevelsTag = tag.getCompound("savedAbilityLevels");
            for (String key : savedAbilityLevelsTag.getAllKeys()) {
                data.savedAbilityLevels.put(key, savedAbilityLevelsTag.getInt(key));
                data.savedAbilities.add(key);
            }
        }

        if (tag.contains("savedAttributeLevels", Tag.TAG_COMPOUND)) {
            CompoundTag savedAttributeLevelsTag = tag.getCompound("savedAttributeLevels");
            for (String key : savedAttributeLevelsTag.getAllKeys()) {
                data.savedAttributeLevels.put(key, savedAttributeLevelsTag.getInt(key));
            }
        }

        return data;
    }

    public UUID ensureTlId() {
        if (tlId == null) {
            tlId = UUID.randomUUID();
        }
        return tlId;
    }

    public boolean isInactive() {
        return dead || stored;
    }

    public boolean isActuallyDead() {
        return dead;
    }

    public static UUID getTlId(LivingEntity tame) {
        if (tame == null) {
            return null;
        }
        CompoundTag data = tame.getPersistentData();
        return data.hasUUID(TL_ID_TAG) ? data.getUUID(TL_ID_TAG) : null;
    }

    public static UUID readOrCreateTlId(LivingEntity tame) {
        if (tame == null) {
            return UUID.randomUUID();
        }
        CompoundTag data = tame.getPersistentData();
        if (data.hasUUID(TL_ID_TAG)) {
            return data.getUUID(TL_ID_TAG);
        }
        UUID created = UUID.randomUUID();
        data.putUUID(TL_ID_TAG, created);
        return created;
    }

    public static void syncTlIdToEntity(LivingEntity tame, UUID tlId) {
        if (tame == null || tlId == null) {
            return;
        }
        tame.getPersistentData().putUUID(TL_ID_TAG, tlId);
    }

    private TameData() {
    }
}
