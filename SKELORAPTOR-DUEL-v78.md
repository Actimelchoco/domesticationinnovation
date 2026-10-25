# Skeloraptor duel attacks v78

The Skeloraptor's native area attacks checked isAlliedTo before dealing damage.
Same-owner duel opponents still counted as allies there, so native swings did no
damage even with a valid combat command and target. Its ally check now treats
actual opposing duel/ranked participants as enemies. Nonparticipants and normal
allies retain native protection. Native damage, animations and timing are preserved.

No extra ticking scans, forced damage or replacement attack goals are added.
Runtime before patch with local Legendary Monsters 2.2.2: allied=true, command=2,
opponent health unchanged at 1000 after a native swing in duel and ranked.

Includes all v77 and earlier changes. Stop server and replace the previous
extension JAR; no configuration changes. Compatibility test input is the local
legendarymonsters-2.2.2 MC 1.20.1.jar. Version 2.2.3 was not available locally.

All four real-mod Forge regression tests passed, including native AI damage on
server ticks, duel/ranked swings, and same-owner nonparticipant protection.
Production build passed.
