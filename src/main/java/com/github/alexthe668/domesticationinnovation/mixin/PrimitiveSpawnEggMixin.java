package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityType.class)
public abstract class PrimitiveSpawnEggMixin {
    @Inject(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;", at = @At("RETURN"))
    private void tl$ownerEgg(ServerLevel level, ItemStack stack, Player player, BlockPos pos, MobSpawnType reason,
                             boolean offset, boolean invert, CallbackInfoReturnable<Entity> cir) {
        if (player != null && reason == MobSpawnType.SPAWN_EGG && stack != null && stack.getItem() instanceof SpawnEggItem
                && cir.getReturnValue() instanceof Mob mob && PrimitiveMobsCompat.isPrimitivePet(mob)) {
            PrimitiveMobsCompat.tame(mob, player);
        }
    }
}
