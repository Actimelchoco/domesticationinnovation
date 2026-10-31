package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.CompanionsRespawnTotem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import java.util.List;

@Pseudo
@Mixin(targets = "dev.xylonity.companions.common.blockentity.RespawnTotemBlockEntity", remap = false)
public class CompanionsRespawnTotemMixin {
    @Redirect(method = "captureNearby", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_45976_(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<?> tl$captureSupportedTames(Level level, Class<?> type, AABB bounds) {
        return CompanionsRespawnTotem.captureCandidates(level, bounds);
    }
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;m_7967_(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean tl$restoreTrackedTame(ServerLevel level, Entity entity, Level tickLevel,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity totem) {
        return CompanionsRespawnTotem.addRespawn(level, entity, totem);
    }
}
