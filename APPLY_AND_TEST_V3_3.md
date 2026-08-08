# Apply & test V3.3

## Apply
Extract the patch over `C:\domesurvival` while on `feature/surface-hazards`.

```powershell
cd C:\domesurvival
git checkout feature/surface-hazards
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_surface_weather_v33_patch.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force
```

## Verify oxygen is untouched

```powershell
git diff -- src/main/java/com/wasted/domesurvival/forge/oxygen
git diff -- src/main/java/com/wasted/domesurvival/forge/client/OxygenHudOverlay.java
```

Expected: no output.

## Build

```powershell
.\gradlew.bat clean build
```

Expected: `BUILD SUCCESSFUL`.

## Acid rain

```mcfunction
/weather rain
```

Check:
- droplets are much smaller than V3.2;
- droplets are dense/continuous;
- outside under open sky: acid damage arrives about every 0.5 s;
- rain ambience from the supplied audio is audible;
- under a roof: no direct rain damage;
- inside the dome: no rain damage, but exterior rain remains visible through the glass and ambience is muffled.

## Acid thunderstorm

```mcfunction
/weather thunder
```

Check:
- darker green atmosphere;
- denser/faster acid droplets;
- supplied rain ambience plus additional thunder rumble;
- thunderstorm damage arrives about every 0.5 s while exposed.

## Sandstorm

```mcfunction
/weather clear
/dome weather sandstorm start
```

Check:
- particles are small orange/gold dust, not large sand-block squares;
- particles move quickly sideways with changing/turbulent wind;
- supplied sandstorm ambience is audible;
- exposed damage arrives about every 0.5 s;
- inside dome: no damage, outside dust remains visible through glass, ambience is muffled.

Stop:

```mcfunction
/dome weather sandstorm stop
```

## Lethal sun

```mcfunction
/weather clear
/time set noon
```

Go outside the dome with open sky. Check:
- amber desert heat haze;
- orange/red pulsing edge glare;
- small rising hot-dust particles;
- solar damage arrives about every 0.5 s;
- roof and dome protection still stop the solar hazard.
