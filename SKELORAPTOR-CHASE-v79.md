# Skeloraptor duel chase v79

The native Skeloraptor registers attacks for nearby targets, owner-following and
wandering, but no dedicated target-chasing goal. A duel-only navigation goal now
approaches assigned opponents and yields at native melee range or during attack
animations. It does not deal damage or replace native attacks.

Skeloraptor's setOrderedToSit(false) also unconditionally stops navigation. Duel
movement refreshes now call native sitting setters only when their state changes,
so repeated unchanged combat commands preserve the existing path.

Chase goals are installed only on participating Skeloraptors and removed when they
leave the battle. Path requests are limited to once per 20 ticks, including across
short stop/start cycles. No additional idle-tame scans or path requests are added.
Original movement speed is used. Native floating/attack animation behavior remains.

Before patch: distant ranked target stayed at health 1000, navigation ended with
target still assigned; partial chase stalled when repeated stand-up setters cleared
its path. Local real-mod compatibility input: Legendary Monsters 2.2.2. 2.2.3 was
not available locally to test.

Includes v78 ally-check fix and all prior changes. Stop server and replace previous
extension JAR. No config changes.

All eight real-mod Forge tests passed, including distant duel/ranked chase and
native damage, navigation preservation during repeated duel movement refreshes,
nonparticipant protection, and chase removal after duel. Production build passed.
