package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.UUID;

/** Bridge the mod's synced ownership and commands into DI without a required dependency. */
@Pseudo
@Mixin(targets = {
        "com.misanthropy.primitive_mobs.entity.monster.PrimitiveCreeper",
        "com.misanthropy.primitive_mobs.entity.monster.BabySpider",
        "com.misanthropy.primitive_mobs.entity.passive.Chameleon"
})
public abstract class PrimitivePetMixin extends Mob implements ModifedToBeTameable, IComandableMob {
    protected PrimitivePetMixin(EntityType<? extends Mob> type, Level level) { super(type, level); }
    @Shadow(remap = false) public abstract UUID getOwnerUuid();
    @Shadow(remap = false) public abstract void setOwnerUuid(UUID owner);

    public boolean isTame() { return getOwnerUuid() != null; }
    public void setTame(boolean value) { if (!value) setOwnerUuid(null); }
    public UUID getTameOwnerUUID() { return getOwnerUuid(); }
    public void setTameOwnerUUID(UUID owner) { setOwnerUuid(owner); }
    public LivingEntity getTameOwner() { return getOwnerUuid() == null ? null : level().getPlayerByUUID(getOwnerUuid()); }
    public int getCommand() { return PrimitiveMobsCompat.getCommand(this); }
    public void setCommand(int command) { PrimitiveMobsCompat.setCommand(this, command); }
    public boolean isStayingStill() { return isTame() && getCommand() == 1; }
    public boolean isFollowingOwner() { return isTame() && getCommand() == 2; }
    public boolean isValidAttackTarget(LivingEntity target) {
        return target != null && target != this && !target.getUUID().equals(getOwnerUuid())
                && !TameableUtils.shouldBlockOffensiveDiTarget(this, target);
    }

    @Inject(method = "m_6071_", at = @At("HEAD"), cancellable = true, remap = false)
    private void tl$wildFood(net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
                             CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        if (isTame()) return;
        if (!isAlive() || getHealth() >= 10.0F || !player.getItemInHand(hand).is(
                com.github.alexthe668.domesticationinnovation.server.item.DIItemRegistry.SINISTER_CARROT.get())) {
            cir.setReturnValue(net.minecraft.world.InteractionResult.PASS);
            return;
        }
        if (!level().isClientSide) {
            PrimitiveMobsCompat.tame(this, player);
            if (!player.getAbilities().instabuild) player.getItemInHand(hand).shrink(1);
        }
        cir.setReturnValue(net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide));
    }
}
