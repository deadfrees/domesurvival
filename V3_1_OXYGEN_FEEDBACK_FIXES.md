# DomeSurvival V3.1 — Oxygen feedback fixes

Changes from playtest feedback:

1. Removed the `O2` text next to the oxygen bubbles.
   In Minecraft's pixel font it looked like the number `02`.
2. Replaced vanilla `drown()` damage with `generic()` damage.
   Zero oxygen no longer pretends the player is underwater and should no longer produce
   the drowning/bubble audiovisual feedback.
3. Increased zero-oxygen damage to 4 damage points (2 hearts) every second.
   This is intentionally stronger than full-hunger natural regeneration.
4. The server simulation interval remains 20 ticks (1 second), so there is no additional
   per-tick server load.
5. Dome structure remains version 7/7. No `/dome upgrade` is required.

Current test balance:
- reserve: 20 seconds
- drain: 1 oxygen/sec
- refill: 5 oxygen/sec
- zero-O2 damage: 2 hearts/sec
