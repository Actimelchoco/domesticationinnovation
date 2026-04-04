package com.github.alexthe668.domesticationinnovation.server.entity.ai;

import com.github.alexthe666.citadel.server.entity.IComandableMob;
import com.github.alexthe668.domesticationinnovation.DomesticationMod;
import com.github.alexthe668.domesticationinnovation.server.enchantment.DIEnchantmentRegistry;
import com.github.alexthe668.domesticationinnovation.server.entity.ModifedToBeTameable;
import com.github.alexthe668.domesticationinnovation.server.entity.TameableUtils;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.ai.TameGoalSupport;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameMode;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;

import java.util.EnumSet;

public class FollowOwner2Goal extends Goal {
    private static final int TELEPORT_CHECK_DELAY_TICKS = 40;

    private final Animal tamable;
    private final LevelReader level;
    private final double speedModifier;
    private final PathNavigation navigation;
    private final float stopDistance;
    private final float startDistance;
    private final boolean canFly;
    private LivingEntity owner;
    private int timeToRecalcPath;
    private float oldWaterCost;

    public FollowOwner2Goal(Animal animal, double speedModifier, float startDistance, float stopDistance, boolean flies) {
        this.tamable = animal;
        this.level = animal.level();
        this.speedModifier = speedModifier;
        this.navigation = animal.getNavigation();
        this.startDistance = startDistance;
        this.stopDistance = stopDistance;
        this.canFly = flies;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity livingentity = ((ModifedToBeTameable) this.tamable).getTameOwner();
        if(tamable instanceof IComandableMob commandableMob && commandableMob.getCommand() != 2 && DomesticationMod.CONFIG.trinaryCommandSystem.get()) {
            return false;
        }else if (livingentity == null) {
            return false;
        } else if (livingentity.isSpectator()) {
            return false;
        } else if (((ModifedToBeTameable) this.tamable).isStayingStill()) {
            return false;
        } else if (this.tamable.distanceToSqr(livingentity) < (double) (this.startDistance * this.startDistance)) {
            return false;
        } else {
            this.owner = livingentity;
            return true;
        }
    }

    public boolean canContinueToUse() {
        if(tamable instanceof IComandableMob commandableMob && commandableMob.getCommand() != 2 && DomesticationMod.CONFIG.trinaryCommandSystem.get()) {
            return false;
        }else if (this.navigation.isDone()) {
            return false;
        } else if (((ModifedToBeTameable) this.tamable).isStayingStill()) {
            return false;
        } else {
            return !(this.tamable.distanceToSqr(this.owner) <= (double) (this.stopDistance * this.stopDistance));
        }
    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.tamable.getPathfindingMalus(BlockPathTypes.WATER);
        this.tamable.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.tamable.setPathfindingMalus(BlockPathTypes.WATER, this.oldWaterCost);
    }

    public void tick() {
        if (TameableUtils.hasEnchant(tamable, DIEnchantmentRegistry.AMPHIBIOUS) && tamable.isInWaterOrBubble() && this.tamable.distanceToSqr(this.owner) < 144.0D) {
            tamable.getNavigation().moveTo(owner, speedModifier);
        }
        this.tamable.getLookControl().setLookAt(this.owner, 10.0F, (float) this.tamable.getMaxHeadXRot());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = TELEPORT_CHECK_DELAY_TICKS;
            if (!this.tamable.isLeashed() && !this.tamable.isPassenger()) {
                if (shouldTeleportToOwner()) {
                    this.teleportToOwner();
                } else {
                    this.navigation.moveTo(this.owner, this.speedModifier);
                }

            }
        }
    }

    private boolean shouldTeleportToOwner() {
        TameData data = TameRegistry.get(this.tamable.getUUID());
        if (data != null) {
            TameMode mode = TameMode.byId(data.mode);
            if (mode == TameMode.BOSS) {
                if (this.tamable.getTarget() != null && this.tamable.getTarget().isAlive()) {
                    return false;
                }
                if (this.tamable instanceof net.minecraft.world.entity.TamableAnimal tame
                        && this.tamable.level() instanceof ServerLevel serverLevel
                        && TameGoalSupport.hasSharedBossTarget(serverLevel, tame)) {
                    return false;
                }
            }
            double teleportDistance = data.closeMovement ? 12.0D : 24.0D;
            if ((mode == TameMode.MONSTER_HUNTER || mode == TameMode.AGGRESSIVE)
                    && this.tamable.getTarget() != null
                    && this.tamable.getTarget().isAlive()) {
                teleportDistance = 44.0D;
            }
            if (mode == TameMode.ARENA) {
                if (this.tamable.getTarget() != null && this.tamable.getTarget().isAlive()) {
                    return false;
                }
                teleportDistance = 64.0D;
            }
            if (mode == TameMode.BODYGUARD) {
                teleportDistance = Math.max(1.0D, data.bodyguardRange * 2.0D);
            }
            return this.tamable.distanceToSqr(this.owner) >= teleportDistance * teleportDistance;
        }
        return this.tamable.distanceToSqr(this.owner) >= 24.0D * 24.0D;
    }

    private void teleportToOwner() {
        BlockPos blockpos = this.owner.blockPosition();

        for (int i = 0; i < 10; ++i) {
            int j = this.randomIntInclusive(-3, 3);
            int k = this.randomIntInclusive(-1, 1);
            int l = this.randomIntInclusive(-3, 3);
            boolean flag = this.maybeTeleportTo(blockpos.getX() + j, blockpos.getY() + k, blockpos.getZ() + l);
            if (flag) {
                return;
            }
        }

    }

    private boolean maybeTeleportTo(int p_25304_, int p_25305_, int p_25306_) {
        if (Math.abs((double) p_25304_ - this.owner.getX()) < 2.0D && Math.abs((double) p_25306_ - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(p_25304_, p_25305_, p_25306_))) {
            return false;
        } else {
            this.tamable.moveTo((double) p_25304_ + 0.5D, p_25305_, (double) p_25306_ + 0.5D, this.tamable.getYRot(), this.tamable.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        BlockPathTypes blockpathtypes = WalkNodeEvaluator.getBlockPathTypeStatic(this.level, pos.mutable());
        if (TameableUtils.hasEnchant(tamable, DIEnchantmentRegistry.AMPHIBIOUS) && level.isWaterAt(pos)) {
            return true;
        }
        if (blockpathtypes != BlockPathTypes.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = this.level.getBlockState(pos.below());
            if (!this.canFly && blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pos.subtract(this.tamable.blockPosition());
                return this.level.noCollision(this.tamable, this.tamable.getBoundingBox().move(blockpos));
            }
        }
    }

    private int randomIntInclusive(int p_25301_, int p_25302_) {
        return this.tamable.getRandom().nextInt(p_25302_ - p_25301_ + 1) + p_25301_;
    }
}
