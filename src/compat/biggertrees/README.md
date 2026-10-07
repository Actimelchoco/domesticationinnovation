# Bigger Vanilla Trees: saplings only

This builds a standalone replacement for `bigger_vanilla_trees-forge-1.20-1.0.jar`.
It does not require Domestication Innovation. Keep only one Bigger Vanilla Trees
JAR in the instance/server mods directory.

The original 31 configured tree features move out of the `minecraft` namespace,
so they no longer override world generation. A Forge sapling-growth handler
selects those larger features for natural growth and bone meal, including 2x2
trees, mangrove propagules and azaleas. Normal grower randomness and bee variants
are retained. Other mods' features and denied growth events are left alone.
Already generated trees stay as they are.

```powershell
.\gradlew.bat -I gradle/biggerTreesSaplingOnly.init.gradle reobfBiggerTreesSaplingOnlyJar --offline '-PbiggerTreesJar=C:\path\bigger_vanilla_trees-forge-1.20-1.0.jar'
```

The output is `build/bigger-trees/bigger_vanilla_trees-forge-1.20-1.0-saplings-only.jar`.
The original input JAR is read only.

Run the three isolated server regressions (registry separation, variant selection
and actual natural/bone-meal growth) with:

```powershell
.\gradlew.bat -I gradle/biggerTreesRegression.init.gradle runGameTestServer --offline
.\gradlew.bat cleanCompileJava cleanProcessResources --offline
.\gradlew.bat compileJava processResources --offline
```

The final two commands remove test-only main-source outputs without deleting the
standalone replacement JAR or other previously built release JARs.
