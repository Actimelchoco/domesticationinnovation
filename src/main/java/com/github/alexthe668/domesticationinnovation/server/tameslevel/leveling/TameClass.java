package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TameClass {

    private static final Map<String, TameClass> REGISTRY = new LinkedHashMap<>();

    public enum Rarity {
        COMMON(10.0D),
        RARE(6.0D),
        EPIC(3.0D),
        LEGENDARY(1.0D);

        private final double weight;

        Rarity(double weight) {
            this.weight = weight;
        }

        public double weight() {
            return weight;
        }
    }

    public static final TameClass TANKER = registerBuiltin("tanker", Rarity.COMMON);
    public static final TameClass DPS = registerBuiltin("dps", Rarity.COMMON);
    public static final TameClass ASSASSIN = registerBuiltin("assassin", Rarity.COMMON);
    public static final TameClass SUPPORTER = registerBuiltin("supporter", Rarity.COMMON);
    public static final TameClass MAGE = registerBuiltin("mage", Rarity.COMMON);
    public static final TameClass SHOOTER = registerBuiltin("shooter", Rarity.COMMON);
    public static final TameClass MANIAC = registerBuiltin("maniac", Rarity.COMMON);
    public static final TameClass ATTRIBUTER = registerBuiltin("attributer", Rarity.COMMON);
    public static final TameClass SPONGE = registerBuiltin("sponge", Rarity.COMMON);
    public static final TameClass ORDINARY = registerBuiltin("ordinary", Rarity.COMMON);
    public static final TameClass UNSTABLE_GOD = registerBuiltin("unstable_god", Rarity.EPIC);
    public static final TameClass STRIKER = registerBuiltin("striker", Rarity.EPIC);
    public static final TameClass GANDALF = registerBuiltin("gandalf", Rarity.EPIC);
    public static final TameClass VORGOTTENLUNCHBOX = registerBuiltin("vorgottenlunchbox", Rarity.EPIC);

    private final String id;
    private final Rarity rarity;

    private TameClass(String id, Rarity rarity) {
        this.id = id;
        this.rarity = rarity;
    }

    private static TameClass registerBuiltin(String id, Rarity rarity) {
        TameClass tameClass = new TameClass(id, rarity);
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
        TameClass created = new TameClass(normalized, Rarity.COMMON);
        REGISTRY.put(normalized, created);
        return created;
    }

    public static TameClass[] values() {
        return REGISTRY.values().toArray(new TameClass[0]);
    }

    public String id() {
        return id;
    }

    public Rarity rarity() {
        return rarity;
    }

    public String name() {
        return id.toUpperCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return name();
    }
}
