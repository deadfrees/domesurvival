# DomeSurvival V3.2 — Surface Weather System

Target: Minecraft 1.20.1 / Forge 47.4.10 / Java 17.

## Implemented

- Direct sunlight hazard outside the generated starter dome.
- Vanilla overworld rain is reinterpreted as **acid rain**.
- Vanilla thunderstorms become **acid thunderstorms** with stronger default damage and denser green atmosphere.
- Rare custom **sandstorms** with persistent server-side scheduling.
- Sandstorms reduce outdoor visibility to 24 blocks by default, create moving sand particles, apply a dust tint and play a dedicated wind sound.
- Weather visuals are synchronized from the server. Damage never depends on whether a client renders the effect.
- A roof/cave blocks direct surface hazard damage because direct sky exposure is required.
- The starter dome and airlock volume remain protected.
- Creative and Spectator players bypass environmental damage.
- Nether/End are not affected yet.

## Visual implementation

No external shader pack is required. The mod uses Forge 1.20.1 client events:

- `ViewportEvent.ComputeFogColor` for green acid atmosphere / sand-colored air;
- `ViewportEvent.RenderFog` for visibility reduction;
- `RenderGuiEvent.Post` for a subtle full-screen atmospheric tint;
- client-local vanilla particles for acid mist and moving sand.

Protected players still see a mild tint through the dome so the outside world visibly remains hostile, but the heavy fog, particles and wind are only applied when directly exposed.

## Random sandstorm defaults

- duration: 120–300 seconds;
- interval: 900–2400 seconds of clear weather;
- interval countdown pauses during vanilla rain/thunder;
- sandstorm state persists in `domesurvival_surface_weather` SavedData.

## Test commands

Acid rain:

```mcfunction
/weather rain
```

Acid thunderstorm:

```mcfunction
/weather thunder
```

Clear weather:

```mcfunction
/weather clear
```

Force a sandstorm:

```mcfunction
/dome weather sandstorm start
```

Stop it:

```mcfunction
/dome weather sandstorm stop
```

Status:

```mcfunction
/dome weather status
```

## Compatibility notes

The oxygen system was intentionally not changed. Surface hazards only use dome geometry, direct sky visibility and their own weather SavedData. The custom client renderer is isolated to `Dist.CLIENT`; no client rendering class is loaded by the dedicated-server event subscriber.

`ModNetwork` protocol is bumped from `1` to `2` because a new S2C weather packet was added. Client and server must therefore run the same V3.2 build.
