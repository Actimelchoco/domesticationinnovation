package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.block.*;
import com.github.alexthe668.domesticationinnovation.server.item.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianChestRegression {
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final FakePlayer owner;final BlockPos chest,post,home,power;
        final Wolf wolf;final TameData data;final ChestBlockEntity container;final LoadedChestDrums.Chest setup;
        ItemStack paper;final Set<UUID> oldPool;
        Fixture(GameTestHelper h){
            this.h=h;GuardianChestAutomation.stop(null);oldPool=TameRegistry.getRankedParticipants();
            var profile=new GameProfile(UUID.randomUUID(),"Guard"+UUID.randomUUID().toString().substring(0,8));
            owner=new FakePlayer(h.getLevel(),profile);
            var server=h.getLevel().getServer();
            if(server.getProfileCache()==null)try{
                var field=net.minecraft.server.MinecraftServer.class.getDeclaredField("services");field.setAccessible(true);
                var old=(net.minecraft.server.Services)field.get(server);
                var cache=new net.minecraft.server.players.GameProfileCache(old.profileRepository(),new java.io.File("guardian-test-profiles.json"));
                field.set(server,new net.minecraft.server.Services(old.sessionService(),old.servicesKeySet(),old.profileRepository(),cache));
            }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
            server.getProfileCache().add(profile);
            chest=h.absolutePos(new BlockPos(2,4,2));post=chest.offset(5,-2,0);home=chest.offset(0,-2,7);power=chest.east();
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)h.getLevel().setBlock(chest.below(2).offset(x,0,z),Blocks.CHEST.defaultBlockState(),3);
            h.getLevel().setBlock(chest.below(),DIBlockRegistry.DRUM.get().defaultBlockState(),3);
            var drum=(DrumBlockEntity)h.getLevel().getBlockEntity(chest.below());drum.setPlacerUUID(owner.getUUID());LoadedChestDrums.add(drum);
            h.getLevel().setBlock(chest,Blocks.CHEST.defaultBlockState(),3);container=(ChestBlockEntity)h.getLevel().getBlockEntity(chest);
            var handler=container.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("no chest inventory"));
            setup=new LoadedChestDrums.Chest(container,handler,owner.getUUID(),0);
            var label=new ItemStack(DIBlockRegistry.DRUM.get().asItem());label.setHoverName(net.minecraft.network.chat.Component.literal(owner.getGameProfile().getName()+": night"));container.setItem(0,label);
            wolf=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(wolf,owner.getUUID());wolf.setNoAi(true);wolf.setPos(home.getX()+0.5,home.getY(),home.getZ()+0.5);h.getLevel().addFreshEntity(wolf);
            data=new TameData(wolf);data.name="ChestWolf";data.hungerInventory.clear();TameRegistry.register(data);LoadedTameIndex.refresh(wolf);
            CompoundTag rest=new CompoundTag();rest.putString("dimension",h.getLevel().dimension().location().toString());rest.putDouble("x",home.getX()+0.5);rest.putDouble("y",home.getY());rest.putDouble("z",home.getZ()+0.5);data.guardianRestHome=rest;
            TameCommands.setGuardianPaperPost(data,"night",h.getLevel(),post);
            paper=new ItemStack(DIItemRegistry.DEED_OF_OWNERSHIP.get());paper.getOrCreateTag().putBoolean("HasBoundEntity",true);paper.getTag().putUUID("BoundEntity",wolf.getUUID());paper.getTag().putString("BoundEntityName",data.name);container.setItem(1,paper);
        }
        void scan(){GuardianChestAutomation.scan(setup);if(!container.getItem(1).isEmpty())paper=container.getItem(1);}
        void tick(){GuardianChestAutomation.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END,()->true,h.getLevel().getServer()));}
        void power(boolean on){h.getLevel().setBlock(power,on?Blocks.REDSTONE_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);scan();}
        @Override public void close(){
            TameDuelManager.endDuelForEntity(h.getLevel().getServer(),data.uuid);GuardianChestAutomation.stop(null);
            TameRegistry.setRankedParticipants(oldPool);TameRegistry.remove(data.uuid);wolf.discard();
            var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);if(live!=null)live.discard();
            h.getLevel().setBlock(chest,Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(chest.below(),Blocks.AIR.defaultBlockState(),3);h.getLevel().setBlock(power,Blocks.AIR.defaultBlockState(),3);
        }
    }
    @GameTest(template="empty") public static void guardianChestRequiresNineChestsAndPermanentlyConvertsDeed(GameTestHelper h){
        try(var f=new Fixture(h)){
            var missing=f.chest.below(2).offset(1,0,1);h.getLevel().setBlock(missing,Blocks.AIR.defaultBlockState(),3);f.scan();
            h.assertTrue(!GuardianChestAutomation.isPaper(f.paper),"eight base chests do not enable automation");
            h.getLevel().setBlock(missing,Blocks.CHEST.defaultBlockState(),3);f.scan();
            h.assertTrue(GuardianChestAutomation.isPaper(f.paper)&&f.paper.getHoverName().getString().equals("night: ChestWolf"),"nine chests convert and label the bound paper");
            h.assertTrue(!f.paper.getTag().contains("BoundEntity")&&!f.paper.getTag().getBoolean("HasBoundEntity"),"adoption credentials are removed permanently");
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,f.paper);
            var interaction=new PlayerInteractEvent.EntityInteract(f.owner,InteractionHand.MAIN_HAND,f.wolf);MinecraftForge.EVENT_BUS.post(interaction);
            h.assertTrue(interaction.isCanceled()&&GuardianChestAutomation.isPaper(f.paper),"guardian papers cannot adopt or be rebound by entity interaction");
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianChestRedstoneDeploysOverridesOtherPostAndReturnsHome(GameTestHelper h){
        try(var f=new Fixture(h)){
            f.scan();f.data.hasHome=true;f.data.homeDimension=h.getLevel().dimension().location().toString();f.data.homeX=f.home.getX();f.data.homeY=f.home.getY();f.data.homeZ=f.home.getZ();f.data.movementOrder=3;
            f.power(true);f.tick();
            h.assertTrue(f.data.hasHome&&f.data.movementOrder==3&&f.data.homeX==f.post.getX()&&f.wolf.blockPosition().equals(f.post),"ON overrides an existing guardian deployment");
            f.power(false);f.tick();
            h.assertTrue(!f.data.hasHome&&f.data.movementOrder==1&&f.wolf.blockPosition().equals(f.home),"OFF releases guardian duty and sends it home sitting");
        }h.succeed();
    }
    @GameTest(template="empty") public static void removedPaperCancelsQueuedMovesAndBodyguardsAreProtected(GameTestHelper h){
        try(var f=new Fixture(h)){
            f.scan();f.power(true);f.container.setItem(1,ItemStack.EMPTY);f.tick();
            h.assertTrue(!f.data.hasHome&&f.wolf.blockPosition().equals(f.home),"removing paper cancels even a queued redstone activation");
            f.container.setItem(1,f.paper);f.data.rosterBodyguard=true;f.scan();f.tick();
            h.assertTrue(f.data.rosterBodyguard&&!f.data.hasHome&&f.wolf.blockPosition().equals(f.home),"automation cannot override a bodyguard");
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianPaperSetsGroupPostAndSneakDeploys(GameTestHelper h){
        try(var f=new Fixture(h)){
            f.scan();f.owner.setItemInHand(InteractionHand.MAIN_HAND,f.paper);BlockPos ground=f.chest.offset(6,-2,2);
            var click=new PlayerInteractEvent.RightClickBlock(f.owner,InteractionHand.MAIN_HAND,ground,new BlockHitResult(Vec3.atCenterOf(ground),Direction.UP,ground,false));
            GuardianChestAutomation.usePaper(click);
            h.assertTrue(f.data.guardianSetAnchors.get("night").getInt("y")==ground.getY()+1,"ground click stores the post above the clicked face");
            f.owner.setShiftKeyDown(true);GuardianChestAutomation.useInAir(new PlayerInteractEvent.RightClickItem(f.owner,InteractionHand.MAIN_HAND));f.tick();
            h.assertTrue(f.data.hasHome&&f.wolf.blockPosition().equals(ground.above()),"sneak use in air deploys to the saved post");
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianChestFeedsRemoteAndUnloadedGuardiansWithoutFoodDuplication(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            f.scan();f.data.hasHome=true;f.data.movementOrder=3;f.wolf.setPos(f.chest.getX()+100,f.chest.getY(),f.chest.getZ());
            f.container.setItem(2,new ItemStack(Items.COOKED_BEEF,8));
            var feed=TameCommands.class.getDeclaredMethod("feedLoadedChestDrums",net.minecraft.server.MinecraftServer.class);feed.setAccessible(true);feed.invoke(null,h.getLevel().getServer());
            int stored=f.data.hungerInventory.stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(stored>0&&stored+f.container.getItem(2).getCount()==8,"out-of-range guardian receives conserved food");
            f.data.hungerInventory.clear();f.wolf.discard();f.container.setItem(2,new ItemStack(Items.COOKED_BEEF,8));feed.invoke(null,h.getLevel().getServer());
            stored=f.data.hungerInventory.stream().mapToInt(ItemStack::getCount).sum();
            h.assertTrue(stored>0&&stored+f.container.getItem(2).getCount()==8,"unloaded guardian receives stored food without rebuilding its body");
            f.data.hungerInventory.clear();f.container.setItem(1,ItemStack.EMPTY);f.container.setItem(2,new ItemStack(Items.COOKED_BEEF,8));feed.invoke(null,h.getLevel().getServer());
            h.assertTrue(f.data.hungerInventory.isEmpty()&&f.container.getItem(2).getCount()==8,"removed papers stop remote feeding");
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianRecallPullsRankedAndSurvivesLaterRoundCleanup(GameTestHelper h){
        try(var f=new Fixture(h)){
            var enemy=EntityType.WOLF.create(h.getLevel());UUID other=UUID.randomUUID();TameEntityAdapter.setOwner(enemy,other);enemy.setNoAi(true);enemy.setPos(f.home.getX(),f.home.getY(),f.home.getZ());h.getLevel().addFreshEntity(enemy);
            var enemyData=new TameData(enemy);TameRegistry.register(enemyData);LoadedTameIndex.refresh(enemy);
            try{
                TameRegistry.setRankedParticipants(Set.of(f.wolf.getUUID()));
                TameDuelManager.startTeamDuel(h.getLevel().getServer(),f.owner.getUUID(),Set.of(f.wolf.getUUID()),other,Set.of(enemy.getUUID()),Set.of(),false,true);
                h.assertTrue(TameDuelManager.rankedBattleId(f.wolf.getUUID())!=null,"fixture is an active ranked round");
                f.scan();f.power(true);f.tick();
                var current=TameRegistry.getByTlId(f.data.tlId);var body=TameEntityAdapter.findLoaded(h.getLevel().getServer(),current.uuid,current.tlId);
                h.assertTrue(!TameDuelManager.isEntityInDuel(current.uuid)&&!TameRegistry.getRankedParticipants().contains(current.uuid)&&current.hasHome&&body.blockPosition().equals(f.post),"recall removes ranked membership before deploying guardian");
                TameDuelManager.tick(h.getLevel().getServer());
                h.assertTrue(body.blockPosition().equals(f.post)&&current.hasHome,"later duel maintenance cannot undo the deployment");
            }finally{TameDuelManager.endDuelForEntity(h.getLevel().getServer(),enemy.getUUID());TameRegistry.remove(enemyData.uuid);enemy.discard();}
        }h.succeed();
    }
    @GameTest(template="empty") public static void guardianRecallReconstructsOnlyHiddenRankedBenchTame(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            CompoundTag snapshot=new CompoundTag();f.wolf.save(snapshot);f.data.entitySnapshot=snapshot;f.wolf.discard();
            TameRegistry.setRankedParticipants(Set.of(f.data.uuid));
            var field=TameCommands.class.getDeclaredField("RANKED_POOL_LOADED_FROM_REGISTRY");field.setAccessible(true);field.setBoolean(null,false);
            f.scan();f.power(true);f.tick();
            var current=TameRegistry.getByTlId(f.data.tlId);var body=TameEntityAdapter.findLoaded(h.getLevel().getServer(),current.uuid,current.tlId);
            h.assertTrue(body!=null&&current.hasHome&&body.blockPosition().equals(f.post),"hidden ranked bench tame is recovered at its guardian post");
            h.assertTrue(!TameRegistry.getRankedParticipants().contains(current.uuid),"hidden ranked tame is removed from matchmaking");
        }h.succeed();
    }
    @GameTest(template="empty",batch="guardianPending",timeoutTicks=100) public static void removedPaperCancelsUnloadedRecallWithoutChangingGuardianState(GameTestHelper h)throws Exception{
        var f=new Fixture(h);
        try{
            f.scan();f.wolf.discard();f.power(true);f.tick();
            h.assertTrue(!f.data.hasHome,"queued unloaded recall does not mutate guardian metadata before the entity arrives");
            f.container.setItem(1,ItemStack.EMPTY);
            h.runAfterDelay(30,()->{
                try{
                    h.assertTrue(!f.data.hasHome&&TameEntityAdapter.findLoaded(h.getLevel().getServer(),f.data.uuid,f.data.tlId)==null,"removed paper cannot leave a dormant deployment or copy an unloaded tame");
                    var field=TameCommands.class.getDeclaredField("PENDING_IMMEDIATE_CHUNK_TELEPORTS");field.setAccessible(true);
                    h.assertTrue(!((Map<?,?>)field.get(null)).containsKey(f.data.tlId),"canceled recall never creates a legacy wide-chunk teleport");
                    h.succeed();
                }catch(ReflectiveOperationException error){h.fail(error.toString());}finally{f.close();}
            });
        }catch(Throwable error){f.close();throw error;}
    }
}
