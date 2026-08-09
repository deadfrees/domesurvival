# DomeSurvival V3.2.5 — M40-inspired mask and backpack harness

## Goal
Refine the accepted V3.2.4 visuals with two concrete changes:

1. Extend the bottle straps so they clearly read as backpack harness straps.
2. Rework the mask using the uploaded M40 gas mask as design reference, recolored into the DomeSurvival cyan/graphite palette.

## What changed

### Mask
`OxygenMaskModel` was redesigned to better match the uploaded gas mask reference:

- wider sealed visor;
- side cheek housings;
- compact central mask body;
- distinct front filter cartridge.

Important:
- this is still an original Minecraft-adapted interpretation;
- it uses the same cyan/graphite palette already established by the tank and prior mask;
- it does not wrap the whole head in a helmet shell.

### Tank
`OxygenTankModel` keeps the already approved back bottle and now continues the harness:

- longer front straps;
- visible shoulder bridge connecting front and back;
- rear straps retained.

This should read more clearly as a backpack harness while still avoiding a chest plate.

## Textures
Updated:
- `oxygen_mask.png`
- `oxygen_tank.png`

## Gameplay
No oxygen mechanics changed.

Still true:
- tank drains only outside breathable atmosphere;
- dome atmosphere does not refill tanks;
- removing the mask stops tank drain;
- each tank has independent NBT oxygen;
- empty tank falls back to personal reserve;
- zero oxygen still deals 2 hearts/sec.

## Structure / protocol
- structureVersion = 7/7
- no `/dome upgrade`
- network protocol remains V3.2 protocol 2

## Acceptance checklist
Front:
- mask resembles a compact gas mask more than goggles;
- visible central filter;
- no oversized helmet shell.

Torso:
- straps continue and read as backpack straps;
- still no bulky chest plate.

Back:
- successful small bottle preserved.
