# Changelog

## 0.1.1 — stable

- Consolidated the current Forge 1.20.1 / 47.4.10 production source.
- Removed historical patch/build artifacts and backup source copies from release tracking.
- Replaced the generated 68-jar local `devmods` build dependency block with explicit Maven dependencies.
- Preserved the current GOTEICRAFT machine, oxygen, pipe, hopper, tank, airlock and progression systems.
- Preserved Survival drops for all non-dome blocks.
- Preserved dome-only destruction protection.
- Consolidated pipe-wrench interaction handling into one event subscriber.
- Removed obsolete transport item-model v4/v5 intermediates.
- Removed unused historical Joseph GUI texture generations.
