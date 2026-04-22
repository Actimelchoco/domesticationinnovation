package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.ai.goal.ITamableMonster.IAnimatedTamableMob")
public abstract class LegendaryMonstersAnimatedTamableMobMixin extends TamableAnimal {

    protected LegendaryMonstersAnimatedTamableMobMixin(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /**
     * @author Codex
     * @reason Keep same-owner duel opponents as valid targets for Legendary Monsters pets.
     */
    @Overwrite(remap = false)
    public void stopAttackingAllies() {
        LivingEntity target = this.getTarget();
        if (!(target instanceof TamableAnimal tameTarget)) {
            return;
        }
        LivingEntity owner = this.getOwner();
        if (owner == null) {
            return;
        }
        if (tameTarget.getOwner() != owner) {
            return;
        }
        if (TameDuelManager.areDuelOpponents(this.getUUID(), tameTarget.getUUID())) {
            return;
        }
        this.setTarget(null);
    }

    @Inject(method = "m_7307_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tl$allowDuelOpponentAsEnemy(Entity other, CallbackInfoReturnable<Boolean> cir) {
        if (other != null && TameDuelManager.areDuelOpponents(this.getUUID(), other.getUUID())) {
            // Entity#isAlliedTo -> false means "not allied", so combat goals can proceed.
            cir.setReturnValue(false);
        }
    }
}
