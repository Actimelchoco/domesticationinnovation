Shared guardian chest and interface collars (v58)
================================================

Both vertical layouts work:

    Control inventory       OR       Drum
    Drum                             Control inventory
    3 x 3 chest layer                3 x 3 chest layer

The base is nine placed chest blocks in one flat layer, centered below the lower
block. Modded chest blocks, including Sophisticated Storage diamond chests, are
accepted. The control inventory uses Forge's item-handler capability. Nine chest
items inside the inventory, or a stone/ore floor, do not count as the base.

The chest and supporting drum are owner neutral. Multiple owners can each put
their own drum item named `<owner>:<guardian deployment group>` in the same
control inventory, together with adoption papers already bound to their tames.
The player who placed the physical drum does not restrict who can use the chest.
The named owner must match the tame's current owner. Online owners and cached
profiles from bound papers resolve owner names without network lookups.

Papers become permanent guardian controls, named `<group>: <tame name>`. They
cannot transfer ownership afterwards. Removing them stops that chest's future
redstone control and remote feeding; it does not release a deployed guardian.
Paper conversion updates the actual inventory slot, including modded inventory
change listeners and saved contents.

Anyone holding a converted paper can manage its bound tame:

* Right click a ground/block face: save that tame's post in the paper's group.
* Sneak right click a block or the air: deploy the tame at that post.
* Punch a block or entity: release guardian duty, teleport home, and sit. The
  punch does not damage that block/entity. Empty-air punches cannot be detected
  by an unmodified Minecraft client/server connection, so hit a block or entity.

These actions never transfer tame ownership or consume the paper. Bodyguards
remain protected. The paper must still match the registered owner: an admin
ownership change or lost/wild tame invalidates an old owner's control paper.

Redstone ON at the physical drum or control inventory deploys linked tames of
all configured owners. OFF recalls them home sitting. Existing group posts are
reused. Missing posts must be set with the paper or existing guardian commands.
An active ranked recall forfeits its round before deployment; regular duels keep
their command lock. Assigned bodyguards are skipped. Dead/stored tames are skipped.

For an owner with several group labels, an ordinary bound adoption paper uses
its sole matching owner/group, or its sole matching saved group post. Ambiguous
papers are left unchanged. Already converted papers match their exact group.
Conflicting group papers for the same tame are not activated together.

Normal chest x drum feeding still works. Linked guardians also receive the
control inventory's food remotely, across dimensions or while unloaded, using
their learned/configured diet. The nine base chests are structure requirements,
not additional food inventories. Existing overlap deactivation still applies.

Use `/tames chestxdrum status` (or `/tames chestxdrum`) near the setup. It reports
the detected base, power state, linked tames, missing posts, bodyguards, dead or
stored tames, unbound papers, and unmatched/ambiguous papers. Readiness, paper
conversion and invalid setup messages also appear automatically for relevant
online owners and nearby players. Messages are deduplicated and throttled.

Performance remains bounded to one setup scan and one tame operation per tick;
requests coalesce by chest/tame. Cold source/destination chunks use the shared
guardian loading budget. Remote feeding never creates or loads a tame entity.

Collar tags now support the specific-entity interaction event used by interface
tames, with strict owner-UUID checks. Collar/tier data and snapshots are saved
immediately. Hungry tames can receive collars, while duel locks, collar refusal
tags and the collar config setting remain respected.

Replace the previous DI extension jar with v58 and keep only one DI jar.
No manual config edits or new client protocol/registry entries are required.
