package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.*;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID)
public final class PrimitiveMobsCompat {
    public static float petBlastRadius(LivingEntity entity, float nativeRadius) {
        if (!TameEntityAdapter.isTame(entity)) return nativeRadius;
        TameData data = TameRegistry.get(entity.getUUID());
        if (data == null) data = TameRegistry.getByTlId(TameData.getTlId(entity));
        return nativeRadius + (data == null ? 0 : (float) Math.max(0, data.bonusKnockback));
    }
    public static boolean isPetCreeper(LivingEntity entity) {
        if (entity == null) return false;
        if (entity instanceof net.minecraft.world.entity.monster.Creeper && isPrimitivePet(entity)) return true;
        return new net.minecraft.resources.ResourceLocation("mutantmonsters", "creeper_minion")
                .equals(ForgeRegistries.ENTITY_TYPES.getKey(entity.getType()));
    }
    public static final String BASELINE = "TLPrimitiveSpawnBaseline";
    private static final String RESET_GENERATION = "TLPrimitiveResetGeneration";
    private PrimitiveMobsCompat() {}

    /** Monster inheritance must not make an owned Primitive mob prey for another pet. */
    public static boolean protectFromOtherPets(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null || !isPrimitivePet(target)
                || !((ModifedToBeTameable) target).isTame()) return false;
        boolean pet = com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameEntityAdapter.isTame(attacker)
                || attacker instanceof OwnableEntity owned && owned.getOwnerUUID() != null;
        return pet && !com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager
                .areDuelOpponents(attacker.getUUID(), target.getUUID());
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void protectTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        if (protectFromOtherPets(event.getEntity(), event.getNewTarget())) event.setNewTarget(null);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void clearOldHostileTarget(LivingEvent.LivingTickEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity() instanceof Mob mob
                && protectFromOtherPets(mob, mob.getTarget())) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void protectFromPetAttack(net.minecraftforge.event.entity.living.LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof LivingEntity attacker
                && protectFromOtherPets(attacker, event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void beforeRegistryBackfill(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity mob) {
            applyRegistryReset(mob);
        }
    }

    public static boolean isPrimitiveType(String type) {
        if (type == null) return false;
        String normalized = type.trim().toLowerCase(Locale.ROOT).replace("_", "");
        if (normalized.startsWith("entity.")) normalized = normalized.substring(7);
        return normalized.startsWith("primitivemobs:") || normalized.startsWith("primitivemobs.")
                || normalized.startsWith("com.misanthropy.primitivemobs.entity.");
    }

    /** Inspect identity fields only: a wolf carrying Raw Dodo is not a Primitive Mobs pet. */
    public static boolean isPrimitiveSnapshot(CompoundTag tag) {
        if (tag == null) return false;
        for (String key : List.of("id", "type", "Type", "EntityType", "entityType")) {
            if (isPrimitiveType(tag.getString(key))) return true;
        }
        for (String key : List.of("snapshot", "entitySnapshot", "EntityData")) {
            if (tag.contains(key, Tag.TAG_COMPOUND) && isPrimitiveSnapshot(tag.getCompound(key))) return true;
        }
        return false;
    }

    public static void applyRegistryReset(LivingEntity mob) {
        if (mob.level().isClientSide || !isPrimitivePet(mob)) return;
        long generation = PrimitiveResetData.get(mob.getServer()).generation();
        if (mob.getPersistentData().getLong(RESET_GENERATION) < generation) {
            ((ModifedToBeTameable) mob).setTameOwnerUUID(null);
            if (mob.getPersistentData().contains(BASELINE)) {
                // Reapply the individual's baseline with zero registry bonuses before forgetting it.
                var cleared = new TameData(mob, null, false);
                com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem.reapplyTypeBasePlusBonuses(mob, cleared);
            }
            mob.getPersistentData().remove(TameData.TL_ID_TAG);
            ((IComandableMob) mob).setCommand(0);
            ((Mob) mob).setTarget(null);
            ((Mob) mob).getNavigation().stop();
            if (mob.hasCustomName()) {
                String name = mob.getCustomName().getString().replaceFirst("(?i)^\\s*\\[(?:lvl|level)\\s*\\d+\\]\\s*", "");
                mob.setCustomName(net.minecraft.network.chat.Component.literal(name));
            }
        }
        mob.getPersistentData().putLong(RESET_GENERATION, generation);
    }

    public static boolean isPrimitivePet(Entity entity) {
        if (!(entity instanceof ModifedToBeTameable)) return false;
        var id = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return id != null && id.getNamespace().equals("primitive_mobs");
    }

    private record Commands(Method get, Method set, Map<String, Object> values) {}
    private static final ClassValue<Commands> COMMANDS = new ClassValue<>() {
        protected Commands computeValue(Class<?> type) {
            try {
                Method get = type.getMethod("getFollowState");
                Class<?> state = get.getReturnType();
                Map<String, Object> values = new HashMap<>();
                for (Object value : state.getEnumConstants()) values.put(((Enum<?>) value).name(), value);
                return new Commands(get, type.getMethod("setFollowState", state), values);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("Unsupported Primitive Mobs commands", e); }
        }
    };

    public static int getCommand(Mob mob) {
        try {
            return switch (((Enum<?>) COMMANDS.get(mob.getClass()).get.invoke(mob)).name()) {
                case "SITTING" -> 1;
                case "FOLLOWING" -> 2;
                default -> 0;
            };
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    public static void setCommand(Mob mob, int command) {
        Commands commands = COMMANDS.get(mob.getClass());
        try {
            commands.set.invoke(mob, commands.values.get(command == 1 ? "SITTING" : command == 2 ? "FOLLOWING" : "WANDERING"));
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    public static void tame(Mob mob, Player player) {
        if (mob.level().isClientSide || !isPrimitivePet(mob) || !mob.isAlive()) return;
        ModifedToBeTameable pet = (ModifedToBeTameable) mob;
        if (pet.isTame()) return; // Never steal an existing owner, including egg NBT.
        pet.setTameOwnerUUID(player.getUUID());
        mob.getPersistentData().putLong(RESET_GENERATION, PrimitiveResetData.get(mob.getServer()).generation());
        ((IComandableMob) mob).setCommand(2);
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        mob.getNavigation().stop();
        mob.setPersistenceRequired();
        if (mob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 7, 0.4, 0.3, 0.4, 0);
        }
    }

    /** Called before registry/class rewards. ForgeData survives saves, storage and respawns. */
    public static void captureSpawnBaseline(LivingEntity mob) {
        if (mob.level().isClientSide || !isPrimitivePet(mob) || mob.getPersistentData().contains(BASELINE)) return;
        CompoundTag baseline = new CompoundTag();
        baseline.putDouble("MaxHealth", mob.getMaxHealth());
        CompoundTag attributes = new CompoundTag();
        CompoundTag modifierIds = new CompoundTag();
        Set<UUID> effectModifiers = new HashSet<>();
        for (MobEffectInstance effect : mob.getActiveEffects()) {
            effect.getEffect().getAttributeModifiers().values().forEach(modifier -> effectModifiers.add(modifier.getId()));
        }
        for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
            var instance = mob.getAttribute(attribute);
            if (instance == null) continue;
            attributes.putDouble(ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), instance.getBaseValue());
            ListTag ids = new ListTag();
            // Spawn rolls may use transient modifiers. Persist those too, without duplicating
            // the modifiers which Minecraft recreates when loading a potion effect.
            for (var modifier : new ArrayList<>(instance.getModifiers())) {
                if (effectModifiers.contains(modifier.getId())) continue;
                ids.add(StringTag.valueOf(modifier.getId().toString()));
                instance.removeModifier(modifier);
                instance.addPermanentModifier(modifier);
            }
            modifierIds.put(ForgeRegistries.ATTRIBUTES.getKey(attribute).toString(), ids);
        }
        baseline.put("Bases", attributes);
        baseline.put("ModifierIds", modifierIds);
        ListTag effects = new ListTag();
        for (MobEffectInstance effect : mob.getActiveEffects()) effects.add(effect.save(new CompoundTag()));
        baseline.put("Effects", effects);
        mob.getPersistentData().put(BASELINE, baseline);
    }

    public static void preserveIndividualModifiers(LivingEntity mob, Attribute attribute, Set<UUID> preserved) {
        if (!isPrimitivePet(mob)) return;
        var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        if (id == null) return;
        for (Tag tag : mob.getPersistentData().getCompound(BASELINE).getCompound("ModifierIds").getList(id.toString(), Tag.TAG_STRING)) {
            try { preserved.add(UUID.fromString(tag.getAsString())); }
            catch (IllegalArgumentException ignored) { /* Ignore a malformed old save entry. */ }
        }
        for (MobEffectInstance effect : mob.getActiveEffects()) {
            var modifier = effect.getEffect().getAttributeModifiers().get(attribute);
            if (modifier != null) preserved.add(modifier.getId());
        }
    }

    public static Double baseValue(TameData data, Attribute attribute) {
        if (data == null || data.entitySnapshot == null) return null;
        CompoundTag bases = data.entitySnapshot.getCompound("ForgeData").getCompound(BASELINE).getCompound("Bases");
        var id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
        return id != null && bases.contains(id.toString(), Tag.TAG_ANY_NUMERIC) ? bases.getDouble(id.toString()) : null;
    }

    public static double hungerExtraLevels(TameData data) {
        if (data == null || !(isPrimitiveType(data.type) || isPrimitiveSnapshot(data.entitySnapshot))) return 0;
        CompoundTag baseline = data.entitySnapshot == null ? new CompoundTag()
                : data.entitySnapshot.getCompound("ForgeData").getCompound(BASELINE);
        double health = baseline.contains("MaxHealth", Tag.TAG_ANY_NUMERIC) ? baseline.getDouble("MaxHealth")
                : originalMaxHealth(data, baseline);
        return Double.isFinite(health) ? Math.max(0, health - 20) : 0;
    }

    private static double originalMaxHealth(TameData data, CompoundTag baseline) {
        // Compatibility with the first integration's baseline format, before MaxHealth existed.
        Double base = baseValue(data, net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (base == null) return 20;
        var instance = new net.minecraft.world.entity.ai.attributes.AttributeInstance(
                net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, ignored -> {});
        instance.setBaseValue(base);
        Set<UUID> originalIds = new HashSet<>();
        for (Tag id : baseline.getCompound("ModifierIds").getList("minecraft:generic.max_health", Tag.TAG_STRING)) {
            try { originalIds.add(UUID.fromString(id.getAsString())); } catch (IllegalArgumentException ignored) { }
        }
        for (Tag attr : data.entitySnapshot.getList("Attributes", Tag.TAG_COMPOUND)) {
            CompoundTag attribute = (CompoundTag) attr;
            if (!attribute.getString("Name").equals("minecraft:generic.max_health")) continue;
            for (Tag mod : attribute.getList("Modifiers", Tag.TAG_COMPOUND)) {
                var modifier = net.minecraft.world.entity.ai.attributes.AttributeModifier.load((CompoundTag) mod);
                if (modifier != null && originalIds.contains(modifier.getId())) instance.addPermanentModifier(modifier);
            }
        }
        return instance.getValue();
    }

    /** Replace hostile prey selection with retaliation and the owner's combat orders. */
    public static final class PetTargetGoal extends Goal {
        private final Mob mob;
        private final ModifedToBeTameable pet;
        private LivingEntity target;
        private int selfHurt = -1, ownerHurt = -1, ownerAttack = -1;
        public PetTargetGoal(Mob mob) {
            this.mob = mob;
            this.pet = (ModifedToBeTameable) mob;
            setFlags(EnumSet.of(Flag.TARGET));
        }
        public boolean canUse() {
            if (!pet.isTame() || pet.isStayingStill()) return false;
            LivingEntity owner = pet.getTameOwner();
            target = null;
            if (selfHurt != mob.getLastHurtByMobTimestamp()) {
                selfHurt = mob.getLastHurtByMobTimestamp();
                target = mob.getLastHurtByMob();
            }
            if (owner != null && ownerHurt != owner.getLastHurtByMobTimestamp()) {
                ownerHurt = owner.getLastHurtByMobTimestamp();
                if (owner.tickCount - ownerHurt < 200 && owner.getLastHurtByMob() != null) target = owner.getLastHurtByMob();
            }
            if (owner != null && ownerAttack != owner.getLastHurtMobTimestamp()) {
                ownerAttack = owner.getLastHurtMobTimestamp();
                if (owner.tickCount - ownerAttack < 200 && owner.getLastHurtMob() != null) target = owner.getLastHurtMob();
            }
            return target != null && target.isAlive() && pet.isValidAttackTarget(target) && mob.canAttack(target);
        }
        public void start() { mob.setTarget(target); }
        public boolean canContinueToUse() { return false; }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !isPrimitivePet(entity) || !((ModifedToBeTameable) entity).isTame()) return;
        if (entity.tickCount % 20 != 0) return;
        for (Tag tag : entity.getPersistentData().getCompound(BASELINE).getList("Effects", Tag.TAG_COMPOUND)) {
            MobEffectInstance original = MobEffectInstance.load((CompoundTag) tag);
            if (original == null) continue;
            MobEffectInstance current = entity.getEffect(original.getEffect());
            if (current == null || current.getAmplifier() < original.getAmplifier()
                    || current.getAmplifier() == original.getAmplifier() && !current.isInfiniteDuration()) {
                entity.addEffect(new MobEffectInstance(original.getEffect(), -1, original.getAmplifier(), original.isAmbient(), original.isVisible(), original.showIcon()));
            }
        }
    }
}
