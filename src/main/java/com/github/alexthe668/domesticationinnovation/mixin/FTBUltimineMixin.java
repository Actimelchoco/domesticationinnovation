package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.UltimineCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.FTBUltimine", remap = false)
public abstract class FTBUltimineMixin {
    @Inject(method = "canUltimine", at = @At("RETURN"), cancellable = true, require = 1)
    private void tl$requireCompanion(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && player instanceof ServerPlayer serverPlayer
                && !UltimineCompanion.hasCompanion(serverPlayer)) cir.setReturnValue(false);
    }

    @Group(name = "tl$ultimineBlock", min = 1, max = 1)
    @Redirect(method = "blockBroken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;destroyBlock(Lnet/minecraft/core/BlockPos;)Z",
            remap = false), remap = false, require = 0)
    private boolean tl$breakDev(ServerPlayerGameMode gameMode, BlockPos pos,
                               net.minecraft.world.level.Level level, BlockPos origin,
                               net.minecraft.world.level.block.state.BlockState state, ServerPlayer player,
                               @org.spongepowered.asm.mixin.injection.Coerce Object xp) {
        return UltimineCompanion.breakBlock(player, pos, () -> gameMode.destroyBlock(pos));
    }

    @Group(name = "tl$ultimineBlock", min = 1, max = 1)
    @Redirect(method = "blockBroken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;m_9280_(Lnet/minecraft/core/BlockPos;)Z",
            remap = false), remap = false, require = 0)
    private boolean tl$breakRelease(ServerPlayerGameMode gameMode, BlockPos pos,
                                   net.minecraft.world.level.Level level, BlockPos origin,
                                   net.minecraft.world.level.block.state.BlockState state, ServerPlayer player,
                                   @org.spongepowered.asm.mixin.injection.Coerce Object xp) {
        return UltimineCompanion.breakBlock(player, pos, () -> gameMode.destroyBlock(pos));
    }

    // Accelerated Decay integration uses a separate removal path for leaves.
    @Group(name = "tl$ultimineLeaves", min = 1, max = 1)
    @Redirect(method = "blockBroken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z",
            remap = false), remap = false, require = 0)
    private boolean tl$leavesDev(net.minecraft.world.level.Level world, BlockPos pos, boolean drops,
                                net.minecraft.world.entity.Entity breaker) {
        return breaker instanceof ServerPlayer player && UltimineCompanion.breakBlock(player, pos,
                () -> world.destroyBlock(pos, drops, breaker));
    }

    @Group(name = "tl$ultimineLeaves", min = 1, max = 1)
    @Redirect(method = "blockBroken", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;m_46953_(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z",
            remap = false), remap = false, require = 0)
    private boolean tl$leavesRelease(net.minecraft.world.level.Level world, BlockPos pos, boolean drops,
                                    net.minecraft.world.entity.Entity breaker) {
        return breaker instanceof ServerPlayer player && UltimineCompanion.breakBlock(player, pos,
                () -> world.destroyBlock(pos, drops, breaker));
    }
}
