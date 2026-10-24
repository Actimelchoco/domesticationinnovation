# Admin respawn morning bypass v76

/tames admin respawn (single, indexed archive and all) and /tames admin player
<player> respawn/arise/reincarnated variants now bypass the next-morning respawn
wait. Their existing admin permission requirements remain in place.

Ordinary player respawn/arise commands still enforce the next-morning gate,
even when an operator uses them. Other respawn checks remain in effect.
The bypass does not reset world time or alter deadlines for other dead tames.

Includes all v75 changes and prior fixes. No configuration changes.
Stop the server and replace the previous extension JAR; keep only one copy.

All 16 Forge regression tests passed; production build passed.
