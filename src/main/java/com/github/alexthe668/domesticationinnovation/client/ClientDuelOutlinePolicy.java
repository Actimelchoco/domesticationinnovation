package com.github.alexthe668.domesticationinnovation.client;

/** Pure matching rules shared by client glow visibility and color selection. */
public final class ClientDuelOutlinePolicy {
    private ClientDuelOutlinePolicy() {}
    public static String battleKey(String team) {
        return team != null && team.length() == 16 && (team.startsWith("tldga") || team.startsWith("tldgb"))
                ? team.substring(5) : null;
    }
    public static int teamForBattle(String battle, String team) {
        if (battle == null || !battle.equals(battleKey(team))) return 0;
        return team.charAt(4) == 'a' ? 1 : 2;
    }
    public static int color(int team) { return team == 1 ? 0x3F76E4 : 0xE53935; }
}
