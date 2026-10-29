package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.TameClass;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class DuelPolishRegression {
    static void check(boolean condition, String message) { if (!condition) throw new GameTestAssertException(message); }
    static void start(HeldItemTameWeaponsRegression.Fixture f, LivingEntity first, LivingEntity second) {
        if (f.owner.connection == null) f.owner.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(f.h.getLevel().getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), f.owner);
        TameDuelManager.startTeamDuel(f.h.getLevel().getServer(), f.owner.getUUID(), Set.of(first.getUUID()),
                UUID.randomUUID(), Set.of(second.getUUID()));
        check(TameDuelManager.areDuelOpponents(first.getUUID(), second.getUUID()), "opponents registered");
    }
    @GameTest(template="held_item_arena") public static void duelTossPreservesItemAndNbt(GameTestHelper h) throws Exception {
        try (var f = new HeldItemTameWeaponsRegression.Fixture(h)) {
            var enemy = f.pet(false);
            var stack = new ItemStack(Items.DIAMOND, 3); stack.getOrCreateTag().putString("marker", "toss-retain");
            f.owner.getInventory().setItem(0, stack);
            start(f, f.owner, enemy);
            try {
                int before = f.owner.getInventory().countItem(Items.DIAMOND);
                var removed = f.owner.getInventory().removeItem(0, 3);
                var thrown = ForgeHooks.onPlayerTossEvent(f.owner, removed, true);
                int after = f.owner.getInventory().countItem(Items.DIAMOND);
                check(after == before, "canceled duel toss must return removed items; before=" + before + " after=" + after);
                check(thrown == null, "refunded stack is not also spawned");
                check(f.owner.getInventory().getItem(0).getOrCreateTag().getString("marker").equals("toss-retain"), "exact NBT retained");
            } finally { TameDuelManager.endDuelsForOwner(h.getLevel().getServer(), f.owner.getUUID()); }
        }
        h.succeed();
    }
    @GameTest(template="held_item_arena") public static void ordinaryTossStillSpawnsItems(GameTestHelper h) throws Exception {
        try (var f = new HeldItemTameWeaponsRegression.Fixture(h)) {
            var thrown = ForgeHooks.onPlayerTossEvent(f.owner, new ItemStack(Items.DIAMOND, 3), true);
            check(thrown != null && thrown.isAlive() && thrown.getItem().getCount() == 3, "ordinary player drop stays in world");
            thrown.discard();
        }
        h.succeed();
    }
    @GameTest(template="held_item_arena") public static void raccoonWakesAndStopsWashingAtDuelStart(GameTestHelper h) throws Exception {
        try (var f = new HeldItemTameWeaponsRegression.Fixture(h)) {
            var raccoon = (EntityRaccoon) f.pet(true); var enemy = f.pet(false);
            raccoon.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BREAD));
            raccoon.setOrderedToSit(true); raccoon.setWashing(true);
            start(f, raccoon, enemy);
            try {
                raccoon.tick();
                boolean nativeSitting = raccoon.isSitting();
                boolean washing = raccoon.isWashing();
                boolean vanillaSit = raccoon.isOrderedToSit();
                check(!nativeSitting && !washing, "raccoon must leave native sitting/washing before combat; nativeSit=" + nativeSitting + " washing=" + washing + " vanillaSit=" + vanillaSit);
                check(raccoon.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal().getClass().getSimpleName().equals("RaccoonAIWash")), "wash goal cannot preempt melee in duel");
            } finally { TameDuelManager.endDuelsForOwner(h.getLevel().getServer(), f.owner.getUUID()); }
        }
        h.succeed();
    }

    static class SkeloFixture implements AutoCloseable {
        final HeldItemTameWeaponsRegression.Fixture f;
        final TamableAnimal pet;
        final LivingEntity target;
        final TameData data;
        SkeloFixture(GameTestHelper h) throws Exception {
            f = new HeldItemTameWeaponsRegression.Fixture(h);
            var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("legendary_monsters", "skeloraptor"));
            pet = (TamableAnimal) type.create(h.getLevel());
            pet.setTame(true); pet.setOwnerUUID(f.owner.getUUID()); pet.setNoAi(true);
            pet.setPos(f.owner.getX() + 2, f.owner.getY(), f.owner.getZ());
            h.getLevel().addFreshEntity(pet); f.entities.add(pet);
            data = new TameData(pet); data.tameClass = TameClass.NONE; data.hungerSaturation = 100000;
            data.attributeLevels.put("lifesteal", 1); TameRegistry.register(data);
            pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200); pet.setHealth(50);
            target = f.enemy(pet); target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(2000); target.setHealth(2000);
        }
        float hit(String kind) throws Exception {
            target.setHealth(target.getMaxHealth()); target.invulnerableTime = 0;
            target.removeAllEffects(); pet.setHealth(50);
            float before = target.getHealth();
            if (kind.equals("shockwave")) {
                var type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("legendary_monsters", "shockwave"));
                var wave = type.create(f.h.getLevel());
                wave.getClass().getMethod("setCaster", LivingEntity.class).invoke(wave, pet);
                wave.getClass().getMethod("setDamage", float.class).invoke(wave, 4F); wave.tickCount = 5;
                var method = wave.getClass().getDeclaredMethod("damage", LivingEntity.class); method.setAccessible(true); method.invoke(wave, target);
                wave.discard();
            } else if (kind.equals("vanilla")) {
                pet.doHurtTarget(target);
            } else {
                var method = pet.getClass().getDeclaredMethod("AreaAttack", float.class,float.class,float.class,float.class,int.class,boolean.class,float.class,boolean.class,float.class,boolean.class);
                method.setAccessible(true); method.invoke(pet, 6F,5F,360F,kind.equals("bite") ? 10F : 7F,0,false,0F,false,0F,false);
            }
            float dealt = before - target.getHealth();
            check(dealt > 0, kind + " hits actual target");
            check(pet.getHealth() > 50, kind + " triggers base-hit lifesteal");
            return dealt;
        }
        @Override public void close() { f.close(); }
    }
    static void nativeBonus(GameTestHelper h, String kind) throws Exception {
        try (var f = new SkeloFixture(h)) {
            float baseline = f.hit(kind);
            f.data.bonusDamage = 12; LevelSystem.reapplyTypeBasePlusBonuses(f.pet, f.data);
            float boosted = f.hit(kind);
            float bonus = boosted - baseline;
            check(Math.abs(bonus - 12) < .05F, kind + " must apply earned base damage exactly once; baseline=" + baseline + " boosted=" + boosted);
        }
        h.succeed();
    }
    @GameTest(template="held_item_arena") public static void skeloraptorBiteBonusAndBaseTriggers(GameTestHelper h) throws Exception { nativeBonus(h, "bite"); }
    @GameTest(template="held_item_arena") public static void skeloraptorTailBonusAndBaseTriggers(GameTestHelper h) throws Exception { nativeBonus(h, "tail"); }
    @GameTest(template="held_item_arena") public static void skeloraptorShockwaveBonusAndBaseTriggers(GameTestHelper h) throws Exception { nativeBonus(h, "shockwave"); }
    @GameTest(template="held_item_arena") public static void skeloraptorVanillaMeleeDoesNotDoubleBonus(GameTestHelper h) throws Exception { nativeBonus(h, "vanilla"); }
}
