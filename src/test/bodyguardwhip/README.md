# Bodyguard whip orders

A successful player hit with a whip assigns the hit entity to all currently active
roster bodyguards. Supported items use forge:tools/whips or have a registry path
of whip / *_whip, covering the pack's Summonity, Tendon and gut whips. Native
whip entities attributed to the owner can also issue the command.

Right-click with a whip cancels the current whip order. Sneak + right-click also
clears other live combat targets from the roster. Air, block and entity right
clicks are handled; while a roster exists these clicks are consumed as commands.
Hitting a new target replaces the previous command. Existing assassin-mode sword
controls remain separate, except matching owner-combat history is cleared when
cancelling the whip order so it cannot immediately reassign the cancelled target.

Orders persist as target UUIDs on each guard across save/load and temporary
unloads, clear on target death or removal from the roster, and temporarily use
assassin targeting without editing configured modes or movement profiles. Follow
leashes yield during pursuit. Friendly/protected targets and duels remain guarded.

Run:
`.\gradlew.bat --no-daemon -I gradle/bodyguardWhipRegression.init.gradle -I gradle/bodyguardProfilesRegression.init.gradle runGameTestServer --offline`

19 combined GameTests cover real damage events, wolf/fox targeting, unchanged
saved settings, save/load, native target loss, leash suppression, air cancellation,
death cleanup, hungry-guard interactions, clearing unmarked targets, unrelated/cancelled hits, protected pets,
actual passive-guard combat, unload/reload, and existing bodyguard profile tests.

Release:
`.\gradlew.bat --no-daemon cleanCompileJava cleanProcessResources build --offline`
