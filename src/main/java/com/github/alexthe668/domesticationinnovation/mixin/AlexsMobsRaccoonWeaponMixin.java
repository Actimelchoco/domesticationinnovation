package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.HeldItemTameWeapons;
import net.minecraft.world.entity.*;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameDuelManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.entity.EntityRaccoon",remap=false)
public abstract class AlexsMobsRaccoonWeaponMixin extends TamableAnimal {
    @Shadow public int lookForWaterBeforeEatingTimer;
    @Shadow public abstract void setWashing(boolean washing);
    @Shadow public abstract boolean isSitting();
    protected AlexsMobsRaccoonWeaponMixin(EntityType<? extends TamableAnimal> type,Level level){super(type,level);}
    @Inject(method={"tick","m_8119_"},at=@At("HEAD"),remap=false)
    private void tl$stopWeaponWashing(CallbackInfo ci){
        boolean dueling = TameDuelManager.isEntityInDuel(getUUID());
        if(dueling || HeldItemTameWeapons.protectHeldItem(this)){lookForWaterBeforeEatingTimer=0;setWashing(false);}
        // Raccoons store sitting separately from vanilla's ordered-to-sit flag.
        if(dueling && isSitting()) setOrderedToSit(false);
    }
    @Inject(method="canTargetItem",at=@At("HEAD"),cancellable=true,remap=false)
    private void tl$keepFoodSeparate(ItemStack stack,CallbackInfoReturnable<Boolean> cir){
        // Native canTargetItem is also the eating predicate; weapons use the separate pickup goal.
        if(TameDuelManager.isEntityInDuel(getUUID())||HeldItemTameWeapons.protectHeldItem(this)||isTame()&&HeldItemTameWeapons.isWeapon(stack))cir.setReturnValue(false);
    }
    @Inject(method="onGetItem",at=@At("HEAD"),cancellable=true,remap=false)
    private void tl$keepHeldWeapon(ItemEntity item,CallbackInfo ci){if(HeldItemTameWeapons.protectHeldItem(this))ci.cancel();}
    @Group(name="tl$enchantedHit",min=1,max=1)
    @Redirect(method={"tick","m_8119_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=false),remap=false,require=0)
    private boolean tl$hitDev(LivingEntity target,DamageSource source,float damage){return tl$hit(target,source,damage);}
    @Group(name="tl$enchantedHit",min=1,max=1)
    @Redirect(method={"tick","m_8119_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=false),remap=false,require=0)
    private boolean tl$hitRelease(LivingEntity target,DamageSource source,float damage){return tl$hit(target,source,damage);}
    @Unique private boolean tl$hit(LivingEntity target,DamageSource source,float damage){
        // Calling this.doHurtTarget would only restart the raccoon's animation.
        return HeldItemTameWeapons.isArmed(this)?super.doHurtTarget(target):target.hurt(source,damage);
    }
    @Group(name="tl$keepHandOnInteract",min=1,max=1)
    @Redirect(method={"mobInteract","m_6071_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;isEmpty()Z",ordinal=0,remap=false),remap=false,require=0)
    private boolean tl$skipNativeDropDev(ItemStack stack){return tl$skipDrop(stack);}
    @Group(name="tl$keepHandOnInteract",min=1,max=1)
    @Redirect(method={"mobInteract","m_6071_"},at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;m_41619_()Z",ordinal=0,remap=false),remap=false,require=0)
    private boolean tl$skipNativeDropRelease(ItemStack stack){return tl$skipDrop(stack);}
    @Unique private boolean tl$skipDrop(ItemStack stack){return HeldItemTameWeapons.protectHeldItem(this)&&stack==getMainHandItem()||stack.isEmpty();}
}
