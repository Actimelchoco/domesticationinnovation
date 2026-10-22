# Guardian wake sitting fix v67

Minecraft's SitWhenOrderedToGoal.canUse returns true for an on-ground tame whose
owner cannot be found, even when its ordered-to-sit flag is false. An offline
guardian can therefore wake correctly, teleport to its post and immediately
restart the native sit goal. The goal stops navigation and sets the sitting
pose, interfering with combat. Forcing monster_hunter mode alone does not
disable that goal.

GuardianSitGoalMixin prevents the native sit goal from starting or continuing
while a registered tame is an awake assigned guardian. Resting guardians,
hunger-forced sitting, bodyguards and ordinary tames retain their normal
behavior. This only adds a registry lookup to the goal predicates; it does not
add entity scans or activity checks. Existing guardian maintenance also repairs
a stale sitting pose even if the ordered-to-sit flag is already false.

The regression reproduces the failure on v66: the real offline wake path clears
both sit flags, but the native sit goal can immediately start again. The fixed
test also checks stale-pose recovery and retained resting/hunger/non-guardian
sitting. Tests run in the isolated Forge GameTest server using the v66 guardian
activation test setup. The IDE had no isolated GameTest debug configuration;
evidence came from the actual Forge regression, not a debugger session.

Includes all v66 activation features. Replace the prior DI extension JAR with
v67; no configuration changes are required.
