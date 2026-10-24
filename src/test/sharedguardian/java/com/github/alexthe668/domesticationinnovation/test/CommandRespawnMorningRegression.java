package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class CommandRespawnMorningRegression {
    @GameTest(template="empty") public static void deathBlocksCommandsUntilMorningAndPersists(GameTestHelper h)throws Exception{
        long clock=h.getLevel().getDayTime();
        try(var f=new GuardianChestRegression.Fixture(h)){
            String name="Morning"+UUID.randomUUID().toString().substring(0,8);
            f.data.name=name;f.wolf.setCustomName(net.minecraft.network.chat.Component.literal(name));
            h.getLevel().setDayTime(1000);
            h.assertTrue(f.wolf.hurt(h.getLevel().damageSources().genericKill(),10000),"real death occurs");
            h.assertTrue(f.data.dead&&f.data.commandRespawnAfterDayTime==24000,"death captures the next Overworld morning");
            var saved=TameData.fromTag(f.data.toTag());
            h.assertTrue(saved.commandRespawnAfterDayTime==24000&&CommandRespawnDelay.failure(h.getLevel().getServer(),saved)!=null,"deadline survives save/reload");
            var older=saved.toTag();older.putLong("commandRespawnAfterDayTime",0);
            h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),TameData.fromTag(older))!=null,"an older archived snapshot cannot bypass the latest death deadline");
            older.putBoolean("dead",false);
            h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),TameData.fromTag(older))!=null,"a pre-death archive cannot override the current death's wait");
            var record=TameRegistry.LAST_DEATHS.get(f.data.uuid);
            h.assertTrue(record!=null&&TameData.fromTag(record.snapshot).commandRespawnAfterDayTime==24000,"admin death archive retains the same deadline");
            var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var source=f.owner.createCommandSourceStack().withPermission(4);
            int xp=f.owner.totalExperience;var inventory=f.owner.getInventory().save(new net.minecraft.nbt.ListTag()).copy();
            for(String command:List.of("tames respawn "+name,"tames respawn all","tames respawn type wolf","tames arise "+name,"tames respawnReincarnated "+name,"tames ariseReincarnated "+name)){
                h.assertTrue(dispatcher.execute(command,source)==0,"command cannot bypass next morning: "+command);
                h.assertTrue(f.data.dead&&TameEntityAdapter.findLoaded(h.getLevel().getServer(),f.data.uuid,f.data.tlId)==null,"blocked command creates no entity");
            }
            h.assertTrue(xp==f.owner.totalExperience&&inventory.equals(f.owner.getInventory().save(new net.minecraft.nbt.ListTag())),"rejected commands consume neither XP nor items");
            h.assertTrue(dispatcher.execute("tames admin respawn "+name,source)==1,"admin single respawn bypasses morning before it passes");
            var immediate=TameEntityAdapter.findLoaded(h.getLevel().getServer(),f.data.uuid,f.data.tlId);
            h.assertTrue(immediate!=null&&immediate.isAlive(),"admin immediately recreates live tame");
            immediate.hurt(h.getLevel().damageSources().genericKill(),10000);
            h.getLevel().setDayTime(23999);h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),saved)!=null,"last tick of the night remains locked");
            h.getLevel().setDayTime(24000);h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),saved)==null,"morning or a sleep skip unlocks immediately");
            h.assertTrue(dispatcher.execute("tames admin respawn "+name,source)==1,"actual command respawns after morning");
            var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),f.data.uuid,f.data.tlId);
            h.assertTrue(live!=null&&live.isAlive(),"one live tame is recreated after morning");
        }finally{h.getLevel().setDayTime(clock);}h.succeed();
    }
    @GameTest(template="empty") public static void mixedBatchSkipsRecentDeathAndNewDayDeathsWaitAgain(GameTestHelper h)throws Exception{
        long clock=h.getLevel().getDayTime();
        try(var f=new GuardianChestRegression.Fixture(h)){
            h.getLevel().setDayTime(23999);f.wolf.hurt(h.getLevel().damageSources().genericKill(),10000);
            h.getLevel().setDayTime(24000);
            var other=EntityType.WOLF.create(h.getLevel());TameEntityAdapter.setOwner(other,f.owner.getUUID());other.setPos(f.home.getX()+2,f.home.getY(),f.home.getZ());h.getLevel().addFreshEntity(other);
            var recent=new TameData(other,f.owner.getUUID(),false);recent.name="RecentWolf";TameRegistry.register(recent);
            try{
                other.hurt(h.getLevel().damageSources().genericKill(),10000);
                h.assertTrue(recent.commandRespawnAfterDayTime==48000,"dying at morning requires the following morning");
                var dispatcher=h.getLevel().getServer().getCommands().getDispatcher();var source=f.owner.createCommandSourceStack().withPermission(4);
                h.assertTrue(dispatcher.execute("tames admin player "+f.owner.getGameProfile().getName()+" respawn all",source)==1,"batch respawns an eligible member");
                h.assertTrue(!TameRegistry.get(f.data.uuid).dead&&!TameRegistry.get(recent.uuid).dead&&TameEntityAdapter.findLoaded(h.getLevel().getServer(),recent.uuid,recent.tlId)!=null,"admin batch includes today's death immediately");
                // Non-death administrative recovery and old saves have no fabricated deadline.
                var old=f.data.toTag();old.putBoolean("dead",true);old.remove("commandRespawnAfterDayTime");
                h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),TameData.fromTag(old))==null,"legacy deaths without an accurate day-time deadline stay eligible");
            }finally{TameRegistry.remove(recent.uuid);other.discard();var live=TameEntityAdapter.findLoaded(h.getLevel().getServer(),recent.uuid,recent.tlId);if(live!=null)live.discard();}
        }finally{h.getLevel().setDayTime(clock);}h.succeed();
    }
    @GameTest(template="empty") public static void deadlineUsesOverworldRatherThanLocalDimension(GameTestHelper h){
        long clock=h.getLevel().getServer().overworld().getDayTime();
        try(var f=new GuardianChestRegression.Fixture(h)){
            h.getLevel().getServer().overworld().setDayTime(47000);
            f.data.dead=true;CommandRespawnDelay.onDeath(h.getLevel().getServer(),f.data);
            h.assertTrue(f.data.commandRespawnAfterDayTime==48000,"all dimension deaths use the server Overworld calendar");
            h.getLevel().getServer().overworld().setDayTime(48000);
            h.assertTrue(CommandRespawnDelay.failure(h.getLevel().getServer(),f.data)==null,"no owner-online timer or ticking countdown is needed");
        }finally{h.getLevel().getServer().overworld().setDayTime(clock);}h.succeed();
    }
}
