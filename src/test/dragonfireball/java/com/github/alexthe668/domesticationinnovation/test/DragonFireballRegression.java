package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class DragonFireballRegression {
    @GameTest(template="empty", timeoutTicks=60)
    public static void flyingFireballExplodesAtBlockSurfaceWithoutBreakingIt(GameTestHelper h) {
        var level=h.getLevel();
        BlockPos wall=h.absolutePos(new BlockPos(4,2,3));
        level.setBlockAndUpdate(wall,Blocks.GLASS.defaultBlockState());
        var owner=EntityType.WOLF.create(level);
        owner.setNoAi(true);
        owner.setPos(wall.getX()-5,wall.getY(),wall.getZ()+0.5);
        level.addFreshEntity(owner);
        var fireball=new TimedTameDragonFireball(level,owner,1,0,0);
        fireball.setPos(wall.getX()-2.5,wall.getY()+0.5,wall.getZ()+0.5);
        fireball.setDeltaMovement(new Vec3(3,0,0));
        level.addFreshEntity(fireball);
        h.runAfterDelay(3,()->{
            var clouds=level.getEntitiesOfClass(TimedTameDragonBreathCloud.class,
                    new net.minecraft.world.phys.AABB(wall).inflate(5));
            if(!fireball.isRemoved()) throw new AssertionError("fireball survives a solid block impact");
            if(clouds.size()!=1) throw new AssertionError("impact must produce exactly one breath cloud");
            var cloud=clouds.get(0);
            if(Math.abs(cloud.getX()-(wall.getX()-0.05))>0.02)
                throw new AssertionError("explosion must be at wall surface, not previous projectile position: "+cloud.position());
            if(!level.getBlockState(wall).is(Blocks.GLASS)) throw new AssertionError("impact destroyed block");
            clouds.forEach(e->e.discard());owner.discard();h.succeed();
        });
    }
}
