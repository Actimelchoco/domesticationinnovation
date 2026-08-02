package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.ArmageddonCompatUtil;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = {
        "net.mcreator.armageddonmod.entity.TheGobelinLordEntity",
        "net.mcreator.armageddonmod.entity.TheIronColossusEntity",
        "net.mcreator.armageddonmod.entity.NyxarisTheVeilOfOblivionEntity",
        "net.mcreator.armageddonmod.entity.SanghorLordOfBloodEntity",
        "net.mcreator.armageddonmod.entity.SanghorLordOfBloodp2Entity",
        "net.mcreator.armageddonmod.entity.VaedrictheFallenWandererEntity",
        "net.mcreator.armageddonmod.entity.ZoranthNewbornOfTheZenithEntity",
        "net.mcreator.armageddonmod.entity.ZoranththeForgottenOneEntity",
        "net.mcreator.armageddonmod.entity.EldoraththeAncientBuilderEntity"
})
public abstract class ArmageddonAdditionalBossEntityMixin extends Mob {

    protected ArmageddonAdditionalBossEntityMixin(EntityType<? extends Mob> type, Level level) {
        super(type, level);
    }

    @Inject(method = "m_8099_", at = @At("TAIL"), remap = false, require = 0)
    private void tl$addTameTargetGoal(CallbackInfo ci) {
        ArmageddonCompatUtil.addBossTameTargetGoal((Mob) (Object) this);
    }
}
