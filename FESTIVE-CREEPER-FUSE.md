v59 - Festive Creeper combat fuse

Install domesticationinnovation-tameslevel-extension-v59-festive-creeper-fuse-1.7.1-1.20.1.jar instead of the previous DI extension JAR. Do not install the test JAR. No Primitive Mobs JAR or config changes are required.

Tamed red Festive Creepers retain their ranged TNT attack and now start a normal fuse when a valid enemy is within 3 blocks and visible. An active fuse continues within 7 blocks while visible. Sitting pets do not start this combat fuse. They survive their own explosion, keep the existing 3-second cooldown, and do not damage blocks. Wild Festive Creepers keep their native behavior.

Cause: FestiveCreeper.tick explicitly disables swelling every tick, and its native goal list has no SwellGoal. The previous survival hook did not start its fuse. Earlier reflection-based explosion tests did not cover this native combat path.

Validated against the real Primitive Mobs 1.0.1 and Forge 47.4.20 in an isolated production server: native tick triggers blast without forced ignition/reflection; survival; cooldown; repeat blast; wild and sitting exclusions; owner collar interaction for festive/support/rocket creepers, baby spiders, and chameleons. Production build passed.

Regression source: src/test/primitivefuse/
Build tests: gradlew.bat -I gradle/primitiveFuseRegression.init.gradle primitiveFuseTestJar reobfPrimitiveFuseTestJar
Run the separate primitive-fuse-tests.jar ONLY in an isolated Forge server with DI, Citadel, and Primitive Mobs. It executes synchronous tests after startup and stops the server. Look for PRIMITIVE_FUSE_TESTS_PASS/FAIL in the log.
