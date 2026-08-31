package com.github.alexthe668.domesticationinnovation.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class ClientDuelGlowSettings {
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("domesticationinnovation-client.properties");
    private static boolean loaded;
    private static boolean duelGlow = true;

    private ClientDuelGlowSettings() {
    }

    public static synchronized boolean duelGlow() {
        load();
        return duelGlow;
    }

    public static synchronized void setDuelGlow(boolean enabled) {
        load();
        duelGlow = enabled;
        Properties properties = new Properties();
        properties.setProperty("duelGlow", Boolean.toString(enabled));
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream output = Files.newOutputStream(FILE)) {
                properties.store(output, "Domestication Innovation client-only settings");
            }
        } catch (IOException ignored) {
        }
    }

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(FILE)) return;
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(FILE)) {
            properties.load(input);
            duelGlow = Boolean.parseBoolean(properties.getProperty("duelGlow", "true"));
        } catch (IOException ignored) {
        }
    }
}
