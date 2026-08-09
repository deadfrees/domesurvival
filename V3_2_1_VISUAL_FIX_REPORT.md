# DomeSurvival V3.2.1 — Visual feedback fix

## Playtest feedback addressed

### 1. Strange tank indicator near the center of the HUD

Removed.

The client still receives OxygenSource (`ENVIRONMENT`, `TANK`, `RESERVE`) because
this information is useful for future UI and warnings, but V3.2.1 does not draw a
separate source icon.

The HUD is again only the approved cyan oxygen bubble row.

### 2. Equipment appearance on the player

Replaced the generic full-body grey/cyan armor texture with a sparse transparent
life-support equipment atlas:

- face: cyan goggles + respirator section;
- head sides/back: thin mask straps;
- chest front: harness straps only;
- chest back: visible cyan oxygen-cylinder panel;
- shoulders: narrow retaining straps;
- the rest of the player skin remains visible.

The equipment still has zero armor defense and is not a protective environmental suit.

## Oxygen behavior

No gameplay values changed.

- tank is consumed only outside breathable atmosphere;
- tank is NOT automatically refilled by returning to the dome;
- personal emergency reserve refills in breathable atmosphere;
- removing the mask stops tank consumption;
- each tank keeps its independent NBT oxygen amount;
- empty tank falls back to the 20-second personal reserve;
- zero oxygen still deals 2 hearts/sec.

## Server/network impact

None.

V3.2.1 is a client visual correction only:
- one Java HUD file changed;
- one armor PNG changed.

No new tick handlers, packets, block scans or inventory scans were added.

Network protocol remains V3.2 protocol `2`.

## Structure

No structure change.
`structureVersion = 7/7`.
Do not use `/dome upgrade`.
