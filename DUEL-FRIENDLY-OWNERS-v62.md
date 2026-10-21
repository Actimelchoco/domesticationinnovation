# v62 - friendlyOwners during duel and ranked

Replace the previous DI extension JAR with domesticationinnovation-tameslevel-extension-v62-duel-friendly-owners-1.7.1-1.20.1.jar. No config changes or new client/network requirements.

friendlyOwners is ignored between actual opposing participants in the same active duel or ranked battle. This includes players, native/interface tames, arrows and other attributed projectiles, TNT/explosions, lingering effects, harmful effects, targeting, and final damage checks.

The bypass uses fighter identities, not just owner identities. Friendly teammates, spectators, owners who are only watching their pets, unselected pets, and fighters in different battles retain friendship protection. Once the match ends, protection returns immediately, including for projectiles fired during the match. Saved friendlyOwners lists are never changed.

The change adds no periodic scans or per-tick work. It uses the existing active battle/team lookup when damage or targeting occurs, with bounded attribution traversal for indirect sources.

Validation: all 38 required Forge GameTests passed, including casual/ranked friendship bypass, player/tame attacks, direct-only projectile attribution, explosion filtering, harmful potion effects, registry/ability targeting, teammate protection, separate battles, and restoration after battle. Existing guardian/chest/Sophisticated Storage tests passed as part of the same suite.

Test log: build/v62-duel-friendship-regression.log. The test server completed and shut down cleanly; this environment's Gradle daemon disconnected afterward. The separate production release build is recorded in build/v62-release-build.log.
