# Ultimine companions

FTB Ultimine block mining requires a living, loaded tame owned by the miner within
16 blocks in the same dimension. Critters and Companions' `crittersandcompanions:roly_poly`
qualifies automatically. Its native mushroom taming, ownership, follow/stay behavior,
and chest remain in use; no custom taming mixin is needed. Faunify's rolypoly does
not receive the automatic qualification or reduced costs.
Other tames need the binary `ultimine` attribute (available through ordinary attribute
rewards and existing attribute administration), or membership in the
`domesticationinnovation:ultimine` entity-type tag. The tag starts empty and can be
extended with a datapack. Tanker is recommended for survivability; it is not required.
Stored, dead, and dueling tames cannot supply mining. Other players' pets cannot pay.

Each successfully mined block costs the selected tame:

| Companion | Physical damage | Saturation |
| --- | ---: | ---: |
| Ordinary eligible tame | 5 × mining level | 100 × mining level |
| Roly poly | 1 × mining level | 67 × mining level |

Mining level is the block's required harvest tier, with a minimum multiplier of 1:
stone/no requirement = 1, iron requirement = 2, diamond requirement = 3.
Registered modded tier tags are supported. This uses the block requirement, not the
player's tool tier. Pack saturation multipliers do not alter these explicit costs.
Holding a weapon adds the normal 50% weapon surcharge (Animights are exempt).
Thus an armed ordinary tame pays 150 saturation per mining level; an armed rolypoly
pays 101 at mining level 1. Fractional Ultimine costs round up per block. Damage is unchanged.

Rolypolies are tried before ordinary tames, then nearest first. Stored food can refill
the saturation buffer using the existing tame food rules. If no candidate can afford
the next block, it is not mined. Failed/protected blocks cost no damage or saturation.
Normal single-block mining remains available without a companion. Each block's damage
uses armor, collar protection, and the normal damage hooks; hurt cooldowns do not make
subsequent blocks free. Mining can kill the companion, so a Tanker and good collar tag
are recommended. Companion eligibility is rechecked for each block.

FTB Ultimine remains optional. This integration targets the installed 1.20.1 release
2001.1.8 and preserves FTB's existing permissions, tool restrictions, drops, cooldowns,
and player costs. Costs apply to block mining, not its separate right-click utilities.

Run the server regressions with the installed FTB/Critters and Companions jars:

```powershell
.\gradlew.bat -I gradle/ultimineRegression.init.gradle '-PtameRegressionForgeVersion=47.4.16' runGameTestServer --offline
```

Use `-PultimineModsDir=<path>` to select another directory with the same mod versions.
The newer Forge version is required by the installed FTB Library in this test setup;
the main mod retains its existing Forge build target.
