# DomeSurvival V3.3 — Weather visual/audio tuning

Branch: `feature/surface-hazards`
Minecraft: 1.20.1
Forge: 47.4.x (project baseline 47.4.10)

## Player-feedback fixes

### Acid rain
- Replaced the large `FALLING_SPORE_BLOSSOM` look with small lime `DustParticleOptions` droplets.
- Rain particles now spawn every client tick and at much higher density.
- Thunderstorms spawn even denser/faster droplets.
- While the player is inside the glass dome, an always-visible exterior rain sheet is spawned outside the dome in the camera-facing direction. This makes the storm readable through the glass without putting lethal effects inside the protected volume.
- The provided `дождь.mp3` was converted to Vorbis OGG and registered as `domesurvival:acid_rain_ambience`.
- Acid thunderstorms layer periodic low-pitch vanilla thunder rumbles over the provided rain ambience.

### Sandstorms
- Replaced large sand block particles with much smaller orange/gold dust particles.
- Increased particle density and horizontal velocity.
- Wind direction now slowly changes and adds turbulence instead of moving every particle in the same rigid direction.
- While protected inside the dome, a camera-facing exterior dust field remains visible through the glass.
- The provided `песчаная буря.mp3` was converted to Vorbis OGG and replaces the old temporary sandstorm ambience.

### Lethal sun / desert atmosphere
- Added server-synchronized `solarExposed` state.
- Clear-sky daytime exposure now applies an amber heat haze and shorter desert horizon visibility.
- Added a subtle pulsing orange/red glare at the screen edges and tiny rising hot-dust particles.
- Visuals are client-side only; the server remains authoritative for damage.

### Damage cadence
- Hazard damage is now evaluated every **10 ticks (0.5 seconds)** instead of every 20 ticks by default.
- Damage-per-hit values remain the same. The increase comes from hit frequency as requested.
- A 5-tick phase offset keeps surface hazard hits away from the oxygen system's normal whole-second boundary.
- Minimum configurable cadence is 10 ticks because faster `LivingEntity#hurt` calls become unreliable due to vanilla hurt cooldown.

## Networking
`SurfaceWeatherSyncPacket` gained `solarExposed`, therefore DomeSurvival network protocol is now **3**. Client and server must use the same updated JAR.

## Compatibility / performance
- No mandatory shader pack.
- Uses Forge `ViewportEvent` for fog and GUI tinting.
- Particles are client-local; no particle packet spam.
- Dome geometry checks are O(1); no flood fills, no chunk scans and no forced chunk loads.
- Oxygen code/HUD was not modified.
