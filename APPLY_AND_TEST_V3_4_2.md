# Apply V3.4.2

1. Close Minecraft/dev client.
2. In PowerShell:

```powershell
cd C:\domesurvival
git checkout feature/surface-hazards

# Remove the accidentally nested V3.4.1 directory if it exists.
Remove-Item "C:\domesurvival\domesurvival_surface_weather_v341_hotfix" -Recurse -Force -ErrorAction SilentlyContinue

# Extract V3.4.2. This ZIP has src/ directly at its root.
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_surface_weather_v342_install_visibility_hotfix.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

# Verify that the ACTUAL project files changed.
Get-Content .\src\main\resources\assets\domesurvival\particles\acid_rain_streak.json
Get-Content .\src\main\resources\assets\domesurvival\particles\sandstorm_mote.json

.\gradlew.bat clean build
```

Expected particle JSON values:

```json
{"textures":["domesurvival:acid_rain_streak"]}
{"textures":["domesurvival:sandstorm_mote"]}
```

Run:

```powershell
.\gradlew.bat runClient
```

## Tests

Acid rain from inside dome:
```
/weather rain
```
- same view distance as clear weather
- green streaks visible outside the glass
- no red/black missing-texture cubes

Thunderstorm:
```
/weather thunder
```
- same view distance as clear weather
- denser acid rain + thunder/audio

Sandstorm:
```
/weather clear
/dome weather sandstorm start
```
- orange dust visible outside the glass
- reduced visibility is intentional only for sandstorm

Lethal clear day:
```
/dome weather sandstorm stop
/weather clear
/time set noon
```
- from inside: red/hot sun + warm exterior haze + heat dust near viewed outer glass
- from outside: stronger haze and smooth heat vignette
