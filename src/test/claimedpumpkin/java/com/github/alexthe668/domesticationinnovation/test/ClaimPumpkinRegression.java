package com.github.alexthe668.domesticationinnovation.test;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ClaimPumpkinRegression {
    @GameTest(template="empty")
    public static void allPumpkinFormsProtectClaimedWater(GameTestHelper h) throws Exception {
        var level=h.getLevel();
        var source=level.getServer().createCommandSourceStack().withPermission(4);
        var team=FTBTeamsAPI.api().getManager().createServerTeam(source,"Pumpkin regression","",Color4I.WHITE);
        var data=FTBChunksAPI.api().getManager().getOrCreateData(team);
        var origin=h.absolutePos(new BlockPos(128,2,128));
        var claimed=new BlockPos((origin.getX()>>4)*16,origin.getY(),(origin.getZ()>>4)*16+8);
        var outside=claimed.west();
        var chunk=new ChunkDimPos(level,claimed);
        h.assertTrue(data.claim(source,chunk,false).isSuccess(),"claim created");
        try {
            for(String name:new String[]{"LordPumpkinheadHeadPriObnovlieniiTikaSushchnostiProcedure",
                    "LordPumpkinheadPriObnovlieniiTikaSushchnostiProcedure", "LordPumpkinheadTicProcedure",
                    "FelsteedParProcedure", "WithoutHorsePProcedure"}) {
                var method=Class.forName("net.mcreator.borninchaosv.procedures."+name)
                        .getMethod("execute",LevelAccessor.class,double.class,double.class,double.class,Entity.class);
                for(var pos:new BlockPos[]{claimed,outside}) {
                    level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());
                    var actor=EntityType.IRON_GOLEM.create(level);
                    actor.setNoAi(true);actor.setPos(outside.getX()+.5,pos.getY()+1,outside.getZ()+.5);
                    try {
                        method.invoke(null,level,pos.getX()+.5,pos.getY()+1.0,pos.getZ()+.5,actor);
                        h.assertTrue(level.getBlockState(pos).is(Blocks.WATER)==pos.equals(claimed),
                                name+": claimed water protected, unclaimed water converted");
                    } finally {actor.discard();}
                }
            }
            h.assertTrue(level.setBlockAndUpdate(claimed,Blocks.STONE.defaultBlockState()),"ordinary building allowed");
        } finally {data.unclaim(source,chunk,false);}
        h.succeed();
    }
}
