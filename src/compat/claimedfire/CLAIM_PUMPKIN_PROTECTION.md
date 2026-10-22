# Lord Pumpkinhead claim protection

Built against Born in Chaos Forge 1.20.1 1.7.5 and the supplied
`ftb-chunks-forge-2001.3.8-displays-empty-hand-only.jar`.

The optional `PumpkinTerrainClaimMixin` intercepts block replacements in the
three Lord Pumpkinhead tick procedures, FelsteedParProcedure and
WithoutHorsePProcedure. Each destination is checked through the existing FTB
claim lookup before writing fel soil. Unclaimed terrain changes continue as
normal. Damage, particles and other procedure behavior are not cancelled.
There are no periodic searches or configuration changes.

Build using `gradle/claimedFireProtection.init.gradle`, with the requested
baseline copied to an isolated dependency folder as
`ftb-chunks-forge-2001.3.8.jar`, alongside FTB Library and Teams. Set
`claimedFireOutputName` to select the output filename. The finish task restores
all original entries except the manifest, retaining their exact bytes.

`gradle/claimedPumpkinRegression.init.gradle` runs the real Born in Chaos
procedures against water inside a claim and directly across its boundary,
including normal building within the claim.
Run with `-PtameRegressionForgeVersion=47.4.16` for the supplied FTB Library's
Forge dependency requirement.

Replace the existing FTB Chunks JAR on the stopped server with the fixed JAR;
keep only one FTB Chunks JAR. Born in Chaos itself is unchanged. Previously
converted blocks are not restored automatically.
