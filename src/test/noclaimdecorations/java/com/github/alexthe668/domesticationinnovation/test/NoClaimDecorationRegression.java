package com.github.alexthe668.domesticationinnovation.test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.*;
import java.util.UUID;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class NoClaimDecorationRegression {
    @GameTest(template="empty")
    public static void absentFtbKeepsNormalTameDamage(GameTestHelper h) {
        if(ModList.get().isLoaded("ftbchunks"))throw new AssertionError("optional-dependency fixture must omit FTB");
        var wolf=EntityType.WOLF.create(h.getLevel());wolf.setTame(true);wolf.setOwnerUUID(UUID.randomUUID());
        var frame=new ItemFrame(h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),Direction.SOUTH);frame.setItem(new ItemStack(Items.DIAMOND));
        var stand=EntityType.ARMOR_STAND.create(h.getLevel());
        try{
            frame.hurt(h.getLevel().damageSources().mobAttack(wolf),1);
            if(!frame.getItem().isEmpty())throw new AssertionError("FTB absent: frame keeps normal item-pop behavior");
            stand.hurt(h.getLevel().damageSources().explosion(wolf,wolf),100);
            if(!stand.isRemoved())throw new AssertionError("FTB absent: armor stand keeps normal explosion damage");
        }finally{wolf.discard();frame.discard();stand.discard();}h.succeed();
    }
}
