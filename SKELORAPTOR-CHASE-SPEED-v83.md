# Skeloraptor chase speed — v83

The v79/v80 chase fallback requested navigation at multiplier 1.0. Legendary
Monsters 2.2.2 gives Skeloraptor a base movement attribute of about 0.1 and uses
multiplier 4.0 in its native FollowOwnerGoal (3.0 in its main random stroll).
Consequently the fallback could chase much more slowly than the species' native
movement.

The shared chase implementation now requests 4.0, matching the native follow
speed. This applies to normal combat, duels and ranked. It does not change saved
or live base speed attributes, add speed modifiers, or toggle sprinting; existing
speed bonuses still apply through native navigation. Attack animations and
target selection are unchanged. Path requests remain capped at once per 20
server ticks, and all existing sit/rest/friendship/leash checks remain.

Real Forge GameTests with Legendary Monsters 2.2.2 reproduce the old speed issue:
`build/v83-chase-speed-before.log` failed the shortened ranked and normal combat
chase tests. The ranked sample moved about 0.4 blocks per 20 ticks while its
movement attribute remained 0.1. With the fix, distant duel/ranked tests must approach and hit within 240 ticks
instead of the old 800-tick allowance. Normal Monster Hunter must reach attack
range within 160 ticks; its existing 800-tick native-attack check is retained
separately, since approach speed and attack animation timing are different.
Duel tests also verify unchanged base speed attributes.
The suite retains native damage, ally protection, navigation continuity,
temporary duel-goal cleanup, and normal sitting/rest/boundary restrictions.
All ten passed in `build/v83-chase-speed-final-validation.log`. The Gradle
daemon disappeared during shutdown after the pass report, as in prior local
runs; the GameTest task did not exit cleanly. The separate production build is
verified independently.

The IDE had no runnable debug configuration for the GameTest source, so runtime
evidence comes from the actual Forge test server. Legendary Monsters 2.2.3 is
not available locally and remains unverified. No exact km/h or production TPS
claim is made.

Replace the prior extension JAR while the server is stopped. No configuration
changes are required. Earlier Animights armor/XP changes are retained.
