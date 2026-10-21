# Offline Hunter Belt - v60

Tamed pets, including guardians and interface tames, keep the owner's equipped Hunter Belt damage multiplier while the owner is offline. Multiple equipped belts stack as before. Cosmetic and inventory belts do not count.

The multiplier is captured at logout and normal server shutdown, stored in world/data/tames_offline_hunter_belts.dat, and survives restarts. Login clears the old snapshot; online combat reads live equipment. Offline attacks grant no belt XP and never load offline player inventories. No periodic scans or per-hit file writes are added.

After installing for the first time, owners who were already offline must log in and out once with their belts equipped. This does not activate inactive guardians or force-load chunks; existing guardian activity rules still apply.

Replace the previous DI extension JAR with domesticationinnovation-tameslevel-extension-v60-offline-hunter-belt-1.7.1-1.20.1.jar. No configuration changes or new client/network requirements are introduced.

Regression tests use the actual Relics 0.8.0.13, Curios 5.14.1, OctoLib, Architectury and Primitive Mobs JARs in an isolated production Forge server. Build the separate test mod with:

gradlew.bat -I gradle/offlineHunterBeltRegression.init.gradle offlineHunterBeltTestJar reobfOfflineHunterBeltTestJar

Install offline-hunter-belt-tests.jar ONLY in a disposable test server. It runs after startup and stops the server. Look for OFFLINE_HUNTER_BELT_TESTS_PASS/FAIL.
