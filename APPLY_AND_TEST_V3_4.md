# Apply and test V3.4

## Install

From PowerShell:

```powershell
cd C:\domesurvival
git checkout feature/surface-hazards

Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_surface_weather_v34_patch.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`.


## Important: existing server config

Forge does not overwrite values already saved in an existing world server config. If this world was already launched with V3.3, locate `domesurvival-server.toml` and either regenerate it or set:

```toml
[surface_hazards.acid_rain]
visibility = 144
thunder_visibility = 112
```

For a dev world you can locate it with:

```powershell
Get-ChildItem -Path .\run -Recurse -Filter "domesurvival-server.toml"
```

## Confirm oxygen was not edited

```powershell
git diff -- src/main/java/com/wasted/domesurvival/core/oxygen
git diff -- src/main/java/com/wasted/domesurvival/forge/oxygen
git diff -- src/main/java/com/wasted/domesurvival/forge/client/OxygenHudOverlay.java
```

Expected: no output.

## Test acid rain

```mcfunction
/weather rain
```

1. Stand inside the middle of the dome and look through side glass and roof glass.
2. Green vanilla rain sheets and custom fine streaks should be visible outside the shell.
3. Walk outside: the green atmosphere should strengthen gradually instead of popping in one frame.
4. Outdoor rain view distance should be noticeably longer than V3.3.
5. Return inside: strong fog should fade rather than disappear instantly.

## Test thunderstorm

```mcfunction
/weather thunder
```

Expected: denser rain, darker green atmosphere, thunder sound and the same smooth dome transition.

## Test sandstorm

```mcfunction
/weather clear
/dome weather sandstorm start
```

1. From inside the dome, look through multiple glass walls: moving orange/gold motes should be visible immediately outside the shell.
2. Outside the dome, the storm should become much denser and visibility should fall sharply.
3. Particle movement should be fast and predominantly horizontal, with visible turbulence.

Stop:

```mcfunction
/dome weather sandstorm stop
```

## Test lethal sun

```mcfunction
/weather clear
/time set noon
```

1. From inside the dome, confirm the sun texture is red/orange and the exterior sky has a mild warm hostile cast.
2. Walk outside into direct sky exposure.
3. The heat vignette should fade in smoothly from the edges instead of showing a hard rectangular frame.
4. Walk back under protection and confirm the vignette fades out smoothly.
