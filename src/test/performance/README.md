# Tame performance regression tests

Run the isolated Forge GameTest server from the repository root:

```powershell
.\gradlew.bat -I gradle/tamePerformanceRegression.init.gradle runGameTestServer --offline
```

The init script includes these test sources and the empty structure only for this
run. The server uses `build/tame-performance-gametest`, separate from normal worlds.

The eight tests cover:

- Taming after entity join, stable identity changes, replacement bodies, delayed
  removal events, repeated missing lookups, death, and unload.
- Exact UUID precedence over a different body's stable identity.
- Cross-dimension lookup and modified tameable species. The Nether fixture is
  always ticking because the test server has no players there.
- Every known ability/attribute ID, external input normalization, level bounds,
  unknown ID behavior, and immediate effect changes after progression edits.
- Full snapshots distributed across maintenance ticks, once per minute per tame.
- Skipping ordinary ticks, recovering missing snapshots and registry rows,
  saving pending location changes, and preserving the final location on unload.
- Duel participant lookup by UUID and stable identity, excluding unrelated tames,
  and safe iteration when a duel ends during processing.
- Cached optional hiding-method lookup, changing hiding timers, and the vanilla
  visibility fallback when a method is missing or throws.

These are correctness regressions, not a server performance benchmark. Compare a
new Spark capture under the same workload to measure the actual improvement.

Before building a release after an opt-in test run, use a normal clean build to
remove test-only outputs:

```powershell
.\gradlew.bat clean --offline
.\gradlew.bat build --offline
```
