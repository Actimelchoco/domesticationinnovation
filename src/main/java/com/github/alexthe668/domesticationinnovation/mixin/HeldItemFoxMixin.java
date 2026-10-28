package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Fox.class)
public abstract class HeldItemFoxMixin extends Animal {
    protected HeldItemFoxMixin(EntityType<? extends Animal> type,Level level){super(type,level);}
    @Inject(method="canHoldItem",at=@At("HEAD"),cancellable=true)
    private void tl$keepWeapon(ItemStack candidate,CallbackInfoReturnable<Boolean> cir){
        if(HeldItemTameWeapons.protectHeldItem(this)
                || TameEntityAdapter.isTame(this)&&HeldItemTameWeapons.isWeapon(candidate)&&TameDuelManager.isEntityInDuel(getUUID()))cir.setReturnValue(false);
    }
    @Inject(method="canEat",at=@At("HEAD"),cancellable=true)
    private void tl$doNotEatWeapon(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
        if(HeldItemTameWeapons.protectHeldItem(this))cir.setReturnValue(false);
    }
}
