# Apply and test V3.2

1. Work only in `feature/surface-hazards`.
2. Extract this ZIP into the repository root with overwrite enabled.
3. Verify oxygen files were not changed.
4. Build and then run the client.

PowerShell:

```powershell
cd C:\domesurvival
git checkout feature/surface-hazards

Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_surface_weather_v32_patch.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

# Must print nothing:
git diff -- src/main/java/com/wasted/domesurvival/core/oxygen
git diff -- src/main/java/com/wasted/domesurvival/forge/oxygen
git diff -- src/main/java/com/wasted/domesurvival/forge/client/OxygenHudOverlay.java

.\gradlew.bat clean build
.\gradlew.bat runClient
```

## Test matrix

### Sun
- `/weather clear`, daytime, outside + open sky: solar damage.
- same position under solid roof: no solar damage.
- inside dome: no solar damage.

### Acid rain
- `/weather rain`.
- outside + open sky: green atmosphere, acid particles, acid damage.
- under roof: no acid damage / heavy exposure effects.
- inside dome: no damage; only mild outside-atmosphere tint remains visible.

### Acid thunderstorm
- `/weather thunder`.
- outside + open sky: darker green atmosphere and stronger damage than rain.
- vanilla lightning/thunder continues normally.

### Sandstorm
- first `/weather clear`, then `/dome weather sandstorm start`.
- outside + open sky: tan fog, about 24-block visibility, moving sand, wind, damage.
- under roof: direct hazard stops.
- inside dome: no damage/heavy storm effects; mild outside tint remains.
- `/dome weather status` shows current mode and scheduler values.
- `/dome weather sandstorm stop` ends it.

### Oxygen regression
- perform the existing oxygen test outside and in the airlock.
- oxygen HUD and suffocation timing must be unchanged.
