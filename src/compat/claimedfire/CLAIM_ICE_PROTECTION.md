# Amarok ice protection in FTB Chunks

Targets Minecraft 1.20.1, FTB Chunks Forge 2001.3.8 and Myths and Legends 0.0.8.8.

Amarok's `BackStepAttackGoal.tryToFreezeWaterUnderneath` places frosted ice directly, and `FrostSpikeEntity.onHitBlock` replaces the impacted block with packed ice. Neither path emits the usual Forge block-placement protection event.

Two optional `@Pseudo` mixins redirect only these calls to a helper which checks FTB Chunks at the destination block before writing. Any claimed destination is blocked, even if the wolf or projectile is outside the claim. There is no owner/ally/admin bypass for these mob-generated terrain changes. Unclaimed destinations keep the original behavior. Projectile impact sounds and disposal still run when terrain replacement is blocked.

Ordinary player block placement and other block writes are unaffected. No global block-write injection, ticking task, entity scan, or chunk scan is added. Myths and Legends remains optional; the targets are skipped if it is absent.

The replacement is built over the existing modified server JAR. Existing FTB Chunks entries, including fire protection and chunk-pruning code, are restored byte-for-byte after reobfuscating the new classes. The new mixins have their own refmap to preserve the original fire-protection refmap. Mod version, registry entries and network channels are unchanged.

Install the resulting JAR in the server's `mods` folder in place of the old FTB Chunks JAR, then restart. This fix does not require a client update or config edits. Keep only one FTB Chunks JAR installed. It prevents future ice placement; existing ice remains.

Build: `gradlew -I gradle/claimedFireProtection.init.gradle -PclaimedFireModsDir=<input-directory> finishClaimedFireProtectionJar`. Input directory must include the existing server JAR copied as `ftb-chunks-forge-2001.3.8.jar`, plus FTB Library 2001.2.13 and FTB Teams 2001.3.2. Never point the build at a live server's JAR for writing.

Regression: `gradlew -I gradle/claimedIceRegression.init.gradle -PclaimedIceModsDir=<server-mods-directory> -PtameRegressionForgeVersion=47.4.16 runGameTestServer`. Runs real Amarok freezing and frost-spike impacts across a claimed/unclaimed boundary, manual ice placement, existing fire boundary checks and pruning command validation in an isolated test world.
