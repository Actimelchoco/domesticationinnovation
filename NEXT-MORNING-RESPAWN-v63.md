# v63 - command respawn waits for the next morning

After a newly recorded tame death, command respawn is unavailable until the next Overworld morning (day time 0 / the next multiple of 24000). Dying during the day waits for the following morning; dying at night waits for the coming morning. Sleeping through the night counts. A death exactly at morning waits for the following morning.

Applies to /tames respawn, arise, their reincarnated variants, all/group/type selections, and admin respawn commands. Mixed batches respawn eligible entries and report entries that still need to wait. Checks occur before payment, target resolution, discarding existing entities, or rebuilding. Rejected commands report the wait and consume no items or XP.

The deadline is saved in registry and death snapshots and survives a restart. Choosing an older death snapshot does not bypass a newer death's wait. No ticking countdown or player-online requirement is introduced. Automatic pet-bed/drum respawns and duel/ranked restoration retain their existing behavior.

Legacy death records made before this update have no reliable Overworld death day-time; they retain their previous eligibility. All new deaths receive the saved deadline, including missing-entity deaths recorded by the registry.

Replace the previous DI extension with domesticationinnovation-tameslevel-extension-v63-next-morning-respawn-1.7.1-1.20.1.jar. No config changes or new client/network requirements.

Validation: isolated Forge GameTests cover real tame death, command rejection for owner/admin variants, no inventory/XP changes, save/archive persistence, exact dawn, a death at dawn, mixed eligible/recent batches, and grandfathered legacy records. Existing duel/friendship/guardian/chest tests run in the same suite. Logs: build/v63-morning-respawn-regression.log and build/v63-release-build.log.
