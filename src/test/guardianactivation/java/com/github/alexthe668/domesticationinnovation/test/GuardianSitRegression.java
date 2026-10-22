package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraftforge.gametest.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class GuardianSitRegression {
    @GameTest(template="empty") public static void offlineAwakeGuardianDoesNotRestartNativeSitGoal(GameTestHelper h) {
        try(var f=new GuardianChestRegression.Fixture(h)) {
            f.power(true);f.tick();
            f.data.guardianResting=true;f.data.hungerSaturation=100;
            f.wolf.setOrderedToSit(true);f.wolf.setInSittingPose(true);
            // The owner is deliberately absent; exercise the actual wake path.
            // Feed a nearby visitor through the real proximity service instead of bypassing rest.
            try {
                var visitor=SharedGuardianRegression.observer(f,java.util.UUID.randomUUID(),"WakeVisitor",new java.util.ArrayList<>());
                visitor.setPos(f.post.getX()+.5,f.post.getY(),f.post.getZ()+.5);
                var players=GuardianActivationRegression.online(h);players.add(visitor);GuardianActivityService.rebuild(h.getLevel().getServer());
                try {TameCommands.processGuardianRest(f.wolf,f.data);} finally {players.remove(visitor);}
            } catch(Exception e){throw new RuntimeException(e);}
            h.assertTrue(!f.data.guardianResting&&!f.wolf.isOrderedToSit()&&!f.wolf.isInSittingPose(),"wake clears ordered sit and pose");
            f.wolf.setOnGround(true);
            var goal=new SitWhenOrderedToGoal(f.wolf);
            h.assertTrue(f.wolf.getOwner()==null,"test owner remains offline");
            h.assertTrue(!goal.canUse(),"awake guardian must not auto-sit because its owner is offline");
            f.wolf.setInSittingPose(true);TameCommands.enforceGuardianCommandLock(f.wolf,f.data);
            h.assertTrue(!f.wolf.isInSittingPose(),"stale sitting pose repaired even when ordered-sit flag was already clear");
            f.data.guardianResting=true;f.wolf.setOrderedToSit(true);
            h.assertTrue(goal.canUse()&&goal.canContinueToUse(),"resting guardian may still sit");
            f.data.guardianResting=false;f.data.hungerForcedSit=true;
            h.assertTrue(goal.canUse(),"hunger-forced sitting remains allowed");
            f.data.hungerForcedSit=false;f.data.hasHome=false;
            h.assertTrue(goal.canUse(),"ordinary tames retain native sitting behavior");
        }h.succeed();
    }
}
