package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class WeaponRetrievalStateRegression {
    static void check(boolean v,String s){if(!v)throw new GameTestAssertException(s);}
    @GameTest(template="held_item_arena") public static void removingWeaponConsumesFollowupClicksAndPreservesAllOrders(GameTestHelper h)throws Exception{
        for(int command=0;command<3;command++)try(var f=new HeldItemTameWeaponsRegression.Fixture(h)){
            var pet=(EntityRaccoon)f.pet(true);pet.setCommand(command);pet.setOrderedToSit(command==2);
            var data=TameRegistry.get(pet.getUUID());data.mode=5;data.hasHome=true;data.homeX=123;data.homeY=64;data.homeZ=456;
            pet.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);f.owner.setSprinting(true);
            var specific=ForgeHooks.onInteractEntityAt(f.owner,pet,new Vec3(0,0.5,0),InteractionHand.MAIN_HAND);
            check(specific!=null&&specific.consumesAction(),"retrieval consumes initial specific interaction");
            check(pet.getMainHandItem().isEmpty()&&f.owner.getMainHandItem().is(Items.DIAMOND_SWORD),"weapon transferred exactly once");
            f.owner.interactOn(pet,InteractionHand.OFF_HAND);
            f.owner.interactOn(pet,InteractionHand.MAIN_HAND);
            check(pet.getCommand()==command&&pet.isSitting()==(command==2),"removing weapon must preserve native state across specific/main/offhand callbacks; initial="+command+" actual="+pet.getCommand());
            check(data.mode==5&&data.hasHome&&data.homeX==123&&data.homeY==64&&data.homeZ==456,"mode and guardian anchor preserved");
            check(f.owner.getMainHandItem().getCount()==1,"followups do not duplicate weapon");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void ordinaryRaccoonInteractionStillChangesCommand(GameTestHelper h)throws Exception{
        try(var f=new HeldItemTameWeaponsRegression.Fixture(h)){
            var pet=(EntityRaccoon)f.pet(true);pet.setCommand(0);f.owner.setSprinting(false);
            f.owner.interactOn(pet,InteractionHand.MAIN_HAND);
            check(pet.getCommand()!=0,"ordinary command interaction still works");
        }h.succeed();
    }
    @GameTest(template="held_item_arena") public static void retrievalGuardExpiresAndAllowsLaterCommands(GameTestHelper h)throws Exception{
        var f=new HeldItemTameWeaponsRegression.Fixture(h);
        var pet=(EntityRaccoon)f.pet(true);pet.setCommand(0);
        pet.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND_SWORD));
        f.owner.setSprinting(true);f.owner.interactOn(pet,InteractionHand.MAIN_HAND);
        check(pet.getCommand()==0&&pet.getMainHandItem().isEmpty(),"initial retrieval preserves command");
        f.owner.setSprinting(false);
        h.runAfterDelay(3,()->{
            try{
                f.owner.interactOn(pet,InteractionHand.MAIN_HAND);
                check(pet.getCommand()!=0,"later deliberate interactions can change command again");
                h.succeed();
            }finally{f.close();}
        });
    }
}
