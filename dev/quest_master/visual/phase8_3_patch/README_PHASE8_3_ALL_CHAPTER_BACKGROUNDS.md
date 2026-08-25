# DomeSurvival Quest Phase 8.3 — all chapter backgrounds

This incremental patch removes the `Глава 5` prefix from the energy chapter list title and replaces the five older 1024×576 chapter backgrounds with newly generated high-detail scenes.

## Updated scenes

- `Под Куполом` — the protected central shelter, airlock, water storage and young trees beneath the Dome.
- `Первые дни` — early beds, food/water storage, workbench and simple lived-in survival space.
- `За воротами` — the Dome airlock and a prepared short-range route into the hostile wasteland.
- `Здесь будут жить` — an organized permanent settlement with homes, workshop and shared infrastructure.
- `Не одним хлебом` — a practical multi-crop food system with irrigation, compost and pantry storage.

All five images were generated with the built-in image generator as crisp realistic game-concept backgrounds. They use a widescreen 16:9 composition, preserve a readable central region for quest nodes, and contain no text, HUD, quest UI, logo, watermark or third-party mod branding.

The archive also includes the already approved detailed energy-workshop background so all six visible chapter backgrounds install as one synchronized set.

## Install

```powershell
powershell -ExecutionPolicy Bypass -File .\INSTALL_PHASE8_3_ALL_CHAPTER_BACKGROUNDS.ps1 -Project "C:\domesurvival"
powershell -ExecutionPolicy Bypass -File .\COMPILE_PHASE8_3_ALL_CHAPTER_BACKGROUNDS.ps1 -Project "C:\domesurvival"
powershell -ExecutionPolicy Bypass -File .\VERIFY_PHASE8_3_ALL_CHAPTER_BACKGROUNDS.ps1 -Project "C:\domesurvival"
```

The installer creates a timestamped backup before replacing source assets and synchronizes the active `run/config/ftbquests` energy chapter when present. Compilation deliberately avoids `clean`.

## Dev run

```powershell
cd C:\domesurvival
.\dev\RUN_DEV_FULL.bat
```
