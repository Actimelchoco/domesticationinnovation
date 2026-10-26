# Vampire and normal Animights armor saves - v86

Vampire level-up rewards now roll 30% attributes, 69% base stats and 1% abilities
(previously 10%, 89%, 1%). Setting attributes to 30% transfers 20 percentage
points from base stats while preserving the ability chance.

The v85 armor alias synchronization covered duel restoration only. Normal death
saves now synchronize Animights' redundant head/chest/legs/feet tags with native
ArmorItems before archiving. The shared respawn/recovery loader also normalizes
older snapshots on a copy, preserving the original archive. Broken armor no
longer reappears alongside its saved scrap through these restoration paths.
This applies to normal/admin respawn, recovery and duel restoration. No added
periodic scans or ticking work.

Validation: all 31 required Forge GameTests passed with real Animights and DNL,
including actual ordinary death, broken armor, respawn and later kill repair,
and stale legacy snapshot loading without mutating its source. Gradle reported
its recurring daemon shutdown failure after all tests passed. The separate
production build succeeded. ZIP integrity, excluded test fixtures, unchanged
client classes, mod metadata and mixin configuration checked against v85.
Logs: build/v86-validation.log and build/v86-production-build.log.

Replace the previous extension JAR while the server is stopped. No config changes
required. All v85 changes retained. Server/world files were not changed.

SHA-256: 417d68522c83ccbc0ae5065a6d5e0dedb5b864428a28251384bad6c7183be5f1
