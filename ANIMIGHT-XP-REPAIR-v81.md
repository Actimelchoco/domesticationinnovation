# Animights XP equipment repair — v81

Animights now repair each damaged armor piece and weapon by two durability per XP
awarded. Each item receives the whole allowance independently. The tame keeps
the full XP award; Mending enchantments are not required. Equipped armor, both
hands and the 54 reserve slots are included. Full durability caps repair, and
zero/negative XP cannot repair equipment.

The existing combat/assist, command XP and duel/ranked reward paths use this
behavior, including saved native equipment for unloaded ranked participants.
Fixed activity and daily-care XP also repair Animights gear without spending XP.
XP scaling and level-up behavior are unchanged. Other species retain their
existing Mending behavior.

The existing kill recovery now also recognizes Dungeon Now Loading item scraps
(`Original` NBT), alongside L2 sealed items (`sealedItem` NBT). Valid originals
return fully repaired with item count, enchantments and custom NBT preserved.
Equipped tokens restore directly in their slots, including when reserves are
full. Newly collected recovery drops also restore. Empty/malformed originals
and nested recovery wrappers remain intact. Normal gear only receives XP-based
repair, rather than an unconditional full repair on kills.
The existing duel restriction on kill-looting/equipment management remains.

This is event-driven: XP and kills examine at most the six equipment slots and
54 reserve slots, without adding world searches, chunk loading or a periodic
inventory scan. Production TPS with hundreds of tames was not benchmarked.

Validation: all 14 Forge GameTests passed with real Animights 1.1.0, Dungeon Now
Loading 2.2, GeckoLib 4.8.4 and KnightLib 1.6.1. L2 uses the existing registry/NBT
fixture rather than the real L2 mod. Coverage includes independent XP budgets,
unenchanted armor and weapons, full XP retained through LevelSystem.grantXP,
negative XP, repair caps/overflow, unloaded snapshot repair, kill-only recovery,
custom NBT, full reserves, malformed tokens, repeat recovery and the previous
equipment/collar balance regressions. Log:
`build/v81-animight-repair-final-validation.log`. The Gradle GameTest daemon
disappeared during shutdown after the explicit pass report; the GameTest task
did not exit cleanly. The separate production build is checked independently.

Replace the prior extension JAR while the server is stopped. No config change
is required.
