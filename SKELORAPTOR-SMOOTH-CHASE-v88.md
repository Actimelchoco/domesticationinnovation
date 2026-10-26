# Skeloraptor continuous chase - v88

Native Skeloraptor command updates can stop navigation even when the tame is
already unsitting. The chase goal stayed active but waited for its next scheduled
path search, creating a stop-start movement pattern. An isolated real-mod duel
regression reproduced an 11-tick stationary pause after this interruption.

The shared chase goal now retains its last calculated path and resumes that
unfinished path if navigation is canceled between searches. This applies to
normal targeting, duels and ranked. Full target-path requests retain the existing
20-tick interval. Changing targets invalidates the retained path; completed paths
and paths whose next node is more than four blocks away are not resumed.
Native attack animations, movement speed, sitting, guardian rest, friendly-target
protection and leash rules retain their existing behavior.

Validation: all 14 Forge GameTests passed using the available real Legendary
Monsters 2.2.2 JAR. Tests cover native attacks, normal Monster Hunter acquisition,
duel/ranked pursuit, cleanup, movement refreshes, protection/rest/leash checks,
and moving-target continuity with and without native navigation interruptions.
The moving-target tests counted at most six target-path requests across 100
sampled ticks and confirmed reuse of calculated paths after interruptions.
The final four movement samples recorded zero stationary chase ticks outside
native attack animations. Log: build/v88-smooth-final.log.
Test structures declare the full chase-lane footprint to avoid overlap between
GameTest batches. The normal moving target starts within Monster Hunter's
10-block acquisition radius. Diagnostic fixtures and resources are not packaged.

The GameTest Gradle daemon disappeared after the explicit all-tests-passed
report during shutdown. The separate production build succeeded:
build/v88-production-build.log. ZIP integrity and absence of test fixtures were
verified. Only SkeloraptorDuelChaseGoal.class differs from v87; client classes,
network metadata, mixin configuration, Vampire settings and ranked fixes remain
unchanged. No production TPS benchmark was performed.

Replace the previous extension JAR while the server is stopped. No config changes
required. Live server/world files were not modified.

SHA-256: 3d428c67b86bb9172b68d75b75cb866c45d931796834137d959cbc78d1903035
