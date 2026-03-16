# TL -> DI Implementation Steps

## Phase 0 - Baseline and Safety
1. Freeze target versions (Minecraft/Forge/DI/TL) used for migration.
2. Back up test world and production world before each migration run.
3. Define rollback trigger (for example: missing migrated stats, pet behavior regressions, join failures).

## Phase 1 - Migration Pipeline Finalization
1. Validate TL export command in a controlled world:
- `/tames admin migrateDI dryrun`
- `/tames admin migrateDI apply`
2. Confirm the saved-data file contains expected entries under `domesticationinnovation_tameslevel_migration`.
3. Load/teleport tames you want converted (including previously unloaded ones).
4. Run DI manual conversion command for loaded tames:
- `/di tlmigration applyloaded dryrun`
- `/di tlmigration applyloaded apply`
5. Use DI status command to validate conversion results:
- `/di tlmigration status`
6. Verify `TamesLevelFeatureBridge` applies at least:
- base bonus stats (`bonusHealth`, `bonusDamage`, `bonusSpeed`, etc.)
- mapped defensive/reactive abilities already implemented.
7. Add idempotency checks in test flow:
- running export multiple times should not duplicate/break state.

## Phase 2 - Feature Mapping from TL to DI
1. Build a mapping table from TL docs/code:
- TL base stats -> DI runtime stat modifiers
- TL attributes -> DI attribute/effect implementation
- TL abilities -> DI ability handlers or removal list
- TL modes -> DI command/AI behavior handling
2. Mark each TL feature as one of:
- `Implement now`
- `Implement later`
- `Drop (intentional)`
3. Implement only server-side logic first (event handlers, attributes, saved data, AI logic).
4. For each implemented feature, add migration behavior for old TL data keys.

## Phase 3 - Collar Tag Rework
1. Restrict collar tags to vanilla armor-related enchantments only.
2. Block or strip non-vanilla DI collar enchantments from new applications.
3. Define migration for existing pets/items with old DI collar enchantments:
- convert to DI ability/attribute where possible, or
- remove with clear fallback rules.
4. Verify no client update is required for this ruleset change.

## Phase 4 - TL Removal Cutover
1. Run final TL export on server with both mods installed.
2. Start server once and verify migrated pets load with expected data.
3. Remove TL from server mod list.
4. Restart server and run validation checklist:
- player join success with normal DI client
- pet stat bonuses present
- migrated abilities still firing
- no spam errors about missing TL classes/data.

## Phase 5 - Cleanup
1. Remove temporary TL-compat-only code paths that are no longer needed.
2. Keep a minimal migration reader for old worlds for at least one release window.
3. Add docs/changelog section for admins describing:
- one-time migration sequence
- TL removal point
- known dropped features.

## Test Checklist (Per Build)
1. Fresh world: DI extension behavior works without TL installed.
2. Existing TL world: migration imports and applies correctly.
3. Reboot persistence: migrated data survives restart.
4. Multiplayer compatibility: unextended DI client can join and play.
5. Edge cases:
- pet unloaded during migration
- duplicate pet names
- missing owner UUID
- malformed payload entries.

## Suggested Next Work Items
1. Add a concrete TL->DI feature mapping matrix file (per ability/attribute/mode).
2. Add automated integration test world scripts for migration dryrun/apply verification.
3. Add runtime logging counters in DI for applied migration payloads and ignored keys.
