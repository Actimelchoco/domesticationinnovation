package com.github.alexthe668.domesticationinnovation.test;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation") @PrefixGameTestTemplate(false)
public class ClaimTrollagerRegression {
    @GameTest(template="empty") public static void trollagerSmashProtectsClaimBoundaryAndStillDamagesEntities(GameTestHelper h)throws Exception {
        var level=h.getLevel();var source=level.getServer().createCommandSourceStack().withPermission(4);
        var team=FTBTeamsAPI.api().getManager().createServerTeam(source,"Trollager regression","",Color4I.WHITE);
        var claims=FTBChunksAPI.api().getManager().getOrCreateData(team);
        var origin=h.absolutePos(new BlockPos(128,4,128));
        var claimed=new BlockPos((origin.getX()>>4)*16,origin.getY(),(origin.getZ()>>4)*16+8);
        var outside=claimed.west();var chunk=new ChunkDimPos(level,claimed);
        h.assertTrue(claims.claim(source,chunk,false).isSuccess(),"claim created");
        var troll=BuiltInRegistries.ENTITY_TYPE.get(new ResourceLocation("primitive_mobs","trollager")).create(level);
        h.assertTrue(troll!=null&&troll.getType()!=EntityType.PIG,"real Primitive Mobs Trollager installed");
        var victim=EntityType.IRON_GOLEM.create(level);victim.setNoAi(true);victim.setPos(outside.getX()+.5,outside.getY()+1,outside.getZ()+.5);level.addFreshEntity(victim);
        try {
            // Check the handler itself with a cross-border block list, independently of FTB's explosion settings.
            var explosion=new Explosion(level,troll,outside.getX(),outside.getY(),outside.getZ(),3,false,Explosion.BlockInteraction.DESTROY);
            explosion.getToBlow().addAll(List.of(claimed,outside));
            var entities=new ArrayList<Entity>();entities.add(victim);
            var event=new ExplosionEvent.Detonate(level,explosion,entities);
            Class.forName("dev.ftb.mods.ftbchunks.claimfire.ClaimTrollagerProtection").getMethod("detonate",ExplosionEvent.Detonate.class).invoke(null,event);
            h.assertTrue(!event.getAffectedBlocks().contains(claimed)&&event.getAffectedBlocks().contains(outside),"only claimed destination blocks are filtered");
            h.assertTrue(event.getAffectedEntities().contains(victim),"combat entity list is retained");
            // Invoke the actual native smash, with explosion griefing enabled.
            var configClass=Class.forName("com.misanthropy.primitive_mobs.core.PrimitiveMobsConfig");
            Object config=configClass.getField("CONFIG").get(null);
            var destruction=(net.minecraftforge.common.ForgeConfigSpec.BooleanValue)configClass.getField("trollagerDestruction").get(config);
            boolean old=destruction.get();boolean grief=level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
            destruction.set(true);level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
            try {
                // Smash lands two blocks in front of the troll, at the unclaimed side of the boundary.
                troll.setPos(outside.getX()+.5,outside.getY()+1,outside.getZ()-1.5);troll.setYRot(0);((net.minecraft.world.entity.Mob)troll).setNoAi(true);
                level.addFreshEntity(troll);
                for(int y=0;y<=2;y++)for(int z=-2;z<=2;z++){
                    level.setBlockAndUpdate(claimed.offset(0,y,z),Blocks.GLASS.defaultBlockState());
                    level.setBlockAndUpdate(outside.offset(0,y,z),Blocks.GLASS.defaultBlockState());
                }
                float health=victim.getHealth();
                var smash=troll.getClass().getDeclaredMethod("smash",LivingEntity.class);smash.setAccessible(true);smash.invoke(troll,victim);
                boolean outsideBroken=false;
                for(int y=0;y<=2;y++)for(int z=-2;z<=2;z++){
                    h.assertTrue(level.getBlockState(claimed.offset(0,y,z)).is(Blocks.GLASS),"native smash preserves every tested claimed glass block");
                    outsideBroken|=!level.getBlockState(outside.offset(0,y,z)).is(Blocks.GLASS);
                }
                h.assertTrue(outsideBroken,"native smash still destroys unclaimed terrain");
                h.assertTrue(victim.getHealth()<health,"native smash still damages entities");
            } finally {destruction.set(old);level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());}
        } finally {claims.unclaim(source,chunk,false);troll.discard();victim.discard();}
        h.succeed();
    }
}
