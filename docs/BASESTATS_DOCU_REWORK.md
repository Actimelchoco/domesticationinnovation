# Base Stats Documentation

This document describes the current implemented base-stat reward behavior in code.

It is intended to match the current implementation, not a proposed rework.

## General
- Base stats are one of the three level-up reward categories:
  - `base stat`
  - `attribute`
  - `ability`
- Category chance is now class-specific.
- Current examples:
  - `TANKER` / `ASSASSIN`: `base 90`, `attribute 10`, `ability 10`
  - `SUPPORTER` / `MAGE` / `SHOOTER`: `base 85`, `attribute 10`, `ability 15`
  - `DPS`: `base 95`, `attribute 3`, `ability 2`
- When a base-stat reward is selected, exactly one base stat is rolled from the base-stat reward table.

## Base Stat Rewards

| Base Stat | Attribute | Current Per Roll |
|---|---|---:|
| `HP` | `MAX_HEALTH` | `+1.0` |
| `Damage` | `ATTACK_DAMAGE` | `+1.0` |
| `Speed` | `MOVEMENT_SPEED` | `+0.01` |
| `Armor` | `ARMOR` | `+1.0` |
| `Armor Toughness` | `ARMOR_TOUGHNESS` | `+1.0` |
| `Attack Knockback` | `ATTACK_KNOCKBACK` | `+0.5` |
| `Knockback Resistance` | `KNOCKBACK_RESISTANCE` | `+0.05` |

Special tuning:
- `DPS` class `Damage` rewards use `+0.8` per roll instead of `+1.0`
- this is intentional to keep average `bonusDamage` near `15` by level `200`

## Default Base-Stat Roll Weights

These are the internal weights used when the reward category is already `base stat`.

| Base Stat | Current Weight |
|---|---:|
| `HP` | `70.0` |
| `Damage` | `3.0` |
| `Speed` | `3.0` |
| `Armor` | `3.0` |
| `Armor Toughness` | `3.0` |
| `Attack Knockback` | `3.0` |
| `Knockback Resistance` | `3.0` |

## Class Modifiers For Base Stats

These modifiers apply to the internal base-stat roll weights.

### `TANKER`
- `HP x3.0`
- `Armor x3.0`
- `Armor Toughness x3.0`

### `DPS`
- `Damage x3.0`
- all other base stats use default weight

### `ASSASSIN`
- `Speed x3.8`
- all other base stats use default weight

### `SUPPORTER`
- `Speed x1.8`
- `Knockback Resistance x2.8`
- all other base stats use default weight

### `MAGE`
- no class-specific base-stat modifier

### `SHOOTER`
- `Speed x1.8`
- `Attack Knockback x2.8`
- all other base stats use default weight

### `MANIAC`
- all base stats `x0.50` equally
- because every base stat gets the same multiplier, this does not change which specific base stat is chosen inside the base-stat roll

### `ATTRIBUTER`
- all base stats `x0.40` equally
- because every base stat gets the same multiplier, this does not change which specific base stat is chosen inside the base-stat roll

## Resulting Effective Base-Stat Weights

### Default / neutral classes
- `HP 70.0`
- `Damage 3.0`
- `Speed 3.0`
- `Armor 3.0`
- `Armor Toughness 3.0`
- `Attack Knockback 3.0`
- `Knockback Resistance 3.0`

### `DPS`
- `HP 70.0`
- `Damage 9.0`
- `Speed 3.0`
- `Armor 3.0`
- `Armor Toughness 3.0`
- `Attack Knockback 3.0`
- `Knockback Resistance 3.0`

### `TANKER`
- `HP 210.0`
- `Damage 3.0`
- `Speed 3.0`
- `Armor 9.0`
- `Armor Toughness 9.0`
- `Attack Knockback 3.0`
- `Knockback Resistance 3.0`

### `ASSASSIN`
- `HP 70.0`
- `Damage 3.0`
- `Speed 11.4`
- `Armor 3.0`
- `Armor Toughness 3.0`
- `Attack Knockback 3.0`
- `Knockback Resistance 3.0`

### `SUPPORTER`
- `HP 70.0`
- `Damage 3.0`
- `Speed 5.4`
- `Armor 3.0`
- `Armor Toughness 3.0`
- `Attack Knockback 3.0`
- `Knockback Resistance 8.4`

### `SHOOTER`
- `HP 70.0`
- `Damage 3.0`
- `Speed 5.4`
- `Armor 3.0`
- `Armor Toughness 3.0`
- `Attack Knockback 8.4`
- `Knockback Resistance 3.0`

### `MANIAC`
- no meaningful class-specific base-stat bias inside the base-stat pool

### `ATTRIBUTER`
- no meaningful class-specific base-stat bias inside the base-stat pool

## Behavior Notes
- On `HP` reward, the tame is healed to full health after the stat increase.
- Base-stat gains are stored as persistent bonus stats in tame progress data.
- Those stored bonus stats are:
  - `bonusHealth`
  - `bonusDamage`
  - `bonusSpeed`
  - `bonusArmor`
  - `bonusArmorToughness`
  - `bonusKnockback`
  - `bonusKnockbackResist`
- Base-stat rewards permanently increase the tame’s tracked bonuses until explicitly removed by admin/reset/death-rollback logic.

## Important Interpretation Notes
- `HP` is overwhelmingly favored in the current implementation.
- `Attack Knockback` currently uses `+0.5` per roll, not `+0.25`.
- `MANIAC` and `ATTRIBUTER` have equal base-stat multipliers across the whole base-stat pool, so those multipliers do not create a specific favored base stat and are omitted from the effective-weight summary.
- The only classes with meaningful specific base-stat favoritism are:
  - `DPS` for `Damage`
  - `SHOOTER` for `Speed`
