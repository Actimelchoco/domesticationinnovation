# Primitive Mobs Trollager claim protection

Primitive Mobs 1.0.1 Trollager.smash creates a radius-three MOB explosion when
its trollagerDestruction config and mobGriefing permit terrain damage. The
ThrownBlock projectile's impact damages entities but does not replace/destroy
terrain.

ClaimTrollagerProtection filters claimed destinations from that Trollager
explosion's affected block list. This preserves blocks across claim boundaries
even when ordinary explosions are allowed by the claim's settings. It does not
cancel the explosion, remove affected entities, change unclaimed blocks, or
disable ordinary combat. The registry-ID check safely does nothing without
Primitive Mobs installed. There are no per-tick scans.

Built from the supplied empty-hand display protection JAR plus the Pumpkinhead
fix. All baseline entries are preserved byte-for-byte except the manifest; the
new subscriber class is added. Existing fire, ice, display, Pumpkinhead and
pruning functionality is retained. No config edits or Primitive Mobs JAR
replacement are required.

Regression uses the real Primitive Mobs Trollager smash with mobGriefing and
trollagerDestruction enabled, and the claim's ALLOW_EXPLOSIONS property true.
It checks claimed glass versus unclaimed glass and retained combat damage,
plus the existing Pumpkinhead conversion regression.

Build: gradle/claimedFireProtection.init.gradle with the isolated Trollager input
folder and claimedFireOutputName. Test: gradle/claimedTrollagerRegression.init.gradle
with -PtameRegressionForgeVersion=47.4.16.
The development test excludes existing DI Primitive taming mixins from its
generated resources because those hooks target production SRG method names.
This test exercises native wild Trollager behavior and the real patched FTB
JAR; it does not replace production testing of DI's Primitive taming hooks.
No production source mixin configuration or release JAR is changed for this
test-only adjustment.

On the stopped server, replace the previous FTB Chunks JAR with the new
pumpkin-trollager-claims-fix JAR, keeping only one FTB Chunks JAR. Existing damage
is not restored automatically.
