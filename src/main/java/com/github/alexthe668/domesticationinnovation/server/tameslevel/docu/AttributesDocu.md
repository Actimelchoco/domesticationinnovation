# Attributes Documentation

## General
- Attributes are uncapped unless explicitly marked as `cannot level`.
- Legacy hard cap: `totem` (max level `5`).
- Already-owned attributes are weighted higher when rolling.
- No rarity tiers are used.

## Always-On Effect Attributes
- `speed`: applies Speed, amplifier `level - 1`.
- `strength`: applies Strength, amplifier `level - 1`.
- `resistance`: applies Resistance, amplifier `level - 1`.
- `fire_resistance`: applies Fire Resistance, amplifier `level - 1`.
  - DI migration note: sourced from `fireproof`.
  - DI class weight: same as `poison_resistance`.
  - Numeric example:
    - L1: Fire Resistance I
    - L3: Fire Resistance III
- `poison_resistance`: removes Poison effects from the tame.
  - DI class weight: mid chance for `TANKER`.
  - Numeric example:
    - L1+: poison cleanse active (binary effect).
- `jump_boost`: applies Jump Boost, amplifier `level - 1`.

## Combat Attributes
- `lifesteal`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit.
  - Level scaling: heal = `damage * (0.05 + 0.02 * level)`.

- `firefang`
  - Damage type: `single-target rider`
  - Activation chance: `100%` on hit.
  - Level scaling: burn duration = `2 + level` seconds.

- `witherfang`
  - Damage type: `single-target rider`
  - Activation chance: `100%` on hit.
  - Level scaling: Wither duration `60 + 20 * level` ticks, stronger amplifier at high levels.

- `lightningfang`
  - Damage type: `single-target`
  - Activation chance: `20%` on hit.
  - Level scaling: bonus lightning damage = `((2 + level) + 0.35 * baseDamage) * abilityPowerMultiplier * 5.0`.
  - Lightning is visual-only (no fire spread).

- `killer`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit.
  - Level scaling: more damage vs low-HP targets.
  - Formula: damage multiplier `1 + missingHealthPercent * level`.

- `pacifist`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit.
  - Level scaling: more damage vs high-HP targets.
  - Formula: damage multiplier `1 + targetHealthPercent * level`.

- `bosskiller`
  - Activation chance: `100%` on hit.
  - Level scaling: threshold starts at `100` target max HP and decreases by `10` per level (min `10`).
  - Per threshold stack, tame gets short Strength + Resistance + Speed buffs.

- `victim_siphon`
  - Trigger: on kill.
  - Level scaling: heal = `5% * level` of the victim's max HP, capped at `50%`.
  - Class weight: high chance for `ASSASSIN`.
  - Numeric example:
    - L1: heal `5%` of victim max HP
    - L5: heal `25%`
    - L10+: heal `50%` cap

- `pierce`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit.
  - Effect: ignores part of the target's armor.
  - Class weight: mid chance for `ASSASSIN`, high chance for `DPS`.
  - Level scaling:
    - L1: `50%` armor pierce
    - L2: `62.5%`
    - L3: `75%`
    - L4: `87.5%`
    - L5+: `100%`

- `sweeping_edge`
  - Damage type: `aoe`
  - Activation chance: `100%` on hit.
  - Level scaling: splash radius and splash damage multiplier both increase with level.

- `smite`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit vs undead.
  - Level scaling: extra damage multiplier `+ (0.20 + 0.08 * level)` against undead.

- `bane_of_arthropods`
  - Damage type: `single-target modifier`
  - Activation chance: `100%` on hit vs arthropods.
  - Level scaling: extra damage multiplier `+ (0.20 + 0.08 * level)` against arthropods.
  - Applies short Slowness on arthropod hit.

- `positive_effect_steal`
  - Activation chance: `10% * level`, capped at `80%`.
  - On trigger: steals one random beneficial effect from target.

- `negative_effect_transfer`
  - Activation chance: `100%` on hit.
  - On trigger: transfers tame's harmful effects to target.
  - Level scaling: transferred duration is increased by `20` ticks per level.

## Utility/Survival Attributes
- `regeneration`
  - Activation chance: periodic while damaged and off cooldown.
  - Level scaling: heal per tick cycle = `0.6 * level`.

- `rejuvenation`
  - Effect: absorbs nearby XP orbs for the tame (same gameplay behavior as DI rejuvenation enchant).
  - Leveling: infinite.
  - DI class weight: high chance for `ASSASSIN`, mid chance for `MAGE`.

- `ability_power`
  - Effect: ability power multiplier `1.0 + 0.12 * level`.

- `emergency_cooldown_reduction`
  - Condition: HP <= `30%`.
  - Activation chance: `12% * level`, capped at `60%`.
  - On trigger: current cooldown forced to `1s`.

- `totem` (max `5`)
  - Trigger: lethal hit.
  - Cooldown: base `10m`, reduced by `1m` per level.
  - Levels `1-2`: weak totem save (partial heal + short regen/absorption).
  - Levels `3-5`: normal totem-style save (`1 HP`, regen, absorption, fire resistance).

- `killexploder`
  - Trigger: kill or assist.
  - Damage type: `aoe`
  - Level scaling: explosion damage and radius increase with level.
  - No-grief direct AoE damage (visual explosion only).
  - Friendly fire: **enabled** (can hit players and allied tames in range).

- `feather_falling`
  - Trigger: when the tame takes fall damage.
  - Level scaling: reduces fall damage by `12% * level`, capped at `90%`.

- `explosion_resistance`
  - Trigger: when the tame takes explosion damage.
  - Level scaling: reduces explosion damage by `10% * level`, capped at `80%`.

## DI Migrated Attributes
- `chain_lightning`
  - Source: `chain_lightning` enchantment becomes this attribute.
  - Damage type: `aoe`
  - DI class weight: high chance for `ASSASSIN`, mid chance for `DPS` and `MAGE`.
  - Leveling: infinite.
  - Numeric example:
    - L1: up to `6` chains (`3 + 3 * 1`)
    - L3: up to `12` chains (`3 + 3 * 3`)
    - L5: up to `18` chains (`3 + 3 * 5`)

- `frost_fang`
  - Source: `frost_fang` enchantment becomes this attribute.
  - Damage type: `single-target rider`
  - DI class weight: same as `firefang`.
  - Scaling: each level increases slowness strength and/or activation chance.
  - Scaling: lower enemy HP increases activation chance.
  - Leveling: infinite.
  - Numeric example (target balancing):
    - L1: Slowness I, base proc chance `15%`
    - L3: Slowness II, base proc chance `30%`
    - L5: Slowness III, base proc chance `45%`

- `magnetic`
  - Source: `magnetic` enchantment becomes this attribute.
  - DI class weight: high chance for `ASSASSIN`.
  - Scaling: level `1-2` low pull, level `3` standard DI pull, level `5+` strong pull.
  - Leveling: infinite.
  - Numeric example (pull strength multiplier):
    - L1: `0.60x`
    - L3: `1.00x`
    - L5: `1.40x`

- `linked_inventory`
  - Source: `linked_inventory` enchantment becomes this attribute.
  - DI class weight: mid chance for `SUPPORTER`.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `health_siphon`
  - Source: `health_siphon` enchantment becomes this attribute.
  - DI class weight: mid chance for `TANKER`.
  - Scaling: as level increases, owner receives less damage.
  - Leveling: infinite.
  - Numeric example (target balancing, owner damage taken):
    - L1: owner takes `85%` of incoming damage
    - L3: owner takes `65%`
    - L5: owner takes `45%`

- `bubbling`
  - Source: `bubbling` enchantment becomes this attribute.
  - Damage type: `single-target`
  - DI class weight: mid chance for `ASSASSIN`.
  - Base rule: does not work on 50+ HP mobs.
  - Scaling: level increases activation chance and allowed target HP cap.
  - Leveling: infinite.
  - Numeric example (target balancing):
    - L1: max target HP `50`, proc chance `15%`
    - L3: max target HP `70`, proc chance `30%`
    - L5: max target HP `90`, proc chance `45%`

- `herding`
  - Source: `herding` enchantment becomes this attribute.
  - DI class weight: mid chance for `SUPPORTER`.
  - Scaling: range increases per level.
  - Leveling: infinite.
  - Numeric example:
    - L1: range `8`
    - L3: range `12`
    - L5: range `16`

- `amphibious`
  - Source: `amphibious` enchantment becomes this attribute.
  - DI class weight: high chance for `ASSASSIN`.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `void_cloud`
  - Source: `void_cloud` enchantment becomes this attribute.
  - DI class weight: mid chance for `ASSASSIN`.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `charisma`
  - Source: `charisma` enchantment becomes this attribute.
  - DI class weight: mid chance for `SUPPORTER`.
  - Scaling: higher levels reduce prices further.
  - Leveling: infinite.
  - Numeric example (price multiplier):
    - L1: `0.95x`
    - L3: `0.85x`
    - L5: `0.75x`

- `disc_jockey`
  - Source: `disc_jockey` enchantment becomes this attribute.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `warping_bite`
  - Source: `warping_bite` enchantment becomes this attribute.
  - Damage type: `single-target`
  - DI class weight: high chance for `ASSASSIN`.
  - Base rule: does not work on 50+ HP mobs.
  - Scaling: level increases activation chance and allowed target HP cap.
  - Leveling: infinite.
  - Numeric example (target balancing):
    - L1: max target HP `50`, proc chance `15%`
    - L3: max target HP `70`, proc chance `30%`
    - L5: max target HP `90`, proc chance `45%`

- `ore_scenting`
  - Source: `ore_scenting` enchantment becomes this attribute.
  - DI class weight: mid chance for `SUPPORTER`.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `gluttonous`
  - Source: `gluttonous` enchantment becomes this attribute.
  - Guaranteed milestone: guaranteed attribute after level 30.
  - Leveling: cannot level.
  - Roll behavior: guaranteed-only (not in random attribute rolls).
  - Numeric example:
    - Unlock milestone: first guaranteed roll at tame level `30`.

- `tethered_teleport`
  - Source: `tethered_teleport` enchantment becomes this attribute.
  - Guaranteed milestone: guaranteed attribute after level 10.
  - Leveling: cannot level.
  - Roll behavior: guaranteed-only (not in random attribute rolls).
  - Numeric example:
    - Unlock milestone: first guaranteed roll at tame level `10`.

- `muffled`
  - Source: `muffled` enchantment becomes this attribute.
  - DI class weight: mid chance for `ASSASSIN`.
  - Leveling: cannot level.
  - Numeric example:
    - L1: enabled (binary effect, no scaling).

- `blazing_protection`
  - Source: `blazing_protection` enchantment becomes this attribute.
  - DI class weight: `TANKER`.
  - Leveling: infinite.
  - Numeric example:
    - L1: max bars `2`
    - L3: max bars `6`
    - L5: max bars `10`
