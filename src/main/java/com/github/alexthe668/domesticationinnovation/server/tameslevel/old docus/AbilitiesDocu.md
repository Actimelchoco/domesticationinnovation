# Abilities Documentation

## General
- Abilities are uncapped (`level` can grow infinitely unless otherwise noted by behavior).
- Offensive abilities use the tame's current target.
- In `passive` mode, offensive abilities are skipped.
- Ability logic is no-grief (no block-breaking explosion behavior from custom ability code).

## Ability Role Types
- `attack`: offensive abilities that count toward the cooldown nerf and also have their own cooldown increased by it.
- `heal`: healing abilities that do not count toward the cooldown nerf and are not affected by it.
- `support`: utility/defensive/control abilities that do not count toward the cooldown nerf and are not affected by it.

### Current Type Map
- `attack`: `arrow_shot`, `snowball_shot`, `ghast_fireball`, `creeper_explosion`, `wither_skull`, `blaze_attack`, `guardian_beam`, `elder_guardian_beam`, `trident`, `crossbow`, `evoker_fangs`, `dragon_fireball`, `llama_spit`, `lightning_strike`, `warden_scream`, `shulker_bullet`, `fishing`, `dash`, `flash`, `shadow_hands`
- `heal`: `healing_aura`, `healing_bottle`, `battlefield_medic`, `triage_pulse`, `revitalizing_presence`, `life_gift`
- `support`: `battle_strength`, `defensive_aura`, `ender_pearl_jump`, `berserker`, `bloodlust`, `retaliation_slow`, `immunity_frame`, `deflection`, `defusal`, `psychic_wall`, `guardian_repulse`, `last_stand_fury`, `shield_block`, `sky_launch`, `guardian_intercept`, `emergency_shield`, `body_block`, `cleanse_touch`, `pack_guard`

## Damage Multiplier
- `ability_power_multiplier = 1.0 + 0.10 * ability_power_level`
- Offensive abilities also use:
  - `level_multiplier = 1.0 + 0.25 * (ability_level - 1)`
  - `bonus_damage_multiplier = 1.0 + 0.05 * bonusDamage * scaling`
- Final offensive cast damage = `base_dps * cooldown_seconds * level_multiplier * bonus_damage_multiplier * ability_power_multiplier`
- The `scaling` term depends on the ability family and is defined in runtime code.

## DPS Example Assumptions
- Tame base damage = `5`
- Every cast hits
- No armor/resistance/reduction on target
- `ability_power` level = `0` (`M = 1.00`) for numeric DPS examples
- `DPS = damage_per_cast / cooldown_seconds`
- Attack-only cooldown nerf:
  - `cooldown_multiplier = 1 + (admin_percent / 100) * log2(attackAbilityCount)`
  - only `attack` abilities count toward `attackAbilityCount`
  - `heal` and `support` abilities are excluded from both the count and the cooldown increase
- Default `admin_percent` is currently `25`
- For exact live numbers on a real tame, use `/tames inspect <pet>`

## Ability Details

### `arrow_shot`
- Cooldown: `3.0s`
- Damage type: `single-target`
- Damage per arrow: `(2.0 + level + 0.65 * baseDamage) * M`
- With baseDamage `5`: damage = `(5.25 + level) * M`
- DPS example:
  - L1: `6.25` damage, `2.08 DPS`
  - L3: `8.25` damage, `2.75 DPS`
  - L5: `10.25` damage, `3.42 DPS`

### `snowball_shot`
- Cooldown: `1.0s`
- Damage type: `single-target`
- Damage: `0.30 * arrow_shot_damage_at_same_level`
- Equivalent formula: `0.30 * (2.0 + level + 0.65 * baseDamage) * M`
- Intended tuning: slightly less DPS than `arrow_shot`
- DPS example:
  - L1: `1.88` damage, `1.88 DPS`
  - L3: `2.48` damage, `2.48 DPS`
  - L5: `3.08` damage, `3.08 DPS`

### `ghast_fireball`
- Cooldown: `5.0s`
- Damage type: `aoe`
- Uses `NoGriefLargeFireball`
- Fireball power: `clamp(level, 1..3)`
- Damage formula on hit: `6.0 + 2.0 * power + 0.80 * baseDamage` (no `M` multiplier in this class)
- With baseDamage `5`: `10.0 + 2.0 * power`
- DPS example:
  - L1 (power 1): `12.0` damage, `2.40 DPS`
  - L3 (power 3): `16.0` damage, `3.20 DPS`
  - L5 (power 3): `16.0` damage, `3.20 DPS`

### `creeper_explosion`
- Cooldown: `10.0s`
- Damage type: `aoe`
- Damage per target in AoE: `(8.0 + 1.5 * (level - 1) + 0.80 * baseDamage) * M`
- Radius: `3.0 + 0.3 * (level - 1)`
- Friendly fire: **enabled** (can hit players and allied tames in range)
- With baseDamage `5`: damage = `(12.0 + 1.5 * (level - 1)) * M`
- DPS example:
  - L1: `12.0` damage, `1.20 DPS`
  - L3: `15.0` damage, `1.50 DPS`
  - L5: `18.0` damage, `1.80 DPS`

### `wither_skull`
- Cooldown: `4.0s`
- Damage type: `aoe`
- Damage: vanilla wither-skull projectile behavior (custom code spawns projectile; does not override hit damage directly)
- Extra behavior: `dangerous = true` at ability level `>= 5`
- DPS example:
  - L1/L3/L5: not defined by custom direct damage formula

### `blaze_attack`
- Cooldown: `2.0s`
- Damage type: `single-target`
- Damage: `0.60 * arrow_shot_damage_at_same_level`
- Equivalent formula: `0.60 * (2.0 + level + 0.65 * baseDamage) * M`
- Intended tuning: slightly less DPS than `arrow_shot`
- DPS example:
  - L1: `3.75` damage, `1.88 DPS`
  - L3: `4.95` damage, `2.48 DPS`
  - L5: `6.15` damage, `3.08 DPS`

### `guardian_beam`
- Cooldown: `3.5s`
- Damage type: `single-target`
- Damage target: `40% of arrow_shot DPS`
- Equivalent per-cast damage: `0.40 * (guardianCooldown / arrowCooldown) * arrow_shot_damage`
- With current cooldowns: `0.40 * (70/60) * arrow_shot_damage = 0.4667 * arrow_shot_damage`
- Equivalent formula: `0.4667 * (2.0 + level + 0.65 * baseDamage) * M`
- DPS example:
  - L1: `2.92` damage, `0.83 DPS`
  - L3: `3.85` damage, `1.10 DPS`
  - L5: `4.78` damage, `1.37 DPS`

### `elder_guardian_beam`
- Cooldown: `6.0s`
- Damage type: `single-target`
- Damage target: slightly above `guardian_beam` DPS
- Current tuning: `elder DPS = 1.125 * guardian DPS`
- Equivalent DPS ratio vs arrow_shot: `0.45 * arrow_shot DPS`
- Equivalent per-cast damage: `0.45 * (elderCooldown / arrowCooldown) * arrow_shot_damage`
- With current cooldowns: `0.45 * (120/60) * arrow_shot_damage = 0.90 * arrow_shot_damage`
- Equivalent formula: `0.90 * (2.0 + level + 0.65 * baseDamage) * M`
- Extra effect: mining fatigue duration/amplifier scales with level
- With baseDamage `5`: damage = `0.90 * (5.25 + level) * M`
- DPS example:
  - L1: `5.63` damage, `0.94 DPS`
  - L3: `7.43` damage, `1.24 DPS`
  - L5: `9.23` damage, `1.54 DPS`

### `trident`
- Cooldown: `4.5s`
- Damage type: `single-target`
- Damage: `1.70 * arrow_shot_damage_at_same_level`
- Equivalent formula: `1.70 * (2.0 + level + 0.65 * baseDamage) * M`
- Intended tuning: slightly more DPS than `arrow_shot`
- DPS example:
  - L1: `10.63` damage, `2.36 DPS`
  - L3: `14.03` damage, `3.12 DPS`
  - L5: `17.43` damage, `3.87 DPS`

### `crossbow`
- Cooldown: `4.0s`
- Damage type: `single-target`
- Fires: `ability level` arrows (every level adds one arrow)
- Damage per arrow: `(3.5 + 0.50 * baseDamage) * M`
- With baseDamage `5`: `6.0 * M` per arrow
- DPS example (all arrows hit):
  - L1: `6.0` total damage, `1.50 DPS`
  - L3: `18.0` total damage, `4.50 DPS`
  - L5: `30.0` total damage, `7.50 DPS`

### `evoker_fangs`
- Cooldown: `5.0s`
- Damage type: `aoe`
- Damage: vanilla evoker-fangs behavior (custom code spawns fangs; no direct damage override)
- Extra behavior:
  - Level `>= 3`: chance to spawn a forward fang line
  - Level `>= 4`: chance to spawn a ring of fangs
- DPS example:
  - L1/L3/L5: not defined by custom direct damage formula

### `dragon_fireball`
- Cooldown: `7.0s`
- Damage type: `aoe`
- Damage per target in AoE: `(6.0 + 2.0 * level + 1.10 * baseDamage) * M`
- Radius: `3.0 + 0.35 * level`
- With baseDamage `5`: damage = `(11.5 + 2.0 * level) * M`
- DPS example:
  - L1: `13.5` damage, `1.93 DPS`
  - L3: `17.5` damage, `2.50 DPS`
  - L5: `21.5` damage, `3.07 DPS`

### `llama_spit`
- Cooldown: `2.5s`
- Damage type: `single-target`
- Damage: `(3.0 + level + 0.60 * baseDamage) * M`
- With baseDamage `5`: damage = `(6.0 + level) * M`
- DPS example:
  - L1: `7.0` damage, `2.80 DPS`
  - L3: `9.0` damage, `3.60 DPS`
  - L5: `11.0` damage, `4.40 DPS`

### `ender_pearl_jump`
- Cooldown: `5.0s` (only when teleport succeeds)
- Trigger: target distance must be `> 5` blocks
- Max step distance: `4.0 + 2.0 * level`
- Damage: none

### `lightning_strike`
- Cooldown: `25.0s`
- Damage type: `single-target`
- Damage: `(5.0 + 2.0 * (level - 1) + 0.80 * baseDamage) * M * 5.0`
- Lightning is visual-only; damage is direct
- With baseDamage `5`: damage = `(9.0 + 2.0 * (level - 1)) * M`
- DPS example:
  - L1: `45.0` damage, `1.80 DPS`
  - L3: `65.0` damage, `2.60 DPS`
  - L5: `85.0` damage, `3.40 DPS`

### `warden_scream`
- Cooldown: `6.0s`
- Damage type: `aoe`
- Trigger: line-of-sight required
- Range: `16.0 + 2.0 * level`
- Damage: `(6.0 + 2.0 * (level - 1) + 1.0 * baseDamage) * M`
- Knockback scales with level
- With baseDamage `5`: damage = `(11.0 + 2.0 * (level - 1)) * M`
- DPS example:
  - L1: `11.0` damage, `1.83 DPS`
  - L3: `15.0` damage, `2.50 DPS`
  - L5: `19.0` damage, `3.17 DPS`

### `battle_strength`
- Cooldown: none (proc-style, on hurt event)
- Proc chance: `10%` on hurt
- Effect: applies Strength to nearby allies
- Damage: no direct damage instance

### `defensive_aura`
- Cooldown: none (proc-style, on hurt event)
- Proc chance: `10%` on hurt
- Effect: applies Resistance to nearby allies
- Damage: no direct damage instance

### `berserker`
- Cooldown: `20.0s`
- Trigger: tame HP `<= 20%`
- Effect: applies Strength + Resistance to self
- Damage: no direct damage instance

### `bloodlust`
- Cooldown: `20.0s`
- Trigger: on kill
- Effect: applies Strength + Resistance to self
- Damage: no direct damage instance

### `shulker_bullet`
- Cooldown: `10.0s`
- Damage type: `single-target`
- Trigger gate: extra levitation application only if target HP `<= 50`
- Damage: vanilla shulker-bullet projectile behavior (custom code does not override hit damage directly)
- DPS example:
  - L1/L3/L5: not defined by custom direct damage formula

### `guardian_repulse`
- Trigger: when owner is hurt and tame is within `3` blocks of owner.
- Effect: knocks nearby hostile mobs away from owner.
- Radius: `2.8 + 0.25 * level` blocks.
- Cooldown: `40s - 3s * (level - 1)`, minimum `1s`.

### `last_stand_fury`
- Trigger: passive (re-applies while active).
- Scaling source: owner's missing HP.
- Effect: grants tame Strength + Speed, scaling up as owner HP gets lower.
- Damage: indirect via stat buffs.

### `shield_block`
- Trigger: when tame is hurt and cooldown is ready.
- Effect: reduces incoming hit by `65% + 3% * (level - 1)`, capped at `95%`.
- Cooldown: `10s - 1s * (level - 1)`, minimum `1s`.

### `sky_launch`
- Trigger: when owner is hurt and tame is within `3` blocks of owner.
- Effect: nearby hostiles are launched upward (and slightly outward), with launch strength scaling by level.
- Radius: `2.5 + 0.20 * level` blocks.
- Cooldown: `12.0s`.
## DI Migrated Abilities

### `immunity_frame`
- DI number: `3`
- Class weight: high chance for `TANKER`
- Leveling: infinite
- Cooldown: passive/reactive (no active cast cooldown)
- Effect scaling:
  - On first incoming hit, grants invulnerability for `20 + 20 * level` ticks.
  - While active, incoming attacks are canceled.
- DPS example:
  - L1: `0.00 DPS`
- Numeric example:
  - L1: invulnerability window `40 ticks = 2.0s`

### `deflection`
- DI number: `4`
- Class weight: high chance for `TANKER`
- Leveling: cannot level
- Cooldown: passive/reactive (no active cast cooldown)
- Effect scaling:
  - Current runtime has no level scaling.
  - Incoming projectile is canceled/deflected.
  - Deflected projectile velocity is multiplied by `-0.2` (reversed, `20%` speed).
- DPS example:
  - L1: `0.00 DPS`
- Numeric example:
  - L1: speed factor `-0.2`, yaw/pitch `+180` on deflect

### `defusal`
- DI number: `21`
- Class weight: high chance for `TANKER`, mid chance for `PROTECTOR` (`SUPPORTER`)
- Leveling: infinite
- Trigger: cancels nearby explosions when off cooldown
- Cooldown scaling:
  - Base: `5.0s` at level `1`
  - Per level: `-1.0s`
  - Minimum: `0.0s`
- Range scaling:
  - Base: `10` blocks
  - Every 3rd level: `+10` blocks (`level / 3` steps)
- DPS example:
  - L1: `0.00 DPS`
  - L5: `0.00 DPS`
- Numeric example:
  - L1: cooldown `5.0s`, range `10`
  - L3: cooldown `3.0s`, range `20`
  - L5: cooldown `1.0s` (min), range `20`
  - L6: cooldown `0.0s`, range `30`

### `shadow_hands`
- DI number: `19`
- Class weight: high chance for `MAGE`
- Leveling: infinite
- Damage type: `single-target`
- Effect scaling (current runtime):
  - Hands active = `+1 every level`
  - Per-hand windup = `10 ticks`
  - Global hand-start cooldown = `5 ticks`
  - Damage per landed punch = `clamp(level, 2..4)`
- DPS example:
  - L1/L3/L5: not defined by custom direct damage formula
- Numeric example:
  - L1: `1` hand, `2` damage per landed punch
  - L3: `2` hands, `3` damage per landed punch
  - L5: `3` hands, `4` damage per landed punch (damage clamp max)

### `psychic_wall`
- DI number: `25`
- Class weight: high chance for `PROTECTOR`
- Leveling: infinite
- Effect scaling (current runtime):
  - Wall width = `level + 1`
  - Wall lifespan = `100 * level` ticks
  - Cooldown after cast = `200 * level + 40` ticks
- DPS example:
  - L1: `0.00 DPS`
- Numeric example:
  - L1: width `2`, lifespan `5s`, cooldown `12s`
  - L3: width `4`, lifespan `15s`, cooldown `32s`
  - L5: width `6`, lifespan `25s`, cooldown `52s`

### `healing_aura`
- DI number: `30`
- Class weight: high chance for `PROTECTOR` (`SUPPORTER`)
- Leveling: infinite
- Effect scaling (current runtime):
  - Applies Regeneration with amplifier `level - 1`
  - Active pulse window = `200 ticks` (`10s`)
  - Inactive downtime = `600..1199 ticks` (`30.0s..59.95s`) before next cycle
- DPS example:
  - L1: `0.00 DPS`
- Numeric example:
  - L1: Regeneration I for `10s` active window, then `30-60s` downtime cycle
  - L3: Regeneration III for `10s` active window, then `30-60s` downtime cycle
