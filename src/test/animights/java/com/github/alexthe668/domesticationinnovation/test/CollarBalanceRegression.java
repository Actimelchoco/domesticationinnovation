package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.CommonProxy;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling.LevelSystem;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class CollarBalanceRegression {
    private static void near(double actual,double expected,String message){if(Math.abs(actual-expected)>.0001)throw new AssertionError(message+": "+actual);}
    @GameTest(template="empty")
    public static void tiersMultiplyBaseStatsWithoutExtraDamageReduction(GameTestHelper h){
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
        wolf.getAttribute(Attributes.ARMOR).setBaseValue(3);wolf.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(2);
        UUID gear=UUID.randomUUID();wolf.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(gear,"equipment fixture",8,AttributeModifier.Operation.ADDITION));
        try{
            for(int tier=1;tier<=12;tier++){
                TameableUtils.clearEnchants(wolf);TameableUtils.setHasCollar(wolf,true);
                TameableUtils.addEnchant(wolf,new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION,tier));
                TameableUtils.refreshCollarAttributes(wolf);TameableUtils.refreshCollarDefense(wolf);
                near(wolf.getAttributeValue(Attributes.ARMOR),Attributes.ARMOR.sanitizeValue(3*tier+8),"base scales once; gear does not");
                near(wolf.getAttributeValue(Attributes.ARMOR_TOUGHNESS),Attributes.ARMOR_TOUGHNESS.sanitizeValue(2*tier),"toughness scales once");
                var hit=new LivingDamageEvent(wolf,h.getLevel().damageSources().mobAttack(wolf),100);new CommonProxy().onLivingDamage(hit);
                near(hit.getAmount(),100,"no second post-armor collar reduction");
            }
            TameableUtils.setHasCollar(wolf,false);TameableUtils.refreshCollarDefense(wolf);
            near(wolf.getAttributeValue(Attributes.ARMOR),11,"collar removal preserves base and gear");
            near(wolf.getAttributeValue(Attributes.ARMOR_TOUGHNESS),2,"collar removal restores toughness");
        }finally{wolf.discard();}h.succeed();
    }
    @GameTest(template="empty")
    public static void oldLevelArmorMigratesAndStatChangesRefresh(GameTestHelper h){
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());var data=new TameData(wolf);
        data.level=100;data.bonusArmor=2;
        wolf.getAttribute(Attributes.ARMOR).addPermanentModifier(new AttributeModifier(UUID.fromString("acbe2d2a-bb17-4f8a-881b-8de25c182adf"),"tl_level_armor",100,AttributeModifier.Operation.ADDITION));
        try{
            LevelSystem.reapplyTypeBasePlusBonuses(wolf,data);near(wolf.getAttributeValue(Attributes.ARMOR),2,"old per-level modifier removed; bonus retained");
            TameableUtils.setHasCollar(wolf,true);TameableUtils.addEnchant(wolf,new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION,3));
            near(wolf.getAttributeValue(Attributes.ARMOR),6,"bonus armor scales with tier");
            data.level=1;LevelSystem.syncLevelArmor(wolf,data);near(wolf.getAttributeValue(Attributes.ARMOR),6,"tame level has no flat armor effect");
            data.bonusArmor=4;LevelSystem.reapplyTypeBasePlusBonuses(wolf,data);near(wolf.getAttributeValue(Attributes.ARMOR),12,"updated bonus scales without compounding");
            var restored=EntityType.WOLF.create(h.getLevel());restored.load(wolf.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
            LevelSystem.reapplyTypeBasePlusBonuses(restored,TameData.fromTag(data.toTag()));near(restored.getAttributeValue(Attributes.ARMOR),12,"save/load keeps exactly one collar multiplier");restored.discard();
        }finally{wolf.discard();}h.succeed();
    }
    @GameTest(template="empty")
    public static void collarCannotCreateDefenseFromNothing(GameTestHelper h){
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
        TameableUtils.setHasCollar(wolf,true);TameableUtils.addEnchant(wolf,new EnchantmentInstance(Enchantments.ALL_DAMAGE_PROTECTION,12));
        near(wolf.getAttributeValue(Attributes.ARMOR),0,"zero armor remains zero");near(wolf.getAttributeValue(Attributes.ARMOR_TOUGHNESS),0,"zero toughness remains zero");
        wolf.discard();h.succeed();
    }
}
