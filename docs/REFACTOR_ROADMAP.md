# Refactor Roadmap

This project is being refactored incrementally. Keep each slice small enough to compile and review on its own.

## Rules

- Preserve gameplay behavior unless the change is explicitly a balance/design change.
- Run `.\gradlew.bat compileJava` after every meaningful slice.
- Do not move unrelated logic just because a file is open.
- Prefer package-private facade methods during extraction before moving deep behavior.
- Do not delete old runtime resource docs unless `/tames info` is updated and tested against the new location.
- Keep command registration separate from command behavior.

## Current Direction

The main goal is to shrink large mixed-responsibility classes:

- `TameCommands.java`
- `LevelSystem.java`
- `TameAbilityEvents.java`
- `TameDuelManager.java`
- `TameableUtils.java`

## Completed First Pass

- Extracted command builders:
  - `TameSearchCommands`
  - `TameHistoryCommands`
  - `TameCollarCommands`
  - `TameBedCommands`
  - `TameAdminRewardCommands`
- Moved class rarity buckets into `class_weights.json`.
- Added `TameClassRoller` for class selection.
- Added `AbilityCooldowns` for ability cooldown policy.
- Added `TameDuelSnapshots` for persistent duel stat copying.
- Moved Java-source old docs into `docs/old/tameslevel`.

## Next Command Extractions

Start here. These are the safest next slices.

1. `TameInfoCommands`
   - Owns `/tames info ...`
   - Keep doc-reading handlers in `TameCommands` first.
   - Expose only needed package-private methods.

2. `TameArenaCommands`
   - Owns `/tames arena ...`
   - Keep arena behavior in `TameCommands` initially.

3. `TameReincarnationCommands`
   - Owns `/tames reincarnate`, `/tames rerollClass`, `/tames reincarnation`, `/tames approvedItems`.

4. `TameOwnerPreferenceCommands`
   - Owns `doNotAttack`, `doNotAttackAnimals`, `healthSiphon`, `herding`, `enterPortalsByThemselves`, `sitOnChairs`.

5. `TameDuelCommands`
   - Owns `/tames duel`, `/tames duelSession`, `/tames duelSessionFFA`, `/tames ranked`, legacy duel compatibility.
   - This is large. Split last among command builders.

## Level System Split

Target package: `server/tameslevel/leveling`.

Suggested order:

1. `TameClassRoller`
   - Already extracted.

2. Reward catalogs
   - Move `BaseStatReward`, `AttributeReward`, `AbilityReward`, and `AbilityType` out of `LevelSystem`.
   - Make the catalogs package-private first.
   - Compile after each enum move.

3. `LevelRewardRoller`
   - Owns category roll, base stat roll, attribute roll, ability roll.
   - Keep application of stat changes in `LevelSystem` until the roller compiles cleanly.

4. `LevelRewardHistory`
   - Owns reward history rows, restore, rollback, repair.

5. `TameStatApplier`
   - Owns base stat modifiers, fixed-health conversion, scrub/reapply modifier logic.

## Ability Event Split

Target package: `server/tameslevel/events`.

Suggested order:

1. `AbilityCooldowns`
   - Already extracted.

2. `AbilityDamageFormula`
   - Move offensive ability damage budget, base DPS, scaling, AOE/single-target scaling.

3. `AttributeEffectHandlers`
   - Move passive attribute effect logic.

4. `AttackAbilityHandlers`
   - Move projectile/attack ability handlers.

5. `SupportAbilityHandlers`
   - Move support abilities like shield, body block, guardian intercept.

6. `HealingAbilityHandlers`
   - Move healing aura, bottle, triage, life gift, revitalizing presence.

Keep `TameAbilityEvents` as the Forge event router.

## Duel Split

Target package: `server/tameslevel/duel` or continue under `server/tameslevel/tame` if you want fewer package moves.

Suggested order:

1. `TameDuelSnapshots`
   - Already extracted.

2. `DuelParticipantRestorer`
   - Move restore player/tame snapshot and respawn-for-duel behavior out of `TameCommands`.
   - This will require exposing or moving respawn/teleport helpers.

3. `DuelInviteStore`
   - Move invite maps and cleanup/pop logic.

4. `DuelSessionService`
   - Move pending/active session maps and session ticking.

5. `DuelRoundSelector`
   - Move round creation, MMR pairing, team selection algorithms.

6. `DuelScoringService`
   - Move duel MMR, points, leaderboard persistence.

## Tame Registry Split

Target package: `server/tameslevel/tame`.

Suggested order:

1. `OwnerPreferenceStore`
   - Owner groups, do-not-attack settings, health siphon, portal settings.

2. `DeathHistoryStore`
   - Last deaths and death history.

3. `TameIndexes`
   - UUID, TL ID, and owner indexes.

4. `RankedParticipantStore`
   - Ranked arena and participant state.

## Docs Cleanup

- Keep authoring docs under `docs/`.
- Keep runtime docs under `src/main/resources` only if the game needs them on the classpath.
- Avoid docs inside `src/main/java`.

## Verification Checklist

After each slice:

1. Run:

   ```powershell
   .\gradlew.bat compileJava
   ```

2. Check:

   ```powershell
   git status --short
   ```

3. If command registration changed, test the affected command group in-game when feasible.

4. If class rarity or reward weights changed, verify effective odds from `class_weights.json`.

## Stop Conditions

Pause and review before continuing if:

- a refactor requires changing gameplay behavior,
- a method extraction needs more than a few public/package-private openings,
- compile errors point to circular ownership,
- a move would require deleting or rewriting persisted data format.
