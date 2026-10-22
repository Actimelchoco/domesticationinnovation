package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.*;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.IItemHandler;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class GenericGuardianInventoryRegression {
    static IItemHandler generic(IItemHandler delegate,boolean reject){return new IItemHandler(){
        public int getSlots(){return delegate.getSlots();}
        public ItemStack getStackInSlot(int slot){return delegate.getStackInSlot(slot);}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return delegate.extractItem(slot,amount,simulate);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return reject&&GuardianChestAutomation.isPaper(stack)?stack:delegate.insertItem(slot,stack,simulate);}
        public int getSlotLimit(int slot){return delegate.getSlotLimit(slot);}
        public boolean isItemValid(int slot,ItemStack stack){return delegate.isItemValid(slot,stack);}
    };}
    @GameTest(template="empty") public static void barrelControllerUsesSameDrumFormation(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            var label=f.container.getItem(0).copy();var paper=f.paper.copy();
            h.getLevel().setBlock(f.chest,Blocks.BARREL.defaultBlockState(),3);
            var container=(BarrelBlockEntity)h.getLevel().getBlockEntity(f.chest);container.setItem(0,label);container.setItem(1,paper);
            var setup=LoadedChestDrums.chestAtDrum(h.getLevel(),f.chest.below());
            h.assertTrue(setup!=null&&setup.pos().equals(f.chest)&&GuardianChestAutomation.foundationCount(setup)==9,"barrel control and nine drums are recognized");
            GuardianChestAutomation.scan(setup);
            h.assertTrue(GuardianChestAutomation.isPaper(container.getItem(1)),"barrel's real inventory persists converted paper");
        }h.succeed();
    }
    @GameTest(template="empty") public static void genericHandlerConvertsAndRejectedConversionRestoresPaper(GameTestHelper h){
        for(boolean reject:new boolean[]{false,true})try(var f=new GuardianChestRegression.Fixture(h)){
            var original=f.paper.copy();var inventory=generic(f.setup.inventory(),reject);
            var setup=new LoadedChestDrums.Chest(f.container,inventory,f.owner.getUUID(),0,f.chest.below());
            GuardianChestAutomation.scan(setup);
            h.assertTrue(reject?ItemStack.matches(f.container.getItem(1),original):GuardianChestAutomation.isPaper(f.container.getItem(1)),"generic extract/insert converts papers or restores the original on rejection");
            h.assertTrue(f.container.getItem(1).getCount()==1,"conversion never duplicates or loses the deed");
        }h.succeed();
    }
    @GameTest(template="empty") public static void sidedOnlyAndContainerOnlyInventoriesAreSupported(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            var optional=LazyOptional.of(()->generic(f.setup.inventory(),false));
            var sided=new BlockEntity(BlockEntityType.CHEST,f.chest,Blocks.CHEST.defaultBlockState()){
                @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction face){
                    return cap==ForgeCapabilities.ITEM_HANDLER&&face==Direction.NORTH?optional.cast():LazyOptional.empty();
                }
            };
            h.assertTrue(LoadedChestDrums.inventory(sided,Direction.DOWN)==optional.orElse(null),"side-only item capability is discovered without an unsided handler");
            var container=new ChestBlockEntity(f.chest,Blocks.CHEST.defaultBlockState()){
                @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction face){return LazyOptional.empty();}
            };
            container.setItem(0,f.paper.copy());
            var wrapper=LoadedChestDrums.inventory(container,Direction.UP);
            h.assertTrue(wrapper!=null&&ItemStack.matches(wrapper.getStackInSlot(0),f.paper),"Container-only block entity is wrapped without a Forge capability");
        }h.succeed();
    }
}
