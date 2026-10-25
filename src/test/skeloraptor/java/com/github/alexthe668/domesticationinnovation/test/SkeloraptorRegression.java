package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class SkeloraptorRegression {
    static class Fixture implements AutoCloseable {
        final GameTestHelper h;final FakePlayer owner;final TamableAnimal raptor,wolf;final UUID opponent=UUID.randomUUID();
        Fixture(GameTestHelper h,boolean ranked){
            this.h=h;owner=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"RaptorFixture"));
            owner.setPos(h.absolutePos(new BlockPos(1,2,1)).getX(),h.absolutePos(new BlockPos(1,2,1)).getY(),h.absolutePos(new BlockPos(1,2,1)).getZ());
            h.getLevel().players().add(owner);
            if(owner.connection==null) owner.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),owner);
            var type=ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("legendary_monsters:skeloraptor"));
            if(type==null)throw new AssertionError("real Skeloraptor type missing");
            raptor=(TamableAnimal)type.create(h.getLevel());wolf=EntityType.WOLF.create(h.getLevel());
            int x=2;for(var pet:List.of(raptor,wolf)){
                pet.setTame(true);pet.setOwnerUUID(owner.getUUID());pet.setNoAi(true);pet.setNoGravity(true);
                pet.moveTo(h.absolutePos(new BlockPos(x++,2,2)),0,0);
                h.getLevel().addFreshEntity(pet);var data=new TameData(pet);data.level=20;data.name="RaptorFixture"+x;TameRegistry.register(data);LoadedTameIndex.refresh(pet);
            }
            wolf.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);wolf.setHealth(1000);
            TameDuelManager.startTeamDuel(h.getLevel().getServer(),owner.getUUID(),Set.of(raptor.getUUID()),opponent,Set.of(wolf.getUUID()),Set.of(),false,ranked);
            h.assertTrue(TameDuelManager.areDuelOpponents(raptor.getUUID(),wolf.getUUID()),"real opposing participants registered");
        }
        void swing()throws Exception{
            var method=raptor.getClass().getDeclaredMethod("AreaAttack",float.class,float.class,float.class,float.class,int.class,boolean.class,float.class,boolean.class,float.class,boolean.class);
            method.setAccessible(true);method.invoke(raptor,6F,5F,360F,10F,0,false,0F,false,0F,false);
        }
        public void close(){
            TameDuelManager.endDuelsForOwner(h.getLevel().getServer(),owner.getUUID());
            for(var pet:List.of(raptor,wolf)){var data=TameRegistry.get(pet.getUUID());var body=data==null?null:TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);TameRegistry.remove(pet.getUUID());if(body!=null)body.discard();pet.discard();}
            h.getLevel().players().remove(owner);
        }
    }
    @GameTest(template="empty") public static void nativeSkeloraptorSwingDamagesDuelOpponent(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,false)){
            float before=f.wolf.getHealth();f.swing();
            h.assertTrue(f.wolf.getHealth()<before,"native swing must damage duel opponent: allied="+f.raptor.isAlliedTo(f.wolf)+", command="+f.raptor.getClass().getMethod("getCommand").invoke(f.raptor)+", health="+f.wolf.getHealth());
        }h.succeed();
    }
    @GameTest(template="empty") public static void nativeSkeloraptorSwingDamagesRankedOpponent(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,true)){float before=f.wolf.getHealth();f.swing();h.assertTrue(f.wolf.getHealth()<before,"ranked opponent receives native attack damage");}h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=160) public static void nativeAttackGoalsActuallyDamageOpponent(GameTestHelper h){
        var f=new Fixture(h,false);float before=f.wolf.getHealth();
        f.raptor.setNoAi(false);
        h.runAfterDelay(110,()->{
            try{h.assertTrue(f.wolf.getHealth()<before,"native attack AI must hit opponent on server ticks: health="+f.wolf.getHealth()+", target="+f.raptor.getTarget()+", sit="+f.raptor.isOrderedToSit());}
            finally{f.close();}h.succeed();
        });
    }
    @GameTest(template="empty") public static void nonparticipantsRemainProtected(GameTestHelper h)throws Exception{
        try(var f=new Fixture(h,false)){
            var friend=EntityType.WOLF.create(h.getLevel());friend.setTame(true);friend.setOwnerUUID(f.owner.getUUID());friend.setNoAi(true);friend.setPos(f.wolf.getX(),f.wolf.getY(),f.wolf.getZ());h.getLevel().addFreshEntity(friend);
            try{float before=friend.getHealth();f.swing();h.assertTrue(friend.getHealth()==before&&f.raptor.isAlliedTo(friend),"same-owner nonparticipant keeps native ally protection");}
            finally{TameRegistry.remove(friend.getUUID());friend.discard();}
        }h.succeed();
    }

}
