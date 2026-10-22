package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.block.*;
import com.github.alexthe668.domesticationinnovation.server.entity.*;
import com.github.alexthe668.domesticationinnovation.server.item.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.gametest.*;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class SharedGuardianRegression {
    @SuppressWarnings("unchecked") static Map<UUID,ServerPlayer> players(GameTestHelper h)throws Exception{
        var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
        return (Map<UUID,ServerPlayer>)field.get(h.getLevel().getServer().getPlayerList());
    }
    static FakePlayer observer(GuardianChestRegression.Fixture f,UUID id,String name,List<String> messages){
        var player=new FakePlayer(f.h.getLevel(),new GameProfile(id,name)){
            @Override public void sendSystemMessage(Component message){messages.add(message.getString());}
        };
        player.setPos(f.home.getX(),f.home.getY(),f.home.getZ());
        f.h.getLevel().getServer().getProfileCache().add(player.getGameProfile());return player;
    }
    static ItemStack label(FakePlayer owner,String group){var stack=new ItemStack(DIBlockRegistry.DRUM.get().asItem());stack.setHoverName(Component.literal(owner.getGameProfile().getName()+": "+group));return stack;}
    static ItemStack deed(LivingEntity pet){var stack=new ItemStack(DIItemRegistry.DEED_OF_OWNERSHIP.get());stack.getOrCreateTag().putBoolean("HasBoundEntity",true);stack.getTag().putUUID("BoundEntity",pet.getUUID());return stack;}
    static Block diamond(GameTestHelper h){
        var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("sophisticatedstorage:diamond_chest"));
        h.assertTrue(block!=null&&block!=Blocks.AIR,"actual Sophisticated Storage diamond chest is loaded for this regression");return block;
    }
    static LoadedChestDrums.Chest replace(GuardianChestRegression.Fixture f,Block block,boolean above){
        ItemStack firstLabel=f.container.getItem(0).copy(),paper=f.container.getItem(1).copy();
        f.h.getLevel().setBlock(f.chest,block.defaultBlockState(),3);
        BlockPos drum=f.chest.below();
        if(above){
            f.h.getLevel().setBlock(drum,Blocks.AIR.defaultBlockState(),3);drum=f.chest.above();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)f.h.getLevel().setBlock(f.chest.below().offset(x,0,z),block.defaultBlockState(),3);
            f.h.getLevel().setBlock(drum,DIBlockRegistry.DRUM.get().defaultBlockState(),3);
            var entity=(DrumBlockEntity)f.h.getLevel().getBlockEntity(drum);entity.setPlacerUUID(UUID.randomUUID());LoadedChestDrums.add(entity);
        }
        var entity=f.h.getLevel().getBlockEntity(f.chest);
        var inventory=entity.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("missing inventory"));
        ((IItemHandlerModifiable)inventory).setStackInSlot(0,firstLabel);((IItemHandlerModifiable)inventory).setStackInSlot(1,paper);
        return new LoadedChestDrums.Chest(entity,inventory,f.owner.getUUID(),0,drum);
    }
    @GameTest(template="empty") public static void actualDiamondChestConvertsPapersThroughHandlerAndSavesThem(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            var setup=replace(f,diamond(h),false);GuardianChestAutomation.scan(setup);
            h.assertTrue(!(setup.blockEntity() instanceof net.minecraft.world.level.block.entity.ChestBlockEntity),"fixture exercises a real non-vanilla chest entity");
            h.assertTrue(GuardianChestAutomation.isPaper(setup.inventory().getStackInSlot(1)),"diamond chest paper is converted through its modifiable handler");
            h.assertTrue(setup.blockEntity().saveWithoutMetadata().toString().contains(GuardianChestAutomation.PAPER),"Sophisticated Storage serialized inventory contains the converted paper");
            h.assertTrue(LoadedChestDrums.chests(h.getLevel()).stream().filter(c->c.pos().equals(f.chest)).count()==1,"foundation center is not a competing second controller");
            h.getLevel().setBlock(f.power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);GuardianChestAutomation.scan(setup);f.tick();
            h.assertTrue(f.data.hasHome&&f.wolf.blockPosition().equals(f.post),"actual modded chest deploys its guardian");
        }h.succeed();
    }
    @GameTest(template="empty") public static void drumAboveDiamondChestAndNineModdedBaseChestsWork(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            try{
                var setup=replace(f,diamond(h),true);var discovered=LoadedChestDrums.chests(h.getLevel()).stream().filter(c->c.pos().equals(f.chest)).findFirst().orElseThrow();
                h.assertTrue(discovered.drumPos().equals(f.chest.above())&&GuardianChestAutomation.foundationCount(discovered)==9,"inverted layout discovers the real drum and nine diamond base chests");
                GuardianChestAutomation.scan(discovered);h.getLevel().setBlock(f.power,Blocks.REDSTONE_BLOCK.defaultBlockState(),3);GuardianChestAutomation.scan(discovered);f.tick();
                h.assertTrue(f.data.hasHome&&f.wolf.blockPosition().equals(f.post),"a drum placed by a different owner still controls the shared chest");
            }finally{h.getLevel().setBlock(f.chest.above(),Blocks.AIR.defaultBlockState(),3);}
        }h.succeed();
    }
    @GameTest(template="empty") public static void twoOwnersShareLabelsPapersAndRedstoneInOneChest(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            List<String> messages=new ArrayList<>();var second=observer(f,UUID.randomUUID(),"SharedOwner",messages);players(h).put(second.getUUID(),second);
            var wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,second.getUUID());wolf.setNoAi(true);wolf.setPos(second.getX(),second.getY(),second.getZ());h.getLevel().addFreshEntity(wolf);
            var data=new TameData(wolf);data.name="OtherOwnerGuard";TameRegistry.register(data);LoadedTameIndex.refresh(wolf);data.guardianRestHome=f.data.guardianRestHome.copy();
            try{
                TameCommands.setGuardianPaperPost(data,"otherpost",h.getLevel(),f.post.south(3));
                f.container.setItem(2,label(second,"otherpost"));f.container.setItem(3,deed(wolf));messages.clear();f.scan();
                h.assertTrue(GuardianChestAutomation.isPaper(f.container.getItem(3))&&messages.isEmpty(),"other owner's paper is converted without automatic chat: "+messages);
                h.assertTrue(GuardianChestAutomation.status(new LoadedChestDrums.Status(f.setup,null)).contains("2 owner/group"),"status explains both shared owner/group labels");
                f.power(true);f.tick();f.tick();h.assertTrue(f.data.hasHome&&data.hasHome&&wolf.blockPosition().equals(f.post.south(3)),"one signal deploys both owners to their separate posts");
                f.power(false);f.tick();f.tick();h.assertTrue(!f.data.hasHome&&!data.hasHome,"one falling edge releases both owners' guardians");
                h.assertTrue(messages.isEmpty(),"automatic deployments and recalls remain silent");
            }finally{players(h).remove(second.getUUID());TameRegistry.remove(wolf.getUUID());wolf.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void anotherHolderCanSetDeployAndPunchReleaseWithoutAdopting(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            f.scan();var holder=observer(f,UUID.randomUUID(),"PaperHolder",new ArrayList<>());holder.setItemInHand(InteractionHand.MAIN_HAND,f.paper);
            var ground=f.chest.offset(7,-2,3);
            GuardianChestAutomation.usePaper(new PlayerInteractEvent.RightClickBlock(holder,InteractionHand.MAIN_HAND,ground,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false)));
            h.assertTrue(f.data.guardianSetAnchors.get("night").getInt("x")==ground.getX(),"another holder can edit the paper's bound tame post");
            holder.setShiftKeyDown(true);GuardianChestAutomation.useInAir(new PlayerInteractEvent.RightClickItem(holder,InteractionHand.MAIN_HAND));f.tick();
            h.assertTrue(f.data.hasHome&&f.wolf.blockPosition().equals(ground.above()),"another holder deploys the original owner's tame");
            holder.getCooldowns().removeCooldown(DIItemRegistry.DEED_OF_OWNERSHIP.get());var attack=new AttackEntityEvent(holder,f.wolf);GuardianChestAutomation.punchEntity(attack);f.tick();
            h.assertTrue(attack.isCanceled()&&!f.data.hasHome&&f.data.movementOrder==1&&f.wolf.blockPosition().equals(f.home),"punching recalls home and sits without damaging the target");
            h.assertTrue(f.owner.getUUID().equals(f.data.ownerUUID)&&f.owner.getUUID().equals(TameEntityAdapter.ownerUuid(f.wolf))&&GuardianChestAutomation.isPaper(f.paper),"management never transfers ownership or consumes the paper");
        }h.succeed();
    }
    @GameTest(template="empty") public static void automaticChestChangesStaySilentAndManualStatusRemainsAvailable(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            List<String> messages=new ArrayList<>();var owner=observer(f,f.owner.getUUID(),f.owner.getGameProfile().getName(),messages);players(h).put(owner.getUUID(),owner);
            try{
                h.getLevel().setBlock(f.chest.below(2).east(),Blocks.AIR.defaultBlockState(),3);f.scan();f.scan();
                h.assertTrue(messages.isEmpty(),"invalid base scans do not send automatic chat");
                h.getLevel().setBlock(f.chest.below(2).east(),Blocks.CHEST.defaultBlockState(),3);GuardianChestAutomation.stop(null);f.scan();
                h.assertTrue(messages.isEmpty()&&GuardianChestAutomation.isPaper(f.container.getItem(1)),"paper conversion succeeds silently");
                f.data.guardianSetAnchors.clear();f.container.setItem(2,new ItemStack(DIItemRegistry.DEED_OF_OWNERSHIP.get()));
                String status=GuardianChestAutomation.status(new LoadedChestDrums.Status(f.setup,null));
                h.assertTrue(status.contains("missing posts 1")&&status.contains("unbound papers 1"),"status distinguishes missing saved posts from unbound adoption papers");
            }finally{players(h).remove(owner.getUUID());}
        }h.succeed();
    }
    @GameTest(template="empty") public static void interfaceTamesAcceptCollarThroughSpecificInteractionAndPersistIt(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            for(EntityType<?> type:List.of(EntityType.FROG,EntityType.FOX,EntityType.RABBIT,EntityType.AXOLOTL)){
                var pet=(LivingEntity)type.create(h.getLevel());h.assertTrue(pet instanceof ModifedToBeTameable,"fixture is a DI interface tame");TameEntityAdapter.setOwner(pet,f.owner.getUUID());
                pet.setPos(f.home.getX(),f.home.getY(),f.home.getZ());h.getLevel().addFreshEntity(pet);var data=new TameData(pet,f.owner.getUUID(),false);TameRegistry.register(data);LoadedTameIndex.refresh(pet);
                try{
                    f.owner.getCooldowns().removeCooldown(DIItemRegistry.COLLAR_TAG.get());var collar=new ItemStack(DIItemRegistry.COLLAR_TAG.get(),2);collar.enchant(Enchantments.ALL_DAMAGE_PROTECTION,2);f.owner.setItemInHand(InteractionHand.MAIN_HAND,collar);
                    var event=new PlayerInteractEvent.EntityInteractSpecific(f.owner,InteractionHand.MAIN_HAND,pet,Vec3.ZERO);MinecraftForge.EVENT_BUS.post(event);
                    h.assertTrue(event.isCanceled()&&TameableUtils.hasCollar(pet)&&data.hasCollarTag,"specific interaction applies collar and updates registry for "+type);
                    h.assertTrue(data.collarTagTier==2&&TameableUtils.getEnchantLevel(pet,Enchantments.ALL_DAMAGE_PROTECTION)==2,"interface collar tier and enchant data are preserved");
                    h.assertTrue(f.owner.isCreative()||collar.getCount()==1,"one collar is consumed");
                    h.assertTrue(!data.entitySnapshot.isEmpty(),"interface collar application immediately saves its entity snapshot");
                }finally{TameRegistry.remove(pet.getUUID());pet.discard();}
            }
        }h.succeed();
    }
    @GameTest(template="empty") public static void collarStillRequiresActualOwnerAndSupportsHungryVanillaTame(GameTestHelper h){
        try(var f=new GuardianChestRegression.Fixture(h)){
            var stranger=observer(f,UUID.randomUUID(),"NotTheOwner",new ArrayList<>());var collar=new ItemStack(DIItemRegistry.COLLAR_TAG.get());stranger.setItemInHand(InteractionHand.MAIN_HAND,collar);
            MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteractSpecific(stranger,InteractionHand.MAIN_HAND,f.wolf,Vec3.ZERO));
            h.assertTrue(!TameableUtils.hasCollar(f.wolf)&&collar.getCount()==1,"non-owner cannot attach or spend a collar on another owner's tame");
            f.data.hungerForcedSit=true;f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(DIItemRegistry.COLLAR_TAG.get()));
            MinecraftForge.EVENT_BUS.post(new PlayerInteractEvent.EntityInteractSpecific(f.owner,InteractionHand.MAIN_HAND,f.wolf,Vec3.ZERO));
            h.assertTrue(TameableUtils.hasCollar(f.wolf)&&f.data.hasCollarTag,"hunger movement lock does not reject owner collar management");
        }h.succeed();
    }
    @GameTest(template="empty") public static void protectedBlockPunchRecallsGuardianAndStatusCommandIsAvailable(GameTestHelper h)throws Exception{
        try(var f=new GuardianChestRegression.Fixture(h)){
            f.scan();TameCommands.guardianAutomationMove(h.getLevel().getServer(),f.data,"night",true,null);
            List<String> messages=new ArrayList<>();var holder=observer(f,UUID.randomUUID(),"BlockHolder",messages);holder.setItemInHand(InteractionHand.MAIN_HAND,f.paper);
            BlockPos ground=f.chest.south(5);h.getLevel().setBlock(ground,Blocks.STONE.defaultBlockState(),3);
            var click=new PlayerInteractEvent.LeftClickBlock(holder,ground,Direction.UP);click.setCanceled(true);MinecraftForge.EVENT_BUS.post(click);f.tick();
            h.assertTrue(click.isCanceled()&&h.getLevel().getBlockState(ground).is(Blocks.STONE)&&!f.data.hasHome&&f.wolf.blockPosition().equals(f.home),"paper release works even when a claim/protection already canceled block punching, without breaking the block");
            int result=h.getLevel().getServer().getCommands().getDispatcher().execute("tames chestxdrum status",holder.createCommandSourceStack());
            h.assertTrue(result==1&&messages.stream().anyMatch(m->m.contains("Guardian automation:")),"the advertised status command provides diagnostics to another owner");
        }h.succeed();
    }
}

