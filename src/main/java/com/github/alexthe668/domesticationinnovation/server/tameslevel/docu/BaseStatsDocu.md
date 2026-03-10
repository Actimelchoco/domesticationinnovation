# Base Stats Documentation

## Base Stats (Level-Up Reward Category)
When the reward category roll lands on base stats, one of these is increased:
- `HP` (`MAX_HEALTH`) `+2.0`
- `Damage` (`ATTACK_DAMAGE`) `+1.0`
- `Speed` (`MOVEMENT_SPEED`) `+0.01`
- `Armor` (`ARMOR`) `+1.0`
- `Armor Toughness` (`ARMOR_TOUGHNESS`) `+1.0`
- `Attack Knockback` (`ATTACK_KNOCKBACK`) `+0.5`
- `Knockback Resistance` (`KNOCKBACK_RESISTANCE`) `+0.05`

## Base Roll Weights
Default weights used when picking which base stat is increased:
- `HP`: `59.7`
- `Damage`: `20.0`
- `Speed`: `3.0`
- `Armor`: `5.0`
- `Armor Toughness`: `2.0`
- `Attack Knockback`: `5.0`
- `Knockback Resistance`: `5.0`

## Notes
- On `HP` reward, tame health is fully restored to max health.
- These base stat increases are permanent bonus stats stored in tame progress.
- Class-specific weight multipliers can bias which base stat gets picked (see `ClassesDocu.md`).
