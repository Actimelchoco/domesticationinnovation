# Short tame type selections v75

Type completions consistently use the entity type path (typeId) for modded and
vanilla tames. Full modId:typeId input remains supported. When different mods
register the same path, completions retain their mod prefixes for exact selection.

Includes owned/admin/leaderboard type lists, known/disabled types, summon types,
ranked forbidden types, do-not-attack types and call-order override type lists.
Single-type admin settings resolve unique short names to canonical registry IDs;
ambiguous short names are rejected. Summon commands preserve raw short input.
Stored entity IDs and world data are unchanged. No periodic work is added.

Includes previous v74 ranked death fix, v73 dragon fireball impact correction,
and v72 100 qualifying tames per owner.
Stop the server and replace the previous extension JAR. No config changes.

Validation: both Forge regression tests passed, including actual short-ID summon commands; production build passed.
