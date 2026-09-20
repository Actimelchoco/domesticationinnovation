# Chest drum integration checks

Build the production and opt-in test jars:

```powershell
./gradlew.bat -I tools/chestdrum_tests/tests.gradle build chestDrumTestJar reobfChestDrumTestJar --offline
```

Run them with Citadel on a disposable Forge 1.20.1 server with a fresh world.
The test mod creates fixtures, runs assertions, prints `CHEST_DRUM_TESTS_PASS`
and `ASYNC_FOOD_DISTRIBUTION_PASS` on success (or a corresponding `FAIL`), then stops the server. Do not install it in a playable world.

Covers automatic discovery, different owners, inclusive X/Z/Y bounds, rejected
food, full inventories, conservation of transferred items, preferred-before-fallback
ordering across overlapping chests, 250-second cadence, guardian recovery, listing
radius, overlap deactivation/reactivation, command availability, and block removal/load lifecycle.
Also exercises asynchronous manual distribution to 40 owned tames over real server
ticks. Checks one recipient per tick, snapshots extracting nothing, balance, partial
supply, changed source items, dead/unloaded/full/transferred tames, replaced chests,
duplicate requests, and cancellation/restart. Snapshot timing is printed separately
from background planning and transfer duration.

The pure planner can be tested without launching Minecraft:

```powershell
javac -d build/food-planner-tests src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/FoodDistributionPlanner.java tools/chestdrum_tests/java/FoodDistributionPlannerTests.java
java -cp build/food-planner-tests com.github.alexthe668.domesticationinnovation.server.tameslevel.FoodDistributionPlannerTests
```

It checks a 200,000-item source against 40 recipients, shared capacity between food
types, partial stacks, rejected foods, and immutable reusable snapshots. This test
class is excluded from the Forge test JAR to avoid a split Java module package.
