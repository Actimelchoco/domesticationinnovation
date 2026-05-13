package com.github.alexthe668.domesticationinnovation.server.tameslevel.leveling;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class TameClass {

    private static final Map<String, TameClass> REGISTRY = new LinkedHashMap<>();

    public enum Rarity {
        COMMON(70.0D),
        RARE(20.0D),
        EPIC(9.0D),
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
    public static final TameClass NONE = registerBuiltin("none", Rarity.COMMON);
    public static final TameClass ORDINARY = registerBuiltin("ordinary", Rarity.COMMON);
    public static final TameClass DPS = registerBuiltin("dps", Rarity.COMMON);

    public static final TameClass SUPPORTER = registerBuiltin("supporter", Rarity.RARE);
    public static final TameClass KNIGHT = registerBuiltin("knight", Rarity.RARE);
    public static final TameClass DEFUSER = registerBuiltin("defuser", Rarity.RARE);

    public static final TameClass DESMOND_DOSS = registerBuiltin("desmond_doss", Rarity.EPIC);
    public static final TameClass THOR = registerBuiltin("thor", Rarity.EPIC);
    public static final TameClass ASSASSIN = registerBuiltin("assassin", Rarity.EPIC);
    public static final TameClass ATTRIBUTER = registerBuiltin("attributer", Rarity.EPIC);
    public static final TameClass BEE = registerBuiltin("bee", Rarity.EPIC);
    public static final TameClass BLAZE = registerBuiltin("blaze", Rarity.EPIC);
    public static final TameClass CRACKHEAD = registerBuiltin("crackhead", Rarity.EPIC);
    public static final TameClass CREEPER = registerBuiltin("creeper", Rarity.EPIC);
    public static final TameClass DISCO = registerBuiltin("disco", Rarity.EPIC);
    public static final TameClass DOLPHIN = registerBuiltin("dolphin", Rarity.EPIC);
    public static final TameClass DROWNED = registerBuiltin("drowned", Rarity.EPIC);
    public static final TameClass ENDERMAN = registerBuiltin("enderman", Rarity.EPIC);
    public static final TameClass EVOKER = registerBuiltin("evoker", Rarity.EPIC);
    public static final TameClass FISHER = registerBuiltin("fisher", Rarity.EPIC);
    public static final TameClass FROST = registerBuiltin("frost", Rarity.EPIC);
    public static final TameClass GHAST = registerBuiltin("ghast", Rarity.EPIC);
    public static final TameClass GOOFYGIRL = registerBuiltin("goofygirl", Rarity.EPIC);
    public static final TameClass GUARDIAN = registerBuiltin("guardian", Rarity.EPIC);
    public static final TameClass HEALER = registerBuiltin("healer", Rarity.EPIC);
    public static final TameClass IRON_GOLEM = registerBuiltin("iron_golem", Rarity.EPIC);
    public static final TameClass MAGE = registerBuiltin("mage", Rarity.EPIC);
    public static final TameClass MANIAC = registerBuiltin("maniac", Rarity.EPIC);
    public static final TameClass PHANTOM = registerBuiltin("phantom", Rarity.EPIC);
    public static final TameClass PIGLIN = registerBuiltin("piglin", Rarity.EPIC);
    public static final TameClass PROTECTEDBITCH = registerBuiltin("protectedbitch", Rarity.EPIC);
    public static final TameClass SHOOTER = registerBuiltin("shooter", Rarity.EPIC);
    public static final TameClass SHULKER = registerBuiltin("shulker", Rarity.EPIC);
    public static final TameClass SKELETON = registerBuiltin("skeleton", Rarity.EPIC);
    public static final TameClass SPEEDSTER = registerBuiltin("speedster", Rarity.EPIC);
    public static final TameClass SPIDER = registerBuiltin("spider", Rarity.EPIC);
    public static final TameClass SPONGE = registerBuiltin("sponge", Rarity.EPIC);
    public static final TameClass WARDEN = registerBuiltin("warden", Rarity.EPIC);
    public static final TameClass WITCH = registerBuiltin("witch", Rarity.EPIC);
    public static final TameClass WITHER = registerBuiltin("wither", Rarity.EPIC);
    public static final TameClass ZOMBIE = registerBuiltin("zombie", Rarity.EPIC);

    public static final TameClass GANDALF = registerBuiltin("gandalf", Rarity.LEGENDARY);
    public static final TameClass STRIKER = registerBuiltin("striker", Rarity.LEGENDARY);
    public static final TameClass UNSTABLE_GOD = registerBuiltin("unstable_god", Rarity.LEGENDARY);
    public static final TameClass VORGOTTENLUNCHBOX = registerBuiltin("vorgottenlunchbox", Rarity.LEGENDARY);

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
