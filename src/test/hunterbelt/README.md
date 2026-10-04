# Hunter Belt compatibility regression

Run against the supplied Relics 0.8.0.11, Curios 5.14.1, OctoLib 0.5.0.1,
and Architectury 9.2.14 JARs. These dependencies are only added by the opt-in
script and are not bundled into a release.

```powershell
.\gradlew.bat -I gradle/tamePerformanceRegression.init.gradle -I gradle/hunterBeltRegression.init.gradle '-PhunterBeltModsDir=C:/Users/bruns/Documents/serverpackv6.43/mods' runGameTestServer --offline
```

The extra GameTest exercises the transformed native Relics damage listener and
our compatibility listener with real Curios equipment. It checks zero through
three equipped belts with different training values, vanilla wolves and tamed
foxes, training XP, removing belts, and excluding inventory/cosmetic copies.
It runs alongside the existing tame regression tests.

Afterward, build the release with separate normal invocations:

```powershell
.\gradlew.bat clean --offline
.\gradlew.bat build --offline
```
