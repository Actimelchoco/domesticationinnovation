# Animights worn armor break protection — v82

Armor worn in an Animight's four armor slots cannot disappear through the normal
durability break path. After the real durability/Unbreaking calculation reports
a break, it becomes one Dungeon Now Loading scrap in the equipment reserve.
Break Protection is not required. The original armor, count, enchantments and
custom NBT are stored in the scrap's `Original` compound. The v81 kill recovery
restores it fully repaired.

When all 54 reserve slots are occupied, the scrap remains safely in the equipment
slot instead. It provides no armor, cannot take durability damage and is neither
dropped nor deleted by periodic equipment management. It moves into reserve when
space opens, or can be restored directly in place by the next eligible kill.
No other item is deleted to make room. Without the optional Dungeon Now Loading
mod, the original is preserved at one durability instead.

Normal early equipment swaps and XP repairs remain. Weapons and other species
retain their previous break behavior. Existing duel snapshot/kill-management
rules remain. This adds a check at the actual break event and uses the existing
equipment maintenance checks; it adds no world searches or per-tick inventory scan.

The Forge integration suite uses real Animights 1.1.0, Dungeon Now Loading 2.2,
GeckoLib 4.8.4 and KnightLib 1.6.1. New coverage checks all four armor slots,
conversion without Break Protection, original NBT, later kill restoration,
unchanged other species, and full-inventory retention without duplicate DNL
conversion. Earlier equipment and XP-repair regressions remain included.
All 16 tests passed in `build/v82-armor-scraps-final-validation.log`. The Gradle
daemon disappeared during shutdown after the explicit pass report, as in prior
local runs, so the GameTest task did not exit cleanly. The separate production
build is verified independently.

Replace the previous extension JAR while the server is stopped. No configuration
change is required.
