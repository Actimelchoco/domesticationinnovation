# Vallumraptor weapons integration

Tamed Vallumraptors grab one dropped sword or axe through their native food-grabbing AI when their main hand is empty. Weapons retain enchantments/custom NBT, use normal mob melee enchantment handling, and do not lose durability. They keep their native animations and reach. Sitting tamed raptors also grab weapons within two blocks without standing or walking; sitting food pickup remains unchanged. There is no equipment inventory.

Owners retrieve the currently held item by sprinting and right-clicking with an empty main hand, without sneaking. The item moves directly into that hand. Normal commands, feeding, and sneak hunger/stat shortcuts retain their interactions. Retrieval works even when hungry. Weapon pickup and retrieval are blocked during duels; already equipped weapons can be used in combat.

Run against the installed Alex's Caves 2.0.2 and Citadel 2.6.3 JARs:

```powershell
.\gradlew.bat --no-daemon -I gradle/vallumraptorWeaponsRegression.init.gradle '-PvallumraptorModsDir=C:/Users/bruns/Documents/linggango server/mods' runGameTestServer --offline
```

Nine GameTests cover native ground AI pickup, exact NBT transfer and entity saving, no eating/replacement/wear of weapons, native meat and wild behavior, sword/axe damage with Smite/Fire Aspect/Knockback, native reach checks, friendly protection, duel equipment locks, owner-only retrieval including food, hunger lock exemption, and preservation of command/feeding/sneak interactions.

Release builds omit the optional test sources and dependencies:

```powershell
.\gradlew.bat --no-daemon cleanCompileJava cleanProcessResources build --offline
```

Tamed Vallumraptors unseal equipped L2 sealed items on their next credited kill.
The original stack is restored in place with exact durability, count, enchantments,
name and custom NBT. Held seals cannot be displaced by a queued native food grab.
Malformed/nested recovery tokens remain intact. Two additional tests cover a real
kill, no recovery from time alone, cancelled deaths, owner kills and wild raptors.
The seal registry fixture uses L2 Hostility 2.5.19's sealed_item / sealedItem format;
the L2 sealing trait itself is not loaded by this fixture.
