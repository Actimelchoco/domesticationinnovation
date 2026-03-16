# Classes Documentation

This document records the intended class weighting model based on the real old `ClassesDocu.md`.

Runtime source of truth:
- [class_weights.json](/C:/Users/bruns/Documents/Workspace/DomesticationInnovation-main/src/main/resources/data/domesticationinnovation/tameslevel/class_weights.json)
- `LevelSystem` now loads class category/base-stat/attribute/ability weights from that JSON at startup.
- If you want weight changes to go live in code, edit the JSON file first and treat this markdown as descriptive documentation.

It is the design source of truth for class-based reward weights that should be implemented for:
- reward category chances
- base-stat weights
- attribute weights
- ability weights

## General
- Available classes:
  - `TANKER`
  - `DPS`
  - `ASSASSIN`
  - `SUPPORTER`
  - `MAGE`
  - `SHOOTER`
  - `MANIAC`
  - `ATTRIBUTER`
- DI naming note:
  - old `PROTECTOR` is now treated as a legacy alias for `SUPPORTER`
- Legacy baseline:
  - `base 93%`
  - `attribute 5%`
  - `ability 2%`
- Guaranteed milestone rules:
  - every level divisible by `30`: guaranteed `ability`
  - every level divisible by `20`: guaranteed `attribute`
  - if both apply, `ability` wins

## Weight Tiers
- `high chance` = about `x4.0`
- `mid chance` = about `x2.5`
- `neutral` = `x1.0`
- `low chance` = about `x0.5`

## Global Preferred Multipliers
- Current implementation applies an extra shared boost on top of class-specific preferred weights:
  - preferred attribute weights: `x30.0`
  - preferred ability weights: `x9.0`
- This extra multiplier is only applied when a class-specific weight is already above `x1.0`.
- Neutral and disfavored entries are not boosted by this global pass.

## Class Entries

### `DPS`
- category chances:
  - `base 96.0%`
  - `attribute 3.0%`
  - `ability 1.0%`
- base-stat weights:
  - `Damage x3.0`
- attribute weights:
  - `strength x4.0`
  - `killer x3.4`
  - `firefang x3.0`
  - `poison_fang x2.6`
  - `frost_fang x2.6`
  - `witherfang x2.6`
  - `lightningfang x2.6`
  - `sweeping_edge x4.0`
  - `chain_lightning x1.5`
  - `smite x2.5`
  - `bane_of_arthropods x2.5`
  - `pierce x4.0`
- ability weights:
  - `bloodlust x4.2`
  - `berserker x3.9`
  - `lightning_strike x1.5`

### `ASSASSIN`
- category chances:
  - `base 90%`
  - `attribute 10%`
  - `ability 10%`
- base-stat weights:
  - `Speed x3.8`
- attribute weights:
  - `speed x3.2`
  - `strength x1.4`
  - `killer x4.0`
  - `lifesteal x3.0`
  - `firefang x2.6`
  - `poison_fang x4.0`
  - `frost_fang x2.6`
  - `witherfang x3.0`
  - `lightningfang x1.6`
  - `chain_lightning x4.0`
  - `magnetic x4.0`
  - `bubbling x2.5`
  - `amphibious x4.0`
  - `void_cloud x2.5`
  - `warping_bite x4.0`
  - `muffled x1.5`
  - `rejuvenation x4.0`
  - `killexploder x2.5`
  - `victim_siphon x4.0`
  - `pierce x2.5`
- ability weights:
  - `shulker_bullet x2.0`
  - `wither_skull x2.4`
  - `bloodlust x3.6`
  - `berserker x3.2`
  - `ender_pearl_jump x4.2`
  - `warden_scream x3.8`
  - `dash x4.0`
  - `retaliation_slow x4.0`

### `TANKER`
- category chances:
  - `base 90%`
  - `attribute 10%`
  - `ability 10%`
- base-stat weights:
  - `HP x3.0`
  - `Armor x3.0`
  - `Armor Toughness x3.0`
- attribute weights:
  - `resistance x4.6`
  - `regeneration x4.2`
  - `fire_resistance x3.8`
  - `poison_resistance x3.8`
  - `totem x3.6`
  - `pacifist x2.8`
  - `health_siphon x2.5`
  - `blazing_protection x4.0`
- ability weights:
  - `defensive_aura x4.8`
  - `immunity_frame x4.0`
  - `deflection x4.0`
  - `defusal x7.0`
  - `guardian_repulse x4.0`
  - `shield_block x4.0`
  - `guardian_intercept x4.0`
  - `emergency_shield x4.0`
  - `body_block x4.0`
  - `psychic_wall x4.0`
  - `last_stand_fury x4.0`

### `SUPPORTER`
- category chances:
  - `base 85%`
  - `attribute 10%`
  - `ability 15%`
- base-stat weights:
  - `Speed x1.8`
  - `Knockback Resistance x2.8`
- attribute weights:
  - `resistance x4.2`
  - `regeneration x4.0`
  - `fire_resistance x3.6`
  - `poison_resistance x3.6`
  - `totem x4.2`
  - `linked_inventory x2.5`
  - `herding x2.5`
  - `charisma x2.5`
  - `ore_scenting x2.5`
- ability weights:
  - `defensive_aura x4.6`
  - `battle_strength x3.2`
  - `healing_aura x10.0`
  - `healing_bottle x6.0`
  - `shield_block x2.5`
  - `battlefield_medic x4.0`
  - `cleanse_touch x4.0`
  - `life_gift x4.0`

### `MAGE`
- category chances:
  - `base 85%`
  - `attribute 10%`
  - `ability 15%`
- attribute weights:
  - `ability_power x6.2`
  - `lightningfang x3.2`
  - `witherfang x2.0`
  - `firefang x2.0`
  - `positive_effect_steal x2.6`
  - `chain_lightning x2.5`
  - `rejuvenation x2.5`
- ability weights:
  - `dragon_fireball x4.6`
  - `guardian_beam x4.4`
  - `elder_guardian_beam x4.2`
  - `wither_skull x4.0`
  - `evoker_fangs x3.6`
  - `lightning_strike x3.6`
  - `ghast_fireball x3.2`
  - `shulker_bullet x4.0`
  - `warden_scream x3.8`
  - `shadow_hands x4.0`

### `SHOOTER`
- category chances:
  - `base 85%`
  - `attribute 10%`
  - `ability 15%`
- base-stat weights:
  - `Speed x1.8`
  - `Knockback x2.8`
- attribute weights:
  - `speed x3.4`
  - `strength x4.0`
  - `killer x3.4`
  - `firefang x3.0`
  - `poison_fang x2.6`
  - `frost_fang x2.6`
  - `witherfang x2.6`
  - `lightningfang x2.6`
  - `sweeping_edge x4.0`
  - `chain_lightning x1.5`
  - `smite x2.5`
  - `bane_of_arthropods x2.5`
  - `pierce x4.0`
- ability weights:
  - `crossbow x5.5`
  - `trident x5.2`
  - `arrow_shot x5.0`
  - `blaze_attack x3.6`
  - `llama_spit x4.2`
  - `snowball_shot x5.8`

### `MANIAC`
- category chances:
  - `base 65.0%`
  - `attribute 5.0%`
  - `ability 30.0%`

### `ATTRIBUTER`
- category chances:
  - `base 63.6%`
  - `attribute 34.2%`
  - `ability 2.2%`

## DI Milestone Attributes
- `gluttonous`: guaranteed attribute after level `30`
- `tethered_teleport`: guaranteed attribute after level `10`

## Notes
- This document is intentionally based on the old intended class weighting model.
- Category entries below are the direct class category weights currently intended for implementation.
