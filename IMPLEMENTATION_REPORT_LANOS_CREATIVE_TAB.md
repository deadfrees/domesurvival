# Implementation report

## Registry compatibility

The Lanos registry IDs were intentionally kept unchanged:

- `domesurvival:lanos_decorative`
- `domesurvival:lanos_abandoned`

This replaces the old visual assets rather than creating duplicate/new block IDs.

## Performance

Both vehicles remain static model blocks with:

- no BlockEntity;
- no server tick;
- no client tick;
- no packets;
- no physical collision shape.

The larger 7x3 geometry only changes the baked visual model and selection outline.

## Creative tab design

The creative tab is registered through the Forge mod event bus using `RegisterEvent` for
`Registries.CREATIVE_MODE_TAB`.

The content callback queries the item registry after registration and accepts all items with
namespace `domesurvival`. This intentionally avoids coupling the tab to a single `ModItems` or
`ModBlocks.ITEMS` instance, allowing both developers to register new items independently.
