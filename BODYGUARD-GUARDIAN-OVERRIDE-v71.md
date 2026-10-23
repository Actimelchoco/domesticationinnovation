# Bodyguard add overrides current guarding

`/tames bodyguard add <selection>` can now replace a selected tame's current
guardian assignment, including sleeping guardians and guardian-chest escorts.
Successful additions become normal roster bodyguards and follow their owner.
Named guardian deployment groups remain saved for later reuse.

Bodyguard composition is checked before guardian duty is cleared: the roster
must still be all one entity type or all unique types. Rejected, dead, stored,
duel-locked or hostile Bewereager tames retain their current assignment. Tab
completion includes guardians that can be added and excludes incompatible or
locked tames. Name, group, type and bulk selection retain their existing rules.

Guardian chests cannot take normal bodyguards back through redstone; their
existing bodyguard exclusion still applies. This command does not alter the
separate three-per-player limit for guardian-chest bodyguards.

Replace the prior DI extension JAR with v71. No config changes are needed.
