package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import it.hurts.sskirillss.relics.items.relics.base.IRelicItem;
import it.hurts.sskirillss.relics.items.relics.belt.HunterBeltItem;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(DomesticationMod.MODID)
@PrefixGameTestTemplate(false)
public final class HunterBeltRegression {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static Map<UUID, ServerPlayer> onlinePlayers(GameTestHelper helper) throws Exception {
        var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        field.setAccessible(true);
        return (Map<UUID, ServerPlayer>) field.get(helper.getLevel().getServer().getPlayerList());
    }

    private static void damage(GameTestHelper helper, LivingEntity attacker, float expected) {
        LivingHurtEvent event = new LivingHurtEvent(attacker, helper.getLevel().damageSources().mobAttack(attacker), 10);
        // Exercise the real transformed native listener and our compatibility listener together.
        HunterBeltItem.HunterBeltEvents.onLivingDamage(event);
        new CommonProxy().onInterfaceTameHunterBeltDamage(event);
        check(Math.abs(event.getAmount() - expected) < 0.001F,
                "expected damage " + expected + " but got " + event.getAmount());
    }

    @GameTest(template = "empty")
    public static void allWornHunterBeltsStack(GameTestHelper helper) throws Exception {
        // Avoid the vanilla mock login's channel-less connection: Relics syncs on login.
        ServerPlayer owner = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "HunterBeltTest"));
        onlinePlayers(helper).put(owner.getUUID(), owner);
        Wolf wolf = helper.spawn(EntityType.WOLF, new BlockPos(1, 1, 1));
        Fox fox = helper.spawn(EntityType.FOX, new BlockPos(2, 1, 1));
        wolf.setTame(true);
        wolf.setOwnerUUID(owner.getUUID());
        var modified = (com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable) fox;
        modified.setTame(true);
        modified.setTameOwnerUUID(owner.getUUID());
        TameRegistry.register(new TameData(wolf));
        TameRegistry.register(new TameData(fox, owner.getUUID(), true));
        var inventory = CuriosApi.getCuriosInventory(owner).orElseThrow(() -> new AssertionError("Curios inventory missing"));
        var originalSlots = new java.util.HashMap<>(inventory.getCurios());
        CurioStacksHandler beltSlots = new CurioStacksHandler(inventory, "belt", 3, true, true, true, ICurio.DropRule.DEFAULT);
        inventory.setCurios(new java.util.HashMap<>(Map.of("belt", beltSlots)));
        var belt = ForgeRegistries.ITEMS.getValue(new ResourceLocation("relics", "hunter_belt"));
        IRelicItem relic = (IRelicItem) belt;
        ItemStack first = new ItemStack(belt);
        ItemStack second = new ItemStack(belt);
        ItemStack third = new ItemStack(belt);
        relic.setAbilityValue(first, "training", "damage", 1.5D);
        relic.setAbilityValue(second, "training", "damage", 2.0D);
        relic.setAbilityValue(third, "training", "damage", 1.25D);
        try {
            damage(helper, wolf, 10);
            beltSlots.getStacks().setStackInSlot(0, first);
            damage(helper, wolf, 15);
            beltSlots.getStacks().setStackInSlot(1, second);
            damage(helper, wolf, 25);
            beltSlots.getStacks().setStackInSlot(2, third);
            damage(helper, wolf, 27.5F);
            damage(helper, fox, 27.5F);
            check(relic.getExperience(third) >= 2, "each worn belt must receive training XP, including normal Relics XP sharing");
            // Inventory and cosmetic copies are not active belts.
            beltSlots.getStacks().setStackInSlot(2, ItemStack.EMPTY);
            owner.getInventory().add(third.copy());
            beltSlots.getCosmeticStacks().setStackInSlot(2, third);
            damage(helper, wolf, 25);
            beltSlots.getStacks().setStackInSlot(1, ItemStack.EMPTY);
            damage(helper, wolf, 15);
        } finally {
            inventory.setCurios(originalSlots);
            onlinePlayers(helper).remove(owner.getUUID());
            for (LivingEntity entity : List.of(wolf, fox)) {
                TameRegistry.remove(entity.getUUID());
                entity.discard();
            }
            owner.discard();
        }
        helper.succeed();
    }
}
