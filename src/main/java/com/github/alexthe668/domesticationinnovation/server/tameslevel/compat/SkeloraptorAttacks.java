package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameData;
import com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;

/** Native animation attacks bypass Mob.doHurtTarget and its attack attribute. */
public final class SkeloraptorAttacks {
    private SkeloraptorAttacks() {}

    public static boolean hurtNativeBase(LivingEntity target, DamageSource source, float damage) {
        if (source.getEntity() instanceof LivingEntity attacker
                && "legendary_monsters:skeloraptor".equals(
                        String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType())))) {
            TameData data = TameRegistry.get(attacker.getUUID());
            if (data != null && Double.isFinite(data.bonusDamage)) {
                damage = (float) Math.min(Float.MAX_VALUE, damage + Math.max(0, data.bonusDamage));
            }
        }
        // Preserve the native caster/source so the normal base-hit event hooks run.
        return target.hurt(source, damage);
    }
}
