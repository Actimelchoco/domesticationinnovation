package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Set;

@Mixin(PathNavigation.class)
public abstract class LostHunterNavigationMixin {
    @Shadow @Final protected Mob mob;
    @Shadow protected Path path;
    @Shadow protected BlockPos targetPos;

    @Inject(method = "createPath(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
    private void tl$budgetLostHunterPath(Set<BlockPos> targets, int range, boolean offset, int reach, float followRange,
                                        CallbackInfoReturnable<Path> cir) {
        if (!TameLimitService.isReleased(mob)) return;
        if (!LostHunterActivity.active(mob)) { cir.setReturnValue(null);return; }
        // Reusing an existing route costs no pathfinding budget.
        if (path != null && !path.isDone() && targets.contains(targetPos)) return;
        // A moving target must not stop an otherwise usable route between refreshes.
        if (!LostHunterActivity.claimPath(mob)) cir.setReturnValue(path != null && !path.isDone() ? path : null);
    }
}
