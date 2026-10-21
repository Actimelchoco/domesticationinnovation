# v61 - belt limit, explosion scaling, and generic guardian inventories

Replace the previous DI extension JAR with domesticationinnovation-tameslevel-extension-v61-belt-cap-explosions-inventories-1.7.1-1.20.1.jar. No config edits or new client/network requirements.

Hunter Belt:
- At most two equipped Hunter Belts contribute damage and training XP, using Curios slot order. Cosmetic/inventory copies do not count. Bonuses add as before; they are not recursively multiplied.
- The same calculation supplies offline snapshots. Old uncapped snapshots are invalidated because their individual belt contributions cannot be recovered. Log in and out once with the desired belts equipped after updating.
- Offline attacks grant no belt XP. Snapshots persist across normal restarts. No periodic inventory scans or player-data loads.

Explosions:
- Native Primitive creeper blasts (fuse, Rocket impact, Festive TNT) add earned attack-damage bonus once. The same applies to unpatched Creeper Minions; an existing Mutant Monsters damage patch is detected to avoid duplication. Hunter Belt multiplies the resulting damage.
- Existing bonus-knockback scaling of native blast strength is retained. Native minion radius synchronization is unchanged.
- Learned creeper_explosion range is now 3 + 0.3*(ability level - 1) + earned knockback bonus, capped at 32 blocks to bound entity queries. Actual spherical range, particles, and inspect text agree. Existing damage/ability-power scaling is retained.
- The learned explosion skips friendly tames and protected wildlife, and does not shove targets when damage is rejected. It does not alter terrain. Native tamed explosion block protection and survival remain in place.

Guardian chest inventories:
- Controller and nine foundation blocks may be any inventory exposed through Forge ITEM_HANDLER or Minecraft Container. No chest class, name, or tag is required.
- Unsided, sided-only, and Container-only inventories are discovered within already loaded chunks. Drum may remain above or below the controller.
- Generic extract/insert handlers can convert a single adoption paper without IItemHandlerModifiable. Rejected conversion restores the original paper; if it cannot be reinserted, it is dropped at the controller instead of destroyed.
- Read-only inventories cannot convert new adoption papers. Containers must expose a standard inventory API; item-only GUI storage without a placed inventory block is not a controller.
- Shared ownership, redstone behavior, feeding, bodyguard exclusions, and existing throttling are retained.

Regression runs: shared guardian Forge GameTests (including actual Sophisticated Storage), isolated production Forge tests with real Relics/Curios and Primitive Mobs for the belt cap/offline persistence and explosion scaling.
