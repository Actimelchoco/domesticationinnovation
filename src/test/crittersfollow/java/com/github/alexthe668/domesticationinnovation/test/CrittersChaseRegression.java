package com.github.alexthe668.domesticationinnovation.test;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.CrittersSlowChaseGoal;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder("domesticationinnovation")
@PrefixGameTestTemplate(false)
public class CrittersChaseRegression {
    static void check(boolean value, String message) { if (!value) throw new GameTestAssertException(message); }

    static CrittersSlowChaseGoal goal(TamableAnimal pet) {
        return pet.goalSelector.getAvailableGoals().stream().map(w -> w.getGoal())
                .filter(g -> g instanceof CrittersSlowChaseGoal).map(g -> (CrittersSlowChaseGoal) g).findFirst().orElseThrow();
    }

    static void chase(GameTestHelper h, String species, boolean ranked) throws Exception {
        var f = new CrittersFollowRegression.Fixture(h, species);
        LivingEntity enemy = ranked ? EntityType.WOLF.create(h.getLevel()) : EntityType.ZOMBIE.create(h.getLevel());
        try {
            f.owner.setPos(f.pet.getX() - 4, f.pet.getY(), f.pet.getZ());
            var base = f.pet.blockPosition();
            for (int x = -2; x <= 12; x++) for (int z = -3; z <= 3; z++) {
                var floor = base.offset(x, -1, z);
                h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
                for (int y = 1; y <= 3; y++) h.getLevel().setBlockAndUpdate(floor.above(y), Blocks.AIR.defaultBlockState());
            }
            UUID enemyOwner = UUID.randomUUID();
            if (enemy instanceof TamableAnimal animal) { animal.setTame(true); animal.setOwnerUUID(enemyOwner); }
            ((Mob) enemy).setNoAi(true); enemy.setPos(f.pet.getX() + 8, f.pet.getY(), f.pet.getZ());
            h.getLevel().addFreshEntity(enemy);
            if (ranked) {
                TameRegistry.register(new TameData(enemy, enemyOwner, false));
                f.owner.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(h.getLevel().getServer(),
                        new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), f.owner);
                TameDuelManager.startTeamDuel(h.getLevel().getServer(), f.owner.getUUID(), Set.of(f.pet.getUUID()),
                        enemyOwner, Set.of(enemy.getUUID()), Set.of(), false, true);
                check(TameDuelManager.areDuelOpponents(f.pet.getUUID(), enemy.getUUID()), "ranked opponents assigned");
                ((Mob) enemy).setNoAi(true);
            } else {
                f.data.hasHome = true; f.data.movementOrder = 3;
                f.data.homeDimension = h.getLevel().dimension().location().toString();
                f.data.homeX = base.getX(); f.data.homeY = base.getY(); f.data.homeZ = base.getZ();
            }
            f.pet.setNoAi(false); f.pet.setOrderedToSit(false); f.pet.setTarget(enemy);
            CrittersSlowChaseGoal.ensure(f.pet); CrittersSlowChaseGoal.ensure(f.pet);
            check(f.pet.goalSelector.getAvailableGoals().stream().filter(w -> w.getGoal() instanceof CrittersSlowChaseGoal).count() == 1,
                    "chase installed exactly once");
            var goal = goal(f.pet);
            check(goal.canUse(), "assigned combat target can be chased");
            double before = f.pet.distanceTo(enemy);
            var start = f.pet.position();
            h.runAfterDelay(70, () -> {
                try {
                    double remainingDistance = f.pet.distanceTo(enemy);
                    double movementSpeed = f.pet.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
                    double controlSpeed = f.pet.getMoveControl().getSpeedModifier();
                    int wakeTicksNow = wakeTicks(f.pet);
                    String runningGoals = f.pet.goalSelector.getAvailableGoals().stream().filter(w -> w.isRunning())
                            .map(w -> w.getGoal().getClass().getSimpleName()).collect(java.util.stream.Collectors.joining(","));
                    check(f.pet.isAlive() && f.pet.getTarget() == enemy, "target remains assigned");
                    check(remainingDistance < before - .25, species + " must walk closer to its " + (ranked ? "ranked" : "guardian") + " target; before=" + before + " after=" + remainingDistance
                            + " position=" + f.pet.position() + " motion=" + f.pet.getDeltaMovement()
                            + " wanted=" + f.pet.getMoveControl().getWantedX() + "," + f.pet.getMoveControl().getWantedY() + "," + f.pet.getMoveControl().getWantedZ()
                            + " collision=" + f.pet.horizontalCollision + " speed=" + movementSpeed + "/" + controlSpeed + " wake=" + wakeTicksNow + " goals=" + runningGoals);
                    check(f.pet.position().distanceToSqr(start) < 64, "slow walking does not teleport toward target or owner");
                    check(f.pet.getMoveControl().getSpeedModifier() <= CrittersSlowChaseGoal.SPEED + .001,
                            "chase uses the slow navigation speed");
                    f.pet.setTarget(null);
                    check(!goal.canUse(), "clearing target stops chase");
                    h.succeed();
                } finally { cleanup(f, enemy); }
            });
        } catch (RuntimeException | Error failure) { cleanup(f, enemy); throw failure; }
    }

    static void cleanup(CrittersFollowRegression.Fixture f, LivingEntity enemy) {
        TameDuelManager.endDuelsForOwner(f.h.getLevel().getServer(), f.owner.getUUID());
        TameRegistry.remove(enemy.getUUID());
        var current = f.h.getLevel().getEntity(enemy.getUUID()); if (current != null && current != enemy) current.discard();
        enemy.discard(); f.close();
    }

    static void restrictions(GameTestHelper h, String species) throws Exception {
        try (var f = new CrittersFollowRegression.Fixture(h, species)) {
            var enemy = EntityType.ZOMBIE.create(h.getLevel()); enemy.setNoAi(true);
            enemy.setPos(f.pet.getX() + 4, f.pet.getY(), f.pet.getZ());
            h.getLevel().addFreshEntity(enemy);
            try {
                f.pet.setNoAi(false); f.pet.setTarget(enemy); CrittersSlowChaseGoal.ensure(f.pet);
                var goal = goal(f.pet); check(goal.canUse(), "ordinary combat target allowed");
                settleSnail(f.pet);
                f.pet.setOrderedToSit(true); check(!goal.canUse(), "sitting respected"); f.pet.setOrderedToSit(false);
                f.data.guardianResting = true; check(!goal.canUse(), "guardian rest respected"); f.data.guardianResting = false;
                f.data.hungerSaturation = 0; check(!goal.canUse(), "empty food respected"); f.data.hungerSaturation = 1000;
                CrittersFollowRegression.rankedPool().add(f.pet.getUUID()); check(!goal.canUse(), "ranked waiting does not chase"); CrittersFollowRegression.rankedPool().remove(f.pet.getUUID());
                f.pet.setTarget(f.owner); check(!goal.canUse(), "owner cannot be chased as enemy");
                f.pet.setTarget(enemy); enemy.setHealth(0); check(!goal.canUse(), "dead targets are ignored");
            } finally { enemy.discard(); }
        }
        h.succeed();
    }

    @GameTest(template="held_item_arena", timeoutTicks=100) public static void rolypolyGuardianChases(GameTestHelper h) throws Exception { chase(h, "roly_poly", false); }
    @GameTest(template="held_item_arena", timeoutTicks=100) public static void snailGuardianChases(GameTestHelper h) throws Exception { chase(h, "snail", false); }
    @GameTest(template="held_item_arena", timeoutTicks=100) public static void rolypolyRankedChases(GameTestHelper h) throws Exception { chase(h, "roly_poly", true); }
    @GameTest(template="held_item_arena", timeoutTicks=100) public static void snailRankedChases(GameTestHelper h) throws Exception { chase(h, "snail", true); }
    @GameTest(template="empty") public static void rolypolyRespectsChaseRestrictions(GameTestHelper h) throws Exception { restrictions(h, "roly_poly"); }
    @GameTest(template="empty") public static void snailRespectsChaseRestrictions(GameTestHelper h) throws Exception { restrictions(h, "snail"); }
    static void settleSnail(TamableAnimal pet) throws Exception {
        if (pet.getType() == net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(
                new net.minecraft.resources.ResourceLocation("crittersandcompanions", "snail")))
            pet.getClass().getMethod("setWakingUpTicks", int.class).invoke(pet, -1);
    }
    static int wakeTicks(TamableAnimal pet) {
        try { return (int) pet.getClass().getMethod("getWakingUpTicks").invoke(pet); }
        catch (ReflectiveOperationException ignored) { return -1; }
    }
    @GameTest(template="empty") public static void rankedRefreshDoesNotRestartSnailWakeAnimation(GameTestHelper h) throws Exception {
        try (var f = new CrittersFollowRegression.Fixture(h, "snail")) {
            settleSnail(f.pet);
            check(!f.pet.isOrderedToSit(), "snail already awake and following");
            var refresh = TameDuelManager.class.getDeclaredMethod("clearDuelRestingState", LivingEntity.class);
            refresh.setAccessible(true); refresh.invoke(null, f.pet);
            int wakeTicks = (int) f.pet.getClass().getMethod("getWakingUpTicks").invoke(f.pet);
            check(wakeTicks < 0, "ranked refresh must not restart wake animation for an already awake snail; ticks=" + wakeTicks);
        }
        h.succeed();
    }
}
