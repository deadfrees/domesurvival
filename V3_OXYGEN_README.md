# DomeSurvival V3 — Oxygen MVP

## Implemented

- Server-authoritative player oxygen reserve.
- One oxygen simulation update per second (20 ticks), not every tick.
- O(1) zone checks based on existing DomeBounds.
- No block scanning, flood-fill, chunk loading, or neighbor traversal.
- Starter dome + underground safe zone are breathable.
- Airlock breathability follows current AirlockState.
- Outside zone drains oxygen.
- Baseline reserve: 20 seconds.
- Return to breathable air restores 5 units/sec.
- At 0 oxygen: drowning-type damage every 2 seconds, 2 damage points.
- Creative/spectator bypass.
- Oxygen only applies to Overworld in this MVP.
- Worlds without a generated DomeSurvival structure remain breathable.
- S2C sync packet: two VarInts + boolean.
- Network packet is sent only when oxygen or breathable-state changes.
- HUD: 10 bubble icons, normalized against current max capacity.
- HUD hides when full and breathable.
- HUD is moved up if vanilla underwater air bubbles are visible.

## Future extension points already reserved

`PlayerOxygenData.maxOxygen(Player)` is the intended place to add suit/tank capacity.

`OxygenEnvironment` is the intended atmosphere query boundary. Future player-built airtight
rooms should be represented as cached volumes, not scanned every player tick.

The packet already sends both current and maximum oxygen, so increasing capacity with tanks
does not require a HUD protocol redesign.

## Current balancing constants

- update interval: 20 ticks / 1 second
- base max oxygen: 20
- outside drain: 1/sec
- breathable refill: 5/sec
- suffocation damage: 2.0 every 2 sec at zero

These are initial test values, not final balance.

## Structure version

No structure geometry changes. `/dome status` remains `7/7`.
Do not run `/dome upgrade` for this patch.
