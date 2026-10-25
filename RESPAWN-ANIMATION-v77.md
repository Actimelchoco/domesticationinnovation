# Respawn animation reset v77

Shared respawn, recovery, ranked restoration and native rebuild paths now sanitize
saved HurtTime/DeathTime/Health before loading snapshots and joining the world.
Full health and zero hurt/death/invulnerability timers are established before
tracking begins. Existing finalization now also clears hurtDuration.

The original saved snapshot is copied, leaving archives and appearance data intact.
No client protocol changes, continuous scans, ticking resets or extra packets added.

Runtime regression before patch: wolf joined with hurtTime=10, deathTime=12;
interface fox joined with health=0, hurtTime=10, deathTime=12.
Visual reproduction with the user's particular modded tame remains unverified.

Includes v76 admin morning bypass and all prior changes.
Stop the server and replace the previous extension JAR. No config changes.

All 18 Forge regression tests passed after patch; production build passed.
