package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameAbilityEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameFoodManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import java.lang.reflect.Method;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class EternalSteakRegression {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static boolean eat(TameData data, LivingEntity tame) throws Exception {
        Method method = TameCommands.class.getDeclaredMethod("consumeOneHungerFood", TameData.class, LivingEntity.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null, data, tame);
    }

    private static long interval(TameData data, LivingEntity tame) throws Exception {
        Method method = TameAbilityEvents.class.getDeclaredMethod("passiveHealInterval", LivingEntity.class, TameData.class);
        method.setAccessible(true);
        return (long) method.invoke(null, tame, data);
    }

    @GameTest(template = "empty")
    public static void reusableSteakAndDietHealing(GameTestHelper helper) throws Exception {
        ItemStack steak = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("artifacts", "eternal_steak")));
        check(TameFoodManager.isEternalSteak(steak), "real Artifacts Eternal Steak must exist");
        Method meat = TameFoodManager.class.getDeclaredMethod("matchesRule", ItemStack.class, String.class);
        meat.setAccessible(true);
        check((boolean) meat.invoke(null, steak, "#meat"), "Steak must count as meat");
        Wolf wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setOwnerUUID(UUID.randomUUID()); wolf.setTame(true);
        TameData wolfData = new TameData(wolf);
        wolfData.hungerInventory.add(steak.copy());
        check(eat(wolfData, wolf), "wolf must eat steak");
        check(wolfData.lastConsumedFoodPreferred && interval(wolfData, wolf) == 100L, "wolf must prefer steak and heal normally");
        check(wolfData.hungerInventory.get(0).getCount() == 1, "preferred steak must survive feeding");
        Cat cat = EntityType.CAT.create(helper.getLevel());
        cat.setOwnerUUID(UUID.randomUUID()); cat.setTame(true);
        TameData data = new TameData(cat);
        data.hungerInventory.add(steak.copy());
        for (int i = 0; i < 3; i++) check(eat(data, cat), "cat without Gluttonous must eat steak repeatedly");
        check(!data.lastConsumedFoodPreferred && data.lastConsumedFoodEternalSteak, "cat must not prefer steak");
        check(data.hungerInventory.size() == 1 && data.hungerInventory.get(0).getCount() == 1, "nonpreferred steak must survive repeated feeding");
        check(data.hungerSaturation > 0 && interval(data, cat) == 400L, "steak must provide hunger and quarter-speed base healing");
        TameData restored = TameData.fromTag(data.toTag());
        check(interval(restored, cat) == 400L, "healing penalty must survive saving");
        data.hungerInventory.clear(); data.hungerInventory.add(new ItemStack(Items.BREAD));
        check(eat(data, cat) && interval(data, cat) == 200L && !data.lastConsumedFoodEternalSteak, "ordinary fallback must replace steak penalty");
        data.hungerInventory.add(new ItemStack(Items.COD));
        check(eat(data, cat) && interval(data, cat) == 100L, "preferred meal must restore normal healing");
        helper.succeed();
    }
}
