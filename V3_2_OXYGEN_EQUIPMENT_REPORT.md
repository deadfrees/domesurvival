# DomeSurvival V3.2 — Oxygen equipment

## Implemented

### Oxygen Mask
Registry ID:
`domesurvival:oxygen_mask`

Equipment slot:
`HEAD`

The mask by itself supplies no oxygen. It must be used together with a compatible
oxygen module in the chest slot.

It provides zero armor defense. Sun/rain/radiation protection remains a separate,
future survival-suit system.

### Small Oxygen Module
Registry ID:
`domesurvival:small_oxygen_tank`

Equipment slot:
`CHEST`

Initial test capacity:
`120 O2`

Consumption:
`1 O2/sec`

Nominal duration:
`120 seconds / 2 minutes`

Each physical ItemStack stores its own oxygen integer in NBT.

Entering the dome does NOT refill a tank. Tank filling is reserved for V3.3
(Oxygen Filling Station).

### Breathing priority

1. Breathable environment
2. Equipped mask + non-empty chest oxygen module
3. Player emergency reserve (V3.1, 20 seconds)
4. Suffocation (V3.1, 2 hearts/sec)

While breathing from a tank, the emergency reserve recovers at the same 5 O2/sec
rate used in breathable atmosphere. This means a sufficiently long tank excursion
restores the full 20-second fallback reserve.

### HUD

The HUD remains ten cyan bubbles.

When the tank is active:
- bubbles represent tank percentage;
- a small cylinder glyph appears to the left of the bubbles.

When the tank becomes empty:
- the cylinder glyph disappears;
- bubbles immediately switch to the player's emergency reserve.

No text label was reintroduced.

### Server optimization

The authoritative simulation is still once per second.

Per player per oxygen update:
- one HEAD equipment lookup;
- one CHEST equipment lookup;
- O(1) dome/airlock geometry check;
- at most one integer NBT write when tank oxygen is consumed;
- no inventory scan;
- no chunk scan;
- no block flood-fill;
- no client-authoritative state.

Network traffic remains event/state-change based. During active tank use the HUD
receives one tiny packet per second because tank O2 changes once per second.

### Network compatibility

The DomeSurvival SimpleChannel protocol is bumped from `1` to `2` because
`OxygenSyncPacket` now carries the active OxygenSource.

Client and server must run the same V3.2 mod build.

### Structure

No world structure changed.

`structureVersion` remains `7/7`.

Do not run `/dome upgrade`.

## Test commands

Give test equipment:

`/give @s domesurvival:oxygen_mask`
`/give @s domesurvival:small_oxygen_tank`

Equip:
- Oxygen Mask -> helmet slot
- Small Oxygen Module -> chest slot

Recommended sequence:

1. Inside dome: tank does not decrease.
2. Exit dome with both items equipped:
   tank glyph appears and tank O2 decreases 1/sec.
3. Remove mask while outside:
   tank stops decreasing; HUD switches to personal reserve.
4. Put mask back:
   tank resumes supplying O2.
5. Remain outside until tank reaches zero:
   HUD switches to the full/partially recovered emergency reserve.
6. Stay outside through emergency reserve:
   V3.1 suffocation begins at 2 hearts/sec.
7. Return to dome:
   personal reserve refills; tank remains empty.
8. Relog/restart:
   tank NBT amount and player reserve should persist normally.

## Intentionally not implemented in V3.2

- crafting recipes/progression;
- tank filling station;
- multiple tank sizes;
- sun/rain protection;
- airtight player bases;
- player-built airlocks.

These are subsequent stages.
