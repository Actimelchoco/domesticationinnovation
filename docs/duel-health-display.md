# Duel health display

The personal duel health display is off by default. Enable it through settings:

```text
/tames settings display duelHealth true
/tames settings display duelHealthMaxNames 6
```

Run either command without its value to see the current settings. The name limit
accepts 1–20 and defaults to 6 total across both teams. The shorter commands
`/tames settings duelHealth` and `/tames settings duelHealthMaxNames` also work.

While you or your tames participate in a duel or ranked match, the top of the screen
shows living, client-tracked tames in that match as `name HP/max HP`. No spyglass is
required. Rows alternate blue/red teams, with names sorted within each team, so
one large team does not occupy every slot. Unrelated matches, players, wild mobs,
and dead tames are excluded. Names leave the display when the duel ends or
they leave client tracking. Health updates from the live entity data.

The display works with duel glow disabled. It appears below the existing spyglass
readout when both are active. Long names are shortened to fit; small windows may
show fewer than the configured maximum so text stays in the top half of the screen.
Hide GUI also hides the display.

Preferences persist locally in `config/domesticationinnovation-client.properties`.
Changing glow preserves the health preferences and vice versa. Install the updated
JAR on the client to use these settings; the server's display menu lists them.
