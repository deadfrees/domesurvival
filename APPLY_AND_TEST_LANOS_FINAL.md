# Apply and test

Close Minecraft, then from PowerShell:

```powershell
cd C:\domesurvival
Expand-Archive `
  -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_final_corrected_models.zip" `
  -DestinationPath "C:\domesurvival" `
  -Force

.\gradlew.bat clean build
.\gradlew.bat runClient
```

Test items:

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Check both cars from front/rear/side, all four facings, inventory icons, and verify the tyres sit on the ground.
