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
