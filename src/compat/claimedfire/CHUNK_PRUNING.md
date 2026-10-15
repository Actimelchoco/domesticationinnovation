Console command: chunks deleteNoClaimedChunks <distance>

Example: chunks deleteNoClaimedChunks 25

Requires permission level 4, a dedicated server console (or RCON), and zero
online players. Distance must be a nonnegative integer, measured in chunks.
Claims and the inclusive square buffer around each claim are preserved in
every loaded dimension. A dimension with no claims is fully pruned. With
distance 0 only the claimed chunks remain.

The command initiates a clean server shutdown. Once storage closes, it locks
the world, validates region files, backs up all affected files, and prunes
terrain, entities and POI data. Surviving region entries are compacted without
changing their payloads or timestamps. Unclaimed external .mcc payloads are
removed too. No deletion happens while world storage is live.

Wait for the [ChunkPruning] completion message before restarting the server.
Affected files are backed up under world/chunk-pruning-backups/<unique ID>/,
including corresponding paths for dimensions. To restore, stop the server
and copy that backup's region/entities/poi files back to the matching world
paths. These are backups of affected files, not full-world snapshots.

Zero-byte region placeholders are skipped and left untouched. Malformed nonempty
region files abort the operation before any deletion. An I/O error
during deletion triggers restoration of affected files; inspect the server
log for restoration failures before restarting. Deleted chunks regenerate
when loaded again. FTB client map images and unrelated mod data are unchanged.
Valid region files with unpadded final sectors are accepted; surviving payloads
are copied unchanged and missing unused padding is filled with zeroes.

Build:
gradlew.bat -I gradle/claimedFireProtection.init.gradle -PclaimedFireModsDir=<original dependency directory> finishClaimedFireProtectionJar

Offline region regression:
javac -d build/claim-pruning-tests src/compat/claimedfire/java/dev/ftb/mods/ftbchunks/claimfire/ClaimChunkPruner.java tools/ClaimChunkPrunerRegression.java
java -cp build/claim-pruning-tests ClaimChunkPrunerRegression
