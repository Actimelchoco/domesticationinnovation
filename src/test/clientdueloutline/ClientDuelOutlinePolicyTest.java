import com.github.alexthe668.domesticationinnovation.client.ClientDuelOutlinePolicy;

public final class ClientDuelOutlinePolicyTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        String blue = "tldga0123456789a", red = "tldgb0123456789a", other = "tldga9876543210b";
        String battle = ClientDuelOutlinePolicy.battleKey(blue);
        check(battle.equals(ClientDuelOutlinePolicy.battleKey(red)), "both teams share the same match");
        check(ClientDuelOutlinePolicy.teamForBattle(battle, blue) == 1, "blue team");
        check(ClientDuelOutlinePolicy.teamForBattle(battle, red) == 2, "red team");
        check(ClientDuelOutlinePolicy.teamForBattle(battle, other) == 0, "other matches stay unoutlined");
        check(ClientDuelOutlinePolicy.teamForBattle(null, blue) == 0, "uninvolved viewers and ended matches have no glow");
        for (String name : new String[]{null, "", "ordinary_team", "tldga", "tldgx0123456789a", "tldga0123456789aa"}) {
            check(ClientDuelOutlinePolicy.battleKey(name) == null, "ignore malformed or unrelated team: " + name);
            check(ClientDuelOutlinePolicy.teamForBattle(battle, name) == 0, "unrelated team remains unaffected");
        }
        check(ClientDuelOutlinePolicy.color(1) == 0x3F76E4, "consistent blue");
        check(ClientDuelOutlinePolicy.color(2) == 0xE53935, "consistent red");
        System.out.println("19 client duel outline policy checks passed.");
    }
}
