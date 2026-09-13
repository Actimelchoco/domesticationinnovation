# Primitive Mobs compatibility checks

These checks run against the actual Primitive Mobs 1.0.0 JAR on a disposable
Forge 1.20.1 server. The test mod stops the server after printing
`PRIMITIVE_COMPAT_TESTS_PASS` or `PRIMITIVE_COMPAT_TESTS_FAIL`.
Never install the test JAR in a playable world.

Build the production and test JARs:

```powershell
.\gradlew.bat -I tools/primitive_tests/tests.gradle build primitiveCompatTestJar reobfPrimitiveCompatTestJar --offline
```

Use a separate server directory with its own world, configs, loopback interface
and available port. Install Citadel, Primitive Mobs 1.0.0, the built DI JAR and
`build/libs/primitive-compat-tests.jar` there, then start Forge normally.

The checks cover all five supported entity types, carrot health/food restrictions,
owner protection, synced commands, individualized bases, level reward reapplication,
spawn effects and modifiers, NBT save/load, hostile target goal removal, surviving
creeper explosions, player versus ownerless spawn eggs, and player versus nonplayer
thrown eggs. The test JAR is separate from the production artifact.

Compatibility behavior: native tame-capable baby spiders, chameleons, festive
creepers, support creepers and rocket creepers participate in DI's existing pet
registry. Only player-used eggs auto-tame; wild mobs require a sinister carrot
below 10 HP. Spawn effects become innate effects on tamed mobs. Existing mobs
without a saved baseline use their attributes/effects when first loaded with this
integration; previously lost spawn rolls cannot be reconstructed.

`/tames admin reset primitiveMobs` (permission level 2) clears all Primitive Mobs
registry entries, death records, temporary archives, ranked references and queued
bed respawns, including legacy type spellings and missing entity types. It clears
old pet ownership on loaded mobs and when unloaded mobs return. New taming still
works. The tests execute this command only in the disposable test world.

Raw Dodo (`primitive_mobs:dodo`) is a preferred food. Hunger adds the existing
level surcharge for `max(0, original max HP - 20)` to the actual-level surcharge;
the two surcharges use the existing 50-level bands separately. For example,
100 original HP at level 50 costs 260% of unscaled saturation. Level-up health
bonuses do not increase the HP surcharge. The test suite covers these calculations,
malformed/stored/orphan registry entries, permissions, reset persistence and reloads.

Owned Primitive Mobs are excluded from other pets' combat target scans, direct
target assignment and damage outside duels, regardless of owner. Existing aggro
is cleared on the next living tick. This also supports external mobs implementing
Minecraft's OwnableEntity interface, even when they are not DI-adapted or
PathfinderMob subclasses. Wild Primitive Mobs remain targetable and noncombat
scans are unaffected. Tests cover same-owner wolves, other-owner wolves, a generic
external owned mob, pre-taming aggro, direct attacks and the duel exception.

Death-removal regressions also cover registered wolves removed without a
LivingDeathEvent. Explicit KILLED removal and zero-health DISCARD must archive
one death; healthy discards, chunk/player unloads, dimension changes and silent
clone cleanup must not. Ordinary damage deaths and duplicate leave events must
not double-count deaths. This covers a reproduced missing-event path, not every
possible third-party disappearance. Existing unloaded registry entries are not
automatically classified as dead.

Duel attribution checks cover two tame contributors followed by an environmental
finishing blow: the last tame attacker gets the kill, the earlier attacker gets
an assist, and damage tracking is released after the death has been attributed.
