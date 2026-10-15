Ordinary guardians activate while their owner is offline if a living,
non-spectator player is within 20 blocks of their saved post in the same
dimension. This is an inclusive spherical radius, twice the shared 10-block
Monster Hunter detection radius. Combat targeting range remains 10 blocks.

Performance limits (server-wide, shared across all owners and entry points):
- Only guardian records are queued; no repeated full-registry copy/scan.
- At most 32 queued records inspected per server tick, in round-robin order.
- Maintenance stops scheduling further work after a soft 0.5 ms time budget.
 Native transfers cannot be preempted; this is not a hard tick-time guarantee.
- Per-guardian rest checks are gated for 40-49 ticks, with staggered phases.
- Player positions are indexed in 32-block cells per dimension once per second.
 A proximity query reads at most nine nearby cells, cached for 40 ticks.
- At most two different guardians start rest/wake transitions per server tick.
- Missing chunks use asynchronous FULL-level tickets, one new request per ten
 ticks globally, at most eight pending. Tickets expire after five seconds.
- Missing-body retries back off for ten seconds. Shared chunk requests coalesce.
- Owner login enters the same queue; it never synchronously loads every tame.
- Bed-cache validation first schedules a cold bed through the same chunk budget.
- Hungry homeward walkers have at most four temporary 3x3 ticking-ticket groups
 globally. Overflow walkers use normally ticking chunks; queued maintenance
 still handles their home fallback. Unchanged walking chunks reuse tickets.

Arrival/departure normally takes a few seconds. Large cold groups can take
longer because loading and transitions queue rather than bursting in one tick.
Without a nearby visitor, guardians retain the existing TPHome rest behavior.
Hunger still suspends guarding; spectators and other dimensions do not count.

The /tames offlineGuardian commands, deployment quota and persistent post
tickets are removed. Legacy deployed posts migrate into normal anchors.
Undeployed legacy selections do not create posts; newer anchors take precedence.
Generic Forge startup ticket cleanup clears old saved tickets.
Temporary wake tickets and hungry walking tickets are released/expired; there
are no permanent guardian post tickets. A bed or destination may temporarily
load to transfer a tame, but the request is asynchronous and globally limited.

The GameTest suite covers behavior, save migration, permissions/removed commands,
shared rate limits, async missing-chunk and cold-bed deferral, hungry walking
ticket caps, and eventual queue progress for 500 records. Benchmark figures
are recorded separately in GUARDIAN_PROXIMITY_BENCHMARK.txt after validation.
The microbenchmark uses guardian data records and player objects; it does not
simulate 500 ticking mobs, or measure combat/pathfinding/total server TPS.
Native mod AI and crowded combat can still be expensive.

Reproduce:
gradlew.bat -I gradle/guardianProximityRegression.init.gradle "-PguardianModsDir=<mods folder>" runGameTestServer
