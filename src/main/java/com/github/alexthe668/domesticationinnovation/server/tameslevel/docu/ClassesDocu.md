# Classes Documentation

## Available Classes
- `TANKER`
- `DPS`
- `ASSASSIN`
- `PROTECTOR`
- `MAGE`
- `SHOOTER`
- `MANIAC`
- `ATTRIBUTER`

DI naming note:
- `SUPPORTER` in migration notes maps to `PROTECTOR` in current class enum.

## Reward Category Chance Multipliers
Base category chances before class multipliers:
- `base stat`: `0.93`
- `attribute`: `0.05`
- `ability`: `0.02`

Guaranteed milestone rules:
- every level divisible by `30`: guaranteed `ability` roll.
- every level divisible by `20`: guaranteed `attribute` roll.
- if both apply (e.g. level `60`), `ability` guarantee wins.

Class multipliers:
- `TANKER`: base `1.40`, attribute `0.65`, ability `0.50`
- `DPS`: base `1.30`, attribute `0.70`, ability `0.80`
- `ASSASSIN`: base `1.25`, attribute `0.75`, ability `1.15`
- `PROTECTOR`: base `1.20`, attribute `1.20`, ability `0.75`
- `MAGE`: base `0.60`, attribute `1.20`, ability `2.80`
- `SHOOTER`: base `0.90`, attribute `0.80`, ability `1.80`
- `MANIAC`: base `0.45`, attribute `0.90`, ability `3.20`
- `ATTRIBUTER`: base `0.35`, attribute `3.50`, ability `0.55`

## Base Stat Weight Bias by Class
Additional class-specific weighting on top of base stat roll weights:
- `TANKER`: `HP x3.0`, `ARMOR x3.0`, `ARMOR_TOUGHNESS x3.0`
- `DPS`: `DAMAGE x3.0`
- `ASSASSIN`: `SPEED x3.8`, `DAMAGE x2.8`
- `PROTECTOR`: `SPEED x1.8`, `KNOCKBACK x2.8`, `KNOCKBACK_RESIST x2.8`
- `MAGE`: all base stats reduced (`x0.7`), `SPEED x0.8`
- `SHOOTER`: `DAMAGE x1.8`, `SPEED x1.8`
- `MANIAC`: all base stats `x0.50`
- `ATTRIBUTER`: all base stats `x0.40`

## Attribute Weight Bias by Class
- `ASSASSIN`: `speed x3.2`, `strength x3.4`, `killer x4.0`, `lifesteal x3.0`, `firefang x2.6`, `witherfang x3.0`, `lightningfang x1.6`
- `DPS`: `strength x4.0`, `ability_power x3.2`, `killer x3.4`, `lifesteal x2.8`, `firefang x3.0`, `lightningfang x1.6`, `sweeping_edge x4.0`
- `PROTECTOR`: `resistance x4.2`, `regeneration x4.0`, `fire_resistance x3.6`, `poison_resistance x3.6`, `totem x4.2`, `pacifist x3.2`, `positive_effect_steal x2.8`
- `TANKER`: `resistance x4.6`, `regeneration x4.2`, `fire_resistance x3.8`, `poison_resistance x3.8`, `totem x3.6`, `pacifist x2.8`
- `MAGE`: `ability_power x4.2`, `lightningfang x3.2`, `witherfang x3.2`, `firefang x3.0`, `negative_effect_transfer x3.2`, `positive_effect_steal x2.6`
- `SHOOTER`: `speed x3.4`, `strength x3.0`, `killer x3.2`, `lightningfang x2.6`, `firefang x2.6`
- `MANIAC`: all attributes `x1.8`
- `ATTRIBUTER`: all attributes `x3.5`

## Ability Weight Bias by Class
- `ASSASSIN`: `shulker_bullet x2.0`, `wither_skull x2.4`, `bloodlust x3.6`, `berserker x3.2`, `ender_pearl_jump x4.2`,`warden_scream x2.8`
- `DPS`: `bloodlust x4.2`, `berserker x3.9`, `arrow_shot x1.5`, `lightning_strike x1.5`, `warden_scream x1.8`
- `PROTECTOR`: `defensive_aura x4.6`, `battle_strength x3.2`, `elder_guardian_beam x1.8`, `warden_scream x1.8`
- `TANKER`: `defensive_aura x4.8`, `berserker x3.7`, `warden_scream x1.0`, `evoker_fangs x1.8`
- `MAGE`: `dragon_fireball x4.6`, `guardian_beam x4.4`, `elder_guardian_beam x4.2`, `wither_skull x4.0`, `evoker_fangs x3.6`, `lightning_strike x3.6`, `ghast_fireball x3.2`, `shulker_bullet x4.0`, `warden_scream x3.8`
- `SHOOTER`: `crossbow x5.5`, `trident x5.2`, `arrow_shot x5.0`, `blaze_attack x3.6`, `llama_spit x4.2`, `snowball_shot x5.8`, `shulker_bullet x2.0`, `wither_skull x2.4`, `ghast_fireball x2.2`, `evoker_fangs x1.8`, `warden_scream x1.8`
- `MANIAC`: all abilities `x3.0`
- `ATTRIBUTER`: all abilities `x0.6`

## DI Migrated Weight Tiers
- `high chance` = `x4.0`
- `mid chance` = `x2.5`
- `neutral` (not listed) = `x1.0`
- `guaranteed after level N` = forced milestone reward (not random weight based).

## DI Migrated Attribute Weights by Class
- `TANKER`: `fire_resistance x2.5`, `poison_resistance x2.5`, `health_siphon x2.5`, `defusal x4.0`, `blazing_protection x4.0`
- `DPS`: `chain_lightning x2.5`
- `ASSASSIN`: `chain_lightning x4.0`, `frost_fang x2.6` (same as `firefang`), `magnetic x4.0`, `bubbling x2.5`, `amphibious x4.0`, `void_cloud x2.5`, `warping_bite x4.0`, `muffled x2.5`
- `PROTECTOR` (`SUPPORTER`): `linked_inventory x2.5`, `herding x2.5`, `charisma x2.5`, `defusal x2.5`, `ore_scenting x2.5`
- `MAGE`: `chain_lightning x2.5`

DI milestone attributes:
- `gluttonous`: guaranteed attribute after level `30`.
- `tethered_teleport`: guaranteed attribute after level `40`.

## DI Migrated Ability Weights by Class
- `TANKER`: `immunity_frame x4.0`, `deflection x4.0`
- `MAGE`: `shadow_hands x4.0`
- `PROTECTOR`: `psychic_wall x4.0`, `healing_aura x4.0`
