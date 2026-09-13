# Fluid pipe development evidence

Only fluid resources are written by this pipeline. See `docs/FLUID_PIPE_AUDIT.md` and `docs/FLUID_PIPE_DESIGN.md`.

Run the isolated client from the repository root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File dev/fluid_pipes/run_fluid_review.ps1
```

The wrapper follows the repository FULL DEV launcher: production mod JARs are temporarily held inside `run/fluid-pipe-visual/production_mod_hold` and restored in `finally`. Existing worlds are never opened. The probe creates a new flat world in `run/fluid-pipe-visual/saves`, drives the actual client/server, captures PNGs and exits. Its source set is included only by `fluid_pipe_test.init.gradle`.

The fixture has 100 long-line pipes per tier, all junction types, three tank-to-tank networks, mixed tiers, actual BlockItem placement/breaking, item frames and dropped items. Tank contents are replenished/drained by test-only code after each measured tick, enabling sustained 2048 mB/t testing with 4000 mB tanks. The measured amount is read before replenishment. Water and lava are tested separately; a water-filled destination rejects lava for ten ticks.

`runtime/checks.txt` and `runtime/frame_times.csv` append on subsequent launches. Preserve the old `runtime` directory under another name before a new evidence run. Comparison pack `fluid_pipe_before` uses the backed-up 15 original JSON files; original texture PNGs remain unchanged in the mod.

For the final build, omit the init script:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17.0.12'
$env:PATH=$env:JAVA_HOME+'\bin;'+$env:PATH
.\gradlew.bat -PdomeFullDev=true build --offline --console=plain *> dev/fluid_pipes/build.log
& dev/fluid_pipes/runtime_blender/5.2/python/bin/python.exe dev/fluid_pipes/verify_release.py
```

Blender runtime binaries and noisy logs are ignored. `.blend`, `.bbmodel`, generated Minecraft JSON/PNG, review screenshots and structured validation results are retained. `baseline_hashes.json` is the original pre-fluid snapshot; `external_energy_snapshot.json` records the independent restoration of the already-approved energy family and prevents this work from overwriting it.
