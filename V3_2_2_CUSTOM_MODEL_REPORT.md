# DomeSurvival V3.2.2 — Custom oxygen equipment models

## Goal

Address the latest visual review comments:

1. The mask must not protrude beyond the player's head and should read as goggles.
2. No chest plate should be visible from the front.
3. Remove the wing-like side/shoulder elements.
4. Add a small 3D oxygen tank on the back.

## What changed

### Oxygen Mask
- `OxygenMaskItem` now provides a custom humanoid armor model on the client.
- The custom model adds a compact goggles band and thin side straps only.
- The face equipment stays close to the vanilla head silhouette and is no longer a bulky front box.

### Small Oxygen Tank
- `OxygenTankItem` now provides a custom humanoid armor model on the client.
- The chest-slot equipment is rendered as a small 3D back-mounted tank.
- There is no front chest plate.
- There are no wing-like shoulder blocks.
- Only thin rear attachment strips remain behind the player.

### Client registration
- Added model layer registration in `ClientModEvents`.
- Added two model classes:
  - `OxygenMaskModel`
  - `OxygenTankModel`
- Added a tiny client-side cache:
  - `OxygenEquipmentModelCache`

### Texture
- Replaced the generic flat armor atlas with a sparse transparent atlas specifically for:
  - goggles;
  - side straps;
  - small rear oxygen tank.

## Gameplay/mechanics

No gameplay logic changed from V3.2/V3.2.1.

Still true:
- tank oxygen is consumed only in non-breathable atmosphere;
- returning to the dome does not refill the tank;
- removing the mask stops tank consumption;
- every tank keeps its own NBT oxygen amount;
- an empty tank falls back to the 20-second personal reserve;
- zero oxygen still deals 2 hearts per second.

## Network/performance

No new simulation cost was introduced.

V3.2.2 changes only:
- client rendering of the mask;
- client rendering of the chest oxygen module.

The server oxygen update frequency remains once per second.
No additional world scans, chunk scans or inventory scans were introduced.

## Structure

No structure change.
`structureVersion = 7/7`
No `/dome upgrade`.

## Acceptance checklist

Front view:
- mask looks like goggles;
- no bulky face box;
- no chest plate.

Side view:
- no oversized shoulder wings.

Rear view:
- small readable 3D oxygen module on the back.

Mechanical regression:
- oxygen behavior remains identical to V3.2.1.
