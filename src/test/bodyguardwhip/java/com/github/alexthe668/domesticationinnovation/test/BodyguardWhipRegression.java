package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class BodyguardWhipRegression {
    static final ResourceLocation WHIP=new ResourceLocation("domesticationinnovation","regression_whip");
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="domesticationinnovation",bus=net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static class Registry {
        @net.minecraftforge.eventbus.api.SubscribeEvent public static void register(RegisterEvent e){
            e.register(ForgeRegistries.Keys.ITEMS,h->h.register(WHIP,new Item(new Item.Properties())));
        }
    }
    static void check(boolean v,String message){if(!v)throw new AssertionError(message);}
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final ServerPlayer owner;final List<LivingEntity> bodies=new ArrayList<>();
        final Map<UUID,ServerPlayer> players;
        @SuppressWarnings("unchecked") Fixture(GameTestHelper helper)throws Exception{
            h=helper;owner=FakePlayerFactory.get(h.getLevel(),new GameProfile(UUID.randomUUID(),"WhipOwner"));
            var f=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");f.setAccessible(true);players=(Map<UUID,ServerPlayer>)f.get(h.getLevel().getServer().getPlayerList());players.put(owner.getUUID(),owner);
            var p=h.absolutePos(new net.minecraft.core.BlockPos(2,2,2));owner.setPos(p.getX(),p.getY(),p.getZ());
            owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ForgeRegistries.ITEMS.getValue(WHIP)));
        }
        Mob guard(EntityType<? extends Mob> type){
            Mob body=type.create(h.getLevel());TameEntityAdapter.setOwner(body,owner.getUUID());body.setNoAi(true);body.setNoGravity(true);body.setPos(owner.getX()+1,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(body);bodies.add(body);
            var data=new TameData(body,owner.getUUID(),!(body instanceof TamableAnimal));data.rosterBodyguard=true;data.mode=TameMode.PASSIVE.id();data.closeMovement=true;data.hungerSaturation=1000;TameRegistry.register(data);LoadedTameIndex.refresh(body);BodyguardService.roster(owner).bodyguards.add(data.ensureTlId());return body;
        }
        LivingEntity enemy(){var e=EntityType.HUSK.create(h.getLevel());e.setNoAi(true);e.setPos(owner.getX()+4,owner.getY(),owner.getZ());h.getLevel().addFreshEntity(e);bodies.add(e);return e;}
        void hit(LivingEntity e){check(e.hurt(h.getLevel().damageSources().playerAttack(owner),1),"actual whip-held hit succeeds");}
        public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());
            BodyguardWhipOrders.clear(owner,true);BodyguardService.roster(owner).bodyguards.clear();TameRegistry.removePlayerDuelStats(owner.getUUID());players.remove(owner.getUUID());TameGoalSupport.clearAssasinTarget(owner.getUUID());
            for(var body:bodies){TameRegistry.remove(body.getUUID());body.discard();}
        }
    }
    @GameTest(template="empty") public static void whipHitOrdersAllGuardsAndPersistsWithoutChangingMode(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var wolf=f.guard(EntityType.WOLF);var fox=f.guard(EntityType.FOX);var enemy=f.enemy();f.hit(enemy);
            for(var guard:List.of(wolf,fox)){
                var data=TameRegistry.get(guard.getUUID());
                check(guard.getTarget()==enemy&&enemy.getUUID().equals(data.bodyguardWhipTarget),"hit assigns vanilla and interface guards immediately");
                check(data.mode==TameMode.PASSIVE.id()&&data.closeMovement&&TameMode.forData(data)==TameMode.ASSASIN,"order temporarily overrides mode without editing settings");
                check(TameData.fromTag(data.toTag()).bodyguardWhipTarget.equals(enemy.getUUID()),"order survives NBT save/load");
                guard.setTarget(null);BodyguardWhipOrders.tick(new LivingEvent.LivingTickEvent(guard));check(guard.getTarget()==enemy,"order restores target after native AI clears it");
                guard.setPos(f.owner.getX()+30,f.owner.getY(),f.owner.getZ());
                com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands.maintainRosterBodyguard(f.owner,data);
                check(guard.distanceToSqr(f.owner)>400,"owner leash does not pull assassin away");
            }
            var event=new PlayerInteractEvent.RightClickItem(f.owner,InteractionHand.MAIN_HAND);BodyguardWhipOrders.click(event);
            check(event.isCanceled(),"clear right click consumes native whip use");
            for(var guard:List.of(wolf,fox))check(guard.getTarget()==null&&TameMode.forData(TameRegistry.get(guard.getUUID()))==TameMode.PASSIVE,"clear restores saved mode and removes target");
        }h.succeed();
    }
    @GameTest(template="empty") public static void targetDeathClearsOrdersAndSneakClearsEveryCombatTarget(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var wolf=f.guard(EntityType.WOLF);var fox=f.guard(EntityType.FOX);var enemy=f.enemy();f.hit(enemy);
            enemy.hurt(h.getLevel().damageSources().genericKill(),1000);
            check(TameRegistry.get(wolf.getUUID()).bodyguardWhipTarget==null&&wolf.getTarget()==null,"actual death clears orders and live target");
            var other=f.enemy();wolf.setTarget(other);fox.setTarget(other);f.owner.setShiftKeyDown(true);
            BodyguardWhipOrders.click(new PlayerInteractEvent.RightClickItem(f.owner,InteractionHand.MAIN_HAND));
            check(wolf.getTarget()==null&&fox.getTarget()==null,"sneak clear removes native targets even without a whip mark");
        }h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=180) public static void passiveGuardActuallyAttacksAndOrderSurvivesUnload(GameTestHelper h)throws Exception{
        var f=new Fixture(h);var wolf=f.guard(EntityType.WOLF);var enemy=f.enemy();var data=TameRegistry.get(wolf.getUUID());
        var saved=new net.minecraft.nbt.CompoundTag();enemy.saveWithoutId(saved);UUID targetId=enemy.getUUID();
        f.hit(enemy);enemy.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        check(BodyguardWhipOrders.target(h.getLevel(),data)==null&&targetId.equals(data.bodyguardWhipTarget),"unload does not silently cancel order");
        var loaded=EntityType.HUSK.create(h.getLevel());loaded.load(saved);loaded.setUUID(targetId);loaded.setNoAi(true);loaded.setPos(wolf.getX()+1,wolf.getY(),wolf.getZ());h.getLevel().addFreshEntity(loaded);f.bodies.add(loaded);
        wolf.setNoAi(false);float initial=loaded.getHealth();
        h.runAtTickTime(100,()->{
            try{
                check(loaded.getHealth()<initial,"passive-configured bodyguard must perform actual melee while ordered");
                check(data.mode==TameMode.PASSIVE.id(),"combat never edits saved passive mode");
                BodyguardWhipOrders.clear(f.owner,false);
                check(data.bodyguardWhipTarget==null&&TameMode.forData(data)==TameMode.PASSIVE,"clear returns passive mode");
            }finally{f.close();}
            h.succeed();
        });
    }

    @GameTest(template="empty") public static void clearingWorksWhenClickingAHungryGuard(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var wolf=f.guard(EntityType.WOLF);var enemy=f.enemy();f.hit(enemy);
            TameRegistry.get(wolf.getUUID()).hungerSaturation=0;f.owner.getFoodData().setSaturation(0);f.owner.setShiftKeyDown(true);
            var event=new PlayerInteractEvent.EntityInteract(f.owner,InteractionHand.MAIN_HAND,wolf);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
            check(event.isCanceled()&&TameRegistry.get(wolf.getUUID()).bodyguardWhipTarget==null&&wolf.getTarget()==null,"whip clearing must still work when hunger blocks native entity commands");
        }h.succeed();
    }

    @GameTest(template="empty") public static void unrelatedHitsAndProtectedTargetsDoNotCreateOrders(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h)){
            var wolf=f.guard(EntityType.WOLF);var data=TameRegistry.get(wolf.getUUID());var enemy=f.enemy();
            f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_SWORD));f.owner.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ForgeRegistries.ITEMS.getValue(WHIP)));f.hit(enemy);
            check(data.bodyguardWhipTarget==null,"sword hit with offhand whip does not create an order");
            check(BodyguardWhipOrders.assign(f.owner,wolf)==0,"cannot mark own pet");
            var stopped=new LivingDamageEvent(enemy,h.getLevel().damageSources().playerAttack(f.owner),1);stopped.setCanceled(true);f.owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ForgeRegistries.ITEMS.getValue(WHIP)));BodyguardWhipOrders.hit(stopped);
            check(data.bodyguardWhipTarget==null,"cancelled hit cannot mark target");
        }h.succeed();
    }
}
