# Nature's Aura 39.4 active-chunk index concurrency fix

Input: `C:/Users/bruns/Downloads/NaturesAura-39.4-throttled.jar`.
Its SHA256 is `edd5616e82373c92348da94d9662a29efd7d867e0d573f46a3e439b12767a96f`.
The identical copy in `Downloads/Mods/KurtsForkAdditions/mods` has the same hash.
Compared with stock 39.4, this baseline only changes ExplosionEffect,
GrassDieEffect and ReplenishingEffect; those changes are preserved unchanged.

## Failure and fix

AuraChunk.deserializeNBT calls addOrRemoveAsActive, which puts/removes entries
in LevelData.auraChunksWithSpots. That shared Long2ObjectOpenHashMap is not safe
for concurrent mutation. Parallel chunk deserialization can race another
insertion, unload removal, or area-query read. The supplied reports repeatedly
fail inside its rehash/insert methods from AuraChunk.addOrRemoveAsActive.

The patch routes the four audited internal operations through
AuraChunkIndexGuard:

- AuraChunk.addOrRemoveAsActive: put and remove.
- CommonEvents.onChunkUnload: remove.
- Helper.getAuraChunksWithSpotsInArea: get.

Each operation takes the same map object's monitor and releases it immediately
after the map operation. Consumer callbacks, aura effects, chunk IO and world
operations run outside the lock. Locks are per level index; different dimensions
do not share a global lock. There are no scheduled tasks, added ticks or scans.
The public field type, NBT format, mod version, network protocol, all original
resources, and the three earlier throttled effect classes remain unchanged.

The patch changes exactly three existing class files and adds one guard class.
The ASM rewrite replaces virtual map operations with static calls having the
same operand-stack effect, preserving existing frames and max-stack sizes.
The builder requires exactly the four expected patch sites and rejects other
input layouts rather than silently producing a partial patch.

## Build and validation

Run `scripts/naturesaura/build.ps1` from PowerShell. It uses Java 17 and the
cached ASM 9.5 and FastUtil 8.5.9 dependencies, and writes a separate output JAR
under `build/naturesaura`; input JARs are read only.

`scripts/naturesaura/AuraIndexStress.java` reproduces lost entries with raw
parallel FastUtil operations. With the guard loaded from the actual patched
JAR, 16 threads exercise six rounds of 131,072 keys: growth, reads, replacement,
removal and final-content verification. Runs use `-Xverify:all`.

Forge integration:

```
.\gradlew.bat -I gradle/naturesAuraConcurrencyRegression.init.gradle -PtameRegressionForgeVersion=47.4.16 runGameTestServer
```

The Forge regression runs actual AuraChunk deserialization, Helper area reads,
CommonEvents unloads and NBT round-trips on 768 detached chunks with 12 worker
threads, over four rounds. Detached chunks avoid generating world regions.
It verifies index contents, exact saved aura NBT and removal of emptied chunks.

## Installation

Stop the server and replace its existing Nature's Aura JAR with
`NaturesAura-39.4-throttled-chunk-threadsafe.jar`. Keep only one Nature's Aura
JAR. No config edits or DI replacement are required for this fix. Restarting
creates a fresh in-memory active-chunk index. Existing Nature's Aura clients
can retain their current 39.4 JAR because metadata, registry content and network
messages are unchanged.
