package com.github.alexthe668.domesticationinnovation.test;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class ClaimIceRegression {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @GameTest(template="empty")
    public static void amarokFreezeAndProjectileRespectDestinationClaim(GameTestHelper helper) throws Exception {
        var level=helper.getLevel();
        var source=level.getServer().createCommandSourceStack().withPermission(4);
        var team=FTBTeamsAPI.api().getManager().createServerTeam(source,"Ice regression","",Color4I.WHITE);
        var data=FTBChunksAPI.api().getManager().getOrCreateData(team);
        BlockPos origin=helper.absolutePos(new BlockPos(128,2,128));
        BlockPos claimed=new BlockPos((origin.getX()>>4)*16,origin.getY(),(origin.getZ()>>4)*16+8);
        BlockPos outside=claimed.west();
        var chunk=new ChunkDimPos(level,claimed);
        check(data.claim(source,chunk,false).isSuccess(),"test claim must succeed");
        var wolf=BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("mythsandlegends","amarok")).create(level);
        check(wolf!=null && wolf.getType()!=net.minecraft.world.entity.EntityType.PIG,"Amarok must be installed");
        try {
            level.setBlockAndUpdate(claimed,Blocks.WATER.defaultBlockState());
            level.setBlockAndUpdate(outside,Blocks.WATER.defaultBlockState());
            wolf.setPos(outside.getX()+.5,outside.getY()+1,outside.getZ()+.5);
            var goalClass=Class.forName("com.ovinter.mythsandlegends.entity.ai.goal.amarok.BackStepAttackGoal");
            var goal=goalClass.getConstructor(wolf.getClass()).newInstance(wolf);
            var freeze=goalClass.getDeclaredMethod("tryToFreezeWaterUnderneath");freeze.setAccessible(true);freeze.invoke(goal);
            check(level.getBlockState(claimed).is(Blocks.WATER),"water in adjacent claim must not freeze");
            check(level.getBlockState(outside).is(Blocks.FROSTED_ICE),"water outside claim must freeze normally");
            for(BlockPos pos:new BlockPos[]{claimed,outside}) {
                level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                var projectile=BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("mythsandlegends","frost_spike")).create(level);
                check(projectile!=null,"Frost Spike must be installed");
                projectile.setPos(outside.getX()+.5,outside.getY()+2,outside.getZ()+.5);
                var impact=projectile.getClass().getDeclaredMethod("onHitBlock",BlockHitResult.class);impact.setAccessible(true);
                impact.invoke(projectile,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
                check(level.getBlockState(pos).is(pos.equals(claimed)?Blocks.STONE:Blocks.PACKED_ICE),"projectile replacement must respect destination claim");
                check(projectile.isRemoved(),"blocked projectile must still finish its impact");
            }
            // Normal building and manual ice placement remain possible.
            check(level.setBlockAndUpdate(claimed,Blocks.ICE.defaultBlockState()),"ordinary block writes in claims must not be intercepted");
            check(level.getBlockState(claimed).is(Blocks.ICE),"manually placed ice is allowed");
        } finally {data.unclaim(source,chunk,false);wolf.discard();}
        helper.succeed();
    }
}
