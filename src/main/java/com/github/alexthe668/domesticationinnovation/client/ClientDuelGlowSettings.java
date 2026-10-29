package com.github.alexthe668.domesticationinnovation.client;

import net.minecraftforge.fml.loading.FMLPaths;

public final class ClientDuelGlowSettings {
    private static final ClientDuelDisplayPreferences SETTINGS = new ClientDuelDisplayPreferences(
            FMLPaths.CONFIGDIR.get().resolve("domesticationinnovation-client.properties"));

    private ClientDuelGlowSettings() {
    }

    public static synchronized boolean duelGlow() {
        return SETTINGS.duelGlow();
    }

    public static synchronized void setDuelGlow(boolean enabled) {
        SETTINGS.setDuelGlow(enabled);
    }
    public static boolean duelHealth() { return SETTINGS.duelHealth(); }
    public static void setDuelHealth(boolean enabled) { SETTINGS.setDuelHealth(enabled); }
    public static int duelHealthMaxNames() { return SETTINGS.duelHealthMaxNames(); }
    public static void setDuelHealthMaxNames(int max) { SETTINGS.setDuelHealthMaxNames(max); }
}
