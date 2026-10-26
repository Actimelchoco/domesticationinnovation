# Ranked death and interrupted-round recovery - v87

A duplicate LivingDeathEvent from a removed duel body could be handled as an
ordinary death after its replacement had already been restored. That incorrectly
marked the replacement's registry entry dead, applied normal penalties and sent
a normal death notification. The ranked validity cleanup could then pull it.
Deaths now track handled bodies using weak references, separate from UUIDs reused
by restored tames. Duplicate events from those old bodies cannot alter the new
body's registry row or award additional XP. The replacement can still die normally.

A mod clearing its native tame flag at death could prevent elimination attribution.
Active duel membership by entity UUID or stable TL identity now takes precedence.

Removing the final fighter on one side could leave a session with an empty side
and its next-round timer disabled. Existing session processing now completes any
interrupted round, clears both sides and schedules the next round automatically.
This covers ranked and regular duel sessions. Idle ticks preserve the timer.
Existing eligibility, food and owner-online rules still apply.

The screenshot alone cannot identify which mod triggered an extra death event.
All three failure paths were reproduced in isolated Forge tests before fixing.
The regression checks include a real ranked death and restoration, a repeated
old-body death event, a later genuine ordinary death, clearing native tame state,
and removing a last fighter followed by an actual new round without a pool edit.

Validation: all 38 required Forge GameTests passed, including the existing duel,
follower, Animights and normal armor-save tests. Log: build/v87-complete-validation.log.
The GameTest Gradle daemon disappeared during shutdown after the explicit pass
report; the separate production build succeeded (build/v87-production-build.log).
ZIP integrity, test-fixture exclusion, unchanged client classes, mod metadata,
mixin configuration and v86 Vampire weights verified against the v86 release.
No production TPS benchmark performed. No additional background scans: death-only
weak-reference bookkeeping and existing session processing perform the new work.

Replace the previous extension JAR while the server is stopped. No config changes
required. All v86 changes retained. Live server and world files were not changed.

SHA-256: c03b24382dfda941c7e8c3d3aae25916a8706052ec5ffc95f691a3dcb948ce12
