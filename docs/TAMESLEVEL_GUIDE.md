# TamesLevel Guide

<span style="color:#d4af37"><strong>Status:</strong></span> Draft guide scaffold.  
<span style="color:#7ec8e3"><strong>Scope:</strong></span> Current `tamesLevel extension` behavior and commands.

<span style="color:#d4af37"><strong>Contents:</strong></span> See [TAMESLEVEL_GUIDE_TOC.md](/C:/Users/bruns/Documents/Workspace/DomesticationInnovation-main/docs/TAMESLEVEL_GUIDE_TOC.md).

## <span style="color:#d4af37">1. Introduction</span>

### 1.1 What this mod adds

Your tames can level.

Next to your tame's name, you can see its level.

Whenever your tame kills a mob, or helps kill a mob, it gains XP. Once it reaches enough XP, it levels up.

When a tame levels up, it can gain:
- better stats
- a new attribute
- a new ability
- or an upgrade to something it already has

So the basic loop of the mod is simple:
- fight with your tames
- gain XP
- level up
- become stronger

### 1.2 What you should know early

There are four important things to understand early:

- tames keep their progress
- dead tames stay in your registry
- tames can be recovered
- `/tames` commands are how you manage everything

So if a tame goes missing or dies, do not assume it is gone forever. Check your commands first.

## <span style="color:#7ec8e3">2. Core Rules</span>

### 2.1 How tame data is stored

The mod keeps a registry for your tames.

That means the game remembers things like:
- the tame's level
- its stats
- its abilities and attributes
- whether it is alive or dead
- where it was last seen
- whether it has a pet bed

Because of that, a dead tame can still show up in your commands, and a missing tame can still often be found or recovered later.

### 2.2 Levels, XP, and progression

Tames gain XP through combat.

The final hit is not the only thing that counts. Assists count too.

When a tame levels up, it can gain:
- better base stats
- attributes
- abilities

Its class also affects what kinds of rewards it is more likely to get.

### 2.3 Deaths, XP loss, and penalties

When a tame dies for real, it loses `5%` of its invested XP.

That is the current death punishment.

Important:
- the tame is still kept in your registry
- you can still view it
- you can still respawn or recover it later

### 2.4 Reincarnation rules

If a tame used to be a higher level than it is now, it can be reincarnation-eligible.

You can check this with:

```text
/tames stat <tame name>
```

If the current level is lower than the `Highest level`, the tame can be reincarnated.

### 2.5 Duel-specific rules

Deaths in duels do not count as normal deaths.

That means:
- no normal death punishment
- no normal death-history punishment path
- the duel system handles the defeat separately

### 2.6 Friendly fire and ownership rules

Normally, tames should not attack their owner or allied tames.

The main exception is duels, where friendly targets can temporarily become valid opponents.

## <span style="color:#90ee90">3. Stats and Progression</span>

### 3.1 Base stats

When a tame levels up, one possible reward is better base stats:
- health
- damage
- speed
- armor
- armor toughness
- attack knockback
- knockback resistance

### 3.2 Bonus stats

Bonus stats are the extra permanent stat points your tame has earned.

You can see them in the detailed stat view.

### 3.3 Attributes

Attributes are passive bonuses or special effects.

Examples:
- lifesteal
- poison resistance
- chain lightning
- explosion resistance
- ability power

Attributes usually change how a tame fights, not just how high its raw numbers are.

### 3.4 Abilities

Abilities are active powers your tame uses in battle.

Examples:
- arrows and tridents
- skulls, fireballs, and snowballs
- guardian beams
- shulker bullets and fangs
- healing pulses
- battlefield support effects
- emergency protection and rescue powers

Some are offensive, some are support, and some are healing.

### 3.5 Classes

Each tame also has a class.

Its class affects what kinds of rewards it is more likely to get as it levels.

Classes can push a tame toward things like:
- a bruiser
- an assassin
- a supporter
- a mage
- a shooter
- a living chaos engine
- or one of the many mob-themed paths now available through class weights

### 3.6 Ability Power and Damage Bonus

Two important offensive stats are:

- `damage bonus`
- `ability_power`

`damage bonus` increases offensive ability damage.

`ability_power` also increases offensive ability damage. Each level of `ability_power` adds another `25%` of the level-1 base damage.

### 3.7 How scaling works

For most players, the simple version is enough:

- level makes an ability stronger
- `damage bonus` makes offensive abilities hit harder
- `ability_power` makes offensive abilities stronger still
- support and healing abilities care more about their own effect than attack damage

If you want the exact current numbers for a loaded tame, use:

```text
/tames inspect <pet>
```

## <span style="color:#ffb347">4. Player Commands</span>

### 4.1 `/tames list`

Use:

```text
/tames list
```

This shows your registered tames.

### 4.2 `/tames loaded`

If one of your tames seems missing, use:

```text
/tames loaded
```

This separates your tames into:
- loaded
- unloaded
- dead

This tells you if the tame is currently loaded, unloaded, or dead.

### 4.3 `/tames strongest`

Use:

```text
/tames strongest
```

This shows your strongest living registered tame.

### 4.4 `/tames stat` and `/tames stats`

These are your main info commands.

Use:

```text
/tames stat <pet>
/tames stats <pet>
```

They show things like:
- level and current XP
- highest recorded level
- whether reincarnation is possible
- kills, assists, deaths, and survival days
- mode and class
- group and bed information
- bonus stats, abilities, and attributes in the longer view

### 4.5 `/tames inspect`

Use:

```text
/tames inspect <pet>
```

This is for loaded, living tames and shows:
- offensive ability damage
- cooldown expectations
- support and heal notes
- attribute combat notes
- current offensive scaling info

### 4.6 `/tames show`

Use:

```text
/tames show
```

This reminds you of the command system and the in-game info pages.

### 4.7 `/tames search`

Use search when you want to find which tame has a certain ability, attribute, or class.

Examples:

```text
/tames search ability <ability>
/tames search attribute <attribute>
/tames search class <class>
```

This is useful when you have many tames.

### 4.8 `/tames deaths`

Use:

```text
/tames deaths <number>
```

This shows recent death entries from your records.

### 4.9 `/tames graveyard`

Use:

```text
/tames graveyard
/tames graveyard <limit>
```

This shows your dead tames and useful recovery information.

### 4.10 `/tames reincarnate`

Use:

Use:

```text
/tames reincarnate <pet>
```

Important:
- the tame must be alive
- the tame must be loaded
- the tame must have a higher remembered level than its current one

## <span style="color:#ff7f7f">5. Combat and Duels</span>

### 5.1 Tame combat basics

A high-level tame can do much more than a normal tame.

Some tames learn projectile attacks, support abilities, healing, or other special combat powers.

If a tame feels weak or wrong in combat, check:
- a tame needs to be alive
- a tame needs to be in the right mode
- a tame needs a valid enemy
- its class, abilities, and attributes matter too

### 5.2 `/tames duel`

Use:

```text
/tames duel ...
```

This is the regular duel command path.

### 5.3 `/tames duelteam`

For team duels, use:

```text
/tames duelteam invite <player> <selection>
/tames duelteam accept <player> <selection>
```

This allows:
- player vs player
- tame vs tame
- player vs tame
- mixed teams of players and tames

### 5.4 Duel selectors

Valid selectors include:
- `myself`
- `all`
- `group <name>`
- `type <name>`
- `state follow`
- `state sit`
- `state wander`
- `follow`
- `sit`
- `wander`
- `name <pet>`

Examples:

```text
/tames duelteam invite niklas myself
/tames duelteam accept kurthagoras name fly
```

Or, for a group:

```text
/tames duelteam invite niklas group guards
```

These selectors are how you choose which fighters enter.

### 5.5 Players vs tames

Yes, players and tames can fight each other in duels.

That is useful if you want:
- yourself against another player's companion
- your tame against another player
- or a mixed fight

### 5.6 Duel death handling

A duel defeat is separate from a normal death:
- duel defeats do not count as true permanent deaths
- duel punishment does not follow the ordinary death-history path
- duel cleanup is meant to restore the defeated fighters

## <span style="color:#c39bd3">6. Modes, Orders, and Targeting</span>

### 6.1 `/tames mode`

Use:

```text
/tames mode <pet> <mode>
/tames mode <all|group|type|state> ... <mode>
/tames mode ... bodyguard <range>
```

Current modes include:
- `default`
- `default_plus`
- `bodyguard`
- `boss`
- `monster_hunter`
- `aggressive`
- `passive`

Mode changes how the tame behaves in combat.

For `bodyguard`, you can append a leash distance:

```text
/tames mode <pet> bodyguard 5
```

That range is saved per tame. If the tame moves farther than that many blocks from its owner, it drops its target and returns to the owner.

### 6.2 `/tames follow`

Use:

```text
/tames follow <name>
/tames follow all
/tames follow group <group>
/tames follow type <type>
/tames follow state <follow|sit|wander>
```

This sets selected tames to follow.

### 6.3 `/tames sit`

Use:

```text
/tames sit <name>
/tames sit all
/tames sit group <group>
/tames sit type <type>
/tames sit state <follow|sit|wander>
```

This sets selected tames to sit.

### 6.4 `/tames wander`

Use:

```text
/tames wander <name>
/tames wander all
/tames wander group <group>
/tames wander type <type>
/tames wander state <follow|sit|wander>
```

This sets selected tames to wander.

### 6.5 `/tames movement`

Use:

```text
/tames movement <pet|all|group|type|state> <default|skeleton|close>
```

Movement profiles:
- `default`: no special spacing behavior
- `skeleton`: if the target is too close, the tame backs away now and then
- `close`: stay closer to the owner while idle

### 6.6 Bodyguard, guardian, passive, aggressive, and other behavior rules

Quick mode summary:

<span style="color:#7ec8e3"><strong>default</strong></span>  
Normal behavior.

<span style="color:#90ee90"><strong>default_plus</strong></span>  
Better general-purpose combat behavior.

<span style="color:#ff7f7f"><strong>bodyguard</strong></span>  
Protect the owner.

<span style="color:#d4af37"><strong>boss</strong></span>  
Better focus on big threats.

<span style="color:#58d68d"><strong>monster_hunter</strong></span>  
Actively hunt nearby monsters.

<span style="color:#ec7063"><strong>aggressive</strong></span>  
Actively seek nearby untamed targets.

<span style="color:#af7ac5"><strong>passive</strong></span>  
Do not fight normally.

### 6.7 `/tames removeTarget`

Use:

```text
/tames removeTarget <name>
/tames removeTarget all
/tames removeTarget group <group>
/tames removeTarget type <type>
/tames removeTarget state <follow|sit|wander>
```

This clears the current target from selected loaded tames.
