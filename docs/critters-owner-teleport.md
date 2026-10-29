# Critters owner-follow teleport fix

Critters and Companions 2.7.1 roly polies and snails can use native owner-follow
teleports independently of the extension's tethered-teleport attribute. The old
follow guard blocked active duels but did not block the ranked waiting pool or a
new guardian assignment before the periodic goal-removal pass. The snail also
overrides the native follow tick, bypassing a guard applied only to the base tick.

Native owner following now checks the tame's assignment when starting, continuing,
ticking, and teleporting. Guardian/home, sit/wander, resting, paused followers, and
ranked/session locks prevent owner-follow movement. The shared teleport method
and destination check also enforce the assignment, covering the snail's override.
Ordinary following resumes when the assignment or ranked lock is cleared.
Guardian return-to-post and deliberate command teleports retain their own rules.

## Slow combat chase

Owned roly polies and snails also get a slow navigation goal for their current
combat target. It uses their naturally slow walking pace (1.0 navigation multiplier), checks its path every
second, and stops within 1.5 blocks. Existing paths to stationary targets are kept
so the slow snail can finish each step. It adds no attacks, damage, teleporting,
or target selection. Higher-priority floating, chest access, and native panic
behavior remain available. Sitting, guardian rest, hunger, ranked waiting,
friendly-target rules, and protection-zone boundaries are respected.

Ranked refreshes no longer repeat unchanged sit-state setters: the snail's native
setter would restart its 26-tick immobile wake-up animation every time. Genuine
sit/stand changes still use that native animation.

Regression command using the installed Critters and Companions 2.7.1 dependencies:

```powershell
.\gradlew.bat -I gradle/crittersFollowRegression.init.gradle '-PtameRegressionForgeVersion=47.4.16' runGameTestServer --offline
```

The tests exercise both species in guardian mode, the ranked waiting pool, and an
active ranked round, including an already-started native follow tick. They also
verify normal owner teleporting resumes afterward. The Ultimine regressions run
in the same suite.
