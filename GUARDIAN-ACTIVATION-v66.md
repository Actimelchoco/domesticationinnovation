# Guardian activation v66

With the owner offline, guardians activate for a living non-spectator player
within 20 blocks of the saved guardian post. While any member of the owner's
FTB team is online, this radius becomes 100 blocks. Distances include height and
require the nearby player to be in the post's dimension. The online teammate
can be elsewhere; another nearby player can trigger activation. Without FTB
Teams, the ordinary radius continues to work.

Keep the existing `<owner>:<deployment group>` drum and tame papers in a valid
guardian chest. Add another drum named `protect:<playername>`. While that player
is online, that powered chest's linked guardians remain active without a nearby
player. Several protect drums are allowed; any named online player suffices.
Removing the protect drum or tame paper, invalidating/unloading the controller,
or turning its power off removes the override. The chest must remain loaded,
as with its existing redstone automation. Hunger, explicit release, bodyguard
exclusion and morning return rules remain in effect.

All assigned guardians use monster_hunter mode. New deployments set it
immediately; existing assignments are corrected by maintenance, including
interface tames. Released tames can change mode again.

Player/team snapshots refresh once per second. Team lookup results are shared
per owner. Proximity checks cache for two seconds per guardian and use spatial
cells (up to 9x9 at the expanded radius). Protect checks cache once per second
per chest, using a reverse tame-to-controller index without inventory scans in
the guardian activity loop. Existing transition, work and chunk-load budgets
are unchanged. Activity increases AI work when more guardians are awake; this
does not guarantee negligible TPS impact for hundreds of fighting tames.

Regression command:

```
.\gradlew.bat -I gradle/sharedGuardianRegression.init.gradle -I gradle/guardianActivationRegression.init.gradle -PtameRegressionForgeVersion=47.4.16 runGameTestServer
```

Production build (without test init scripts):

```
.\gradlew.bat cleanCompileJava cleanProcessResources build
```

Replace the current DI extension JAR with v66. No configuration changes or
client protocol changes are required.
