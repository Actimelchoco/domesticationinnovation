# Classes Documentation

## General
- Classes are assigned randomly when a tame is first registered.
- Available classes: `TANKER`, `DPS`, `ASSASSIN`, `PROTECTOR`, `MAGE`, `SHOOTER`, `MANIAC`, `ATTRIBUTER`.
- Assignment is uniform unless changed by admin command.
- Classes affect random reward weighting.
- Current global class multipliers:
  - `MANIAC`: base stat `x0.50`, attribute `x1.8`, ability `x3.0`
  - `ATTRIBUTER`: base stat `x0.40`, attribute `x3.5`, ability `x0.6`
- Other classes are neutral by default unless a reward-specific weight is listed in that class section.
- Admin override: `/tames admin setClass <pet> <class>`

### `TANKER`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific weights: none currently implemented.

### `DPS`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific attribute weights:
  - `pierce x4.0` (high chance)

### `ASSASSIN`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific attribute weights:
  - `victim_siphon x4.0` (high chance)
  - `pierce x2.5` (mid chance)

### `PROTECTOR`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific weights: none currently implemented.

### `MAGE`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific weights: none currently implemented.

### `SHOOTER`
- General role: neutral class in current code.
- Global multipliers: none.
- Reward-specific weights: none currently implemented.

### `MANIAC`
- Global multipliers:
  - base stat `x0.50`
  - attribute `x1.8`
  - ability `x3.0`
- Reward-specific overrides:
  - all attributes additionally inherit the `x1.8` class multiplier
  - all abilities additionally inherit the `x3.0` class multiplier

### `ATTRIBUTER`
- Global multipliers:
  - base stat `x0.40`
  - attribute `x3.5`
  - ability `x0.6`
- Reward-specific overrides:
  - all attributes additionally inherit the `x3.5` class multiplier
  - all abilities additionally inherit the `x0.6` class multiplier
