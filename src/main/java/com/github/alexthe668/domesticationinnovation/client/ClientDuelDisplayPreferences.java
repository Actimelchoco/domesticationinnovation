package com.github.alexthe668.domesticationinnovation.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Personal settings; preserve other keys when updating an individual preference. */
public final class ClientDuelDisplayPreferences {
    private final Path file;
    private final Properties values = new Properties();
    private boolean loaded;
    public ClientDuelDisplayPreferences(Path file) { this.file = file; }
    public synchronized boolean duelGlow() { load(); return Boolean.parseBoolean(values.getProperty("duelGlow", "true")); }
    public synchronized boolean duelHealth() { load(); return Boolean.parseBoolean(values.getProperty("duelHealth", "false")); }
    public synchronized int duelHealthMaxNames() {
        load();
        try { return ClientDuelHealthPolicy.clampMaxNames(Integer.parseInt(values.getProperty("duelHealthMaxNames",
                    Integer.toString(ClientDuelHealthPolicy.DEFAULT_MAX_NAMES)))); }
        catch (NumberFormatException ignored) { return ClientDuelHealthPolicy.DEFAULT_MAX_NAMES; }
    }
    public synchronized void setDuelGlow(boolean enabled) { set("duelGlow", Boolean.toString(enabled)); }
    public synchronized void setDuelHealth(boolean enabled) { set("duelHealth", Boolean.toString(enabled)); }
    public synchronized void setDuelHealthMaxNames(int max) {
        set("duelHealthMaxNames", Integer.toString(ClientDuelHealthPolicy.clampMaxNames(max)));
    }
    private void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(file)) return;
        try (var input = Files.newInputStream(file)) { values.load(input); }
        catch (IOException ignored) { }
    }
    private void set(String key, String value) {
        load(); values.setProperty(key, value);
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (var output = Files.newOutputStream(file)) {
                values.store(output, "Domestication Innovation client-only settings");
            }
        } catch (IOException ignored) { }
    }
}
