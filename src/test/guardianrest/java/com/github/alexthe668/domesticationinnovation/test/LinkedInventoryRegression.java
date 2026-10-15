package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class LinkedInventoryRegression {
    @GameTest(template="empty")
    public static void linkedPickupConservesItems(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();UUID id=UUID.randomUUID();
        var owner=FakePlayerFactory.get(level,new GameProfile(id,"LinkedOwner"));
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        @SuppressWarnings("unchecked") var players=(Map<UUID,ServerPlayer>)field.get(level.getServer().getPlayerList());
        players.put(id,owner);
        Wolf wolf=EntityType.WOLF.create(level);wolf.setTame(true);wolf.setOwnerUUID(id);level.addFreshEntity(wolf);
        TameData data=new TameData(wolf);data.attributeLevels.put("linked_inventory",1);TameRegistry.register(data);
        var pickup=net.minecraft.world.entity.Mob.class.getDeclaredMethod("pickUpItem",ItemEntity.class);pickup.setAccessible(true);
        List<ItemEntity> drops=new ArrayList<>();
        try {
            for(int scenario=0;scenario<4;scenario++) {
                owner.getInventory().clearContent();
                if(scenario==1||scenario==2) {
                    for(int slot=0;slot<36;slot++)owner.getInventory().setItem(slot,new ItemStack(Items.COBBLESTONE,64));
                    if(scenario==1)owner.getInventory().setItem(0,new ItemStack(Items.DIAMOND,60));
                }
                if(scenario==3)players.remove(id);
                ItemEntity item=new ItemEntity(level,0,2,0,new ItemStack(Items.DIAMOND,10));level.addFreshEntity(item);drops.add(item);
                pickup.invoke(wolf,item);
                int expectedInserted=scenario==0?10:scenario==1?4:0;
                int inventory=owner.getInventory().countItem(Items.DIAMOND)-(scenario==1?60:0);
                check(inventory==expectedInserted,"owner receives correct quantity in scenario "+scenario);
                check((item.isRemoved()?0:item.getItem().getCount())==10-expectedInserted,"world retains every untransferred item in scenario "+scenario);
                check(wolf.getMainHandItem().isEmpty(),"linked pickup never equips the drop");
            }
        } finally {
            players.remove(id);drops.forEach(ItemEntity::discard);wolf.discard();TameRegistry.remove(data.uuid);
        }
        helper.succeed();
    }
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
}
