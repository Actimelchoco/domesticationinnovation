package test;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.TameActivityProgressEvents;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import java.util.UUID;

final class ActivityProgressTests {
    private static int checks;
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    private static class FollowingWolf extends Wolf {
        LivingEntity owner;
        FollowingWolf(ServerLevel level, LivingEntity owner) {
            super(EntityType.WOLF, level);
            this.owner = owner;
            setTame(true); setOwnerUUID(owner.getUUID());
            ((IComandableMob) this).setCommand(2);
            moveTo(0, -59, 0);
        }
        @Override public LivingEntity getOwner() { return owner; }
    }
    private static void ticks(LivingEntity pet, int count, boolean move) {
        for (int i = 0; i < count; i++) {
            if (move) {
                LivingEntity owner = TameEntityAdapter.owner(pet);
                owner.setPos(owner.getX() + 0.01, owner.getY(), owner.getZ());
            }
            TameActivityProgressEvents.tick(new LivingEvent.LivingTickEvent(pet));
        }
    }
    static int run(ServerLevel level) throws Exception {
        var owner = FakePlayerFactory.get(level, new GameProfile(UUID.randomUUID(), "ActivityOwner"));
        var pet = new FollowingWolf(level, owner);
        level.addFreshEntity(pet);
        TameData data = TameRegistry.get(pet.getUUID());
        data.movementOrder = 0; data.hungerSaturation = 1000;
        ticks(pet, 11999, true);
        check(data.xp == 0, "following awards nothing before ten minutes");
        ticks(pet, 1, true);
        check(data.xp == 1, "continuous moving-owner follow grants exactly one XP");
        ticks(pet, 10000, true);
        ticks(pet, 1200, false);
        ticks(pet, 1000, true);
        check(data.xp == 1, "one minute owner idle resets the whole follow interval");
        ticks(pet, 10000, true);
        data.hungerSaturation = 0; ticks(pet, 1, true); data.hungerSaturation = 1000;
        ticks(pet, 1000, true);
        check(data.xp == 1, "starvation resets following progress");
        ticks(pet, 10000, true);
        TameActivityProgressEvents.leave(new EntityLeaveLevelEvent(pet, level));
        ticks(pet, 1000, true);
        check(data.xp == 1, "unloading resets following progress");
        data.movementOrder = 3; data.hasHome = true;
        data.homeDimension = level.dimension().location().toString(); data.homeX = 0; data.homeY = -59; data.homeZ = 0;
        double before = data.bonusHealth;
        ticks(pet, 23999, false);
        check(data.bonusHealth == before, "guard reward waits a whole Minecraft day");
        ticks(pet, 1, false);
        check(data.bonusHealth == before + 1, "guard earns permanent one HP bonus");
        ticks(pet, 1, false);
        check(data.bonusHealth == before + 1, "guard reward does not repeat next tick");
        ticks(pet, 23998, false);
        data.homeX = 1;
        ticks(pet, 1, false);
        check(data.bonusHealth == before + 1, "changing guard location starts a fresh day");
        data.hasHome = false; data.movementOrder = 0;
        var attacker = EntityType.ZOMBIE.create(level);
        var damage = new LivingDamageEvent(pet, pet.damageSources().mobAttack(attacker), 1);
        TameActivityProgressEvents.damaged(damage);
        TameActivityProgressEvents.damaged(damage);
        TameActivityProgressEvents.died(new LivingDeathEvent(attacker, attacker.damageSources().generic()));
        check(data.xp == 2, "receiving repeated damage earns only one participation XP per attacker death");
        TameActivityProgressEvents.died(new LivingDeathEvent(attacker, attacker.damageSources().generic()));
        check(data.xp == 2, "repeated death event cannot duplicate participation reward");
        var clock = TameActivityProgressEvents.class.getDeclaredField("clock"); clock.setAccessible(true);
        TameActivityProgressEvents.damaged(damage);
        clock.setLong(null, clock.getLong(null) + TameActivityProgressEvents.PARTICIPATION_TICKS);
        TameActivityProgressEvents.died(new LivingDeathEvent(attacker, attacker.damageSources().generic()));
        check(data.xp == 2, "participation expires after five minutes");
        TameActivityProgressEvents.damaged(new LivingDamageEvent(pet, pet.damageSources().mobAttack(attacker), 0));
        TameActivityProgressEvents.died(new LivingDeathEvent(attacker, attacker.damageSources().generic()));
        check(data.xp == 2, "zero damage grants no participation");
        pet.discard();
        return checks;
    }
}
