# Guardian drum formation and quiet automation v70

The control block supports Forge ITEM_HANDLER inventories (including sided-only
handlers) and ordinary Minecraft Containers. The inventory must allow the rule
drums, papers and food to be inserted/extracted; a machine with incompatible
slot restrictions will not work as an unrestricted chest.

Replace the old nine-inventory foundation with a flat 3x3 layer of nine drums.
Place one control inventory directly above the centre drum (or use the mirrored
layout, with the inventory below the centre drum). Keep the named group drum,
papers and optional protect/bodyguard drums inside that inventory as before.

Top view of the drum layer:

```
D D D
D D D   Control inventory is above/below the centre D.
D D D
```

Ordinary chest x drum uses the same supported inventory types. Its single drum
and optional horizontal extension drums remain valid for feeding. A five-drum
cross does not activate guardian automation; the full nine-drum square does.
Inventories do not substitute for missing drums. Existing guardian chests built
with the old inventory foundation need rebuilding. No config edits are needed.

The outer drums route redstone to the guardian controller rather than issuing
independent broad tame commands, including while the formation is incomplete.
Background scans, conversion, redstone actions and bodyguard role changes stay
silent. Delayed paper-request completion/timeout chat was also removed. Manual
status/help commands and immediate paper-use feedback remain available.

All v68 guardian bodyguard features are retained. Validation: all 48 required
Forge GameTests passed, including actual Sophisticated Storage inventories,
barrels, sided/generic handlers, normal versus guardian drum formations,
inverted placement, quiet scans and the prior guardian/bodyguard regressions.

Replace the previous DI extension JAR with v70; keep only one DI extension JAR.
