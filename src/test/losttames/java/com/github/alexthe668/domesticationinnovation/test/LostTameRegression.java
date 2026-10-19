package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class LostTameRegression {
    @GameTest(template="empty")
    public static void imprintedOwnerReclaimsWithFoodAndListIsPrivate(GameTestHelper h) throws Exception {
        UUID imprint=UUID.randomUUID(),lastOwner=UUID.randomUUID(),stranger=UUID.randomUUID();
        var body=pet(h,EntityType.WOLF,imprint);var data=TameRegistry.get(body.getUUID());
        TameEntityAdapter.setOwner(body,lastOwner);TameRegistry.transferOwnership(data.uuid,lastOwner);
        TameLimitService.release(h.getLevel().getServer(),data);UUID id=archiveFor(h,data.uuid);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(imprint,"Reclaimer"));player.getAbilities().instabuild=false;
        try {
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.LostTameCommands.playerEntries(h.getLevel().getServer(),imprint).contains(id),"imprinted owner sees lost tame");
            check(com.github.alexthe668.domesticationinnovation.server.tameslevel.LostTameCommands.playerEntries(h.getLevel().getServer(),lastOwner).contains(id),"last owner sees lost tame");
            check(!com.github.alexthe668.domesticationinnovation.server.tameslevel.LostTameCommands.playerEntries(h.getLevel().getServer(),stranger).contains(id),"unrelated players cannot list another owner's lost tame");
            check(h.getLevel().getServer().getCommands().getDispatcher().execute("tames lost",player.createCommandSourceStack().withPermission(0))==1,"player command needs no admin permission");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STICK,2));
            check(TameLimitService.retameLost(player,body)!=null&&TameLimitService.isReleased(body),"wrong food cannot retame even for imprint");
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.BEEF,2));
            var event=new net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND,body);
            LostTameInteractions.interact(event);
            check(event.isCanceled()&&!TameLimitService.isReleased(body)&&imprint.equals(TameEntityAdapter.ownerUuid(body)),"imprinted owner immediately reclaims through interaction");
            check(player.getMainHandItem().getCount()==1,"exactly one accepted food consumed");
            check(TameRegistry.get(body.getUUID()).level==15&&TameRegistry.get(body.getUUID()).abilityLevels.get("arrow_shot")==3,"existing progression preserved");
            check(TameRegistry.get(body.getUUID()).limitOnlineTicks==0&&!TameLimitService.applyPending(body),"grace reset and tombstone cannot undo reclaim");
            check(LostTameRegistry.get(h.getLevel().getServer()).entry(id).contains("recoveredAt"),"admin archive closed after retame");
            check(!com.github.alexthe668.domesticationinnovation.server.tameslevel.LostTameCommands.playerEntries(h.getLevel().getServer(),imprint).contains(id),"reclaimed tame removed from current lost list");
        } finally {player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);clean(h,body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void publicRetamingUnlocksAfterExactlyThreeMinecraftDays(GameTestHelper h) {
        var body=pet(h,EntityType.WOLF,UUID.randomUUID());var data=TameRegistry.get(body.getUUID());
        var server=h.getLevel().getServer();long before=server.overworld().getGameTime();
        TameLimitService.release(server,data);
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"NewOwner"));player.getAbilities().instabuild=false;
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new ItemStack(Items.BEEF,2));
        try {
            server.getWorldData().overworldData().setGameTime(before+71999);
            check(TameLimitService.retameLost(player,body,net.minecraft.world.InteractionHand.OFF_HAND)!=null,"one tick early is blocked");
            check(player.getOffhandItem().getCount()==2&&TameLimitService.isReleased(body),"failed attempt consumes nothing");
            server.getWorldData().overworldData().setGameTime(before+72000);
            check(TameLimitService.retameLost(player,body,net.minecraft.world.InteractionHand.OFF_HAND)==null,"any player may retame at three days");
            check(player.getUUID().equals(TameEntityAdapter.ownerUuid(body))&&!player.getUUID().equals(TameRegistry.get(body.getUUID()).imprintedOwnerUUID()),"new ownership preserves first imprint");
            check(player.getOffhandItem().getCount()==1,"offhand food consumed once");
            TameLimitService.release(server,TameRegistry.get(body.getUUID()));
            check(TameLimitService.retameLost(player,body,net.minecraft.world.InteractionHand.OFF_HAND)!=null,"new loss starts a fresh public cooldown");
        } finally {server.getWorldData().overworldData().setGameTime(before);player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,ItemStack.EMPTY);clean(h,body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void interfaceTameCanBeReclaimedAndSnapshotRemainsOwned(GameTestHelper h) {
        UUID owner=UUID.randomUUID();var body=pet(h,EntityType.FOX,owner);
        TameLimitService.release(h.getLevel().getServer(),TameRegistry.get(body.getUUID()));
        var player=FakePlayerFactory.get(h.getLevel(),new GameProfile(owner,"FoxOwner"));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.SWEET_BERRIES,2));
        try {
            check(TameLimitService.retameLost(player,body)==null,"interface fox accepts its native food");
            var saved=new CompoundTag();body.save(saved);
            var reloaded=(LivingEntity)EntityType.loadEntityRecursive(saved,h.getLevel(),e->e);
            check(reloaded!=null&&!TameLimitService.applyPending(reloaded)&&owner.equals(TameEntityAdapter.ownerUuid(reloaded)),"reclaimed ownership survives snapshot reload");
        } finally {player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);clean(h,body);}h.succeed();
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static LivingEntity pet(GameTestHelper h,EntityType<? extends LivingEntity> type,UUID owner){
        var pet=type.create(h.getLevel());TameEntityAdapter.setOwner(pet,owner);
        pet.moveTo(h.absolutePos(new BlockPos(2,2,2)),0,0);h.getLevel().addFreshEntity(pet);
        var data=new TameData(pet,owner,!(pet instanceof TamableAnimal));data.name="LostFixture";
        data.level=15;data.abilities.add("arrow_shot");data.abilityLevels.put("arrow_shot",3);data.attributeLevels.put("armor",2);
        TameRegistry.register(data);LoadedTameIndex.refresh(pet);return pet;
    }
    private static UUID archiveFor(GameTestHelper h,UUID original){
        var archive=LostTameRegistry.get(h.getLevel().getServer());
        return archive.ids(true).stream().filter(id->original.equals(TameData.fromTag(archive.entry(id).getCompound("tame")).uuid)).findFirst().orElseThrow();
    }
    private static void clean(GameTestHelper h,LivingEntity... bodies){
        for(var body:bodies)if(body!=null){TameRegistry.remove(body.getUUID());body.discard();}
    }
    @GameTest(template="empty")
    public static void archivePreservesOwnerImprintProgressAndLatestEquipment(GameTestHelper h){
        UUID first=UUID.randomUUID(),owner=UUID.randomUUID();var body=pet(h,EntityType.WOLF,first);
        TameData data=TameRegistry.get(body.getUUID());TameEntityAdapter.setOwner(body,owner);TameRegistry.transferOwnership(data.uuid,owner);
        body.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.DIAMOND));
        TameLimitService.release(h.getLevel().getServer(),data);UUID id=archiveFor(h,data.uuid);
        try{
            var archive=LostTameRegistry.get(h.getLevel().getServer());var row=archive.entry(id);
            var saved=TameData.fromTag(row.getCompound("tame"));
            check(owner.equals(saved.ownerUUID)&&first.equals(saved.imprintedOwnerUUID()),"original owner and distinct imprint are saved before clearing ownership");
            check(saved.level==15&&saved.abilityLevels.get("arrow_shot")==3&&saved.attributeLevels.get("armor")==2,"all progression saved");
            check(saved.entitySnapshot.getList("HandItems",10).getCompound(0).getString("id").equals("minecraft:diamond"),"fresh physical equipment is archived, not stale registry snapshot");
            var reloaded=LostTameRegistry.load(archive.save(new CompoundTag()));
            check(reloaded.entry(id)!=null&&reloaded.entry(id).getString("reason").equals("tame_type_limit"),"archive survives SavedData serialization");
            TameLimitService.death(new LivingDeathEvent(body,h.getLevel().damageSources().generic()));
            check(archive.entry(id)!=null,"wild death cleanup cannot erase the recovery archive");
        }finally{clean(h,body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void adminRecoveryRetiresWildCopiesWithoutBlockingTransfers(GameTestHelper h)throws Exception{
        UUID owner=UUID.randomUUID();var body=pet(h,EntityType.WOLF,owner);TameData original=TameRegistry.get(body.getUUID());
        TameLimitService.release(h.getLevel().getServer(),original);UUID id=archiveFor(h,original.uuid);
        CompoundTag wildSnapshot=new CompoundTag();body.save(wildSnapshot);
        var admin=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"LostAdmin"));admin.setPos(body.getX(),body.getY(),body.getZ());
        var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var source=admin.createCommandSourceStack();
        LivingEntity recovered=null;
        try{
            boolean denied=false;try{dispatcher.execute("tames admin lost recover "+id,source.withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
            check(denied,"non-admin cannot recover lost tames");
            check(dispatcher.execute("tames admin lost list",source.withPermission(2))==1&&dispatcher.execute("tames admin lost info "+id,source.withPermission(2))==1,"admin can list and inspect archive");
            check(dispatcher.execute("tames admin lost recover "+id,source.withPermission(2))==1,"admin recovery command succeeds");
            var entry=LostTameRegistry.get(h.getLevel().getServer()).entry(id);recovered=(LivingEntity)h.getLevel().getEntity(entry.getUUID("recoveredBody"));
            check(recovered!=null&&body.isRemoved()&&owner.equals(TameEntityAdapter.ownerUuid(recovered)),"old body replaced by one owned recovered tame");
            var restored=TameRegistry.get(recovered.getUUID());
            check(restored.level==15&&restored.abilityLevels.get("arrow_shot")==3&&restored.limitOnlineTicks==0,"progress restored and grace reset");
            check(restored.movementOrder==1&&((TamableAnimal)recovered).isOrderedToSit(),"recovery sits the tame");
            check(dispatcher.execute("tames admin lost recover "+id,source.withPermission(2))==0,"same archive cannot be recovered twice");
            var stale=(LivingEntity)EntityType.loadEntityRecursive(wildSnapshot,h.getLevel(),entity->entity);
            check(!h.getLevel().addFreshEntity(stale)&&stale.isRemoved(),"old wild copy is rejected when its chunk loads later");
            UUID recipient=UUID.randomUUID();TameEntityAdapter.setOwner(recovered,recipient);TameRegistry.transferOwnership(recovered.getUUID(),recipient);
            check(!TameLimitService.applyPending(recovered)&&recipient.equals(TameEntityAdapter.ownerUuid(recovered)),"later legitimate ownership transfers are not overridden");
            CompoundTag futureSnapshot=new CompoundTag();recovered.save(futureSnapshot);
            var future=(LivingEntity)EntityType.loadEntityRecursive(futureSnapshot,h.getLevel(),entity->entity);future.setUUID(UUID.randomUUID());
            try{check(!TameLimitService.applyPending(future)&&!future.isRemoved(),"normal respawn with a new UUID is allowed through normal identity validation");}finally{future.discard();}
        }finally{clean(h,body,recovered);}h.succeed();
    }
    public static class RejectSpawn {
        @SubscribeEvent public void join(EntityJoinLevelEvent event){if(event.getEntity().getPersistentData().getBoolean("LostTestReject"))event.setCanceled(true);}
    }
    @GameTest(template="empty")
    public static void spawnFailurePreservesArchiveAndWildBody(GameTestHelper h){
        var body=pet(h,EntityType.WOLF,UUID.randomUUID());body.getPersistentData().putBoolean("LostTestReject",true);
        var data=TameRegistry.get(body.getUUID());TameLimitService.release(h.getLevel().getServer(),data);UUID id=archiveFor(h,data.uuid);
        var listener=new RejectSpawn();MinecraftForge.EVENT_BUS.register(listener);
        try{
            var result=TameLimitService.recoverLost(h.getLevel().getServer(),id,h.getLevel(),body.position());
            check(result.entity()==null&&!body.isRemoved()&&TameLimitService.isReleased(body),"rejected recovery leaves the original wild body intact");
            check(!LostTameRegistry.get(h.getLevel().getServer()).entry(id).contains("recoveredAt"),"failed recovery stays recoverable");
            check(TameRegistry.getByTlId(data.tlId)==null,"failed recovery leaves no active registry duplicate");
        }finally{MinecraftForge.EVENT_BUS.unregister(listener);clean(h,body);}h.succeed();
    }
    @GameTest(template="empty")
    public static void storedInterfaceSnapshotRecoversAfterOriginalIsGone(GameTestHelper h){
        UUID owner=UUID.randomUUID();var body=pet(h,EntityType.FOX,owner);var data=TameRegistry.get(body.getUUID());
        data.entitySnapshot=new CompoundTag();body.save(data.entitySnapshot);data.stored=true;body.discard();
        TameLimitService.release(h.getLevel().getServer(),data);UUID id=archiveFor(h,data.uuid);
        var result=TameLimitService.recoverLost(h.getLevel().getServer(),id,h.getLevel(),body.position());
        try{
            check(result.entity()!=null&&owner.equals(TameEntityAdapter.ownerUuid(result.entity())),"interface tame reconstructed from stored archive");
            TameLimitService.advanceOnlinePlayers(h.getLevel().getServer());
            check(h.getLevel().getEntity(data.uuid)==null,"pending stored release cannot also materialize the old body");
        }finally{clean(h,body,result.entity());}h.succeed();
    }
}
