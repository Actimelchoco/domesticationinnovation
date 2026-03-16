# General Documentation Rework

This is the proposed replacement `GeneralDocu` for the balancing update.

It is a design document, not the current implementation.

## Compatibility Rule
All `tamesLevel extension` work should preserve join compatibility for players running plain `Domestication Innovation 1.7.1` without the extension.

This means:
- keep the same `modId`
- avoid version and metadata changes that cause Forge mismatch rejections
- avoid client-required registry, networking, or sync changes unless compatibility is intentionally being broken
- treat compatibility with non-extension `DI 1.7.1` clients as the default requirement

## Overview
The rework changes TamesLevel progression so that:
- base stats are the main progression layer
- attributes are rarer specialization
- abilities are rarer active identity
- offensive power is less dependent on raw live damage stats
- ability and attribute scaling depend more on their own level

## Core Progression Identity
Target reward split:
- `base stat`: `80%`
- `attribute`: `10%`
- `ability`: `10%`

This means:
- most levels improve the tame's core body and combat stats
- some levels unlock specialization
- abilities and attributes feel more valuable because they are rarer

## Base Stats
Base stats are permanent direct stat increases:
- `HP`
- `Damage`
- `Speed`
- `Armor`
- `Armor Toughness`
- `Attack Knockback`
- `Knockback Resistance`

New direction:
- base stats should matter a lot
- `Damage` should still matter for real melee hits
- `Damage` should no longer dominate offensive scaling for abilities
- `Damage` base-stat weight should be only `5.0`

See:
- [`BASESTATS_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\BASESTATS_DOCU_REWORK.md)

## Attributes
Attributes are passive or conditional modifiers that shape how the tame fights, survives, or supports.

New direction:
- attributes should scale mainly with attribute level
- offensive attributes should not scale heavily from actual live attack damage
- rider and proc attributes should mostly use:
  - proc chance
  - duration
  - flat rider damage
  - caps
  - conditional strength

Special rule:
- `lifesteal` should heal from actual damage dealt by real base attacks only
- `lifesteal` should not trigger from abilities

See:
- [`ATTRIBUTE_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ATTRIBUTE_DOCU_REWORK.md)

## Abilities
Abilities are active attacks, heals, or support actions.

New direction:
- abilities should scale much less from the tame's actual damage stat
- abilities should scale mainly from:
  - ability level
  - cooldown
  - role
  - target coverage
  - utility strength
  - stored TamesLevel `damage bonus`

Wolf baseline:
- vanilla wolf orientation baseline = about `2 DPS`

Target offensive balance:
- average level 1 single-target offensive ability = about `1 DPS`
- average level 5 single-target offensive ability = about `2 DPS`

AoE rule:
- the more targets an ability can hit, the lower its single-target DPS should be

Utility rule:
- the more utility, control, healing, safety, or mobility an ability provides, the lower its damage budget should be

See:
- [`ABILITY_BALANCING_PLAN.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ABILITY_BALANCING_PLAN.md)
- [`ABILITY_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ABILITY_DOCU_REWORK.md)

## Damage Bonus
`damage bonus` remains relevant, but its role changes.

New direction:
- use stored TamesLevel `damage bonus`
- do not balance most abilities around actual live `ATTACK_DAMAGE`
- target contribution:
  - every `1 damage bonus` adds about `+2%` offensive effectiveness

This keeps:
- offensive scaling understandable
- tame species easier to compare
- modded base-stat weirdness under control

## Level Scaling

### Ability effectiveness
Every `5` ability levels, effectiveness doubles.

For levels `1-5`:
- L1 = `1.00x`
- L2 = `1.25x`
- L3 = `1.50x`
- L4 = `1.75x`
- L5 = `2.00x`

This same general philosophy should also be used for attribute scaling where it makes sense.

### Attribute effectiveness
Attributes should feel clearly stronger with level.

Preferred tools:
- stronger proc chance
- stronger duration
- stronger radius
- stronger threshold
- stronger reduction
- stronger flat rider damage

Not preferred:
- large hidden scaling from live attack damage

## New Ability vs Existing Ability Progression
When an ability roll happens:
- early new ability chance = `10%`
- once an owned ability reaches level `5`, new ability chance = `20%`

This means:
- early progression usually deepens existing identity
- broader ability kits open up later

## Guaranteed Milestone Rewards
Remove guaranteed ability/attribute injections every `30` levels.

New direction:
- no forced milestone grants
- progression should come from the normal roll system

## Classes
Classes should bias progression, not hard-lock it.

Classes should influence:
- favored base stats
- favored attributes
- favored abilities

Classes should feel like identity guidance, not scripted builds.

See:
- [`CLASSES_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\CLASSES_DOCU_REWORK.md)

## Design Rules Summary
- base stats are the main progression layer
- `Damage` base-stat weight should only be `5.0`
- offensive abilities scale mostly from ability level and `damage bonus`
- offensive attributes scale mostly from attribute level
- `lifesteal` uses real base-hit damage only
- AoE abilities must do less single-target DPS
- utility-heavy abilities must do less damage
- no guaranteed milestone ability/attribute grants every 30 levels

## Recommended Reading Order
1. [`GENERAL_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\GENERAL_DOCU_REWORK.md)
2. [`BASESTATS_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\BASESTATS_DOCU_REWORK.md)
3. [`ATTRIBUTE_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ATTRIBUTE_DOCU_REWORK.md)
4. [`ABILITY_BALANCING_PLAN.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ABILITY_BALANCING_PLAN.md)
5. [`ABILITY_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\ABILITY_DOCU_REWORK.md)
6. [`CLASSES_DOCU_REWORK.md`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\docs\CLASSES_DOCU_REWORK.md)

## Next Step
Turn these docs into code changes in:
- [`LevelSystem.java`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\src\main\java\com\github\alexthe668\domesticationinnovation\server\tameslevel\leveling\LevelSystem.java)
- [`TameAbilityEvents.java`](C:\Users\bruns\Documents\Workspace\DomesticationInnovation-main\src\main\java\com\github\alexthe668\domesticationinnovation\server\tameslevel\events\TameAbilityEvents.java)
