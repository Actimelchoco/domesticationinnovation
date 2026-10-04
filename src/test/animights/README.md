# Animights equipment integration regressions

```powershell
.\gradlew.bat -I gradle/tamePerformanceRegression.init.gradle -I gradle/animightEquipmentRegression.init.gradle '-PanimightModsDir=C:/Users/bruns/Documents/serverpackv6.43/mods' runGameTestServer --offline
```

Loads the real Animights 1.0.0, Dungeon Now Loading 2.11, GeckoLib 4.8.2, and KnightLib 1.4.3 JARs.
The opt-in script selects Forge 47.4.16 because Animights requires at least 47.2.30;
ordinary builds retain the original Forge 47.2.0 target. Run once without `--offline`
if those test dependencies have not been cached.

Runs the existing 14 regressions plus four equipment tests covering armor ranking,
Protection ties, weak/damaged spare rejection, retained item NBT, occupied slots,
serialization, independent duel copies, automatic occupied-slot upgrades, player-added upgrades, armor kill loot with food pickup disabled,
live menu consistency, real DNL scrap conversion, full reserves, and pre-hit rescue.
L2 unsealing uses a registry fixture with the real `l2hostility:sealed_item` ID and
`sealedItem` NBT format from L2's SealedItem source. It checks kill-only restoration,
all original items/counts/NBT, immediate armor upgrades, full reserves, repeat calls,
and unchanged malformed seals and DNL scraps. The L2 transformation itself requires
its JAR to test; it is not part of this fixture.
Source: https://github.com/Minecraft-LightLand/L2Hostility/blob/main/src/main/java/dev/xkmc/l2hostility/content/item/traits/SealedItem.java

Remove opt-in outputs before producing a release, using separate invocations:

```powershell
.\gradlew.bat clean --offline
.\gradlew.bat build --offline
```
