package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.BewereagerCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.ninni.species.server.entity.mob.update_3.Bewereager")
public abstract class SpeciesBewereagerMixin extends Monster implements ModifedToBeTameable, IComandableMob {
    protected SpeciesBewereagerMixin(EntityType<? extends Monster> type, Level level) { super(type, level); }
    @Shadow(remap = false) public abstract void setOwnerUUID(UUID id);
    @Shadow(remap = false) public abstract UUID m_21805_();
    @Unique private boolean tl$goalsInstalled;

    public boolean isTame() { return getPersistentData().getBoolean(BewereagerCompat.PERMANENT); }
    public void setTame(boolean value) { getPersistentData().putBoolean(BewereagerCompat.PERMANENT, value); }
    public UUID getTameOwnerUUID() { return m_21805_(); }
    public void setTameOwnerUUID(UUID id) { setOwnerUUID(id); }
    public LivingEntity getTameOwner() { return getTameOwnerUUID() == null ? null : level().getPlayerByUUID(getTameOwnerUUID()); }
    public int getCommand() { return getPersistentData().getInt("DICommand"); }
    public void setCommand(int command) { getPersistentData().putInt("DICommand", command); }
    public boolean isStayingStill() { return isTame() && getCommand() == 1; }
    public boolean isFollowingOwner() { return isTame() && getCommand() == 2; }
    public boolean isValidAttackTarget(LivingEntity target) {
        return target != this && target != getTameOwner() && !TameableUtils.shouldBlockOffensiveDiTarget(this, target);
    }

    @Inject(method = "m_8119_", at = @At("HEAD"), remap = false, require = 0)
    private void tl$tick(CallbackInfo ci) {
        if (!isTame() || level().isClientSide) return;
        if (!tl$goalsInstalled) {
            targetSelector.removeAllGoals(goal -> true);
            goalSelector.addGoal(0, new BewereagerCompat.PetMovementGoal(this));
            tl$goalsInstalled = true;
        }
        if (isStayingStill() || getTarget() != null && !isValidAttackTarget(getTarget())) setTarget(null);
        LivingEntity owner = getTameOwner();
        if (!isStayingStill() && getTarget() == null && owner != null) {
            LivingEntity enemy = owner.getLastHurtByMob();
            if (enemy == null || !enemy.isAlive()) enemy = owner.getLastHurtMob();
            if (enemy != null && enemy.isAlive() && isValidAttackTarget(enemy)) setTarget(enemy);
        }
    }

    @Inject(method = {"transform", "split"}, at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tl$keepForm(CallbackInfo ci) {
        if (isTame() || BewereagerCompat.isTemporary(this)) ci.cancel();
    }
}
