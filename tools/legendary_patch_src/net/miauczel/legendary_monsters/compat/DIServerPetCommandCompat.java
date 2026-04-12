package net.miauczel.legendary_monsters.compat;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

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
            animal.setTarget(null);
        }
    }

    public static void setWander(Object tame, boolean enabled) {
        if (!(tame instanceof TamableAnimal animal)) {
            return;
        }
        if (enabled) {
            writeCommand(animal, COMMAND_WANDER);
            setSitting(animal, false);
            animal.setTarget(null);
            stopNavigation(animal);
        } else if (readCommand(animal) == COMMAND_WANDER) {
            writeCommand(animal, COMMAND_FOLLOW);
        }
    }

    public static boolean shouldFollow(Object tame) {
        return tame instanceof TamableAnimal animal
                && readCommand(animal) == COMMAND_FOLLOW
                && !animal.isOrderedToSit();
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
        writeCommand(animal, animal.isOrderedToSit() ? COMMAND_SIT : COMMAND_FOLLOW);
        if (animal.isOrderedToSit()) {
            animal.setTarget(null);
            stopNavigation(animal);
        }
    }

    public static void syncManualCommand(Object player, Object tame) {
        syncManualCommand(tame);
    }

    private static void setSitting(TamableAnimal animal, boolean sitting) {
        tryInvokeSetOrderedToSit(animal, sitting);
        animal.setInSittingPose(sitting);
        if (sitting) {
            animal.setTarget(null);
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
        animal.setOrderedToSit(sitting);
    }

    private static void stopNavigation(TamableAnimal animal) {
        PathNavigation navigation = animal.getNavigation();
        if (navigation != null) {
            navigation.stop();
        }
    }

    private static int readCommand(TamableAnimal animal) {
        try {
            if (animal.getPersistentData().contains(COMMAND_TAG)) {
                return animal.getPersistentData().getInt(COMMAND_TAG);
            }
        } catch (Throwable ignored) {
        }
        return animal.isOrderedToSit() ? COMMAND_SIT : COMMAND_FOLLOW;
    }

    private static void writeCommand(TamableAnimal animal, int command) {
        animal.getPersistentData().putInt(COMMAND_TAG, command);
    }
}
