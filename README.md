# Dome Survival — stable 0.1.1

Minecraft Forge mod for the WASTED / GOTEICRAFT survival project.

## Target

- Minecraft 1.20.1
- Forge 47.4.10
- Java 17
- Curios 5.14.1
- CoFH Core 11.0.2.56
- Thermal Core 11.0.6.24
- CustomNPCs-Unofficial 1.20.1.20260711

## Build

```powershell
.\gradlew.bat clean build --console=plain
```

The release build uses public Maven/CurseMaven dependencies. The old generated `devmods`
flat-directory dependency block is no longer required.

## Stable scope

The 0.1.1 stable snapshot contains the current production code for:

- dome survival and surface hazards;
- oxygen equipment and sealed-room oxygen simulation;
- machines and energy storage;
- energy, fluid, oxygen and item transport networks;
- tiered hoppers;
- universal tank;
- airlock gate/control system;
- oxygen reclamation complex;
- technical service pass-through;
- CustomNPCs progression integration.

Historical patch archives, build logs, test-world copies, Forge MDK duplicates and local
modpack jars are intentionally excluded from source control.
