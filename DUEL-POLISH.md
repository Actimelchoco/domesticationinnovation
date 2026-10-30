# Duel fixes

- Blocked player tosses return the original stack and NBT to inventory. If the inventory cannot accept everything, the remainder spawns normally instead of disappearing.
- Lifetime duel/ranked leaderboards require at least 11 recorded ranked matches for each player or tame. Regular duels do not count. The new lifetime ranked counter persists across saves and duel restoration and resets with duel stats.
- Old saves did not distinguish lifetime ranked matches from regular duels. Existing tame entries retain the verifiable ranked daily count as a lower bound; existing player entries begin at zero. Combined legacy duel totals are not treated as ranked history.
- Alex's Mobs raccoons clear their separate native sitting/washing states at duel startup. Washing and begging goals are suspended during combat and restored afterward; native melee animations remain intact.
- Legendary Monsters Skeloraptor bite, tail and shockwave attacks now add earned base damage once. Standard melee continues using its existing attack attribute. Native damage sources are preserved, allowing existing base-hit attributes and abilities to run.

Validation uses the actual installed Alex's Mobs 1.22.9 and Legendary Monsters 2.2.2 entities on Forge 47.4.16, including real Forge toss events, native damage calls, lifesteal, Battle Strength, raccoon navigation, leaderboard threshold boundaries, save migration, snapshots, and ranked versus regular result recording.

Regression command (requires local mod fixtures):

```powershell
.\gradlew.bat -I gradle/heldItemTameWeaponsRegression.init.gradle -I gradle/duelPolishRegression.init.gradle '-PvallumraptorModsDir=C:/Users/bruns/Documents/linggango server/mods' '-PtameRegressionForgeVersion=47.4.16' runGameTestServer --offline
```
