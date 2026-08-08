# DomeSurvival V3.4.3 — Atmosphere tuning

This patch keeps the V3.4.2 particle/resource fixes and retunes the actual weather atmosphere based on in-game feedback.

## Acid rain
- Green toxic atmosphere restored both outside and while protected inside the dome.
- Adds only light distance fog instead of the old opaque wall.
- Protected player: ~94% of normal far distance.
- Exposed player: ~88% of normal far distance.
- A subtle green screen cast makes the weather readable even when the horizon fog is far away.

## Acid thunderstorm
- Stronger toxic-green atmosphere than normal rain.
- Still retains long-range visibility.
- Protected player: ~90% of normal far distance.
- Exposed player: ~80% of normal far distance.

## Sandstorm
- The storm now has a much stronger orange/brown atmospheric body, not just visible motes.
- Fog is visible through the dome glass.
- Protected visibility target: at least 72 blocks.
- Exposed visibility target: at least 30 blocks (or the configured sandstorm visibility if larger).
- Stronger orange viewport tint and a subtle full-screen dust cast.

## Lethal clear-day sun
- Solar visibility raised substantially: effective exposed minimum is 192 blocks.
- Protected view uses a mild haze target of at least 224 blocks.
- A very faint warm screen cast remains visible from inside the dome.
- The full heat vignette is still only shown while directly exposed.

## Compatibility
- No oxygen classes are modified.
- No network protocol change from V3.4.2.
- No mandatory external shader pack.
