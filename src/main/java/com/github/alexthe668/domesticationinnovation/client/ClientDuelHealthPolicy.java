package com.github.alexthe668.domesticationinnovation.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Selection rules independent of the renderer and client world. */
public final class ClientDuelHealthPolicy {
    public static final int DEFAULT_MAX_NAMES = 6;
    public static final int MAX_NAMES = 20;
    public record Candidate(int entityId, String name, String team, boolean tame, boolean alive) { }
    private ClientDuelHealthPolicy() { }
    public static int clampMaxNames(int max) { return Math.max(1, Math.min(MAX_NAMES, max)); }

    public static List<Candidate> select(String battle, List<Candidate> candidates, int max) {
        if (battle == null) return List.of();
        var order = Comparator.comparing(Candidate::name, String.CASE_INSENSITIVE_ORDER).thenComparingInt(Candidate::entityId);
        var blue = candidates.stream().filter(c -> c.tame() && c.alive()
                && ClientDuelOutlinePolicy.teamForBattle(battle, c.team()) == 1).sorted(order).toList();
        var red = candidates.stream().filter(c -> c.tame() && c.alive()
                && ClientDuelOutlinePolicy.teamForBattle(battle, c.team()) == 2).sorted(order).toList();
        List<Candidate> result = new ArrayList<>();
        int limit = clampMaxNames(max);
        // Interleave teams so a large team cannot hide every opponent.
        for (int i = 0; result.size() < limit && (i < blue.size() || i < red.size()); i++) {
            if (i < blue.size()) result.add(blue.get(i));
            if (result.size() < limit && i < red.size()) result.add(red.get(i));
        }
        return List.copyOf(result);
    }
}
