# TamesLevel Mod - General Documentation

## What This Mod Adds
TamesLevel adds RPG-style progression to tamed mobs:
- level and XP progression,
- base stat growth,
- attributes,
- abilities,
- AI behavior modes,
- tame commands for control and management.

The mod tracks each tame with persistent data (level, class, stats, abilities, attributes, mode, group, etc.).

## Core Progression
- Tames gain XP from combat contribution (kills and assists).
- On level up, one reward category is rolled:
  - base stat,
  - attribute,
  - ability.
- Milestones:
  - every 20th level: guaranteed attribute roll,
  - every 30th level: guaranteed ability roll.

## Reward Chances (Current)
- Base stat: 93%
- Attribute: 5%
- Ability: 2%

Class multipliers and class-specific weight biases can modify these outcomes.

## Base Stats
Base stats are permanent direct stat increases (health, damage, armor, speed, knockback, etc.).
See: `BaseStatsDocu.md`

## Attributes
Attributes are progression modifiers and effect systems (for example: speed, resistance, lifesteal, killer/pacifist, totem, fang effects, cooldown/ability scaling).
- Attributes are uncapped.
- Owned attributes are weighted much higher in rolls:
  - below level 5: very high bias,
  - level 5 and above: moderate bias.
See: `AttributesDocu.md`

## Abilities
Abilities are active/reactive combat skills (projectiles, beams, buffs, teleports, utility).
- Abilities are uncapped.
- Owned abilities are weighted much higher in rolls:
  - below level 5: very high bias,
  - level 5 and above: moderate bias.
- Ability use is no-grief by design (no block breaking; lightning/fireball behavior is sanitized).
See: `AbilitiesDocu.md`

## Classes
Each tame gets a class that biases roll probabilities and reward weights.
Available classes:
- `TANKER`
- `DPS`
- `ASSASSIN`
- `PROTECTOR`
- `MAGE`
- `SHOOTER`
- `MANIAC`
- `ATTRIBUTER`

See exact multipliers and biases: `ClassesDocu.md`

## AI Modes and Behavior
Modes control combat targeting logic and behavior style (`boss`, `bodyguard`, `monster_hunter`, `passive`, `aggressive`).

Important runtime layers:
- mode goals,
- escape/runaway behavior,
- protection zone behavior.

See detailed mode behavior: `ModesDocu.md`

## Groups, Zones, and Emergency Controls
- Groups let you batch-control tames.
- Zones create a protected area behavior anchor.
- Emergency commands quickly switch nearby tames:
  - berserk,
  - run,
  - passive.

## Commands
Main root commands:
- `/tames ...`
- `/tame ...` (alias)

Notable control/recovery commands:
- `/tames tp ...` for loaded tame teleport control.
- `/tames recover <name>` for unloaded tame recovery (same UUID, exact saved copy, costs 25% invested tame XP).
- `/tames info ability <id>` and `/tames info attribute <id>` to print exact documentation sections in-game.

Use command reference:
- `CommandDocu`

## Data + Persistence
Each tame has persistent saved data (registry-backed), including:
- identity (name, type, owner),
- progression (level/xp/class),
- combat stats,
- modes/group/zone,
- attributes and abilities with levels,
- cooldown state,
- snapshot data for reset/restore/recovery mechanics (entity snapshot + progression snapshot).

## Dimension Follow Behavior
- After a player changes dimension, loaded owned tames that are in follow mode (not sitting) are auto-teleported to the owner after ~1 second.
- Unloaded tames are not auto-teleported; use `/tames recover <name>`.

## Debug/Feedback
When abilities trigger, owner chat can receive concise usage lines in the format:
- `<TameName>: <ability_id>`

## Related Documentation Files
- `CommandDocu`
- `ModesDocu.md`
- `BaseStatsDocu.md`
- `AttributesDocu.md`
- `AbilitiesDocu.md`
- `ClassesDocu.md`
