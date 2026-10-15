Place `prune_unclaimed_chunks.py` directly inside the world folder containing
`level.dat` and `ftbchunks`. Requires Python 3.10 or newer; no extra packages.
Stop the server completely first. The script checks Minecraft's session.lock
and refuses to work on a running world.

From a terminal in that world folder:

```
python prune_unclaimed_chunks.py 25
python prune_unclaimed_chunks.py 25 --apply
```

The first command previews. The second removes chunks outside an inclusive
25-chunk square buffer around every claim. Distance 0 keeps only actual claims.
Overworld, Nether, End and custom dimensions are handled separately; dimensions
without claims are fully pruned. Current top-level FTB `chunks` records are
authoritative; historical `member_data.original_claims` records are ignored.
Missing/malformed claim data aborts rather than being treated as zero claims.

All region allocations and payload lengths are validated before modifying any
chunk files. Empty placeholders are left untouched; valid final sectors with
missing unused padding are accepted. Terrain, entity, POI and external .mcc
data use the same claim buffer. Surviving chunk bytes and timestamps are retained.

Progress and the deletion count are printed. Entire files selected for removal
are moved into `chunk-pruning-backups/<id>` on the same disk; partially retained
regions are copied there and compacted. Thus whole-region deletion avoids copying
gigabytes of data. Backup manifests support restoration after an interrupted run:

```
python prune_unclaimed_chunks.py --restore chunk-pruning-backups/<id>
```

Ordinary failures and Ctrl+C automatically attempt restoration. After a forced
process termination or power loss, use the restore command before restarting.
Do not restore old backups after subsequent world play unless intentionally
rolling those chunks back. Backups retain old data and occupy disk space; remove
them manually only after checking the cleaned world and deciding to discard them.

The script defaults to its own folder, independently of the terminal's current
folder. `--world <path>` overrides that. Server startup/shutdown is not performed.
FTB's client map images and unrelated mod data are not modified.

Tests: `python scripts/test_prune_unclaimed_chunks.py`
