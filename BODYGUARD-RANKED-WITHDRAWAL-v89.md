# Bodyguard duty overrides guardian and ranked assignments - v89

/tames bodyguard add <selection> keeps the existing guardian override and now
withdraws eligible owned tames from ranked immediately. Active rounds restore
and finish before bodyguard assignment, so ranked restoration cannot overwrite
new duty. Waiting hidden tames are reconstructed beside the owner using their
saved snapshot. Suggestions include eligible active and waiting ranked tames.

Withdrawal removes both entity UUID and stable TL-ID aliases from the persisted
ranked pool, session pools, current round, deferred pulls and idle timers. The
ranked session schedules its next match. The bodyguard command reacquires restored
registry rows, including later candidates in bulk selections. Guardian anchors,
rest activity and pending transfers are cleared; named guardian deployment groups
remain saved. Guardian chest automation already respects normal bodyguards.

Ownership, bodyguard composition, living/unstored requirements, hostile Bewereager
locks and ordinary duel locks still apply. Rejected candidates are validated
before withdrawal. Ranked recall follows the existing guardian-recall round
forfeit behavior, without a normal death penalty. An exceptional snapshot rebuild
failure reports that the tame was withdrawn but could not be loaded for duty.

Validation: all 28 required Forge GameTests passed. Added cases cover active
ranked recall and session bookkeeping/next-round scheduling, hidden interface
reconstruction, bulk promotion after restoration replaces registry rows, rejection
without ranked withdrawal, and ordinary-duel protection with a stale ranked entry.
Existing guardian override, named-group retention, composition, unlimited same-type
rosters and removal tests also passed. Log: build/v89-final-validation.log.
The GameTest Gradle daemon disappeared during shutdown after the explicit pass
report; the separate production build succeeded: build/v89-production-build.log.
ZIP integrity, test-fixture exclusion, unchanged client classes, network metadata,
mixin configuration, Vampire weights and v88 chase bytecode verified. Only
TameCommands/TameDuelManager class files differ from v88. No extra ticking scans
or background loops were added; this work runs when the command is used.

Replace the previous extension JAR while the server is stopped. No config changes
required. Live server/world files were not modified. All v88 changes retained.

SHA-256: dcea0f5fe862a45e7e32ee745f1b7c2ec7379bb3dbffcd08ca8091f7f6402682
