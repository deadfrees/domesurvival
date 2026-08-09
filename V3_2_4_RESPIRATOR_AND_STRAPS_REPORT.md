# DomeSurvival V3.2.4 — respirator and harness straps

## Goal
Apply the latest visual review feedback on top of the accepted V3.2.3 baseline:

1. Add a compact volumetric respirator under the goggles.
2. Add thin visible harness straps for the back-mounted oxygen bottle.

## What changed

### Mask
`OxygenMaskModel` now includes:
- the existing thin visor;
- the existing side arms of the goggles;
- a new small 3D respirator body under the visor;
- a small lower cartridge/lip so the respirator reads as a separate volume.

Important:
- this is still NOT a helmet shell;
- the head silhouette remains mostly vanilla;
- the respirator is much smaller than the rejected V3.2.2 front box.

### Tank
`OxygenTankModel` keeps the existing small back bottle and now adds:
- two thin rear straps framing the bottle;
- two thin front straps near the torso edges.

Important:
- no chest plate was reintroduced;
- the front remains mostly the player skin, with only narrow strap lines;
- no wing-like shoulder geometry was added.

### Textures
Both dedicated textures were updated:
- `oxygen_mask.png`
- `oxygen_tank.png`

The separate-texture approach from V3.2.3 remains unchanged.

## Gameplay
No oxygen gameplay logic changed.

Still true:
- tank drains only outside breathable atmosphere;
- dome atmosphere does not refill tanks;
- removing the mask stops tank drain;
- each tank has its own NBT oxygen;
- an empty tank falls back to the 20-second personal reserve;
- zero oxygen still deals 2 hearts/sec.

## Performance / network
No server-side logic changed.
This is a client visual patch only.

## Structure
- structureVersion remains 7/7
- no `/dome upgrade`
- network protocol remains V3.2 protocol 2

## Acceptance checklist
Front:
- visor visible;
- small respirator visible under the visor;
- no large face box;
- no chest plate.

Back:
- small bottle visible;
- thin straps visible.
