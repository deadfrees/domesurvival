# DomeSurvival — Lanos v3 model replacement

This patch replaces only the two Lanos resource models with the user-provided `lanos(3)` / `lanosold(3)` versions.

Registry IDs remain unchanged:
- `domesurvival:lanos_decorative`
- `domesurvival:lanos_abandoned`

Creative tab / Java registration are not changed.

## Apply
```powershell
cd C:\domesurvival
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_v3_models_patch.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

.\gradlew.bat clean build
.\gradlew.bat runClient
```

## Test
```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```
