package test;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod("primitive_compat_tests")
@Mod.EventBusSubscriber(modid = "primitive_compat_tests")
public class PrimitiveCompatTests {
    private static int checks;
    private static void testLastDuelAttacker(ServerLevel level) {
        var first = deathFixture(level);
        var last = deathFixture(level);
        var victim = deathFixture(level);
        UUID owner = first.getOwnerUUID();
        last.setOwnerUUID(owner);
        TameRegistry.get(last.getUUID()).ownerUUID = owner;
        TameDuelManager.startTeamDuel(level.getServer(), owner, Set.of(first.getUUID(), last.getUUID()),
                victim.getOwnerUUID(), Set.of(victim.getUUID()));
        check(TameDuelManager.areDuelOpponents(first.getUUID(), victim.getUUID()), "last attacker duel started");
        LevelSystem.trackDamage(victim, first);
        LevelSystem.trackDamage(victim, last);
        victim.hurt(victim.damageSources().generic(), 1000);
        TameData credited = TameRegistry.get(last.getUUID());
        check(credited.duelKills == 1, "last tame attacker receives environmental duel kill");
        check(TameRegistry.get(first.getUUID()).duelKills == 0, "earlier attacker is not killer");
        check(TameRegistry.get(first.getUUID()).duelAssists == 1, "earlier attacker keeps assist credit");
        check(LevelSystem.lastTameDamager(victim.getUUID()) == null, "damage tracking released after attribution");
        TameDuelManager.endDuelForEntity(level.getServer(), first.getUUID());
    }
    private static void testMissingDeathEvent(ServerLevel level) {
        for (Entity.RemovalReason reason : Entity.RemovalReason.values()) {
            for (boolean zeroHealth : new boolean[] {false, true}) {
                var wolf = deathFixture(level);
                TameData data = TameRegistry.get(wolf.getUUID());
                if (zeroHealth) wolf.setHealth(0);
                wolf.remove(reason);
                boolean shouldDie = reason == Entity.RemovalReason.KILLED
                        || (reason == Entity.RemovalReason.DISCARDED && zeroHealth);
                check(data.dead == shouldDie, "removal death classification " + reason + " zero=" + zeroHealth);
                check(data.deaths == (shouldDie ? 1 : 0), "removal death counted once");
                check(data.deathHistory.size() == (shouldDie ? 1 : 0), "removal archived once");
                com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameCombatEvents.onRemovedTame(
                        new net.minecraftforge.event.entity.EntityLeaveLevelEvent(wolf, level));
                check(data.deaths == (shouldDie ? 1 : 0), "duplicate leave event is harmless");
            }
        }
        var normal = deathFixture(level);
        TameData normalData = TameRegistry.get(normal.getUUID());
        normal.hurt(normal.damageSources().generic(), 1000);
        check(normalData.dead && normalData.deaths == 1 && normalData.deathHistory.size() == 1,
                "ordinary damage death is not counted twice by removal fallback");
        var clone = deathFixture(level);
        TameData cloneData = TameRegistry.get(clone.getUUID());
        clone.getPersistentData().putBoolean(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.ADMIN_CLONE_SILENT_TAG, true);
        clone.remove(Entity.RemovalReason.KILLED);
        check(!cloneData.dead, "silent clone cleanup is not a death");
    }
    private static net.minecraft.world.entity.animal.Wolf deathFixture(ServerLevel level) {
        var wolf = EntityType.WOLF.create(level);
        wolf.setTame(true);
        wolf.setOwnerUUID(UUID.randomUUID());
        wolf.moveTo(0, -59, 0);
        level.addFreshEntity(wolf);
        check(TameRegistry.get(wolf.getUUID()) != null, "death fixture registered");
        return wolf;
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private static EntityType<?> type(String name) { return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("primitive_mobs", name)); }
    private static Mob create(ServerLevel level, String name, double health) {
        Mob mob = (Mob) type(name).create(level);
        mob.moveTo(0, -59, 0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        mob.getAttribute(Attributes.ARMOR).addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                UUID.fromString("3765073d-d1d5-4e83-b167-1d06a1a472e0"), "individual spawn armor", 7,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        mob.setHealth((float) health);
        mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0));
        level.addFreshEntity(mob);
        return mob;
    }
    @SubscribeEvent
    public static void run(ServerStartedEvent event) {
        try {
            ServerLevel level = event.getServer().overworld();
            testMissingDeathEvent(level);
            testLastDuelAttacker(level);
            Player owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PrimitiveOwner"));
            owner.moveTo(0, -59, 0);
            Player stranger = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "PrimitiveOther"));
            for (String name : List.of("baby_spider", "chameleon", "festive_creeper", "support_creeper", "rocket_creeper")) {
                Mob mob = create(level, name, 37);
                check(mob instanceof ModifedToBeTameable, name + " bridge");
                check(!TameEntityAdapter.isTame(mob), name + " wild spawn");
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DIItemRegistry.SINISTER_CARROT.get(), 4));
                mob.setHealth(10);
                mob.interact(owner, InteractionHand.MAIN_HAND);
                check(!TameEntityAdapter.isTame(mob) && owner.getMainHandItem().getCount() == 4, name + " exact 10 HP rejection");
                mob.setHealth(9.5F);
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ROTTEN_FLESH, 4));
                mob.interact(owner, InteractionHand.MAIN_HAND);
                check(!TameEntityAdapter.isTame(mob), name + " old food rejected");
                owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(DIItemRegistry.SINISTER_CARROT.get(), 4));
                mob.interact(owner, InteractionHand.MAIN_HAND);
                check(owner.getUUID().equals(TameEntityAdapter.ownerUuid(mob)) && owner.getMainHandItem().getCount() == 3, name + " carrot tame");
                check(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) == 37, name + " native 20 HP reset removed");
                PrimitiveMobsCompat.tame(mob, stranger);
                check(owner.getUUID().equals(TameEntityAdapter.ownerUuid(mob)), name + " ownership protected");
                for (int command = 0; command < 3; command++) {
                    ((IComandableMob) mob).setCommand(command);
                    check(((IComandableMob) mob).getCommand() == command, name + " synced command " + command);
                }
                TameData data = new TameData(mob, owner.getUUID(), false);
                data.bonusHealth = 5;
                for (int n = 0; n < 3; n++) {
                    LevelSystem.reapplyTypeBasePlusBonuses(mob, data);
                    check(mob.getAttributeBaseValue(Attributes.MAX_HEALTH) == 37 && mob.getMaxHealth() == 42, name + " baseline and nonstacking rewards");
                }
                mob.removeAllEffects();
                mob.tickCount = 20;
                PrimitiveMobsCompat.tick(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(mob));
                check(mob.hasEffect(MobEffects.FIRE_RESISTANCE) && mob.getEffect(MobEffects.FIRE_RESISTANCE).isInfiniteDuration(), name + " innate effect restored");
                CompoundTag snapshot = new CompoundTag();
                mob.save(snapshot);
                Mob restored = (Mob) mob.getType().create(level);
                restored.load(snapshot);
                check(owner.getUUID().equals(TameEntityAdapter.ownerUuid(restored)), name + " saved ownership");
                check(restored.getAttribute(Attributes.ARMOR).getModifier(UUID.fromString("3765073d-d1d5-4e83-b167-1d06a1a472e0")) != null, name + " transient spawn bonus saved");
                check(restored.getPersistentData().getCompound(PrimitiveMobsCompat.BASELINE).equals(mob.getPersistentData().getCompound(PrimitiveMobsCompat.BASELINE)), name + " saved individual baseline");
                ObfuscationReflectionHelper.findMethod(Mob.class, "m_6140_").invoke(mob);
                check(mob.targetSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal), name + " hostile target goals removed");
                check(!((ModifedToBeTameable) mob).isValidAttackTarget(owner), name + " owner protected");
                if (mob instanceof Creeper creeper) {
                    ObfuscationReflectionHelper.findMethod(Creeper.class, "m_32315_").invoke(creeper);
                    check(creeper.isAlive() && !creeper.isRemoved() && !creeper.isIgnited(), name + " survives explosion");
                }
                mob.discard();
                Mob other = create(level, name, 53);
                check(other.getAttributeBaseValue(Attributes.MAX_HEALTH) == 53, name + " same species different base");
                other.discard();

                SpawnEggItem egg = SpawnEggItem.byId(type(name));
                // The mod supplies thrown eggs. Also exercise vanilla eggs carrying EntityTag.
                ItemStack spawnEgg = new ItemStack(egg == null ? Items.CREEPER_SPAWN_EGG : egg);
                spawnEgg.getOrCreateTagElement("EntityTag").putString("id", "primitive_mobs:" + name);
                Entity wildEgg = type(name).spawn(level, spawnEgg.copy(), null, new BlockPos(5, -59, 0), MobSpawnType.SPAWN_EGG, false, false);
                check(!TameEntityAdapter.isTame(wildEgg), name + " ownerless/dispenser egg stays wild");
                wildEgg.discard();
                Entity ownedEgg = type(name).spawn(level, spawnEgg.copy(), owner, new BlockPos(5, -59, 0), MobSpawnType.SPAWN_EGG, false, false);
                check(owner.getUUID().equals(TameEntityAdapter.ownerUuid(ownedEgg)), name + " player spawn egg tames");
                ownedEgg.discard();
            }
            Class<?> eggClass = Class.forName("com.misanthropy.primitive_mobs.entity.projectile.ThrownPrimitiveEgg");
            Class<?> kindClass = Class.forName(eggClass.getName() + "$Kind");
            var hatch = eggClass.getDeclaredMethod("spawnHatchling"); hatch.setAccessible(true);
            for (Object kind : kindClass.getEnumConstants()) {
                if (((Enum<?>) kind).name().equals("DODO")) continue;
                for (LivingEntity thrower : new LivingEntity[] {owner, EntityType.ZOMBIE.create(level)}) {
                    Set<UUID> before = new HashSet<>(); level.getAllEntities().forEach(e -> before.add(e.getUUID()));
                    Entity egg = (Entity) eggClass.getConstructor(Level.class, LivingEntity.class, kindClass, int.class).newInstance(level, thrower, kind, 1);
                    egg.moveTo(8, -59, 0);
                    hatch.invoke(egg);
                    List<Entity> hatched = new ArrayList<>();
                    level.getAllEntities().forEach(e -> { if (!before.contains(e.getUUID()) && PrimitiveMobsCompat.isPrimitivePet(e)) hatched.add(e); });
                    check(hatched.size() == 1, kind + " hatch count");
                    check(TameEntityAdapter.isTame(hatched.get(0)) == (thrower == owner), kind + " thrown egg owner restriction");
                    hatched.forEach(Entity::discard);
                }
            }
            checkOtherPets(level, owner);
            checkFoodAndReset(level, owner);
            System.out.println("PRIMITIVE_COMPAT_TESTS_PASS " + checks);
        } catch (Throwable failure) {
            System.out.println("PRIMITIVE_COMPAT_TESTS_FAIL"); failure.printStackTrace();
        } finally { event.getServer().halt(false); }
    }

    /** Models an external mod's owned Mob which is neither TamableAnimal nor PathfinderMob. */
    private static final class ExternalOwnedMob extends Mob implements OwnableEntity {
        private final UUID ownerId;
        private ExternalOwnedMob(ServerLevel level, UUID ownerId) {
            super(EntityType.ZOMBIE, level);
            this.ownerId = ownerId;
            moveTo(0, -59, 0);
        }
        @Override public UUID getOwnerUUID() { return ownerId; }
    }

    private static void checkOtherPets(ServerLevel level, Player owner) {
        UUID otherOwner = UUID.randomUUID();
        for (String name : List.of("baby_spider", "chameleon", "festive_creeper", "support_creeper", "rocket_creeper")) {
            Mob primitive = create(level, name, 100);
            Mob external = new ExternalOwnedMob(level, otherOwner);
            external.setTarget(primitive);
            check(external.getTarget() == primitive, name + " wild target remains selectable");
            PrimitiveMobsCompat.tame(primitive, owner);
            PrimitiveMobsCompat.clearOldHostileTarget(new net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent(external));
            check(external.getTarget() == null, name + " pre-taming aggro cleared");
            var sameOwnerWolf = EntityType.WOLF.create(level);
            sameOwnerWolf.setTame(true); sameOwnerWolf.setOwnerUUID(owner.getUUID()); sameOwnerWolf.moveTo(0, -59, 0);
            var otherOwnerWolf = EntityType.WOLF.create(level);
            otherOwnerWolf.setTame(true); otherOwnerWolf.setOwnerUUID(otherOwner); otherOwnerWolf.moveTo(0, -59, 0);
            for (Mob pet : List.of(sameOwnerWolf, otherOwnerWolf, external)) {
                check(PrimitiveMobsCompat.protectFromOtherPets(pet, primitive), name + " owned-pet recognition");
                check(!net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().test(pet, primitive), name + " monster scan rejects tame");
                check(net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat().test(pet, primitive), name + " noncombat scans still see tame");
                pet.setTarget(primitive);
                check(pet.getTarget() == null, name + " direct target assignment blocked");
                float health = primitive.getHealth();
                primitive.hurt(level.damageSources().mobAttack(pet), 3);
                check(primitive.getHealth() == health, name + " custom pet attack cannot damage tame");
            }
            Mob unowned = new ExternalOwnedMob(level, null);
            check(!PrimitiveMobsCompat.protectFromOtherPets(unowned, primitive), name + " ownerless mob not falsely treated as pet");
            Mob zombie = EntityType.ZOMBIE.create(level);
            check(!PrimitiveMobsCompat.protectFromOtherPets(zombie, primitive), name + " wild hostile unaffected");
            primitive.discard();
        }

        var wolf = EntityType.WOLF.create(level);
        wolf.setTame(true); wolf.setOwnerUUID(otherOwner); wolf.moveTo(3, -59, 0);
        level.addFreshEntity(wolf);
        Mob primitive = create(level, "baby_spider", 100);
        PrimitiveMobsCompat.tame(primitive, owner);
        TameData a = new TameData(wolf, otherOwner, false);
        TameData b = new TameData(primitive, owner.getUUID(), false);
        a.hungerSaturation = 10000; b.hungerSaturation = 10000;
        TameRegistry.register(a); TameRegistry.register(b);
        TameDuelManager.startTeamDuel(level.getServer(), otherOwner, Set.of(wolf.getUUID()), owner.getUUID(), Set.of(primitive.getUUID()));
        check(TameDuelManager.areDuelOpponents(wolf.getUUID(), primitive.getUUID()), "duel fixture started");
        check(!PrimitiveMobsCompat.protectFromOtherPets(wolf, primitive), "intentional duel opponents remain targetable");
        TameDuelManager.endDuelForEntity(level.getServer(), wolf.getUUID());
        wolf.discard(); primitive.discard();
    }

    private static void checkFoodAndReset(ServerLevel level, Player owner) throws Exception {
        var commandsClass = com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.class;
        var cost = commandsClass.getDeclaredMethod("scaleSaturationCost", TameData.class, int.class);
        cost.setAccessible(true);
        var preferred = commandsClass.getDeclaredMethod("isPreferredDistributionFood", ItemStack.class, TameData.class, LivingEntity.class);
        preferred.setAccessible(true);
        ItemStack raw = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("primitive_mobs:dodo")));
        check(!raw.isEmpty(), "Raw Dodo ID resolves");
        Mob preOwned = (Mob) type("rocket_creeper").create(level);
        preOwned.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        preOwned.setHealth(100);
        PrimitiveMobsCompat.tame(preOwned, owner);
        level.addFreshEntity(preOwned);
        TameData automatic = TameRegistry.get(preOwned.getUUID());
        check(automatic != null && PrimitiveMobsCompat.hungerExtraLevels(automatic) == 80, "automatic join backfill keeps 100 HP baseline");
        check(preOwned.getAttributeBaseValue(Attributes.MAX_HEALTH) == 100, "automatic join registration does not reset base HP");
        preOwned.discard();
        for (String name : List.of("baby_spider", "chameleon", "festive_creeper", "support_creeper", "rocket_creeper")) {
            Mob mob = create(level, name, 100);
            PrimitiveMobsCompat.tame(mob, owner);
            TameData data = new TameData(mob, owner.getUUID(), false);
            check(TameFoodManager.accepts(raw, data, mob), name + " accepts Raw Dodo");
            check((boolean) preferred.invoke(null, raw, data, null), name + " stored pets prefer Raw Dodo");
            check(TameFoodManager.foodsForDisplay(level.getServer(), data.type).stream().anyMatch(s -> s.contains("Raw Dodo")), name + " displays Raw Dodo");
            data.level = 50;
            check(PrimitiveMobsCompat.hungerExtraLevels(data) == 80, name + " 100 HP adds 80 hunger levels");
            check((int) cost.invoke(null, data, 100) == 260, name + " additive 100 HP + level 50 costs 260%");
            data.bonusHealth = 400;
            LevelSystem.reapplyTypeBasePlusBonuses(mob, data);
            mob.save(data.entitySnapshot);
            check((int) cost.invoke(null, data, 100) == 260, name + " leveling HP does not inflate baseline cost");
            data.entitySnapshot.getCompound("ForgeData").getCompound(PrimitiveMobsCompat.BASELINE).putDouble("MaxHealth", 20);
            check((int) cost.invoke(null, data, 100) == 150, name + " 20 HP has normal level cost");
            data.entitySnapshot.getCompound("ForgeData").getCompound(PrimitiveMobsCompat.BASELINE).putDouble("MaxHealth", 10);
            check((int) cost.invoke(null, data, 100) == 150, name + " below 20 HP has normal level cost");
            data.entitySnapshot.getCompound("ForgeData").getCompound(PrimitiveMobsCompat.BASELINE).remove("MaxHealth");
            check((int) cost.invoke(null, data, 100) == 260, name + " first-version baseline migrates without reward HP");
            data.level = Integer.MAX_VALUE;
            check((int) cost.invoke(null, data, 4) == Integer.MAX_VALUE, name + " huge levels saturate without overflow");
            mob.discard();
        }

        Mob loaded = create(level, "rocket_creeper", 100);
        PrimitiveMobsCompat.tame(loaded, owner);
        TameData liveData = new TameData(loaded, owner.getUUID(), false);
        TameRegistry.register(liveData);
        UUID oldIdentity = liveData.tlId;
        CompoundTag oldEntity = new CompoundTag(); loaded.save(oldEntity);
        ObfuscationReflectionHelper.findMethod(Mob.class, "m_6140_").invoke(loaded);

        TameData broken = TameData.fromTag(liveData.toTag());
        broken.uuid = UUID.randomUUID(); broken.tlId = UUID.randomUUID(); broken.ownerUUID = null;
        broken.type = "entity.primitiveMobs.removed_creeper"; broken.entitySnapshot = new CompoundTag();
        broken.name = "Broken old primitive entry"; broken.dead = true;
        TameRegistry.register(broken);
        UUID brokenId = broken.uuid;
        broken.uuid = null; // A malformed row whose map key and value identity no longer agree.

        TameData snapshotOnly = TameData.fromTag(liveData.toTag());
        snapshotOnly.uuid = UUID.randomUUID(); snapshotOnly.tlId = UUID.randomUUID(); snapshotOnly.type = "unknown";
        snapshotOnly.name = "Snapshot only primitive"; snapshotOnly.stored = true;
        TameRegistry.register(snapshotOnly);
        var death = new TameDeathRecord(); death.uuid = UUID.randomUUID(); death.type = "primitive_mobs:removed_type";
        TameRegistry.LAST_DEATHS.put(death.uuid, death); TameRegistry.DEATH_HISTORY.add(death);
        TameRegistry.archiveTemporaryTame(liveData.toTag());
        TameRegistry.setRankedParticipants(Set.of(oldIdentity, death.uuid));

        var wolf = EntityType.WOLF.create(level);
        TameData keep = new TameData(wolf, owner.getUUID(), false);
        keep.name = "Unrelated wolf with Raw Dodo"; keep.hungerInventory.add(raw.copy());
        TameRegistry.register(keep);
        check((int) cost.invoke(null, keep, 100) == 101, "other species hunger unchanged");
        var worldData = com.github.alexthe668.domesticationinnovation.server.misc.DIWorldData.get(level);
        worldData.addRespawnRequest(new com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest(
                "primitive_mobs:removed_type", level.dimension().location().toString(), oldEntity, new BlockPos(0, -59, 0), 0, "old"));
        worldData.addRespawnRequest(new com.github.alexthe668.domesticationinnovation.server.misc.RespawnRequest(
                "minecraft:wolf", level.dimension().location().toString(), keep.entitySnapshot, new BlockPos(0, -59, 0), 0, "keep"));
        var source = level.getServer().createCommandSourceStack();
        var dispatcher = level.getServer().getCommands().getDispatcher();
        boolean denied = false;
        try { dispatcher.execute("tames admin reset primitiveMobs", source.withPermission(0)); }
        catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { denied = true; }
        check(denied, "reset requires admin permission");
        check(dispatcher.execute("tames admin reset primitiveMobs", source.withPermission(4)) == 1, "exact reset command runs");
        check(TameRegistry.get(brokenId) == null, "broken UUID row purged");
        check(TameRegistry.get(snapshotOnly.uuid) == null, "snapshot-only row purged");
        check(TameRegistry.getByTlId(oldIdentity) == null, "secondary index purged");
        check(TameRegistry.getOwned(owner.getUUID()).stream().noneMatch(d -> PrimitiveMobsCompat.isPrimitiveType(d.type)), "owner index purged");
        check(TameRegistry.get(keep.uuid) == keep, "unrelated tame with Raw Dodo retained");
        check(!TameRegistry.LAST_DEATHS.containsKey(death.uuid) && !TameRegistry.DEATH_HISTORY.contains(death), "orphan death records purged");
        check(TameRegistry.getTemporaryTames().stream().noneMatch(PrimitiveMobsCompat::isPrimitiveSnapshot), "temporary archive purged");
        check(TameRegistry.getRankedParticipants().isEmpty(), "ranked references purged");
        check(worldData.getRespawnRequestsSnapshot().stream().noneMatch(r -> PrimitiveMobsCompat.isPrimitiveType(r.getEntityTypeLoc())), "old respawn requests purged");
        check(worldData.getRespawnRequestsSnapshot().stream().anyMatch(r -> r.getEntityTypeLoc().equals("minecraft:wolf")), "other respawns retained");
        check(!TameEntityAdapter.isTame(loaded) && TameData.getTlId(loaded) == null, "loaded ownership and identity cleared");
        ObfuscationReflectionHelper.findMethod(Mob.class, "m_6140_").invoke(loaded);
        check(loaded.targetSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof PrimitiveMobsCompat.PetTargetGoal), "wild AI restored after reset");
        loaded.discard();
        Mob stale = (Mob) type("rocket_creeper").create(level); stale.load(oldEntity);
        level.addFreshEntity(stale);
        check(!TameEntityAdapter.isTame(stale) && TameRegistry.get(stale.getUUID()) == null, "unloaded stale snapshot cannot re-register");
        var epoch = com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveResetData.get(level.getServer());
        check(com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveResetData.load(((net.minecraft.world.level.saveddata.SavedData) epoch).save(new CompoundTag())).generation() == epoch.generation(), "reset epoch persists");
        PrimitiveMobsCompat.tame(stale, owner);
        check(owner.getUUID().equals(TameEntityAdapter.ownerUuid(stale)), "fresh taming works after reset");
        PrimitiveMobsCompat.applyRegistryReset(stale);
        check(TameEntityAdapter.isTame(stale), "fresh tame not invalidated by old reset");
        stale.discard();
    }
}
