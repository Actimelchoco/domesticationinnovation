package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class CollarBalanceRegression {
    private static void near(double actual, double expected, String message) {
        if (Math.abs(actual - expected) > 0.0001) throw new AssertionError(message + ": " + actual);
    }

    @GameTest(template = "empty")
    public static void collarTierDamageAndBypass(GameTestHelper helper) {
        var wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        var proxy = new CommonProxy();
        for (int tier = 1; tier <= 12; tier++) {
            TameableUtils.clearEnchants(wolf);
            TameableUtils.setHasCollar(wolf, true);
            TameableUtils.addEnchant(wolf, new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION, tier));
            var hit = new LivingDamageEvent(wolf, helper.getLevel().damageSources().mobAttack(wolf), 100);
            proxy.onLivingDamage(hit);
            near(hit.getAmount(), 100 * (1.0 - tier * 0.67 / 12), "tier must scale evenly");
        }
        var bypassType = helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getTag(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS).orElseThrow().iterator().next();
        var bypass = new LivingDamageEvent(wolf, new net.minecraft.world.damagesource.DamageSource(bypassType), 100);
        proxy.onLivingDamage(bypass);
        near(bypass.getAmount(), 100, "enchantment-bypassing damage must remain unchanged");
        wolf.discard(); helper.succeed();
    }

    @GameTest(template = "empty")
    public static void levelArmorRefreshAndRestoration(GameTestHelper helper) {
        var wolf = EntityType.WOLF.create(helper.getLevel());
        wolf.setTame(true); wolf.setOwnerUUID(UUID.randomUUID());
        var data = new TameData(wolf);
        data.level = 10; data.bonusArmor = 2;
        LevelSystem.reapplyTypeBasePlusBonuses(wolf, data);
        near(wolf.getAttributeValue(Attributes.ARMOR), 12, "level armor must stack with reward armor");
        LevelSystem.reapplyTypeBasePlusBonuses(wolf, data);
        LevelSystem.syncLevelArmor(wolf, data);
        near(wolf.getAttributeValue(Attributes.ARMOR), 12, "repeated refresh must not duplicate level armor");
        data.level = 20;
        LevelSystem.syncLevelArmor(wolf, data);
        near(wolf.getAttributeValue(Attributes.ARMOR), 22, "level increase must update armor");
        data.level = 3;
        LevelSystem.syncLevelArmor(wolf, data);
        near(wolf.getAttributeValue(Attributes.ARMOR), 5, "level decrease must remove only lost level armor");
        TameableUtils.setHasCollar(wolf, true);
        TameableUtils.refreshCollarAttributes(wolf);
        near(wolf.getAttributeValue(Attributes.ARMOR), 10, "collar armor must stack with level and reward armor");
        var restored = EntityType.WOLF.create(helper.getLevel());
        restored.load(wolf.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        LevelSystem.reapplyTypeBasePlusBonuses(restored, TameData.fromTag(data.toTag()));
        near(restored.getAttributeValue(Attributes.ARMOR), 10, "saved/restored tame must retain exactly one level bonus");
        wolf.discard(); restored.discard(); helper.succeed();
    }
}
