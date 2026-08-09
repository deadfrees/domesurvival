# Lanos plate + wheels fix
- front plate text corrected to readable LANOS
- removed tire_vertical elements that created black square dividers inside the wheels

Apply over the current project root and rebuild:

```powershell
cd C:\domesurvival
Expand-Archive -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_plate_and_wheels_fix_final2.zip" -DestinationPath "C:\domesurvival" -Force
.\gradlew.bat clean build
.\gradlew.bat runClient
```
