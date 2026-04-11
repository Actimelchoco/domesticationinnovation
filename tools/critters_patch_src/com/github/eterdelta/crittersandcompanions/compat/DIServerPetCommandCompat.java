package com.github.eterdelta.crittersandcompanions.compat;

import com.github.eterdelta.crittersandcompanions.entity.DragonflyEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.navigation.PathNavigation;

import java.lang.reflect.Method;

public class DIServerPetCommandCompat {
    private static final String COMMAND_TAG = "di_server_pet_command";
    private static final int COMMAND_FOLLOW = 0;
    private static final int COMMAND_SIT = 1;
    private static final int COMMAND_WANDER = 2;

    public static void setFollow(Object tame, boolean enabled) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        if (enabled) {
            writeCommand(dragonfly, COMMAND_FOLLOW);
            dragonfly.m_21839_(false);
            return;
        }
        if (readCommand(dragonfly) == COMMAND_FOLLOW) {
            writeCommand(dragonfly, COMMAND_WANDER);
        }
    }

    public static void setSit(Object tame, boolean enabled) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        writeCommand(dragonfly, enabled ? COMMAND_SIT : COMMAND_FOLLOW);
        dragonfly.m_21839_(enabled);
        if (enabled) {
            stopNavigation(dragonfly);
        }
    }

    public static void setWander(Object tame, boolean enabled) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        if (enabled) {
            writeCommand(dragonfly, COMMAND_WANDER);
            dragonfly.m_21839_(false);
            stopNavigation(dragonfly);
            return;
        }
        if (readCommand(dragonfly) == COMMAND_WANDER) {
            writeCommand(dragonfly, COMMAND_FOLLOW);
        }
    }

    public static boolean shouldFollow(Object tame) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return false;
        }
        return readCommand(dragonfly) == COMMAND_FOLLOW;
    }

    public static void syncManualCommand(Object tame) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        writeCommand(dragonfly, dragonfly.m_21827_() ? COMMAND_SIT : COMMAND_FOLLOW);
        if (dragonfly.m_21827_()) {
            stopNavigation(dragonfly);
        }
    }

    public static void syncManualCommand(Object player, Object tame) {
        syncManualCommand(tame);
        if (!(player instanceof Player owner) || !(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        int command = readCommand(dragonfly);
        int messageId = command == COMMAND_SIT ? 1 : command == COMMAND_WANDER ? 0 : 2;
        owner.displayClientMessage(Component.translatable("message.domesticationinnovation.command_" + messageId, dragonfly.getName()), true);
    }

    public static void tickSync(Object tame) {
        if (!(tame instanceof DragonflyEntity dragonfly)) {
            return;
        }
        int command = readCommand(dragonfly);
        if (command == COMMAND_SIT) {
            if (!dragonfly.m_21827_()) {
                dragonfly.m_21839_(true);
            }
            stopNavigation(dragonfly);
            return;
        }
        if (dragonfly.m_21827_()) {
            dragonfly.m_21839_(false);
        }
        if (command == COMMAND_WANDER) {
            stopNavigation(dragonfly);
        }
    }

    private static void stopNavigation(DragonflyEntity dragonfly) {
        PathNavigation navigation = dragonfly.m_21573_();
        if (navigation != null) {
            navigation.m_26573_();
        }
    }

    private static int readCommand(DragonflyEntity dragonfly) {
        try {
            Object tag = dragonfly.getClass().getMethod("getPersistentData").invoke(dragonfly);
            Method contains = tag.getClass().getMethod("m_128441_", String.class);
            if ((boolean) contains.invoke(tag, COMMAND_TAG)) {
                Method getInt = tag.getClass().getMethod("m_128451_", String.class);
                return (int) getInt.invoke(tag, COMMAND_TAG);
            }
        } catch (Throwable ignored) {
        }
        return dragonfly.m_21827_() ? COMMAND_SIT : COMMAND_FOLLOW;
    }

    private static void writeCommand(DragonflyEntity dragonfly, int command) {
        try {
            Object tag = dragonfly.getClass().getMethod("getPersistentData").invoke(dragonfly);
            Method putInt = tag.getClass().getMethod("m_128405_", String.class, int.class);
            putInt.invoke(tag, COMMAND_TAG, command);
        } catch (Throwable ignored) {
        }
    }
}
