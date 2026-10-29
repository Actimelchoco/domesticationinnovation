# Held-item tame weapons

Full support is included for tamed foxes, Alex's Mobs raccoons, and Alex's Caves
Vallumraptors:

- Pick up one dropped sword or axe into the existing main hand, without adding an inventory.
- Approach nearby weapons when moving, or collect reachable weapons within two blocks while sitting.
- Keep held weapons and seals rather than replacing them with food. Unarmed food behavior remains native.
- Use standard melee weapon attributes and enchantment hooks. Raccoons and Vallumraptors retain their native attack animations.
- Held weapons do not lose durability. Existing damage is preserved.
- Holding a sword, axe, bow, crossbow, or trident in either hand increases saturation
  costs by 50%. Two weapons still give a single 1.5x multiplier. Animights are exempt.
  The surcharge stacks multiplicatively with existing level and pack costs, applies
  to activity, healing, and Ultimine, and ends immediately when the weapons are removed.
- Owners sprint and right-click with an empty main hand to retrieve the exact held stack.
- The next credited kill unseals equipped L2 sealed items, preserving their original data.
- Weapon pickup/retrieval retain duel restrictions. Wild mobs keep native behavior.

Raccoons do not wash/eat weapons; ordinary commands, carpets and food interactions
remain available while armed. Vallumraptors retry nearby native grabs so their
animation can finish even if the original item-target goal stops early.

## Recognizing other modded tames

Retrieval, durability protection and kill-unsealing recognize existing standard
main-hand equipment on owned mobs. Ownership is detected through Minecraft's
TamableAnimal, this mod's ModifedToBeTameable adapter, or another mod's standard
OwnableEntity implementation with an owner UUID. These shared features do not
require a hardcoded entity ID. Animights keep their separate equipment inventory
and durability/recovery behavior.

Every Minecraft mob has equipment slots, including mobs that never display or
use held items. Slot availability, pickup flags, or an arbitrary method name
cannot reliably prove that an unknown species can render and safely use weapons.
The feature therefore does not automatically give unknown mobs weapon pickup.
Custom item inventories, custom ownership systems and custom combat/consumption
code may require an additional adapter.

## Listing supported types

Admins can run `/tames admin canPickUpWeapons` (permission level 2; console also
supported). It prints a sorted list of installed entity IDs and names, labeled
`held item` or `equipment inventory`. It reflects the loaded pickup/exclusion
tags and includes installed Animights Canito/Catwain inventory support. It does
not infer pickup support merely because an arbitrary mob has equipment slots.

## Enabling another reviewed pickup type

A datapack can extend this entity-type tag:
`data/domesticationinnovation/tags/entity_types/held_item_weapon_tames.json`

```json
{
  "replace": false,
  "values": [
    {"id": "examplemod:item_carrying_pet", "required": false}
  ]
}
```

The type must use normal main-hand equipment and recognized ownership. The tag
adds shared weapon pickup; it cannot automatically adapt an unknown custom
attack animation or prevent a custom item-consumption routine. Review those paths
before enabling a new type. Native vanilla mob melee already supplies weapon
modifiers/enchantment hooks; custom direct-damage attacks may need a mixin.

To exclude a type from shared held-item rules, add it to:
`data/domesticationinnovation/tags/entity_types/held_item_weapon_excluded_tames.json`

## Validation

18 integration tests run against Alex's Caves 2.0.2, Alex's Mobs 1.22.9, and
Citadel 2.6.3. They cover the three supported species and a generic OwnableEntity
carrier, native/custom melee with Smite/Fire Aspect/Knockback, pickup in standing
and sitting states, exact NBT transfer, food/washing behavior, native raccoon
commands/carpet interactions, owner retrieval, kill recovery, malformed seals,
wild exclusions, and duel locks. L2 seals use a registry fixture with the installed
L2 Hostility 2.5.19 item ID and NBT format; the sealing trait itself is not loaded.

```powershell
.\gradlew.bat --no-daemon -I gradle/heldItemTameWeaponsRegression.init.gradle '-PvallumraptorModsDir=C:/Users/bruns/Documents/linggango server/mods' runGameTestServer --offline
```

After tests, release builds omit test fixtures and optional test dependencies:

```powershell
.\gradlew.bat --no-daemon cleanCompileJava cleanProcessResources build --offline
```
