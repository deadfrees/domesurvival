# DomeSurvival Lanos flush-mounted lights/badges hotfix

This patch does NOT change vehicle scale, Y alignment, registry IDs, blocks, collision, or Creative Tab.

Changed on both Lanos models:
- front headlights and indicators moved closer to the front body surface;
- front Chevrolet badge outline/fill moved closer to grille;
- rear lamp assemblies moved flush toward the rear body surface;
- rear Chevrolet badge moved flush to the rear surface.

Install over the current project and run:

```powershell
cd C:\domesurvival
Expand-Archive -Path "$env:USERPROFILE\Downloads\domesurvival_lanos_flush_lights_badges_fix.zip" -DestinationPath "C:\domesurvival" -Force
.\gradlew.bat clean build
.\gradlew.bat runClient
```

Test:

```mcfunction
/give @s domesurvival:lanos_decorative
/give @s domesurvival:lanos_abandoned
```

Check front and rear from side/3-4 angle. The car ground contact should be unchanged.
