Guard duty and duel/ranked calls (v57)
====================================

Assigned bodyguards and guardians ignore duel and ranked calls until their guard
assignment is released. Regular tames in a mixed selection remain eligible.

The exclusion applies to names, groups, types, all/nearby selections, pending duel
drafts, ranked enrollment, automatic matchmaking and the duel manager's final
start gate. Session movement, waiting-area holding, hiding and snapshot recovery
also skip guards. Stale ranked guard entries are removed from the pool without
recalling or hiding the tame. Ranked referees do not borrow guard templates.

Release bodyguards with `/tames bodyguard remove <selection>` (or `remove all`).
Release guardians with `/tames guardian release <selection>` or the existing
`/tames release ...` commands. They can then be selected or re-enrolled normally.

The checks use registry identity lookups and add no periodic scans. Guardian chest
automation from v56 remains included. Replace the older DI extension jar with v57;
keep only one DI jar. No configuration changes or client protocol changes needed.
