package fuzs.mutantmonsters.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public final class DITameCreeperMinionCompat {
    private static final String CREEPER_MINION_CLASS = "fuzs.mutantmonsters.world.entity.CreeperMinion";
    private static final String TAME_REGISTRY_CLASS = "com.github.alexthe668.domesticationinnovation.server.tameslevel.tame.TameRegistry";
    private static final String BONUS_DAMAGE_FIELD = "bonusDamage";

    private DITameCreeperMinionCompat() {
    }

    public static void enforceTamedSettings(Object creeperMinion) {
        if (!isTamedCreeperMinion(creeperMinion)) {
            return;
        }
        invokeBooleanMethod(creeperMinion, "setDestroyBlocks", false);
    }

    public static boolean shouldDiscardAfterExplosion(Object creeperMinion) {
        return !isTamedCreeperMinion(creeperMinion);
    }

    public static float adjustExplosionDamage(Object exploder, Object target, float baseDamage) {
        if (!isTamedCreeperMinion(exploder)) {
            return baseDamage;
        }
        if (exploder == target) {
            return 0.0F;
        }
        float bonusDamage = getTameBonusDamage(exploder);
        return bonusDamage <= 0.0F ? baseDamage : baseDamage + bonusDamage;
    }

    private static boolean isTamedCreeperMinion(Object entity) {
        if (entity == null || !CREEPER_MINION_CLASS.equals(entity.getClass().getName())) {
            return false;
        }
        Object tamed = invokeZeroArg(entity, "m_21824_", "isTame");
        return tamed instanceof Boolean && (Boolean) tamed;
    }

    private static float getTameBonusDamage(Object entity) {
        try {
            UUID uuid = getEntityUuid(entity);
            if (uuid == null) {
                return 0.0F;
            }
            Class<?> registryClass = Class.forName(TAME_REGISTRY_CLASS);
            Method getMethod = registryClass.getMethod("get", UUID.class);
            Object data = getMethod.invoke(null, uuid);
            if (data == null) {
                return 0.0F;
            }
            Field field = data.getClass().getField(BONUS_DAMAGE_FIELD);
            double bonusDamage = field.getDouble(data);
            return (float) Math.max(0.0D, bonusDamage);
        } catch (Throwable ignored) {
            return 0.0F;
        }
    }

    private static UUID getEntityUuid(Object entity) {
        Object value = invokeZeroArg(entity, "m_20148_", "getUUID");
        return value instanceof UUID uuid ? uuid : null;
    }

    private static void invokeBooleanMethod(Object target, String methodName, boolean value) {
        try {
            Method method = findMethod(target.getClass(), methodName, boolean.class);
            if (method == null) {
                return;
            }
            method.setAccessible(true);
            method.invoke(target, value);
        } catch (Throwable ignored) {
        }
    }

    private static Object invokeZeroArg(Object target, String... names) {
        for (String name : names) {
            try {
                Method method = findMethod(target.getClass(), name);
                if (method == null) {
                    continue;
                }
                method.setAccessible(true);
                return method.invoke(target);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return current.getMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
