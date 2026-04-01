package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.ArmageddonCompatUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.mcreator.armageddonmod.entity.TheFamineEntity")
public abstract class ArmageddonTheFamineEntityMixin extends Mob {

    protected ArmageddonTheFamineEntityMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "m_8099_", at = @At("TAIL"), remap = false, require = 0)
    private void tl$addTameTargetGoal(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(self, TamableAnimal.class, 0, false, false,
                tame -> ArmageddonCompatUtil.isValidBossTameTarget(self, tame)));
    }
}
