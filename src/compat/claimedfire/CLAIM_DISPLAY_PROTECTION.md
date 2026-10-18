# Claimed frame and armor-stand protection

Minecraft 1.20.1 / FTB Chunks Forge 2001.3.8. Standalone server-side addition to the existing fire/Amarok/pruning fork; no DomesticationInnovation dependency.

Armor stands, normal item frames, glow frames and subclasses using their vanilla damage methods cannot take damage inside any claim. A direct PLAYER_ATTACK from a player holding an empty main hand, a SwordItem, or a forge:tools/swords item follows normal damage/removal rules. This does not bypass FTB player permissions. Axes, other held items, creative wrong-tool attacks, arrows, mobs, TNT, fire, lightning and other environmental damage cannot break claimed displays or pop frame contents. Player-owned projectiles/explosions remain blocked even if their owner is currently holding a sword.

Frames also survive lost backing blocks, overlap/support checks, movement and pushing while claimed. Explosion entity lists omit claimed frames and armor stands to prevent blast displacement. Unclaiming takes effect immediately. The feature affects frames and stands; other entities keep their prior behavior. Explicit administrative entity removal is not intercepted.

Claim lookups use the display location (frame anchor). There are no new ticking tasks, scans or forced chunks; frame support is checked only through the existing survival check.

Install only the resulting FTB Chunks JAR on the server in place of its existing FTB Chunks JAR. No client or config changes. Existing fire, Amarok ice and pruning entries are retained byte-for-byte; the only existing entry changed is the manifest to register the additional mixin config. New classes and refmap/config files are added separately.

Build: copy the existing modified input JAR as ftb-chunks-forge-2001.3.8.jar into a build input directory with FTB Library 2001.2.13 and Teams 2001.3.2. Run gradlew -I gradle/claimedFireProtection.init.gradle -PclaimedFireModsDir=<input-dir> finishClaimedFireProtectionJar.

Regression: gradlew -I gradle/claimDisplayRegression.init.gradle -PclaimedFireModsDir=<FTB-dependencies-dir> -PtameRegressionForgeVersion=47.4.16 runGameTestServer. Uses the built replacement plus real FTB dependencies in an isolated world; tests non-melee/creative wrong-tool immunity, hand/sword removal, contents, lost support, motion, explosion lists, unclaim behavior, existing fire protection and pruning command checks.
