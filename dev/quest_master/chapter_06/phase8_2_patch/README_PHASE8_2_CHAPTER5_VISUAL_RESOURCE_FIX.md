# DomeSurvival Quest Phase 8.2 — Chapter 5 visual/resource fix

This patch updates the existing energy chapter `4A2E731D5C9B684F` without replacing quest IDs or changing its global completion/reward flow.

## Changes

- Player-facing title is `Глава 5 — «Пусть горит свет»`.
- The old Chapter 5 removal notice and player-facing Chapter 6 wording are absent.
- The background is a new high-detail industrial power workshop beneath the Dome, with no text, watermark, frame, or embedded UI.
- The FTB theme continues to use the native per-chapter background ResourceLocation `domesurvival:textures/gui/quests/chapter_06_power.png`.
- Mechanism quest icons use real DomeSurvival registry IDs:
  - `domesurvival:coal_generator`
  - `domesurvival:basic_energy_pipe`
  - `domesurvival:energy_buffer`
  - `domesurvival:airlock_control_panel`
- Item models, parent block models, blockstates, and required textures are included in the payload.
- DomeSurvival generator/link/storage detection now checks exact canonical registry IDs first and keeps registry-path compatibility as a fallback.
- `ForgeCapabilities.ENERGY`, all existing `CH6_*` save-safe internal action names, global rewards, offline catch-up, and `POWER_INFRASTRUCTURE_ESTABLISHED` are preserved.

## Install

```powershell
powershell -ExecutionPolicy Bypass -File .\INSTALL_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX.ps1 -Project "C:\domesurvival"
```

The installer backs up every replaced target under `C:\domesurvival\backups\phase8_2_chapter5_visual_resource_fix_<timestamp>` and also refreshes the active `run/config/ftbquests` chapter when that dev runtime exists.

## Compile resources

```powershell
powershell -ExecutionPolicy Bypass -File .\COMPILE_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX.ps1 -Project "C:\domesurvival"
```

This deliberately runs `compileJava processResources` without `clean`.

## Verify

```powershell
powershell -ExecutionPolicy Bypass -File .\VERIFY_PHASE8_2_CHAPTER5_VISUAL_RESOURCE_FIX.ps1 -Project "C:\domesurvival"
```

The verifier is Windows PowerShell 5.1-compatible and checks chapter numbering, 20 quest IDs, final flag, manual checkmarks, background dimensions/theme binding, real registrations, item/block/texture chains, processed resources, exact DomeSurvival energy detection, generic compatibility fallbacks, and Forge Energy integration.

## Dev run

```powershell
cd C:\domesurvival
.\dev\RUN_DEV_FULL.bat
```

The generated background prompt used the built-in image generator and requested a crisp 16:9 industrial power workshop beneath a glass survival dome, warm amber work lights, restrained cyan energy indicators, a readable central aisle, no text/UI/logo/watermark, and no third-party branding.

