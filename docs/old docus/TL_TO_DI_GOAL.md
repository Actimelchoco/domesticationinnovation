# TL -> DI Server-Side Extension Goal

## Purpose
Move the relevant TamesLevel (TL) progression/features into Domestication Innovation (DI) while keeping DI playable for clients that only have the normal (unextended) DI client mod installed.

## Scope
- Keep all extensions server-side only whenever possible.
- Preserve compatibility for players joining with unextended DI client builds.
- Use TL only as a temporary migration source during transition.

## Migration Strategy (Operational)
1. Start server once with both mods: TL + DI extension build.
2. Export/sync TL progression into DI migration storage.
3. Verify migrated data is being applied to pets in DI runtime.
4. Remove TL from server modpack.
5. Continue running with DI extension only.

## Current Code Status (Already Present)
- TL side exports migration payloads into saved data key:
  - `domesticationinnovation_tameslevel_migration`
  - command path: `/tames admin migrateDI dryrun|apply`
- DI side already contains:
  - saved data reader: `TamesLevelMigrationData`
  - DI runtime progress table: `tameRegistry` (single authoritative per-tame store)
  - runtime bridge: `TamesLevelFeatureBridge`
  - manual conversion path in `CommonProxy` (`/di tlmigration applyloaded ...`)

## Collar Tag Direction
1. Collar tags should accept only vanilla armor-type enchantments (for example Protection, Feather Falling, etc.).
2. Existing DI collar-tag-exclusive enchantments should be either:
- converted into TL/DI-style ability or attribute systems, or
- removed (with explicit migration handling so old saves do not break).

## Compatibility Requirements
- Do not add mandatory client-only rendering/UI dependencies for core logic.
- Prefer server-authoritative behavior using existing packets/sync paths.
- Avoid changing network expectations in ways that require a custom client update just to join.

## Non-Goals (For This Phase)
- Full client UX refresh for TL systems.
- Perfect 1:1 parity of every TL debug/admin command.
- Re-adding TL-only compatibility hacks that existed only because TL and DI were separate mods.

## Acceptance Criteria
- Server can migrate TL progress to DI in a controlled one-time flow.
- After TL removal, migrated pets keep expected progression effects under DI extension.
- Players using normal DI clients can still connect and play.
- Collar tags use vanilla armor-style enchantments only.
- Legacy DI collar enchantments are mapped to abilities/attributes or explicitly retired.
