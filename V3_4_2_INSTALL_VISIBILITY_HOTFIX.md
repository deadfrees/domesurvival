# DomeSurvival V3.4.2 — Installation & Visibility Hotfix

## Root cause fixed
V3.4.1 was accidentally distributed with an extra top-level directory. Extracting it into `C:\domesurvival` created `C:\domesurvival\domesurvival_surface_weather_v341_hotfix\...` instead of replacing the real project `src`. As a result the game continued to run V3.4.

This V3.4.2 archive is deliberately FLAT: `src/` is at the ZIP root.

## Changes
- Correct particle description texture IDs (`domesurvival:acid_rain_streak`, `domesurvival:sandstorm_mote`).
- Acid rain particle texture changed to an atlas-safe square 16x16 transparent streak.
- Acid rain and acid thunderstorm do not alter fog distance: render distance stays at the normal/clear-weather distance.
- Protected players inside the dome now get proxy rain/sand particles at the section of the OUTER glass shell they are looking toward.
- Clear lethal-sun weather now also spawns subtle heat dust on the OUTER glass shell while the player remains protected inside.
- Solar tint through the glass is slightly stronger so the hostile exterior is readable from inside without applying the exposed-player damage vignette.
- Strong solar vignette remains exposure-only.
- Sandstorm remains the only weather state that intentionally reduces visibility.
- Oxygen implementation is untouched.
