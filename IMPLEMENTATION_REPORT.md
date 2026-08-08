# Implementation report — V3.2 Surface Weather

## Server

- `SurfaceWeatherService` owns weather classification, random sandstorm scheduling and state synchronization.
- `SurfaceWeatherSavedData` persists sandstorm duration/cooldown independently from dome and oxygen SavedData.
- `SurfaceHazardService` applies damage at a configurable whole-second cadence, offset by 10 ticks from the existing oxygen cadence.
- `SurfaceHazardEnvironment` performs O(1) dome classification + one `canSeeSky` lookup. No region scans, flood-fill or forced chunk loads.

## Network

- Added `SurfaceWeatherSyncPacket` (weather enum, direct-exposure boolean, optional remaining duration).
- Packets are sent only when the player's weather/exposure state changes.
- Network protocol bumped to 2 to reject incompatible older clients cleanly.

## Client

- No mandatory shader pack.
- Fog color and fog distance use Forge viewport events.
- Full-screen tint uses `RenderGuiEvent.Post`.
- Particles are client-local and therefore consume no server particle bandwidth.
- Sandstorm wind uses an embedded six-second OGG resource on the WEATHER sound channel.

## Default balance

- Solar: 2.0 health / second.
- Acid rain: 1.0 health / second.
- Acid thunderstorm: 2.0 health / second.
- Sandstorm: 1.0 health / second.
- Sandstorm visibility: 24 blocks.
- Random sandstorm duration: 2–5 minutes.
- Random interval: 15–40 clear-weather minutes.

All values are configurable in the Forge SERVER config.

## Validation performed in this workspace

- JSON resource files parse successfully.
- New common weather enum compiles with Java 17.
- Generated OGG validates as Vorbis, mono 44.1 kHz, 6 seconds.
- Oxygen source directories are byte-for-byte unchanged compared with the uploaded baseline.

A full ForgeGradle build still must be run on the developer machine because this sandbox does not contain the Forge/Gradle dependency cache required for an offline 1.20.1 build.
