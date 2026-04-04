package com.github.alexthe668.domesticationinnovation.server.entity;

import com.github.alexthe666.citadel.Citadel;
import com.github.alexthe666.citadel.server.entity.CitadelEntityData;
import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe666.citadel.server.message.PropertiesMessage;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import com.github.alexthe668.domesticationinnovation.server.misc.DIParticleRegistry;
import com.github.alexthe668.domesticationinnovation.server.misc.DITameProgressData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class TameableUtils {

    private static final String ENCHANTMENT_TAG = "StoredPetEnchantments";
    private static final String FAKE_VISUAL_ENCHANT_TAG = "TLFakeVisualEnchant";
    private static final String COLLAR_TAG = "HasPetCollar";
    private static final String IMMUNITY_TIME_TAG = "PetImmunityTimer";
    private static final String IMMUNITY_COOLDOWN_TAG = "PetImmunityCooldown";
    private static final String FROZEN_TIME_TAG = "PetFrozenTime";
    private static final String FROZEN_LEVEL_TAG = "PetFrozenLevel";
    private static final String ATTACK_TARGET_ENTITY = "PetAttackTarget";
    private static final String SHADOW_PUNCH_TIMES = "PetShadowPunchTimes";
    private static final String SHADOW_PUNCH_COOLDOWN = "PetShadowPunchCooldown";
    private static final String PSYCHIC_WALL_COOLDOWN = "PetPsychicWallCooldown";
    private static final String INTIMIDATION_COOLDOWN = "PetIntimidationCooldown";
    private static final String SHADOW_PUNCH_STRIKING = "PetShadowPunchStriking";
    private static final String JUKEBOX_FOLLOWER_UUID = "PetJukeboxFollowerUUID";
    private static final String JUKEBOX_FOLLOWER_DISC = "PetJukeboxFollowerDisc";
    private static final String BLAZING_PROTECTION_BARS = "PetBlazingProtectionBars";
    private static final String BLAZING_PROTECTION_COOLDOWN = "PetBlazingProtectionCooldown";
    private static final String HEALING_AURA_TIME = "PetHealingAuraTime";
    private static final String HEALING_AURA_IMPULSE = "PetHealingAuraImpulse";
    private static final String HAS_PET_BED = "HasPetBed";
    private static final String PET_BED_X = "PetBedX";
    private static final String PET_BED_Y = "PetBedY";
    private static final String PET_BED_Z = "PetBedZ";
    private static final String PET_BED_DIMENSION = "PetBedDimension";
    private static final String FALL_DISTANCE_SYNC = "SyncedFallDistance";
    private static final String ZOMBIE_PET = "ZombiePet";
    private static final String SAFE_PET_HEALTH = "SafePetHealth";
    private static final String LEGACY_TL_MIGRATION_TAG = "TamesLevelMigrationData";
    private static final String COLLAR_SWAP_COOLDOWN = "CollarSwapCooldown";
    private static final String TL_ATTRIBUTE_LEVELS_SYNC = "TLAttributeLevelsSync";
    private static final String TL_ABILITY_LEVELS_SYNC = "TLAbilityLevelsSync";
    private static final UUID HEALTH_BOOST_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B166EEEEE");
    private static final UUID SPEED_BOOST_UUID = UUID.fromString("ff465ded-9040-4eb5-93a1-7bbe97c31744");
    private static final UUID COLLAR_ARMOR_UUID = UUID.fromString("e6e52fdd-8e14-4c0d-9ac1-8fbc60f3dd01");
    private static final UUID COLLAR_ARMOR_TOUGHNESS_UUID = UUID.fromString("f2f6c7ab-8a73-4d1c-95e4-07f171ddca8f");

    private static final UUID SPEED_BOOST_AQUATIC_LAND_UUID = UUID.fromString("ff465ded-9040-4eb5-93a1-7bbe97c31745");
    private static final Map<String, Enchantment> VISUAL_FAKE_ENCHANTMENTS;

    static {
        Map<String, Enchantment> visualFakeEnchants = new LinkedHashMap<>();
        visualFakeEnchants.put("magnetic", DIEnchantmentRegistry.MAGNETIC);
        visualFakeEnchants.put("immunity_frame", DIEnchantmentRegistry.IMMUNITY_FRAME);
        visualFakeEnchants.put("health_siphon", DIEnchantmentRegistry.HEALTH_SIPHON);
        visualFakeEnchants.put("healing_aura", DIEnchantmentRegistry.HEALING_AURA);
        visualFakeEnchants.put("void_cloud", DIEnchantmentRegistry.VOID_CLOUD);
        visualFakeEnchants.put("blazing_protection", DIEnchantmentRegistry.BLAZING_PROTECTION);
        visualFakeEnchants.put("shadow_hands", DIEnchantmentRegistry.SHADOW_HANDS);
        visualFakeEnchants.put("disc_jockey", DIEnchantmentRegistry.DISK_JOCKEY);
        VISUAL_FAKE_ENCHANTMENTS = Collections.unmodifiableMap(visualFakeEnchants);
    }

    public static boolean hasSameOwnerAs(LivingEntity tameable, Entity target) {
        return hasSameOwnerAsOneWay(tameable, target) || hasSameOwnerAsOneWay(target, tameable);
    }

    public static boolean shouldBlockFriendlyDiEffect(LivingEntity source, Entity target) {
        if (source == null || target == null) {
            return false;
        }
        if (target instanceof LivingEntity living && TameDuelManager.areDuelOpponents(source.getUUID(), living.getUUID())) {
            return false;
        }
        if (source.isAlliedTo(target)) {
            return true;
        }
        if (target instanceof Player) {
            return hasSameOwnerAs(source, target);
        }
        return target instanceof TamableAnimal tame && tame.isTame() && hasSameOwnerAs(source, target);
    }

    public static boolean shouldBlockOffensiveDiTarget(LivingEntity source, Entity target) {
        if (source == null || target == null) {
            return false;
        }
        if (shouldBlockFriendlyDiEffect(source, target)) {
            return true;
        }
        if (target instanceof LivingEntity living && TameDuelManager.areDuelOpponents(source.getUUID(), living.getUUID())) {
            return false;
        }
        if (target instanceof Player) {
            return true;
        }
        if (target instanceof TamableAnimal tame && tame.isTame()) {
            return true;
        }
        if (source instanceof TamableAnimal tameSource && tameSource.isTame()) {
            return TameRegistry.isProtectedAttackTarget(tameSource, target);
        }
        return false;
    }

    private static boolean hasSameOwnerAsOneWay(Entity tameable, Entity target) {
        if (tameable instanceof TamableAnimal tamed && tamed.getOwner() != null) {
            if (target instanceof ModifedToBeTameable axolotl && axolotl.getTameOwner() != null) {
                if (tamed.getOwner().equals(axolotl.getTameOwner())) {
                    return true;
                }
            }
            if (target instanceof TamableAnimal otherPet && otherPet.getOwner() != null) {
                if (tamed.getOwner().equals(otherPet.getOwner())) {
                    return true;
                }
            }
            return tamed.getOwner().equals(target);
        } else if (tameable instanceof ModifedToBeTameable axolotl && axolotl.getTameOwner() != null) {
            if (tameable instanceof TamableAnimal tamed && tamed.getOwner() != null) {
                if (tamed.getOwner().equals(axolotl.getTameOwner())) {
                    return true;
                }
            }
            if (target instanceof ModifedToBeTameable otherPet && otherPet.getTameOwner() != null) {
                if (axolotl.getTameOwner().equals(otherPet.getTameOwner())) {
                    return true;
                }
            }
            return axolotl.getTameOwner().equals(target);
        }
        return false;
    }

    public static boolean shouldUnloadToLantern(LivingEntity tameable) {
        if (DomesticationMod.CONFIG.trinaryCommandSystem.get() && tameable instanceof IComandableMob commandableMob) {
            return commandableMob.getCommand() == 2;
        } else {
            CompoundTag tag = new CompoundTag();
            tameable.addAdditionalSaveData(tag);
            int command = -1;
            //compat with alexs mobs
            for (String s : tag.getAllKeys()) {
                if (s.endsWith("Command") && tag.contains(s, 1)) {
                    command = tag.getInt(s);
                }
            }
            if (command != -1) {
                return command == 1;
            } else if (tameable instanceof TamableAnimal animal) {
                return !animal.isOrderedToSit();
            }
        }
        return false;
    }

    public static boolean isPetOf(Player player, Entity entity) {
        return entity != null && (entity.isAlliedTo(player) || hasSameOwnerAsOneWay(entity, player));
    }

    public static boolean isTamed(Entity entity) {
        //sometimes these are not bound on runtime
        if (entity instanceof Axolotl) {
            return ((ModifedToBeTameable) entity).isTame() && DomesticationMod.CONFIG.tameableAxolotl.get();
        }
        if (entity instanceof Fox) {
            return ((ModifedToBeTameable) entity).isTame() && DomesticationMod.CONFIG.tameableFox.get();
        }
        if (entity instanceof Rabbit) {
            return ((ModifedToBeTameable) entity).isTame() && DomesticationMod.CONFIG.tameableRabbit.get();
        }
        if (entity instanceof Frog) {
            return ((ModifedToBeTameable) entity).isTame() && DomesticationMod.CONFIG.tameableFrog.get();
        }
        return entity instanceof ModifedToBeTameable && ((ModifedToBeTameable) entity).isTame() || entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame();
    }

    public static boolean couldBeTamed(Entity entity) {
        return entity instanceof ModifedToBeTameable || entity instanceof TamableAnimal;
    }


    public static Entity getOwnerOf(Entity entity) {
        if (entity instanceof ModifedToBeTameable) {
            return ((ModifedToBeTameable) entity).getTameOwner();
        }
        if (entity instanceof TamableAnimal) {
            return ((TamableAnimal) entity).getOwner();
        }
        return null;
    }

    public static UUID getOwnerUUIDOf(Entity entity) {
        if (entity instanceof ModifedToBeTameable) {
            return ((ModifedToBeTameable) entity).getTameOwnerUUID();
        }
        if (entity instanceof TamableAnimal) {
            return ((TamableAnimal) entity).getOwnerUUID();
        }
        return null;
    }

    public static void setOwnerUUIDOf(Entity entity, UUID uuid) {
        if (entity instanceof ModifedToBeTameable) {
            ((ModifedToBeTameable) entity).setTameOwnerUUID(uuid);
        }
        if (entity instanceof TamableAnimal) {
            ((TamableAnimal) entity).setOwnerUUID(uuid);
        }
    }

    private static void setEnchantmentTag(LivingEntity enchanted, ListTag enchants) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        Map<ResourceLocation, Integer> prevEnchants = getEnchants(enchanted);
        tag.put(ENCHANTMENT_TAG, enchants);
        tag.putInt(COLLAR_SWAP_COOLDOWN, 20);
        tag.putBoolean(COLLAR_TAG, true);
        sync(enchanted, tag);
        onUpdateEnchants(prevEnchants, enchanted);
    }

    private static void onUpdateEnchants(@Nullable Map<ResourceLocation, Integer> prevEnchants, LivingEntity enchanted) {
        int healthExtra = getEnchantLevel(enchanted, DIEnchantmentRegistry.HEALTH_BOOST);
        int speedExtra = getEnchantLevel(enchanted, DIEnchantmentRegistry.SPEEDSTER);
        int protectionLevel = hasCollar(enchanted) ? getEnchantLevel(enchanted, net.minecraft.world.item.enchantment.Enchantments.ALL_DAMAGE_PROTECTION) : 0;
        float collarArmor = 0.0F;
        float collarArmorToughness = 0.0F;
        if (hasCollar(enchanted)) {
            float[] mapped = mapProtectionToArmorStats(protectionLevel);
            collarArmor = mapped[0];
            collarArmorToughness = mapped[1];
        }
        boolean amphib = hasEnchant(enchanted, DIEnchantmentRegistry.AMPHIBIOUS) && !enchanted.isInWaterOrBubble() && isWaterCreature(enchanted);
        AttributeInstance health = enchanted.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance speed = enchanted.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance armor = enchanted.getAttribute(Attributes.ARMOR);
        AttributeInstance armorToughness = enchanted.getAttribute(Attributes.ARMOR_TOUGHNESS);
        if (hasEnchant(enchanted, DIEnchantmentRegistry.IMMATURITY_CURSE) || prevEnchants != null && prevEnchants.keySet().contains(ForgeRegistries.ENCHANTMENTS.getKey(DIEnchantmentRegistry.IMMATURITY_CURSE))) {
            //change pose to update client
            enchanted.setPose(Pose.FALL_FLYING);
            enchanted.refreshDimensions();
        }
        if (health != null) {
            if (healthExtra > 0) {
                AttributeModifier attributemodifier = new AttributeModifier(HEALTH_BOOST_UUID, "health boost pet upgrade", healthExtra * 10, AttributeModifier.Operation.ADDITION);
                if (health.hasModifier(attributemodifier)) {
                    health.removeModifier(attributemodifier);
                    health.addPermanentModifier(attributemodifier);
                } else {
                    health.addPermanentModifier(attributemodifier);
                }
            } else {
                health.removePermanentModifier(HEALTH_BOOST_UUID);
            }
        }
        if (speed != null) {
            if (speedExtra > 0) {
                AttributeModifier attributemodifier = new AttributeModifier(SPEED_BOOST_UUID, "speedster pet upgrade", speedExtra * 0.075F, AttributeModifier.Operation.ADDITION);
                if (speed.hasModifier(attributemodifier)) {
                    speed.removeModifier(attributemodifier);
                    speed.addPermanentModifier(attributemodifier);
                } else {
                    speed.addPermanentModifier(attributemodifier);
                }
            } else {
                speed.removePermanentModifier(SPEED_BOOST_UUID);
            }
            if (amphib) {
                AttributeModifier attributemodifier = new AttributeModifier(SPEED_BOOST_AQUATIC_LAND_UUID, "amphibious pet upgrade", 0.13F, AttributeModifier.Operation.ADDITION);
                if (speed.hasModifier(attributemodifier)) {
                    speed.removeModifier(attributemodifier);
                    speed.addPermanentModifier(attributemodifier);
                } else {
                    speed.addPermanentModifier(attributemodifier);
                }
            } else {
                speed.removePermanentModifier(SPEED_BOOST_AQUATIC_LAND_UUID);
            }
        }
        if (armor != null) {
            if (collarArmor > 0.0F) {
                AttributeModifier armorModifier = new AttributeModifier(COLLAR_ARMOR_UUID, "collar tag armor bonus", collarArmor, AttributeModifier.Operation.ADDITION);
                if (armor.hasModifier(armorModifier)) {
                    armor.removeModifier(armorModifier);
                    armor.addPermanentModifier(armorModifier);
                } else {
                    armor.addPermanentModifier(armorModifier);
                }
            } else {
                armor.removePermanentModifier(COLLAR_ARMOR_UUID);
            }
        }
        if (armorToughness != null) {
            if (collarArmorToughness > 0.0F) {
                AttributeModifier toughnessModifier = new AttributeModifier(COLLAR_ARMOR_TOUGHNESS_UUID, "collar tag armor toughness bonus", collarArmorToughness, AttributeModifier.Operation.ADDITION);
                if (armorToughness.hasModifier(toughnessModifier)) {
                    armorToughness.removeModifier(toughnessModifier);
                    armorToughness.addPermanentModifier(toughnessModifier);
                } else {
                    armorToughness.addPermanentModifier(toughnessModifier);
                }
            } else {
                armorToughness.removePermanentModifier(COLLAR_ARMOR_TOUGHNESS_UUID);
            }
        }
    }

    private static float[] mapProtectionToArmorStats(int protectionLevel) {
        if (protectionLevel <= 0) {
            // Leather-equivalent baseline for wearing any collar tag.
            return new float[]{7.0F, 0.0F};
        }
        if (protectionLevel == 1) {
            // Chainmail-equivalent.
            return new float[]{12.0F, 0.0F};
        }
        if (protectionLevel == 2) {
            // Iron-equivalent.
            return new float[]{15.0F, 0.0F};
        }
        if (protectionLevel == 3) {
            // Diamond-equivalent.
            return new float[]{20.0F, 2.0F};
        }
        // Protection IV and above starts at netherite-equivalent and scales further.
        float extra = protectionLevel - 4.0F;
        return new float[]{20.0F + extra, 3.0F + (extra * 0.5F)};
    }

    private static boolean isWaterCreature(LivingEntity enchanted) {
        return enchanted.getType().getCategory() == MobCategory.WATER_CREATURE || enchanted.getType().getCategory() == MobCategory.UNDERGROUND_WATER_CREATURE || enchanted.getType().getCategory() == MobCategory.WATER_AMBIENT;
    }

    @Nullable
    private static ListTag getEnchantmentList(LivingEntity entity) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(entity);
        if (tag.contains(ENCHANTMENT_TAG)) {
            return tag.getList(ENCHANTMENT_TAG, 10);
        }
        return null;
    }

    public static int getEnchantLevel(LivingEntity entity, Enchantment enchantment) {
        ListTag listtag = getEnchantmentList(entity);
        if (listtag != null && DomesticationMod.CONFIG.isEnchantEnabled(enchantment)) {
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                if (shouldSkipServerSideVisualFakeEnchant(entity, enchantment, compoundtag)) {
                    continue;
                }
                ResourceLocation res = EnchantmentHelper.getEnchantmentId(compoundtag);
                if (res != null && res.equals(ForgeRegistries.ENCHANTMENTS.getKey(enchantment))) {
                    return EnchantmentHelper.getEnchantmentLevel(compoundtag);
                }
            }
        }
        return 0;
    }

    public static boolean hasEnchant(LivingEntity entity, Enchantment enchantment) {
        return getEnchantLevel(entity, enchantment) > 0;
    }

    @Nullable
    public static Map<ResourceLocation, Integer> getEnchants(LivingEntity entity) {
        ListTag listtag = getEnchantmentList(entity);
        if (listtag == null) {
            return null;
        }
        Map<ResourceLocation, Integer> enchants = new HashMap<>();
        for (int i = 0; i < listtag.size(); ++i) {
            CompoundTag compoundtag = listtag.getCompound(i);
            if (isVisualFakeEnchant(compoundtag)) {
                continue;
            }
            ResourceLocation res = EnchantmentHelper.getEnchantmentId(compoundtag);
            if (DomesticationMod.CONFIG.isEnchantEnabled(res.getPath())) {
                enchants.put(res, EnchantmentHelper.getEnchantmentLevel(compoundtag));
            }
        }
        return enchants;
    }

    public static List<Component> getEnchantDescriptions(LivingEntity entity) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal("   ").append(Component.translatable("message.domesticationinnovation.enchantments").withStyle(ChatFormatting.GOLD)));
        Map<ResourceLocation, Integer> map = getEnchants(entity);
        if (map != null) {
            for (Map.Entry<ResourceLocation, Integer> entry : map.entrySet()) {
                if (DomesticationMod.CONFIG.isEnchantEnabled(entry.getKey().getPath())) {
                    boolean isCurse = entry.getKey().getPath().contains("curse");
                    list.add(Component.translatable("enchantment." + entry.getKey().getNamespace() + "." + entry.getKey().getPath()).append(Component.literal(" ")).append(Component.translatable("enchantment.level." + entry.getValue())).withStyle(isCurse ? ChatFormatting.RED : ChatFormatting.AQUA));
                }
            }
        }
        return list;
    }

    public static boolean hasAnyEnchants(LivingEntity entity) {
        Map<ResourceLocation, Integer> enchants = getEnchants(entity);
        return enchants != null && !enchants.isEmpty();
    }

    public static boolean hasAnyAbilityOrAttributeProgress(LivingEntity entity) {
        if (entity == null) return false;
        TameData data = TameRegistry.get(entity.getUUID());
        if (data != null) {
            if (hasAnyPositiveLevels(data.attributeLevels)) return true;
            return hasAnyPositiveLevels(data.abilityLevels);
        }
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(entity);
        return hasAnyPositiveLevels(levelMapFromTag(tag.getCompound(TL_ATTRIBUTE_LEVELS_SYNC)))
                || hasAnyPositiveLevels(levelMapFromTag(tag.getCompound(TL_ABILITY_LEVELS_SYNC)));
    }

    public static List<Component> getAbilityAttributeDescriptions(LivingEntity entity) {
        List<Component> list = new ArrayList<>();
        if (entity == null) return list;
        TameData data = TameRegistry.get(entity.getUUID());

        Map<String, Integer> attributeLevels;
        Map<String, Integer> abilityLevels;
        if (data != null) {
            attributeLevels = data.attributeLevels == null ? Map.of() : data.attributeLevels;
            abilityLevels = data.abilityLevels == null ? Map.of() : data.abilityLevels;
        } else {
            CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(entity);
            attributeLevels = levelMapFromTag(tag.getCompound(TL_ATTRIBUTE_LEVELS_SYNC));
            abilityLevels = levelMapFromTag(tag.getCompound(TL_ABILITY_LEVELS_SYNC));
            if (attributeLevels.isEmpty() && abilityLevels.isEmpty()) {
                return list;
            }
        }

        list.add(Component.literal("   ").append(Component.literal("Attributes").withStyle(ChatFormatting.GOLD)));
        List<String> attrs = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : attributeLevels.entrySet()) {
            if (entry.getValue() <= 0) continue;
            attrs.add(entry.getKey() + " " + entry.getValue());
        }
        attrs.sort(String::compareToIgnoreCase);
        if (attrs.isEmpty()) {
            list.add(Component.literal("- none").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (String attr : attrs) {
                list.add(Component.literal("- ").append(Component.literal(attr).withStyle(ChatFormatting.AQUA)));
            }
        }

        list.add(Component.literal("   ").append(Component.literal("Abilities").withStyle(ChatFormatting.GOLD)));
        List<String> abilities = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : abilityLevels.entrySet()) {
            if (entry.getValue() <= 0) continue;
            abilities.add(entry.getKey() + " " + entry.getValue());
        }
        abilities.sort(String::compareToIgnoreCase);
        if (abilities.isEmpty()) {
            list.add(Component.literal("- none").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (String ability : abilities) {
                list.add(Component.literal("- ").append(Component.literal(ability).withStyle(ChatFormatting.LIGHT_PURPLE)));
            }
        }

        // Keep output compact if some tames roll very large sets.
        int maxLines = 20;
        if (list.size() > maxLines) {
            list = new ArrayList<>(list.subList(0, maxLines));
            list.add(Component.literal("...").withStyle(ChatFormatting.GRAY));
        }
        return list;
    }

    public static void syncAbilityAttributeProgressPreview(LivingEntity entity, TameData data) {
        if (entity == null || data == null || entity.level().isClientSide) {
            return;
        }
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(entity);
        CompoundTag attr = levelTagFromMap(data.attributeLevels);
        CompoundTag abil = levelTagFromMap(data.abilityLevels);
        CompoundTag prevAttr = tag.contains(TL_ATTRIBUTE_LEVELS_SYNC, Tag.TAG_COMPOUND) ? tag.getCompound(TL_ATTRIBUTE_LEVELS_SYNC) : new CompoundTag();
        CompoundTag prevAbil = tag.contains(TL_ABILITY_LEVELS_SYNC, Tag.TAG_COMPOUND) ? tag.getCompound(TL_ABILITY_LEVELS_SYNC) : new CompoundTag();
        if (attr.equals(prevAttr) && abil.equals(prevAbil)) {
            return;
        }
        tag.put(TL_ATTRIBUTE_LEVELS_SYNC, attr);
        tag.put(TL_ABILITY_LEVELS_SYNC, abil);
        sync(entity, tag);
        syncVisualCollarEnchants(entity);
    }

    public static void addEnchant(LivingEntity entity, EnchantmentInstance enchantment) {
        ListTag listtag = getEnchantmentList(entity);
        addEnchant(entity, enchantment, listtag);
    }

    public static void addEnchant(LivingEntity entity, EnchantmentInstance enchantment, ListTag listtag) {
        if (listtag != null && DomesticationMod.CONFIG.isEnchantEnabled(enchantment.enchantment)) {
            ResourceLocation resourcelocation = EnchantmentHelper.getEnchantmentId(enchantment.enchantment);
            boolean flag = true;
            for (int i = 0; i < listtag.size(); ++i) {
                CompoundTag compoundtag = listtag.getCompound(i);
                ResourceLocation resourcelocation1 = EnchantmentHelper.getEnchantmentId(compoundtag);
                if (resourcelocation1 != null && resourcelocation1.equals(resourcelocation)) {
                    if (EnchantmentHelper.getEnchantmentLevel(compoundtag) < enchantment.level) {
                        EnchantmentHelper.setEnchantmentLevel(compoundtag, enchantment.level);
                    }

                    flag = false;
                    break;
                }
            }
            if (flag) {
                listtag.add(EnchantmentHelper.storeEnchantment(resourcelocation, enchantment.level));
            }
            setEnchantmentTag(entity, listtag);
        }
    }

    public static void clearEnchants(LivingEntity entity) {
        setEnchantmentTag(entity, new ListTag());
        clearShadowHandState(entity);
    }

    public static void setHasCollar(LivingEntity enchanted, boolean collar) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        Map<ResourceLocation, Integer> prevEnchants = getEnchants(enchanted);
        tag.putBoolean(COLLAR_TAG, collar);
        sync(enchanted, tag);
        onUpdateEnchants(prevEnchants, enchanted);
        syncVisualCollarEnchants(enchanted);
    }

    public static boolean hasCollar(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.contains(COLLAR_TAG) && tag.getBoolean(COLLAR_TAG);
    }

    public static void syncVisualCollarEnchants(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide) {
            return;
        }
        ListTag current = getEnchantmentList(entity);
        ListTag updated = new ListTag();
        if (current != null) {
            for (int i = 0; i < current.size(); ++i) {
                CompoundTag enchantTag = current.getCompound(i).copy();
                if (!isVisualFakeEnchant(enchantTag)) {
                    updated.add(enchantTag);
                }
            }
        }
        if (hasCollar(entity)) {
            for (Map.Entry<String, Enchantment> entry : VISUAL_FAKE_ENCHANTMENTS.entrySet()) {
                int level = getVisualFakeEnchantLevel(entity, entry.getKey(), entry.getValue());
                if (level > 0) {
                    CompoundTag stored = EnchantmentHelper.storeEnchantment(ForgeRegistries.ENCHANTMENTS.getKey(entry.getValue()), level);
                    stored.putBoolean(FAKE_VISUAL_ENCHANT_TAG, true);
                    updated.add(stored);
                }
            }
        }
        if (!listTagEquals(current, updated)) {
            setEnchantmentTag(entity, updated);
        }
    }

    private static int getVisualFakeEnchantLevel(LivingEntity entity, String effectId, Enchantment enchantment) {
        if ("health_siphon".equals(effectId)) {
            UUID ownerUuid = getOwnerUUIDOf(entity);
            if (ownerUuid != null && !TameRegistry.isHealthSiphonEnabled(ownerUuid)) {
                return 0;
            }
        }
        int level = getProgressOnlyLevel(entity, effectId);
        if (level <= 0) {
            return 0;
        }
        return Mth.clamp(level, 1, enchantment.getMaxLevel());
    }

    private static int getProgressOnlyLevel(LivingEntity entity, String id) {
        TameData data = TameRegistry.get(entity.getUUID());
        if (data != null) {
            int ability = data.abilityLevels == null ? 0 : data.abilityLevels.getOrDefault(id, 0);
            int attribute = data.attributeLevels == null ? 0 : data.attributeLevels.getOrDefault(id, 0);
            return Math.max(ability, attribute);
        }
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(entity);
        return Math.max(levelFromSyncTag(tag, TL_ABILITY_LEVELS_SYNC, id), levelFromSyncTag(tag, TL_ATTRIBUTE_LEVELS_SYNC, id));
    }

    private static int levelFromSyncTag(CompoundTag tag, String key, String id) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return 0;
        }
        CompoundTag levels = tag.getCompound(key);
        return levels.contains(id, Tag.TAG_INT) ? levels.getInt(id) : 0;
    }

    private static boolean listTagEquals(@Nullable ListTag first, ListTag second) {
        if (first == null) {
            return second.isEmpty();
        }
        return first.equals(second);
    }

    private static boolean isVisualFakeEnchant(CompoundTag compoundTag) {
        return compoundTag.getBoolean(FAKE_VISUAL_ENCHANT_TAG);
    }

    private static boolean isServerSideVisualFakeEnchant(LivingEntity entity, CompoundTag compoundTag) {
        return !entity.level().isClientSide && isVisualFakeEnchant(compoundTag);
    }

    private static boolean shouldSkipServerSideVisualFakeEnchant(LivingEntity entity, Enchantment enchantment, CompoundTag compoundTag) {
        if (!isServerSideVisualFakeEnchant(entity, compoundTag)) {
            return false;
        }
        return enchantment != DIEnchantmentRegistry.MAGNETIC;
    }

    @Nullable
    public static CompoundTag getDIProgressData(LivingEntity enchanted) {
        DITameProgressData table = DITameProgressData.get(enchanted.level());
        if (table != null) {
            CompoundTag payload = table.getPayload(enchanted.getUUID());
            if (payload != null && !payload.isEmpty()) {
                return payload;
            }
        }
        return null;
    }

    public static boolean hasDIProgressData(LivingEntity enchanted) {
        return getDIProgressData(enchanted) != null;
    }

    public static void setDIProgressData(LivingEntity enchanted, CompoundTag payload) {
        DITameProgressData table = DITameProgressData.get(enchanted.level());
        if (table != null) {
            if (payload == null || payload.isEmpty()) {
                table.remove(enchanted.getUUID());
            } else {
                table.upsert(enchanted.getUUID(), payload);
            }
        }
        // Canonical DI progress lives in the registry table only.
        // Clear legacy migration payload on any canonical write.
        CompoundTag data = enchanted.getPersistentData();
        data.remove(LEGACY_TL_MIGRATION_TAG);
    }

    @Nullable
    public static CompoundTag getTamesLevelMigrationData(LivingEntity enchanted) {
        CompoundTag data = enchanted.getPersistentData();
        if (data.contains(LEGACY_TL_MIGRATION_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag payload = data.getCompound(LEGACY_TL_MIGRATION_TAG);
            return payload.isEmpty() ? null : payload.copy();
        }
        return null;
    }

    public static boolean hasTamesLevelMigrationData(LivingEntity enchanted) {
        return getTamesLevelMigrationData(enchanted) != null;
    }

    public static void setTamesLevelMigrationData(LivingEntity enchanted, CompoundTag payload) {
        CompoundTag data = enchanted.getPersistentData();
        if (payload == null || payload.isEmpty()) {
            data.remove(LEGACY_TL_MIGRATION_TAG);
            return;
        }
        data.put(LEGACY_TL_MIGRATION_TAG, payload.copy());
    }

    public static int getImmuneTime(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(IMMUNITY_TIME_TAG);
    }

    public static void setImmuneTime(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(IMMUNITY_TIME_TAG, time);
        sync(enchanted, tag);
    }

    public static int getImmuneCooldown(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(IMMUNITY_COOLDOWN_TAG);
    }

    public static void setImmuneCooldown(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(IMMUNITY_COOLDOWN_TAG, time);
        sync(enchanted, tag);
    }

    public static int getFrozenTime(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(FROZEN_TIME_TAG);
    }

    public static void setFrozenTimeTag(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(FROZEN_TIME_TAG, time);
        sync(enchanted, tag);
    }

    public static int getFrozenLevel(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return Math.max(0, tag.getInt(FROZEN_LEVEL_TAG));
    }

    public static void setFrozenLevel(LivingEntity enchanted, int level) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(FROZEN_LEVEL_TAG, Math.max(0, level));
        sync(enchanted, tag);
    }

    public static int getPetAttackTargetID(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return !tag.contains(ATTACK_TARGET_ENTITY) ? -1 : tag.getInt(ATTACK_TARGET_ENTITY);
    }

    @Nullable
    public static Entity getPetAttackTarget(LivingEntity enchanted) {
        int i = getPetAttackTargetID(enchanted);
        return i == -1 ? null : enchanted.level().getEntity(i);
    }

    public static void setPetAttackTarget(LivingEntity enchanted, int id) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(ATTACK_TARGET_ENTITY, id);
        sync(enchanted, tag);
    }

    public static int getShadowPunchCooldown(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(SHADOW_PUNCH_COOLDOWN);
    }

    public static void setShadowPunchCooldown(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(SHADOW_PUNCH_COOLDOWN, time);
        sync(enchanted, tag);
    }

    public static int[] getShadowPunchTimes(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getIntArray(SHADOW_PUNCH_TIMES);
    }

    public static void setShadowPunchTimes(LivingEntity enchanted, int[] times) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putIntArray(SHADOW_PUNCH_TIMES, times);
        sync(enchanted, tag);
    }

    public static void setShadowPunchStriking(LivingEntity enchanted, int[] times) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putIntArray(SHADOW_PUNCH_STRIKING, times);
        sync(enchanted, tag);
    }

    public static int[] getShadowPunchStriking(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getIntArray(SHADOW_PUNCH_STRIKING);
    }

    public static void clearShadowHandState(LivingEntity enchanted) {
        if (enchanted == null) {
            return;
        }
        setShadowPunchCooldown(enchanted, 0);
        setShadowPunchTimes(enchanted, new int[0]);
        setShadowPunchStriking(enchanted, new int[0]);
        setPetAttackTarget(enchanted, -1);
    }

    public static void setPetJukeboxUUID(LivingEntity enchanted, UUID id) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putUUID(JUKEBOX_FOLLOWER_UUID, id);
        sync(enchanted, tag);
    }

    public static UUID getPetJukeboxUUID(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.contains(JUKEBOX_FOLLOWER_UUID) ? tag.getUUID(JUKEBOX_FOLLOWER_UUID) : null;
    }

    public static void setPetJukeboxDisc(LivingEntity enchanted, ItemStack stack) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.put(JUKEBOX_FOLLOWER_DISC, stack.save(new CompoundTag()));
        sync(enchanted, tag);
    }

    public static ItemStack getPetJukeboxDisc(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.contains(JUKEBOX_FOLLOWER_DISC) ? ItemStack.of(tag.getCompound(JUKEBOX_FOLLOWER_DISC)) : ItemStack.EMPTY;
    }

    public static int getPsychicWallCooldown(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(PSYCHIC_WALL_COOLDOWN);
    }

    public static void setPsychicWallCooldown(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(PSYCHIC_WALL_COOLDOWN, time);
        sync(enchanted, tag);
    }

    public static int getIntimidationCooldown(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(INTIMIDATION_COOLDOWN);
    }

    public static void setIntimidationCooldown(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(INTIMIDATION_COOLDOWN, time);
        sync(enchanted, tag);
    }

    public static int getBlazingProtectionCooldown(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(BLAZING_PROTECTION_COOLDOWN);
    }

    public static void setBlazingProtectionCooldown(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(BLAZING_PROTECTION_COOLDOWN, time);
        sync(enchanted, tag);
    }

    public static int getHealingAuraTime(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(HEALING_AURA_TIME);
    }

    public static void setHealingAuraTime(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(HEALING_AURA_TIME, time);
        sync(enchanted, tag);
    }


    public static boolean getHealingAuraImpulse(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getBoolean(HEALING_AURA_IMPULSE);
    }

    public static void setHealingAuraImpulse(LivingEntity enchanted, boolean impulse) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putBoolean(HEALING_AURA_IMPULSE, impulse);
        sync(enchanted, tag);
    }

    public static int getBlazingProtectionBars(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getInt(BLAZING_PROTECTION_BARS);
    }

    public static void setBlazingProtectionBars(LivingEntity enchanted, int time) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putInt(BLAZING_PROTECTION_BARS, time);
        sync(enchanted, tag);
    }

    private static void sync(LivingEntity enchanted, CompoundTag tag) {
        CitadelEntityData.setCitadelTag(enchanted, tag);
        if (!enchanted.level().isClientSide) {
            Citadel.sendMSGToAll(new PropertiesMessage("CitadelTagUpdate", tag, enchanted.getId()));
        } else {
            Citadel.sendMSGToServer(new PropertiesMessage("CitadelTagUpdate", tag, enchanted.getId()));
        }
    }

    private static boolean hasAnyPositiveLevels(Map<String, Integer> levels) {
        if (levels == null || levels.isEmpty()) return false;
        for (int value : levels.values()) {
            if (value > 0) return true;
        }
        return false;
    }

    private static CompoundTag levelTagFromMap(Map<String, Integer> levels) {
        CompoundTag out = new CompoundTag();
        if (levels == null || levels.isEmpty()) {
            return out;
        }
        for (Map.Entry<String, Integer> entry : levels.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            if (entry.getValue() <= 0) continue;
            out.putInt(entry.getKey(), entry.getValue());
        }
        return out;
    }

    private static Map<String, Integer> levelMapFromTag(CompoundTag tag) {
        Map<String, Integer> out = new HashMap<>();
        if (tag == null || tag.isEmpty()) {
            return out;
        }
        for (String key : tag.getAllKeys()) {
            int value = Math.max(0, tag.getInt(key));
            if (value > 0) {
                out.put(key, value);
            }
        }
        return out;
    }

    @Nullable
    public static BlockPos getPetBedPos(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        if (tag.getBoolean(HAS_PET_BED) && tag.contains(PET_BED_X) && tag.contains(PET_BED_Y) && tag.contains(PET_BED_Z)) {
            return new BlockPos(tag.getInt(PET_BED_X), tag.getInt(PET_BED_Y), tag.getInt(PET_BED_Z));
        }
        return null;
    }

    public static void setPetBedPos(LivingEntity enchanted, BlockPos petBed) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putBoolean(HAS_PET_BED, true);
        tag.putInt(PET_BED_X, petBed.getX());
        tag.putInt(PET_BED_Y, petBed.getY());
        tag.putInt(PET_BED_Z, petBed.getZ());
        sync(enchanted, tag);
    }

    public static void removePetBedPos(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putBoolean(HAS_PET_BED, false);
        sync(enchanted, tag);
    }

    public static String getPetBedDimension(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return !tag.contains(PET_BED_DIMENSION) ? "minecraft:overworld" : tag.getString(PET_BED_DIMENSION);
    }

    public static void setPetBedDimension(LivingEntity enchanted, String dimension) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putString(PET_BED_DIMENSION, dimension);
        sync(enchanted, tag);
    }

    public static double getSafePetHealth(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getDouble(SAFE_PET_HEALTH);
    }

    public static void setSafePetHealth(LivingEntity enchanted, double health) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putDouble(SAFE_PET_HEALTH, health);
        sync(enchanted, tag);
    }

    public static void normalizeLegacyHealthBoostState(LivingEntity enchanted) {
        if (enchanted == null) {
            return;
        }
        AttributeInstance health = enchanted.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && !hasEnchant(enchanted, DIEnchantmentRegistry.HEALTH_BOOST)) {
            health.removePermanentModifier(HEALTH_BOOST_UUID);
        }
        double clampedHealth = Mth.clamp(enchanted.getHealth(), 0.0D, enchanted.getMaxHealth());
        setSafePetHealth(enchanted, clampedHealth);
    }

    public static void attractAnimals(LivingEntity attractor, int max) {
        if ((attractor.tickCount + attractor.getId()) % 8 == 0) {
            Predicate<Entity> notOnTeam = (animal) ->
                    !hasSameOwnerAs((LivingEntity) animal, attractor)
                            && animal.distanceTo(attractor) > 3 + attractor.getBbWidth() * 1.6F
                            && (TLAdminRuntimeSettings.herdingAffectsTames() || !isTamed(animal));
            List<Animal> list = attractor.level().getEntitiesOfClass(Animal.class, attractor.getBoundingBox().inflate(16, 8, 16), EntitySelector.NO_SPECTATORS.and(notOnTeam));
            list.sort(Comparator.comparingDouble(attractor::distanceToSqr));
            for (int i = 0; i < Math.min(max, list.size()); i++) {
                Animal e = list.get(i);
                e.setTarget(null);
                e.setLastHurtByMob(null);
                e.getNavigation().moveTo(attractor, 1.1D);
            }

        }
    }

    public static void aggroRandomMonsters(LivingEntity attractor) {
        if ((attractor.tickCount + attractor.getId()) % 400 == 0) {
            Predicate<Entity> notOnTeamAndMonster = (animal) -> animal instanceof Monster && !hasSameOwnerAs((LivingEntity) animal, attractor) && animal.distanceTo(attractor) > 3 + attractor.getBbWidth() * 1.6F;
            List<Mob> list = attractor.level().getEntitiesOfClass(Mob.class, attractor.getBoundingBox().inflate(20, 8, 20), EntitySelector.NO_SPECTATORS.and(notOnTeamAndMonster));
            list.sort(Comparator.comparingDouble(attractor::distanceToSqr));
            if (!list.isEmpty()) {
                list.get(0).setTarget(attractor);
            }
        }
    }

    public static void scareRandomMonsters(LivingEntity scary, int level) {
        int cooldown = getIntimidationCooldown(scary);
        if (cooldown > 0) {
            setIntimidationCooldown(scary, cooldown - 1);
            return;
        }
        if (!(scary instanceof Mob mob) || mob.getTarget() == null || !mob.getTarget().isAlive()) {
            return;
        }
        Predicate<Entity> notOnTeamAndMonster = (animal) -> animal instanceof Monster && !hasSameOwnerAs((LivingEntity) animal, scary) && animal.distanceTo(scary) > 3 + scary.getBbWidth() * 1.6F;
        List<PathfinderMob> list = scary.level().getEntitiesOfClass(PathfinderMob.class, scary.getBoundingBox().inflate(10 * level, 8 * level, 10 * level), EntitySelector.NO_SPECTATORS.and(notOnTeamAndMonster));
        list.sort(Comparator.comparingDouble(scary::distanceToSqr));
        if (!list.isEmpty()) {
            List<PathfinderMob> affected = list.stream().filter(monster -> scary.getRandom().nextDouble() < intimidationProcChance(level, monster)).toList();
            if (affected.isEmpty()) {
                return;
            }
            Vec3 rots = affected.get(0).getEyePosition().subtract(scary.getEyePosition()).normalize();
            float f = Mth.sqrt((float) (rots.x * rots.x + rots.z * rots.z));
            double yRot = Math.atan2(-rots.z, -rots.x) * (double) (180F / (float) Math.PI) + 90F;
            double xRot = Math.atan2(-rots.y, f) * (double) (180F / (float) Math.PI);
            scary.level().addParticle(DIParticleRegistry.INTIMIDATION.get(), scary.getX(), scary.getY(), scary.getZ(), scary.getId(), xRot, yRot);
            setIntimidationCooldown(scary, 1200);
            mob.playAmbientSound();
            for (PathfinderMob monster : affected) {
                Vec3 vec = LandRandomPos.getPosAway(monster, 11 * level, 7, scary.position());
                if (vec != null) {
                    monster.getNavigation().moveTo(vec.x, vec.y, vec.z, 1.5D);
                }
            }
        }
    }

    private static double intimidationProcChance(int level, LivingEntity target) {
        double baseChance = Math.min(0.80D, 0.22D + Math.max(0, level - 1) * 0.05D);
        return scaleMinorEnemyProcChance(baseChance, level, target);
    }

    public static double scaleMinorEnemyProcChance(double baseChance, int level, LivingEntity target) {
        double maxHealth = Math.max(1.0D, target.getMaxHealth());
        if (maxHealth <= 20.0D) {
            return baseChance;
        }
        double capAt100 = minorEnemyProcCapAt100(level);
        double allowedChance;
        if (maxHealth <= 100.0D) {
            double progress = (maxHealth - 20.0D) / 80.0D;
            allowedChance = Mth.lerp(progress, baseChance, capAt100);
        } else {
            double over100Scale = Math.pow(100.0D / maxHealth, 3.0D);
            allowedChance = capAt100 * over100Scale;
        }
        return Math.min(baseChance, allowedChance);
    }

    private static double minorEnemyProcCapAt100(int level) {
        int safeLevel = Math.max(1, level);
        if (safeLevel <= 5) {
            return 0.007D + (safeLevel - 1) * 0.00075D;
        }
        return Math.min(0.05D, 0.010D + (safeLevel - 5) * 0.008D);
    }

    public static void detectRandomOres(LivingEntity attractor, int interval, int range, int effectLength, int maxOres) {
        int tick = (attractor.tickCount + attractor.getId()) % interval;
        if (tick <= 30) {
            attractor.xRotO = attractor.getXRot();
            attractor.setXRot((float) Math.sin(tick * 0.6F) * 30F);
            Vec3 look = attractor.getEyePosition().add(attractor.getViewVector(1.0F).scale(attractor.getBbWidth()));
            for (int i = 0; i < 3; i++) {
                double x = attractor.getRandomX(2.0F);
                double y = attractor.position().y;
                double z = attractor.getRandomZ(2.0F);
                attractor.level().addParticle(DIParticleRegistry.SNIFF.get(), x, y, z, look.x, look.y, look.z);
            }
        }
        if (tick == 30) {
            List<BlockPos> ores = new ArrayList<>();
            BlockPos blockpos = attractor.blockPosition();
            int half = range / 2;
            for (int i = 0; i <= half && i >= -half; i = (i <= 0 ? 1 : 0) - i) {
                for (int j = 0; j <= range && j >= -range; j = (j <= 0 ? 1 : 0) - j) {
                    for (int k = 0; k <= range && k >= -range; k = (k <= 0 ? 1 : 0) - k) {
                        BlockPos offset = blockpos.offset(j, i, k);
                        BlockState state = attractor.level().getBlockState(offset);
                        if (state.is(Tags.Blocks.ORES)) {
                            if (ores.size() < maxOres) {
                                ores.add(offset);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            for (BlockPos ore : ores) {
                HighlightedBlockEntity highlight = DIEntityRegistry.HIGHLIGHTED_BLOCK.get().create(attractor.level());
                highlight.setPos(Vec3.atBottomCenterOf(ore));
                highlight.setLifespan(effectLength);
                highlight.setXRot(0);
                highlight.setYRot(0);
                attractor.level().addFreshEntity(highlight);
            }
        }
    }

    public static void destroyRandomPlants(LivingEntity living) {
        if ((living.tickCount + living.getId()) % 200 == 0) {
            int range = 2;
            List<BlockPos> plants = new ArrayList<>();
            List<BlockPos> grasses = new ArrayList<>();
            BlockPos blockpos = living.blockPosition();
            int half = range / 2;
            RandomSource r = living.getRandom();
            int maxPlants = 2 + r.nextInt(2);
            for (int i = 0; i <= half && i >= -half; i = (i <= 0 ? 1 : 0) - i) {
                for (int j = 0; j <= range && j >= -range; j = (j <= 0 ? 1 : 0) - j) {
                    for (int k = 0; k <= range && k >= -range; k = (k <= 0 ? 1 : 0) - k) {
                        BlockPos offset = blockpos.offset(j, i, k);
                        BlockState state = living.level().getBlockState(offset);
                        if (!state.isAir() && r.nextInt(4) == 0) {
                            if (state.is(BlockTags.FLOWERS) || state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.CROPS)) {
                                plants.add(offset);
                            } else if (state.is(BlockTags.DIRT) && !state.is(Blocks.DIRT) && !state.is(Blocks.COARSE_DIRT) || state.is(Blocks.FARMLAND)) {
                                grasses.add(offset);
                            }
                        }
                    }
                }
            }
            for (BlockPos plant : plants) {
                living.level().setBlockAndUpdate(plant, Blocks.AIR.defaultBlockState());
                for (int i = 0; i < 1 + r.nextInt(2); i++) {
                    living.level().addParticle(DIParticleRegistry.BLIGHT.get(), plant.getX() + r.nextFloat(), plant.getY() + r.nextFloat(), plant.getZ() + r.nextFloat(), 0, 0.08F, 0);
                }
            }
            for (BlockPos dirt : grasses) {
                living.level().setBlockAndUpdate(dirt, r.nextBoolean() ? Blocks.COARSE_DIRT.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                for (int i = 0; i < 1 + r.nextInt(2); i++) {
                    living.level().addParticle(DIParticleRegistry.BLIGHT.get(), dirt.getX() + r.nextFloat(), dirt.getY() + 1, dirt.getZ() + r.nextFloat(), 0, 0.08F, 0);
                }
            }
        }
    }

    public static List<LivingEntity> getAuraHealables(LivingEntity pet) {
        if (pet == null || TameDuelManager.isEntityInDuel(pet.getUUID())) {
            return List.of();
        }
        Predicate<Entity> hurtAndOnTeam = (animal) ->
                hasSameOwnerAs((LivingEntity) animal, pet)
                        && animal.distanceTo(pet) < 4
                        && ((LivingEntity) animal).getHealth() < ((LivingEntity) animal).getMaxHealth()
                        && TameDuelManager.canProvideSupport(pet.getUUID(), animal.getUUID());
        return pet.level().getEntitiesOfClass(LivingEntity.class, pet.getBoundingBox().inflate(4, 4, 4), EntitySelector.NO_SPECTATORS.and(hurtAndOnTeam));
    }

    public static List<LivingEntity> getNearbyHealers(LivingEntity hurtOwner) {
        if (hurtOwner == null || TameDuelManager.isEntityInDuel(hurtOwner.getUUID())) {
            return List.of();
        }
        Predicate<Entity> healer = (animal) ->
                hasSameOwnerAs((LivingEntity) animal, hurtOwner)
                        && hasEnchant((LivingEntity) animal, DIEnchantmentRegistry.HEALING_AURA)
                        && getHealingAuraTime((LivingEntity) animal) == 0
                        && TameDuelManager.canProvideSupport(animal.getUUID(), hurtOwner.getUUID());
        return hurtOwner.level().getEntitiesOfClass(LivingEntity.class, hurtOwner.getBoundingBox().inflate(16, 4, 16), EntitySelector.NO_SPECTATORS.and(healer));
    }

    public static float getFallDistance(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getFloat(FALL_DISTANCE_SYNC);
    }

    public static void setFallDistance(LivingEntity enchanted, float dist) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putFloat(FALL_DISTANCE_SYNC, dist);
        sync(enchanted, tag);

    }


    public static void setZombiePet(LivingEntity enchanted, boolean zombiefied) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        tag.putBoolean(ZOMBIE_PET, zombiefied);
        sync(enchanted, tag);

    }

    public static boolean isZombiePet(LivingEntity enchanted) {
        CompoundTag tag = CitadelEntityData.getOrCreateCitadelTag(enchanted);
        return tag.getBoolean(ZOMBIE_PET);
    }

    public static int getCharismaBonusForOwner(Player player) {
        Predicate<Entity> pet = (animal) -> isTamed(animal) && isPetOf(player, animal);
        List<LivingEntity> list = player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(25, 8, 25), EntitySelector.NO_SPECTATORS.and(pet));
        int charismas = 0;
        for (LivingEntity entity : list) {
            charismas += 10 * getEnchantLevel(entity, DIEnchantmentRegistry.CHARISMA);
        }
        return Math.min(charismas, 50);
    }

    public static boolean isValidTeleporter(LivingEntity owner, Mob animal) {
        boolean hasTeleportFlag = hasEnchant(animal, DIEnchantmentRegistry.TETHERED_TELEPORT);
        if (!hasTeleportFlag) {
            TameData data = TameRegistry.get(animal.getUUID());
            hasTeleportFlag = data != null && data.attributeLevels.getOrDefault("tethered_teleport", 0) > 0;
        }
        if (hasTeleportFlag) {
            if (animal instanceof IComandableMob commandableMob) {
                return commandableMob.getCommand() == 2;
            } else if (animal instanceof TamableAnimal tame) {
                return !tame.isOrderedToSit();
            }
        }
        return false;
    }

    public static void absorbExpOrbs(LivingEntity living) {
        absorbExpOrbs(living, 1);
    }

    public static void absorbExpOrbs(LivingEntity living, int healPerXpPoint) {
        if (healPerXpPoint <= 0 || living.level().isClientSide || living.getHealth() >= living.getMaxHealth()) {
            return;
        }
        for (ExperienceOrb experienceorb : living.level().getEntitiesOfClass(ExperienceOrb.class, living.getBoundingBox().inflate(3D))) {
            if (living.getHealth() >= living.getMaxHealth()) {
                break;
            }
            Vec3 vec3 = new Vec3(living.getX() - experienceorb.getX(), living.getY() + (double) living.getEyeHeight() / 2.0D - experienceorb.getY(), living.getZ() - experienceorb.getZ());
            double d0 = vec3.lengthSqr();
            if (d0 < 2.0D) {
                int xpValue = Math.max(0, experienceorb.value);
                if (xpValue > 0) {
                    float missingHealth = Math.max(0.0F, living.getMaxHealth() - living.getHealth());
                    if (missingHealth > 0.0F) {
                        float fullOrbHealing = xpValue * (float) healPerXpPoint;
                        if (fullOrbHealing <= missingHealth) {
                            living.heal(fullOrbHealing);
                            experienceorb.discard();
                        } else {
                            int xpUsed = Math.max(1, (int) Math.ceil(missingHealth / (double) healPerXpPoint));
                            xpUsed = Math.min(xpValue, xpUsed);
                            living.heal(xpUsed * (float) healPerXpPoint);
                            experienceorb.value = xpValue - xpUsed;
                            if (experienceorb.value <= 0) {
                                experienceorb.discard();
                            }
                        }
                    }
                }
            }
            if (d0 < 64.0D) {
                double d1 = 1.0D - Math.sqrt(d0) / 8.0D;
                experienceorb.setDeltaMovement(experienceorb.getDeltaMovement().add(vec3.normalize().scale(d1 * d1 * 0.5D)));
            }
        }
    }
}
