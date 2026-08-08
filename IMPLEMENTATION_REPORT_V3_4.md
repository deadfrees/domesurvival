# Implementation report — V3.4

## Architecture

- Server remains authoritative for weather type, direct exposure and damage.
- `solarActive` is synchronized separately from `solarExposed`, allowing protected clients to render a hostile clear-day exterior without receiving damage visuals.
- Client visual transitions are interpolation-only and do not affect gameplay state.
- Custom particles are registered with Forge `DeferredRegister<ParticleType<?>>` and client providers are registered on `RegisterParticleProvidersEvent`.
- Weather particles are spawned locally. No per-particle server packets are generated.

## Performance

- No block scans or chunk scans were added.
- Dome exterior particle placement uses fixed dome geometry and O(1) arithmetic.
- Weather particle counts are bounded per client tick.
- Network synchronization still occurs only when the compact visible state changes.

## Resources

- `domesurvival:acid_rain_streak`
- `domesurvival:sandstorm_mote`
- `assets/minecraft/textures/environment/rain.png` (acid rain override)
- `assets/minecraft/textures/environment/sun.png` (red-hot sun override)
- Existing V3.3 rain/sandstorm OGG ambience files are preserved.

## Compatibility note

The sun and rain overrides are normal resource assets. A user-selected resource pack with higher resource priority can replace them visually; gameplay damage remains server-authoritative.
