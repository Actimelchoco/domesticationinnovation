package net.miauczel.legendary_monsters.compat;

import net.minecraft.world.entity.TamableAnimal;

import java.lang.reflect.Method;

public final class DIServerPetCommandCompat {
    private static final String COMMAND_TAG = "di_server_pet_command";
    private static final int COMMAND_FOLLOW = 0;
    private static final int COMMAND_SIT = 1;
    private static final int COMMAND_WANDER = 2;

    private DIServerPetCommandCompat() {
    }

    public static void setFollow(Object tame, boolean enabled) {
        if (!(tame instanceof TamableAnimal animal)) {
            return;
        }
        if (enabled) {
            writeCommand(animal, COMMAND_FOLLOW);
            setSitting(animal, false);
        } else if (readCommand(animal) == COMMAND_FOLLOW) {
            writeCommand(animal, COMMAND_WANDER);
            stopNavigation(animal);
        }
    }

    public static void setSit(Object tame, boolean enabled) {
        if (!(tame instanceof TamableAnimal animal)) {
            return;
        }
        writeCommand(animal, enabled ? COMMAND_SIT : COMMAND_FOLLOW);
        setSitting(animal, enabled);
        if (enabled) {
            stopNavigation(animal);
            clearTarget(animal);
        }
    }

    public static void setWander(Object tame, boolean enabled) {
        if (!(tame instanceof TamableAnimal animal)) {
            return;
        }
        if (enabled) {
            writeCommand(animal, COMMAND_WANDER);
            setSitting(animal, false);
            clearTarget(animal);
            stopNavigation(animal);
        } else if (readCommand(animal) == COMMAND_WANDER) {
            writeCommand(animal, COMMAND_FOLLOW);
        }
    }

    public static boolean shouldFollow(Object tame) {
        return tame instanceof TamableAnimal animal
                && readCommand(animal) == COMMAND_FOLLOW
                && !isOrderedToSit(animal);
    }

    public static boolean isWandering(Object tame) {
        return tame instanceof TamableAnimal animal && readCommand(animal) == COMMAND_WANDER;
    }

    public static boolean isFollowing(Object tame) {
        return tame instanceof TamableAnimal animal && readCommand(animal) == COMMAND_FOLLOW;
    }

    public static boolean isSitting(Object tame) {
        return tame instanceof TamableAnimal animal && readCommand(animal) == COMMAND_SIT;
    }

    public static void syncManualCommand(Object tame) {
        if (!(tame instanceof TamableAnimal animal)) {
            return;
        }
        writeCommand(animal, isOrderedToSit(animal) ? COMMAND_SIT : COMMAND_FOLLOW);
        if (isOrderedToSit(animal)) {
            clearTarget(animal);
            stopNavigation(animal);
        }
    }

    public static void syncManualCommand(Object player, Object tame) {
        syncManualCommand(tame);
    }

    private static void setSitting(TamableAnimal animal, boolean sitting) {
        tryInvokeSetOrderedToSit(animal, sitting);
        tryInvokeSetInSittingPose(animal, sitting);
        if (sitting) {
            clearTarget(animal);
        }
    }

    private static void tryInvokeSetOrderedToSit(TamableAnimal animal, boolean sitting) {
        try {
            Method method = animal.getClass().getMethod("m_21839_", boolean.class);
            method.setAccessible(true);
            method.invoke(animal, sitting);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = animal.getClass().getMethod("setOrderedToSit", boolean.class);
            method.setAccessible(true);
            method.invoke(animal, sitting);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void tryInvokeSetInSittingPose(TamableAnimal animal, boolean sitting) {
        try {
            Method method = animal.getClass().getMethod("m_21837_", boolean.class);
            method.setAccessible(true);
            method.invoke(animal, sitting);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = animal.getClass().getMethod("setInSittingPose", boolean.class);
            method.setAccessible(true);
            method.invoke(animal, sitting);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void clearTarget(TamableAnimal animal) {
        try {
            Method method = animal.getClass().getMethod("m_6710_", net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            method.invoke(animal, new Object[]{null});
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = animal.getClass().getMethod("setTarget", net.minecraft.world.entity.LivingEntity.class);
            method.setAccessible(true);
            method.invoke(animal, new Object[]{null});
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void stopNavigation(TamableAnimal animal) {
        try {
            Method getNavigation = animal.getClass().getMethod("m_21573_");
            getNavigation.setAccessible(true);
            Object navigation = getNavigation.invoke(animal);
            if (navigation != null) {
                invokeStopNavigation(navigation);
            }
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method getNavigation = animal.getClass().getMethod("getNavigation");
            getNavigation.setAccessible(true);
            Object navigation = getNavigation.invoke(animal);
            if (navigation != null) {
                invokeStopNavigation(navigation);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invokeStopNavigation(Object navigation) {
        try {
            Method stop = navigation.getClass().getMethod("m_26573_");
            stop.setAccessible(true);
            stop.invoke(navigation);
            return;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method stop = navigation.getClass().getMethod("stop");
            stop.setAccessible(true);
            stop.invoke(navigation);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static int readCommand(TamableAnimal animal) {
        try {
            Object tag = getPersistentData(animal);
            if (tag != null && hasIntTag(tag, COMMAND_TAG)) {
                return getIntTag(tag, COMMAND_TAG);
            }
        } catch (Throwable ignored) {
        }
        return isOrderedToSit(animal) ? COMMAND_SIT : COMMAND_FOLLOW;
    }

    private static void writeCommand(TamableAnimal animal, int command) {
        try {
            Object tag = getPersistentData(animal);
            if (tag != null) {
                putIntTag(tag, COMMAND_TAG, command);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isOrderedToSit(TamableAnimal animal) {
        try {
            Method method = animal.getClass().getMethod("isOrderedToSit");
            method.setAccessible(true);
            Object value = method.invoke(animal);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method method = animal.getClass().getMethod("m_21827_");
            method.setAccessible(true);
            Object value = method.invoke(animal);
            if (value instanceof Boolean bool) {
                return bool;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return false;
    }

    private static Object getPersistentData(TamableAnimal animal) throws ReflectiveOperationException {
        try {
            Method method = animal.getClass().getMethod("getPersistentData");
            method.setAccessible(true);
            return method.invoke(animal);
        } catch (NoSuchMethodException ignored) {
            Method method = animal.getClass().getMethod("m_20241_");
            method.setAccessible(true);
            return method.invoke(animal);
        }
    }

    private static boolean hasIntTag(Object tag, String key) throws ReflectiveOperationException {
        try {
            Method method = tag.getClass().getMethod("m_128441_", String.class);
            method.setAccessible(true);
            Object value = method.invoke(tag, key);
            return value instanceof Boolean bool && bool;
        } catch (NoSuchMethodException ignored) {
            Method method = tag.getClass().getMethod("contains", String.class);
            method.setAccessible(true);
            Object value = method.invoke(tag, key);
            return value instanceof Boolean bool && bool;
        }
    }

    private static int getIntTag(Object tag, String key) throws ReflectiveOperationException {
        try {
            Method method = tag.getClass().getMethod("m_128451_", String.class);
            method.setAccessible(true);
            Object value = method.invoke(tag, key);
            return value instanceof Integer integer ? integer : COMMAND_FOLLOW;
        } catch (NoSuchMethodException ignored) {
            Method method = tag.getClass().getMethod("getInt", String.class);
            method.setAccessible(true);
            Object value = method.invoke(tag, key);
            return value instanceof Integer integer ? integer : COMMAND_FOLLOW;
        }
    }

    private static void putIntTag(Object tag, String key, int value) throws ReflectiveOperationException {
        try {
            Method method = tag.getClass().getMethod("m_128405_", String.class, int.class);
            method.setAccessible(true);
            method.invoke(tag, key, value);
            return;
        } catch (NoSuchMethodException ignored) {
        }
        Method method = tag.getClass().getMethod("putInt", String.class, int.class);
        method.setAccessible(true);
        method.invoke(tag, key, value);
    }
}
