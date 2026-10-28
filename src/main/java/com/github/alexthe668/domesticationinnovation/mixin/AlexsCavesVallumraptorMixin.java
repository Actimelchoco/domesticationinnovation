package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.VallumraptorWeapons;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.github.alexmodguy.alexscaves.server.entity.living.VallumraptorEntity", remap=false)
public abstract class AlexsCavesVallumraptorMixin extends TamableAnimal {
    protected AlexsCavesVallumraptorMixin(EntityType<? extends TamableAnimal> type, Level level) { super(type, level); }

    @Shadow public abstract void onGetItem(ItemEntity item);

    @Inject(method={"tick", "m_8119_"}, at=@At("TAIL"), remap=false)
    private void tl$grabNearbyWeapon(CallbackInfo ci) {
        if (level().isClientSide || !isAlive() || !isTame() || isNoAi()
                || !com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons.supportsHeldItems(this)
                || !getMainHandItem().isEmpty() || TameDuelManager.isEntityInDuel(getUUID())) return;
        // The native item goal refuses sitting animals and may stop before its
        // grab animation completes. Retry nearby weapons through the native grab,
        // without adding navigation or changing the selected movement command.
        ItemEntity closest = null;
        double distance = 4.0D;
        for (ItemEntity item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(2.0D))) {
            double candidate = distanceToSqr(item);
            if (item.isAlive() && !item.hasPickUpDelay() && VallumraptorWeapons.isWeapon(item.getItem())
                    && candidate < distance && hasLineOfSight(item)) {
                closest = item;
                distance = candidate;
            }
        }
        if (closest != null) onGetItem(closest);
    }

    @Inject(method="canTargetItem", at=@At("HEAD"), cancellable=true, remap=false)
    private void tl$targetWeapons(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (!com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons.supportsHeldItems(this)) return;
        // This predicate is also used by tick's eating code. A held weapon must
        // return false there, and must not be displaced by another food/item grab.
        if (VallumraptorWeapons.isArmed(this)) cir.setReturnValue(false);
        else if (VallumraptorWeapons.isWeapon(stack)) cir.setReturnValue(isTame() && getMainHandItem().isEmpty()
                && !TameDuelManager.isEntityInDuel(getUUID()));
    }

    @Inject(method="onGetItem", at=@At("HEAD"), cancellable=true, remap=false)
    private void tl$keepHeldWeapon(ItemEntity item, CallbackInfo ci) {
        if (!com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons.supportsHeldItems(this)) return;
        // A goal may already have selected food when the hand changes.
        if (VallumraptorWeapons.isArmed(this)
                || isTame() && com.github.alexthe668.domesticationinnovation.server.tameslevel.events.AnimightEquipmentEvents.isSealedItem(getMainHandItem())
                || VallumraptorWeapons.isWeapon(item.getItem())
                && (!isTame() || TameDuelManager.isEntityInDuel(getUUID()))) ci.cancel();
    }
}
