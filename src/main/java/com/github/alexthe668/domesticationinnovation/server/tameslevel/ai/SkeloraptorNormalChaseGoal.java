package com.github.alexthe668.domesticationinnovation.server.tameslevel.ai;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.events.FriendlyOwnerProtection;
import net.minecraft.world.entity.player.Player;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraftforge.registries.ForgeRegistries;

/** Per-entity navigation fallback; target selection and native attacks remain unchanged. */
public final class SkeloraptorNormalChaseGoal extends SkeloraptorDuelChaseGoal {
    private final TamableAnimal pet;

    private SkeloraptorNormalChaseGoal(TamableAnimal pet) throws NoSuchMethodException {
        super(pet);
        this.pet = pet;
    }

    public static void ensure(TamableAnimal pet) {
        var id = ForgeRegistries.ENTITY_TYPES.getKey(pet.getType());
        if (id == null || !id.toString().equals("legendary_monsters:skeloraptor")) return;
        if (pet.goalSelector.getAvailableGoals().stream()
                .anyMatch(g -> g.getGoal() instanceof SkeloraptorNormalChaseGoal)) return;
        try { pet.goalSelector.addGoal(0, new SkeloraptorNormalChaseGoal(pet)); }
        catch (NoSuchMethodException ignored) { /* Preserve unknown versions' native AI. */ }
    }

    @Override
    protected boolean allowsTarget(LivingEntity target) {
        if (!pet.isTame() || TameDuelManager.isTameInDuel(pet.getUUID())) return false;
        TameData data = TameRegistry.get(pet.getUUID());
        if (data == null || data.dead || data.stored || data.guardianResting || data.hungerForcedSit
                || data.movementOrder == 1 || TameMode.byId(data.mode) == TameMode.PASSIVE
                || pet.isAlliedTo(target) || TameRegistry.isProtectedAttackTarget(pet, target)
                || FriendlyOwnerProtection.blocks(target, pet)
                || target instanceof Player && PlayerDebugSettings.tamesFriendly(data.ownerUUID)) return false;
        if (data.hasProtectionZone) {
            if (!pet.level().dimension().location().toString().equals(data.protectionDimension)) return false;
            double radius = data.protectionRadius;
            double x = data.protectionX + 0.5D, y = data.protectionY, z = data.protectionZ + 0.5D;
            if (pet.distanceToSqr(x, y, z) > (radius + 6) * (radius + 6)
                    || target.distanceToSqr(x, y, z) > radius * radius) return false;
        }
        if (TameMode.byId(data.mode) == TameMode.BODYGUARD) {
            LivingEntity owner = pet.getOwner();
            double range = Math.max(1.0D, data.bodyguardRange);
            if (owner == null || pet.distanceToSqr(owner) > range * range) return false;
        }
        return true;
    }
}
