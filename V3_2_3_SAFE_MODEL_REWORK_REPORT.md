# DomeSurvival V3.2.3 — Safe model rework

## Status of V3.2.2
V3.2.2 is considered visually rejected and should not be merged as the final model implementation.

## Root cause fixed
The V3.2.2 custom models were based on `HumanoidModel.createMesh()`. That method creates
normal humanoid body cubes. Forge then rendered those body cubes together with our equipment,
which produced the unwanted chest shell, shoulder blocks and oversized head geometry.

V3.2.3 creates the HumanoidModel node hierarchy manually with EMPTY vanilla body parts.
Only our explicit equipment children contain geometry.

## Mask
- custom model renders only a thin goggles visor and side arms;
- no helmet shell;
- no respirator/front box;
- visor thickness is only 0.25 model pixels and sits almost flush with the vanilla face;
- mask has its own dedicated `oxygen_mask.png` texture.

## Tank
- custom chest-slot model renders geometry only behind the torso;
- no chest plate/front geometry;
- no shoulder geometry;
- small 4x7x2.75 back bottle;
- small valve and two short rear brackets;
- tank has its own dedicated `oxygen_tank.png` texture.

## Mechanics
No oxygen gameplay code was changed.

Still valid:
- tank drains only in non-breathable atmosphere;
- dome does not refill tanks;
- removing mask stops tank drain;
- each tank has independent NBT oxygen;
- empty tank falls back to 20-second personal reserve;
- zero oxygen deals 2 hearts/sec.

## Server performance
No server-side code was changed by V3.2.3.
This is a client rendering/resource correction only.

## Structure/network
- structureVersion remains 7/7;
- no `/dome upgrade`;
- network protocol remains V3.2 protocol 2.

## Acceptance
Front: only goggles should be visible; chest remains the player skin.
Side: no wings/shoulder shell.
Rear: small 3D bottle mounted on the back.
