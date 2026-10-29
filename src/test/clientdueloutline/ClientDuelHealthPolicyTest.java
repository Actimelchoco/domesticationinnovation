import com.github.alexthe668.domesticationinnovation.client.ClientDuelHealthPolicy;
import com.github.alexthe668.domesticationinnovation.client.ClientDuelHealthPolicy.Candidate;
import com.github.alexthe668.domesticationinnovation.client.ClientDuelOutlinePolicy;
import com.github.alexthe668.domesticationinnovation.client.ClientDuelDisplayPreferences;
import java.nio.file.Files;
import java.util.List;

public final class ClientDuelHealthPolicyTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        String blue = "tldga0123456789a", red = "tldgb0123456789a", other = "tldga9876543210b";
        String battle = ClientDuelOutlinePolicy.battleKey(blue);
        var candidates = List.of(new Candidate(1, "Zulu", blue, true, true),
                new Candidate(2, "Alpha", blue, true, true), new Candidate(3, "Enemy", red, true, true),
                new Candidate(4, "Dead", blue, true, false), new Candidate(5, "Player", red, false, true),
                new Candidate(6, "Other match", other, true, true));
        check(ClientDuelHealthPolicy.select(null, candidates, 6).isEmpty(), "no match means no HUD");
        var rows = ClientDuelHealthPolicy.select(battle, candidates, 6);
        check(rows.stream().map(Candidate::entityId).toList().equals(List.of(2, 3, 1)), "only living same-match tames, sorted and interleaved");
        check(ClientDuelHealthPolicy.select(battle, candidates, 2).equals(rows.subList(0, 2)), "maximum is total across teams");
        check(ClientDuelHealthPolicy.select(battle, candidates, 1).size() == 1, "limit one works");
        check(ClientDuelHealthPolicy.select(battle, List.of(), 6).isEmpty(), "ended or empty match clears rows");
        check(ClientDuelHealthPolicy.clampMaxNames(0) == 1 && ClientDuelHealthPolicy.clampMaxNames(999) == 20, "bounds clamped");

        var file = Files.createTempDirectory("duel-health-preferences").resolve("settings.properties");
        try {
            var prefs = new ClientDuelDisplayPreferences(file);
            check(!prefs.duelHealth() && prefs.duelHealthMaxNames() == 6 && prefs.duelGlow(), "safe defaults for new installs");
            prefs.setDuelGlow(false); prefs.setDuelHealth(true); prefs.setDuelHealthMaxNames(8);
            var reloaded = new ClientDuelDisplayPreferences(file);
            check(reloaded.duelHealth() && !reloaded.duelGlow() && reloaded.duelHealthMaxNames() == 8, "health works independently of disabled glow and persists");
            reloaded.setDuelGlow(true);
            check(new ClientDuelDisplayPreferences(file).duelHealthMaxNames() == 8
                    && new ClientDuelDisplayPreferences(file).duelHealth(), "updating glow preserves health settings");
            Files.writeString(file, "duelGlow=false\nduelHealthMaxNames=invalid\nunknownKey=keep\n");
            var legacy = new ClientDuelDisplayPreferences(file);
            check(!legacy.duelHealth() && !legacy.duelGlow() && legacy.duelHealthMaxNames() == 6, "legacy file preserves glow and defaults new/invalid settings");
            legacy.setDuelHealthMaxNames(99);
            check(new ClientDuelDisplayPreferences(file).duelHealthMaxNames() == 20, "persisted limits are bounded");
            check(Files.readString(file).contains("unknownKey=keep"), "unrelated preferences survive updates");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(file.getParent()); }
        System.out.println("12 duel health selection and persistence checks passed.");
    }
}
