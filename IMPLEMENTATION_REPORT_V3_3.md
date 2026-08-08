# V3.3 implementation report

Implemented from play-test feedback:

1. Acid rain particles made smaller and substantially denser.
2. Acid rain and sandstorm weather remain visually readable from inside the glass dome by spawning only the exterior particle field with always-visible client particles.
3. Sandstorm particles replaced with small orange/gold dust and given faster, turbulent horizontal motion.
4. User-provided rain and sandstorm MP3 assets converted to valid Vorbis OGG resources.
5. Acid rain ambience registered through Forge sound registry; thunderstorm layers thunder rumble.
6. Sandstorm ambience replaced with the supplied sound asset.
7. Added lethal-sun client state and amber desert heat/glare effects.
8. Surface hazard damage cadence increased from 1 hit/sec to 2 hits/sec without increasing the configured damage-per-hit values.
9. Network protocol incremented from 2 to 3 because solar exposure is synchronized to the client.
10. Oxygen implementation remains unchanged.

Validation performed in the build workspace:
- `sounds.json` and language JSON parsed successfully.
- Both weather OGG files verified as Vorbis audio.
- Oxygen package and Oxygen HUD byte-for-byte unchanged from V3.2 workspace.
- Full Gradle compile could not be executed in the sandbox because Gradle 8.8 distribution is not cached and external Gradle download is blocked. Final build verification must be run with `gradlew.bat clean build` on the developer machine.
