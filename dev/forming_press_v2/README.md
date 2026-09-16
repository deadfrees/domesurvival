# Forming press and native wrench review

`runtime_probe/FormingPressProbe.java` is an isolated development source set; it is
not included in the mod JAR. It starts a fresh flat world, runs processing, port,
module, survival and actual item-use checks, then opens the networked GUI and
captures its tabs at scales 2 and 3. It exits the client after reporting results.

Run from the repository root using Java 17:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File dev/forming_press_v2/run_review.ps1
```

The runner temporarily holds production mod JARs, as the full development profile
already loads them through Gradle, and restores them in `finally`. Do not launch
another client or release build until this runner exits. `runtime/checks.txt`
appends results; archive it before a new review. Logs and screenshots are local.
The isolated client's `options.txt` controls review language.

After `gradlew.bat -PdomeFullDev=true build --offline`, save output to `build.log`
and run `verify_release.py` with Python. The verifier checks runtime results,
packaged resources, native wrench bytecode, absence of probe classes and restored
production mods. `release_validation.json` records the artifact hash.

Repeatable art generators live in `source_assets/blender/scripts/`:
`create_forming_press_v2.py` and `create_forming_press_gui.py`.
