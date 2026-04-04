package com.github.alexthe668.domesticationinnovation.server.tameslevel.compat;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;

public final class BlessfulledCompat {
    private static final String MOD_ID = "blessfulled";
    private static final String POPUP_DATA_CLASS = "org.aqutheseal.blessfulled.particle.DamagePopupParticleData";
    private static final int POPUP_TYPE_DEFAULT = 0;
    private static final int POPUP_TYPE_CRIT = 1;
    private static final int POPUP_TYPE_MAGIC = 5;

    private static boolean resolved;
    private static boolean available;
    private static Constructor<?> popupCtor;

    private BlessfulledCompat() {
    }

    public static void showDamagePopup(Entity attacker, LivingEntity victim, float amount) {
        showDamagePopup(attacker, victim, amount, 6);
    }

    public static void showDamagePopup(Entity attacker, LivingEntity victim, float amount, int kind) {
        if (victim == null || amount <= 0.0F || !(victim.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        ParticleOptions popup = createDamagePopup(amount, kind);
        if (popup == null) {
            return;
        }
        Vec3 pos = victim.position();
        serverLevel.sendParticles(popup, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static void showTameDealtDamagePopup(Entity attacker, LivingEntity victim, float amount) {
        showDamagePopup(attacker, victim, amount, POPUP_TYPE_DEFAULT);
    }

    public static void showTameReceivedDamagePopup(Entity attacker, LivingEntity victim, float amount) {
        showDamagePopup(attacker, victim, amount, POPUP_TYPE_MAGIC);
    }

    private static ParticleOptions createDamagePopup(float amount, int kind) {
        resolve();
        if (!available || popupCtor == null) {
            return null;
        }
        try {
            return (ParticleOptions) popupCtor.newInstance(amount, kind);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        try {
            Class<?> dataClass = Class.forName(POPUP_DATA_CLASS);
            popupCtor = dataClass.getConstructor(float.class, int.class);
            available = true;
        } catch (Throwable ignored) {
            available = false;
            popupCtor = null;
        }
    }
}
