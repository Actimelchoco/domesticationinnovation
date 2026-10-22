# Guardian chest bodyguards v68

Keep the normal `<owner>:<guardian group>` drum and linked tame papers in a
valid, powered guardian chest. Add a drum named:

```
bodyguard Rex:Jana
```

This assigns the linked guardian named Rex to follow and defend Jana while
Jana is online. Tame names and player names match without regard to case; tame
names can contain spaces. The tame must be deployed at that chest group's
saved guardian post. Several drums can configure several tames.

Only the three highest-level eligible guardian bodyguards across all loaded
guardian chests can protect a given player. Level ties use stable tame IDs.
Lower-level tames remain ordinary guardians. Level changes, deaths, stored
tames and player login/logout are reconsidered once per second. A duplicate
tame name in one chest, conflicting rule drums, or conflicting protected
players for the same tame do not choose an arbitrary tame/player.

The normal bodyguard roster remains separate: normal bodyguards plus up to
three guardian chest bodyguards can protect the same player. These escorts
retain their original owner, guardian status, post, monster_hunter setting,
guardian command locks and chest feeding. They do not enter the normal
bodyguard roster or receive its roster-only bonuses. Their movement and target
selection follow the protected player instead of returning to the post while
the escort is active. The protected player and their tames are protected from
the escort's damage, including attributed projectile/explosion damage.

When the player goes offline, the tame loses its top-three slot, the rule or
paper is removed, or the controller becomes invalid/unloaded, the escort ends
and normal guardian behavior resumes. Former escorts return to their post
through the existing throttled transfer path. Power OFF retains the chest's
normal release-and-home behavior. Hungry guardians still rest; the drum does
not bypass food requirements. Guardian chests must remain loaded, as before.

Performance: a once-per-second selection pass only reads indexed chest rules,
with a shared online-player name map and a bounded top-three list per player.
Loaded escorts stagger movement/target work to ten ticks. Cold body recovery,
return transfers and cross-dimension following share existing guardian chunk
and transition budgets. No global tame or entity searches are added to the
selection pass, and no permanent chunk tickets are added.

Validation: all 47 required Forge GameTests passed, including real drum/paper
parsing, login/logout, global selection across controllers, independent normal
bodyguards, level promotion, return-to-post, power OFF, vanilla/interface
targeting, protected-player projectile/explosion safety, and prior guardian,
chest, duel and respawn regressions.

Install v68 in place of the earlier DI extension JAR. No configuration or
network protocol changes are required. In-game help: `/tames chestxdrum system`.
