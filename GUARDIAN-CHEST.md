Guardian chest automation (v56)
==============================

Build the setup vertically:

    Control chest: named drum, bound adoption papers, food
    Supporting drum (placed by the owner)
    Flat 3 x 3 layer of nine chest blocks, centered below the drum

The nine lower chests activate the structure. Put the control items and food in
the top chest. Regular or trapped chest blocks work for the lower layer.

1. Rename a spare drum to `<your player name>:<guardian deployment group>` and
   put it in the top chest. For example: `Bruns:nightwatch`.
2. Put adoption papers bound to your tames in that same chest. Each becomes a
   permanent guardian paper named `<group>: <tame name>`. These papers can never
   adopt or transfer a tame again, including after removal from the chest.
3. Existing posts in that named guardian deployment group are reused. Otherwise,
   take a converted paper out and right click a block to save its tame's post
   next to the clicked face (click the top of the ground for a post above it).
   Sneak right click a block or the air with the paper to deploy that tame.
4. Put the papers back in the top chest. Power the supporting drum or top chest
   to deploy linked tames. Remove power to release them and send them home sitting.

The control drum and papers must name the supporting drum's owner and the same
deployment group. Ambiguous labels for different groups disable the controller.
Owner checks use UUIDs, including for offline owners. Guardian papers remain
bound to their original control owner and group. A different group needs its
own papers. Removing a paper stops that chest's control of its tame and cancels
its queued moves; it does not release a guardian that has already deployed.

Bodyguards are always skipped. Other guardian deployments can be replaced.
Ranked membership is removed on recall. If the tame is fighting in a ranked
round, its side forfeits that round before guardian deployment, preventing a
later duel snapshot restoration from moving it back. Ordinary duels retain their
command lock. Dead tames and tames stored outside ranked are skipped.

The normal chest x drum feeding still works for nearby tames of any owner.
Additionally, linked assigned guardians can receive the top chest's food at any
distance or in another dimension, even while their entity is unloaded. Food is
transferred into their saved hunger inventory; remote feeding never loads their
chunks. Unloaded feeding uses learned or configured diet rules; successful live
feeding also teaches that food for later unloaded feeding. Removing the paper stops remote feeding. Food acceptance, green refill
limit, inventory capacity and preferred-food ordering still apply.

Only loaded, active chest x drums work. Existing overlap deactivation still
applies. Lower chests are the activation structure, not extra food inventories.
Breaking or unloading the controller cancels its pending automation; deployed
guardians keep their ordinary guardian behavior. Loading a powered controller
reconciles its present papers and deploys them again. New papers in a powered
controller are also deployed. An initially unpowered controller issues no move.

Performance: loaded-drum indexing only; no global tame or block scans for control.
The existing once-per-second index refresh queues setup checks. Signal changes
prioritize a known setup. At most one setup and one tame move are processed per
tick. Queues coalesce by chest/tame, so rapid clocks select the latest state
instead of accumulating teleports. Cold source and destination chunks share the
existing guardian load budget (one cold request per ten ticks, at most eight
pending requests); ordinary unloaded tames use the single-chunk guardian wake
path rather than the legacy nine-chunk teleport path or stale snapshot copies. Retries expire
after 60 seconds. Missing group posts require setting a post and toggling power
again. Teleports do not charge the offline owner's XP.

Install the v56 jar instead of the earlier DI extension jar, keeping one DI jar.
No config edits or client-side protocol/registry additions are required.
In-game help: `/tames chestxdrum system`.
