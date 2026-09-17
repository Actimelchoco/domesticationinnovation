# Chest drum integration checks

Build the production and opt-in test jars:

```powershell
./gradlew.bat -I tools/chestdrum_tests/tests.gradle build chestDrumTestJar reobfChestDrumTestJar --offline
```

Run them with Citadel on a disposable Forge 1.20.1 server with a fresh world.
The test mod creates fixtures, runs assertions, prints `CHEST_DRUM_TESTS_PASS`
or `CHEST_DRUM_TESTS_FAIL`, then stops the server. Do not install it in a playable world.

Covers automatic discovery, different owners, inclusive X/Z/Y bounds, rejected
food, full inventories, conservation of transferred items, preferred-before-fallback
ordering across overlapping chests, 250-second cadence, guardian recovery, listing
radius, overlap deactivation/reactivation, command availability, and block removal/load lifecycle.
Also exercises balanced manual distribution of 3,456 items to 40 owned tames
and prints the elapsed time for that operation.
