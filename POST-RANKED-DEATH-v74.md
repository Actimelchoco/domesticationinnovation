# Post-ranked death fix v74

Removed the persistent in-memory recent-duel-elimination markers. These could
survive ranked restoration and cause a later ordinary death to leave a tame's
registry record alive while its body was removed, requiring admin recovery.

The highest-priority tame death handler now uses current battle participation
(including stable TL identity) before normal-priority elimination/restoration runs.
Actual duel/ranked deaths keep their special restoration behavior. Later ordinary
deaths record the normal death history, penalty and next-morning respawn gate.
No extra periodic scans or per-tick work added.

Includes v73 dragon fireball surface impacts and v72 100 qualifying tames per owner.
Stop the server and replace the previous extension JAR. No config changes.
Existing missing bodies still require recovery; this prevents future occurrences.

Regression: unpatched code reproduced the post-ranked death misclassification.
All four isolated Forge regression tests passed after the fix; production build passed.
