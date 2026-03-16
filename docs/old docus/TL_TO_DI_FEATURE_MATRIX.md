# TL -> DI Feature Mapping Matrix

Legend:
- `Recommend`: proposed action (`Keep`, `Replace`, `Drop`)
- `Obsolete after extension`: `Yes` means TL-only behavior/compat path should be removable once DI extension is stable

## Core Systems
| TL feature | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| Per-tame progression data (level/xp/base stats/abilities/attributes) | Keep | DI persistent migration payload + runtime bridge | Import from TL payload, keep in DI-owned data path | Yes (TL registry dependency) | Core value of TL |
| TL registry saved data as authority | Replace | DI as single authority | One-time import then DI-only writes | Yes | Prevent split-brain data |
| `/tames admin migrateDI dryrun/apply` | Keep (temporary) | TL-side export command | Run during cutover only | Yes | Remove operational dependence after rollout window |
| TL duplicate/unite/recover admin flows | Drop | None (or DI admin tools later) | No import needed beyond final tame state | Yes | Mostly TL maintenance utilities |
| TL duel/debug subsystems | Drop | None | Ignore fields in payload | Yes | Not part of stated DI goal |

## Base Stats
| TL feature | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| Bonus health (`bonusHealth`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus damage (`bonusDamage`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus speed (`bonusSpeed`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus armor (`bonusArmor`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus armor toughness (`bonusArmorToughness`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus attack knockback (`bonusKnockback`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |
| Bonus knockback resist (`bonusKnockbackResist`) | Keep | `TamesLevelFeatureBridge` attribute modifier | Direct map existing key | No | Already implemented path |

## Attributes
| TL feature | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| `ability_power` | Keep | DI ability-scaling multiplier in bridge | Read `attributeLevels.ability_power` | No | High leverage for many abilities |
| `totem` | Keep | Defensive passive handler | Read level and apply cooldown/trigger logic | No | Strong identity feature |
| `regeneration` | Keep | Periodic heal passive | Map level scaling from TL | No | Server-side easy |
| `resistance` / `strength` / `speed` / `jump_boost` / `fire_resistance` | Replace | Vanilla effect-based passive set | Map levels to effect amplifiers | No | Normalize into one consistent passive system |
| `feather_falling` / `explosion_resistance` | Keep | Damage-reduction hooks | Read level on damage events | No | Aligns with armor-like direction |
| `lifesteal` / `sweeping_edge` / `smite` / `bane_of_arthropods` | Replace | Combat modifier hooks | Map to DI combat pipeline | No | Keep gameplay, simplify implementation surface |
| `positive_effect_steal` / `negative_effect_transfer` | Replace | Proc-style status handlers | Keep level/chance semantics where possible | No | Maintain fantasy, reduce TL coupling |
| `killer` / `pacifist` / `bosskiller` | Replace | Contextual damage scaling module | Map formula approximately | No | Could rebalance later |
| `killexploder` | Drop | None (or optional later) | Ignore key during runtime | Yes | Friendly-fire AoE grief risk |
| `lightningfang` / `firefang` / `witherfang` | Keep | On-hit proc hooks | Map level to effect strength/duration | No | Straightforward server logic |
| `emergency_cooldown_reduction` | Replace | Shared cooldown controller | Map trigger chance and clamp | No | Works better as cross-ability utility |

## Abilities
| TL feature | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| `damage_intercept` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `guardian_repulse` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `sky_launch` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `shield_block` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `defensive_aura` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `last_stand_fury` | Keep | `TamesLevelFeatureBridge` | Already read from `abilityLevels` | No | Implemented |
| `battle_strength` / `berserker` / `bloodlust` | Keep | Buff proc module | Read levels and implement hooks | No | High value, low client impact |
| Projectile abilities (`arrow_shot`, `snowball_shot`, `blaze_attack`, `trident`, `crossbow`, `llama_spit`) | Replace | Unified projectile ability executor | Map IDs to shared server executor | No | Avoid duplicated handlers |
| Beam/burst abilities (`guardian_beam`, `elder_guardian_beam`, `warden_scream`) | Keep | Dedicated server handlers | Keep ID/level mapping | No | Distinct gameplay niches |
| Explosive abilities (`creeper_explosion`, `dragon_fireball`, `ghast_fireball`) | Replace | Sanitized no-grief AoE module | Map with no-block-damage guarantee | No | Safety-first behavior |
| `wither_skull`, `evoker_fangs`, `shulker_bullet`, `ender_pearl_jump`, `lightning_strike` | Keep | Ability-specific handlers | Map one-to-one by ID | No | Good TL identity transfer |

## Modes and AI
| TL feature | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| `passive` behavior guard | Keep | Server combat-event guard | Map mode flag in runtime data | No | Important control mode |
| `aggressive`, `monster_hunter`, `bodyguard`, `boss` | Replace | DI goal installer + mode selector | Map nearest equivalent semantics | No | Keep behavior, modernize internals |
| `default_plus` | Drop | DI default follow/combat behavior | Ignore if present | Yes | Transitional TL tuning mode |
| Zone/group emergency commands | Replace | Minimal DI command set | Optional import of group labels | Yes (for TL command plumbing) | Keep only if still needed operationally |

## Collar Tag and Enchantments
| TL/DI area | Recommend | DI target | Migration handling | Obsolete after extension | Notes |
|---|---|---|---|---|---|
| Collar tags accept DI pet enchantments | Replace | Vanilla armor-related enchantments only | Block new non-vanilla applications | No | Matches your new rule |
| Existing DI collar enchantments on items/pets | Replace | Attribute/ability mapping table | Convert where possible, else remove safely | No | Must be deterministic and logged |
| TL compatibility glue added only for DI interaction | Drop | None | Remove after stable migration window | Yes | Explicit cleanup target |

## Immediate Implementation Order (Recommended)
1. Finalize migration reliability and idempotency.
2. Lock collar tag rules to vanilla armor-related enchantments.
3. Complete ability parity for high-value defensive/proc features.
4. Add mode mapping with minimal DI command/control surface.
5. Remove TL-only compatibility code after one release window.
