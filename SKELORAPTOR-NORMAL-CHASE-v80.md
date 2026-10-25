# Skeloraptor normal combat chase — v80

Extends the v79 duel/ranked navigation fix to normal tamed Skeloraptor combat.
The selected combat mode still chooses the target; Legendary Monsters still
chooses attack animations and deals the damage. Native movement speed is unchanged.

Normal chase is installed once on each Skeloraptor through the existing tame goal
installer. Its lifetime follows the entity, without a global list retaining pets.
It does no target searches or chunk loading. Path requests are capped at once per
20 server ticks, and stop in native attack range or during an attack animation.
The idle goal returns immediately when there is no target.

Sitting, disabled AI, riding, passive mode, guardian rest, hunger forced sitting,
friendly target protections, protection-zone boundaries and bodyguard leashes
prevent normal chase. Duel/ranked continue to use their separate temporary chase
goal and opponent rules. Wild Skeloraptors keep their native movement.

Validation uses the actual Legendary Monsters 2.2.2 Forge mod. The ten GameTests
cover normal Monster Hunter acquiring/chasing/hitting a hostile, sitting/rest/
hunger/passive/friendly/boundary/leash restrictions, duplicate installation,
distant duel/ranked chasing, native attack damage, ally protection, duel cleanup,
and preservation of paths across repeated duel movement refreshes.
All ten passed in `build/v80-normal-chase-final-validation.log`. The Gradle
GameTest daemon then disappeared during shutdown after the pass report, as in
earlier local runs; this was not a clean Gradle GameTest task exit.
Version 2.2.3 is not locally available and remains unverified. These checks do not
measure TPS on a production server with hundreds of pets.

Replace the previous Domestication Innovation extension JAR while the server is
stopped. No configuration changes are required.
