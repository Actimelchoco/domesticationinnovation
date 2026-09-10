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
            System.out.println("PRIMITIVE_COMPAT_TESTS_PASS " + checks);
        } catch (Throwable failure) {
            System.out.println("PRIMITIVE_COMPAT_TESTS_FAIL"); failure.printStackTrace();
        } finally { event.getServer().halt(false); }
    }
}
