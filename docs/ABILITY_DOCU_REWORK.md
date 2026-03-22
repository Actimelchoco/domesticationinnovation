# Abilities Documentation Rework

This is the proposed replacement `AbilityDocu` for the balancing update.

It is a design document, not the current implementation.

## General
- Offensive ability power should no longer be driven mainly by the tame's actual `ATTACK_DAMAGE`.
- Offensive ability damage should be driven mainly by:
  - ability identity
  - cooldown
  - ability level
  - target coverage
  - utility strength
  - stored TamesLevel `damage bonus`
- Vanilla wolf DPS is the orientation baseline.
- Working baseline: normal vanilla wolf = about `2 DPS`.
- Average level 1 single-target attack ability target = about `1 DPS`.
- Average level 5 single-target attack ability target = about `2 DPS`.

## Ability Level Scaling
Every `5` ability levels, effectiveness doubles.

### Level 1-5 Curve
| Level | Multiplier |
|---|---:|
| 1 | 1.00 |
| 2 | 1.25 |
| 3 | 1.50 |
| 4 | 1.75 |
| 5 | 2.00 |

For the first 5 levels:
- `LM = 1.0 + 0.25 * (level - 1)`

Extended rule:
- levels `1-5` = `1x -> 2x`
- levels `6-10` = `2x -> 4x`
- levels `11-15` = `4x -> 8x`

## Damage Bonus Scaling
Use stored TamesLevel `damage bonus`, not the tame's actual current attack stat.

Target rule:
- every `1 damage bonus` = `+2%` offensive effectiveness

Definitions used below:
- `B = damage bonus`
- `DBM = damage bonus multiplier = 1.0 + 0.02 * B`

## Proposed Offensive Formula
For most offensive abilities:

`castDamage = cooldownSeconds * baseSingleTargetDps * LM * adjustedDBM`

Where:
- `baseSingleTargetDps` is the ability's single-target DPS budget at level 1
- `LM` is the ability-level multiplier
- `adjustedDBM` is how much of the damage-bonus multiplier the ability should use

Recommended defaults:
- standard single-target abilities: `adjustedDBM = DBM`
- light AoE or utility-heavy attacks: `adjustedDBM = 1.0 + (DBM - 1.0) * 0.75`
- medium/large AoE attacks: `adjustedDBM = 1.0 + (DBM - 1.0) * 0.50`

## Role Notes
- `attack`: uses DPS budget and damage formula
- `heal`: mostly scales healing, radius, uptime, or proc reliability
- `support`: mostly scales utility, control, survivability, or uptime

Support and heal abilities should usually not have meaningful direct-damage budgets.

## Weighting Note
- When ability rewards are rolled, current implementation also applies a global preferred-weight boost on top of class-specific preferred entries:
  - preferred ability weights are multiplied by `x9.0`
- This applies only to class-specific entries whose base class weight is already above `x1.0`.

## Proposed Attack Ability Profiles

Assumptions in this section:
- ranking is based on single-target damage only
- `B = 0` for ranking tables
- no armor/resistance mitigation
- cooldowns use their current runtime values unless otherwise noted
- the ranking is a balance target, not current code behavior

| Ability | Cooldown | Single-Target DPS L1 | Single-Target DPS L5 | Damage Bonus Scaling | Notes |
|---|---:|---:|---:|---:|---|
| `trident` | `4.5s` | `1.15` | `2.30` | `100%` | premium single-target burst |
| `arrow_shot` | `3.0s` | `1.00` | `2.00` | `100%` | standard baseline single-target attack |
| `crossbow` | `4.0s` | `0.95` | `1.90` | `100%` | strong ranged pressure but safety tax |
| `llama_spit` | `2.5s` | `0.95` | `1.90` | `100%` | fast baseline ranged poke |
| `blaze_attack` | `2.0s` | `0.90` | `1.80` | `100%` | frequent ranged poke |
| `elder_guardian_beam` | `6.0s` | `0.85` | `1.70` | `85%` | damage lowered for fatigue utility |
| `shulker_bullet` | `10.0s` | `0.85` | `1.70` | `85%` | levitation utility tax |
| `lightning_strike` | `25.0s` | `0.80` | `1.60` | `85%` | rare burst, high cast damage, long cooldown |
| `shadow_hands` | `steady` | `0.80` | `1.60` | `90%` | sustained pressure, not a simple cast model |
| `wither_skull` | `4.0s` | `0.75` | `1.50` | `75%` | projectile + splash potential |
| `guardian_beam` | `3.5s` | `0.70` | `1.40` | `85%` | precise damage, lower raw output |
| `snowball_shot` | `1.0s` | `0.70` | `1.40` | `100%` | low-damage rapid poke |
| `fishing` | `4.5s` | `0.65` | `1.30` | `75%` | pull/displacement utility tax |
| `dash` | `4.5s` | `0.65` | `1.30` | `75%` | mobility and sweep utility tax |
| `warden_scream` | `6.0s` | `0.60` | `1.20` | `50%` | cone/AoE plus knockback/control |
| `ghast_fireball` | `5.0s` | `0.55` | `1.10` | `50%` | splash zoning projectile |
| `evoker_fangs` | `5.0s` | `0.50` | `1.00` | `50%` | multi-hit area denial |
| `dragon_fireball` | `7.0s` | `0.50` | `1.00` | `50%` | large AoE and zone pressure |
| `creeper_explosion` | `10.0s` | `0.45` | `0.90` | `50%` | large AoE, highest multi-target potential |

## Proposed Attack Formulas

### `arrow_shot`
- Cooldown: `3.0s`
- Baseline DPS: `1.00`
- Damage bonus scaling: `100%`
- Formula:
  - `castDamage = 3.0 * 1.00 * LM * DBM`
- Example with `B = 0`:
  - L1: `3.0`
  - L5: `6.0`

### `snowball_shot`
- Cooldown: `1.0s`
- Baseline DPS: `0.70`
- Damage bonus scaling: `100%`
- Formula:
  - `castDamage = 1.0 * 0.70 * LM * DBM`
- Example with `B = 0`:
  - L1: `0.7`
  - L5: `1.4`

### `ghast_fireball`
- Cooldown: `5.0s`
- Baseline DPS: `0.55`
- Damage bonus scaling: `50%`
- Formula:
  - `castDamage = 5.0 * 0.55 * LM * (1.0 + (DBM - 1.0) * 0.50)`
- Example with `B = 0`:
  - L1: `2.75`
  - L5: `5.5`

### `creeper_explosion`
- Cooldown: `10.0s`
- Baseline DPS: `0.45`
- Damage bonus scaling: `50%`
- Formula:
  - `castDamage = 10.0 * 0.45 * LM * (1.0 + (DBM - 1.0) * 0.50)`
- Example with `B = 0`:
  - L1: `4.5`
  - L5: `9.0`
- Damage is intentionally low on one target because the ability can hit many tames/mobs.

### `wither_skull`
- Cooldown: `4.0s`
- Baseline DPS: `0.75`
- Damage bonus scaling: `75%`
- Formula:
  - `castDamage = 4.0 * 0.75 * LM * (1.0 + (DBM - 1.0) * 0.75)`
- Example with `B = 0`:
  - L1: `3.0`
  - L5: `6.0`

### `blaze_attack`
- Cooldown: `2.0s`
- Baseline DPS: `0.90`
- Damage bonus scaling: `100%`
- Formula:
  - `castDamage = 2.0 * 0.90 * LM * DBM`
- Example with `B = 0`:
  - L1: `1.8`
  - L5: `3.6`

### `guardian_beam`
- Cooldown: `3.5s`
- Baseline DPS: `0.70`
- Damage bonus scaling: `85%`
- Formula:
  - `castDamage = 3.5 * 0.70 * LM * (1.0 + (DBM - 1.0) * 0.85)`
- Example with `B = 0`:
  - L1: `2.45`
  - L5: `4.9`

### `elder_guardian_beam`
- Cooldown: `6.0s`
- Baseline DPS: `0.85`
- Damage bonus scaling: `85%`
- Formula:
  - `castDamage = 6.0 * 0.85 * LM * (1.0 + (DBM - 1.0) * 0.85)`
- Example with `B = 0`:
  - L1: `5.1`
  - L5: `10.2`
- Damage is higher than `guardian_beam`, but still reduced for strong utility.

### `trident`
- Cooldown: `4.5s`
- Baseline DPS: `1.15`
- Damage bonus scaling: `100%`
- Formula:
  - `castDamage = 4.5 * 1.15 * LM * DBM`
- Example with `B = 0`:
  - L1: `5.175`
  - L5: `10.35`

### `crossbow`
- Cooldown: `4.0s`
- Baseline DPS: `0.95`
- Damage bonus scaling: `100%`
- Formula:
  - `totalCastDamage = 4.0 * 0.95 * LM * DBM`
- Example with `B = 0`:
  - L1: `3.8`
  - L5: `7.6`
- If multi-arrow behavior remains, total arrow damage should sum to this budget instead of scaling beyond it.

### `evoker_fangs`
- Cooldown: `5.0s`
- Baseline DPS: `0.50`
- Damage bonus scaling: `50%`
- Formula:
  - `castDamage = 5.0 * 0.50 * LM * (1.0 + (DBM - 1.0) * 0.50)`
- Example with `B = 0`:
  - L1: `2.5`
  - L5: `5.0`

### `dragon_fireball`
- Cooldown: `7.0s`
- Baseline DPS: `0.50`
- Damage bonus scaling: `50%`
- Formula:
  - `castDamage = 7.0 * 0.50 * LM * (1.0 + (DBM - 1.0) * 0.50)`
- Example with `B = 0`:
  - L1: `3.5`
  - L5: `7.0`

### `llama_spit`
- Cooldown: `2.5s`
- Baseline DPS: `0.95`
- Damage bonus scaling: `100%`
- Formula:
  - `castDamage = 2.5 * 0.95 * LM * DBM`
- Example with `B = 0`:
  - L1: `2.375`
  - L5: `4.75`

### `fishing`
- Cooldown target for budgeting: `4.5s`
- Baseline DPS: `0.65`
- Damage bonus scaling: `75%`
- Formula:
  - `castDamage = 4.5 * 0.65 * LM * (1.0 + (DBM - 1.0) * 0.75)`
- Example with `B = 0`:
  - L1: `2.925`
  - L5: `5.85`
- Pull utility is part of the power budget, so raw damage stays below normal projectile attacks.

### `dash`
- Cooldown target for budgeting: `4.5s`
- Baseline DPS: `0.65`
- Damage bonus scaling: `75%`
- Formula:
  - `castDamage = 4.5 * 0.65 * LM * (1.0 + (DBM - 1.0) * 0.75)`
- Example with `B = 0`:
  - L1: `2.925`
  - L5: `5.85`
- Mobility and sweep potential are part of the budget, so single-target damage stays moderate.

### `lightning_strike`
- Cooldown: `25.0s`
- Baseline DPS: `0.80`
- Damage bonus scaling: `85%`
- Formula:
  - `castDamage = 25.0 * 0.80 * LM * (1.0 + (DBM - 1.0) * 0.85)`
- Example with `B = 0`:
  - L1: `20.0`
  - L5: `40.0`
- This is intentionally high cast damage, but not high average DPS.

### `warden_scream`
- Cooldown: `6.0s`
- Baseline DPS: `0.60`
- Damage bonus scaling: `50%`
- Formula:
  - `castDamage = 6.0 * 0.60 * LM * (1.0 + (DBM - 1.0) * 0.50)`
- Example with `B = 0`:
  - L1: `3.6`
  - L5: `7.2`

### `shulker_bullet`
- Cooldown: `10.0s`
- Baseline DPS: `0.85`
- Damage bonus scaling: `85%`
- Formula:
  - `castDamage = 10.0 * 0.85 * LM * (1.0 + (DBM - 1.0) * 0.85)`
- Example with `B = 0`:
  - L1: `8.5`
  - L5: `17.0`

### `shadow_hands`
- Damage model: sustained, not simple cast burst
- Baseline DPS: `0.80`
- Damage bonus scaling: `90%`
- Target formula:
  - `singleTargetDps = 0.80 * LM * (1.0 + (DBM - 1.0) * 0.90)`
- Example with `B = 0`:
  - L1: `0.80 DPS`
  - L5: `1.60 DPS`
- Recommended implementation:
  - keep hand count and animation scaling tied to level
  - normalize total average DPS to this budget

## Support and Heal Abilities

These should not be ranked by single-target damage.

### `battle_strength`
- Trigger: `10%` proc when the tame is hurt
- Effect: grants nearby friendly entities `Strength`
- Radius: `8` blocks
- Duration: `100 ticks` (`5.0s`)
- Scaling:
  - amplifier = `level - 1`
  - level mostly increases the buff tier, not the radius

### `defensive_aura`
- Trigger: `10%` proc when the tame is hurt
- Effect: grants nearby friendly entities `Resistance`
- Radius: `8` blocks
- Duration: `100 + 20 * (level - 1)` ticks
- Scaling:
  - amplifier = `level - 1`
  - higher levels increase both duration and mitigation strength

### `ender_pearl_jump`
- Trigger: when the current target is more than `5` blocks away and the cooldown is ready
- Effect: teleports the tame toward the target
- Max teleport distance: `4 + 2 * level` blocks
- Cooldown: `100 ticks` (`5.0s`)
- Purpose: gap-closing and repositioning, not direct damage

### `berserker`
- Trigger: when the tame would drop to `<= 20%` HP and the cooldown is ready
- Effect: grants the tame `Strength` and `Resistance`
- Duration: `400 ticks` (`20.0s`)
- Scaling:
  - both amplifiers = `level - 1`
- Cooldown: `400 ticks` (`20.0s`)

### `bloodlust`
- Trigger: on kill, if the cooldown is ready
- Effect: grants the tame `Strength` and `Resistance`
- Duration: `400 ticks` (`20.0s`)
- Scaling:
  - both amplifiers = `level - 1`
- Cooldown: `400 ticks` (`20.0s`)

### `retaliation_slow`
- Trigger: on hurt, if the cooldown is ready
- Proc chance: `20% + 4% * level`, capped at `85%`
- Effect: applies an AoE `Slowness` pulse to nearby enemies around the tame
- Cooldown: `60 ticks` (`3.0s`)
- Scaling:
  - duration = `40 + 10 * level` ticks
  - slowness amplifier increases every 3rd level
  - radius grows on the non-amplifier levels
- Current radius formula: `2.0 + 0.35 * (level - floor(level / 3))`

### `immunity_frame`
- Trigger: reactive defensive passive
- Effect: grants a short invulnerability window after the tame is hit
- Duration: `20 + 20 * level` ticks
- Notes:
  - no active cast damage
  - this is a breakpoint-sensitive defensive ability because extra invulnerability time is extremely strong

### `deflection`
- Trigger: passive projectile defense
- Effect: reverses an incoming projectile and sends it back at `20%` speed
- Leveling: effectively binary in the current runtime
- Notes:
  - no direct damage budget
  - value comes from projectile cancel/reflect utility

### `defusal`
- Trigger: passively cancels nearby explosions when off cooldown
- Radius: `10 + 10 * floor(level / 3)` blocks
- Cooldown: `max(0, 100 - 20 * (level - 1))` ticks
- Notes:
  - no direct damage
  - power comes from coverage and cooldown access

### `psychic_wall`
- Trigger: active wall summon
- Effect: creates a control/defense wall
- Scaling:
  - wall width = `level + 1`
  - lifespan = `100 * level` ticks
  - cooldown = `200 * level + 40` ticks
- Notes:
  - no direct damage
  - value comes from space control, projectile/body blocking, and uptime

### `healing_aura`
- Trigger: periodic healing cycle
- Effect: applies `Regeneration` pulses during an active window
- Active window: `200 ticks` (`10.0s`)
- Downtime: random `600..1199` ticks (`30.0s..59.95s`) between cycles
- Scaling:
  - regeneration amplifier = `level - 1`
- Notes:
  - this is sustain over time, not burst healing

### `healing_bottle`
- Trigger: when the tame is injured and the cooldown is ready
- Effect: throws a self-targeted splash healing potion
- Potion payload:
  - instant healing `I` up to level `3`
  - instant healing `II` at level `4+`
  - always adds `Regeneration`
- Regeneration:
  - duration = `60 + 40 * (level - 1)` ticks
  - amplifier = `floor((level - 1) / 3)`, capped at `IV`
- Cooldown: `max(60, 220 - 15 * (level - 1))` ticks
- Notes:
  - identity is self-sustain, not damage

### `guardian_repulse`
- Trigger 1: regular support pulse around the tame
- Tame pulse:
  - retargets nearby monsters onto the tame
  - radius = `6.0 + 0.6 * level`
  - chance per monster = `12% + 4% * level`, capped at `90%`
  - cooldown = `400 ticks` (`20.0s`)
- Trigger 2: owner-protection pulse when the owner is hurt and the tame is within `3` blocks
- Owner pulse:
  - knocks nearby monsters away from the owner
  - radius = `2.8 + 0.25 * level`
  - cooldown = `400 ticks` (`20.0s`)
- Notes:
  - this is an aggro-control and peel tool, not a damage spell

### `last_stand_fury`
- Trigger: passive while the owner is injured
- Effect: the tame gains scaling `Strength` and `Speed` based on owner missing HP
- Refresh cadence: every `40 ticks` (`2.0s`)
- Scaling:
  - lower owner HP gives higher temporary buff amplifiers
- Notes:
  - indirect offensive/defensive value through owner danger response

### `shield_block`
- Trigger: when the tame is hurt and the cooldown is ready
- Effect: reduces the triggering hit
- Mitigation: `65% + 3% * (level - 1)`, capped at `95%`
- Cooldown: `max(20, 200 - 20 * (level - 1))` ticks
- Notes:
  - applies to the tame itself, not an ally

### `sky_launch`
- Trigger 1: offensive proc against an enemy target
- Offensive proc:
  - chance = base `10%` on offense, `16%` on defensive retaliation
  - extra chance = `+4% * level`
  - cooldown = `70 ticks` (`3.5s`)
  - deals its normal cast damage and launches the target upward
- Trigger 2: owner-protection pulse when the owner is hurt and the tame is within `3` blocks
- Owner pulse:
  - launches nearby monsters upward and slightly outward
  - radius = `2.5 + 0.20 * level`
  - lift = `0.30 + 0.15 * level`
  - cooldown = `240 ticks` (`12.0s`)
- Notes:
  - part damage/control on offense, pure peel/control on owner defense

### `guardian_intercept`
- Trigger: allied hurt response
- Effect: redirects part of an ally's incoming hit to the supporter
- Redirected share: `20% + 10% * level`, capped at `60%`
- Cooldown: `max(40, 410 - 10 * level)` ticks
- Notes:
  - the ally takes less damage
  - the supporter pays that redirected amount instead

### `emergency_shield`
- Trigger: allied hurt response when the hit would drop the ally below `35%` HP
- Effect:
  - reduces the triggering hit by `20% + 8% * level`, capped at `60%`
  - grants `Absorption`
  - grants short `Resistance`
- Reach: supporter must be within `5` blocks of the ally
- Absorption duration: `80 + 20 * level` ticks
- Resistance duration: `40 + 20 * level` ticks
- Cooldown: `max(80, 240 - 20 * level)` ticks
- Notes:
  - intended as a clutch save, not a constant mitigation aura

### `body_block`
- Trigger: allied hurt response against projectile damage only
- Effect: prevents part of the projectile hit
- Prevention: `45% + 10% * level`, capped at `90%`
- Support backlash: supporter takes `50%` of the prevented damage
- Cooldown: `max(40, 415 - 15 * level)` ticks
- Notes:
  - this is projectile-specific protection

### `battlefield_medic`
- Trigger: on kill or assist, if the cooldown is ready
- Effect: heals nearby allies within `8` blocks
- Heal amount:
  - assist = `1.0 + 0.75 * level`
  - kill = `2.0 + 0.75 * level`
- Cooldown:
  - assist = `120 ticks` (`6.0s`)
  - kill = `80 ticks` (`4.0s`)

### `triage_pulse`
- Trigger: periodic support heal
- Effect: heals the lowest-health ally within `10` blocks
- Heal amount: `1.5 + 0.75 * level`
- Cooldown: `max(40, 120 - 10 * (level - 1))` ticks

### `revitalizing_presence`
- Trigger: when an ally is healed and the cooldown is ready
- Effect: mirrors part of that heal to another injured ally within `10` blocks
- Mirrored share: `20% + 10% * level` of the original heal
- Cooldown: `max(20, 80 - 5 * level)` ticks

### `cleanse_touch`
- Trigger: periodic support cleanse
- Effect: removes one harmful effect from a debuffed ally within `10` blocks
- Cooldown: `max(60, 180 - 15 * (level - 1))` ticks

### `pack_guard`
- Trigger: when an ally is hurt by a monster
- Effect:
  - applies `Weakness` to the attacker
  - forces the attacker to retarget onto the supporter
- Weakness duration: `60 + 20 * level` ticks
- Weakness tier:
  - `I` below level `4`
  - `II` at level `4+`
- Cooldown: `max(40, 140 - 10 * level)` ticks

### `life_gift`
- Trigger: lethal-save response for allied tames
- Effect: transfers the supporter's own HP to prevent the ally from dying
- Safety rule:
  - the supporter will not spend below `5` HP
- Desired post-save recovery: up to `2 + level` HP, capped by how much the supporter can spare
- Cooldown: `max(100, 300 - 20 * level)` ticks

## Single-Target DPS Ranking

Ranking assumptions:
- `B = 0`
- no armor/resistance
- ranking is by proposed level 1 single-target DPS
- level 5 ranking order is the same because all offensive abilities double by level 5

| Rank | Ability | DPS L1 | DPS L5 |
|---:|---|---:|---:|
| 1 | `trident` | `1.15` | `2.30` |
| 2 | `arrow_shot` | `1.00` | `2.00` |
| 3 | `crossbow` | `0.95` | `1.90` |
| 4 | `llama_spit` | `0.95` | `1.90` |
| 5 | `blaze_attack` | `0.90` | `1.80` |
| 6 | `elder_guardian_beam` | `0.85` | `1.70` |
| 7 | `shulker_bullet` | `0.85` | `1.70` |
| 8 | `lightning_strike` | `0.80` | `1.60` |
| 9 | `shadow_hands` | `0.80` | `1.60` |
| 10 | `wither_skull` | `0.75` | `1.50` |
| 11 | `guardian_beam` | `0.70` | `1.40` |
| 12 | `snowball_shot` | `0.70` | `1.40` |
| 13 | `fishing` | `0.65` | `1.30` |
| 14 | `dash` | `0.65` | `1.30` |
| 15 | `warden_scream` | `0.60` | `1.20` |
| 16 | `ghast_fireball` | `0.55` | `1.10` |
| 17 | `evoker_fangs` | `0.50` | `1.00` |
| 18 | `dragon_fireball` | `0.50` | `1.00` |
| 19 | `creeper_explosion` | `0.45` | `0.90` |

## Notes For Implementation
- `crossbow` should not exceed its DPS budget just because more arrows are fired; arrow count should divide the damage budget, not multiply it for free.
- `shadow_hands` should be normalized to a sustained DPS target.
- `lightning_strike` should feel like burst, not sustained DPS dominance.
- `creeper_explosion`, `dragon_fireball`, `ghast_fireball`, and `warden_scream` must stay lower on one target because their real value comes from coverage and utility.
- `damage bonus` should remain relevant through a clean `+2% per point` multiplier model, but it should no longer break ability balance.
