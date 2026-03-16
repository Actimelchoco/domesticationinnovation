package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TameClass {

    private static final Map<String, TameClass> REGISTRY = new LinkedHashMap<>();

    public static final TameClass TANKER = registerBuiltin("tanker");
    public static final TameClass DPS = registerBuiltin("dps");
    public static final TameClass ASSASSIN = registerBuiltin("assassin");
    public static final TameClass SUPPORTER = registerBuiltin("supporter");
    public static final TameClass MAGE = registerBuiltin("mage");
    public static final TameClass SHOOTER = registerBuiltin("shooter");
    public static final TameClass MANIAC = registerBuiltin("maniac");
    public static final TameClass ATTRIBUTER = registerBuiltin("attributer");

    private final String id;

    private TameClass(String id) {
        this.id = id;
    }

    private static TameClass registerBuiltin(String id) {
        TameClass tameClass = new TameClass(id);
        REGISTRY.put(id, tameClass);
        return tameClass;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("protector".equals(normalized)) {
            return "supporter";
        }
        return normalized;
    }

    public static TameClass parse(String raw) {
        String normalized = normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }
        return REGISTRY.get(normalized);
    }

    public static TameClass ensureRegistered(String raw) {
        String normalized = normalize(raw);
        if (normalized.isBlank()) {
            return null;
        }
        TameClass existing = REGISTRY.get(normalized);
        if (existing != null) {
            return existing;
        }
        TameClass created = new TameClass(normalized);
        REGISTRY.put(normalized, created);
        return created;
    }

    public static TameClass[] values() {
        return REGISTRY.values().toArray(new TameClass[0]);
    }

    public String id() {
        return id;
    }

    public String name() {
        return id.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return name();
    }
}
