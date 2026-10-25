package com.github.alexthe668.domesticationinnovation.test;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.TameCommands;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.*;
import java.util.*;
import java.util.function.Consumer;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class RespawnAnimationRegression {
    static void verify(GameTestHelper h,EntityType<? extends LivingEntity> type){
        var original=type.create(h.getLevel());UUID owner=UUID.randomUUID();TameEntityAdapter.setOwner(original,owner);
        original.moveTo(h.absolutePos(new BlockPos(2,2,2)),0,0);
        var data=new TameData(original,owner,!(original instanceof TamableAnimal));
        CompoundTag snapshot=new CompoundTag();original.save(snapshot);
        snapshot.putShort("HurtTime",(short)10);snapshot.putShort("DeathTime",(short)12);snapshot.putFloat("Health",0);
        data.entitySnapshot=snapshot;data.dead=true;TameRegistry.register(data);
        float[] observed={-1,-1,-1};
        Consumer<EntityJoinLevelEvent> listener=event->{if(event.getEntity() instanceof LivingEntity e&&e.getUUID().equals(data.uuid)){
            observed[0]=e.getHealth();observed[1]=e.hurtTime;observed[2]=e.deathTime;
        }};
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST,listener);
        try{
            h.assertTrue(TameCommands.respawnDeadTameForDuel(h.getLevel().getServer(),data.uuid),"actual shared respawn path succeeds");
            h.assertTrue(observed[0]>0&&observed[1]==0&&observed[2]==0,
                "first world join must already be alive and animation-clean: health="+observed[0]+", hurtTime="+observed[1]+", deathTime="+observed[2]);
            var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);
            h.assertTrue(live!=null&&live.hurtTime==0&&live.hurtDuration==0&&live.deathTime==0,"final respawn state is clean");
            h.assertTrue(snapshot.getShort("HurtTime")==10&&snapshot.getShort("DeathTime")==12,"saved archive is not mutated");
        }finally{MinecraftForge.EVENT_BUS.unregister(listener);var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),data.uuid,data.tlId);TameRegistry.remove(data.uuid);if(live!=null)live.discard();}
    }
    @GameTest(template="empty") public static void nativeTameStartsCleanBeforeWorldJoin(GameTestHelper h){verify(h,EntityType.WOLF);h.succeed();}
    @GameTest(template="empty") public static void interfaceTameStartsCleanBeforeWorldJoin(GameTestHelper h){verify(h,EntityType.FOX);h.succeed();}
}
