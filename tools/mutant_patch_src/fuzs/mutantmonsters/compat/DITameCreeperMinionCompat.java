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

    public static void handlePostExplosion(Object creeperMinion) {
        if (creeperMinion == null) {
            return;
        }
        if (isTamedCreeperMinion(creeperMinion)) {
            return;
        }
        notifyExplosionOwner(creeperMinion);
        setBooleanField(creeperMinion, "f_20890_", true);
        invokeZeroArg(creeperMinion, "m_146870_");
        try {
            Class<?> entityUtilClass = Class.forName("fuzs.mutantmonsters.util.EntityUtil");
            Method method = entityUtilClass.getMethod("spawnLingeringCloud", Class.forName("net.minecraft.world.entity.LivingEntity"));
            method.invoke(null, creeperMinion);
        } catch (Throwable ignored) {
        }
    }

    private static void notifyExplosionOwner(Object creeperMinion) {
        try {
            Object level = invokeZeroArg(creeperMinion, "m_9236_", "level");
            if (level == null) {
                return;
            }
            Object gameRules = invokeZeroArg(level, "m_46469_", "getGameRules");
            if (gameRules == null) {
                return;
            }
            Class<?> gameRulesClass = Class.forName("net.minecraft.world.level.GameRules");
            Field sendDeathMessagesField = gameRulesClass.getField("f_46142_");
            Object sendDeathMessagesKey = sendDeathMessagesField.get(null);
            Method getBoolean = gameRulesClass.getMethod("m_46207_", sendDeathMessagesKey.getClass());
            Object enabled = getBoolean.invoke(gameRules, sendDeathMessagesKey);
            if (!(enabled instanceof Boolean) || !((Boolean) enabled)) {
                return;
            }
            Object owner = invokeZeroArg(creeperMinion, "m_269323_", "getOwner");
            if (owner == null || !owner.getClass().getName().equals("net.minecraft.server.level.ServerPlayer")) {
                return;
            }
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component");
            Method getName = findZeroArgMethod(creeperMinion.getClass(), "m_5446_", "getDisplayName");
            if (getName == null) {
                return;
            }
            Object displayName = getName.invoke(creeperMinion);
            Method translatable = componentClass.getMethod("m_237110_", String.class, Object[].class);
            Object message = translatable.invoke(null, "death.attack.explosion", new Object[]{new Object[]{displayName}});
            Method sendSystemMessage = findMethod(owner.getClass(), "m_213846_", componentClass);
            if (sendSystemMessage != null) {
                sendSystemMessage.invoke(owner, message);
            }
        } catch (Throwable ignored) {
        }
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

    private static void setBooleanField(Object target, String fieldName, boolean value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            field.setBoolean(target, value);
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

    private static Method findZeroArgMethod(Class<?> type, String... names) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                for (String name : names) {
                    if (method.getName().equals(name) && method.getParameterCount() == 0) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
            try {
                for (Method method : current.getMethods()) {
                    for (String name : names) {
                        if (method.getName().equals(name) && method.getParameterCount() == 0) {
                            method.setAccessible(true);
                            return method;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
