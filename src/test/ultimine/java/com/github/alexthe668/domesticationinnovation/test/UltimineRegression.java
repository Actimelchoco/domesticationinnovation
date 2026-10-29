package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.UltimineCompanion;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class UltimineRegression {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static class Fixture implements AutoCloseable {
        final GameTestHelper h;
        final FakePlayer player;
        final List<LivingEntity> pets = new ArrayList<>();
        final BlockPos block;
        Fixture(GameTestHelper h) {
            this.h = h;
            player = new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "UltimineTest"));
            block = h.absolutePos(new BlockPos(1, 2, 1));
            player.setPos(block.getX(), block.getY(), block.getZ());
            h.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        }
        LivingEntity pet(boolean roly) {
            EntityType<?> type = roly ? ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("crittersandcompanions", "roly_poly")) : EntityType.WOLF;
            var pet = (LivingEntity) Objects.requireNonNull(type).create(h.getLevel());
            TameEntityAdapter.setOwner(pet, player.getUUID());
            pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            pet.getAttribute(Attributes.ARMOR).setBaseValue(0);
            pet.setHealth(200);
            ((Mob) pet).setNoAi(true);
            pet.setPos(player.getX() + 2, player.getY(), player.getZ());
            h.getLevel().addFreshEntity(pet);
            TameData data = new TameData(pet, player.getUUID(), false);
            data.attributeLevels.put("ultimine", 1);
            data.hungerSaturation = 1000;
            TameRegistry.register(data);
            // Entity-join registration reapplies species base stats; establish the test
            // health/armor after that registration has completed.
            pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            pet.getAttribute(Attributes.ARMOR).setBaseValue(0);
            pet.setHealth(200);
            pets.add(pet);
            return pet;
        }
        TameData data(LivingEntity pet) { return TameRegistry.get(pet.getUUID()); }
        boolean mine() { return UltimineCompanion.breakBlock(player, block, () -> true); }
        @Override public void close() { for (var pet : pets) { TameRegistry.remove(pet.getUUID()); pet.discard(); } }
    }

    @GameTest(template = "empty")
    public static void requiresOwnedNearbyQualifiedTame(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            check(!UltimineCompanion.hasCompanion(f.player) && !f.mine(), "no free mining without a pet");
            var pet = f.pet(false); var data = f.data(pet);
            check(UltimineCompanion.hasCompanion(f.player), "attribute qualifies");
            data.attributeLevels.clear();
            check(!UltimineCompanion.hasCompanion(f.player), "unqualified tame rejected");
            data.attributeLevels.put("ultimine", 1);
            TameEntityAdapter.setOwner(pet, UUID.randomUUID());
            check(!UltimineCompanion.hasCompanion(f.player), "other owner's tame rejected");
            TameEntityAdapter.setOwner(pet, f.player.getUUID());
            pet.setPos(f.player.getX() + 17, f.player.getY(), f.player.getZ());
            check(!UltimineCompanion.hasCompanion(f.player), "outside 16 blocks rejected");
            pet.setPos(f.player.getX() + 16, f.player.getY(), f.player.getZ());
            check(UltimineCompanion.hasCompanion(f.player), "16-block boundary included");
            data.stored = true;
            check(!UltimineCompanion.hasCompanion(f.player), "stored row rejected");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void everyBlockChargesDamageInSameTick(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var pet = f.pet(false);
            check(f.mine() && f.mine() && f.mine(), "three blocks allowed; hp=" + pet.getHealth());
            check(Math.abs(pet.getHealth() - 185) < .01, "three blocks deal 15 damage despite hurt cooldown; hp=" + pet.getHealth());
            check(f.data(pet).hungerSaturation == 700, "three blocks cost 300 saturation");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void requiredMiningTierScalesBothCosts(GameTestHelper h) {
        check(UltimineCompanion.miningLevel(Blocks.STONE.defaultBlockState()) == 1, "stone costs 1x");
        check(UltimineCompanion.miningLevel(Blocks.IRON_ORE.defaultBlockState()) == 1, "stone-tier requirement costs 1x");
        check(UltimineCompanion.miningLevel(Blocks.DIAMOND_ORE.defaultBlockState()) == 2, "iron-tier requirement costs 2x");
        check(UltimineCompanion.miningLevel(Blocks.OBSIDIAN.defaultBlockState()) == 3, "diamond-tier requirement costs 3x");
        try (var f = new Fixture(h)) {
            var pet = f.pet(false);
            h.getLevel().setBlockAndUpdate(f.block, Blocks.OBSIDIAN.defaultBlockState());
            check(f.mine(), "obsidian allowed");
            check(pet.getHealth() == 185 && f.data(pet).hungerSaturation == 700, "both costs multiplied by three");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void protectedAndUnaffordableBlocksAreNotCharged(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var pet = f.pet(false); var data = f.data(pet);
            check(!UltimineCompanion.breakBlock(f.player, f.block, () -> false), "failed removal remains failed");
            check(data.hungerSaturation == 1000 && pet.getHealth() == 200, "failed block costs nothing");
            data.hungerSaturation = 99;
            var called = new AtomicBoolean();
            check(!UltimineCompanion.breakBlock(f.player, f.block, () -> { called.set(true); return true; }), "insufficient food blocks mining");
            check(!called.get() && data.hungerSaturation == 99 && pet.getHealth() == 200, "unaffordable block never removed");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void rolypolyQualifiesWithoutAttributeAndIsPreferred(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var ordinary = f.pet(false);
            var roly = f.pet(true); var data = f.data(roly); data.attributeLevels.clear();
            roly.setPos(f.player.getX() + 4, f.player.getY(), f.player.getZ());
            check(f.mine() && f.mine(), "rolypoly enables mining without attribute");
            check(roly.getHealth() == 198 && data.hungerSaturation == 866, "two rolypoly blocks cost 2 damage and 134 saturation; hp=" + roly.getHealth() + " sat=" + data.hungerSaturation);
            check(ordinary.getHealth() == 200 && f.data(ordinary).hungerSaturation == 1000, "rolypoly preferred despite greater distance");
            var saved = roly.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            var restored = (LivingEntity) roly.getType().create(h.getLevel());
            restored.load(saved);
            check(TameEntityAdapter.isTame(restored) && f.player.getUUID().equals(TameEntityAdapter.ownerUuid(restored)), "rolypoly ownership survives save/load");
            restored.discard();
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void armorMitigatesPhysicalMiningDamage(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var pet = f.pet(false);
            pet.getAttribute(Attributes.ARMOR).setBaseValue(20);
            check(f.mine(), "armored tame can mine");
            check(pet.getHealth() > 195 && pet.getHealth() < 200, "physical damage respects armor");
            check(f.data(pet).hungerSaturation == 900, "armor does not reduce food cost");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void heldWeaponsIncreaseFoodCostsAndRemovalStopsSurcharge(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var pet = f.pet(false); var data = f.data(pet);
            var hand = EquipmentSlot.MAINHAND;
            var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
            pet.setItemSlot(hand, sword);
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.consumeHungerForNaturalHeal(data, pet, 1), "first armed heal affordable");
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.consumeHungerForNaturalHeal(data, pet, 1), "second armed heal affordable");
            check(data.hungerSaturation == 697, "two heals use 303 rather than 202 saturation, preserving the half-unit remainder");
            pet.setItemSlot(hand, net.minecraft.world.item.ItemStack.EMPTY);
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.consumeHungerForNaturalHeal(data, pet, 1), "unarmed heal affordable");
            check(data.hungerSaturation == 596, "removing weapon immediately restores normal cost");
            pet.setItemSlot(EquipmentSlot.OFFHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOW));
            var weapons = com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons.weaponSaturationMultiplier(pet);
            check(weapons == 1.5D, "offhand ranged weapons count");
            pet.setItemSlot(hand, sword);
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons.weaponSaturationMultiplier(pet) == 1.5D, "two weapons do not stack");
            data.packModifiers = new TamePackService.Modifiers(1, 1, 1, 1, 1, 0, 2);
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.consumeHungerForNaturalHeal(data, pet, 1), "pack-scaled heal affordable");
            check(data.hungerSaturation == 293, "weapon multiplier stacks with pack multiplier");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void armedUltimineChargesMoreFoodButSameDamage(GameTestHelper h) {
        try (var f = new Fixture(h)) {
            var pet = f.pet(false);
            pet.setItemSlot(EquipmentSlot.MAINHAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            check(f.mine(), "armed companion can mine");
            check(f.data(pet).hungerSaturation == 850 && pet.getHealth() == 195, "weapon raises food cost to 150 without increasing damage");
            pet.setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            check(f.mine() && f.data(pet).hungerSaturation == 750, "removal restores 100 saturation cost");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void ftbMixinLoadsAndPreservesItsRestrictions(GameTestHelper h) throws Exception {
        check(LevelSystem.knownAttributeIds().contains("ultimine"), "attribute is registered");
        var type = Class.forName("dev.ftb.mods.ftbultimine.FTBUltimine");
        Object ftb = type.getField("instance").get(null);
        check(ftb != null, "FTB and its transformed class loaded");
        try (var f = new Fixture(h)) {
            var pet = f.pet(false);
            check(!(boolean) type.getMethod("canUltimine", net.minecraft.world.entity.player.Player.class).invoke(ftb, f.player), "FTB still denies fake players with a companion");
            var real = new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "UltimineReal"));
            real.setPos(f.player.getX(), f.player.getY(), f.player.getZ());
            real.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
            var can = type.getMethod("canUltimine", net.minecraft.world.entity.player.Player.class);
            check(!(boolean) can.invoke(ftb, real), "real player cannot Ultimine with another owner's companion");
            TameEntityAdapter.setOwner(pet, real.getUUID());
            f.data(pet).ownerUUID = real.getUUID();
            check((boolean) can.invoke(ftb, real), "real player with tool and owned companion can Ultimine");
            f.data(pet).attributeLevels.clear();
            check(!(boolean) can.invoke(ftb, real), "removing the attribute disables Ultimine");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crittersRolypolyKeepsNativeMushroomTamingAndCommands(GameTestHelper h) {
        var f = new Fixture(h);
        try {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("crittersandcompanions", "roly_poly"));
            var pet = (TamableAnimal) type.create(h.getLevel());
            pet.setPos(f.player.getX() + 2, f.player.getY(), f.player.getZ());
            h.getLevel().addFreshEntity(pet); f.pets.add(pet);
            check(!TameEntityAdapter.isTame(pet), "native rolypoly starts wild");
            var mushrooms = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_MUSHROOM, 128);
            f.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mushrooms);
            pet.getRandom().setSeed(0);
            while (!pet.isTame() && !mushrooms.isEmpty()) pet.interact(f.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            check(pet.isTame() && f.player.getUUID().equals(pet.getOwnerUUID()), "native mushrooms create ownership");
            check(mushrooms.getCount() < 128, "native taming consumes mushrooms");
            pet.setOrderedToSit(false);
            check(TameEntityAdapter.isFollowingOwner(pet), "native follow command supported");
            pet.setOrderedToSit(true);
            check(TameEntityAdapter.isStayingStill(pet), "native stay command supported");
            // Native taming fires the Forge event before isTame becomes true. The
            // existing once-per-second registration pass picks it up afterward.
            h.runAfterDelay(21, () -> {
                try (f) {
                    check(f.data(pet) != null, "native rolypoly is registered by the normal tame tick");
                    check(UltimineCompanion.hasCompanion(f.player), "registered rolypoly enables Ultimine without an attribute");
                    h.succeed();
                }
            });
        } catch (RuntimeException | Error failure) {
            f.close(); throw failure;
        }
    }
}
