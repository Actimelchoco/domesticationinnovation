# Modes Documentation

## Available Modes
- `default`: vanilla-like behavior; no forced owner-combat retarget from this mod.
- `default_plus`: modded default with more consistent owner combat retargeting/range handling.
- `boss`: when the owner starts battle, tames focus high-HP hostile targets.
- `bodyguard`: protects owner when hostiles are near/engaging owner.
- `monster_hunter`: hunts nearby hostile mobs aggressively (short local radius).
- `passive`: does not deal attack damage; keeps movement/following behavior.
- `aggressive`: hunts nearby untamed mobs aggressively (short local radius).

## Notes
- Mode goals now use goal selector priorities instead of one global tick behavior block.
- Passive blocks outgoing damage via combat-event guard.
