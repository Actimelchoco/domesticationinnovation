# Eternal Steak compatibility regression

Runs the existing 14 GameTests plus one integration test against Artifacts 9.5.16.
The extra test checks meat classification, wolf preference, unrestricted fallback
feeding for cats, item reuse, saved healing state, and recovery after another meal.
Artifacts embeds ExpandAbility; the test script also loads Curios, Cloth Config,
and Architectury from the specified directory.

```powershell
.\gradlew.bat -I gradle/tamePerformanceRegression.init.gradle -I gradle/eternalSteakRegression.init.gradle '-PeternalSteakModsDir=C:/Users/bruns/Documents/serverpackv6.43/mods' runGameTestServer --offline
```

Before building a release, run normal clean and build in separate invocations:

```powershell
.\gradlew.bat clean --offline
.\gradlew.bat build --offline
```
