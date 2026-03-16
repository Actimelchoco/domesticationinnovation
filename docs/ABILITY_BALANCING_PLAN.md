# Ability Balancing Plan

## Purpose
This document defines the next large balance pass for TamesLevel abilities.

The current system ties a lot of offensive ability output to the tame's direct damage stat. The new direction is to make ability damage and ability effectiveness mostly independent from normal attack damage, and instead driven by:
- ability identity
- cooldown
- target coverage
- utility strength
- ability level
- a smaller contribution from `damage bonus`

## Design Goals
- Use vanilla wolf DPS as the baseline orientation.
- Treat normal vanilla wolf damage as roughly `2 DPS`.
- Make the average level 1 single-target offensive ability land around `1 DPS`.
- Make AoE abilities lower in single-target DPS than single-target abilities.
- Make level scaling matter more than tame attack damage scaling.
- Make every `+5` ability levels feel like a doubling in effectiveness.
- Keep `damage bonus` relevant, but no longer let it dominate ability tuning.

## Key Direction Changes

### 1. Decouple ability damage from tame attack damage
Current ability formulas often use the tame's actual damage stat or a large fraction of it.

Proposed direction:
- offensive ability damage should come primarily from an ability-specific DPS budget
- tame `ATTACK_DAMAGE` should not be the main driver anymore
- if any stat contributes, it should be the stored TamesLevel `damage bonus`, not raw current attack damage

This keeps:
- wolves, flutters, dragons, and other tame types easier to compare
- ability power mostly under TamesLevel control
- weird runaway scaling from modded tame base stats under control

### 2. Use `damage bonus`, not actual damage stat
Use the stored TamesLevel `damage bonus` as the offensive stat hook.

Target contribution:
- every `1 damage bonus` adds `+2%` offensive effectiveness

Suggested translation:
- `damageBonusMultiplier = 1.0 + 0.02 * damageBonus`

Recommended use:
- multiply the final offensive ability output by this value
- if an AoE or utility-heavy ability needs a softer interaction, apply a reduced effectiveness factor before multiplication

### 3. Make ability level the main scaler
Use a simple per-5-level doubling curve.

For the first five levels:

| Ability Level | Multiplier |
|---|---:|
| 1 | 1.00 |
| 2 | 1.25 |
| 3 | 1.50 |
| 4 | 1.75 |
| 5 | 2.00 |

Equivalent formula:
- `levelMultiplier = 1.0 + 0.25 * (level - 1)`

This matches the target:
- level 1 = baseline
- level 5 = double effectiveness

For later levels, continue in 5-level bands:
- levels `1-5` = `1.0x -> 2.0x`
- levels `6-10` = `2.0x -> 4.0x`
- levels `11-15` = `4.0x -> 8.0x`

Suggested generalized formula:
- `levelBand = floor((level - 1) / 5)`
- `bandStartMultiplier = 2 ^ levelBand`
- `inBandStep = ((level - 1) % 5) * 0.25`
- `levelMultiplier = bandStartMultiplier * (1.0 + inBandStep)`

This needs an implementation cap review later, but it matches the requested rule: every 5 ability levels, effectiveness doubles.

## Baseline DPS Budgets

### Single-target attack baseline
For an average offensive single-target ability:
- level 1 target = `1.0 DPS`
- level 5 target = `2.0 DPS`

Example:
- projectile every `10s`
- level 1 cast damage = `10`
- level 2 cast damage = `12.5`
- level 3 cast damage = `15`
- level 5 cast damage = `20`

This should be the default reference point for "normal" attack abilities with no major utility upside.

### AoE baseline
AoE abilities should have lower single-target DPS because they can hit multiple enemies.

Recommended starting multipliers relative to the single-target baseline:
- pure single-target: `1.00`
- narrow line / small splash / limited chain: `0.80`
- medium AoE / modest multi-hit potential: `0.65`
- large AoE / high expected multi-target coverage: `0.50`

Interpretation:
- if a normal single-target ability is budgeted for `1.0 DPS` at level 1
- then a large AoE ability should often be closer to `0.5 DPS` to `0.65 DPS` on one target

### Utility tax
The more non-damage value an ability brings, the lower its damage budget should be.

Suggested starting utility multipliers:
- no meaningful extra utility: `1.00`
- light utility: `0.90`
- moderate utility: `0.80`
- strong utility / strong control / strong safety: `0.65`
- hybrid support-heal-damage behavior: `0.50` to `0.70`

Examples of utility that should reduce damage budget:
- strong knockback
- strong displacement
- stun-like behavior
- reliable slowing / weakness / fatigue
- mobility or gap closing
- healing or team support
- defensive effects
- area denial

## Proposed Ability Damage Formula

Recommended generic formula for offensive abilities:

`castDamage = cooldownSeconds * baseDpsBudget * targetCoverageMultiplier * utilityMultiplier * levelMultiplier * damageBonusMultiplier`

Where:
- `baseDpsBudget` is usually `1.0` for a standard single-target attack ability
- `targetCoverageMultiplier` is lower for AoE or multi-target abilities
- `utilityMultiplier` is lower for abilities with strong side value
- `levelMultiplier = 1.0 + 0.25 * (level - 1)`
- `damageBonusMultiplier = 1.0 + 0.02 * damageBonus`

Simpler default version:

`castDamage = cooldownSeconds * effectiveDpsBudget * levelMultiplier * damageBonusMultiplier`

This allows most balancing to happen by profile data instead of ad hoc formulas.

## Ability Categories

### Standard single-target attacks
Examples:
- `arrow_shot`
- `trident`
- `llama_spit`
- `blaze_attack`

Target:
- around the standard single-target DPS baseline

### Low single-target DPS multi-target attacks
Examples:
- `creeper_explosion`
- `dragon_fireball`
- `warden_scream`
- `ghast_fireball`
- `evoker_fangs`

Target:
- lower single-target DPS
- strength comes from hitting several mobs or controlling space

### Precision / rare burst attacks
Examples:
- `lightning_strike`
- `elder_guardian_beam`
- `wither_skull`

Target:
- can have bigger per-cast numbers
- average DPS still must fit the budget after cooldown and utility are accounted for

### Support or hybrid abilities
Examples:
- `guardian_repulse`
- `retaliation_slow`
- `healing_bottle`
- `healing_aura`
- `psychic_wall`

Target:
- direct damage should be low or zero unless the support value is intentionally weak
- level scaling should often improve duration, radius, uptime, or proc reliability instead of damage

## Damage Bonus Stat Weight
Current code uses:
- `Damage` base stat reward weight = `20.0`

Proposed target:
- reduce `Damage` base stat reward weight to `5.0`

Reason:
- if abilities no longer scale heavily from raw damage, the damage stat does not need to be overrepresented
- reducing this weight should also lower runaway offensive scaling and make other stat rolls more competitive

## Progression Roll Distribution
Target reward split on level-up:
- `base stat`: `80%`
- `attribute`: `10%`
- `ability`: `10%`

This should become the new default progression identity:
- base stats are the main progression layer
- attributes are rarer specialization
- abilities are rare and valuable

## New Ability vs Existing Ability Progression
When an ability roll happens, new abilities should be much rarer early and become slightly more likely once an owned ability is more developed.

Requested target:
- with an owned ability at level `1`, chance to gain a new ability = `10%`
- when the relevant owned ability reaches level `5`, chance to gain a new ability = `20%`

Design interpretation:
- early ability progression should mostly deepen an existing ability
- new ability acquisition should remain possible, but limited
- once an ability reaches level 5, expansion into additional abilities becomes more acceptable

Recommended first implementation rule:
- if player has no abilities yet: the first ability roll grants a new ability
- otherwise:
  - owned ability max level below `5`: `10%` chance for new ability, `90%` chance to upgrade existing ability
  - owned ability max level `>= 5`: `20%` chance for new ability, `80%` chance to upgrade existing ability

This replaces the current stronger bias logic and should be revisited after playtesting.

## Guaranteed Ability / Attribute Rolls
Remove the guaranteed progression injections every `30` levels.

Current code still has guaranteed attribute-style grants and special handling around these level milestones.

Proposed direction:
- no forced ability or attribute injections at fixed level milestones
- all ability and attribute acquisition should come through the normal roll system
- progression identity should come from the roll distribution, not milestone overrides

## Non-Damage Effectiveness Scaling
The same "level 5 is about double level 1" principle should apply outside direct damage where possible.

Examples:
- duration
- radius
- proc chance
- healing amount
- shield amount
- displacement strength
- number of projectiles
- chain count

Preferred rule:
- use the same level curve unless the ability needs a custom cap
- custom exceptions should be documented per ability