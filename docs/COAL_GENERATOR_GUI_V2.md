# Coal generator control panel and ports

Scope: coal generator plus the explicitly requested buffer-module item model, existing branch `feature/infrastructure-phase-b`.

## Design references

- [Mekanism's official machine configuration guide](https://wiki.aidancbrady.com/wiki/Tutorials/Machine_Configuration): front-facing side diagram, explicit input/output permissions, secondary configuration panel. The guide describes an older interface; it is a design reference, not an API dependency.
- [Team CoFH Steam Dynamo](https://teamcofh.com/docs/1.12/thermal-expansion/steam-dynamo/): clear separation of fuel, energy storage and machine controls.
- Visually inspected the installed Thermal Core 11.0.6.24 `item_dynamo.png` and Immersive Engineering 10.2.0-183 `blast_furnace.png`. Thermal keeps the slot layout simple; IE ties the background's material to the machine. No third-party artwork is copied into the mod.

## Artwork and GUI

Original physical control panel generated through Blender Python: graphite casing, satin metal bevels, brass nameplate, recessed readouts, fasteners, cooling slots and a gear button. Colours follow the generator's approved black model and pipe materials. Soft lighting and fine procedural grain replace the previous stack of flat `fill()` rectangles.

- Source: `source_assets/blender/scripts/create_coal_generator_gui.py`.
- Editable scenes: `source_assets/blender/coal_generator_gui/coal_generator_gui.blend`.
- Main texture: 880 × 1064, displayed at 220 × 266 GUI units; linear texture filtering preserves smooth gradients.
- Configuration insert and widget atlas use the same four-times resolution.
- Original 37 menu slots and their coordinates are retained; one module slot is appended. Vanilla draws each item and count once.
- The settings button and configuration diagram stay inside the main window. The fuel slot remains accessible while configuring sides.
- Energy and fuel indicators follow synchronized server values. Generation reads zero when idle or when the buffer is full.
- Russian and English labels and tooltips describe the generator's actual capabilities.
- The name is restored to **Угольный генератор / Coal Generator**. The complete background is darkened by 0.6 exposure stops. The decorative blue stripe above the main fuel slot and orange hotbar divider are removed, while functional port mode colours remain in settings.

## Buffer module

The existing `domesurvival:buffer_module` is supported through the shared `IModularMachine` / `MachineModuleInventory` contract. Its established +75% capacity effect raises the buffer from 50,000 to 87,500 FE. Processing-cost and processing-speed modules do not have a meaningful matching operation here; flag-only modules likewise have no supported additional function. Only the buffer module is accepted, one at a time.

A separate chip-button tab contains the real module slot and effect description. The slot is hidden/inactive on other tabs; server-side insertion is restricted to this tab and the supported module. Removing an installed module is blocked while energy exceeds the base 50,000 FE, avoiding silent energy loss. Module data survives save/load and dismantling NBT; normal block removal drops it. Changing the module leaves combustion progress intact.

The module item has an original 37-cuboid 3D cartridge model: graphite frame, four capacitor housings, steel lids, blue polarity marks and brass edge contacts. It reuses the generator's approved satin material atlas. Source and preview: `source_assets/blender/buffer_module/`; generator: `source_assets/blender/scripts/create_buffer_module_v2.py`. The other five module models and behaviours are unchanged.

## Port contract

| Mode | Fuel items | Energy | Faces |
|---|---|---|---|
| Blue / INPUT | Coal, charcoal and existing coal coke accepted; no extraction | Receive only, up to 128 FE per capability operation | Top, bottom, left, right, back |
| Orange / OUTPUT | No item capability | Extract only; existing automatic output budget 128 FE/t | Top, bottom, left, right, back |
| OFF | Unavailable | Unavailable | All |
| Front | Unavailable | Unavailable | Always reserved for the firebox |

The machine creates energy, not item products. No ash recipe or output slot is introduced. Generation remains 64 FE/t and base capacity remains 50,000 FE. Existing default directions and saved side configuration remain compatible. Switching modes invalidates old capability optionals and updates the physical connector. External energy changes mark the block entity dirty for saving.

Unsided item capability queries are denied: the existing item transport retries without a side when a face refuses access. This closes that fallback without changing any pipe or other machine, so orange, OFF and front faces cannot secretly receive fuel.

## Verification

`dev/coal_generator_gui/run_review.ps1` runs an isolated client and restores temporarily held development mods in `finally`. Its probe is a separate Gradle source set, excluded from production.

The probe checks fuel and FE simulation, input/output isolation, capacity limits, saved data, all five usable sides for four facings, the blocked front, actual pipe transport, networked GUI side buttons, full-buffer synchronization, slot count and shift-click fuel insertion. Module checks cover supported/rejected items, legacy saves, 87,500 FE synchronization, removal protection, safe removal, preserved fuel progress, dismantling NBT and single module drops. It captures idle, working, full-buffer, configuration and module views at GUI scales 2 and 3. Release verification is recorded in `dev/coal_generator_gui/release_validation.json`.
