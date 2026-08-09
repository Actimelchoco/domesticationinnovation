package com.github.alexthe668.domesticationinnovation.server;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.block.DIBlockRegistry;
import com.github.alexthe668.domesticationinnovation.server.block.PetBedBlock;
import com.github.alexthe668.domesticationinnovation.server.block.PetBedBlockEntity;
import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import com.github.alexthe668.domesticationinnovation.server.entity.*;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.item.DeedOfOwnershipItem;
import com.github.alexthe668.domesticationinnovation.server.misc.*;
import com.github.alexthe668.domesticationinnovation.server.misc.trades.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAutoFollowEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameBehaviorEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameCombatEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameCrittersEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.GuardianToolEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TamePersistenceEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TamePortalStabilizeEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameProjectileTimeoutEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameProtectionEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameRenameEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameSpawnEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameWorldLoadEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.PlayerDebugSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TLAdminRuntimeSettings;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.TameClass;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.Tag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.*;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = DomesticationMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonProxy {

    public static final String SKIP_LANTERN_UNLOAD_ONCE_TAG = "diSkipLanternUnloadOnce";
    private static final Pattern NUMERIC_SUFFIX = Pattern.compile("^(.*?)(?:\\s+(\\d+))?$");
    private static final UUID FROST_FANG_SLOW = UUID.fromString("1eaf83ff-7207-4596-b37a-d7a07b3ec4cf");
    private static final TargetingConditions ZOMBIE_TARGET = TargetingConditions.forCombat().range(32.0D);
    private static final double PSYCHIC_WALL_OWNER_PROTECT_RANGE_BASE = 5.0D;
    private static final double PSYCHIC_WALL_OWNER_PROTECT_RANGE_PER_LEVEL = 1.5D;
    public static boolean queueLegacyPetTeleport(Entity entity, ServerLevel endpointWorld, UUID ownerUUID, long queuedGameTime) {
        return false;
    }

    private static final Map<Level, CollarTickTracker> COLLAR_TICK_TRACKER_MAP = new HashMap<>();
    private static long tlMigrationLastScanned = 0L;
    private static long tlMigrationLastMatchedPayload = 0L;
    private static long tlMigrationLastMissingPayload = 0L;
    private static long tlMigrationLastAppliedNew = 0L;
    private static long tlMigrationLastAppliedUpdate = 0L;
    private static long tlMigrationLastSkippedStale = 0L;
    private static String tlMigrationLastMode = "none";
    private static boolean tlIntegrationRegistered = false;
    private static final Map<UUID, PendingShadowHandsDebug> PENDING_SHADOW_HANDS_DEBUG = new HashMap<>();

    private record PendingShadowHandsDebug(UUID ownerId, UUID attackerId, UUID targetId, long gameTime, String tameName, String targetName, float amount) {
    }

    public void init() {
        registerTLIntegration();
    }

    private static void registerTLIntegration() {
        if (tlIntegrationRegistered) {
            return;
        }
        tlIntegrationRegistered = true;
        DomesticationMod.LOGGER.info("Registering TL integration handlers in DomesticationMod.");
        MinecraftForge.EVENT_BUS.register(TameCombatEvents.class);
        MinecraftForge.EVENT_BUS.register(TameCrittersEvents.class);
        MinecraftForge.EVENT_BUS.register(GuardianToolEvents.class);
        MinecraftForge.EVENT_BUS.register(TameAbilityEvents.class);
        MinecraftForge.EVENT_BUS.register(TameAutoFollowEvents.class);
        MinecraftForge.EVENT_BUS.register(TameBehaviorEvents.class);
        MinecraftForge.EVENT_BUS.register(TamePersistenceEvents.class);
        MinecraftForge.EVENT_BUS.register(TameRenameEvents.class);
        MinecraftForge.EVENT_BUS.register(TameSpawnEvents.class);
        MinecraftForge.EVENT_BUS.register(TameWorldLoadEvents.class);
        MinecraftForge.EVENT_BUS.register(TameCommands.class);
        MinecraftForge.EVENT_BUS.register(TameProtectionEvents.class);
        MinecraftForge.EVENT_BUS.register(TamePortalStabilizeEvents.class);
        MinecraftForge.EVENT_BUS.register(TameProjectileTimeoutEvents.class);
        registerOptionalWaystonesCompat();
    }

    private static void registerOptionalWaystonesCompat() {
        if (!ModList.get().isLoaded("waystones")) {
            return;
        }
        try {
            Class<?> compat = Class.forName("com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.WaystonesTeleportCompat");
            compat.getMethod("init").invoke(null);
            MinecraftForge.EVENT_BUS.register(compat);
            DomesticationMod.LOGGER.info("Registered optional Waystones TL teleport compat.");
        } catch (Throwable throwable) {
            DomesticationMod.LOGGER.error("Failed to register optional Waystones TL teleport compat.", throwable);
        }
    }

    public void serverInit() {
        ForgeChunkManager.setForcedChunkLoadingCallback(DomesticationMod.MODID, this::removeAllChunkTickets);
        DIVillagePieceRegistry.registerHouses();
    }

    private void removeAllChunkTickets(ServerLevel serverLevel, ForgeChunkManager.TicketHelper ticketHelper) {
        int i = 0;
        for (Map.Entry<UUID, Pair<LongSet, LongSet>> entry : ticketHelper.getEntityTickets().entrySet()) {
            ticketHelper.removeAllTickets(entry.getKey());
            i++;
        }
        DomesticationMod.LOGGER.debug("Removed " + i + " chunkloading tickets");
    }

    public void clientInit() {
    }

    public void updateVisualDataForMob(Entity entity, int[] arr) {

    }

    public void updateEntityStatus(Entity entity, byte updateKind) {

    }

    @SubscribeEvent
    public void onEntityJoinWorldEvent(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living && TameableUtils.couldBeTamed(living)) {
            if (TameableUtils.hasEnchant(living, DIEnchantmentRegistry.HEALTH_BOOST)) {
                living.setHealth((float) Math.max(living.getHealth(), TameableUtils.getSafePetHealth(living)));
            }
            if (living.isAlive() && TameableUtils.isTamed(living)) {
                DIWorldData data = DIWorldData.get(living.level());
                if (data != null) {
                    data.removeMatchingLanternRequests(living.getUUID());
                }
                if (!living.getPersistentData().getBoolean(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.ADMIN_CLONE_SILENT_TAG)) {
                    ensureDiProgressEntryForTame(living, null);
                }
            }
            if (!living.level().isClientSide
                    && living.level().getServer() != null
                    && living instanceof TamableAnimal tame
                    && tame.isTame()
                    && TameDuelManager.isEntityInDuel(tame.getUUID())) {
                TameDuelManager.refreshLoadedDuelParticipant(living.level().getServer(), tame);
            } else if (!living.level().isClientSide
                    && living instanceof TamableAnimal tame
                    && tame.isTame()) {
                TameDuelManager.restorePostDuelTargetGoalsIfNeeded(tame);
            }
        }
    }

    @SubscribeEvent
    public void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event == null || event.getSlot() != EquipmentSlot.CHEST) {
            return;
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // TL/DI extension commands are registered under /tames (and /tame alias) in TameCommands.
    }

    @SubscribeEvent
    public void onEntityLeaveWorld(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            if (living.getPersistentData().getBoolean(SKIP_LANTERN_UNLOAD_ONCE_TAG)) {
                living.getPersistentData().remove(SKIP_LANTERN_UNLOAD_ONCE_TAG);
                return;
            }
            if (living instanceof TamableAnimal tame && tame.isTame()) {
                TamePersistenceEvents.onTameEntityLeave(tame);
            }
            if (TameableUtils.couldBeTamed(living) && TameableUtils.hasEnchant(living, DIEnchantmentRegistry.HEALTH_BOOST)) {
                TameableUtils.setSafePetHealth(living, living.getHealth());
            }
        }
    }

    @SubscribeEvent
    public void onAnimalTamed(AnimalTameEvent event) {
        if (!event.getAnimal().level().isClientSide) {
            UUID ownerUUID = event.getTamer() == null ? null : event.getTamer().getUUID();
            if (!event.getAnimal().getPersistentData().getBoolean(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.ADMIN_CLONE_SILENT_TAG)) {
                ensureDiProgressEntryForTame(event.getAnimal(), ownerUUID);
            }
        }
    }

    @SubscribeEvent
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event == null || !event.isCancelable()) {
            return;
        }
        if (!(event.getEntity() instanceof TamableAnimal tame) || !tame.isTame()) {
            return;
        }
        UUID tameId = tame.getUUID();
        if (!TameCommands.isDuelSessionLocked(tameId)) {
            return;
        }
        LivingEntity owner = tame.getOwner();
        if (owner == null || owner.level() != tame.level()) {
            return;
        }
        double beforeOwnerDistSqr = tame.distanceToSqr(owner);
        double dx = event.getTargetX() - owner.getX();
        double dy = event.getTargetY() - owner.getY();
        double dz = event.getTargetZ() - owner.getZ();
        double afterOwnerDistSqr = dx * dx + dy * dy + dz * dz;
        // Block suspicious snapback teleports that pull duel participants directly to owner.
        if (beforeOwnerDistSqr > 64.0D && afterOwnerDistSqr <= 16.0D) {
            event.setCanceled(true);
            if (tame.level().getServer() != null) {
                TameDuelManager.refreshLoadedDuelParticipant(tame.level().getServer(), tame);
            }
        }
    }

    private boolean canTickCollar(Entity entity){
        if(entity.level().isClientSide){
            return true;
        }else{
            CollarTickTracker tracker = COLLAR_TICK_TRACKER_MAP.get(entity.level());
            return tracker == null || !tracker.isEntityBlocked(entity);
        }
    }

    private void blockCollarTick(Entity entity){
        if(!entity.level().isClientSide){
            CollarTickTracker tracker = COLLAR_TICK_TRACKER_MAP.get(entity.level());
            if(tracker != null){
                tracker.addBlockedEntityTick(entity.getUUID(), 5);
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.LevelTickEvent tick) {
        if (!tick.level.isClientSide) {
            COLLAR_TICK_TRACKER_MAP.computeIfAbsent(tick.level, k -> new CollarTickTracker());
            CollarTickTracker tracker = COLLAR_TICK_TRACKER_MAP.get(tick.level);
            tracker.tick();
        }
    }

    @SubscribeEvent
    public void onProjectileImpactEvent(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult) {
            Entity hit = ((EntityHitResult) event.getRayTraceResult()).getEntity();
            if (event.getProjectile().getOwner() instanceof Player) {
                Player player = (Player) event.getProjectile().getOwner();
                if (TameableUtils.isPetOf(player, hit)) {
                    event.setCanceled(true);
                }
            }
            if (TameableUtils.isTamed(hit)) {
                if (event.getEntity() instanceof AbstractArrow arrow) {
                    //fixes soft crash with vanilla
                    if (arrow.getPierceLevel() > 0) {
                        arrow.setPierceLevel((byte) 0);
                        arrow.remove(Entity.RemovalReason.DISCARDED);
                        event.setCanceled(true);
                        return;
                    }
                }
                if (getAbilityOrEnchantLevel((LivingEntity) hit, "deflection") > 0) {
                    event.setCanceled(true);
                    float xRot = event.getProjectile().getXRot();
                    float yRot = event.getProjectile().yRotO;
                    Vec3 vec3 = event.getProjectile().position().subtract(hit.position()).normalize().scale(hit.getBbWidth() + 0.5F);
                    Vec3 vec32 = hit.position().add(vec3);
                    hit.level().addParticle(DIParticleRegistry.DEFLECTION_SHIELD.get(), vec32.x, vec32.y, vec32.z, xRot, yRot, 0.0F);
                    event.getProjectile().setDeltaMovement(event.getProjectile().getDeltaMovement().scale(-0.2D));
                    event.getProjectile().setYRot(yRot + 180);
                    event.getProjectile().setXRot(xRot + 180);
                    debugDiAbilityUse((LivingEntity) hit, "deflection");
                }
            }
        }
    }

    @SubscribeEvent
    public void onItemDespawnEvent(ItemExpireEvent event) {
        if (event.getEntity().getItem().getItem() == Items.APPLE && DomesticationMod.CONFIG.rottenApple.get()) {
            if (new Random().nextFloat() < 0.1F * event.getEntity().getItem().getCount()) {
                event.getEntity().getItem().shrink(1);
                event.setExtraLife(10);
                ItemEntity rotten = new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(DIItemRegistry.ROTTEN_APPLE.get()));
                event.getEntity().level().addFreshEntity(rotten);
            }
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        enforceNoGriefMutantCreeperMinion(event.getEntity());
        int frozenTime = TameableUtils.getFrozenTime(event.getEntity());
        if (TameableUtils.couldBeTamed(event.getEntity()) && canTickCollar(event.getEntity())) {
            if (!event.getEntity().level().isClientSide && event.getEntity().tickCount % 20 == 0) {
                TameableUtils.syncVisualCollarEnchants(event.getEntity());
            }
            if (!event.getEntity().level().isClientSide) {
                int i = TameableUtils.getImmuneTime(event.getEntity());
                if (i > 0) {
                    TameableUtils.setImmuneTime(event.getEntity(), i - 1);
                }
                int cooldown = TameableUtils.getImmuneCooldown(event.getEntity());
                if (cooldown > 0) {
                    TameableUtils.setImmuneCooldown(event.getEntity(), cooldown - 1);
                }
            }
            if (event.getEntity().hasEffect(MobEffects.POISON) && TameableUtils.hasEnchant(event.getEntity(), DIEnchantmentRegistry.POISON_RESISTANCE)) {
                event.getEntity().removeEffect(MobEffects.POISON);
            }
            int amphibiousLevel = getDiEffectLevel(event.getEntity(), "amphibious");
            if (amphibiousLevel > 0) {
                event.getEntity().setAirSupply(event.getEntity().getMaxAirSupply());
            }
            int magneticLevel = getDiEffectLevel(event.getEntity(), "magnetic");
            boolean magneticVisual = TameableUtils.hasEnchant(event.getEntity(), DIEnchantmentRegistry.MAGNETIC);
            if (event.getEntity() instanceof Mob mob && ((magneticLevel > 0 && !mob.level().isClientSide) || magneticVisual)) {
                Entity sucking = TameableUtils.getPetAttackTarget(mob);
                if (!mob.level().isClientSide) {
                    LivingEntity liveTarget = mob.getTarget();
                    if (liveTarget == null || !liveTarget.isAlive() || mob.getRootVehicle() instanceof GiantBubbleEntity) {
                        if (!shouldRetainMagneticTarget(mob, sucking) && TameableUtils.getPetAttackTargetID(mob) != -1) {
                            TameableUtils.setPetAttackTarget(mob, -1);
                        }
                    } else {
                        TameableUtils.setPetAttackTarget(mob, liveTarget.getId());
                    }
                } else if (magneticVisual) {
                    if (sucking != null) {
                        double dist = mob.distanceTo(sucking);
                        Vec3 start = mob.position().add(0, mob.getBbHeight() * 0.5F, 0);
                        Vec3 end = sucking.position().add(0, sucking.getBbHeight() * 0.5F, 0).subtract(start);
                        for (float distStep = mob.getBbWidth() + 0.8F; distStep < (int) Math.ceil(dist); distStep++) {
                            Vec3 vec3 = start.add(end.scale(distStep / dist));
                            float f1 = 0.5F * (mob.getRandom().nextFloat() - 0.5F);
                            float f2 = 0.5F * (mob.getRandom().nextFloat() - 0.5F);
                            float f3 = 0.5F * (mob.getRandom().nextFloat() - 0.5F);
                            mob.level().addParticle(DIParticleRegistry.MAGNET.get(), vec3.x + f1, vec3.y + f2, vec3.z + f3, 0.0F, 0.0F, 0.0F);
                        }
                    }
                }
                if (sucking != null) {
                    if (mob.tickCount % 15 == 0) {
                        mob.playSound(DISoundRegistry.MAGNET_LOOP.get(), 1F, 1F);
                    }
                    mob.setDeltaMovement(mob.getDeltaMovement().multiply(0.88D, 1.0D, 0.88D));
                    if (!TameableUtils.shouldBlockFriendlyDiEffect(mob, sucking)) {
                        Vec3 move = new Vec3(mob.getX() - sucking.getX(), mob.getY() - (double) sucking.getEyeHeight() / 2.0D - sucking.getY(), mob.getZ() - sucking.getZ());
                        double resistanceMultiplier = sucking instanceof LivingEntity living ? magneticResistanceMultiplier(living) : 1.0D;
                        double pullStrength = magneticPullStrength(magneticLevel, mob.onGround()) * resistanceMultiplier;
                        if (pullStrength > 0.0D) {
                            sucking.setDeltaMovement(sucking.getDeltaMovement().add(move.normalize().scale(pullStrength)));
                        }
                    }
                }
            }
            int shadowHandsLevel = getAbilityOrEnchantLevel(event.getEntity(), "shadow_hands");
            if (shadowHandsLevel > 0 && event.getEntity() instanceof Mob mob) {
                DomesticationMod.PROXY.updateVisualDataForMob(event.getEntity(), TameableUtils.getShadowPunchTimes(mob));
                if (!mob.level().isClientSide) {
                    spawnShadowHandsFallbackParticles((ServerLevel) mob.level(), mob, shadowHandsLevel);
                    Entity punching = TameableUtils.getPetAttackTarget(mob);
                    int[] punchProgress = TameableUtils.getShadowPunchTimes(mob);
                    if (punching != null && punching.isAlive() && mob.distanceTo(punching) < 16) {
                        int[] striking = TameableUtils.getShadowPunchStriking(mob);
                        if (punchProgress == null || punchProgress.length < shadowHandsLevel || striking == null || striking.length < shadowHandsLevel) {
                            int[] clean = new int[shadowHandsLevel];
                            debugShadowHandsState(mob, punching, "init arrays times="
                                    + (punchProgress == null ? "null" : punchProgress.length)
                                    + ", striking="
                                    + (striking == null ? "null" : striking.length)
                                    + ", need=" + shadowHandsLevel);
                            TameableUtils.setShadowPunchTimes(mob, clean);
                            TameableUtils.setShadowPunchStriking(mob, clean);
                        } else {
                            int cooldown = TameableUtils.getShadowPunchCooldown(mob);
                            if (cooldown <= 0) {
                                boolean flag = false;
                                int start = shadowHandsLevel == 1 ? 0 : mob.getRandom().nextInt(shadowHandsLevel - 1);
                                for (int i = start; i < shadowHandsLevel; i++) {
                                    if (striking[i] == 0) {
                                        striking[i] = 1;
                                        debugShadowHandsState(mob, punching, "start hand=" + i + ", cooldown=5, dist=" + fmt(mob.distanceTo(punching)));
                                        flag = true;
                                        break;
                                    }
                                }
                                if (flag) {
                                    TameableUtils.setShadowPunchCooldown(mob, 5);
                                }
                            } else {
                                TameableUtils.setShadowPunchCooldown(mob, cooldown - 1);
                            }
                            for (int i = 0; i < Math.min(shadowHandsLevel, Math.min(striking.length, punchProgress.length)); i++) {
                                if (striking[i] != 0) {
                                    if (punchProgress[i] < 10) {
                                        punchProgress[i] = punchProgress[i] + 1;
                                    } else {
                                        float shadowHandsDamage = Mth.clamp(shadowHandsLevel, 2, 4);
                                        debugShadowHandsStrike(mob, punching, shadowHandsDamage);
                                        boolean dealt = punching.hurt(punching.damageSources().mobAttack(mob), shadowHandsDamage);
                                        debugShadowHandsHurtResult(mob, punching, shadowHandsDamage, dealt);
                                        if (dealt) {
                                            debugDiAbilityUse(mob, "shadow_hands");
                                        }
                                        striking[i] = 0;
                                    }
                                }
                                if (striking[i] == 0 && punchProgress[i] > 0) {
                                    punchProgress[i] = punchProgress[i] - 1;
                                }
                            }
                            TameableUtils.setShadowPunchStriking(mob, striking);
                            TameableUtils.setShadowPunchTimes(mob, punchProgress);
                        }
                    } else {
                        if (punching != null) {
                            boolean flag = true;
                            for (int i = 0; i < Math.min(shadowHandsLevel, punchProgress.length); i++) {
                                if (punchProgress[i] > 0) {
                                    punchProgress[i] = punchProgress[i] - 1;
                                    flag = false;
                                }
                            }
                            TameableUtils.setShadowPunchStriking(mob, new int[shadowHandsLevel]);
                            TameableUtils.setShadowPunchTimes(mob, punchProgress);
                            if (flag) {
                                debugShadowHandsState(mob, punching, "clear target after punch wind-down");
                                TameableUtils.setPetAttackTarget(mob, -1);
                            }
                        }
                        Entity punchingTarget = null;
                        if (mob.getTarget() != null) {
                            punchingTarget = mob.getTarget();
                        } else if (TameableUtils.getOwnerOf(mob) instanceof LivingEntity owner) {
                            if (owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive() && !TameableUtils.hasSameOwnerAs(mob, owner.getLastHurtByMob())) {
                                punchingTarget = owner.getLastHurtByMob();
                            }
                            if (owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive() && !TameableUtils.hasSameOwnerAs(mob, owner.getLastHurtMob())) {
                                punchingTarget = owner.getLastHurtMob();
                            }
                        }
                        if (punchingTarget != null && punchingTarget.isAlive()) {
                            debugShadowHandsState(mob, punchingTarget, "acquire target from " + (mob.getTarget() != null ? "mobTarget" : "ownerCombat"));
                            TameableUtils.setPetAttackTarget(mob, punchingTarget.getId());
                        }
                    }
                }
            } else if (event.getEntity() instanceof Mob mob) {
                if (TameableUtils.getShadowPunchTimes(mob).length > 0
                        || TameableUtils.getShadowPunchStriking(mob).length > 0
                        || TameableUtils.getShadowPunchCooldown(mob) > 0
                        || TameableUtils.getPetAttackTargetID(mob) != -1) {
                    TameableUtils.clearShadowHandState(mob);
                }
                DomesticationMod.PROXY.updateVisualDataForMob(event.getEntity(), new int[0]);
            }
            int discJockeyLevel = getDiEffectLevel(event.getEntity(), "disc_jockey");
            if (discJockeyLevel > 0 && !event.getEntity().level().isClientSide && event.getEntity().tickCount % 10 == 0) {
                UUID uuid = TameableUtils.getPetJukeboxUUID(event.getEntity());
                if (uuid == null || !(((ServerLevel) event.getEntity().level()).getEntity(uuid) instanceof FollowingJukeboxEntity)) {
                    FollowingJukeboxEntity follower = DIEntityRegistry.FOLLOWING_JUKEBOX.get().create(event.getEntity().level());
                    follower.setFollowingUUID(event.getEntity().getUUID());
                    follower.copyPosition(event.getEntity());
                    event.getEntity().level().addFreshEntity(follower);
                    TameableUtils.setPetJukeboxUUID(event.getEntity(), follower.getUUID());
                }
            }
            int linkedInventoryLevel = getDiEffectLevel(event.getEntity(), "linked_inventory");
            if (linkedInventoryLevel > 0 && event.getEntity() instanceof Mob mob) {
                if (!mob.canPickUpLoot()) {
                    mob.setCanPickUpLoot(true);
                }
            }
            int shepherdLvl = getDiEffectLevel(event.getEntity(), "herding");
            if (shepherdLvl > 0) {
                TameableUtils.attractAnimals(event.getEntity(), shepherdLvl * 3);
            }
            if (TameableUtils.hasEnchant(event.getEntity(), DIEnchantmentRegistry.INFAMY_CURSE)) {
                TameableUtils.aggroRandomMonsters(event.getEntity());
            }
            int intimidationLevel = getDiEffectLevel(event.getEntity(), "intimidation");
            if (intimidationLevel > 0) {
                TameableUtils.scareRandomMonsters(event.getEntity(), intimidationLevel);
            }
            if (TameableUtils.hasEnchant(event.getEntity(), DIEnchantmentRegistry.BLIGHT_CURSE)) {
                TameableUtils.destroyRandomPlants(event.getEntity());
            }
            int rejuvenationLevel = getDiEffectLevel(event.getEntity(), "rejuvenation");
            if (rejuvenationLevel > 0) {
                TameableUtils.absorbExpOrbs(event.getEntity(), rejuvenationLevel);
            }
            int voidCloudLevel = getDiEffectLevel(event.getEntity(), "void_cloud");
            if (voidCloudLevel > 0 && !event.getEntity().isInWaterOrBubble() && event.getEntity().fallDistance > 3.0F && !event.getEntity().onGround()) {
                Entity owner = TameableUtils.getOwnerOf(event.getEntity());
                boolean shouldMoveToOwnerXZ = owner != null && Math.abs(owner.getY() - event.getEntity().getY()) < 1;
                double targetX = shouldMoveToOwnerXZ ? owner.getX() : event.getEntity().getX();
                double targetY = Math.max(event.getEntity().level().getMinBuildHeight() + 0.5F, owner == null ? 64F : owner.getY() < event.getEntity().getY() ? owner.getY() + 0.6F : owner.getY(1.0F) + event.getEntity().getBbHeight());
                if (owner != null && owner.getRootVehicle() == event.getEntity()) {
                    targetY = Math.min(event.getEntity().level().getMinBuildHeight() + 0.5F, event.getEntity().getY() - 0.5F);
                }
                double targetZ = shouldMoveToOwnerXZ ? owner.getZ() : event.getEntity().getZ();
                if (event.getEntity().verticalCollision) {
                    event.getEntity().setOnGround(true);
                    targetX += (event.getEntity().getRandom().nextFloat() - 0.5F) * 4;
                    targetZ += (event.getEntity().getRandom().nextFloat() - 0.5F) * 4;
                }
                Vec3 move = new Vec3(targetX - event.getEntity().getX(), targetY - event.getEntity().getY(), targetZ - event.getEntity().getZ());
                event.getEntity().setDeltaMovement(event.getEntity().getDeltaMovement().add(move.normalize().scale(0.15D)).multiply(0.5F, 0.5F, 0.5F));
                if (event.getEntity().level() instanceof ServerLevel) {
                    TameableUtils.setFallDistance(event.getEntity(), event.getEntity().fallDistance);
                    ((ServerLevel) event.getEntity().level()).sendParticles(ParticleTypes.REVERSE_PORTAL, event.getEntity().getRandomX(1.5F), event.getEntity().getY() - event.getEntity().getRandom().nextFloat(), event.getEntity().getRandomZ(1.5F), 0, 0, -0.2F, 0, 1.0D);
                }
            }
            int oreLvl = getDiEffectLevel(event.getEntity(), "ore_scenting");
            if (oreLvl > 0 && !event.getEntity().level().isClientSide) {
                TameData tameData = TameRegistry.get(event.getEntity().getUUID());
                String preferredOreId = tameData == null ? "" : tameData.oreScentingOreId;
                if (preferredOreId == null || preferredOreId.isBlank()) {
                    preferredOreId = "";
                }
                if (!preferredOreId.isBlank()) {
                    int interval = 100 + Math.max(150, 550 - oreLvl * 100);
                    TameableUtils.detectRandomOres(event.getEntity(), interval, 5 + oreLvl * 2, oreLvl * 50, oreLvl * 3, preferredOreId);
                }
            }
            if (TameableUtils.isZombiePet(event.getEntity()) && !event.getEntity().level().isClientSide && event.getEntity() instanceof Mob mob) {
                if (mob.getTarget() instanceof Player && ((Player) mob.getTarget()).isCreative()) {
                    mob.setTarget(null);
                }
                if (mob.getTarget() == null || !mob.getTarget().isAlive()) {
                    mob.setTarget(mob.level().getNearestPlayer(ZOMBIE_TARGET, mob));
                } else if (mob.distanceTo(mob.getTarget()) < mob.getBbWidth() + 0.5F) {
                    mob.doHurtTarget(mob.getTarget());
                } else if (mob.getNavigation().isDone()) {
                    mob.getNavigation().moveTo(mob.getTarget(), 1.0D);
                }
            }
            int psychicWallLevel = getAbilityOrEnchantLevel(event.getEntity(), "psychic_wall");
            if (psychicWallLevel > 0 && event.getEntity() instanceof Mob mob && !event.getEntity().level().isClientSide) {
                int cooldown = TameableUtils.getPsychicWallCooldown(mob);
                if (cooldown > 0) {
                    TameableUtils.setPsychicWallCooldown(mob, cooldown - 1);
                } else {
                    Entity blocking = null;
                    Entity blockingFrom = null;
                    if (mob.getTarget() != null) {
                        blocking = mob.getTarget();
                        blockingFrom = mob;
                    } else if (TameableUtils.getOwnerOf(mob) instanceof LivingEntity owner) {
                        double ownerProtectRange = PSYCHIC_WALL_OWNER_PROTECT_RANGE_BASE + Math.max(0, psychicWallLevel - 1) * PSYCHIC_WALL_OWNER_PROTECT_RANGE_PER_LEVEL;
                        boolean ownerNearby = mob.distanceToSqr(owner) <= ownerProtectRange * ownerProtectRange;
                        if (ownerNearby && owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive() && !TameableUtils.hasSameOwnerAs(mob, owner.getLastHurtByMob())) {
                            blocking = owner.getLastHurtByMob();
                            blockingFrom = owner;
                        }
                        if (ownerNearby && owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive() && !TameableUtils.hasSameOwnerAs(mob, owner.getLastHurtMob())) {
                            blocking = owner.getLastHurtMob();
                            blockingFrom = owner;
                        }
                    }
                    if (blocking != null) {
                        int width = psychicWallLevel + 1;
                        float yAdditional = blocking.getBbHeight() * 0.5F + width * 0.5F;
                        Vec3 vec3 = blockingFrom.position().add(0, yAdditional, 0);
                        Vec3 vec32 = blocking.position().add(0, yAdditional, 0);
                        Vec3 vec33 = vec3.add(vec32);
                        Vec3 avg = new Vec3(vec33.x / 2F, Math.floor(vec33.y / 2F), vec33.z / 2F);
                        Vec3 rotationFrom = avg.subtract(vec3);
                        Direction dir = Direction.getNearest(rotationFrom.x, rotationFrom.y, rotationFrom.z);
                        PsychicWallEntity wall = DIEntityRegistry.PSYCHIC_WALL.get().create(mob.level());
                        wall.setPos(avg.x, avg.y, avg.z);
                        wall.setBlockWidth(width);
                        wall.setCreatorId(mob.getUUID());
                        wall.setLifespan(psychicWallLevel * 100);
                        wall.setWallDirection(dir);
                        mob.level().addFreshEntity(wall);
                        TameableUtils.setPsychicWallCooldown(mob, psychicWallLevel * 260 + 60);
                        debugDiAbilityUse(mob, "psychic_wall");
                    }
                }
            }
            int blazingProtectionLevel = getDiEffectLevel(event.getEntity(), "blazing_protection");
            if (blazingProtectionLevel > 0 && !event.getEntity().level().isClientSide) {
                int bars = TameableUtils.getBlazingProtectionBars(event.getEntity());
                if (bars < blazingProtectionBarCap(blazingProtectionLevel)) {
                    int cooldown = TameableUtils.getBlazingProtectionCooldown(event.getEntity());
                    if (cooldown > 0) {
                        cooldown--;
                    } else {
                        TameableUtils.setBlazingProtectionBars(event.getEntity(), bars + 1);
                        cooldown = blazingProtectionRechargeTicks(blazingProtectionLevel);
                    }
                    TameableUtils.setBlazingProtectionCooldown(event.getEntity(), cooldown);
                }
                spawnBlazingProtectionFallbackParticles((ServerLevel) event.getEntity().level(), event.getEntity(), TameableUtils.getBlazingProtectionBars(event.getEntity()));
            }
            int healingAuraLevel = getAbilityOrEnchantLevel(event.getEntity(), "healing_aura");
            if (healingAuraLevel > 0 && !event.getEntity().level().isClientSide) {
                int time = TameableUtils.getHealingAuraTime(event.getEntity());
                if (time > 0) {
                    List<LivingEntity> hurtNearby = TameableUtils.getAuraHealables(event.getEntity());
                    boolean applied = false;
                    for (LivingEntity needsHealing : hurtNearby) {
                        if (!needsHealing.hasEffect(MobEffects.REGENERATION)) {
                            needsHealing.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, Math.max(0, healingAuraLevel / 4)));
                            applied = true;
                        }
                    }
                    if (applied) {
                        debugDiAbilityUse(event.getEntity(), "healing_aura");
                    }
                    time--;
                    if (time == 0) {
                        time = -400 - event.getEntity().getRandom().nextInt(601);
                    }
                } else if (time < 0) {
                    time++;
                } else if ((event.getEntity().tickCount + event.getEntity().getId()) % 200 == 0 || TameableUtils.getHealingAuraImpulse(event.getEntity())) {
                    List<LivingEntity> hurtNearby = TameableUtils.getAuraHealables(event.getEntity());
                    if (!hurtNearby.isEmpty()) {
                        time = 200;
                    }
                    TameableUtils.setHealingAuraImpulse(event.getEntity(), false);
                }
                TameableUtils.setHealingAuraTime(event.getEntity(), time);
            }
        }

        if (frozenTime > 0) {
            TameableUtils.setFrozenTimeTag(event.getEntity(), frozenTime - 1);
            int frozenLevel = Math.max(1, TameableUtils.getFrozenLevel(event.getEntity()));
            AttributeInstance instance = event.getEntity().getAttribute(Attributes.MOVEMENT_SPEED);
            if (instance != null) {
                float scale = (float) ((1.0F + Math.max(0, frozenLevel - 1) * 0.08F) * frostFangResistanceMultiplier(event.getEntity()));
                float f = Math.max(-0.35F, -0.06F * event.getEntity().getPercentFrozen() * scale);
                if (frozenTime > 1) {
                    AttributeModifier fangModifier = new AttributeModifier(FROST_FANG_SLOW, "Frost fang slow", f, AttributeModifier.Operation.ADDITION);
                    if (!instance.hasModifier(fangModifier)) {
                        instance.removeModifier(FROST_FANG_SLOW);
                        instance.addTransientModifier(fangModifier);
                    }
                } else {
                    instance.removeModifier(FROST_FANG_SLOW);
                }
            }
            for (int i = 0; i < 1 + event.getEntity().getRandom().nextInt(2); i++) {
                event.getEntity().level().addParticle(ParticleTypes.SNOWFLAKE, event.getEntity().getRandomX(0.7F), event.getEntity().getRandomY(), event.getEntity().getRandomZ(0.7F), 0.0F, 0.0F, 0.0F);
            }
        } else {
            AttributeInstance instance = event.getEntity().getAttribute(Attributes.MOVEMENT_SPEED);
            if (instance != null) {
                instance.removeModifier(FROST_FANG_SLOW);
            }
            if (TameableUtils.getFrozenLevel(event.getEntity()) != 0) {
                TameableUtils.setFrozenLevel(event.getEntity(), 0);
            }
        }
    }

    @SubscribeEvent
    public void onLivingHurt(LivingAttackEvent event) {
        debugShadowHandsAttackEvent(event);
        if (TameableUtils.isTamed(event.getEntity()) && !event.getSource().is(DIDamageTypes.SIPHON)) {
            boolean flag = false;
            int thornsLevel = 0;
            if (thornsLevel > 0 && event.getSource().getEntity() instanceof LivingEntity attacker && !TameableUtils.hasSameOwnerAs(attacker, event.getEntity())) {
                float chance = Math.min(1.0F, 0.15F * thornsLevel);
                if (event.getEntity().getRandom().nextFloat() < chance) {
                    float reflect = thornsLevel > 10 ? (float) (thornsLevel - 10) : (1.0F + event.getEntity().getRandom().nextInt(4));
                    attacker.hurt(event.getEntity().damageSources().thorns(event.getEntity()), reflect);
                }
            }
            int immunityFrameLevel = getAbilityOrEnchantLevel(event.getEntity(), "immunity_frame");
            if (immunityFrameLevel > 0) {
                int level = immunityFrameLevel;
                if (TameableUtils.getImmuneTime(event.getEntity()) > 0) {
                    flag = true;
                    event.setCanceled(true);
                    debugDiAbilityUse(event.getEntity(), "immunity_frame");
                } else if (TameableUtils.getImmuneCooldown(event.getEntity()) <= 0) {
                    TameableUtils.setImmuneTime(event.getEntity(), Math.min(100, 5 + level * 5));
                    TameableUtils.setImmuneCooldown(event.getEntity(), 250);
                    flag = true;
                    event.setCanceled(true);
                    debugDiAbilityUse(event.getEntity(), "immunity_frame");
                }
            }
            int blazingProtectionLevel = getDiEffectLevel(event.getEntity(), "blazing_protection");
            if (blazingProtectionLevel > 0) {
                int bars = TameableUtils.getBlazingProtectionBars(event.getEntity());
                if (bars > 0) {
                    Entity attacker = event.getSource().getEntity();
                    if (attacker instanceof LivingEntity livingAttacker && !TameableUtils.shouldBlockFriendlyDiEffect((LivingEntity) event.getEntity(), livingAttacker)) {
                        livingAttacker.setSecondsOnFire(blazingProtectionFireSeconds(blazingProtectionLevel) + event.getEntity().getRandom().nextInt(2));
                        livingAttacker.knockback(blazingProtectionKnockback(blazingProtectionLevel), event.getEntity().getX() - livingAttacker.getX(), event.getEntity().getZ() - livingAttacker.getZ());
                    }
                    event.setCanceled(true);
                    flag = true;
                    if(attacker != null){
                        for (int i = 0; i < 3 + event.getEntity().getRandom().nextInt(3); i++) {
                            attacker.level().addParticle(ParticleTypes.FLAME, event.getEntity().getRandomX(0.8F), event.getEntity().getRandomY(), event.getEntity().getRandomZ(0.8F), 0.0F, 0.0F, 0.0F);
                        }
                    }
                    event.getEntity().playSound(DISoundRegistry.BLAZING_PROTECTION.get(), 1, event.getEntity().getVoicePitch());
                    TameableUtils.setBlazingProtectionBars(event.getEntity(), bars - 1);
                    TameableUtils.setBlazingProtectionCooldown(event.getEntity(), blazingProtectionRecoveryTicks(blazingProtectionLevel));
                }
            }
            if ((event.getSource().is(DamageTypes.DROWN) || event.getSource().is(DamageTypes.DRY_OUT)) && getDiEffectLevel(event.getEntity(), "amphibious") > 0) {
                event.setCanceled(true);
                flag = true;
            }
            if (!flag && (event.getSource().is(DamageTypes.FALL) || event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) && getDiEffectLevel(event.getEntity(), "void_cloud") > 0) {
                event.setCanceled(true);
                flag = true;
            }
            int healthSiphonLevel = getDiEffectLevel(event.getEntity(), "health_siphon");
            if (!flag && healthSiphonLevel > 0 && TameRegistry.isHealthSiphonEnabled(TameableUtils.getOwnerUUIDOf(event.getEntity()))) {
                Entity owner = TameableUtils.getOwnerOf(event.getEntity());
                if (owner != null && owner.isAlive() && owner.distanceTo(event.getEntity()) < healthSiphonRange(healthSiphonLevel) && owner != event.getEntity()) {
                    owner.hurt(event.getSource(), event.getAmount());
                    event.setCanceled(true);
                    flag = true;
                    event.getEntity().hurt(DIDamageTypes.causeSiphonDamage(owner.level().registryAccess()), 0.0F);
                }
            }
            if (!flag && TameableUtils.hasEnchant(event.getEntity(), DIEnchantmentRegistry.TOTAL_RECALL) && event.getEntity().getHealth() - event.getAmount() <= 2.0D && !TameableUtils.isZombiePet(event.getEntity())) {
                UUID owner = TameableUtils.getOwnerUUIDOf(event.getEntity());
                if (owner != null) {
                    if (event.getEntity() instanceof Mob mob) {
                        mob.playAmbientSound();
                    }
                    event.getEntity().playSound(SoundEvents.ENDER_CHEST_CLOSE, 1.0F, 1.5F);
                    RecallBallEntity recallBall = DIEntityRegistry.RECALL_BALL.get().create(event.getEntity().level());
                    recallBall.setOwnerUUID(owner);
                    CompoundTag tag = new CompoundTag();
                    event.getEntity().addAdditionalSaveData(tag);
                    recallBall.setContainedData(tag);
                    recallBall.setContainedEntityType(ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString());
                    recallBall.setPos(event.getEntity().getX(), Math.max(event.getEntity().getY(), event.getEntity().level().getMinBuildHeight() + 1), event.getEntity().getZ());
                    recallBall.setYRot(event.getEntity().getYRot());
                    recallBall.setInvulnerable(true);
                    event.getEntity().stopRiding();
                    if (event.getEntity().level().addFreshEntity(recallBall)) {
                        event.getEntity().discard();
                    }
                    flag = true;
                    event.setCanceled(true);
                }
            }
        }
        if (event.getSource().getEntity() != null && TameableUtils.isTamed(event.getSource().getEntity())) {
            LivingEntity attacker = (LivingEntity) event.getSource().getEntity();
            int lightningLevel = getDiEffectLevel(attacker, "chain_lightning");
            int bubblingLevel = getDiEffectLevel(attacker, "bubbling");
            int vampireLevel = TameableUtils.getEnchantLevel(attacker, DIEnchantmentRegistry.VAMPIRE);

            if (lightningLevel > 0 && !hasTlAttributeLevel(attacker, "chain_lightning")) {
                if (!TameableUtils.shouldBlockOffensiveDiTarget(attacker, event.getEntity())) {
                    spawnLegacyChainLightning(attacker, event.getEntity(), lightningLevel, event.getAmount());
                }
            }
            int frostFangLevel = getDiEffectLevel(attacker, "frost_fang");
            if (!TameableUtils.shouldBlockFriendlyDiEffect(attacker, event.getEntity()) && shouldApplyLegacyFrostFang(attacker, frostFangLevel)) {
                int safeLevel = Math.max(1, frostFangLevel);
                double resistanceMultiplier = frostFangResistanceMultiplier(event.getEntity());
                int frozenTicks = Math.max(10, Mth.floor((100 + Math.max(0, safeLevel - 1) * 20) * resistanceMultiplier));
                int frozenTimeApplied = Math.max(5, Mth.floor((30 + Math.max(0, safeLevel - 1) * 4) * resistanceMultiplier));
                event.getEntity().setTicksFrozen(event.getEntity().getTicksRequiredToFreeze() + frozenTicks);
                Vec3 vec3 = event.getEntity().getEyePosition().subtract(attacker.getEyePosition()).normalize().scale(attacker.getBbWidth() + 0.5F);
                Vec3 vec32 = attacker.getEyePosition().add(vec3);
                for (int i = 0; i < 3 + attacker.getRandom().nextInt(3); i++) {
                    float f1 = 0.2F * (attacker.getRandom().nextFloat() - 1.0F);
                    float f2 = 0.2F * (attacker.getRandom().nextFloat() - 1.0F);
                    float f3 = 0.2F * (attacker.getRandom().nextFloat() - 1.0F);
                    attacker.level().addParticle(ParticleTypes.SNOWFLAKE, vec32.x + f1, vec32.y + f2, vec32.z + f3, 0.0F, 0.0F, 0.0F);
                }
                TameableUtils.setFrozenTimeTag(event.getEntity(), frozenTimeApplied);
                TameableUtils.setFrozenLevel(event.getEntity(), safeLevel);
            }
            if (bubblingLevel > 0
                    && isBaseAttackHit(attacker, event)
                    && event.getEntity() instanceof Enemy
                    && !TameableUtils.shouldBlockOffensiveDiTarget(attacker, event.getEntity())
                    && attacker.getRandom().nextDouble() < bubblingProcChance(bubblingLevel, event.getEntity())) {
                if (!(event.getEntity().getRootVehicle() instanceof GiantBubbleEntity) && (event.getEntity().onGround() || event.getEntity().isInWaterOrBubble() || event.getEntity().isInLava())) {
                    GiantBubbleEntity bubble = DIEntityRegistry.GIANT_BUBBLE.get().create(event.getEntity().level());
                    bubble.copyPosition(event.getEntity());
                    event.getEntity().startRiding(bubble, true);
                    bubble.setpopsIn(bubblingLevel * 40 + 40);
                    event.getEntity().level().addFreshEntity(bubble);
                    event.getEntity().playSound(DISoundRegistry.GIANT_BUBBLE_INFLATE.get(), 1F, 1F);

                }
            }
            if (vampireLevel > 0) {
                if (attacker.getHealth() < attacker.getMaxHealth()) {
                    float f = Mth.clamp(event.getAmount() * vampireLevel * 0.5F, 1F, 10F);
                    attacker.heal(f);
                    if (event.getEntity().level() instanceof ServerLevel) {
                        for (int i = 0; i < 5 + event.getEntity().getRandom().nextInt(3); i++) {
                            double f1 = event.getEntity().getRandomX(0.7F);
                            double f2 = event.getEntity().getY(0.4F + event.getEntity().getRandom().nextFloat() * 0.2F);
                            double f3 = event.getEntity().getRandomZ(0.7F);
                            Vec3 motion = attacker.getEyePosition().subtract(f1, f2, f3).normalize().scale(0.2F);
                            ((ServerLevel) event.getEntity().level()).sendParticles(DIParticleRegistry.VAMPIRE.get(), f1, f2, f3, 1, motion.x, motion.y, motion.z, 0.2F);
                        }
                    }
                }
            }
            int warpingBiteLevel = getDiEffectLevel(attacker, "warping_bite");
            if (!event.getEntity().level().isClientSide
                    && warpingBiteLevel > 0
                    && isBaseAttackHit(attacker, event)
                    && event.getEntity() instanceof Enemy
                    && !TameableUtils.shouldBlockOffensiveDiTarget(attacker, event.getEntity())
                    && attacker.getRandom().nextDouble() < warpingBiteProcChance(warpingBiteLevel, event.getEntity())) {
                int attempts = warpingBiteAttempts(warpingBiteLevel);
                double horizontalRange = warpingBiteHorizontalRange(warpingBiteLevel);
                int verticalRange = warpingBiteVerticalRange(warpingBiteLevel);
                for (int i = 0; i < attempts; ++i) {
                    double d3 = event.getEntity().getX() + (attacker.getRandom().nextDouble() - 0.5D) * horizontalRange;
                    double d4 = Mth.clamp(event.getEntity().getY() + (double) (attacker.getRandom().nextInt(verticalRange) - verticalRange / 2), event.getEntity().level().getMinBuildHeight(), event.getEntity().level().getMinBuildHeight() + ((ServerLevel) event.getEntity().level()).getLogicalHeight() - 1);
                    double d5 = event.getEntity().getZ() + (attacker.getRandom().nextDouble() - 0.5D) * horizontalRange;
                    if (event.getEntity().randomTeleport(d3, d4, d5, true)) {
                        SoundEvent soundevent = event.getEntity() instanceof Fox ? SoundEvents.FOX_TELEPORT : SoundEvents.CHORUS_FRUIT_TELEPORT;
                        event.getEntity().playSound(soundevent, 1.0F, 1.0F);
                        break;
                    }
                }
            }
        }
        if (!event.isCanceled()) {
            List<LivingEntity> nearbyHealers = TameableUtils.getNearbyHealers(event.getEntity());
            if (!nearbyHealers.isEmpty()) {
                for (LivingEntity healer : nearbyHealers) {
                    TameableUtils.setHealingAuraImpulse(healer, true);
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        debugShadowHandsDamageEvent(event);
        if (event.getSource().getEntity() instanceof LivingEntity && TameableUtils.isTamed(event.getSource().getEntity())) {
            LivingEntity pet = (LivingEntity) event.getSource().getEntity();
            if (TameableUtils.hasEnchant(pet, DIEnchantmentRegistry.IMMATURITY_CURSE)) {
                event.setAmount((float) Math.ceil(event.getAmount() * 0.7F));
            }
        }
    }

    @SubscribeEvent
    public void onCollarProtectionDamageReduction(LivingHurtEvent event) {
        if (!TameableUtils.isTamed(event.getEntity())) {
            return;
        }
        // Protection is now represented as direct armor/armor_toughness scaling on collar tags.
        // Keep vanilla armor formula as the single source of truth and avoid extra EPF-style reduction.
    }

    @SubscribeEvent
    public void onLivingDie(LivingDeathEvent event) {
        if (TameableUtils.isTamed(event.getEntity()) && !TameableUtils.isZombiePet(event.getEntity())) {
            if (!(event.getEntity() instanceof TamableAnimal)) {
                Entity owner = TameableUtils.getOwnerOf(event.getEntity());
                if (!event.getEntity().level().isClientSide && event.getEntity().level().getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES) && owner instanceof ServerPlayer) {
                    owner.sendSystemMessage(event.getEntity().getCombatTracker().getDeathMessage());
                }
            }
            if (event.getEntity() instanceof Mob mob && event.getEntity().level().getDifficulty() != Difficulty.PEACEFUL && TameableUtils.hasEnchant(mob, DIEnchantmentRegistry.UNDEAD_CURSE)) {
                Mob zombieCopy = (Mob) mob.getType().create(mob.level());
                int id = zombieCopy.getId();
                Entity owner = TameableUtils.getOwnerOf(mob);
                CompoundTag livingNbt = new CompoundTag();
                mob.addAdditionalSaveData(livingNbt);
                livingNbt.putString("DeathLootTable", BuiltInLootTables.EMPTY.toString());
                zombieCopy.readAdditionalSaveData(livingNbt);
                zombieCopy.setId(id);
                if (zombieCopy instanceof TamableAnimal tamed) {
                    tamed.setTame(false);
                    tamed.setOwnerUUID(null);
                    tamed.setOrderedToSit(false);
                }
                if (zombieCopy instanceof ModifedToBeTameable tameable) {
                    tameable.setTame(false);
                    tameable.setTameOwnerUUID(null);
                }
                if (zombieCopy instanceof IComandableMob commandableMob) {
                    commandableMob.setCommand(0);
                }
                zombieCopy.copyPosition(mob);
                zombieCopy.setTarget(owner instanceof Player && !((Player) owner).isCreative() ? (Player) owner : mob.level().getNearestPlayer(ZOMBIE_TARGET, mob));
                mob.level().addFreshEntity(zombieCopy);
                zombieCopy.setHealth(zombieCopy.getMaxHealth());
                TameableUtils.setZombiePet(zombieCopy, true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityMount(EntityMountEvent event) {
        if (event.getEntityBeingMounted() instanceof GiantBubbleEntity && event.isDismounting() && event.getEntityBeingMounted().isAlive()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDuelRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!isDuelRestrictedPlayer(event.getEntity())) {
            return;
        }
        if (isAllowedDuelItemUse(event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public void onDuelRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!isDuelRestrictedPlayer(event.getEntity())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
    }

    @SubscribeEvent
    public void onDuelLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!isDuelRestrictedPlayer(event.getEntity())) {
            return;
        }
        event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHungryTameEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (blockHungryTameCommandInteract(event.getEntity(), event.getHand(), event.getTarget(), event)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHungryTameEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (blockHungryTameCommandInteract(event.getEntity(), event.getHand(), event.getTarget(), event)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private boolean blockHungryTameCommandInteract(Player player, InteractionHand hand, Entity target, PlayerInteractEvent event) {
        if (player == null || target == null || event == null || player.level().isClientSide || hand != InteractionHand.MAIN_HAND) {
            return false;
        }
        if (!(target instanceof TamableAnimal tame) || !tame.isTame() || !player.getUUID().equals(tame.getOwnerUUID())) {
            return false;
        }
        if (!TameCommands.isHungerBlockingMovement(tame)) {
            return false;
        }
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        return held.isEmpty() || !held.getItem().isEdible();
    }

    @SubscribeEvent
    public void onDuelEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        Player player = event.getEntity();
        if (isDuelRestrictedPlayer(player) || isOwnerInteractingWithDuelTame(player, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (handleHungerInventoryShortcut(player, event.getHand(), event.getTarget(), event)) {
            return;
        }
    }

    @SubscribeEvent
    public void onInteractWithEntity(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (isDuelRestrictedPlayer(player)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (isOwnerInteractingWithDuelTame(player, event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        Entity entity = event.getTarget();
        ItemStack stack = event.getItemStack();
        if (handleHungerInventoryShortcut(player, event.getHand(), entity, event)) {
            return;
        }
        if (TameableUtils.isTamed(event.getTarget())) {
            if (event.getItemStack().is(DIItemRegistry.DEED_OF_OWNERSHIP.get())) {
                CompoundTag tag = stack.getTag();
                boolean unbound = !DeedOfOwnershipItem.isBound(event.getItemStack());
                Entity currentOwner = TameableUtils.getOwnerOf(entity);
                if (TameableUtils.isTamed(entity) && currentOwner != null && currentOwner.equals(player) && unbound) {
                    CompoundTag newTag = new CompoundTag();
                    newTag.putBoolean("HasBoundEntity", true);
                    newTag.putUUID("BoundEntity", entity.getUUID());
                    newTag.putString("BoundEntityName", entity.getName().getString());
                    stack.setTag(newTag);
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
                if (TameableUtils.isTamed(entity) && tag != null && tag.getBoolean("HasBoundEntity") && tag.getUUID("BoundEntity") != null) {
                    UUID fromItem = tag.getUUID("BoundEntity");
                    if (entity.getUUID().equals(fromItem)) {
                        player.getCooldowns().addCooldown(stack.getItem(), 5);
                        TameableUtils.setOwnerUUIDOf(entity, player.getUUID());
                        TameRegistry.transferOwnership(entity.getUUID(), player.getUUID());
                        player.displayClientMessage(Component.translatable("message.domesticationinnovation.set_owner", player.getName(), entity.getName()), true);
                        if (currentOwner instanceof Player && !currentOwner.equals(player)) {
                            ((Player) currentOwner).displayClientMessage(Component.translatable("message.domesticationinnovation.set_owner", player.getName(), entity.getName()), true);
                        }
                        stack.setTag(new CompoundTag());
                        if (!player.isCreative()) {
                            stack.shrink(1);
                        }
                        event.setCanceled(true);
                        event.setCancellationResult(InteractionResult.SUCCESS);
                    }
                }

            }
        }
        if (TameableUtils.couldBeTamed(event.getTarget()) && TameableUtils.isZombiePet((LivingEntity) event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        if (event.getTarget() instanceof LivingEntity living && TameableUtils.isTamed(entity) && TameableUtils.hasEnchant(living, DIEnchantmentRegistry.GLUTTONOUS)) {
            if (stack.getItem().isEdible() && living.getHealth() < living.getMaxHealth() && stack.getItem().getFoodProperties() != null) {
                living.heal((float) Math.floor(stack.getItem().getFoodProperties().getNutrition() * 1.5F));
                if (!event.getEntity().isCreative()) {
                    stack.shrink(1);
                }
                living.playSound(living.getRandom().nextBoolean() ? SoundEvents.PLAYER_BURP : SoundEvents.GENERIC_EAT, 1F, living.getVoicePitch());
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
        if (event.getTarget() instanceof Rabbit rabbit && DomesticationMod.CONFIG.tameableRabbit.get()) {
            if (stack.getItem() == Items.HAY_BLOCK) {
                if (TameableUtils.isTamed(rabbit) && rabbit.getHealth() < rabbit.getMaxHealth()) {
                    rabbit.heal(3);
                    if (!event.getEntity().isCreative()) {
                        stack.shrink(1);
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
                if (!TameableUtils.isTamed(rabbit) && !rabbit.level().isClientSide) {
                    if (!event.getEntity().isCreative()) {
                        stack.shrink(1);
                    }
                    rabbit.playSound(SoundEvents.FOX_EAT);
                    if (rabbit.getRandom().nextBoolean()) {
                        for (int i = 0; i < 3; ++i) {
                            double d0 = rabbit.getRandom().nextGaussian() * 0.02D;
                            double d1 = rabbit.getRandom().nextGaussian() * 0.02D;
                            double d2 = rabbit.getRandom().nextGaussian() * 0.02D;
                            ((ServerLevel) rabbit.level()).sendParticles(ParticleTypes.HEART, rabbit.getRandomX(1.0D), rabbit.getRandomY() + 0.5D, rabbit.getRandomZ(1.0D), 3, d0, d1, d2, 0.02F);
                        }
                        ((ModifedToBeTameable) rabbit).setTame(true);
                        ((ModifedToBeTameable) rabbit).setTameOwnerUUID(event.getEntity().getUUID());
                        ((IComandableMob) rabbit).setCommand(1);
                    } else {
                        for (int i = 0; i < 3; ++i) {
                            double d0 = rabbit.getRandom().nextGaussian() * 0.02D;
                            double d1 = rabbit.getRandom().nextGaussian() * 0.02D;
                            double d2 = rabbit.getRandom().nextGaussian() * 0.02D;
                            ((ServerLevel) rabbit.level()).sendParticles(ParticleTypes.SMOKE, rabbit.getRandomX(1.0D), rabbit.getRandomY() + 0.5D, rabbit.getRandomZ(1.0D), 3, d0, d1, d2, 0.02F);
                        }
                    }
                    event.setCanceled(true);
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    return;
                }
            }
            if (TameableUtils.isTamed(rabbit) && TameableUtils.isPetOf(event.getEntity(), rabbit)) {
                ((IComandableMob) rabbit).playerSetCommand(event.getEntity(), rabbit);
            }
        }
        if (event.getTarget() instanceof LivingEntity living && TameableUtils.isPetOf(event.getEntity(), entity) && !living.getType().is(DITagRegistry.REFUSES_COLLAR_TAGS)) {
            if (event.getItemStack().is(DIItemRegistry.COLLAR_TAG.get()) && DomesticationMod.CONFIG.collarTag.get()) {
                if (!event.getEntity().level().isClientSide && living.isAlive()) {
                    Map<Enchantment, Integer> itemEnchantments = readAnvilEnchantments(stack);
                    Map<ResourceLocation, Integer> entityEnchantments = TameableUtils.getEnchants(living);
                    if (stack.hasCustomHoverName() && living.hasCustomName() && stack.getHoverName().equals(living.getCustomName())) {
                        boolean hasSameEnchants = itemEnchantments.isEmpty();
                        if (entityEnchantments != null) {
                            hasSameEnchants = true;
                            for (Map.Entry<Enchantment, Integer> itemEntry : itemEnchantments.entrySet()) {
                                ResourceLocation name = ForgeRegistries.ENCHANTMENTS.getKey(itemEntry.getKey());
                                if (entityEnchantments.get(name) == null || !entityEnchantments.get(name).equals(itemEntry.getValue())) {
                                    hasSameEnchants = false;
                                }
                            }
                        }
                        if (hasSameEnchants) {
                            event.setCanceled(true);
                            event.setCancellationResult(InteractionResult.FAIL);
                            return;
                        }
                    }
                    if (stack.hasCustomHoverName()) {
                        living.setCustomName(stack.getHoverName());
                    }
                    if (!event.getEntity().isCreative()) {
                        stack.shrink(1);
                    }
                    blockCollarTick(living);
                    if (TameableUtils.hasCollar(living)) {
                        ItemStack collarFrom = new ItemStack(DIItemRegistry.COLLAR_TAG.get());
                        if (entityEnchantments != null) {
                            collarFrom.getOrCreateTag();
                            if (!collarFrom.getTag().contains("Enchantments", 9)) {
                                collarFrom.getTag().put("Enchantments", new ListTag());
                            }

                            ListTag listtag = collarFrom.getTag().getList("Enchantments", 10);
                            for (Map.Entry<ResourceLocation, Integer> entry : entityEnchantments.entrySet()) {
                                listtag.add(EnchantmentHelper.storeEnchantment(entry.getKey(), entry.getValue()));
                            }
                        } else {
                            collarFrom.setTag(null);
                        }
                        living.spawnAtLocation(collarFrom);
                    }
                    living.playSound(DISoundRegistry.COLLAR_TAG.get(), 1, 1);
                    if (itemEnchantments.isEmpty()) {
                        TameableUtils.clearEnchants(living);
                    } else {
                        ListTag listTag = new ListTag();
                        for (Map.Entry<Enchantment, Integer> entry : itemEnchantments.entrySet()) {
                            TameableUtils.addEnchant(living, new EnchantmentInstance(entry.getKey(), entry.getValue()), listTag);
                        }
                    }
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (isDuelRestrictedPlayer(event.getPlayer())) {
            event.setCanceled(true);
            return;
        }
        if (event.getState().getBlock() instanceof PetBedBlock) {
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof PetBedBlockEntity entity1) {
                entity1.removeAllRequestsFor(event.getPlayer());
                entity1.resetBedsForNearbyPets();
            }
        }
    }

    @SubscribeEvent
    public void onDuelBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player && isDuelRestrictedPlayer(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDuelItemToss(ItemTossEvent event) {
        if (event.getPlayer() != null && isDuelRestrictedPlayer(event.getPlayer())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onDuelItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || !isDuelRestrictedPlayer(player)) {
            return;
        }
        if (isAllowedDuelItemUse(event.getItem())) {
            return;
        }
        event.setCanceled(true);
    }

    private static boolean isDuelRestrictedPlayer(Player player) {
        return player != null && !player.level().isClientSide && TameDuelManager.isEntityInDuel(player.getUUID());
    }

    private boolean handleHungerInventoryShortcut(Player player, InteractionHand hand, Entity target, PlayerInteractEvent event) {
        if (player == null || target == null || event == null) {
            return false;
        }
        if (hand != InteractionHand.MAIN_HAND || player.level().isClientSide || !player.isShiftKeyDown()) {
            return false;
        }
        if (!player.getMainHandItem().isEmpty()) {
            return false;
        }
        if (!(target instanceof TamableAnimal tame) || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (!TameCommands.openHungerInventory(serverPlayer, tame)) {
            return false;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    private static boolean isOwnerInteractingWithDuelTame(Player player, Entity target) {
        if (player == null || target == null || player.level().isClientSide) {
            return false;
        }
        if (!TameableUtils.isTamed(target) || !TameDuelManager.isEntityInDuel(target.getUUID())) {
            return false;
        }
        UUID ownerId = TameableUtils.getOwnerUUIDOf(target);
        return ownerId != null && ownerId.equals(player.getUUID());
    }

    private static boolean isAllowedDuelItemUse(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof ProjectileWeaponItem;
    }

    @SubscribeEvent
    public void onEntityJoinWorld(MobSpawnEvent.FinalizeSpawn event) {
        try {
            if (event.getEntity() != null && event.getEntity() instanceof Ravager && DomesticationMod.CONFIG.rabbitsScareRavagers.get()) {
                Ravager ravager = (Ravager) event.getEntity();
                ravager.goalSelector.addGoal(4, new AvoidEntityGoal(ravager, Rabbit.class, 13.0F, 1.5D, 2.0D, EntitySelector.NO_SPECTATORS));
            }
        } catch (Exception e) {
            DomesticationMod.LOGGER.warn("could not add ai tasks to ravager");
        }
    }

    @SubscribeEvent
    public void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == DIVillagerRegistry.ANIMAL_TAMER.get()) {
            List<VillagerTrades.ItemListing> level1 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level2 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level3 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level4 = new ArrayList<>();
            List<VillagerTrades.ItemListing> level5 = new ArrayList<>();
            level1.add(new BuyingItemTrade(Items.TROPICAL_FISH, 10, 2, 10, 2));
            level1.add(new SellingItemTrade(Items.BONE, 3, 10, 6, 4));
            level1.add(new BuyingItemTrade(Items.HAY_BLOCK, 7, 1, 9, 1));
            level1.add(new SellingItemTrade(Items.COD, 2, 7, 6, 3));
            level1.add(new SellingItemTrade(Items.EGG, 4, 2, 9, 3));
            level1.add(new SellingItemTrade(DIItemRegistry.FEATHER_ON_A_STICK.get(), 3, 1, 2, 3));
            level2.add(new SellingItemTrade(Items.TROPICAL_FISH_BUCKET, 2, 1, 6, 7));
            level2.add(new BuyingItemTrade(DIItemRegistry.COLLAR_TAG.get(), 5, 1, 12, 7));
            level2.add(new SellingItemTrade(Items.APPLE, 4, 12, 3, 7));
            level2.add(new SellingOneOfTheseItemsTrade(ImmutableSet.of(
                    DIBlockRegistry.WHITE_PET_BED.get(), DIBlockRegistry.ORANGE_PET_BED.get(), DIBlockRegistry.MAGENTA_PET_BED.get(), DIBlockRegistry.LIGHT_BLUE_PET_BED.get(), DIBlockRegistry.YELLOW_PET_BED.get(), DIBlockRegistry.LIME_PET_BED.get(), DIBlockRegistry.PINK_PET_BED.get(), DIBlockRegistry.GRAY_PET_BED.get(), DIBlockRegistry.LIGHT_GRAY_PET_BED.get(), DIBlockRegistry.CYAN_PET_BED.get(), DIBlockRegistry.PURPLE_PET_BED.get(), DIBlockRegistry.BLUE_PET_BED.get(), DIBlockRegistry.BROWN_PET_BED.get(), DIBlockRegistry.GREEN_PET_BED.get(), DIBlockRegistry.RED_PET_BED.get(), DIBlockRegistry.BLACK_PET_BED.get()
            ), 2, 1, 6, 7));
            level2.add(new SellingItemTrade(DIItemRegistry.DEED_OF_OWNERSHIP.get(), 3, 1, 2, 7));
            level3.add(new SellingItemTrade(DIItemRegistry.ROTTEN_APPLE.get(), 4, 1, 1, 10));
            level3.add(new SellingItemTrade(Items.CARROT_ON_A_STICK, 3, 1, 2, 10));
            level3.add(new SellingItemTrade(Items.LEAD, 3, 2, 5, 10));
            level3.add(new SellingItemTrade(Items.LEATHER_HORSE_ARMOR, 4, 1, 3, 11));
            level3.add(new SellingItemTrade(DIBlockRegistry.DRUM.get(), 2, 3, 7, 11));
            level3.add(new SellingItemTrade(Items.TADPOLE_BUCKET, 6, 1, 4, 13));
            level3.add(new EnchantItemTrade(DIItemRegistry.COLLAR_TAG.get(), 20, 2, 8, 3, 10));
            level4.add(new SellingItemTrade(Items.IRON_HORSE_ARMOR, 8, 1, 2, 15));
            level4.add(new SellingItemTrade(Items.AXOLOTL_BUCKET, 11, 1, 2, 15));
            level4.add(new SellingItemTrade(Items.TURTLE_EGG, 26, 1, 2, 15));
            level4.add(new EnchantItemTrade(DIItemRegistry.COLLAR_TAG.get(), 40, 3, 18, 3, 15));
            level5.add(new SellingItemTrade(Items.GOLDEN_HORSE_ARMOR, 13, 1, 1, 18));
            level5.add(new SellingItemTrade(Items.SCUTE, 21, 1, 3, 18));
            level5.add(new EnchantItemTrade(DIItemRegistry.COLLAR_TAG.get(), 50, 4, 38, 3, 20));
            event.getTrades().put(1, level1);
            event.getTrades().put(2, level2);
            event.getTrades().put(3, level3);
            event.getTrades().put(4, level4);
            event.getTrades().put(5, level5);
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (TameableUtils.isTamed(event.getEntity()) && TameableUtils.getPetBedPos(event.getEntity()) != null) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Start event) {
        if (explosionOwnedByTamedEntity(event)) {
            return;
        }
        float dist = 30;
        Vec3 center = event.getExplosion().getPosition();
        Vec3 bottom = center.add(-dist, -dist, -dist);
        Vec3 top = center.add(dist, dist, dist);
        Predicate<Entity> defusal = (animal) -> {
            if (!(animal instanceof TamableAnimal tame) || !tame.isTame()) {
                return false;
            }
            TameData data = TameRegistry.get(tame.getUUID());
            return data != null && LevelSystem.hasAbility(data, "defusal");
        };

        TamableAnimal defuserHit = null;
        TameData defuserData = null;
        int defusalLevel = 0;
        long now = event.getLevel().getGameTime();
        for (LivingEntity entity : event.getLevel().getEntitiesOfClass(LivingEntity.class, new AABB(bottom, top), EntitySelector.NO_SPECTATORS.and(defusal))) {
            if (!(entity instanceof TamableAnimal tame)) {
                continue;
            }
            TameData data = TameRegistry.get(tame.getUUID());
            if (data == null) {
                continue;
            }
            int level = Math.max(1, LevelSystem.getAbilityLevel(data, "defusal"));
            long readyAt = data.cooldowns.getOrDefault("defusal_tick", 0L);
            if (now < readyAt) {
                continue;
            }

            double range = 10.0D + (level / 3) * 10.0D; // every 3rd level increases range
            if (entity.distanceToSqr(center) <= range * range) {
                defuserHit = tame;
                defuserData = data;
                defusalLevel = level;
                break;
            }
        }

        if (defuserHit != null && defuserData != null) {
            event.setCanceled(true);
            float pitch = 1.5F + new Random().nextFloat();
            event.getLevel().playSound(null, center.x, center.y, center.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, pitch);
            if (event.getLevel() instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 5; i++) {
                    serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y + 1.0F, center.z, 5, 0, 0F, 0, 0.2F);
                }
            }
            int cooldownSeconds = Math.max(1, 5 - Math.max(0, defusalLevel - 1));
            defuserData.cooldowns.put("defusal_tick", now + cooldownSeconds * 20L);
            TameRegistry.markDirty();
            debugDiAbilityUse(defuserHit, "defusal");
        }
    }

    private static boolean explosionOwnedByTamedEntity(ExplosionEvent.Start event) {
        if (event == null || event.getExplosion() == null) {
            return false;
        }
        DamageSource damageSource = event.getExplosion().getDamageSource();
        if (damageSource == null) {
            return false;
        }
        Entity source = damageSource.getEntity();
        return source instanceof TamableAnimal tame && tame.isTame();
    }

    private static void enforceNoGriefMutantCreeperMinion(Entity entity) {
        if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || tame.level().isClientSide) {
            return;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        if (key == null || !"mutantmonsters".equals(key.getNamespace()) || !"creeper_minion".equals(key.getPath())) {
            return;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        if (data == null) {
            UUID tlId = TameData.getTlId(tame);
            if (tlId != null) {
                data = TameRegistry.getByTlId(tlId);
            }
        }
        try {
            Method method = tame.getClass().getMethod("setDestroyBlocks", boolean.class);
            method.setAccessible(true);
            method.invoke(tame, false);
        } catch (Throwable ignored) {
        }
        try {
            Method getExplosionRadius = tame.getClass().getMethod("getExplosionRadius");
            Method setExplosionRadius = tame.getClass().getMethod("setExplosionRadius", float.class);
            getExplosionRadius.setAccessible(true);
            setExplosionRadius.setAccessible(true);
            Object radiusObj = getExplosionRadius.invoke(tame);
            if (!(radiusObj instanceof Number radiusNumber)) {
                return;
            }
            float currentRadius = radiusNumber.floatValue();
            CompoundTag tag = tame.getPersistentData();
            float previousBonus = tag.getFloat("diMutantMinionAppliedRadiusBonus");
            float baseRadius = Math.max(0.0F, currentRadius - previousBonus);
            float newBonus = data == null ? 0.0F : (float) Math.max(0.0D, data.bonusKnockback);
            float desiredRadius = Math.max(0.1F, baseRadius + newBonus);
            if (Math.abs(desiredRadius - currentRadius) > 0.001F) {
                setExplosionRadius.invoke(tame, desiredRadius);
            }
            tag.putFloat("diMutantMinionAppliedRadiusBonus", newBonus);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public void onUpdateAnvil(AnvilUpdateEvent event) {
        if (event.getLeft().is(DIItemRegistry.COLLAR_TAG.get())) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            event.setMaterialCost(0);
        }
    }

    private static Map<Enchantment, Integer> readAnvilEnchantments(ItemStack stack) {
        Map<Enchantment, Integer> enchants = new HashMap<>(EnchantmentHelper.getEnchantments(stack));
        if (stack.is(Items.ENCHANTED_BOOK)) {
            Map<Enchantment, Integer> stored = EnchantmentHelper.deserializeEnchantments(EnchantedBookItem.getEnchantments(stack));
            for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
                int prev = enchants.getOrDefault(entry.getKey(), 0);
                if (entry.getValue() > prev) {
                    enchants.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return enchants;
    }

    private static int diList(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<Map.Entry<UUID, CompoundTag>> tames = getOwnedTames(source, player.getUUID());
        if (tames.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no registered tames."));
            return 0;
        }
        tames.sort((a, b) -> {
            int levelA = Math.max(1, a.getValue().getInt("level"));
            int levelB = Math.max(1, b.getValue().getInt("level"));
            if (levelA != levelB) {
                return Integer.compare(levelB, levelA);
            }
            String nameA = tameName(a.getValue()).toLowerCase(Locale.ROOT);
            String nameB = tameName(b.getValue()).toLowerCase(Locale.ROOT);
            return nameA.compareTo(nameB);
        });
        player.sendSystemMessage(Component.literal("---- Your Tames ----"));
        for (int i = 0; i < tames.size(); i++) {
            CompoundTag d = tames.get(i).getValue();
            int level = Math.max(1, d.getInt("level"));
            int kills = Math.max(0, d.getInt("kills"));
            int assists = Math.max(0, d.getInt("assists"));
            int deaths = Math.max(0, d.getInt("deaths"));
            long days = daysAlive(source, d);
            player.sendSystemMessage(Component.literal(
                    (i + 1) + ". [" + level + "] " + tameName(d) + " (" + kills + "/" + assists + "/" + days + "/" + deaths + ")"
            ));
        }
        return 1;
    }

    private static int diStat(CommandSourceStack source, String tameName) {
        ServerPlayer player = source.getPlayer();
        Map.Entry<UUID, CompoundTag> tame = findOwnedTameByName(source, player.getUUID(), tameName);
        if (tame == null) {
            player.sendSystemMessage(Component.literal("Pet not found."));
            return 0;
        }
        sendDiTameStats(source, player, tame.getKey(), tame.getValue(), true);
        return 1;
    }

    private static int diStrongest(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        List<Map.Entry<UUID, CompoundTag>> tames = getOwnedTames(source, player.getUUID());
        if (tames.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no registered tames."));
            return 0;
        }
        Map.Entry<UUID, CompoundTag> best = null;
        for (Map.Entry<UUID, CompoundTag> entry : tames) {
            if (best == null) {
                best = entry;
                continue;
            }
            CompoundTag a = best.getValue();
            CompoundTag b = entry.getValue();
            int levelA = Math.max(1, a.getInt("level"));
            int levelB = Math.max(1, b.getInt("level"));
            if (levelB > levelA) {
                best = entry;
                continue;
            }
            if (levelB == levelA) {
                int killsA = Math.max(0, a.getInt("kills"));
                int killsB = Math.max(0, b.getInt("kills"));
                if (killsB > killsA) {
                    best = entry;
                }
            }
        }
        sendDiTameStats(source, player, best.getKey(), best.getValue(), true);
        return 1;
    }

    private static void sendDiTameStats(CommandSourceStack source, ServerPlayer receiver, UUID tameUuid, CompoundTag d, boolean detailed) {
        receiver.sendSystemMessage(Component.literal("=== " + tameName(d) + " ===").withStyle(ChatFormatting.GOLD));
        receiver.sendSystemMessage(Component.literal("Lvl " + Math.max(1, d.getInt("level")) + "  XP " + Math.max(0, d.getInt("xp")) + "/" + Math.max(1, d.getInt("xpToNext"))).withStyle(ChatFormatting.YELLOW));
        receiver.sendSystemMessage(Component.literal("K " + Math.max(0, d.getInt("kills")) + "  A " + Math.max(0, d.getInt("assists")) + "  D " + Math.max(0, d.getInt("deaths"))).withStyle(ChatFormatting.AQUA));
        receiver.sendSystemMessage(Component.literal("Mode " + modeKeyById(d.getInt("mode")) + "  Class " + tameClassName(d)).withStyle(ChatFormatting.GREEN));
        receiver.sendSystemMessage(Component.literal("Group " + (d.getString("group").isBlank() ? "-" : d.getString("group"))).withStyle(ChatFormatting.DARK_GREEN));
        if (!detailed) {
            return;
        }

        TamableAnimal loaded = findLoadedOwnedTameByUuid(source, receiver.getUUID(), tameUuid);
        double actualHp = getBaseAttributeValue(loaded, Attributes.MAX_HEALTH);
        double actualDmg = getBaseAttributeValue(loaded, Attributes.ATTACK_DAMAGE);
        double actualSpd = getBaseAttributeValue(loaded, Attributes.MOVEMENT_SPEED);
        double actualArm = getBaseAttributeValue(loaded, Attributes.ARMOR);
        double actualTgh = getBaseAttributeValue(loaded, Attributes.ARMOR_TOUGHNESS);
        double actualKb = getBaseAttributeValue(loaded, Attributes.ATTACK_KNOCKBACK);
        double actualKbr = getBaseAttributeValue(loaded, Attributes.KNOCKBACK_RESISTANCE);

        receiver.sendSystemMessage(Component.literal("Actual HP" + fmtStat(actualHp) + " DMG" + fmtStat(actualDmg) + " SPD" + fmtStat(actualSpd)
                + " ARM" + fmtStat(actualArm) + " TGH" + fmtStat(actualTgh)
                + " KB" + fmtStat(actualKb) + " KBR" + fmtStat(actualKbr)).withStyle(ChatFormatting.GRAY));
        receiver.sendSystemMessage(Component.literal("Base HP+" + fmt(d.getDouble("bonusHealth")) + " DMG+" + fmt(d.getDouble("bonusDamage")) + " SPD+" + fmt(d.getDouble("bonusSpeed"))
                + " ARM+" + fmt(d.getDouble("bonusArmor")) + " TGH+" + fmt(d.getDouble("bonusArmorToughness"))
                + " KB+" + fmt(d.getDouble("bonusKnockback")) + " KBR+" + fmt(d.getDouble("bonusKnockbackResist"))).withStyle(ChatFormatting.GRAY));
        receiver.sendSystemMessage(Component.literal("Attributes: " + formatLevelsCompact(d.getCompound("attributeLevels"))).withStyle(ChatFormatting.LIGHT_PURPLE));
        receiver.sendSystemMessage(Component.literal("Abilities: " + formatLevelsCompact(d.getCompound("abilityLevels"))).withStyle(ChatFormatting.BLUE));
    }

    private static CompletableFuture<Suggestions> suggestOwnedTameNames(CommandSourceStack source, SuggestionsBuilder builder) {
        ServerPlayer player = source.getPlayer();
        for (Map.Entry<UUID, CompoundTag> entry : getOwnedTames(source, player.getUUID())) {
            builder.suggest(tameName(entry.getValue()));
        }
        return builder.buildFuture();
    }

    private static List<Map.Entry<UUID, CompoundTag>> getOwnedTames(CommandSourceStack source, UUID ownerUuid) {
        DITameProgressData table = DITameProgressData.get(source.getLevel());
        if (table == null) {
            return List.of();
        }
        List<Map.Entry<UUID, CompoundTag>> result = new ArrayList<>();
        for (Map.Entry<UUID, CompoundTag> entry : table.getEntriesCopy().entrySet()) {
            CompoundTag payload = entry.getValue();
            if (!payload.hasUUID("ownerUUID")) {
                continue;
            }
            if (ownerUuid.equals(payload.getUUID("ownerUUID"))) {
                result.add(new AbstractMap.SimpleEntry<>(entry.getKey(), payload));
            }
        }
        return result;
    }

    private static Map.Entry<UUID, CompoundTag> findOwnedTameByName(CommandSourceStack source, UUID ownerUuid, String name) {
        for (Map.Entry<UUID, CompoundTag> entry : getOwnedTames(source, ownerUuid)) {
            if (tameName(entry.getValue()).equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    private static String tameName(CompoundTag payload) {
        String name = payload.getString("name");
        return name == null || name.isBlank() ? "Unnamed" : name;
    }

    private static String tameClassName(CompoundTag payload) {
        String tameClass = payload.getString("tameClass");
        return tameClass == null || tameClass.isBlank() ? "-" : tameClass.toLowerCase(Locale.ROOT);
    }

    private static long daysAlive(CommandSourceStack source, CompoundTag data) {
        if (data == null || !data.contains("bornDayTime", Tag.TAG_LONG)) {
            return 0L;
        }
        long born = data.getLong("bornDayTime");
        if (born <= 0L || source.getServer() == null || source.getServer().overworld() == null) {
            return 0L;
        }
        long now = source.getServer().overworld().getDayTime();
        return Math.max(0L, now - born) / 24000L;
    }

    private static String modeKeyById(int id) {
        return switch (id) {
            case 1 -> "boss";
            case 2 -> "bodyguard";
            case 3 -> "monster_hunter";
            case 6 -> "passive";
            case 8 -> "aggressive";
            case 9, 4, 5, 7 -> "default_plus";
            default -> "default";
        };
    }

    private static TamableAnimal findLoadedOwnedTameByUuid(CommandSourceStack source, UUID owner, UUID tameUuid) {
        if (source.getServer() == null) {
            return null;
        }
        for (ServerLevel level : source.getServer().getAllLevels()) {
            Entity entity = level.getEntity(tameUuid);
            if (!(entity instanceof TamableAnimal tame) || !tame.isTame()) {
                continue;
            }
            if (!owner.equals(tame.getOwnerUUID())) {
                continue;
            }
            return tame;
        }
        return null;
    }

    private static double getBaseAttributeValue(TamableAnimal tame, Attribute attribute) {
        if (tame == null || attribute == null) {
            return Double.NaN;
        }
        AttributeInstance instance = tame.getAttribute(attribute);
        if (instance == null) {
            return Double.NaN;
        }
        return instance.getBaseValue();
    }

    private static String fmtStat(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "?";
        }
        return fmt(value);
    }

    private static String fmt(double value) {
        return Math.floor(value) == value ? Integer.toString((int) value) : String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatLevelsCompact(CompoundTag levels) {
        if (levels == null || levels.isEmpty()) {
            return "none";
        }
        List<String> keys = new ArrayList<>(levels.getAllKeys());
        keys.sort(String::compareToIgnoreCase);
        List<String> parts = new ArrayList<>();
        for (String key : keys) {
            parts.add(key + "[" + levels.getInt(key) + "]");
        }
        return String.join(", ", parts);
    }

    private static int showTlMigrationStatus(CommandSourceStack source) {
        TLMigrationImportData migrationData = TLMigrationImportData.get(source.getLevel());
        int migrationEntries = migrationData == null ? 0 : migrationData.size();
        DITameProgressData progressData = DITameProgressData.get(source.getLevel());
        int progressEntries = progressData == null ? 0 : progressData.size();
        source.sendSuccess(() -> Component.literal(
                "[DI] TL migration status: migrationEntries=" + migrationEntries
                        + ", progressEntries=" + progressEntries
                        + ", lastMode=" + tlMigrationLastMode
                        + ", scanned=" + tlMigrationLastScanned
                        + ", matchedPayload=" + tlMigrationLastMatchedPayload
                        + ", missingPayload=" + tlMigrationLastMissingPayload
                        + ", appliedNew=" + tlMigrationLastAppliedNew
                        + ", appliedUpdate=" + tlMigrationLastAppliedUpdate
                        + ", skippedStale=" + tlMigrationLastSkippedStale
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int applyTlMigrationToLoaded(CommandSourceStack source, boolean dryRun) {
        MinecraftServer server = source.getServer();
        TLMigrationImportData data = TLMigrationImportData.get(source.getLevel());
        if (data == null) {
            source.sendFailure(Component.literal("[DI] TL migration data store unavailable."));
            return 0;
        }

        long scanned = 0L;
        long matchedPayload = 0L;
        long missingPayload = 0L;
        long appliedNew = 0L;
        long appliedUpdate = 0L;
        long skippedStale = 0L;

        for (ServerLevel serverLevel : server.getAllLevels()) {
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof LivingEntity living) || !TameableUtils.couldBeTamed(living) || !TameableUtils.isTamed(living)) {
                    continue;
                }
                scanned++;
                CompoundTag payload = data.getPayload(living.getUUID());
                if (payload == null || payload.isEmpty()) {
                    missingPayload++;
                    continue;
                }
                payload = normalizeDIMigrationPayload(payload);
                matchedPayload++;
                CompoundTag current = TameableUtils.getDIProgressData(living);
                if (current == null || current.isEmpty()) {
                    if (!dryRun) {
                        TameableUtils.setDIProgressData(living, payload);
                    }
                    appliedNew++;
                    continue;
                }
                if (shouldReplaceMigrationPayload(current, payload)) {
                    if (!dryRun) {
                        TameableUtils.setDIProgressData(living, payload);
                    }
                    appliedUpdate++;
                } else {
                    skippedStale++;
                }
            }
        }

        tlMigrationLastMode = dryRun ? "dryrun" : "apply";
        tlMigrationLastScanned = scanned;
        tlMigrationLastMatchedPayload = matchedPayload;
        tlMigrationLastMissingPayload = missingPayload;
        tlMigrationLastAppliedNew = appliedNew;
        tlMigrationLastAppliedUpdate = appliedUpdate;
        tlMigrationLastSkippedStale = skippedStale;

        final long fScanned = scanned;
        final long fMatchedPayload = matchedPayload;
        final long fMissingPayload = missingPayload;
        final long fAppliedNew = appliedNew;
        final long fAppliedUpdate = appliedUpdate;
        final long fSkippedStale = skippedStale;
        source.sendSuccess(() -> Component.literal(
                "[DI] TL migration " + tlMigrationLastMode + ": scanned=" + fScanned
                        + ", matchedPayload=" + fMatchedPayload
                        + ", missingPayload=" + fMissingPayload
                        + ", appliedNew=" + fAppliedNew
                        + ", appliedUpdate=" + fAppliedUpdate
                        + ", skippedStale=" + fSkippedStale
        ), true);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean shouldReplaceMigrationPayload(CompoundTag current, CompoundTag incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return false;
        }
        if (current == null || current.isEmpty()) {
            return true;
        }
        long currentExport = getExportedAtGameTime(current);
        long incomingExport = getExportedAtGameTime(incoming);
        if (incomingExport > currentExport) {
            return true;
        }
        if (incomingExport < currentExport) {
            return false;
        }
        return !incoming.equals(current);
    }

    private static long getExportedAtGameTime(CompoundTag payload) {
        if (payload != null && payload.contains("exportedAtGameTime", Tag.TAG_LONG)) {
            return payload.getLong("exportedAtGameTime");
        }
        return Long.MIN_VALUE;
    }

    private static CompoundTag normalizeDIMigrationPayload(CompoundTag payload) {
        if (payload == null || payload.isEmpty()) {
            return payload;
        }
        CompoundTag normalized = payload.copy();

        // One-time migration for HP bonus scale change (legacy +1 HP bonus -> new +2 HP bonus).
        if (!normalized.getBoolean("diMigrationNormalizedV2")) {
            if (normalized.contains("bonusHealth", Tag.TAG_DOUBLE)) {
                normalized.putDouble("bonusHealth", normalized.getDouble("bonusHealth") * 2.0D);
            }
            if (normalized.contains("savedBonusHealth", Tag.TAG_DOUBLE)) {
                normalized.putDouble("savedBonusHealth", normalized.getDouble("savedBonusHealth") * 2.0D);
            }
            normalized.putBoolean("diMigrationNormalizedV2", true);
        }

        int level = Math.max(1, normalized.getInt("level"));
        ensureGuaranteedMilestoneAttributes(normalized, "attributeLevels", level);

        int savedLevel = Math.max(1, normalized.getInt("savedLevel"));
        ensureGuaranteedMilestoneAttributes(normalized, "savedAttributeLevels", savedLevel);
        return normalized;
    }

    private static void ensureGuaranteedMilestoneAttributes(CompoundTag payload, String tagKey, int level) {
        if (payload == null || tagKey == null || tagKey.isBlank()) return;
        CompoundTag attributes = payload.contains(tagKey, Tag.TAG_COMPOUND)
                ? payload.getCompound(tagKey).copy()
                : new CompoundTag();
        if (level >= 10) {
            attributes.putInt("tethered_teleport", Math.max(1, attributes.getInt("tethered_teleport")));
        }
        if (level >= 30) {
            attributes.putInt("gluttonous", Math.max(1, attributes.getInt("gluttonous")));
        }
        payload.put(tagKey, attributes);
    }

    private static void ensureDiProgressEntryForTame(LivingEntity tame, UUID forcedOwnerUuid) {
        if (tame.level().isClientSide || !TameableUtils.couldBeTamed(tame) || !TameableUtils.isTamed(tame)) {
            return;
        }
        DITameProgressData table = DITameProgressData.get(tame.level());
        if (table == null) {
            return;
        }
        UUID ownerUUID = forcedOwnerUuid != null ? forcedOwnerUuid : TameableUtils.getOwnerUUIDOf(tame);
        CompoundTag existing = table.getPayload(tame.getUUID());
        if (existing != null && !existing.isEmpty()) {
            boolean changed = false;
            if (ownerUUID != null && !existing.hasUUID("ownerUUID")) {
                existing.putUUID("ownerUUID", ownerUUID);
                changed = true;
            }
            if (ownerUUID != null) {
                String currentName = tameName(existing);
                String uniqueName = ensureUniqueNameForOwner(table, ownerUUID, tame.getUUID(), currentName);
                if (!uniqueName.equals(currentName)) {
                    existing.putString("name", uniqueName);
                    tame.setCustomName(Component.literal(uniqueName));
                    changed = true;
                }
            }
            if (changed) {
                TameableUtils.setDIProgressData(tame, existing);
            }
            return;
        }
        // Don't auto-create for legacy TL payload entities; those should be converted manually.
        if (TameableUtils.hasTamesLevelMigrationData(tame)) {
            return;
        }
        CompoundTag payload = createDefaultProgressPayload(tame, ownerUUID, table);
        TameableUtils.setDIProgressData(tame, payload);
        tame.setCustomName(Component.literal(tameName(payload)));
        if (ownerUUID != null && tame.level().getServer() != null) {
            ServerPlayer owner = tame.level().getServer().getPlayerList().getPlayer(ownerUUID);
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(
                        "§b" + tameName(payload) + " class assigned: §e" + payload.getString("tameClass")
                ));
            }
        }
    }

    private static CompoundTag createDefaultProgressPayload(LivingEntity tame, UUID ownerUUID, DITameProgressData table) {
        CompoundTag payload = new CompoundTag();
        String name = tame.hasCustomName() ? tame.getCustomName().getString() : tame.getName().getString();
        String type = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType()) == null
                ? tame.getType().toString()
                : ForgeRegistries.ENTITY_TYPES.getKey(tame.getType()).toString();
        int x = tame.blockPosition().getX();
        int y = tame.blockPosition().getY();
        int z = tame.blockPosition().getZ();
        long gameTime = tame.level().getGameTime();
        String dimension = tame.level().dimension().location().toString();

        payload.putString("sourceModId", "domesticationinnovation");
        payload.putInt("schemaVersion", 1);
        payload.putString("diSource", "di_extension");
        payload.putInt("diSchemaVersion", 1);

        payload.putUUID("uuid", tame.getUUID());
        if (ownerUUID != null) {
            payload.putUUID("ownerUUID", ownerUUID);
            name = ensureUniqueNameForOwner(table, ownerUUID, tame.getUUID(), name);
        }
        payload.putString("name", name);
        payload.putString("type", type);
        payload.putInt("level", 1);
        payload.putInt("xp", 0);
        payload.putInt("xpToNext", 50);
        payload.putLong("bornDayTime", tame.level().getDayTime());
        payload.putInt("kills", 0);
        payload.putInt("assists", 0);
        payload.putInt("deaths", 0);
        payload.putString("lastKnownDimension", dimension);
        payload.putInt("lastKnownX", x);
        payload.putInt("lastKnownY", y);
        payload.putInt("lastKnownZ", z);
        payload.putLong("lastKnownGameTime", gameTime);
        payload.putBoolean("hasHome", false);
        payload.putString("homeDimension", dimension);
        payload.putInt("homeX", x);
        payload.putInt("homeY", y);
        payload.putInt("homeZ", z);

        payload.putDouble("baseSpeed", tame.getAttribute(Attributes.MOVEMENT_SPEED) != null ? tame.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue() : 0.0D);
        payload.putDouble("bonusHealth", 0.0D);
        payload.putDouble("bonusDamage", 0.0D);
        payload.putDouble("bonusSpeed", 0.0D);
        payload.putDouble("bonusArmor", 0.0D);
        payload.putDouble("bonusArmorToughness", 0.0D);
        payload.putDouble("bonusKnockback", 0.0D);
        payload.putDouble("bonusKnockbackResist", 0.0D);

        payload.putInt("mode", 0);
        payload.putString("tameClass", randomTameClass(tame));
        payload.putString("group", "");
        payload.putBoolean("defendAllies", false);
        payload.putBoolean("escapeMode", true);
        payload.putBoolean("escapeActive", false);
        payload.putBoolean("hasProtectionZone", false);
        payload.putString("protectionDimension", "");
        payload.putInt("protectionX", 0);
        payload.putInt("protectionY", 0);
        payload.putInt("protectionZ", 0);
        payload.putInt("protectionRadius", 16);

        payload.put("abilities", new ListTag());
        payload.put("abilityLevels", new CompoundTag());
        payload.put("attributeLevels", new CompoundTag());
        payload.put("cooldowns", new CompoundTag());
        payload.put("entitySnapshot", new CompoundTag());

        payload.putBoolean("hasSavedProgress", false);
        payload.putInt("savedProgressCost", 0);
        payload.putInt("savedLevel", 1);
        payload.putInt("savedXp", 0);
        payload.putInt("savedXpToNext", 50);
        payload.putInt("savedKills", 0);
        payload.putInt("savedAssists", 0);
        payload.putDouble("savedBonusHealth", 0.0D);
        payload.putDouble("savedBonusDamage", 0.0D);
        payload.putDouble("savedBonusSpeed", 0.0D);
        payload.putDouble("savedBonusArmor", 0.0D);
        payload.putDouble("savedBonusArmorToughness", 0.0D);
        payload.putDouble("savedBonusKnockback", 0.0D);
        payload.putDouble("savedBonusKnockbackResist", 0.0D);
        payload.put("savedAbilities", new ListTag());
        payload.put("savedAbilityLevels", new CompoundTag());
        payload.put("savedAttributeLevels", new CompoundTag());

        payload.putLong("createdAtEpochMillis", System.currentTimeMillis());
        return payload;
    }

    private static String randomTameClass(LivingEntity tame) {
        TameClass[] values = TameClass.values();
        if (values.length == 0) {
            return "TANKER";
        }
        if (tame == null) {
            return TameClass.TANKER.name();
        }
        return values[tame.getRandom().nextInt(values.length)].name();
    }

    private static String ensureUniqueNameForOwner(DITameProgressData table, UUID ownerUuid, UUID selfUuid, String requestedName) {
        String baseName = requestedName == null || requestedName.isBlank() ? "Unnamed" : requestedName.trim();
        if (table == null || ownerUuid == null) {
            return baseName;
        }
        int highest = 0;
        boolean exactTaken = false;
        for (Map.Entry<UUID, CompoundTag> entry : table.getEntriesCopy().entrySet()) {
            if (selfUuid != null && selfUuid.equals(entry.getKey())) {
                continue;
            }
            CompoundTag payload = entry.getValue();
            if (!payload.hasUUID("ownerUUID") || !ownerUuid.equals(payload.getUUID("ownerUUID"))) {
                continue;
            }
            String existing = tameName(payload);
            Matcher matcher = NUMERIC_SUFFIX.matcher(existing);
            if (!matcher.matches()) {
                continue;
            }
            String existingBase = matcher.group(1) == null ? existing : matcher.group(1).trim();
            if (!existingBase.equalsIgnoreCase(baseName)) {
                continue;
            }
            String suffix = matcher.group(2);
            if (suffix == null || suffix.isBlank()) {
                exactTaken = true;
                highest = Math.max(highest, 1);
            } else {
                try {
                    highest = Math.max(highest, Integer.parseInt(suffix));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (!exactTaken && highest == 0) {
            return baseName;
        }
        int next = Math.max(2, highest + 1);
        return baseName + " " + next;
    }

    private static int showDiProgress(CommandSourceStack source, UUID tameUuid) {
        DITameProgressData table = DITameProgressData.get(source.getLevel());
        CompoundTag payload = table == null ? null : table.getPayload(tameUuid);
        if (payload == null || payload.isEmpty()) {
            source.sendFailure(Component.literal("[DI] No progress entry for tame " + tameUuid + "."));
            return 0;
        }
        int level = Math.max(1, payload.getInt("level"));
        int xp = Math.max(0, payload.getInt("xp"));
        int xpToNext = Math.max(1, payload.contains("xpToNext", Tag.TAG_INT) ? payload.getInt("xpToNext") : 50);
        int kills = Math.max(0, payload.getInt("kills"));
        int assists = Math.max(0, payload.getInt("assists"));
        int deaths = Math.max(0, payload.getInt("deaths"));
        source.sendSuccess(() -> Component.literal(
                "[DI] Progress " + tameUuid
                        + ": level=" + level
                        + ", xp=" + xp + "/" + xpToNext
                        + ", kills=" + kills
                        + ", assists=" + assists
                        + ", deaths=" + deaths
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void debugDiAbilityUse(LivingEntity entity, String abilityId) {
        if (entity == null || abilityId == null || abilityId.isBlank()) {
            return;
        }
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID ownerId = TameableUtils.getOwnerUUIDOf(entity);
        if (ownerId == null || !PlayerDebugSettings.abilityUsed(ownerId)) {
            return;
        }
        ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        String tameName = entity.hasCustomName() && entity.getCustomName() != null
                ? entity.getCustomName().getString()
                : entity.getName().getString();
        owner.sendSystemMessage(Component.literal(tameName + ": " + abilityId));
    }

    private static void debugShadowHandsStrike(Mob mob, Entity target, float amount) {
        PendingShadowHandsDebug debug = createShadowHandsDebug(mob, target, amount);
        if (debug == null) {
            return;
        }
        PENDING_SHADOW_HANDS_DEBUG.put(debug.targetId(), debug);
        sendShadowHandsDebug(debug.ownerId(), debug.tameName() + " -> " + debug.targetName()
                + " [strike] dmg=" + fmt(amount)
                + ", direct=" + simpleEntityName(target)
                + ", dist=" + fmt(mob.distanceTo(target)));
    }

    private static void debugShadowHandsHurtResult(Mob mob, Entity target, float amount, boolean dealt) {
        PendingShadowHandsDebug debug = createShadowHandsDebug(mob, target, amount);
        if (debug == null) {
            return;
        }
        if (!dealt) {
            sendShadowHandsDebug(debug.ownerId(), debug.tameName() + " -> " + debug.targetName()
                    + " [hurt=false] invul=" + (target instanceof LivingEntity living ? living.invulnerableTime : -1)
                    + ", alive=" + target.isAlive()
                    + ", removed=" + target.isRemoved());
            PENDING_SHADOW_HANDS_DEBUG.remove(debug.targetId());
        }
    }

    private static void debugShadowHandsState(Mob mob, Entity target, String stage) {
        PendingShadowHandsDebug debug = createShadowHandsDebug(mob, target, 0.0F);
        if (debug == null) {
            return;
        }
        sendShadowHandsDebug(debug.ownerId(), debug.tameName() + " -> " + debug.targetName() + " [" + stage + "]");
    }

    private static void debugShadowHandsAttackEvent(LivingAttackEvent event) {
        PendingShadowHandsDebug debug = findShadowHandsDebug(event.getEntity(), event.getSource());
        if (debug == null) {
            return;
        }
        sendShadowHandsDebug(debug.ownerId(), debug.tameName() + " -> " + debug.targetName()
                + " [attackEvent] canceled=" + event.isCanceled()
                + ", amount=" + fmt(event.getAmount())
                + ", source=" + shadowHandsDamageSource(event.getSource()));
    }

    private static void debugShadowHandsDamageEvent(LivingDamageEvent event) {
        PendingShadowHandsDebug debug = findShadowHandsDebug(event.getEntity(), event.getSource());
        if (debug == null) {
            return;
        }
        sendShadowHandsDebug(debug.ownerId(), debug.tameName() + " -> " + debug.targetName()
                + " [damageEvent] canceled=" + event.isCanceled()
                + ", amount=" + fmt(event.getAmount()));
        PENDING_SHADOW_HANDS_DEBUG.remove(debug.targetId());
    }

    private static PendingShadowHandsDebug findShadowHandsDebug(LivingEntity target, DamageSource source) {
        if (target == null || source == null) {
            return null;
        }
        PendingShadowHandsDebug debug = PENDING_SHADOW_HANDS_DEBUG.get(target.getUUID());
        if (debug == null) {
            return null;
        }
        if (!(target.level() instanceof ServerLevel level) || level.getGameTime() - debug.gameTime() > 2L) {
            PENDING_SHADOW_HANDS_DEBUG.remove(target.getUUID());
            return null;
        }
        Entity attacker = source.getEntity();
        if (attacker == null || !debug.attackerId().equals(attacker.getUUID())) {
            return null;
        }
        return debug;
    }

    private static PendingShadowHandsDebug createShadowHandsDebug(Mob mob, Entity target, float amount) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return null;
        }
        UUID ownerId = TameableUtils.getOwnerUUIDOf(mob);
        if (ownerId == null || !PlayerDebugSettings.shadowHands(ownerId)) {
            return null;
        }
        String tameName = mob.hasCustomName() && mob.getCustomName() != null ? mob.getCustomName().getString() : mob.getName().getString();
        String targetName = target.hasCustomName() && target.getCustomName() != null ? target.getCustomName().getString() : target.getName().getString();
        return new PendingShadowHandsDebug(ownerId, mob.getUUID(), target.getUUID(), level.getGameTime(), tameName, targetName, amount);
    }

    private static void sendShadowHandsDebug(UUID ownerId, String message) {
        if (ownerId == null || message == null || message.isBlank()) {
            return;
        }
        ServerPlayer owner = ServerLifecycleHooks.getCurrentServer() == null ? null : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(ownerId);
        if (owner == null) {
            return;
        }
        owner.sendSystemMessage(Component.literal("SHDBG " + message).withStyle(ChatFormatting.YELLOW));
    }

    private static String shadowHandsDamageSource(DamageSource source) {
        if (source == null) {
            return "null";
        }
        Entity direct = source.getDirectEntity();
        Entity attacker = source.getEntity();
        return source.getMsgId()
                + ", attacker=" + simpleEntityName(attacker)
                + ", direct=" + simpleEntityName(direct);
    }

    private static String simpleEntityName(Entity entity) {
        if (entity == null) {
            return "null";
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        return key == null ? entity.getType().toString() : key.toString();
    }

    public static int getAbilityOrEnchantLevelForCompat(LivingEntity entity, String abilityId) {
        return getAbilityOrEnchantLevel(entity, abilityId);
    }

    private static int getAbilityOrEnchantLevel(LivingEntity entity, String abilityId) {
        return getDiEffectLevel(entity, abilityId);
    }

    private static int getDiEffectLevel(LivingEntity entity, String effectId) {
        if (entity == null || effectId == null || effectId.isBlank()) {
            return 0;
        }
        int abilityLevel = 0;
        int attributeLevel = 0;
        if (entity instanceof TamableAnimal tame && tame.isTame()) {
            TameData data = TameRegistry.get(tame.getUUID());
            if (data != null) {
                abilityLevel = Math.max(0, LevelSystem.getAbilityLevel(data, effectId));
                attributeLevel = Math.max(0, LevelSystem.getAttributeLevel(data, effectId));
            }
        }

        if ("psychic_wall".equals(effectId)) {
            abilityLevel = psychicAbilityToEnchantScale(abilityLevel);
        }

        return Math.max(abilityLevel, attributeLevel);
    }

    private static boolean hasTlAttributeLevel(LivingEntity entity, String attributeId) {
        if (!(entity instanceof TamableAnimal tame) || !tame.isTame() || attributeId == null || attributeId.isBlank()) {
            return false;
        }
        TameData data = TameRegistry.get(tame.getUUID());
        return data != null && LevelSystem.getAttributeLevel(data, attributeId) > 0;
    }

    private static boolean shouldApplyLegacyFrostFang(LivingEntity attacker, int level) {
        if (attacker == null || level <= 0) {
            return false;
        }
        if (TameableUtils.hasEnchant(attacker, DIEnchantmentRegistry.FROST_FANG)) {
            return true;
        }
        double chance = Math.min(0.45D, 0.15D + Math.max(0, level - 1) * 0.075D);
        return attacker.getRandom().nextDouble() < chance;
    }

    private static void spawnLegacyChainLightning(LivingEntity attacker, LivingEntity target, int level, float hitDamage) {
        if (attacker == null || target == null || level <= 0) {
            return;
        }
        ChainLightningEntity lightning = DIEntityRegistry.CHAIN_LIGHTNING.get().create(target.level());
        if (lightning == null) {
            return;
        }
        boolean enchanted = TameableUtils.hasEnchant(attacker, DIEnchantmentRegistry.CHAIN_LIGHTNING);
        if (!enchanted) {
            double chance = Math.min(0.28D, 0.12D + Math.max(0, level - 1) * 0.04D);
            if (attacker.getRandom().nextDouble() >= chance) {
                return;
            }
            lightning.setChainsLeft(1 + level);
            lightning.setShockDamage(extensionChainLightningDamage(hitDamage));
        } else {
            lightning.setChainsLeft(3 + level * 3);
            lightning.setShockDamage(extensionChainLightningDamage(hitDamage));
        }
        lightning.setCreatorEntityID(attacker.getId());
        lightning.setFromEntityID(attacker.getId());
        lightning.setToEntityID(target.getId());
        lightning.copyPosition(target);
        target.level().addFreshEntity(lightning);
        target.playSound(DISoundRegistry.CHAIN_LIGHTNING.get(), 1F, 1F);
        debugDiAbilityUse(attacker, "chain_lightning");
    }

    private static float extensionChainLightningDamage(float hitDamage) {
        return Math.max(1.0F, hitDamage * 0.33F);
    }

    private static double magneticPullStrength(int level, boolean onGround) {
        int safeLevel = Math.max(1, level);
        double base = onGround ? 0.12D : 0.04D;
        return Math.min(onGround ? 0.32D : 0.14D, base + Math.max(0, safeLevel - 1) * (onGround ? 0.03D : 0.015D));
    }

    private static boolean shouldRetainMagneticTarget(Mob mob, Entity existingTarget) {
        if (mob == null || existingTarget == null) {
            return false;
        }
        if (!existingTarget.isAlive() || existingTarget.level() != mob.level()) {
            return false;
        }
        if (mob.getRootVehicle() instanceof GiantBubbleEntity) {
            return false;
        }
        return mob.distanceToSqr(existingTarget) <= 24.0D * 24.0D;
    }

    private static double bubblingProcChance(int level, LivingEntity target) {
        double baseChance = Math.min(0.75D, 0.20D + Math.max(0, level - 1) * 0.05D);
        return TameableUtils.scaleMinorEnemyProcChance(baseChance, level, target);
    }

    private static double warpingBiteProcChance(int level, LivingEntity target) {
        double baseChance = Math.min(0.65D, 0.18D + Math.max(0, level - 1) * 0.04D);
        return TameableUtils.scaleMinorEnemyProcChance(baseChance, level, target);
    }

    private static double magneticResistanceMultiplier(LivingEntity target) {
        double knockbackResistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        return Math.max(0.15D, 1.0D - knockbackResistance);
    }

    private static double frostFangResistanceMultiplier(LivingEntity target) {
        double knockbackResistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        if (knockbackResistance <= 0.0D) {
            return 1.0D;
        }
        if (knockbackResistance >= 1.0D) {
            return 0.01D;
        }
        return 0.01D + 0.99D * Math.pow(1.0D - knockbackResistance, 2.7224660245D);
    }

    private static double healthSiphonRange(int level) {
        int safeLevel = Math.max(1, level);
        return Math.min(128.0D, 32.0D + Math.max(0, safeLevel - 1) * 16.0D);
    }

    private static void spawnShadowHandsFallbackParticles(ServerLevel level, Mob mob, int shadowHandsLevel) {
        if (level == null || mob == null || shadowHandsLevel <= 0 || mob.tickCount % 6 != 0) {
            return;
        }
        int particles = Math.min(8, Math.max(3, shadowHandsLevel));
        double radius = mob.getBbWidth() + 0.35D;
        double y = mob.getY(0.7D);
        for (int i = 0; i < particles; i++) {
            double angle = (Math.PI * 2.0D / particles) * i + (mob.tickCount * 0.17D);
            double x = mob.getX() + Math.cos(angle) * radius;
            double z = mob.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 1, 0.01D, 0.01D, 0.01D, 0.0D);
        }
    }

    private static void spawnBlazingProtectionFallbackParticles(ServerLevel level, LivingEntity entity, int bars) {
        if (level == null || entity == null || bars <= 0 || entity.tickCount % 8 != 0) {
            return;
        }
        int particles = Math.min(10, Math.max(3, bars));
        double radius = entity.getBbWidth() + 0.25D;
        double y = entity.getY(0.5D);
        for (int i = 0; i < particles; i++) {
            double angle = (Math.PI * 2.0D / particles) * i + (entity.tickCount * 0.14D);
            double x = entity.getX() + Math.cos(angle) * radius;
            double z = entity.getZ() + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.FLAME, x, y, z, 1, 0.01D, 0.02D, 0.01D, 0.0D);
        }
    }

    private static int blazingProtectionBarCap(int level) {
        return Math.max(2, Math.max(1, level) * 2);
    }

    private static int blazingProtectionRechargeTicks(int level) {
        int safeLevel = Math.max(1, level);
        return Math.max(80, 220 - Math.max(0, safeLevel - 1) * 20);
    }

    private static int blazingProtectionRecoveryTicks(int level) {
        int safeLevel = Math.max(1, level);
        return Math.max(180, 600 - Math.max(0, safeLevel - 1) * 60);
    }

    private static int blazingProtectionFireSeconds(int level) {
        return Math.min(10, 4 + Math.max(1, level));
    }

    private static double blazingProtectionKnockback(int level) {
        return Math.min(1.0D, 0.30D + Math.max(0, level - 1) * 0.08D);
    }

    private static int warpingBiteAttempts(int level) {
        return Math.min(32, 10 + Math.max(0, level - 1) * 2);
    }

    private static double warpingBiteHorizontalRange(int level) {
        return Math.min(32.0D, 16.0D + Math.max(0, level - 1) * 4.0D);
    }

    private static int warpingBiteVerticalRange(int level) {
        return Math.min(24, 16 + Math.max(0, level - 1) * 2);
    }

    private static boolean isBaseAttackHit(LivingEntity attacker, LivingAttackEvent event) {
        if (attacker == null || event == null || event.getSource() == null) {
            return false;
        }
        return (event.getSource().getEntity() == attacker && event.getSource().getDirectEntity() == attacker)
                || isAlexsMobsProjectileBaseAttack(attacker, event)
                || isMutantCreeperMinionExplosion(attacker, event);
    }

    private static boolean isAlexsMobsProjectileBaseAttack(LivingEntity attacker, LivingAttackEvent event) {
        if (attacker == null || event == null || event.getSource() == null) {
            return false;
        }
        Entity sourceEntity = event.getSource().getEntity();
        Entity directEntity = event.getSource().getDirectEntity();
        if (sourceEntity != attacker || directEntity == null || directEntity == attacker) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(directEntity.getType());
        return key != null && "alexsmobs".equals(key.getNamespace());
    }

    private static boolean isMutantCreeperMinionExplosion(LivingEntity attacker, LivingAttackEvent event) {
        if (!(attacker instanceof TamableAnimal tame) || event == null || event.getSource() == null) {
            return false;
        }
        ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(tame.getType());
        return key != null
                && "mutantmonsters".equals(key.getNamespace())
                && "creeper_minion".equals(key.getPath())
                && event.getSource().getEntity() == attacker
                && event.getSource().is(DamageTypeTags.IS_EXPLOSION);
    }

    private static int psychicAbilityToEnchantScale(int abilityLevel) {
        if (abilityLevel <= 0) {
            return 0;
        }
        // 5 ability levels should match enchantment level 3 strength.
        return Math.max(1, (int) Math.ceil(abilityLevel * 3.0D / 5.0D));
    }
}
