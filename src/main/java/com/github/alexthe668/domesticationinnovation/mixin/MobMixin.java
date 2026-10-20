package com.github.alexthe668.domesticationinnovation.mixin;

import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.compat.PrimitiveMobsCompat;
import com.github.alexthe668.domesticationinnovation.server.entity.ai.AmphibiousPathNavigation;
import com.github.alexthe668.domesticationinnovation.server.entity.ai.AquaticMoveControl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void tl$blockFriendlyOwnerMelee(Entity target, CallbackInfoReturnable<Boolean> cir) {
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection.blocks(target, this)) cir.setReturnValue(false);
    }

    @Shadow protected MoveControl moveControl;
    @Shadow protected PathNavigation navigation;
    @Shadow  public abstract void setPathfindingMalus(BlockPathTypes water, float v);
    private boolean hasWaterEnchantNavigator = false;
    private MoveControl prevMoveControl;
    private PathNavigation prevNavigation;
    @Unique private boolean tl$primitiveGoalsInstalled;
    @Unique private final java.util.List<net.minecraft.world.entity.ai.goal.WrappedGoal> tl$primitiveWildGoals = new java.util.ArrayList<>();

    @Inject(method = "serverAiStep", at = @At("HEAD"), cancellable = true)
    private void tl$pauseUnobservedLostHunter(CallbackInfo ci) {
        Mob mob = (Mob)(Object)this;
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.RankedRefereeService.isReferee(mob)) { ci.cancel();return; }
        if (com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameLimitService.isReleased(mob)
                && !com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.LostHunterActivity.active(mob)) ci.cancel();
    }

    @Inject(method = "serverAiStep", at = @At("HEAD"))
    private void tl$primitivePetGoals(CallbackInfo ci) {
        Mob mob = (Mob) (Object) this;
        if (!PrimitiveMobsCompat.isPrimitivePet(mob)) return;
        if (!((ModifedToBeTameable) mob).isTame()) {
            if (tl$primitiveGoalsInstalled) {
                mob.targetSelector.removeAllGoals(goal -> true);
                for (var goal : tl$primitiveWildGoals) mob.targetSelector.addGoal(goal.getPriority(), goal.getGoal());
                tl$primitiveWildGoals.clear();
                tl$primitiveGoalsInstalled = false;
            }
            return;
        }
        if (!tl$primitiveGoalsInstalled) {
            tl$primitiveWildGoals.addAll(mob.targetSelector.getAvailableGoals());
            mob.targetSelector.removeAllGoals(goal -> true);
            mob.targetSelector.addGoal(1, new PrimitiveMobsCompat.PetTargetGoal(mob));
            tl$primitiveGoalsInstalled = true;
        }
        var pet = (ModifedToBeTameable) mob;
        if (pet.isStayingStill() || mob.getTarget() != null && !pet.isValidAttackTarget(mob.getTarget())) mob.setTarget(null);
    }

    protected MobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Mob;tick()V"},
            remap = true,
            at = @At(value = "TAIL")
    )
    private void di_tick(CallbackInfo ci) {
        if(TameableUtils.isTamed(this) && !level().isClientSide){
            if(TameableUtils.hasEnchant(this, DIEnchantmentRegistry.AMPHIBIOUS) && !((LivingEntity)this instanceof Axolotl)){
                if(!hasWaterEnchantNavigator && this.isInWaterOrBubble()){
                    this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
                    this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
                    prevMoveControl = moveControl;
                    prevNavigation = navigation;
                    moveControl = new AquaticMoveControl((Mob)(LivingEntity)this);
                    navigation = new AmphibiousPathNavigation((Mob)(LivingEntity)this, level());
                    hasWaterEnchantNavigator = true;
                }
                if(hasWaterEnchantNavigator && !this.isInWaterOrBubble()) {
                    moveControl = prevMoveControl;
                    navigation = prevNavigation;
                    hasWaterEnchantNavigator = false;
                }
            }else if(hasWaterEnchantNavigator){
                moveControl = prevMoveControl;
                navigation = prevNavigation;
                hasWaterEnchantNavigator = false;
            }
        }
    }


    @Inject(
            method = {"Lnet/minecraft/world/entity/Mob;pickUpItem(Lnet/minecraft/world/entity/item/ItemEntity;)V"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void di_pickUpItem(ItemEntity item, CallbackInfo ci) {
        if(TameableUtils.isTamed(this) && (TameableUtils.hasEnchant(this, DIEnchantmentRegistry.LINKED_INVENTORY)
                || com.github.alexthe668.domesticationinnovation.server.CommonProxy.getAbilityOrEnchantLevelForCompat(this, "linked_inventory") > 0)){
            // Never let vanilla equipment pickup consume linked items when the owner is absent.
            ci.cancel();
            var ownerId = TameableUtils.getOwnerUUIDOf(this);
            Entity owner = getServer() == null || ownerId == null ? null : getServer().getPlayerList().getPlayer(ownerId);
            if(owner instanceof Player player && !item.isRemoved()){
                // Inventory.add can succeed after inserting only part of a stack.
                // Work on a copy and retain the exact remainder in the world.
                var remaining = item.getItem().copy();
                player.addItem(remaining);
                if(remaining.isEmpty()){
                    item.discard();
                }else{
                    item.setItem(remaining);
                    item.copyPosition(owner);
                }
            }

        }
    }

    @Inject(
            method = {"Lnet/minecraft/world/entity/Mob;playAmbientSound()V"},
            remap = true,
            at = @At(value = "HEAD"),
            cancellable = true
    )
    private void di_playAmbientSound(CallbackInfo ci) {
        if(TameableUtils.isTamed(this) && TameableUtils.hasEnchant(this, DIEnchantmentRegistry.MUFFLED)){
            ci.cancel();
        }
    }
}
