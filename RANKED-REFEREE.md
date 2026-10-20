Ranked referee (v55)
====================

Stand at the desired arena post and run `/tames admin ranked setRefereeSpot`.
This saves the post for the selected ranked arena. No config-file changes are needed.

When an outside hostile targets or attacks a ranked participant, a temporary copy
of the highest-MMR eligible tame in the ranked pool that is not in any duel appears
at the post. The original tame stays where it was and keeps its equipment and MMR.
The referee fights registered threats, returns to the post, sits, and wakes for
another attacker. It disappears with its temporary projectiles when the round ends.
It is invulnerable, cannot be tamed or saved, and cannot damage fighters or arena
blocks through its tracked attacks. It does not enter the roster or affect scores.

The post must be in the participant's dimension and an already loaded chunk.
Threats must be wild hostile mobs within 128 blocks of the post. Casual duels are
unaffected. A referee needs an eligible bench tame with a usable entity snapshot.

Threat discovery uses target/attack events, with no periodic nearby-entity scan.
Only spawned referees run the controller: decisions every 10 ticks, paths and
melee attempts every 20 ticks, copied extension abilities at most every 20 ticks
while fighting. Idle referees sit. Returning referees teleport to the post if
they remain stuck for 200 ticks. Missing templates retry at most every 200 ticks.
Each battle tracks at most 64 threats and uses one referee.

Install the v55 jar in place of the older extension jar, keeping only one DI jar.
No client registry or network protocol changes were added.
