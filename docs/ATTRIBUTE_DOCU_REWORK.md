# Attributes Documentation Rework

This is the proposed replacement `AttributesDocu` for the balancing update.

It is a design document, not the current implementation.

## General
- Attributes should scale mainly with attribute level.
- Attributes should not scale heavily from the tame's actual `ATTACK_DAMAGE`.
- Damage-related attributes should use:
  - fixed level-based values
  - percentages
  - proc chances
  - edurations
  - caps
- If an attribute still uses an offensive stat hook, it should prefer stored TamesLevel `damage bonus`, not actual live damage.

## Core Rework Direction
Current issue:
- several offensive attributes scale too much from base damage or directly multiply already-high direct damage
- this makes modded tame statlines and damage stacking too swingy

Target direction:
- level is the primary scaling source
- `damage bonus` is only a secondary hook where needed
- utility and survivability attributes scale by stronger levels, not by hidden attack-stat inflation

## Shared Level Scaling Rule
Use the same first-band progression idea as abilities.

### Level 1-5 Curve
| Level | Multiplier |
|---|---:|
| 1 | 1.00 |
| 2 | 1.25 |
| 3 | 1.50 |
| 4 | 1.75 |
| 5 | 2.00 |

For many numeric attributes:
- `LM = 1.0 + 0.25 * (level - 1)`

Not every attribute needs a raw multiplier, but every scalable attribute should feel meaningfully stronger by level and roughly doubled by level 5 unless capped for safety.

## Damage Bonus Hook For Attributes
If an offensive attribute needs an extra offensive stat hook:
- use stored `damage bonus`
- target conversion: `1 damage bonus = +2%` offensive effectiveness

For on-hit rider attributes, this should usually be translated into one of:
- slightly more rider damage
- slightly stronger proc chance
- slightly higher cap

Recommended rule:
- use `damage bonus` lightly
- do not let it dominate the attribute

## Weighting Note
- When attribute rewards are rolled, current implementation also applies a global preferred-weight boost on top of class-specific preferred entries:
  - preferred attribute weights are multiplied by `x30.0`
- This applies only to class-specific entries whose base class weight is already above `x1.0`.

## Attribute Groups

### 1. Always-on self-buff attributes
Examples:
- `speed`
- `strength`
- `resistance`
- `fire_resistance`
- `jump_boost`

These should scale by level directly through amplifier, uptime, or breakpoint unlocks.

### 2. On-hit rider attributes
Examples:
- `lifesteal`
- `firefang`
- `poison_fang`
- `witherfang`
- `lightningfang`
- `frost_fang`

These should scale mainly through:
- proc chance
- duration
- flat rider damage
- cap increases

They should not primarily scale from live melee damage.

### 3. Conditional combat modifiers
Examples:
- `killer`
- `pacifist`
- `bosskiller`
- `pierce`
- `smite`
- `bane_of_arthropods`
- `sweeping_edge`

These should scale mainly through:
- condition strength
- conditional multiplier
- penetration percent
- splash percent
- radius

### 4. Survival / sustain attributes
Examples:
- `regeneration`
- `totem`
- `feather_falling`
- `explosion_resistance`
- `health_siphon`
- `emergency_cooldown_reduction`

These should scale by stronger thresholds, uptime, reduction, or recovery, not damage.

### 5. Utility / control attributes
Examples:
- `positive_effect_steal`
- `negative_effect_transfer`
- `magnetic`
- `linked_inventory`
- `herding`
- `amphibious`
- `wall_climber`
- `void_cloud`
- `charisma`
- `disc_jockey`
- `ore_scenting`
- `gluttonous`
- `tethered_teleport`
- `muffled`
- `blazing_protection`

These should scale by:
- chance
- range
- uptime
- strength
- number of affected targets
- cooldown or activation reliability

## Proposed Attribute Rework

## Always-On Effect Attributes

### `speed`
- Type: self-buff
- Scaling:
  - L1: Speed I
  - L3: Speed II
  - L5: Speed III
- Recommended rule:
  - one amplifier increase every 2 levels instead of `level - 1` forever
- Reason:
  - current linear amplifier growth is too explosive at high levels

### `strength`
- Type: self-buff
- Scaling:
  - L1: Strength I
  - L3: Strength II
  - L5: Strength III
- Recommended rule:
  - same breakpoint-style scaling as `speed`
- Reason:
  - keeps it strong without multiplying melee damage out of control

### `resistance`
- Type: self-buff
- Scaling:
  - L1: Resistance I
  - L3: Resistance II
  - L5: Resistance III
- Recommended rule:
  - same breakpoint-style scaling as `speed`
- Reason:
  - survivability should scale clearly, but not infinitely per level

### `fire_resistance`
- Type: self-buff
- Scaling:
  - L1+: enabled
- Recommended rule:
  - binary effect
- Reason:
  - extra levels should not be wasted here

### `poison_resistance`
- Type: self-buff
- Scaling:
  - L1+: enabled
- Recommended rule:
  - binary effect

### `jump_boost`
- Type: self-buff
- Scaling:
  - L1: Jump Boost I
  - L3: Jump Boost II
  - L5: Jump Boost III
- Recommended rule:
  - breakpoint-style amplifier scaling

### `wall_climber`
- Type: utility / movement
- Current implementation:
  - binary effect
  - while the tame is pressing into a vertical wall, it gains spider-style upward movement
  - does not add a passive climb when the tame is idle away from a wall
- Leveling:
  - unlevelable
  - one point enables the full effect
- Recommended rule:
  - keep this binary unless you later want higher levels to increase climb speed or wall-stick reliability

## Combat Attributes

### `lifesteal`
- Type: on-hit sustain
- Trigger rule:
  - only triggers on real base-attack hits
  - does not trigger from abilities
  - does not trigger from projectile abilities, rider abilities, or indirect damage events
- Proposed scaling:
  - heal fraction = `4% + 2% * level`
  - L1: `6%`
  - L3: `10%`
  - L5: `14%`
- Healing source:
  - heals from the actual final damage dealt by the base attack hit
- Damage scaling rule:
  - do not add separate base-damage scaling logic to `lifesteal`
  - lifesteal should naturally scale only because the real melee hit dealt more or less damage
- Reason:
  - this keeps lifesteal intuitive
  - it rewards stronger real melee hits
  - it prevents ability spam from double-dipping sustain

### `firefang`
- Type: on-hit rider
- Proposed scaling:
  - burn duration = `2 + level` seconds
  - no extra scaling from base damage
- Optional enhancement:
  - at L3+, small flat bonus hit damage like `+1`
  - at L5+, `+2`

### `poison_fang`
- Type: on-hit rider
- Proposed scaling:
  - poison duration = `40 + 20 * level` ticks
  - poison amplifier:
    - L1-L2: Poison I
    - L3-L5: Poison II
- No base-damage scaling

### `witherfang`
- Type: on-hit rider
- Proposed scaling:
  - wither duration = `40 + 20 * level` ticks
  - amplifier:
    - L1-L3: Wither I
    - L4-L5: Wither II
- No base-damage scaling

### `lightningfang`
- Type: proc rider
- Current problem:
  - old formula depends on base damage
- Proposed scaling:
  - proc chance = `10% + 5% * level`
  - bonus damage on proc = `2 + 2 * level`
  - L1: `15%` for `4` damage
  - L3: `25%` for `8` damage
  - L5: `35%` for `12` damage
- Optional `damage bonus` hook:
  - multiply proc damage by `1.0 + 0.02 * damageBonus`
- Reason:
  - cleaner, flatter scaling and easier balancing

### `killer`
- Type: conditional finisher
- Proposed scaling:
  - extra damage multiplier vs low-HP targets:
  - `1 + missingHealthPercent * (0.20 + 0.10 * level)`
  - L1: low-to-medium execute boost
  - L5: strong execute identity
- Avoid base-damage hooks

### `pacifist`
- Type: opener / anti-full-health
- Proposed scaling:
  - extra damage multiplier vs high-HP targets:
  - `1 + targetHealthPercent * (0.20 + 0.10 * level)`
- Symmetric identity with `killer`

### `bosskiller`
- Type: anti-elite ramp
- Proposed scaling:
  - boss threshold = `100 - 10 * (level - 1)`, min `40`
  - buff strength or uptime scales by level
- No attack-damage scaling

### `victim_siphon`
- Type: kill sustain
- Proposed scaling:
  - heal = `4% + 4% * level` of victim max HP
  - L1: `8%`
  - L3: `16%`
  - L5: `24%`
  - hard cap: `35%`
- Level-based, not damage-based

### `pierce`
- Type: armor bypass
- Proposed scaling:
  - L1: `20%`
  - L2: `35%`
  - L3: `50%`
  - L4: `65%`
  - L5: `80%`
- Recommended:
  - do not hit `100%` pierce so early
- Reason:
  - full armor ignore is too binary

### `sweeping_edge`
- Type: splash rider
- Proposed scaling:
  - splash damage fraction:
    - L1: `20%`
    - L3: `35%`
    - L5: `50%`
  - splash radius:
    - L1: `1.4`
    - L3: `1.8`
    - L5: `2.2`
- If any offensive stat hook is used:
  - use a `+2% per damage bonus` multiplier, not base damage

### `smite`
- Type: conditional anti-undead
- Proposed scaling:
  - bonus multiplier vs undead:
    - L1: `+25%`
    - L3: `+45%`
    - L5: `+65%`
- Flat conditional identity, no base-damage scaling needed

### `bane_of_arthropods`
- Type: conditional anti-arthropod
- Proposed scaling:
  - bonus multiplier vs arthropods:
    - L1: `+25%`
    - L3: `+45%`
    - L5: `+65%`
  - slow duration grows with level

### `positive_effect_steal`
- Type: control / utility
- Proposed scaling:
  - proc chance = `8% + 6% * level`
  - L1: `14%`
  - L3: `26%`
  - L5: `38%`
- Level matters through reliability, not damage

### `negative_effect_transfer`
- Type: control / utility
- Proposed scaling:
  - transferred duration multiplier:
    - L1: `1.00x`
    - L3: `1.50x`
    - L5: `2.00x`

## Utility / Survival Attributes

### `regeneration`
- Type: sustain
- Proposed scaling:
  - heal pulse amount = `0.5 + 0.5 * level`
  - pulse interval may shorten slightly at L3 and L5
- Level-based sustain, not damage-based

### `rejuvenation`
- Type: sustain utility
- Effect:
  - automatically absorbs nearby XP orbs while the tame is injured
  - absorbed XP heals the tame
- Current runtime:
  - search radius = `3` blocks
  - healing = `level` HP per absorbed XP point
- Notes:
  - only matters while the tame is missing health
  - this is sustain from XP orb pickup, not combat damage scaling

### `ability_power`
- Type: offensive support
- Proposed scaling:
  - `ability multiplier = 1.0 + 0.10 * level`
  - L1: `1.10`
  - L3: `1.30`
  - L5: `1.50`
- Reason:
  - keep strong, but slightly flatter than old scaling if ability damage is already reworked

### `emergency_cooldown_reduction`
- Type: clutch utility
- Proposed scaling:
  - proc chance = `8% + 6% * level`
  - L1: `14%`
  - L3: `26%`
  - L5: `38%`
- Trigger threshold can also soften:
  - L1: HP <= `25%`
  - L5: HP <= `35%`

### `totem`
- Type: cheat-death
- Max level: `5`
- Proposed scaling:
  - cooldown:
    - L1: `10m`
    - L3: `8m`
    - L5: `6m`
  - effect quality improves at L3 and L5
- Level-based only

### `killexploder`
- Type: kill-based AoE
- Proposed scaling:
  - explosion damage:
    - L1: `4`
    - L3: `7`
    - L5: `10`
  - radius:
    - L1: `2.0`
    - L3: `2.8`
    - L5: `3.6`
- If using offensive stat hook:
  - use reduced `damage bonus` scaling only, based on the `+2% per damage bonus` model

### `feather_falling`
- Type: survival
- Proposed scaling:
  - fall-damage reduction:
    - L1: `20%`
    - L3: `45%`
    - L5: `70%`

### `explosion_resistance`
- Type: survival
- Proposed scaling:
  - explosion-damage reduction:
    - L1: `15%`
    - L3: `35%`
    - L5: `55%`

## DI Migrated Attributes

### `chain_lightning`
- Type: proc AoE utility damage
- Proposed scaling:
  - proc chance:
    - L1: `12%`
    - L3: `20%`
    - L5: `28%`
  - max jumps:
    - L1: `2`
    - L3: `4`
    - L5: `6`
  - jump damage:
    - L1: `3`
    - L3: `5`
    - L5: `7`
- Do not scale mainly from base damage

### `frost_fang`
- Type: control rider
- Proposed scaling:
  - proc chance:
    - L1: `15%`
    - L3: `30%`
    - L5: `45%`
  - slowness amplifier:
    - L1-L2: I
    - L3-L4: II
    - L5+: III
- Level-driven control identity

### `magnetic`
- Type: control utility
- Effect:
  - pulls struck targets toward the tame on hit
- Current runtime:
  - L1-L2: pull strength `0.60`
  - L3: pull strength `1.00`
  - L4: pull strength `1.20`
  - L5+: `1.40 + 0.10 * (level - 5)`, capped at `2.50`
- Notes:
  - applies forced movement, not extra damage
  - the pull also adds a small upward motion

### `linked_inventory`
- Type: utility
- Effect:
  - migrated DI inventory-sharing utility
- Current status:
  - binary attribute at L1
  - no level scaling
- Notes:
  - utility value depends on the linked-inventory interaction, not combat numbers

### `health_siphon`
- Type: owner defense redirect
- Effect:
  - reduces damage taken by the owner
- Target scaling reference:
  - L1: owner takes `85%` of incoming damage
  - L3: owner takes `65%`
  - L5: owner takes `45%`
- Notes:
  - this is a protective owner-facing utility attribute
  - current TamesLevel inspect treats it as situational because the effect is not direct DPS

### `bubbling`
- Type: control / execute utility
- Effect:
  - on hit, can trap a valid target inside a giant bubble entity
- Legacy/reference scaling:
  - base rule: does not work on high-HP targets
  - L1: max target HP `50`, proc chance `15%`
  - L3: max target HP `70`, proc chance `30%`
  - L5: max target HP `90`, proc chance `45%`
- Notes:
  - primarily crowd-control and displacement
  - migrated from DI enchant behavior

### `herding`
- Type: utility
- Effect:
  - migrated DI shepherd/herding utility for managing nearby animals/tames
- Target scaling reference:
  - L1: range `8`
  - L3: range `12`
  - L5: range `16`
- Notes:
  - utility/mob-control attribute, not combat DPS

### `amphibious`
- Type: utility
- Effect:
  - binary aquatic movement upgrade
  - also adds a permanent land-speed modifier in the current runtime
- Current runtime:
  - grants a permanent `+0.13` movement-speed addition via attribute modifier
- Notes:
  - binary at L1
  - no further level scaling

### `void_cloud`
- Type: utility / survival
- Effect:
  - migrated DI void/fall-survival utility
- Current status:
  - binary at L1
  - no level scaling
- Notes:
  - utility value is survival/mobility oriented rather than numeric DPS

### `charisma`
- Type: economy utility
- Effect:
  - improves owner villager trading prices while compatible pets are nearby
- Current runtime helper:
  - each nearby qualifying pet contributes `10 * charisma level`
  - total owner bonus is capped at `50`
- Older target scaling reference:
  - L1: price multiplier `0.95x`
  - L3: `0.85x`
  - L5: `0.75x`

### `disc_jockey`
- Type: utility
- Effect:
  - migrated DI disc/music utility
- Current status:
  - binary at L1
  - no level scaling

### `warping_bite`
- Type: control / execute utility
- Effect:
  - teleports struck targets to a random nearby position
- Current runtime:
  - attempts up to `16` random teleports within a `16` block horizontal spread
- Legacy/reference scaling:
  - L1: max target HP `50`, proc chance `15%`
  - L3: max target HP `70`, proc chance `30%`
  - L5: max target HP `90`, proc chance `45%`
- Notes:
  - strong control/disruption utility, not raw damage

### `ore_scenting`
- Type: utility
- Effect:
  - migrated DI ore-detection utility
- Current status:
  - binary at L1
  - no combat scaling
- Notes:
  - any radius/search behavior belongs to the ore-finding mechanic, not damage

### `gluttonous`
- Type: utility / sustain
- Effect:
  - guaranteed milestone utility attribute
- Unlock rule:
  - guaranteed once the tame reaches level `30`
- Current status:
  - binary at L1
  - not intended as a normal scaling combat attribute

### `tethered_teleport`
- Type: utility
- Effect:
  - enables teleport-valid behavior for following tames
- Current runtime:
  - `TameableUtils.isValidTeleporter(...)` treats the attribute as the teleport flag
  - for commandable mobs, teleport is allowed while in follow command
  - for normal tamables, teleport is allowed while not sitting
- Unlock rule:
  - guaranteed once the tame reaches level `10`
- Current status:
  - binary at L1

### `muffled`
- Type: utility
- Effect:
  - migrated DI stealth/noise-reduction utility
- Current status:
  - binary at L1
  - no level scaling

### `blazing_protection`
- Type: defensive utility
- Effect:
  - migrated DI defensive fire/barrier utility
- Older target scaling reference:
  - L1: max bars `2`
  - L3: max bars `6`
  - L5: max bars `10`
- Notes:
  - defensive utility rather than direct damage
  - current TamesLevel inspect treats it as situational because the active barrier logic is not expressed as a simple DPS number

## Design Rules Summary
- offensive attributes should not scale mainly from live attack damage
- rider damage should mostly be flat and level-based
- conditional damage modifiers should mostly scale by percentages and breakpoints
- sustain and utility attributes should scale by reliability, duration, radius, and thresholds
- binary utility attributes should stay binary unless there is a good reason to add more scaling
