# Duel selection and Animights equipment — v85

The `/tames duel` selection menu now shows selection, add, addNearby, invite,
cancel and the actions currently needed. `remove` appears only when something
is selected, `addMyself` disappears once selected, and `start` disappears while
that player's draft is ready. Own edits reset readiness and refresh the command
tree; opponent edits retain the player's confirmation. `selection` shows the
rosters during selection. During battle the menu contains only `ff` and `info`.
The duplicate selection-phase `info` alias and bare root tame-selection argument
were removed, so tame suggestions appear under `add`/`remove` instead of flooding
the root command list.

Animights now manage equipment during duels and can use gear down to its actual
breaking point instead of retiring it at the normal ten-percent safety margin.
Armor takes vanilla-rate durability wear from armor-absorbed damage. Successful
native melee attacks wear the held weapon (one durability for swords, two for
other supported melee weapons), and native bow shots wear the bow by one.
Native Unbreaking and explicit unbreakable-item behavior are respected.

Broken worn armor remains protected as before. Dueling weapons also become DNL
scraps instead of disappearing. Enchantments and custom NBT survive. Full
reserves retain pending scraps in their equipment slot, including the offhand,
until space opens or a kill repairs them. Duel kills can repair/unseal originals
and re-equip them; XP repairs continue without spending the tame's XP. Equipment
management does not enable looting other duel participants or opening the
equipment GUI during a fight. Optional worn-armor disposal stays off in duels.

Duel restoration retains the current equipped items and reserve together,
preventing old gear from returning alongside its scrap. Animights stores armor
both in vanilla `ArmorItems` and separate head/chest/legs/feet tags; restoration
now synchronizes both representations, including empty slots after breakage.
Death-restoration tests reproduced the stale helmet before this fix.

Validation: all 29 Forge GameTests passed with Animights 1.1.0, Dungeon Now Loading
2.2, GeckoLib 4.8.4 and KnightLib 1.6.1. This includes existing selection/readiness,
nearby-range, battle-info and follower-pause tests, plus actual melee/bow wear,
duel kill repair, full inventories, and restoration after both cancellation and
death without duplicate originals. L2 sealed-item coverage uses the existing
registry/NBT fixture. Log: `build/v85-selection-equipment-complete-validation.log`.
The local GameTest Gradle daemon disappeared during shutdown after the explicit
pass report, as in prior runs; the separate production build is checked
independently. No production TPS benchmark was performed. Wear is event-driven;
equipment swaps use the existing bounded reserve maintenance.

Replace the previous extension JAR while the server is stopped. No configuration
changes are required. Earlier guardian, Animights and Skeloraptor fixes remain.
