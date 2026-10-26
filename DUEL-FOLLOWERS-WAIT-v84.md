# Followers wait for player duel participants — v84

When a player enters an active duel as a fighter, their nonparticipating following
tames and normal bodyguards temporarily sit and pause their AI. Guardian chest
bodyguards wait for the player they protect, including guards owned by someone
else. Their saved follow orders, modes, guardian posts and bodyguard assignments
remain unchanged. Selected tame fighters continue fighting; static guardians
and unrelated players' pets continue their normal duties. A player watching a
tame-only battle does not trigger the pause.

Waiting lasts until the battle ends or is cancelled, including when the player
is eliminated before their team finishes. The shared battle lifecycle covers
duels and ranked player fights. Existing sitting, native command and disabled-AI
states are restored, rather than forcing every pet to follow afterwards.
Nonparticipating follower abilities and automatic owner-teleport following are
paused as well. Guardian maintenance and movement synchronization cannot undo
the temporary sit or write it back as a permanent order.

There is no continuous world scan. Battle start/end visit the existing loaded
tame index once. During combat the existing tame tick uses a small player-ID set
and saved marker, with stop enforcement staggered to ten ticks. No chunks are
loaded to pause pets. Newly loaded followers pause through the same callback.
An entity-local saved marker restores unloaded followers when they later load,
and clears stale waiting states after a restart. Normal shutdown restores loaded
followers. Temporary referee copies do not inherit the source's wait marker.

The isolated Forge suite includes five follower tests plus four earlier
post-ranked-death regressions. It checks player versus tame-only battles,
native/interface followers, normal bodyguards, cross-owner guardian bodyguards,
static guardian/selected-fighter exclusions, saved assignment preservation,
movement synchronization, teleport checks, exact prior sit/NoAI restoration,
late followers, unload/reload recovery, and an 80-tick maintenance run after
the player is eliminated while their team remains in battle.
All nine passed in `build/v84-duel-followers-final-check.log`. The local Gradle
daemon disappeared during shutdown after the pass report, as in earlier runs;
the GameTest task did not exit cleanly. The separate production build is checked
independently. Production TPS with hundreds of followers was not benchmarked.

Replace the previous extension JAR while the server is stopped. No config
changes are required. Earlier Skeloraptor and Animights fixes remain included.
