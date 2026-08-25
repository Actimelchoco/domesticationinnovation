package com.github.alexthe668.domesticationinnovation.server.tameslevel.tame;

public enum TameMode {
    DEFAULT(0),
    BOSS(1),
    BODYGUARD(2),
    MONSTER_HUNTER(3),
    ARENA(4),
    PASSIVE(6),
    AGGRESSIVE(8),
    DEFAULT_PLUS(9),
    ASSASIN(10);

    private final int id;

    TameMode(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static TameMode byId(int id) {
        return switch (id) {
            case 0 -> DEFAULT;
            case 1 -> BOSS;
            case 2 -> BODYGUARD;
            case 3 -> MONSTER_HUNTER;
            case 4 -> ARENA;
            case 6 -> PASSIVE;
            case 8 -> AGGRESSIVE;
            case 9 -> DEFAULT_PLUS;
            case 10 -> ASSASIN;
            // legacy removed modes map to default_plus
            case 5, 7 -> DEFAULT_PLUS;
            default -> DEFAULT;
        };
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static TameMode byName(String name) {
        TameMode mode = tryByName(name);
        return mode == null ? DEFAULT : mode;
    }

    public static TameMode tryByName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim().toLowerCase(java.util.Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (normalized.equals("agressive")) {
            return AGGRESSIVE;
        }
        if (normalized.equals("default")
                || normalized.equals("vanilla")) {
            return DEFAULT;
        }
        if (normalized.equals("default_plus")
                || normalized.equals("defaultplus")
                || normalized.equals("modded_default")
                || normalized.equals("dungeon")
                || normalized.equals("travel")
                || normalized.equals("distant_bodyguard")) {
            return DEFAULT_PLUS;
        }
        String compact = normalized.replace("_", "");
        for (TameMode mode : values()) {
            String key = mode.key();
            if (key.equals(normalized) || key.replace("_", "").equals(compact)) {
                return mode;
            }
        }
        return null;
    }
}
