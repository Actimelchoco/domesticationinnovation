# Balance Update Checklist

When balance numbers change, update these places together so runtime, inspect/info output, and docs stay aligned.

## Runtime Code
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/leveling/LevelSystem.java`
  - reward-category chances
  - class reward weights
  - base-stat weights
  - migration/reroll behavior
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/events/TameAbilityEvents.java`
  - offensive ability damage formulas
  - cooldown formulas
  - support/heal timing if relevant
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/tame/TLAdminRuntimeSettings.java`
  - default admin runtime knobs

## Player-Facing Commands
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/TameCommands.java`
  - `/tames inspect` damage/cooldown text
  - `/tames info ability` live runtime notes
  - admin command names and help text

## Docs Used By `/tames info`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/GeneralDocu.md`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/BaseStatsDocu.md`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/ClassesDocu.md`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/AbilitiesDocu.md`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/AttributesDocu.md`
- `src/main/java/com/github/alexthe668/domesticationinnovation/server/tameslevel/old docus/CommandDocu`

## Sanity Check After Changes
1. Run `./gradlew compileJava`.
2. Check `/tames inspect <pet>` for a tame with attack abilities.
3. Check `/tames info ability <id>`, `/tames info class <class>`, and `/tames info` for stale numbers.
4. If class roll behavior changed, update expected examples or admin migration commands.
