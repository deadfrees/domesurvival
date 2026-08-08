# DomeSurvival V3.4 — Weather visibility, particles and lethal sun presentation

## Feedback addressed

- Acid rain was almost invisible after the previous particle-size reduction.
- Weather looked too different immediately after crossing the glass shell.
- From inside the dome the full outside storm was not readable enough.
- Sandstorm motes were not visible from inside the dome.
- The solar-damage border was too rectangular and abrupt.
- The clear-sky sun still looked like the normal Minecraft sun.
- Acid-rain visibility was too short.

## V3.4 changes

### Acid rain

- Adds a registered `acid_rain_streak` particle with a narrow translucent neon-green texture.
- Uses `overrideLimiter=true` and `ClientLevel#addAlwaysVisibleParticle` for dome-shell weather.
- Acid-rain streaks spawn every client tick in a denser field.
- When protected inside the dome, rain is spawned immediately outside the physical glass shell instead of 50–70 blocks in front of the camera.
- A custom `assets/minecraft/textures/environment/rain.png` changes vanilla rain sheets to toxic green. This makes precipitation readable even if client particle settings are reduced.
- Rain particles use physics and stop against the dome glass instead of simply passing through it.

### Sandstorm

- Adds a registered `sandstorm_mote` particle with a soft orange/gold fleck texture.
- Particles are smaller than the original blocky prototype but large enough to remain visible through the dome from tens of blocks away.
- Faster horizontal movement, varying crosswind and per-particle turbulence.
- Protected players receive an exterior ring of storm particles just beyond the glass shell.

### Dome-to-surface transition

- Weather presence and exposure are now eased on the client instead of switching in one frame.
- Fog color, fog distance and overlays smoothly transition for roughly one second when entering/leaving protection.
- Protected weather still has a mild atmospheric tint but keeps a long view distance so the storm can be seen through the glass.

### Rain visibility

Default acid-rain fog distance raised from 72 to 144 blocks.
Default acid-thunderstorm fog distance raised from 48 to 112 blocks.
Inside the dome the client uses a much longer protected view target (224 blocks for rain / 176 for thunder), so the dome interior itself is not swallowed by green fog.

### Lethal sun

- The server now synchronizes a separate `solarActive` bit. This is true during lethal clear daytime even when the local player is protected by the dome.
- Protected players therefore see a mild hostile warm atmosphere through the glass.
- Direct exposure still controls damage, strong heat haze and the heat vignette.
- The heat border is now a 14-band smooth red/orange vignette instead of four hard rectangles.
- Adds a built-in replacement `assets/minecraft/textures/environment/sun.png`: red/orange, overheated and much more hostile than the vanilla white square.

### Networking

Weather protocol version is now `4` because `solarActive` was added to `SurfaceWeatherSyncPacket`.
Client and server must use the same V3.4 jar.

## Oxygen ownership

No oxygen logic, oxygen HUD, oxygen item data or oxygen damage code is changed by this patch.
