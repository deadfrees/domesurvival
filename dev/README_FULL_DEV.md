# DomeSurvival FULL DEV V6.9 STABLE

This is the canonical Forge 1.20.1 development profile for the complete
DomeSurvival modpack.

## Architecture

- `run/mods` keeps the normal physical production modpack.
- DomeSurvival itself runs from `sourceSets.main`.
- Core hard dependencies are declared in `build.gradle` through `fg.deobf`.
- Remaining physical third-party JARs are copied to a generated local cache and
  attached through `fg.deobf`.
- A Java 17 development-only bridge repairs production SRG references that
  ForgeGradle does not remap inside problematic Mixin strings and nested JarJar
  archives.
- FULL DEV V6.9 invalidates older generated caches and explicitly verifies the
  Alex's Caves Minecraft camera mixin remap (`m_91288_ -> setCameraEntity`).
- Original `run/mods` JARs are never modified.
- Generated data lives under `dev/generated/` and is not committed.

## Requirements

- Java 17 JDK (`java` and `javac`)
- ForgeGradle 6 / Forge 47.4.x project
- approved physical third-party modpack in `run/mods`

The launch scripts automatically select an installed JDK 17 from `JAVA_HOME`,
the standard Oracle/Adoptium/Microsoft installation folders, or the location of
`javac.exe`. This keeps an older system-default Java from being selected by Gradle.

An optional external repair source can be configured through:

    setx DOMESURVIVAL_MODPACK_SOURCE "D:\path\to\approved\mods"

It is NOT required if `run/mods` already contains the approved physical pack.

## Developer 2 — first setup

    cd C:\domesurvival
    git pull --ff-only
    .\dev\SECOND_DEVELOPER_SETUP.bat

## Daily workflow

    .\dev\UPDATE_AND_RUN_FULL.bat

The daily launcher performs a safe fast-forward Git update and then starts the
complete FULL DEV environment.

## Direct full run without Git pull

    .\dev\RUN_DEV_FULL.bat

## Force rebuild after a bridge/cache compatibility failure

    .\dev\FORCE_REBUILD_FULL_CACHE.bat
    .\dev\RUN_DEV_FULL.bat

Use this path instead of invoking `gradlew -PdomeFullDev=true runClient` directly
when the generated deobfuscation cache may be stale.

## Recovery after interrupted Java/Gradle process

    .\dev\RESTORE_ALL_MODS.bat

## Diagnostics

    run\logs\FULL_DEV_GRADLE_LAST.txt
    dev\generated\mixin_srg_bridge_report.txt

## Important

The committed `build.gradle` already contains the FULL DEV hook. Stable launch
scripts do not patch `build.gradle` at runtime.
